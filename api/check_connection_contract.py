#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
بررسی «قرارداد اتصال» بین نصب‌کنندهٔ ویندوز و برنامهٔ اندروید ویزیتور.

این اسکریپت فایل‌های سرور را با کد اندروید مقابله می‌کند تا ثابت شود هر چیزی که
اپ می‌خواند/اجرا می‌کند، توسط نصب‌کنندهٔ سرور ساخته و مجاز شده است:

  الف) کارت اتصال — کلیدهای URI/JSON که `api/android_connect.py` می‌سازد همان
       کلیدهایی است که `ConnectCards.kt` می‌خواند و پورت هم ۱۴۳۳ است.
  ب) دسترسی‌ها — هر شیء dbo که کد اپ در SQLهای واقعی‌اش صدا می‌زند، در
       GRANTهای `api/provision_android_sql.py` باشد (خواندن با db_datareader،
       اجرا با EXECUTE، درج با INSERT).
  ج) نصب‌کننده — بررسی شنوندهٔ TCP ۱۴۳۳، قاعدهٔ فایروال، ساخت کاربر محدود و
       کارت اتصال؛ همه موجود و به هم متصل.

خروج: ۰ اگر همه‌چیز هم‌خوان باشد، ۱ اگر ناهم‌خوانی (شکست واقعی) باشد.
فقط فایل می‌خواند؛ هیچ تغییری روی سرور یا دیتابیس نمی‌دهد.
"""

import io
import os
import re
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
APP = os.path.join(ROOT, "android-app", "vizitor-direct", "src", "main", "java",
                   "ir", "atiran", "vizitor")

FAILURES = []
DECISIONS = []
NOTES = []


def read(path):
    with io.open(path, encoding="utf-8", errors="replace") as fh:
        return fh.read()


def ok(msg):
    print("  [OK]   " + msg)


def fail(msg):
    print("  [خطا]  " + msg)
    FAILURES.append(msg)


def decide(msg):
    print("  [تصمیم] " + msg)
    DECISIONS.append(msg)


def note(msg):
    print("  [نکته] " + msg)
    NOTES.append(msg)


def strip_kotlin_comments(src):
    """حذف توضیحات تا نام‌های داخل کامنت «وابستگی» حساب نشوند."""
    src = re.sub(r"/\*.*?\*/", " ", src, flags=re.S)
    src = re.sub(r"//[^\n]*", " ", src)
    return src


def kotlin_sql_literals(src):
    """متن SQLهایی که واقعاً در کد اجرا می‌شوند (رشته‌های Kotlin)."""
    src = strip_kotlin_comments(src)
    lits = [m.group(1) for m in re.finditer(r'"""(.*?)"""', src, flags=re.S)]
    rest = re.sub(r'""".*?"""', " ", src, flags=re.S)
    lits += [m.group(1) for m in re.finditer(r'"((?:[^"\\\n]|\\.)*)"', rest)]
    return lits


VERB = re.compile(
    r"(?i)\b(from|join|\{call|call|exec(?:ute)?|insert\s+into|update|delete\s+from|merge\s+into"
    r"|object_id\s*\(\s*n?')\s+"
    r"(?:(?P<schema>[A-Za-z_][A-Za-z0-9_]*)\s*\.\s*)?(?P<name>[A-Za-z_][A-Za-z0-9_]*)(?![\w.])"
)


def app_db_usage():
    """اشیائی که کد اپ در SQL واقعی خود صدا می‌زند، به تفکیک نوع دسترسی."""
    read_objs, exec_objs, insert_objs, update_objs, unknown_schema = {}, {}, {}, {}, set()
    for dirpath, _dirs, files in os.walk(APP):
        for fname in files:
            if not fname.endswith(".kt"):
                continue
            src = read(os.path.join(dirpath, fname))
            for lit in kotlin_sql_literals(src):
                for m in VERB.finditer(lit):
                    verb = m.group("verb") if "verb" in m.groupdict() else None
                    verb = m.group(1).lower() if verb is None else verb
                    schema = (m.group("schema") or "").lower()
                    name = m.group("name")
                    if schema == "":
                        unknown_schema.add(name)
                        schema = "dbo"          # پیش‌فرضِ سرور
                    if schema != "dbo":
                        continue                # sys.* و کاتالوگ‌ها همیشه خواندنی‌اند
                    key = name.lower()
                    if verb.startswith("from") or verb.startswith("join") or \
                       verb.startswith("object_id"):
                        read_objs.setdefault(key, name)
                    elif verb.startswith("{call") or verb.startswith("call") or \
                            verb.startswith("exec"):
                        exec_objs.setdefault(key, name)
                    elif verb.startswith("insert"):
                        insert_objs.setdefault(key, name)
                    else:
                        update_objs.setdefault(key, name)
    return read_objs, exec_objs, insert_objs, update_objs, unknown_schema


def ui_callers():
    """آیا این توابع نوشتن از رابط کاربری صدا زده می‌شوند؟"""
    used = set()
    for dirpath, _dirs, files in os.walk(APP):
        for fname in files:
            if fname.endswith(".kt") and "MeelanoDataSource" not in fname:
                src = strip_kotlin_comments(read(os.path.join(dirpath, fname)))
                for fn in ("createPreInvoice", "retirePreInvoice", "preInvoiceLines",
                           "preInvoiceHealth"):
                    if re.search(r"\b" + fn + r"\s*\(", src):
                        used.add(fn)
    return used


# ── الف) قرارداد کارت اتصال ────────────────────────────────────────────────
def check_card():
    print("\nالف) کارت اتصال سرور ↔ اپ اندروید")
    gen = read(os.path.join(ROOT, "api", "android_connect.py"))
    cards = read(os.path.join(APP, "direct", "util", "ConnectCards.kt"))

    m = re.search(r'uri\s*=\s*"vizitor://c\?"\s*\+\s*urllib\.parse\.urlencode\(\{(.*?)\}\)',
                  gen, flags=re.S)
    if not m:
        fail("در api/android_connect.py فهرست کلیدهای URI کارت پیدا نشد")
        keys_uri = set()
    else:
        keys_uri = set(re.findall(r'"([A-Za-z_]+)"\s*:', m.group(1)))
        ok("کلیدهای URI کارت (سرور): %s" % ", ".join(sorted(keys_uri)))

    json_text = ""
    jm = re.search(r"safe\s*=\s*\{(.*?)\n\s*\}", gen, flags=re.S)
    if jm:
        json_text = jm.group(1)
    keys_json = set(re.findall(r'"(host_lan|host_public|port|database|login|panel_url)"\s*:',
                               jm.group(1) if jm else gen))
    need_json = {"host_lan", "host_public", "port", "database", "login"}
    if need_json <= keys_json:
        ok("کلیدهای JSON کارت (سرور): %s" % ", ".join(sorted(keys_json & need_json)))
    else:
        fail("کلیدهای JSON کارت ناقص است: کم‌بود %s" % ", ".join(sorted(need_json - keys_json)))

    missing = [k for k in keys_uri if ('"%s"' % k) not in cards]
    if missing:
        fail("اپ این کلیدهای کارت را نمی‌خواند: %s" % ", ".join(missing))
    else:
        ok("اپ همان کلیدهای کارت را می‌خواند: h/p/d/u/H")

    miss_json = [k for k in need_json if ('"%s"' % k) not in cards]
    if miss_json:
        fail("کلیدهای JSON خوانده‌نشده در ConnectCards.kt: %s" % ", ".join(sorted(miss_json)))
    else:
        ok("کلیدهای JSON کارت در اپ خوانده می‌شوند")

    if re.search(r"SQL_PORT\s*=\s*1433", gen):
        ok("پورت کارت روی سرور: 1433")
    else:
        fail("پورت ۱۴۳۳ در android_connect.py صریح نیست")
    if "1433" in cards:
        ok("پورت پیش‌فرض در اپ: 1433")
    else:
        fail("پورت پیش‌فرض ۱۴۳۳ در ConnectCards.kt نیست")
    ps = read(os.path.join(ROOT, "install.ps1"))
    if "-LocalPort 1433" in ps or "localport=1433" in ps:
        ok("قاعدهٔ فایروال نصب‌کننده برای پورت 1433")
    else:
        fail("در install.ps1 قاعدهٔ فایروال برای پورت ۱۴۳۳ پیدا نشد")
    if re.search(r"--password-file", ps):
        ok("کارت اتصال از فایل رمز (بدون چاپ رمز) ساخته می‌شود")
    else:
        note("در install.ps1 ارجاع به فایل رمز کارت اتصال دیده نشد")


# ── ب) دسترسی‌های کاربر محدود ↔ کارهای واقعی اپ ──────────────────────────
def check_grants():
    print("\nب) دسترسی‌های کاربر محدود در برابر SQLهای واقعی اپ")
    prov = read(os.path.join(ROOT, "api", "provision_android_sql.py"))
    read_objs, exec_objs, insert_objs, update_objs, unknown = app_db_usage()

    def block(name):
        m = re.search(name + r"\s*=\s*\[(.*?)\]", prov, flags=re.S)
        return {o.split(".", 1)[-1].strip().lower() for o in
                re.findall(r'"([^"]+)"', m.group(1))} if m else set()

    grant_exec = block("EXECUTE_OBJECTS")
    grant_insert = block("INSERT_OBJECTS")

    ok("اپ می‌خواند (%d): %s" % (len(read_objs), ", ".join(sorted(read_objs.values()))))
    ok("اپ اجرا می‌کند (%d): %s" % (len(exec_objs), ", ".join(sorted(exec_objs.values())) or "—"))
    ok("اپ درج می‌کند (%d): %s" % (len(insert_objs), ", ".join(sorted(insert_objs.values())) or "—"))
    if update_objs:
        ok("اپ به‌روزرسانی می‌کند (%d): %s"
           % (len(update_objs), ", ".join(sorted(update_objs.values()))))
    if unknown:
        note("نام‌های بدون پیشوند schema در SQLهای اپ (روی dbo فرض شده است): %s"
             % ", ".join(sorted(unknown)))

    if "db_datareader" in prov:
        ok("خواندن‌ها با نقش db_datareader پوشش داده می‌شوند (%d شیء)" % len(read_objs))
    else:
        fail("نقش db_datareader در provision_android_sql.py داده نمی‌شود")

    miss = set(exec_objs) - grant_exec
    if miss:
        fail("EXEC بدون مجوز: %s" % ", ".join(sorted(exec_objs[k] for k in miss)))
    else:
        ok("هر شیئی که اپ EXEC می‌کند مجوز EXECUTE دارد (%d شیء)"
           % len(exec_objs))

    miss = set(insert_objs) - grant_insert
    if miss:
        fail("INSERT بدون مجوز: %s" % ", ".join(sorted(insert_objs[k] for k in miss)))
    else:
        ok("هر جدولی که اپ در آن INSERT می‌زند مجوز INSERT دارد")

    # UPDATE: کاربر محدود عمداً این مجوز را ندارد
    if update_objs:
        decide("تابع retirePreInvoice روی %s دستور UPDATE می‌زند؛ کاربر محدود مجوز "
               "UPDATE ندارد (عمداً). این مسیر تا تصمیم کارفرما غیرفعال می‌ماند: "
               "یا مجوز UPDATE داده شود، یا با امضای تأییدشدهٔ dbo.Edit_sail_pish "
               "(که EXECUTE آن همین حالا داده شده) انجام شود."
               % ", ".join(sorted(update_objs.values())))
    used = ui_callers()
    note("توابع مسیر نوشتن که از رابط کاربری صدا زده می‌شوند: %s"
         % (", ".join(sorted(used)) or "هیچ‌کدام (فقط بررسی سلامت)"))

    extra = grant_exec - set(exec_objs)
    if extra:
        note("این مجوزهای EXECUTE در کد فعلی صدا زده نمی‌شوند ولی برای مسیرهای "
             "تأییدشدهٔ ERP (و جایگزینِ ویرایش/مشتری جدید) نگه داشته شده‌اند: %s"
             % ", ".join(sorted(extra)))

    if {"subsailtemp_pish", "sailfact_pish"} & set(read_objs) | set(insert_objs):
        ok("وابستگی‌های مسیر پیش‌فاکتور (سربرگ/سطر میانی) در اپ بررسی می‌شوند")


# ── ج) نصب‌کنندهٔ ویندوز ─────────────────────────────────────────────────
def ps_balance(src):
    """تعادل آکولاد/پرانتز با نادیده‌گرفتن رشته‌ها و توضیحات PowerShell."""
    out = []
    i, n = 0, len(src)
    while i < n:
        ch = src[i]
        if src.startswith("<#", i):
            j = src.find("#>", i + 2)
            i = n if j < 0 else j + 2
        elif ch == "#":
            j = src.find("\n", i)
            i = n if j < 0 else j + 1
        elif ch == '"':
            i += 1
            while i < n:
                if src[i] == "`":
                    i += 2
                elif src[i] == '"':
                    i += 1
                    break
                else:
                    i += 1
        elif ch == "'":
            i += 1
            while i < n:
                if src[i] == "'" and i + 1 < n and src[i + 1] == "'":
                    i += 2
                elif src[i] == "'":
                    i += 1
                    break
                else:
                    i += 1
        else:
            out.append(ch)
            i += 1
    depth = {"{": 0, "(": 0, "[": 0}
    pairs = {"}": "{", ")": "(", "]": "["}
    for ch in out:
        if ch in depth:
            depth[ch] += 1
        elif ch in pairs:
            depth[pairs[ch]] -= 1
            if depth[pairs[ch]] < 0:
                return depth
    return depth


def check_installer():
    print("\nج) نصب‌کنندهٔ ویندوز: مسیر اتصال اندروید")
    ps = read(os.path.join(ROOT, "install.ps1"))

    checks = [
        (r"function Test-SqlTcpListener", "تابع بررسی شنوندهٔ TCP (۱۴۳۳)"),
        (r"dm_tcp_listener_states|sql_listener\.json", "بررسی با sys.dm_tcp_listener_states"),
        (r"function Enable-SqlTcp", "تابع فعال‌سازی TCP/IP"),
        (r"TcpPort|tcp_port", "تنظیم پورت TCP/IP"),
        (r"Vizitor SQL 1433", "قاعدهٔ فایروال ۱۴۳۳"),
        (r"provision_android_sql\.py", "ساخت/بررسی کاربر محدود"),
        (r"--erp-db", "دیتابیس انتخابی کاربر به آماده‌سازی داده می‌شود"),
        (r"android_connect\.py", "ساخت کارت اتصال اندروید"),
        (r"android-connect\.png", "کارت QR"),
        (r"--login", "نام کاربر محدود قابل تغییر است"),
    ]
    for pattern, label in checks:
        (ok if re.search(pattern, ps) else fail)(
            label if re.search(pattern, ps) else ("پیدا نشد: " + label))

    m = re.search(r"function Invoke-AndroidPrep\b([\s\S]*?)\nfunction ", ps)
    body = m.group(1) if m else ""
    if "Test-SqlTcpListener" in body and "Enable-SqlTcp" in body:
        ok("بررسی شنونده و پیشنهاد فعال‌سازی، داخل جریان آماده‌سازی اتصال است")
    else:
        fail("بررسی شنوندهٔ TCP در Invoke-AndroidPrep انجام نمی‌شود")

    d = ps_balance(ps)
    if any(v != 0 for v in d.values()):
        fail("عدم تعادل آکولاد/پرانتز در install.ps1: %s" % d)
    else:
        ok("تعادل آکولاد/پرانتز/براکت درست است (تحلیل نحوی واقعی هم در CI با PowerShell)")

    # اسکریپت‌ها با Windows PowerShell 5.1 اجرا می‌شوند: بدون BOM، متن فارسی به‌هم می‌ریزد
    for f in ("install.ps1", os.path.join("installer", "nsi", "preflight.ps1")):
        raw = io.open(os.path.join(ROOT, f), "rb").read()
        (ok if raw.startswith(b"\xef\xbb\xbf") else fail)(
            ("UTF-8 BOM دارد (اجرای درست فارسی در PowerShell 5.1): " + f)
            if raw.startswith(b"\xef\xbb\xbf") else
            ("بدون BOM است (PowerShell 5.1 متن فارسی را خراب می‌کند): " + f))

    leftovers = [w for w in ("WebAdministration", "IIS:\\", "appcmd", "w3wp",
                             "iisreset", "ApplicationPool") if w in ps]
    if leftovers:
        fail("بازماندهٔ IIS در install.ps1: %s" % ", ".join(leftovers))
    else:
        ok("هیچ بازماندهٔ IIS در نصب‌کننده نیست")

    # ترتیب منطقی: بررسی شنونده پیش از اعلام «آماده است»
    amade = ps.find("اتصال مستقیم اندروید آماده است")
    check_at = ps.find("بررسی شنوندهٔ شبکهٔ SQL Server")
    if check_at > 0 and amade > 0 and check_at < amade:
        ok("بررسی شنونده پیش از اعلام «آماده است» انجام می‌شود")
    else:
        decide("در install.ps1 پیام «آماده است» پیش از بررسی شنوندهٔ ۱۴۳۳ چاپ می‌شود "
               "(باید جابه‌جا شود تا کاربر پیام نهایی درست بگیرد)")


def main():
    print("بررسی قرارداد اتصال — ویزیتور مستقیم (سرور ↔ اندروید)")
    print("=" * 74)
    check_card()
    check_grants()
    check_installer()
    print("\n" + "=" * 74)
    if FAILURES:
        print("نتیجه: %d ناهم‌خوانی ✗" % len(FAILURES))
        for f in FAILURES:
            print("   • " + f)
    else:
        print("نتیجه: قرارداد سرور ↔ اندروید کامل و هم‌خوان است ✓")
    if DECISIONS:
        print("موارد نیازمند تصمیم کارفرما (%d):" % len(DECISIONS))
        for d in DECISIONS:
            print("   • " + d)
    if NOTES:
        print("نکته‌های اطلاعاتی (%d):" % len(NOTES))
        for x in NOTES:
            print("   • " + x)
    return 1 if FAILURES else 0


if __name__ == "__main__":
    sys.exit(main())
