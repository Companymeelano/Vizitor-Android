#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Vizitor — connection card for the Android app  (DIRECT SQL Server only)

The Android app talks to SQL Server directly; there is no API hop in its
connection settings.  This script writes everything needed to configure the app
once, so that afterwards a visitor only types his own user name and password:

    1. server address:  192.168.1.150 (LAN)  and/or  the fixed/public IP
    2. port 1433, plus a database user with full access (one time only)
    3. the app lists the databases of the instance, the operator picks the
       accounting database
    4. the app reads the settings it needs from that database
    5. the connection health is checked and shown

Writes:
    C:\ProgramData\Vizitor\android_config.json     full card (has the SQL password, ACL protected)
    <package>\android-connect.json                   safe card (no password)
    <package>\android-connect.txt                    Persian instructions (printable)
    <package>\android-connect.png / .svg             QR code (server + port + database + user)

The SQL password is NEVER printed to the console or written to the safe card.
The QR generator (segno, BSD-3) is vendored under api/vendor/segno.

Usage
    python android_connect.py --config <config.json> [--package-dir <dir>]
        --erp-db <database chosen from the list> [--login vizitor_android] [--password-file <path>]
        [--host-lan 192.168.1.150] [--host-public 37.143.147.19]
        [--update-config] [--no-qr]
