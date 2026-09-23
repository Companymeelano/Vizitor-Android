#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Vizitor API — HTTP server (standard library only).
Endpoints (JSON, UTF-8):
  GET  /api/ping      -> health ping (no auth, no db needed)
  GET  /api/config    -> public config for the Android app (self-configuration)
  GET  /api/health    -> full health report (db, activation, ...)
  GET  /api/activate  -> current activation state
  POST /api/activate  -> activate with a code  {"code": "..."}
  POST /api/login     -> admin login            {"username": "...", "password": "..."}
  GET  /api/visitors  -> list visitors (bearer token, activated)
  POST /api/visitors  -> create visitor (bearer token, activated)
  GET  /api/visitors/since?after_id=N  -> incremental fetch (bearer token, activated)
  GET  /api/events    -> Server-Sent Events realtime stream (bearer token, activated)

  GET  /               -> the admin panel (panel/index.html)
  GET  /fonts/<file>   -> panel font assets (local files, no internet needed)
  GET  /api/db/info       -> current connection settings (password never returned)
  GET  /api/db/databases  -> read-only list of the databases on the SQL Server
  POST /api/db/test       -> test a host/port/user/password without saving it
  POST /api/db/save       -> store host/port/user/password/database in config.json (admin token)
"""
import argparse
import hmac
import json
import os
import queue
import secrets
import signal
import sys
import threading
import time
import urllib.parse
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from db import DB, default_config_path, hash_password, load_config, save_config, verify_password  # noqa: E402
import sql_admin_tools as sat  # noqa: E402  (read-only SQL helpers, no credentials on the command line)

VERSION = "1.2.0"
START_TIME = time.time()
STATE = {"cfg": None, "db": None, "db_error": None, "config_path": None, "panel_dir": None}

DEFAULT_PANEL_DIR = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "panel")

STATIC_TYPES = {
    ".html": "text/html; charset=utf-8",
    ".css": "text/css; charset=utf-8",
    ".js": "application/javascript; charset=utf-8",
    ".json": "application/json; charset=utf-8",
    ".txt": "text/plain; charset=utf-8",
    ".svg": "image/svg+xml",
    ".png": "image/png",
    ".jpg": "image/jpeg",
    ".jpeg": "image/jpeg",
    ".ico": "image/x-icon",
    ".woff2": "font/woff2",
    ".woff": "font/woff",
    ".ttf": "font/ttf",
}


# ---------------------------------------------------------------- realtime
class EventBus:
    """In-process pub/sub for SSE clients. One queue per subscriber."""

    def __init__(self):
        self._lock = threading.Lock()
        self._clients = set()
        self._seq = 0
        self._seq_lock = threading.Lock()

    def _next_seq(self):
        with self._seq_lock:
            self._seq += 1
            return self._seq

    def subscribe(self):
        q = queue.Queue(maxsize=256)
        with self._lock:
            self._clients.add(q)
        return q

    def unsubscribe(self, q):
        with self._lock:
            self._clients.discard(q)

    def client_count(self):
        with self._lock:
            return len(self._clients)

    def publish(self, event_type, data):
        """Publish to all live SSE clients; slow clients are dropped."""
        ev = {
            "seq": self._next_seq(),
            "type": event_type,
            "time": time.strftime("%Y-%m-%d %H:%M:%S"),
            "data": data,
        }
        with self._lock:
            clients = list(self._clients)
            for q in clients:
                try:
                    q.put_nowait(ev)
                except queue.Full:
                    self._clients.discard(q)
        return ev


EVENT_BUS = EventBus()


def get_db():
    if STATE["db"] is None:
        try:
            STATE["db"] = DB(STATE["cfg"])
            STATE["db_error"] = None
        except Exception as exc:  # keep the HTTP layer alive for diagnostics
            STATE["db"] = None
            STATE["db_error"] = str(exc)
    return STATE["db"]


def api_url(cfg):
    return (cfg.get("api") or {}).get("url", "")


def is_activated(db):
    if not db:
        return False
    try:
        return db.get_setting("activated") == "1"
    except Exception:
        return False


def public_config(cfg):
    db = get_db()
    body = {
        "name": (cfg.get("app") or {}).get("name", "Vizitor"),
        "version": VERSION,
        "api_url": api_url(cfg),
        "activated": is_activated(db),
        "db_engine": (cfg.get("db") or {}).get("engine", "sqlite"),
        "server_time": time.strftime("%Y-%m-%d %H:%M:%S"),
        # real-time capability of the server panel (the Android app talks to SQL directly)
        "realtime": True,
        "sse_path": "/api/events",
        "features": ["self_config", "sse", "incremental_sync"],
    }
    return body


def health_report(cfg):
    db = get_db()
    db_status, db_error = "ok", None
    if db:
        try:
            db.ping()
        except Exception as exc:
            db_status, db_error = "error", str(exc)
    else:
        db_status, db_error = "error", STATE["db_error"]
    return {
        "status": "ok",
        "version": VERSION,
        "api_url": api_url(cfg),
        "db": {
            "engine": (cfg.get("db") or {}).get("engine", "sqlite"),
            "status": db_status,
            "error": db_error,
        },
        "activated": is_activated(db),
        "uptime_sec": int(time.time() - START_TIME),
    }


# ------------------------------------------------------- SQL (read-only) helpers
def db_settings(cfg):
    """Connection settings as they are stored in config.json (no password in the answer)."""
    db = cfg.get("db") or {}
    android = cfg.get("android") or {}
    erp = str(android.get("erp_db") or "").strip()
    if not erp:
        # fall back to the non-secret file the installer writes next to the app
        try:
            txt = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "setup", "connect.txt")
            if os.path.exists(txt):
                for line in io_open_text(txt):
                    if line.lower().startswith("androidsqldatabase="):
                        erp = line.split("=", 1)[1].strip()
        except Exception:
            pass
    return {
        "engine": str(db.get("engine") or "sqlite"),
        "host": str(db.get("host") or ""),
        "port": str(db.get("port") or "1433"),
        "user": str(db.get("user") or ""),
        "database": str(db.get("name") or ""),
        "erp_database": erp,
        "password_set": bool(str(db.get("password") or "")),
    }


def io_open_text(path):
    with open(path, encoding="utf-8", errors="replace") as handle:
        return handle.read().splitlines()


def list_databases(cfg, host, port, user, password, timeout=8):
    """Read-only: the user databases of the instance (same query as sql_admin_tools)."""
    result = {"ok": False, "databases": []}
    pyodbc = sat.load_pyodbc(result)
    if not pyodbc:
        return result
    driver = sat.pick_driver(pyodbc)
    if not driver:
        result["error"] = "no_sql_driver"
        return result
    result["server"] = host if str(port) in ("", "1433") else "%s,%s" % (host, port)
    result["login"] = user or "(windows)"
    try:
        cn = sat.connect(pyodbc, driver, host, str(port), "master", user, password, timeout=timeout)
    except Exception as exc:
        result["error"] = "connect_failed"
        result["detail"] = str(exc)
        return result
    try:
        rows = cn.cursor().execute(
            "SELECT d.name, d.state_desc, CONVERT(nvarchar(20), d.create_date, 120), "
            "ISNULL((SELECT SUM(p.rows) FROM sys.partitions p "
            "        WHERE p.index_id IN (0,1) AND p.object_id IN "
            "        (SELECT object_id FROM sys.tables t "
            "         WHERE t.name IN (N'sys_users', N'CUSTOMERS', N'inventory', "
            "                          N'sailfact_pish', N'subsailfact_pish'))), 0) "
            "FROM sys.databases d WHERE d.database_id > 4 ORDER BY d.name"
        ).fetchall()
        for name, state, created, rows_count in rows:
            result["databases"].append({
                "name": name,
                "state": state,
                "created": (created or "").strip(),
                "has_vizitor_tables": bool(rows_count),
            })
        result["ok"] = True
        result["count"] = len(result["databases"])
        result["driver"] = driver
    except Exception as exc:
        result["error"] = "query_failed"
        result["detail"] = str(exc)
    finally:
        try:
            cn.close()
        except Exception:
            pass
    return result


class Handler(BaseHTTPRequestHandler):
    server_version = "VizitorAPI/" + VERSION
    protocol_version = "HTTP/1.1"

    def log_message(self, fmt, *args):
        sys.stderr.write("[%s] %s\n" % (self.log_date_time_string(), fmt % args))
        sys.stderr.flush()


    def _send(self, code, obj):
        """Send one JSON answer; a client that hangs up first is not an error."""
        body = json.dumps(obj, ensure_ascii=False).encode("utf-8")
        self.send_response(code)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(body)))
        self.send_header("Access-Control-Allow-Origin", "*")
        self.end_headers()
        try:
            self.wfile.write(body)
        except (BrokenPipeError, ConnectionResetError):
            self.close_connection = True

    def _send_file(self, rel):
        """Serve a panel asset; only whitelisted extensions, never outside the panel folder."""
        panel = STATE.get("panel_dir")
        if not panel:
            return False
        rel = urllib.parse.unquote(rel).lstrip("/")
        if rel.startswith("panel/"):      # /panel/index.html -> index.html
            rel = rel[6:]
        if rel in ("", "index.html"):
            rel = "index.html"
        if ".." in rel.replace("\\", "/").split("/") or rel.startswith("/"):
            return False
        full = os.path.abspath(os.path.join(panel, rel.replace("/", os.sep)))
        if not full.startswith(os.path.abspath(panel)):
            return False
        ext = os.path.splitext(full)[1].lower()
        if ext not in STATIC_TYPES or not os.path.isfile(full):
            return False
        try:
            with open(full, "rb") as handle:
                body = handle.read()
        except OSError:
            return False
        self.send_response(200)
        self.send_header("Content-Type", STATIC_TYPES[ext])
        self.send_header("Content-Length", str(len(body)))
        self.send_header("Cache-Control", "no-cache")
        self.send_header("Access-Control-Allow-Origin", "*")
        self.end_headers()
        try:
            self.wfile.write(body)
        except (BrokenPipeError, ConnectionResetError):
            self.close_connection = True
        return True

    def _body(self):
        try:
            length = int(self.headers.get("Content-Length") or 0)
        except ValueError:
            length = 0
        if length <= 0:
            return {}
        try:
            return json.loads(self.rfile.read(length).decode("utf-8"))
        except Exception:
            return {}

    def _authorized_user(self):
        auth = self.headers.get("Authorization", "")
        if not auth.startswith("Bearer "):
            return None
        token = auth[7:].strip()
        if not token:
            return None
        db = get_db()
        if not db:
            return None
        try:
            rows = db.q(
                "SELECT u.id, u.username, u.is_admin FROM sessions s "
                "JOIN users u ON u.id = s.user_id WHERE s.token = ?",
                (token,),
            )
            return rows[0] if rows else None
        except Exception:
            return None

    # -------------------------------------------------------------- routes
    def do_OPTIONS(self):
        self.send_response(204)
        self.send_header("Access-Control-Allow-Origin", "*")
        self.send_header("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
        self.send_header("Access-Control-Allow-Headers", "Content-Type, Authorization")
        self.send_header("Content-Length", "0")
        self.end_headers()

    def do_GET(self):
        path = self.path.split("?", 1)[0]
        if not path.startswith("/api/"):
            if not self._send_file(path):
                self._send(404, {"error": "not_found", "path": self.path,
                                 "hint": "panel files live in the panel folder (--panel)"})
            return
        if path != "/":
            path = path.rstrip("/")
        if path == "/api/events":
            # SSE streams handle their own errors; never wrap in the 500 handler
            self._events_stream()
            return
        try:
            if path in ("", "/"):
                self._send(
                    200,
                    {
                        "name": "Vizitor API",
                        "version": VERSION,
                        "endpoints": [
                            "GET  /api/ping",
                            "GET  /api/config",
                            "GET  /api/health",
                            "GET  /api/activate",
                            "POST /api/activate",
                            "POST /api/login",
                            "GET  /api/visitors",
                            "POST /api/visitors",
                            "GET  /api/visitors/since?after_id=N",
                            "GET  /api/events   (SSE realtime stream)",
                    "GET  /             (admin panel)",
                    "GET  /api/db/info      (connection settings, no password)",
                    "GET  /api/db/databases (read-only database list)",
                    "POST /api/db/test      {host, port, user, password}",
                    "POST /api/db/save      {host, port, user, password, database}  (admin token)",
                        ],
                    },
                )
            elif path == "/api/ping":
                self._send(200, {"status": "ok", "time": time.time()})
            elif path == "/api/config":
                self._send(200, public_config(STATE["cfg"]))
            elif path == "/api/health":
                self._send(200, health_report(STATE["cfg"]))
            elif path == "/api/activate":
                db = get_db()
                self._send(200, {"activated": is_activated(db)})
            elif path == "/api/visitors":
                self._list_visitors()
            elif path == "/api/db/info":
                self._send(200, dict({"ok": True}, **db_settings(STATE["cfg"])))
            elif path == "/api/db/databases":
                settings = db_settings(STATE["cfg"])
                result = list_databases(STATE["cfg"], settings["host"] or "localhost",
                                        settings["port"] or "1433", settings["user"],
                                        str((STATE["cfg"].get("db") or {}).get("password") or ""))
                self._send(200 if result.get("ok") else 502, result)
            elif path == "/api/visitors/since":
                self._list_visitors_since()
            else:
                self._send(404, {"error": "not_found", "path": self.path})
        except (BrokenPipeError, ConnectionResetError):
            pass
        except Exception as exc:
            try:
                self._send(500, {"error": "internal", "detail": str(exc)})
            except Exception:
                pass

    def do_POST(self):
        path = self.path.split("?", 1)[0]
        if path != "/":
            path = path.rstrip("/")
        try:
            if path == "/api/activate":
                self._activate()
            elif path == "/api/login":
                self._login()
            elif path == "/api/db/test":
                self._db_test()
            elif path == "/api/db/save":
                self._db_save()
            elif path == "/api/visitors":
                self._create_visitor()
            else:
                self._send(404, {"error": "not_found", "path": self.path})
        except (BrokenPipeError, ConnectionResetError):
            pass
        except Exception as exc:
            try:
                self._send(500, {"error": "internal", "detail": str(exc)})
            except Exception:
                pass

    # ----------------------------------------------------------- endpoints

    def _activate(self):
        """Activate the system: the first code wins, later ones must match it."""
        db = get_db()
        if not db:
            self._send(500, {"ok": False, "error": "db_unavailable", "detail": STATE["db_error"]})
            return
        code = str(self._body().get("code", "")).strip()
        if not code:
            self._send(400, {"ok": False, "error": "code_required"})
            return
        stored = db.get_setting("activation_code")
        if not stored:
            db.upsert_setting("activation_code", code)
            db.upsert_setting("activated", "1")
            db.upsert_setting("activated_at", time.strftime("%Y-%m-%d %H:%M:%S"))
            EVENT_BUS.publish("activation.changed", {"activated": True})
            self._send(200, {"ok": True, "activated": True, "message": "first activation saved"})
        elif hmac.compare_digest(stored, code):
            db.upsert_setting("activated", "1")
            EVENT_BUS.publish("activation.changed", {"activated": True})
            self._send(200, {"ok": True, "activated": True})
        else:
            self._send(403, {"ok": False, "activated": False, "error": "invalid_code"})

    def _db_test(self):
        """Test a connection without saving anything."""
        body = self._body()
        result = list_databases(
            STATE["cfg"],
            str(body.get("host") or "localhost").strip(),
            str(body.get("port") or "1433").strip(),
            str(body.get("user") or "").strip(),
            str(body.get("password") or ""),
        )
        self._send(200 if result.get("ok") else 502, result)

    def _db_save(self):
        """Store connection settings (and the chosen accounting database) in config.json.

        Only the admin (bearer token from /api/login) may write.  The password is written
        to the config file but is never sent back in any answer.
        """
        if not self._authorized_user():
            self._send(401, {"ok": False, "error": "unauthorized",
                             "detail": "برای تغییر تنظیمات، اول در کارت «ورود مدیر» وارد شوید."})
            return
        body = self._body()
        cfg = STATE["cfg"]
        db = cfg.setdefault("db", {})
        saved = []
        allowed = ("host", "port", "user", "password", "name", "engine")
        for key in allowed:
            if key in body and body[key] is not None and str(body[key]) != "":
                db[key] = str(body[key]).strip() if key != "port" else int(str(body[key]).strip())
                saved.append(key)
        database = str(body.get("database") or "").strip()
        if database:
            cfg.setdefault("android", {})["erp_db"] = database
            saved.append("database")
        if not saved:
            self._send(400, {"ok": False, "error": "nothing_to_save"})
            return
        try:
            save_config(cfg, STATE.get("config_path"))
        except Exception as exc:
            self._send(500, {"ok": False, "error": "save_failed", "detail": str(exc)})
            return
        EVENT_BUS.publish("settings.changed", {"saved": saved})
        answer = dict({"ok": True, "saved": saved}, **db_settings(cfg))
        self._send(200, answer)

    def _login(self):
        db = get_db()
        if not db:
            self._send(500, {"ok": False, "error": "db_unavailable", "detail": STATE["db_error"]})
            return
        body = self._body()
        username = str(body.get("username", "")).strip()
        password = str(body.get("password", ""))
        try:
            rows = db.q("SELECT id, username, password_hash, is_admin FROM users WHERE username = ?", (username,))
        except Exception as exc:
            self._send(500, {"ok": False, "error": "db_error", "detail": str(exc)})
            return
        if rows and verify_password(password, rows[0]["password_hash"]):
            token = secrets.token_hex(32)
            db.execute("INSERT INTO sessions (token, user_id) VALUES (?, ?)", (token, rows[0]["id"]))
            self._send(
                200,
                {
                    "ok": True,
                    "token": token,
                    "user": {
                        "id": rows[0]["id"],
                        "username": rows[0]["username"],
                        "is_admin": bool(rows[0]["is_admin"]),
                    },
                },
            )
        else:
            self._send(401, {"ok": False, "error": "bad_credentials"})

    def _gate(self):
        """Common checks for data endpoints; sends an error and returns False on failure."""
        db = get_db()
        if not db:
            self._send(500, {"error": "db_unavailable", "detail": STATE["db_error"]})
            return db, False
        if not is_activated(db):
            self._send(402, {"error": "not_activated", "detail": "send your activation code to POST /api/activate"})
            return db, False
        user = self._authorized_user()
        if not user:
            self._send(401, {"error": "unauthorized", "detail": "login first with POST /api/login"})
            return db, False
        return db, True

    def _list_visitors(self):
        db, ok = self._gate()
        if not ok:
            return
        try:
            if db.engine == "sqlserver":
                rows = db.q(
                    "SELECT TOP (100) id, name, phone, purpose, host_name, created_at "
                    "FROM visitors ORDER BY id DESC"
                )
            else:
                rows = db.q(
                    "SELECT id, name, phone, purpose, host_name, created_at "
                    "FROM visitors ORDER BY id DESC LIMIT 100"
                )
        except Exception as exc:
            self._send(500, {"error": "db_error", "detail": str(exc)})
            return
        self._send(200, {"ok": True, "count": len(rows), "visitors": rows})

    def _list_visitors_since(self):
        """Incremental fetch: visitors with id > after_id (ascending)."""
        db, ok = self._gate()
        if not ok:
            return
        query = self.path.split("?", 1)[1] if "?" in self.path else ""
        params = urllib.parse.parse_qs(query)
        try:
            after_id = int((params.get("after_id") or ["0"])[0])
        except ValueError:
            after_id = 0
        try:
            limit = max(1, min(int((params.get("limit") or ["100"])[0]), 500))
        except ValueError:
            limit = 100
        cols = "id, name, phone, purpose, host_name, created_at"
        try:
            if db.engine == "sqlserver":
                rows = db.q(
                    "SELECT TOP (?) %s FROM visitors WHERE id > ? ORDER BY id ASC" % cols,
                    (limit, after_id),
                )
            else:
                rows = db.q(
                    "SELECT %s FROM visitors WHERE id > ? ORDER BY id ASC LIMIT ?" % cols,
                    (after_id, limit),
                )
        except Exception as exc:
            self._send(500, {"error": "db_error", "detail": str(exc)})
            return
        max_id = rows[-1]["id"] if rows else after_id
        self._send(
            200,
            {
                "ok": True,
                "count": len(rows),
                "visitors": rows,
                "max_id": max_id,
                "has_more": len(rows) == limit,
            },
        )

    def _events_stream(self):
        """Server-Sent Events: pushes visitor.created / activation.changed live."""
        db, ok = self._gate()
        if not ok:
            return
        last_id = 0
        try:
            rows = db.q("SELECT COALESCE(MAX(id), 0) AS m FROM visitors")
            last_id = int(rows[0].get("m", 0)) if rows else 0
        except Exception:
            pass
        q = EVENT_BUS.subscribe()

        def write_event(obj):
            self.wfile.write(("data: %s\n\n" % json.dumps(obj, ensure_ascii=False)).encode("utf-8"))
            self.wfile.flush()

        try:
            self.send_response(200)
            self.send_header("Content-Type", "text/event-stream; charset=utf-8")
            self.send_header("Cache-Control", "no-cache, no-transform")
            self.send_header("Connection", "close")
            self.send_header("X-Accel-Buffering", "no")
            self.send_header("Access-Control-Allow-Origin", "*")
            self.end_headers()
            # hello: lets the client sync state (then GET /api/visitors/since)
            write_event(
                {
                    "type": "hello",
                    "version": VERSION,
                    "activated": is_activated(db),
                    "last_visitor_id": last_id,
                    "retry_ms": 5000,
                }
            )
            while True:
                try:
                    write_event(q.get(timeout=15))
                except queue.Empty:
                    # periodic keepalive comment (15s)
                    try:
                        self.wfile.write(b": keepalive\n\n")
                        self.wfile.flush()
                    except Exception:
                        break
        except (BrokenPipeError, ConnectionResetError, TimeoutError):
            pass
        except Exception:
            pass
        finally:
            EVENT_BUS.unsubscribe(q)
            self.close_connection = True

    def _create_visitor(self):
        db, ok = self._gate()
        if not ok:
            return
        body = self._body()
        name = str(body.get("name", "")).strip()
        if not name:
            self._send(400, {"ok": False, "error": "name_required"})
            return
        visitor_id = db.execute(
            "INSERT INTO visitors (name, phone, purpose, host_name) VALUES (?, ?, ?, ?)",
            (
                name,
                str(body.get("phone", "")).strip(),
                str(body.get("purpose", "")).strip(),
                str(body.get("host_name", "")).strip(),
            ),
        )
        # push to realtime subscribers (best effort)
        try:
            rows = db.q(
                "SELECT id, name, phone, purpose, host_name, created_at FROM visitors WHERE id = ?",
                (visitor_id,),
            )
            ev = EVENT_BUS.publish("visitor.created", rows[0] if rows else {"id": visitor_id})
        except Exception:
            ev = EVENT_BUS.publish("visitor.created", {"id": visitor_id})
        self._send(201, {"ok": True, "id": visitor_id, "event_seq": ev.get("seq") if ev else None})


def main():
    parser = argparse.ArgumentParser(description="Vizitor API server")
    parser.add_argument("--config", default=None, help="path to config.json")
    parser.add_argument("--panel", default=None,
                        help="folder holding the admin panel (index.html + fonts)")
    args = parser.parse_args()

    cfg_path = args.config or default_config_path()
    try:
        cfg = load_config(cfg_path)
    except Exception as exc:
        sys.stderr.write("FATAL: cannot load config %s: %s\n" % (cfg_path, exc))
        sys.exit(1)
    STATE["cfg"] = cfg
    STATE["config_path"] = cfg_path
    panel_dir = args.panel or DEFAULT_PANEL_DIR
    if os.path.isfile(os.path.join(panel_dir, "index.html")):
        STATE["panel_dir"] = panel_dir
    else:
        sys.stderr.write("WARN: panel not found in %s — / will return 404\n" % panel_dir)

    api = cfg.get("api") or {}
    host = api.get("bind_ip", "0.0.0.0")
    try:
        port = int(api.get("port", 9595))
    except (TypeError, ValueError):
        port = 9595

    try:
        httpd = ThreadingHTTPServer((host, port), Handler)
        httpd.daemon_threads = True  # SSE listener threads die with the process
    except OSError as exc:
        sys.stderr.write("FATAL: cannot bind %s:%s — %s\n" % (host, port, exc))
        sys.exit(1)

    def shutdown(_signum, _frame):
        threading.Thread(target=httpd.shutdown, daemon=True).start()

    signal.signal(signal.SIGTERM, shutdown)
    signal.signal(signal.SIGINT, shutdown)

    sys.stdout.write(
        "Vizitor API v%s starting on %s:%s (config: %s, api_url: %s, panel: %s)\n"
        % (VERSION, host, port, cfg_path, api_url(cfg), STATE.get("panel_dir") or "-")
    )
    sys.stdout.flush()
    httpd.serve_forever()
    httpd.server_close()


if __name__ == "__main__":
    main()
