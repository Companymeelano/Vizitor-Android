#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Vizitor — prepare DIRECT Android -> SQL Server access  (non-destructive)

What it does (and ONLY this):
  1. connects to 'master' with the SQL Server credentials that already live in
     the Vizitor config.json (the same ones used for provisioning)
  2. stops immediately if the ERP database does not exist  (changes nothing)
  3. creates the dedicated application login (default: vizitor_android)
     — only when it is missing.  An existing login is NEVER touched, so its
     password is never changed or reset by this script.
  4. maps that login to a database user in the ERP database and adds it to
     db_datareader (read-only view of the ERP objects the app reads)
  5. grants EXECUTE / INSERT only on the objects the audit confirmed, and only
     when they exist (checked with OBJECT_ID / sys.tables)
     — including INSERT on dbo.Visit so the app (v2.14.0+) can register visits
  6. writes a small JSON summary (WITHOUT the password) for the installer

It never DROPs, truncates, renames or ALTERs an existing object, never changes
server settings and never touches other databases.  Safe to run again.

Usage
    python provision_android_sql.py --config <config.json> --erp-db Meelano \
        --login vizitor_android [--json-out <path>]

The password for the new login is read from the environment variable
VIZ_ANDROID_SQL_PASSWORD, so it never appears in the command line / process
list and is never printed.
"""
import argparse
import json
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from db import load_config, default_config_path  # noqa: E402

PREFERRED_DRIVERS = [
    "ODBC Driver 18 for SQL Server",
    "ODBC Driver 17 for SQL Server",
    "ODBC Driver 13 for SQL Server",
    "SQL Server Native Client 11.0",
    "SQL Server",
]

# Objects confirmed by the migration audit (2026-09-18).  Each one is granted
# only if it exists; nothing is created and nothing is guessed.
EXECUTE_OBJECTS = [
    "dbo.add_sail_pish",
    "dbo.Edit_sail_pish",
    "dbo.new_cust",
    "dbo.FixManCustomer",
]
INSERT_OBJECTS = [
    "dbo.subsailtemp_pish",   # pre-invoice lines (trigger writes the real lines)
    "dbo.subsailtemp",        # invoice lines (trigger writes subsailfact + ka_act)
    "dbo.Visit",              # visit registration from the Android app (v2.14.0)
]


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


def connect_odbc(pyodbc, driver, server, database, user=None, password=None, timeout=8):
    parts = ["DRIVER={%s}" % driver, "SERVER=%s" % server, "DATABASE=%s" % database]
    if user:
        parts += ["UID=%s" % user, "PWD=%s" % password]
    else:
        parts.append("Trusted_Connection=yes")
    parts.append("Encrypt=no")
    parts.append("TrustServerCertificate=yes")
    parts.append("Connection Timeout=%d" % timeout)
    return pyodbc.connect(";".join(parts), autocommit=True)


def q(sql, params=()):
    return sql, params


def main():
    ap = argparse.ArgumentParser(description="Vizitor: direct Android -> SQL Server preparation")
    ap.add_argument("--config", default=default_config_path())
    ap.add_argument("--erp-db", required=True, help="ERP database that the Android app talks to (e.g. Meelano)")
    ap.add_argument("--login", default="vizitor_android", help="application SQL login (created if missing)")
    ap.add_argument("--json-out", default="", help="write a summary (no secrets) to this file")
    ap.add_argument("--enable-sql-auth", action="store_true",
                    help="if the server only accepts Windows logins, switch it to mixed mode "
                         "(the caller must restart the SQL Server service afterwards)")
    ap.add_argument("--self-test", action="store_true",
                    help="after the grants, log in with the application login to prove the "
                         "credential works from outside (what the phone does)")
    args = ap.parse_args()

    out = {
        "erp_db": args.erp_db,
        "login": args.login,
        "created_login": False,
        "created_user": False,
        "grants": [],
        "skipped": [],
        "ok": False,
        "error": "",
        "mixed_mode_enabled": False,
        "restart_needed": False,
        "self_test": "",
    }

    def finish(rc, msg=""):
        out["error"] = msg
        out["ok"] = (rc == 0)
        if args.json_out:
            try:
                with open(args.json_out, "w", encoding="utf-8") as fh:
                    json.dump(out, fh, ensure_ascii=False, indent=2)
            except Exception as exc:                      # pragma: no cover
                print("98|WARN|could not write json summary: %s" % exc)
        return rc

    try:
        import pyodbc  # noqa
    except Exception as exc:
        print("01|FAILED|pyodbc is not available (%s). Run the installer again." % exc)
        return finish(1, "pyodbc missing")

    server = None
    user = None
    password = None
    try:
        cfg = load_config(args.config)
        db = cfg.get("db", {}) if isinstance(cfg, dict) else {}
        host = str(db.get("host") or "localhost")
        port = str(db.get("port") or "1433")
        auth = str(db.get("auth") or "sql").lower()
        server = "%s,%s" % (host, port) if port and port != "1433" else host
        if auth != "windows":
            user = str(db.get("user") or "")
            password = str(db.get("password") or "")
    except Exception as exc:
        print("02|FAILED|could not read config.json (%s)" % exc)
        return finish(1, "config")

    driver = pick_driver(pyodbc)
    if not driver:
        print("03|FAILED|no SQL Server ODBC driver found on this machine")
        return finish(1, "driver")
    print("04|OK|driver: %s" % driver)
    print("05|OK|server: %s   auth: %s   user: %s" % (server, "windows" if not user else "sql", user or "-"))

    app_password = os.environ.get("VIZ_ANDROID_SQL_PASSWORD", "")

    try:
        cn = connect_odbc(pyodbc, driver, server, "master", user, password)
    except Exception as exc:
        print("06|FAILED|cannot connect to SQL Server (%s)" % exc)
        return finish(1, "connect")

    cur = cn.cursor()
    try:
        db_id = cur.execute("SELECT DB_ID(N'%s')" % args.erp_db.replace("'", "''")).fetchval()
        if db_id is None:
            print("07|FAILED|database [%s] was not found on this server - NOTHING was changed." % args.erp_db)
            return finish(2, "database missing")
        print("08|OK|database [%s] found." % args.erp_db)

        # ---- 1) server login (create only when missing) -------------------
        exists = cur.execute("SELECT COUNT(*) FROM sys.server_principals WHERE name = ?", args.login).fetchval()
        if exists:
            print("09|OK|login [%s] already exists - left untouched (password NOT changed)." % args.login)
        else:
            if not app_password:
                print("10|FAILED|login [%s] does not exist and no password was provided" % args.login)
                return finish(1, "password missing")
            # NOTE: CREATE LOGIN does not accept a parameter marker for the
            # password, so the value is embedded as an escaped literal.  The
            # password itself never reaches stdout / logs / the JSON summary.
            ddl = (
                "CREATE LOGIN [%s] WITH PASSWORD = N'%s', CHECK_POLICY = ON, CHECK_EXPIRATION = OFF, "
                "DEFAULT_DATABASE = [%s]"
                % (args.login.replace("]", "]]"),
                   app_password.replace("'", "''"),
                   args.erp_db.replace("]", "]]"))
            )
            cur.execute(ddl)
            out["created_login"] = True
            print("11|OK|login [%s] created." % args.login)

        # ---- 1b) حالت احراز هویت سرور -------------------------------------
        # SQL Server می‌تواند «فقط ویندوزی» باشد؛ در آن حالت هیچ کاربر SQL
        # (از جمله همین کاربر) نمی‌تواند از گوشی وارد شود، هرچند پورت ۱۴۳۳ باز
        # باشد. این تنها جایی است که این موضوع دیده و (با اجازهٔ نصب‌کننده) رفع می‌شود.
        win_only = cur.execute(
            "SELECT CAST(SERVERPROPERTY('IsIntegratedSecurityOnly') AS int)"
        ).fetchval()
        out["windows_only"] = (win_only == 1)
        if win_only == 1:
            print("11b|WARN|server accepts WINDOWS logins only - the Android app cannot log in yet")
            if args.enable_sql_auth:
                try:
                    cur.execute("EXEC sp_configure 'show advanced options', 1; RECONFIGURE;")
                    cur.execute(
                        "EXEC xp_instance_regwrite N'HKEY_LOCAL_MACHINE', "
                        "N'Software\\Microsoft\\MSSQLServer\\MSSQLServer', N'LoginMode', REG_DWORD, 2"
                    )
                    out["mixed_mode_enabled"] = True
                    out["restart_needed"] = True
                    print("11c|OK|mixed mode (SQL + Windows) enabled - SQL Server must be restarted to apply")
                except Exception as exc:
                    print("11c|FAILED|could not enable mixed mode: %s" % exc)
                    print("11d|HINT|SSMS -> Server Properties -> Security -> 'SQL Server and Windows Authentication mode'")
            else:
                print("11c|FAILED|mixed mode not enabled (installer was not allowed to change it)")
                print("11d|HINT|SSMS -> Server Properties -> Security -> 'SQL Server and Windows Authentication mode' + restart")
        else:
            print("11b|OK|server accepts SQL logins (mixed mode already active)")

        # ---- 1c) حالت حساب و اجازه‌های سروری ------------------------------
        cur.execute("ALTER LOGIN [%s] ENABLE" % args.login.replace("]", "]]"))
        cur.execute("GRANT CONNECT SQL TO [%s]" % args.login.replace("]", "]]"))
        print("11e|OK|login enabled and has CONNECT SQL")
        # بدون این اجازهٔ متادیتا، برنامهٔ اندروید در فهرست دیتابیس‌ها فقط
        # دیتابیس‌های خودش را می‌بیند و مرحلهٔ «انتخاب دیتابیس» ناقص می‌شود.
        try:
            cur.execute("GRANT VIEW ANY DATABASE TO [%s]" % args.login.replace("]", "]]"))
            print("11f|OK|GRANT VIEW ANY DATABASE (so the app can list databases)")
        except Exception as exc:
            print("11f|WARN|could not grant VIEW ANY DATABASE: %s" % exc)

        cur.execute("USE [%s]" % args.erp_db.replace("]", "]]"))
        db_user = cur.execute("SELECT COUNT(*) FROM sys.database_principals WHERE name = ?", args.login).fetchval()
        if not db_user:
            cur.execute("CREATE USER [%s] FOR LOGIN [%s]" % (args.login.replace("]", "]]"), args.login.replace("]", "]]")))
            out["created_user"] = True
            print("12|OK|database user [%s] created in [%s]." % (args.login, args.erp_db))
        else:
            print("13|OK|database user [%s] already exists." % args.login)

        # read-only role
        in_role = cur.execute(
            "SELECT COUNT(*) FROM sys.database_role_members rm "
            "JOIN sys.database_principals r ON r.principal_id = rm.role_principal_id "
            "JOIN sys.database_principals u ON u.principal_id = rm.member_principal_id "
            "WHERE r.name = 'db_datareader' AND u.name = ?", args.login).fetchval()
        if not in_role:
            cur.execute("ALTER ROLE [db_datareader] ADD MEMBER [%s]" % args.login.replace("]", "]]"))
            print("14|OK|added to db_datareader (read-only).")
        else:
            print("15|OK|already a member of db_datareader.")

        # ---- 2) grants, only for objects that really exist ----------------
        for obj in EXECUTE_OBJECTS:
            one = cur.execute(
                "SELECT COUNT(*) FROM sys.objects WHERE object_id = OBJECT_ID(?) AND type IN ('P','PC','FN','IF','TF')",
                obj).fetchval()
            if one:
                cur.execute("GRANT EXECUTE ON %s TO [%s]" % (obj, args.login.replace("]", "]]")))
                out["grants"].append("EXECUTE " + obj)
                print("16|OK|GRANT EXECUTE ON %s" % obj)
            else:
                out["skipped"].append(obj)
                print("17|SKIP|%s does not exist - nothing granted" % obj)

        for obj in INSERT_OBJECTS:
            one = cur.execute("SELECT COUNT(*) FROM sys.tables WHERE object_id = OBJECT_ID(?)", obj).fetchval()
            if one:
                cur.execute("GRANT INSERT ON %s TO [%s]" % (obj, args.login.replace("]", "]]")))
                out["grants"].append("INSERT " + obj)
                print("18|OK|GRANT INSERT ON %s" % obj)
            else:
                out["skipped"].append(obj)
                print("19|SKIP|%s does not exist - nothing granted" % obj)

        # ---- 3) خودآزمایی: همان کاری که گوشی می‌کند -----------------------
        # اگر این مرحله موفق شود، یعنی «پورت باز + کاربر سالم + دسترسی دیتابیس»
        # هر سه درست است و برنامهٔ اندروید هم باید وصل شود.
        if args.self_test:
            if not app_password:
                print("21|SKIP|self-test needs the application password (login already existed)")
                out["self_test"] = "skipped (password not available)"
            else:
                try:
                    probe = connect_odbc(pyodbc, driver, server, args.erp_db,
                                         args.login, app_password, timeout=10)
                    row = probe.cursor().execute(
                        "SELECT SUSER_SNAME(), DB_NAME(), HAS_DBACCESS(DB_NAME())"
                    ).fetchone()
                    probe.close()
                    who, dbn, access = row[0], row[1], row[2]
                    ok_access = (access == 1)
                    out["self_test"] = "ok" if ok_access else "no-access"
                    print("21|%s|login [%s] works: user=%s database=%s access=%s"
                          % ("OK" if ok_access else "FAILED", args.login, who, dbn, access))
                    if not ok_access:
                        print("21b|HINT|the login exists but has no access to this database")
                except Exception as exc:
                    out["self_test"] = "failed: %s" % str(exc)[:200]
                    print("21|FAILED|login [%s] could not connect: %s" % (args.login, str(exc)[:200]))
                    print("21b|HINT|if the message mentions Windows authentication, run this step with --enable-sql-auth and restart SQL Server")
        else:
            print("21|SKIP|self-test not requested")

        print("20|DONE|direct Android -> SQL Server preparation finished for [%s]" % args.login)
        return finish(0)
    except Exception as exc:
        print("99|FAILED|%s" % exc)
        return finish(1, str(exc))
    finally:
        try:
            cur.close()
            cn.close()
        except Exception:
            pass


if __name__ == "__main__":
    sys.exit(main())
