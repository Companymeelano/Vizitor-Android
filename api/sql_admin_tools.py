#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Vizitor — SQL Server admin helpers for the direct-Android path (read only)

Three small jobs, each printing one JSON object (and optionally writing it to a
file).  Nothing is ever created, changed or deleted here.

    databases   list the user databases of a SQL Server instance
                (used by the installer / the Android setup wizard to let the
                operator pick the accounting database)

    probe       look inside one database and report exactly what the Vizitor
                Android app needs: which of the known tables/objects exist,
                how many rows they hold, and whether an active login user is
                available.  Only names confirmed by the migration audit are
                used - nothing is guessed.

    health      same connection test as the installer's "health" tick, reading
                the credentials from config.json

Credentials never come from the command line: they are read from an INI file
(--creds) or from config.json (--config), so a password can never show up in
the process list, in a log or in a report.

Usage
    python sql_admin_tools.py databases --creds creds.ini [--out list.json]
    python sql_admin_tools.py probe     --creds creds.ini --db Meelano [--out probe.json]
    python sql_admin_tools.py health    --config config.json [--db Meelano] [--out health.json]
"""
import argparse
import datetime
import json
import os
import sys

try:
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
except Exception:
    pass

PREFERRED_DRIVERS = [
    "ODBC Driver 18 for SQL Server",
    "ODBC Driver 17 for SQL Server",
    "ODBC Driver 13 for SQL Server",
    "SQL Server Native Client 11.0",
    "SQL Server",
]

# ---------------------------------------------------------------- audit data
# Objects the Android app needs in the accounting database.  Every name below
# comes from the migration audit (docs/VERIFIED-SCHEMA-Meelano.md and
# docs/schema/*.tsv) - none of them is a guess.
EXPECTED_TABLES = [
    ("dbo.sys_users", "کاربران سامانهٔ حسابداری (ورود ویزیتور)"),
    ("dbo.CUSTOMERS", "مشتریان"),
    ("dbo.inventory", "کالاها / موجودی"),
    ("dbo.custgroup", "گروه‌های مشتری (قیمت‌گذاری)"),
    ("dbo.forosh_price", "قیمت‌های فروش"),
    ("dbo.ka_act", "اسناد انبار / گردش کالا"),
    ("dbo.sailfact_pish", "سرِ پیش‌فاکتورها"),
    ("dbo.subsailfact_pish", "سطرهای پیش‌فاکتور"),
    ("dbo.subsailtemp_pish", "جدول واسط نوشتن سطر پیش‌فاکتور"),
    ("dbo.sal_mali", "سال مالی جاری سامانه"),
]
EXPECTED_PROCS = [
    ("dbo.add_sail_pish", "ثبت سرِ پیش‌فاکتور"),
    ("dbo.Edit_sail_pish", "ویرایش پیش‌فاکتور"),
    ("dbo.new_cust", "ثبت مشتری جدید"),
    ("dbo.FixManCustomer", "اصلاح مشتری"),
]
EXPECTED_TRIGGERS = [
    ("trig_sst_pish", "نوشتن سطرهای پیش‌فاکتور از جدول واسط"),
]


def now():
    return datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S")


def read_ini(path):
    out = {}
    try:
        with open(path, encoding="utf-8-sig") as fh:
            for line in fh:
                line = line.strip()
                if not line or line.startswith((";", "#", "[")):
                    continue
                if "=" in line:
                    k, v = line.split("=", 1)
                    out[k.strip().lower()] = v.strip()
    except Exception:
        pass
    return out


def read_json(path):
    try:
        with open(path, encoding="utf-8-sig") as fh:
            return json.load(fh)
    except Exception:
        return {}


def creds_from(args):
    """host / port / user / password, from the INI file or from config.json."""
    host, port, user, password = "localhost", "1433", "", ""
    if args.creds:
        ini = read_ini(args.creds)
        host = ini.get("server") or ini.get("host") or host
        port = ini.get("port") or port
        user = ini.get("user") or ini.get("username") or user
        password = ini.get("pass") or ini.get("password") or password
    if args.config:
        db = (read_json(args.config) or {}).get("db") or {}
        host = db.get("host") or host
        port = str(db.get("port") or port)
        user = db.get("user") or user
        password = db.get("password") or password
    return host, str(port), user, password


def connect(pyodbc, driver, host, port, database, user, password, timeout=8):
    server = host if str(port) in ("", "1433") else "%s,%s" % (host, port)
    parts = ["DRIVER={%s}" % driver, "SERVER=%s" % server, "DATABASE=%s" % (database or "master"),
             "Encrypt=no", "TrustServerCertificate=yes", "Connection Timeout=%d" % timeout]
    if user:
        parts += ["UID=%s" % user, "PWD=%s" % password]
    else:
        parts.append("Trusted_Connection=yes")
    return pyodbc.connect(";".join(parts), autocommit=True)


def pick_driver(pyodbc):
    try:
        drivers = list(pyodbc.drivers())
    except Exception:
        drivers = []
    for want in PREFERRED_DRIVERS:
        if want in drivers:
            return want
    for d in drivers:
        if "SQL Server" in d:
            return d
    return None


def write_list_ini(path, res):
    """INI form of the database list, easy to read from the NSIS installer."""
    lines = ["[result]", "ok=%d" % (1 if res.get("ok") else 0),
             "count=%d" % len(res.get("databases") or []),
             "server=%s" % (res.get("server") or ""),
             "error=%s" % (res.get("error") or "")]
    for i, d in enumerate(res.get("databases") or [], 1):
        hint = "پیشنهاد: جدول‌های ویزیتور را دارد" if d.get("has_vizitor_tables") else d.get("state", "")
        lines += ["[db%d]" % i, "name=%s" % d.get("name", ""), "hint=%s" % hint]
    try:
        d = os.path.dirname(path)
        if d:
            os.makedirs(d, exist_ok=True)
        with open(path, "w", encoding="utf-16") as fh:      # UTF-16: NSIS reads it fine
            fh.write("\r\n".join(lines) + "\r\n")
    except Exception:
        pass


def emit(obj, out_path):
    txt = json.dumps(obj, ensure_ascii=False, indent=2)
    if out_path:
        try:
            d = os.path.dirname(out_path)
            if d:
                os.makedirs(d, exist_ok=True)
            with open(out_path, "w", encoding="utf-8") as fh:
                fh.write(txt + "\n")
        except Exception as exc:
            obj["warn"] = "could not write %s (%s)" % (out_path, exc)
            txt = json.dumps(obj, ensure_ascii=False, indent=2)
    print(txt)
    return 0 if obj.get("ok") else 1


def load_pyodbc(result):
    try:
        import pyodbc
    except Exception as exc:
        result["ok"] = False
        result["error"] = "pyodbc_not_available"
        result["detail"] = str(exc)
        return None
    try:
        pyodbc.drivers()          # touches the ODBC runtime; fails if it is missing
    except Exception as exc:
        result["ok"] = False
        result["error"] = "odbc_runtime_missing"
        result["detail"] = ("%s - on Windows install 'Microsoft ODBC Driver 17/18 for SQL Server' "
                            "(the installer does this automatically)" % exc)
        return None
    return pyodbc


# ------------------------------------------------------------------ commands
LAST_RESULT = {}


def emit_db(obj, out_path):
    """databases command: remember the result so main() can always write the INI."""
    LAST_RESULT.clear()
    LAST_RESULT.update(obj)
    return emit(obj, out_path)


def cmd_databases(args):
    res = {"ok": False, "at": now(), "databases": []}
    pyodbc = load_pyodbc(res)
    if not pyodbc:
        return emit_db(res, args.out)
    driver = pick_driver(pyodbc)
    if not driver:
        res["error"] = "no_sql_driver"
        return emit_db(res, args.out)
    host, port, user, password = creds_from(args)
    res["server"] = host if port in ("", "1433") else "%s,%s" % (host, port)
    res["login"] = user or "(windows)"
    try:
        cn = connect(pyodbc, driver, host, port, "master", user, password)
    except Exception as exc:
        res["error"] = "connect_failed"
        res["detail"] = str(exc)
        return emit_db(res, args.out)
    try:
        cur = cn.cursor()
        rows = cur.execute(
            "SELECT d.name, d.state_desc, "
            "CONVERT(nvarchar(20), d.create_date, 120), "
            "ISNULL((SELECT SUM(p.rows) FROM sys.partitions p "
            "        WHERE p.index_id IN (0,1) AND p.object_id IN "
            "        (SELECT object_id FROM sys.tables t "
            "         WHERE t.name IN (N'sys_users', N'CUSTOMERS', N'inventory', "
            "                          N'sailfact_pish', N'subsailfact_pish'))), 0) "
            "FROM sys.databases d WHERE d.database_id > 4 ORDER BY d.name"
        ).fetchall()
        for name, state, created, rowsn in rows:
            res["databases"].append({
                "name": name,
                "state": state,
                "created": (created or "").strip(),
                "has_vizitor_tables": bool(rowsn),
            })
        res["ok"] = True
        res["count"] = len(res["databases"])
        res["driver"] = driver
    except Exception as exc:
        res["error"] = "query_failed"
        res["detail"] = str(exc)
    finally:
        try:
            cn.close()
        except Exception:
            pass
    return emit_db(res, args.out)


def cmd_listener(args):
    """آیا SQL Server واقعاً روی TCP گوش می‌دهد؟ (فقط خواندن — sys.dm_tcp_listener_states)

    این همان چیزی است که اتصال مستقیم برنامهٔ اندروید به آن وابسته است: اگر پروتکل
    TCP/IP در SQL Server غیرفعال باشد، خودِ سرور (روی همان ماشین) با named pipes وصل
    می‌شود و همه‌چیز سالم به نظر می‌رسد، ولی گوشی هرگز نمی‌تواند وصل شود.
    """
    res = {"ok": False, "at": now()}
    pyodbc = load_pyodbc(res)
    if not pyodbc:
        return emit(res, args.out)
    driver = pick_driver(pyodbc)
    if not driver:
        res["error"] = "no_sql_driver"
        return emit(res, args.out)
    host, port, user, password = creds_from(args)
    want = int(str(args.port or port or "1433"))
    res["wanted_port"] = want
    try:
        cn = connect(pyodbc, driver, host, port, "master", user, password)
    except Exception as exc:
        res["error"] = "connect_failed"
        res["detail"] = str(exc)
        return emit(res, args.out)
    try:
        cur = cn.cursor()
        rows = cur.execute(
            "SELECT ip_address, port, type_desc, state_desc, is_ipv4 "
            "FROM sys.dm_tcp_listener_states ORDER BY port, ip_address").fetchall()
        listeners = []
        for ip_address, lport, type_desc, state_desc, is_ipv4 in rows:
            listeners.append({
                "ip": str(ip_address), "port": int(lport) if lport is not None else None,
                "type": str(type_desc), "state": str(state_desc), "ipv4": bool(is_ipv4),
            })
        res["listeners"] = listeners
        online = [l for l in listeners if l["state"].lower() == "online"]
        res["listening_on_wanted_port"] = any(l["port"] == want for l in online)
        res["any_tcp"] = bool(online)
        res["all_ips"] = any(l["ip"] in ("0.0.0.0", "::") for l in online)
        if res["listening_on_wanted_port"] and res["all_ips"]:
            res["ok"] = True
            res["verdict"] = "tcp_ok_all_ips"
        elif res["listening_on_wanted_port"]:
            res["ok"] = True
            res["verdict"] = "tcp_ok_single_ip"
            res["warn"] = ("TCP روی پورت درست فعال است ولی SQL فقط روی یک آی‌پی گوش می‌دهد؛ "
                           "اگر گوشی به همان کارت شبکه وصل نیست، اتصال برقرار نمی‌شود.")
        elif res["any_tcp"]:
            res["error"] = "wrong_port"
            res["verdict"] = "tcp_on_other_port"
            res["detail"] = "پورت فعال: " + ", ".join(str(l["port"]) for l in online)
        else:
            res["error"] = "tcp_disabled"
            res["verdict"] = "tcp_disabled"
            res["detail"] = ("پروتکل TCP/IP در SQL Server فعال نیست (یا سرویس هنوز ری‌استارت نشده) — "
                             "برنامهٔ اندروید نمی‌تواند وصل شود.")
    except Exception as exc:
        res["error"] = "query_failed"
        res["detail"] = str(exc)
    finally:
        try:
            cn.close()
        except Exception:
            pass
    return emit(res, args.out)


def cmd_probe(args):
    res = {"ok": False, "at": now(), "db": args.db}
    if not args.db:
        res["error"] = "db_required"
        return emit(res, args.out)
    pyodbc = load_pyodbc(res)
    if not pyodbc:
        return emit(res, args.out)
    driver = pick_driver(pyodbc)
    if not driver:
        res["error"] = "no_sql_driver"
        return emit(res, args.out)
    host, port, user, password = creds_from(args)
    res["server"] = host if port in ("", "1433") else "%s,%s" % (host, port)
    try:
        cn = connect(pyodbc, driver, host, port, args.db, user, password)
    except Exception as exc:
        res["error"] = "connect_failed"
        res["detail"] = str(exc)
        return emit(res, args.out)
    tables, missing = [], []
    try:
        cur = cn.cursor()
        res["sql_version"] = cur.execute("SELECT @@VERSION").fetchval()
        res["sql_major"] = None
        try:
            m = cur.execute("SELECT SERVERPROPERTY('ProductMajorVersion')").fetchval()
            res["sql_major"] = int(m) if m is not None else None
        except Exception:
            pass
        cur.execute("USE [%s]" % str(args.db).replace("]", "]]"))
        for obj, fa in EXPECTED_TABLES + EXPECTED_PROCS:
            oid = cur.execute("SELECT OBJECT_ID(N'%s')" % obj.replace("'", "''")).fetchval()
            item = {"name": obj, "what": fa, "present": oid is not None, "rows": None}
            if oid is not None and obj in [t[0] for t in EXPECTED_TABLES]:
                try:
                    item["rows"] = cur.execute(
                        "SELECT SUM(p.rows) FROM sys.partitions p "
                        "WHERE p.object_id = OBJECT_ID(N'%s') AND p.index_id IN (0,1)"
                        % obj.replace("'", "''")).fetchval()
                except Exception:
                    pass
            (tables if item["present"] else missing).append(item)
        for trg, fa in EXPECTED_TRIGGERS:
            oid = cur.execute("SELECT OBJECT_ID(N'%s')" % trg.replace("'", "''")).fetchval()
            item = {"name": trg, "what": fa, "present": oid is not None, "rows": None}
            (tables if item["present"] else missing).append(item)
        try:
            res["active_users"] = cur.execute(
                "SELECT COUNT(*) FROM dbo.sys_users WHERE active = 1").fetchval()
        except Exception:
            res["active_users"] = None
        try:
            res["current_fiscal_db"] = cur.execute("SELECT TOP 1 nam_db FROM dbo.sal_mali").fetchval()
        except Exception:
            res["current_fiscal_db"] = None
        res["found"] = tables
        res["missing"] = missing
        res["preinvoice_ready"] = all(
            any(t["name"] == n and t["present"] for t in tables)
            for n in ("dbo.sailfact_pish", "dbo.subsailfact_pish", "dbo.subsailtemp_pish"))
        res["ok"] = len(missing) == 0
        if missing:
            res["error"] = "some_objects_missing"
            res["detail"] = ", ".join(m["name"] for m in missing)
    except Exception as exc:
        res["error"] = "query_failed"
        res["detail"] = str(exc)
    finally:
        try:
            cn.close()
        except Exception:
            pass
    return emit(res, args.out)


def cmd_health(args):
    res = {"ok": False, "at": now()}
    cfg = read_json(args.config) if args.config else {}
    db = (cfg.get("db") or {}) if isinstance(cfg, dict) else {}
    if not db:
        res["error"] = "no_db_in_config"
        return emit(res, args.out)
    res["engine"] = db.get("engine")
    res["server"] = "%s,%s" % (db.get("host"), db.get("port") or 1433)
    res["database"] = args.db or db.get("name")
    if str(db.get("engine", "")).lower() != "sqlserver":
        res["skipped"] = "engine is not sqlserver"
        res["ok"] = True
        return emit(res, args.out)

    pyodbc = load_pyodbc(res)
    if not pyodbc:
        return emit(res, args.out)
    driver = pick_driver(pyodbc)
    if not driver:
        res["error"] = "no_sql_driver"
        return emit(res, args.out)
    res["driver"] = driver
    try:
        cn = connect(pyodbc, driver, db.get("host"), db.get("port") or 1433,
                     res["database"], db.get("user"), db.get("password"))
        cur = cn.cursor()
        res["server_time"] = str(cur.execute("SELECT GETDATE()").fetchval())
        res["db_name"] = cur.execute("SELECT DB_NAME()").fetchval()
        res["ok"] = True
        checks = {}
        for obj, fa in EXPECTED_TABLES + EXPECTED_PROCS + EXPECTED_TRIGGERS:
            checks[obj] = bool(cur.execute("SELECT OBJECT_ID(N'%s')"
                                           % obj.replace("'", "''")).fetchval())
        res["checks"] = checks
        res["missing_count"] = sum(1 for v in checks.values() if not v)
        cn.close()
    except Exception as exc:
        res["error"] = "connect_failed"
        res["detail"] = str(exc)
    return emit(res, args.out)


def cmd_authmode(args):
    """آیا سرور ورود با کاربر SQL را می‌پذیرد؟ (فقط خواندن)

    اگر `IsIntegratedSecurityOnly = 1` باشد، سرور فقط ورود ویندوزی را می‌پذیرد و
    هیچ کاربر SQL — از جمله کاربری که نصب‌کننده می‌سازد — نمی‌تواند از گوشی وارد
    شود؛ هرچند پورت ۱۴۳۳ باز و سبز باشد. این بررسی همان چیزی است که «ping.eu سبز»
    نمی‌گوید.
    """
    res = {"ok": False, "at": now()}
    pyodbc = load_pyodbc(res)
    if not pyodbc:
        return emit(res, args.out)
    driver = pick_driver(pyodbc)
    if not driver:
        res["error"] = "no_sql_driver"
        return emit(res, args.out)
    host, port, user, password = creds_from(args)
    try:
        cn = connect(pyodbc, driver, host, port, "master", user, password)
    except Exception as exc:
        res["error"] = "connect_failed"
        res["detail"] = str(exc)
        return emit(res, args.out)
    try:
        cur = cn.cursor()
        row = cur.execute(
            "SELECT CAST(SERVERPROPERTY('IsIntegratedSecurityOnly') AS int), "
            "       CAST(SERVERPROPERTY('ProductVersion') AS nvarchar(64)), "
            "       CONVERT(nvarchar(400), @@SERVERNAME)").fetchone()
        windows_only = (row[0] == 1)
        res["windows_only"] = windows_only
        res["mixed_mode"] = (not windows_only)
        res["product_version"] = str(row[1]) if row[1] else ""
        res["server_name"] = str(row[2]) if row[2] else ""
        res["ok"] = True
        res["verdict"] = "windows_only" if windows_only else "mixed_mode"
        if windows_only:
            res["warn"] = ("سرور فقط ورود ویندوزی را می‌پذیرد؛ تا حالت Mixed Mode فعال نشود، "
                           "برنامهٔ اندروید نمی‌تواند با کاربر SQL وارد شود (هرچند پورت ۱۴۳۳ باز است).")
    except Exception as exc:
        res["error"] = "query_failed"
        res["detail"] = str(exc)
    finally:
        try:
            cn.close()
        except Exception:
            pass
    return emit(res, args.out)


def main():
    ap = argparse.ArgumentParser(description="Vizitor SQL admin helpers (read only)")
    sub = ap.add_subparsers(dest="cmd")

    def common(p, need_db=False):
        p.add_argument("--creds", default="", help="INI file with server/port/user/pass")
        p.add_argument("--config", default="", help="Vizitor config.json (alternative)")
        p.add_argument("--out", default="", help="write the JSON result here too")
        if need_db:
            p.add_argument("--db", required=True, help="database to look inside")

    lis = sub.add_parser("listener")
    common(lis)
    lis.add_argument("--port", default="1433", help="پورتی که انتظار داریم SQL روی آن گوش بدهد")
    dbs = sub.add_parser("databases")
    common(dbs)
    dbs.add_argument("--out-ini", default="",
                     help="also write the list as an INI file (for the NSIS installer)")
    common(sub.add_parser("probe"), need_db=True)
    common(sub.add_parser("authmode"))
    hp = sub.add_parser("health")
    common(hp)
    hp.add_argument("--db", default="", help="database to check (default: config name)")

    args = ap.parse_args()
    if args.cmd == "databases":
        rc = cmd_databases(args)
        if getattr(args, "out_ini", ""):
            write_list_ini(args.out_ini, LAST_RESULT or {"ok": False, "databases": [],
                                                         "error": "no_result"})
        return rc
    if args.cmd == "listener":
        return cmd_listener(args)
    if args.cmd == "authmode":
        return cmd_authmode(args)
    if args.cmd == "probe":
        return cmd_probe(args)
    if args.cmd == "health":
        return cmd_health(args)
    ap.print_help()
    return 2


if __name__ == "__main__":
    sys.exit(main())
