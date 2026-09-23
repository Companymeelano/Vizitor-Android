#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Non-destructive SQL Server provisioning for Vizitor.

What it does (and ONLY this):
  1. connects to the 'master' database
  2. creates the target database  — only if it does not exist yet
  3. (SQL auth) creates the login — only if it does not exist yet
     (an existing login's password is NEVER changed)
  4. maps the login to a database user inside the target DB — only if missing
  5. grants db_datareader / db_datawriter / db_ddladmin on the target DB only

It never DROPs, truncates or ALTERs any existing object and never changes
server-level settings (ports, sa password, TCP settings, other databases...).
The script is idempotent: it is safe to re-run any number of times.

Usage: python3 provision_sqlserver.py [--config /path/to/config.json]
"""
import argparse
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from db import default_config_path, load_config  # noqa: E402

PREFERRED_DRIVERS = [
    "ODBC Driver 18 for SQL Server",
    "ODBC Driver 17 for SQL Server",
    "ODBC Driver 13 for SQL Server",
    "SQL Server Native Client 11.0",
    "SQL Server",
]


def pick_driver(pyodbc):
    try:
        drivers = list(pyodbc.drivers())
    except Exception:
        drivers = []
    for p in PREFERRED_DRIVERS:
        if p in drivers:
            return p
    return drivers[0] if drivers else None


def ident(name):
    """Bracket-escape an identifier."""
    return "[%s]" % str(name).replace("]", "]]")


def main():
    parser = argparse.ArgumentParser(description="Vizitor SQL Server provisioning (non-destructive)")
    parser.add_argument("--config", default=None)
    args = parser.parse_args()

    cfg = load_config(args.config or default_config_path())
    d = cfg.get("db") or {}
    if d.get("engine") != "sqlserver":
        print("provision: engine is not sqlserver; nothing to do")
        return

    import pyodbc

    driver = pick_driver(pyodbc)
    if not driver:
        raise SystemExit(
            "provision: no ODBC driver for SQL Server found. "
            "Install 'ODBC Driver 17 for SQL Server' first."
        )

    host = d.get("host", "localhost") or "localhost"
    port = int(d.get("port", 1433))
    db_name = d.get("name", "vizitor") or "vizitor"
    user = d.get("user", "") or ""
    password = d.get("password", "") or ""
    auth = (d.get("auth", "sql") or "sql").lower()

    def conn_str(database):
        common = (
            "DRIVER={%s};SERVER=tcp:%s,%d;DATABASE=%s;"
            "TrustServerCertificate=yes;Encrypt=no;Connection Timeout=8;"
            % (driver, host, port, database)
        )
        if auth == "windows":
            return common + "Trusted_Connection=yes;"
        return common + "UID=%s;PWD=%s;" % (user, password)

    print("provision: connecting to SQL Server at %s:%d (driver: %s, auth: %s)" % (host, port, driver, auth))
    conn = pyodbc.connect(conn_str("master"), autocommit=True)
    cur = conn.cursor()

    def scalar(sql, params=()):
        cur.execute(sql, tuple(params))
        row = cur.fetchone()
        return row[0] if row else None

    try:
        # 1) target database — create only if missing
        exists = scalar("SELECT COUNT(*) FROM sys.databases WHERE name = ?", (db_name,))
        if not exists:
            cur.execute("CREATE DATABASE %s" % ident(db_name))
            print("provision: created database %s" % db_name)
        else:
            print("provision: database %s already exists (left untouched)" % db_name)

        # 2) server login — create only if missing, never touch its password
        if user:
            exists = scalar(
                "SELECT COUNT(*) FROM sys.server_principals WHERE name = ? AND type IN ('S','U','G')",
                (user,),
            )
            if not exists:
                if auth == "windows":
                    cur.execute(
                        "CREATE LOGIN %s FROM WINDOWS" % ident(user)
                    )
                else:
                    cur.execute(
                        "CREATE LOGIN %s WITH PASSWORD = ?" % ident(user),
                        (password,),
                    )
                print("provision: created login %s" % user)
            else:
                print("provision: login %s already exists (password left untouched)" % user)

        # 3) database user inside the target DB + scoped grants
        cur.execute("USE %s" % ident(db_name))
        if user:
            exists = scalar("SELECT COUNT(*) FROM sys.database_principals WHERE name = ?", (user,))
            if not exists:
                if auth == "windows":
                    cur.execute(
                        "CREATE USER %s FOR WINDOWS_LOGIN '%s'" % (ident(user), user.replace("'", "''"))
                    )
                else:
                    cur.execute("CREATE USER %s FOR LOGIN %s" % (ident(user), ident(user)))
                print("provision: created database user %s in %s" % (user, db_name))
            else:
                print("provision: database user %s already exists (left untouched)" % user)

            for role in ("db_datareader", "db_datawriter", "db_ddladmin"):
                is_member = scalar(
                    "SELECT COUNT(*) FROM sys.role_members "
                    "WHERE role_principal_id = (SELECT principal_id FROM sys.database_principals WHERE name = ?) "
                    "AND member_principal_id = (SELECT principal_id FROM sys.database_principals WHERE name = ?)",
                    (role, user),
                )
                if not is_member:
                    cur.execute("ALTER ROLE %s ADD MEMBER %s" % (role, ident(user)))
                    print("provision: granted %s on %s" % (role, db_name))
    finally:
        conn.close()

    print("provision ok: sqlserver %s:%d db=%s auth=%s" % (host, port, db_name, auth))


if __name__ == "__main__":
    main()