"""
import argparse
import datetime
import json
import os
import sys
import urllib.parse

try:
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
except Exception:
    pass

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(HERE, "vendor"))

SQL_PORT = 1433


def say(msg):
    print(msg, flush=True)


def read_json(path, default=None):
    try:
        with open(path, encoding="utf-8-sig") as fh:
            return json.load(fh)
    except Exception:
        return default


def write_json(path, obj, indent=2):
    d = os.path.dirname(path)
    if d:
        os.makedirs(d, exist_ok=True)
    with open(path, "w", encoding="utf-8") as fh:
        json.dump(obj, fh, ensure_ascii=False, indent=indent)
        fh.write("\n")


def first_good(*cands):
    for cand in cands:
        cand = (cand or "").strip()
        if cand and cand not in ("127.0.0.1", "localhost", "0.0.0.0"):
            return cand
    return ""


def host_addresses(cfg, arg_lan="", arg_public=""):
    """Both server addresses: the LAN one (inside the shop) and the fixed/public one."""
    meta = cfg.get("meta") or {}
    dsql = cfg.get("direct_sql") or {}
    lan = first_good(arg_lan, dsql.get("host_lan"), dsql.get("host"),
                     meta.get("local_ip"), (cfg.get("db") or {}).get("host"))
    public = first_good(arg_public, dsql.get("host_public"), meta.get("public_ip"))
    if not lan:
        lan = public or "SERVER-IP"
    return lan, public


def api_base(cfg, host):
    url = ((cfg.get("api") or {}).get("url") or "").strip().rstrip("/")
    if url:
        # replace whatever host the installer guessed with the LAN IP the phone can reach
        try:
            parts = urllib.parse.urlsplit(url if "//" in url else "http://" + url)
            port = parts.port
            netloc = host + ((":%d" % port) if port and port != 80 else "")
            return "http://%s%s" % (netloc, parts.path.rstrip("/"))
        except Exception:
            pass
    return "http://%s/api" % host


def build_qr_uri(payload, path_png, path_svg):
    try:
        import segno
    except Exception as exc:
        return None, "segno not available (%s)" % exc
    try:
        qr = segno.make(payload, error="m")
        qr.save(path_png, scale=6, border=3, dark="#12183a", light="#ffffff")
        qr.save(path_svg, scale=6, border=3, dark="#12183a", light="#ffffff")
        return qr, None
    except Exception as exc:
        return None, str(exc)


def main():
    ap = argparse.ArgumentParser(description="Vizitor - Android direct SQL connection card")
    ap.add_argument("--config", required=True, help="config.json written by the installer")
    ap.add_argument("--package-dir", default="", help="folder for the safe card / QR (e.g. C:\\Vizitor\\setup)")
    ap.add_argument("--host-lan", default="", help="LAN address of the server (inside the shop)")
    ap.add_argument("--host-public", default="", help="fixed / public address of the server (optional)")
    ap.add_argument("--erp-db", default="", help="accounting database chosen by the operator (required)")
    ap.add_argument("--login", default="", help="restricted SQL login (default: vizitor_android)")
    ap.add_argument("--password-file", default="", help="file that holds the SQL password for that login")
    ap.add_argument("--update-config", action="store_true", help="record the direct_sql block in config.json")
    ap.add_argument("--no-qr", action="store_true", help="skip the QR image")
    args = ap.parse_args()

    cfg = read_json(args.config)
    if not isinstance(cfg, dict):
        say("01|FAILED|cannot read config.json: %s" % args.config)
        return 1

    data_dir = os.path.dirname(os.path.abspath(args.config))
    prev = cfg.get("direct_sql") or {}

    host, host_public = host_addresses(cfg, args.host_lan, args.host_public)

    erp = (args.erp_db or prev.get("database") or "").strip()
    if not erp:
        say("01|FAILED|no accounting database was chosen - run the installer, pick the database "
            "from the list and try again (nothing is forced: any database on your server works)")
        return 2
    login = (args.login or prev.get("login") or "vizitor_android").strip()
    pw_file = (args.password_file or prev.get("password_file")
               or os.path.join(data_dir, "android_app_password.txt")).strip()

    password = ""
    if os.path.exists(pw_file):
        try:
            with open(pw_file, encoding="utf-8-sig") as fh:
                password = fh.read().strip()
        except Exception:
            password = ""

    base = api_base(cfg, host)
    sql_server = "%s,%d" % (host, SQL_PORT)

    block = {
        "enabled": True,
        "mode": "direct_sql",
        "host": host,                     # LAN address (phones inside the shop)
        "host_lan": host,
        "host_public": host_public,       # fixed / internet address, optional
        "port": SQL_PORT,
        "database": erp,
        "login": login,
        "password_file": pw_file,
        "password_ready": bool(password),
        "encrypt": "no",
        "trust_server_certificate": True,
        "application_intent": "ReadOnly",
        "flow": ["server", "sql_credentials", "list_databases", "pick_database",
                 "read_settings", "health_check", "visitor_login"],
        "panel_url": base,
        "updated_at": datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
        "updated_by": "vizitor installer",
    }

    # ---- 1) config.json (the API reads this) -------------------------------
    if args.update_config:
        cfg["direct_sql"] = block
        write_json(args.config, cfg)
        say("02|OK|config.json updated: direct_sql -> %s,%d / %s / %s" % (host, SQL_PORT, erp, login))

    # ---- 2) full card, stays in the protected data folder ------------------
    full = dict(block)
    full["password"] = password
    full["connection_string"] = (
        "jdbc:jtds:sqlserver://%s/%s;user=%s;password=%s;useUnicode=true;characterEncoding=UTF-8"
        % (sql_server, erp, login, "***" if password else "")
    )
    full_path = os.path.join(data_dir, "android_config.json")
    write_json(full_path, full)
    say("03|OK|full card (with SQL password, protected): %s" % full_path)

    # ---- 3) safe card + QR + text, next to the installer -------------------
    safe = dict(block)
    safe["password"] = None
    safe["password_required"] = True
    safe["setup_hint"] = ("the app asks once for a database user with full access, lists the databases, "
                          "the operator picks the accounting one, then the app reads its settings and "
                          "checks the connection health")
    safe["restricted_login_password_file"] = pw_file
    pkg = args.package_dir or os.path.dirname(os.path.abspath(__file__)) + os.sep + ".."
    pkg = os.path.abspath(pkg)

    safe_path = os.path.join(pkg, "android-connect.json")
    write_json(safe_path, safe)
    say("04|OK|safe card (no password): %s" % safe_path)

    # کوتاه و استاندارد: همان پنج مقدار، با کلیدهای یک‌حرفی تا کد QR کم‌چگالی
    # و راحت‌اسکن بماند. معنی کلیدها در کارت متنی آمده است.
    uri = "vizitor://c?" + urllib.parse.urlencode({
        "h": host,                                   # host (LAN)
        "p": str(SQL_PORT),                          # port
        "d": erp,                                    # database
        "u": login,                                  # restricted user (optional)
        "H": host_public,                            # fixed / public host (optional)
    })
    pretty = ("vizitor://connect?sql=%s&db=%s&user=%s%s"
              % (sql_server, erp, login, ("&public=" + host_public) if host_public else ""))
    text_path = os.path.join(pkg, "android-connect.txt")
    with open(text_path, "w", encoding="utf-8") as fh:
        fh.write("کارت اتصال برنامهٔ اندروید ویزیتور — اتصال مستقیم به SQL Server\r\n")
        fh.write("========================================================================\r\n\r\n")
        fh.write("۱) نشانی سرور — هر کدام را که دارید:\r\n")
        if host_public:
            fh.write("     • آی‌پی اختصاصی (از بیرون شبکه): %s   ← اگر بیرون از فروشگاه هستید\r\n" % host_public)
        fh.write("     • آی‌پی داخلی شبکهٔ فروشگاه      : %s   ← اگر داخل فروشگاه هستید\r\n" % host)
        fh.write("   پورت: %d (پیش‌فرض — همان پورتی که باید از بیرون هم باز باشد)\r\n" % SQL_PORT)
        fh.write("\r\n۲) در برنامه، دو اعتبارنامه لازم است (هر دو در همین کارت توضیح داده شده):\r\n")
        fh.write("      الف) «کاربر محدود دیتابیس» = %s  → رمزش در کارت نیست؛\r\n" % login)
        fh.write("           در فایل رمز روی سرور است: %s\r\n" % pw_file)
        fh.write("      ب) خودِ ویزیتور: نام کاربری و کلمهٔ عبور او در جدول sys_users سامانه\r\n")
        fh.write("   اگر بیرون از فروشگاه هستید، در برنامه کلید «اتصال از بیرون شبکه» را روشن کنید.\r\n")
        fh.write("\r\n۳) دکمهٔ «تأیید و دریافت لیست دیتابیس‌ها» → لیست دیتابیس‌های همان سرور می‌آید\r\n")
        fh.write("\r\n۴) دیتابیس برنامه را انتخاب کنید: %s   (هیچ دیتابیسی اجباری نیست)\r\n" % erp)
        fh.write("\r\n۵) «اعمال تنظیمات و تست اتصال» → بررسی سلامت → تمام\r\n")
        fh.write("\r\nاز این پس هر ویزیتور فقط نام کاربری و کلمهٔ عبور خودش را وارد می‌کند\r\n")
        fh.write("و می‌تواند پیش‌فاکتور ثبت و ارسال کند.\r\n")
        fh.write("\r\nکاربر محدود سامانه: %s   (رمز: %s)\r\n" % (login, pw_file))
        fh.write("\r\nبرنامهٔ اندروید: نسخهٔ «ویزیتور مستقیم» — پروژهٔ آمادهٔ ساخت در پوشهٔ android-app\r\n")
        fh.write("     (همین کارت را می‌توانید با دکمهٔ «اسکن کارت اتصال» یا «خواندن فایل android-connect.json» وارد کنید)\r\n")
        fh.write("\r\nمتن QR:  %s\r\n" % uri)
        fh.write("  معنی کلیدها: h=آی‌پی داخلی، p=پورت، d=دیتابیس، u=کاربر محدود، H=آی‌پی اختصاصی\r\n")
        if host_public:
            fh.write("\r\nنکته: برای اتصال از بیرون، پورت %d باید در روتر به همین سرور فوروارد شده باشد\r\n" % SQL_PORT)
            fh.write("و در فایروال ویندوز هم اجازهٔ «از هر آدرس» داشته باشد (تیک مربوطه در نصب‌کننده).\r\n")
        else:
            fh.write("\r\nنکته: اتصال فعلی فقط در شبکهٔ داخلی باز است (آی‌پی اختصاصی وارد نشده).\r\n")
    say("05|OK|Persian connection card: %s" % text_path)

    if not args.no_qr:
        png = os.path.join(pkg, "android-connect.png")
        svg = os.path.join(pkg, "android-connect.svg")
        qr, err = build_qr_uri(uri, png, svg)
        if qr is None:
            say("06|WARN|QR was not generated: %s" % err)
        else:
            say("07|OK|QR code (version %s): %s" % (qr.version, png))
            say("08|OK|QR code vector: %s" % svg)

    say("09|OK|Android app: server %s | database %s | login %s | password %s"
        % (sql_server, erp, login, "ready" if password else "MISSING (run the installer again)"))
    say("10|DONE|connection card ready")
    return 0


if __name__ == "__main__":
    sys.exit(main())
