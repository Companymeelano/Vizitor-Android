#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Laye' dadegan (database layer) baraye' API Vizitor.
Supported engines: sqlite3 (default, no dependency) | mysql/mariadb (via optional pymysql).
"""
import hashlib
import hmac
import json
import os
import secrets
import sqlite3
import threading


def default_data_dir():
    if os.path.isdir("/var/lib/vizitor") or os.geteuid() == 0:
        return "/var/lib/vizitor"
    return os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "data")


def default_config_path():
    env = os.environ.get("VIZITOR_CONFIG")
    if env:
        return env
    return os.path.join(default_data_dir(), "config.json")


def load_config(path=None):
    path = path or default_config_path()
    with open(path, "r", encoding="utf-8") as f:
        return json.load(f)


def save_config(cfg, path=None):
    path = path or default_config_path()
    d = os.path.dirname(path)
    if d:
        os.makedirs(d, exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(cfg, f, ensure_ascii=False, indent=2)


def split_statements(sql_text):
    lines = [ln for ln in sql_text.splitlines() if not ln.strip().startswith("--")]
    stmts = []
    for raw in "\n".join(lines).split(";"):
        s = raw.strip()
        if s:
            stmts.append(s)
    return stmts


def hash_password(password, salt=None):
    salt = salt or secrets.token_hex(16)
    dk = hashlib.pbkdf2_hmac("sha256", password.encode("utf-8"), salt.encode("utf-8"), 120000)
    return "%s$%s" % (salt, dk.hex())


def verify_password(password, stored):
    if not stored or "$" not in stored:
        return False
    salt, expected = stored.split("$", 1)
    dk = hashlib.pbkdf2_hmac("sha256", password.encode("utf-8"), salt.encode("utf-8"), 120000)
    return hmac.compare_digest(dk.hex(), expected)


def _jsonable(v):
    """Convert values that json cannot serialize (e.g. SQL Server DATETIME2) to str."""
    if v is None or isinstance(v, (bool, int, float, str)):
        return v
    return str(v)


class DB:
    """Thin thread-safe wrapper around sqlite3, pymysql or pyodbc (SQL Server)."""

    def __init__(self, cfg):
        self.cfg = cfg or {}
        self.lock = threading.RLock()
        self.conn = None
        self.engine = (self.cfg.get("db") or {}).get("engine", "sqlite")
        if self.engine not in ("sqlite", "mysql", "sqlserver"):
            raise ValueError("unsupported db engine: %s" % self.engine)
        self._pymysql = None
        self._pyodbc = None
        if self.engine == "mysql":
            import pymysql  # noqa: optional dependency

            self._pymysql = pymysql
        if self.engine == "sqlserver":
            import pyodbc  # noqa: optional dependency

            self._pyodbc = pyodbc
        self.connect()

    # ------------------------------------------------------------ connection
    def connect(self):
        d = self.cfg.get("db") or {}
        if self.engine == "mysql":
            self.conn = self._pymysql.connect(
                host=d.get("host", "127.0.0.1"),
                port=int(d.get("port", 3306)),
                user=d.get("user", "vizitor"),
                password=d.get("password", ""),
                database=d.get("name", "vizitor"),
                charset="utf8mb4",
                autocommit=True,
                connect_timeout=8,
            )
        elif self.engine == "sqlserver":
            self.conn = self._connect_sqlserver(d)
        else:
            path = d.get("path") or os.path.join(default_data_dir(), "vizitor.db")
            parent = os.path.dirname(path)
            if parent:
                os.makedirs(parent, exist_ok=True)
            self.conn = sqlite3.connect(path, check_same_thread=False)
            self.conn.row_factory = sqlite3.Row
        self.ensure_schema()

    def _connect_sqlserver(self, d):
        host = d.get("host", "localhost") or "localhost"
        port = int(d.get("port", 1433))
        name = d.get("name", "vizitor")
        auth = (d.get("auth", "sql") or "sql").lower()
        try:
            drivers = list(self._pyodbc.drivers())
        except Exception:
            drivers = []
        preferred = [
            "ODBC Driver 18 for SQL Server",
            "ODBC Driver 17 for SQL Server",
            "ODBC Driver 13 for SQL Server",
            "SQL Server Native Client 11.0",
            "SQL Server",
        ]
        driver = next((p for p in preferred if p in drivers), None)
        if driver is None:
            if drivers:
                driver = drivers[0]
            else:
                raise RuntimeError(
                    "no ODBC driver for SQL Server found on this machine. "
                    "Install 'ODBC Driver 17 for SQL Server' (see INSTALL.md)."
                )
        common = (
            "DRIVER={%s};SERVER=tcp:%s,%d;DATABASE=%s;"
            "TrustServerCertificate=yes;Encrypt=no;Connection Timeout=8;"
            % (driver, host, port, name)
        )
        if auth == "windows":
            conn_str = common + "Trusted_Connection=yes;"
        else:
            conn_str = common + "UID=%s;PWD=%s;" % (d.get("user", ""), d.get("password", ""))
        return self._pyodbc.connect(conn_str, autocommit=True)

    def _schema_file(self):
        base = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
        names = {
            "mysql": "schema_mysql.sql",
            "sqlite": "schema_sqlite.sql",
            "sqlserver": "schema_sqlserver.sql",
        }
        return os.path.join(base, "database", names[self.engine])

    def ensure_schema(self):
        path = self._schema_file()
        if not os.path.exists(path):
            return
        with open(path, "r", encoding="utf-8") as f:
            text = f.read()
        with self.lock:
            for stmt in split_statements(text):
                self.conn.execute(stmt)
            self.conn.commit()

    # ------------------------------------------------------------- execution
    def _convert(self, sql):
        if self.engine == "mysql":
            return sql.replace("?", "%s")
        return sql

    def q(self, sql, params=()):
        with self.lock:
            cur = self.conn.execute(self._convert(sql), tuple(params))
            self.conn.commit()
            rows = cur.fetchall()
            if self.engine == "sqlite":
                return [dict(r) for r in rows]
            cols = [c[0] for c in cur.description] if cur.description else []
            return [
                {col: _jsonable(val) for col, val in zip(cols, row)}
                for row in rows
            ]

    def execute(self, sql, params=()):
        with self.lock:
            cur = self.conn.execute(self._convert(sql), tuple(params))
            self.conn.commit()
            if self.engine == "sqlserver":
                # pyodbc lastrowid is unreliable on SQL Server
                cur2 = self.conn.execute("SELECT CAST(SCOPE_IDENTITY() AS BIGINT)")
                row = cur2.fetchone()
                return int(row[0]) if row else None
            return cur.lastrowid

    def ping(self):
        with self.lock:
            self.conn.execute("SELECT 1")
            return True

    # ------------------------------------------------------------- settings
    def upsert_setting(self, key, value):
        value = str(value)
        if self.engine == "mysql":
            self.execute(
                "INSERT INTO settings (k, v) VALUES (?, ?) "
                "ON DUPLICATE KEY UPDATE v = VALUES(v)",
                (key, value),
            )
        elif self.engine == "sqlserver":
            self.execute(
                "IF EXISTS (SELECT 1 FROM settings WHERE k = ?) "
                "UPDATE settings SET v = ? WHERE k = ? "
                "ELSE INSERT INTO settings (k, v) VALUES (?, ?)",
                (key, value, key, key, value),
            )
        else:
            self.execute(
                "INSERT INTO settings (k, v) VALUES (?, ?) "
                "ON CONFLICT(k) DO UPDATE SET v = excluded.v",
                (key, value),
            )

    def get_setting(self, key):
        rows = self.q("SELECT v FROM settings WHERE k = ?", (key,))
        return rows[0]["v"] if rows else None

    def close(self):
        try:
            if self.conn:
                self.conn.close()
        except Exception:
            pass
