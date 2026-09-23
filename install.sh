#!/usr/bin/env bash
# ============================================================================
#  Vizitor — نصب‌کننده هوشمند (Smart Installer)
# ----------------------------------------------------------------------------
#  این اسکریپت:
#   1. سیستم‌عامل، نرم‌افزارها و آی‌پی‌های سرور (عمومی + داخلی) را تشخیص می‌دهد
#   2. پیش‌نیازهای کم (python3, دیتابیس, pip) را با هوش نصب می‌کند
#   3. از کاربر می‌پرسد: آی‌پی/دامنه، پورت، مشخصات دیتابیس، حساب ادمین،
#      کد فعال‌سازی و سایر اطلاعات ضروری — همه با مقادیر پیش‌فرض هوشمند
#   4. فایل‌ها را منتشر می‌کند و فایل تنظیمات (config.json) را می‌سازد
#   5. سرویس API را راه‌اندازی می‌کند (systemd یا روش جایگزین)
#   6. همه‌چیز را چک می‌کند (API، دیتابیس، فعال‌سازی، لاگین) و در صورت
#      هر مشکل، تنظیمات را بازبینی/تعمیر کرده و دوباره چک می‌کند (تا ۴ دور)
#   7. در پایان، آدرس دقیق API برای برنامه اندروید + خلاصه تنظیمات را نمایش می‌دهد
#
#  مثال‌ها:
#    sudo bash install.sh                 # نصب تعاملی (پیشنهادی)
#    sudo bash install.sh --auto          # نصب بدون سؤال (مقادیر هوشمند)
#    sudo bash install.sh --recheck       # فقط بازرسی و تعمیر نصب موجود
#    sudo bash install.sh --port 9000     # اجبار پورت خاص
# ============================================================================

set -u
INSTALLER_VERSION="1.0.0"

# ---------------------------- مسیرهای ثابت ------------------------------
APP_HOME="/opt/vizitor"
DATA_DIR="/var/lib/vizitor"
CONFIG_FILE="$DATA_DIR/config.json"
LOG_FILE="$DATA_DIR/vizitor.log"
PID_FILE="$DATA_DIR/vizitor.pid"
UNIT_FILE="/etc/systemd/system/vizitor.service"
INIT_FILE="/etc/init.d/vizitor"

# ---------------------------- رنگ‌ها -------------------------------------
if [ -t 1 ]; then
  C_OK=$'\033[32m'; C_ERR=$'\033[31m'; C_WARN=$'\033[33m'; C_INFO=$'\033[36m'; C_BOLD=$'\033[1m'; C_END=$'\033[0m'
else
  C_OK=""; C_ERR=""; C_WARN=""; C_INFO=""; C_BOLD=""; C_END=""
fi

ok()   { printf '%s[ OK ]%s %s\n' "$C_OK" "$C_END" "$*"; }
warn() { printf '%s[!!]%s %s\n' "$C_WARN" "$C_END" "$*"; }
err()  { printf '%s[ERR]%s %s\n' "$C_ERR" "$C_END" "$*" >&2; }
info() { printf '     %s%s%s\n' "$C_INFO" "$*" "$C_END"; }

STEP_N=0
step() {
  STEP_N=$((STEP_N + 1))
  printf '\n%s%s==>  گام %s: %s%s\n' "$C_BOLD" "$C_INFO" "$STEP_N" "$*" "$C_END"
}

banner() {
  printf '%s' "$C_BOLD"
  cat <<'EOF'
============================================================
   VIZITOR  —  نصب‌کننده هوشمند سرور
   (Smart Server Installer)
============================================================
EOF
  printf '%s' "$C_END"
}

# ---------------------------- روت بودن ----------------------------------
banner
if [ "$(id -u)" -ne 0 ]; then
  if command -v sudo >/dev/null 2>&1; then
    info "اجرا با دسترسی روت نیاز دارد؛ دوباره با sudo اجرا می‌شود ..."
    exec sudo bash "$0" "$@"
  else
    err "این اسکریپت باید به‌عنوان روت اجرا شود (sudo نیاز است)."
    exit 1
  fi
fi

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

# ---------------------------- پرچم‌ها ------------------------------------
AUTO=0
RECHECK_ONLY=0
FORCE_PORT=""
FORCE_ADDR=""
while [ $# -gt 0 ]; do
  case "$1" in
    --auto|-y) AUTO=1 ;;
    --recheck) RECHECK_ONLY=1 ;;
    --port) FORCE_PORT="${2:-}"; shift ;;
    --ip) FORCE_ADDR="${2:-}"; shift ;;
    -h|--help)
      sed -n '2,/^# =====/{/^# =====/!p}' "$0" | sed 's/^# \{0,1\}//'
      exit 0
      ;;
    *) echo "پرچم ناشناخته: $1 (برای راهنما --help)"; exit 2 ;;
  esac
  shift
done

# ---------------------------- توابع کمکی ---------------------------------
gen_secret() {
  local n="${1:-16}" out=""
  if command -v openssl >/dev/null 2>&1; then
    out="$(openssl rand -base64 64 2>/dev/null | tr -dc 'A-Za-z0-9' | head -c "$n")"
  fi
  if [ -z "$out" ]; then
    out="$(tr -dc 'A-Za-z0-9' </dev/urandom 2>/dev/null | head -c "$n")"
  fi
  printf '%s' "$out"
}

# پرسش از کاربر؛ اگر stdin بسته باشد یا --auto فعال باشد، مقدار پیش‌فرض
# نکته: متن سوال روی stderr می‌رود تا مقدار برگشتی $(ask ...) آلوده نشود
# $1 = سوال    $2 = مقدار پیش‌فرض (اختیاری)
ask() {
  local prompt="$1" def="${2-}" ans=""
  if [ "$AUTO" -eq 1 ]; then
    if [ -n "$def" ]; then info "$prompt [$def]" >&2; else info "$prompt" >&2; fi
    printf '%s' "$def"
    return 0
  fi
  if [ -n "$def" ]; then
    printf '%s%s [%s]%s: ' "$C_BOLD" "$prompt" "$def" "$C_END" >&2
  else
    printf '%s%s%s: ' "$C_BOLD" "$prompt" "$C_END" >&2
  fi
  IFS= read -r ans || ans=""
  if [ -z "$ans" ]; then ans="$def"; fi
  printf '%s' "$ans"
}

# پرسش راز (پسورد) — ورودی نمایش داده نمی‌شود
ask_secret() {
  local prompt="$1" def="${2-}" ans=""
  if [ "$AUTO" -eq 1 ]; then
    if [ -n "$def" ]; then info "$prompt [*** پیش‌فرض (تولیدشده) ***]" >&2; else info "$prompt" >&2; fi
    printf '%s' "$def"
    return 0
  fi
  if [ -n "$def" ]; then
    printf '%s%s [%s]%s: ' "$C_BOLD" "$prompt" "********" "$C_END" >&2
  else
    printf '%s%s%s: ' "$C_BOLD" "$prompt" "$C_END" >&2
  fi
  if [ -t 0 ]; then
    IFS= read -r -s ans || ans=""
    printf '\n' >&2
  else
    IFS= read -r ans || ans=""
  fi
  if [ -z "$ans" ]; then ans="$def"; fi
  printf '%s' "$ans"
}

# آیا پورت آزاد است؟ خروجی: free / used
port_free() {
  python3 - "$1" <<'PY' 2>/dev/null
import socket, sys
s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
try:
    s.bind(("0.0.0.0", int(sys.argv[1])))
    print("free")
except OSError:
    print("used")
finally:
    s.close()
PY
}

# آیا چیزی روی پورت در حال listen است؟ خروجی: up / down
port_up() {
  python3 - "$1" <<'PY' 2>/dev/null
import socket, sys
s = socket.socket()
s.settimeout(3)
try:
    s.connect(("127.0.0.1", int(sys.argv[1])))
    print("up")
except OSError:
    print("down")
finally:
    s.close()
PY
}

http_get()  { curl -fsS -m 10 "$1" 2>/dev/null; }
http_post() { curl -fsS -m 10 -H 'Content-Type: application/json' -d "$2" "$1" 2>/dev/null; }

# ---------------------------- تشخیص سیستم‌عامل --------------------------
step "بررسی سیستم‌عامل و ابزارها"

OS_ID=""
OS_PRETTY=""
if [ -r /etc/os-release ]; then
  # shellcheck disable=SC1091
  . /etc/os-release
  OS_ID="${ID:-unknown}"
  OS_PRETTY="${PRETTY_NAME:-$OS_ID}"
fi
ok "سیستم‌عامل: $OS_PRETTY"

if ! command -v curl >/dev/null 2>&1; then
  warn "curl یافت نشد؛ تلاش برای نصب ..."
  if command -v apt-get; then apt-get install -y -qq curl >/dev/null 2>&1
  elif command -v dnf; then dnf install -y -q curl >/dev/null 2>&1
  elif command -v yum; then yum install -y -q curl >/dev/null 2>&1
  fi
fi
if ! command -v curl >/dev/null 2>&1; then
  err "curl در دسترس نیست و نصب ممکن نشد."
  exit 1
fi

# مدیر بسته (apt/dnf/yum/zypper/apk)
PKG=""
if command -v apt-get >/dev/null 2>&1; then PKG="apt"
elif command -v dnf >/dev/null 2>&1; then PKG="dnf"
elif command -v yum >/dev/null 2>&1; then PKG="yum"
elif command -v zypper >/dev/null 2>&1; then PKG="zypper"
elif command -v apk >/dev/null 2>&1; then PKG="apk"
fi
[ -n "$PKG" ] && ok "مدیر بسته: $PKG" || warn "مدیر بسته شناخته‌شده‌ای یافت نشد (نصب خودکار بسته ممکن نیست)"

pkg_install() {
  case "$PKG" in
    apt) DEBIAN_FRONTEND=noninteractive apt-get install -y -qq "$@" >/dev/null 2>&1 ;;
    dnf) dnf install -y -q "$@" >/dev/null 2>&1 ;;
    yum) yum install -y -q "$@" >/dev/null 2>&1 ;;
    zypper) zypper --non-interactive install "$@" >/dev/null 2>&1 ;;
    apk) apk add --no-cache "$@" >/dev/null 2>&1 ;;
    *) return 1 ;;
  esac
}

# python3 ضروری است
if ! command -v python3 >/dev/null 2>&1; then
  warn "python3 یافت نشد؛ تلاش برای نصب ..."
  pkg_install python3 || true
fi
if ! command -v python3 >/dev/null 2>&1; then
  err "python3 در دسترس نیست و نصبش ممکن نشد. API روی python3 اجرا می‌شود؛ لطفاً python3 را دستی نصب کنید."
  exit 1
fi
PY_VER="$(python3 -c 'import sys; print("%d.%d" % sys.version_info[:2])')"
ok "python3 نسخه $PY_VER"

# systemd؟ (فقط اگر واقعاً کار می‌کند؛ در بعضی کنتنرها systemctl موجود است ولی Bus بسته است)
HAS_SYSTEMD=0
if [ -d /run/systemd/system ] && command -v systemctl >/dev/null 2>&1 \
   && systemctl list-units --type=service --no-pager >/dev/null 2>&1; then
  HAS_SYSTEMD=1
fi
if [ "$HAS_SYSTEMD" -eq 1 ]; then
  ok "systemd فعال است (سرویس رسمی ساخته می‌شود)"
else
  warn "systemd در دسترس نیست — سرویس به‌صورت مستقیم (nohup + init.d) راه‌اندازی می‌شود"
fi

# ---------------------------- تشخیص آی‌پی سرور ---------------------------
step "تشخیص آی‌پی سرور"

PUBLIC_IP="$(curl -4 -fsS --max-time 5 https://api.ipify.org 2>/dev/null | tr -d '[:space:]')"
if ! [[ "$PUBLIC_IP" =~ ^[0-9]{1,3}(\.[0-9]{1,3}){3}$ ]]; then
  PUBLIC_IP=""
  for u in "https://ifconfig.me/ip" "http://checkip.amazonaws.com" "https://icanhazip.com" "https://ip.sb"; do
    ip_try="$(curl -4 -fsS --max-time 5 "$u" 2>/dev/null | tr -d '[:space:]')"
    if [[ "$ip_try" =~ ^[0-9]{1,3}(\.[0-9]{1,3}){3}$ ]]; then PUBLIC_IP="$ip_try"; break; fi
  done
fi

PRIVATE_IP="$(hostname -I 2>/dev/null | awk '{for(i=1;i<=NF;i++){if($i !~ /^169\.254\./ && $i !~ /^127\./){print $i; exit}}}')"
[ -z "$PRIVATE_IP" ] && PRIVATE_IP="$(hostname -I 2>/dev/null | awk '{print $1}')"
[ -z "$PRIVATE_IP" ] && PRIVATE_IP="$(ip -4 -o addr show 2>/dev/null | awk '{print $4}' | cut -d/ -f1 | grep -v '^127\.' | head -1)"

if [ -n "$PUBLIC_IP" ]; then
  ok "آی‌پی عمومی (Public IP): $PUBLIC_IP"
else
  warn "آی‌پی عمومی شناسایی نشد (اتصال به بیرون ممکن نیست) — از آی‌پی داخلی استفاده می‌شود"
fi
if [ -n "$PRIVATE_IP" ]; then
  ok "آی‌پی داخلی (Private IP): $PRIVATE_IP"
fi
if [ -z "$PUBLIC_IP" ] && [ -z "$PRIVATE_IP" ]; then
  err "هیچ آی‌پی‌ای شناسایی نشد! نصب بدون آدرس ممکن نیست."
  exit 1
fi

[ -z "$PUBLIC_IP" ] && [ -n "$FORCE_ADDR" ] && warn "آدرس توسط کاربر اجبار شد: $FORCE_ADDR"

# ---------------------------- نصب موجود؟ --------------------------------
EXISTING=0
[ -f "$CONFIG_FILE" ] && EXISTING=1

MODE="install"   # install | keep | reconfigure
if [ "$EXISTING" -eq 1 ] && [ "$RECHECK_ONLY" -eq 0 ]; then
  if [ "$AUTO" -eq 0 ]; then
    printf '\n%sنصب قبلی Vizitor یافت شد (%s)%s\n' "$C_BOLD" "$CONFIG_FILE" "$C_END"
    printf '  [1] ادامه با تنظیمات فعلی (فقط بازرسی و تعمیر) — پیشنهادی\n'
    printf '  [2] بازپیکربندی (دوباره از من می‌پرسد)\n'
    printf '  [3] خروج\n'
    printf '%sانتخاب [%s]: ' "$C_BOLD" "1" "$C_END"
    IFS= read -r MODE_PICK || MODE_PICK="1"
    case "$MODE_PICK" in
      2) MODE="reconfigure" ;;
      3) info "خروج."; exit 0 ;;
      *) MODE="keep" ;;
    esac
  else
    MODE="keep"
    info "نصب قبلی یافت شد؛ در حالت --auto تنظیمات فعلی حفظ و فقط بازرسی/تعمیر انجام می‌شود"
  fi
fi
[ "$RECHECK_ONLY" -eq 1 ] && MODE="keep"

# ---------------------------- سؤالات تنظیمات ----------------------------
step "دریافت اطلاعات ضروری از شما"

# خواندن مقادیر قبلی (برای مقادیر پیش‌فرض هوشمند در بازپیکربندی)
PREV_ADDR="" PREV_PORT="" PREV_DB_ENGINE="" PREV_DB_HOST="" PREV_DB_DPORT=""
PREV_DB_NAME="" PREV_DB_USER="" PREV_DB_PASS="" PREV_ADMIN_USER="" PREV_ADMIN_PASS=""
PREV_ACT_CODE=""
if [ "$EXISTING" -eq 1 ]; then
  eval "$(python3 - "$CONFIG_FILE" <<'PY'
import json, sys
try:
    c = json.load(open(sys.argv[1], encoding="utf-8"))
except Exception:
    sys.exit(0)
def esc(v):
    s = str(v)
    for ch in ("\\", '"', "$", "`"):
        s = s.replace(ch, "\\" + ch)
    return s
api = c.get("api", {}); db = c.get("db", {}); ad = c.get("admin", {}); ac = c.get("activation", {})
url = api.get("url", "")
host = url.split("://", 1)[-1].split("/")[0]
import re
m = re.match(r"^(https?)://([^:/]+)(?::(\d+))?", url)
addr = m.group(2) if m else host
port = m.group(3) if (m and m.group(3)) else str(api.get("port", ""))
print(f'PREV_ADDR="{esc(addr)}"')
print(f'PREV_PORT="{esc(port)}"')
print(f'PREV_DB_ENGINE="{esc(db.get("engine",""))}"')
print(f'PREV_DB_HOST="{esc(db.get("host",""))}"')
print(f'PREV_DB_DPORT="{esc(db.get("port",""))}"')
print(f'PREV_DB_NAME="{esc(db.get("name",""))}"')
print(f'PREV_DB_USER="{esc(db.get("user",""))}"')
print(f'PREV_DB_PASS="{esc(db.get("password",""))}"')
print(f'PREV_ADMIN_USER="{esc(ad.get("username",""))}"')
print(f'PREV_ADMIN_PASS="{esc(ad.get("password",""))}"')
print(f'PREV_ACT_CODE="{esc(ac.get("code",""))}"')
PY
)" 2>/dev/null
fi

# آدرس پیش‌فرض هوشمند: اجبار کاربر > آدرس قبلی > IP عمومی > IP داخلی
DEFAULT_ADDR="${FORCE_ADDR:-${PREV_ADDR:-${PUBLIC_IP:-$PRIVATE_IP}}}"

# 1) آدرس (IP یا دامنه)
ADDR="$(ask "آدرس سرور برای اتصال برنامه اندروید (IP یا دامنه)" "$DEFAULT_ADDR")"
ADDR="${ADDR:-$DEFAULT_ADDR}"

# 2) پروتکل
if [[ "$ADDR" =~ ^[0-9]{1,3}(\.[0-9]{1,3}){3}$ ]]; then
  PROTO="http"
  info "پروتکل: http (برای IP ساده)"
else
  PROTO="$(ask "پروتکل (http / https)" "http")"
  [ "$PROTO" != "https" ] && PROTO="http"
fi

# 3) پورت
if [ -n "$FORCE_PORT" ]; then
  PORT="$FORCE_PORT"
  info "پورت (اجباری با --port): $PORT"
else
  DEFAULT_PORT="${PREV_PORT:-}"
  if [ -z "$DEFAULT_PORT" ]; then
    for p in 9595 9596 9597 9600; do
      if [ "$(port_free "$p")" = "free" ]; then DEFAULT_PORT="$p"; break; fi
    done
    [ -z "$DEFAULT_PORT" ] && DEFAULT_PORT=9595
  fi
  PORT="$(ask "پورت API (پیشنهاد هوشمند: $DEFAULT_PORT — پورت 80 یعنی بدون شماره پورت در آدرس)" "$DEFAULT_PORT")"
  case "$PORT" in (*[!0-9]*|'') PORT="$DEFAULT_PORT" ;; esac
fi

# 4) موتور دیتابیس
SMART_DB="sqlite"
if command -v mysqld >/dev/null 2>&1 || command -v mariadbd >/dev/null 2>&1 \
   || command -v mysql >/dev/null 2>&1 && mysqladmin ping --silent >/dev/null 2>&1; then
  SMART_DB="mysql"
fi
ENGINE_DEFAULT="${PREV_DB_ENGINE:-$SMART_DB}"
[ -z "$ENGINE_DEFAULT" ] && ENGINE_DEFAULT="$SMART_DB"
DB_ENGINE="$(ask "موتور دیتابیس (mysql / sqlite) — هوشمند: $SMART_DB" "$ENGINE_DEFAULT")"
[ "$DB_ENGINE" != "mysql" ] && DB_ENGINE="sqlite"

DB_HOST="127.0.0.1"; DB_DPORT="3306"
DB_NAME="vizitor";  DB_USER="vizitor"; DB_PASS=""
ADMIN_USER="admin"; ADMIN_PASS="$(gen_secret 16)"
ACT_CODE=""

if [ "$DB_ENGINE" = "mysql" ]; then
  DB_HOST="$(ask "آدرس سرور دیتابیس" "${PREV_DB_HOST:-127.0.0.1}")"
  DB_DPORT="$(ask "پورت دیتابیس" "${PREV_DB_DPORT:-3306}")"
  DB_NAME="$(ask "نام دیتابیس" "${PREV_DB_NAME:-vizitor}")"
  DB_USER="$(ask "نام کاربری دیتابیس" "${PREV_DB_USER:-vizitor}")"
  GEN_DB_PASS="$(gen_secret 16)"
  DB_PASS="$(ask_secret "گذرواژه کاربر دیتابیس (خالی بگذارید = تولید خودکار: $GEN_DB_PASS)" "${PREV_DB_PASS:-$GEN_DB_PASS}")"
  [ -z "$DB_PASS" ] && DB_PASS="$GEN_DB_PASS"
fi

ADMIN_USER="$(ask "نام کاربری ادمین" "${PREV_ADMIN_USER:-admin}")"
GEN_ADMIN_PASS="$(gen_secret 16)"
ADMIN_PASS="$(ask_secret "گذرواژه ادمین (خالی بگذارید = تولید خودکار: $GEN_ADMIN_PASS)" "${PREV_ADMIN_PASS:-$GEN_ADMIN_PASS}")"
[ -z "$ADMIN_PASS" ] && ADMIN_PASS="$GEN_ADMIN_PASS"

ACT_CODE="$(ask "کد فعال‌سازی (Activation Code) — می‌توانید بعداً از برنامه اندروید وارد کنید" "${PREV_ACT_CODE:-}")"
if [ -z "$ACT_CODE" ] && [ "$AUTO" -eq 0 ]; then
  CONFIRM="$(ask "  ⚠ کد فعال‌سازی خالی است؛ برنامه تا ورود کد «غیرفعال» می‌ماند. ادامه؟ [آ/خ]" "آ")"
  case "$CONFIRM" in خ|x|X|n|N) info "خروج."; exit 0 ;; esac
fi

API_URL="$PROTO://$ADDR"
[ "$PORT" != "80" ] && [ "$PORT" != "443" ] && API_URL="$API_URL:$PORT"
API_URL="$API_URL/api"

printf '\n%sخلاصه انتخاب‌ها:%s\n' "$C_BOLD" "$C_END"
info "آدرس API (اندروید): $API_URL"
info "دیتابیس: $DB_ENGINE"
[ "$DB_ENGINE" = "mysql" ] && info "  $DB_NAME @ $DB_HOST:$DB_DPORT (کاربر: $DB_USER)"
info "ادمین: $ADMIN_USER"
if [ -n "$ACT_CODE" ]; then info "کد فعال‌سازی: وارد شده ✓"; else info "کد فعال‌سازی: خالی (فعال‌سازی بعدی)"; fi

# ---------------------------- نصب نرم‌افزارها ----------------------------
step "بررسی و نصب پیش‌نیازهای نرم‌افزاری"

if [ "$DB_ENGINE" = "mysql" ]; then
  # 4.1) سرور دیتابیس
  if ! command -v mysqladmin >/dev/null 2>&1 && ! command -v mariadb-admin >/dev/null 2>&1; then
    info "سرور دیتابیس نصب نیست؛ تلاش برای نصب mariadb-server ..."
    if [ "$PKG" = "apt" ]; then
      timeout 300 bash -c 'DEBIAN_FRONTEND=noninteractive apt-get update -qq >/dev/null 2>&1; DEBIAN_FRONTEND=noninteractive apt-get install -y -qq mariadb-server mariadb-client >/dev/null 2>&1'
    else
      pkg_install mariadb-server mariadb-client
    fi
  fi
  if command -v mysqld >/dev/null 2>&1 || command -v mariadbd >/dev/null 2>&1 || mysqladmin ping --silent >/dev/null 2>&1; then
    ok "سرور دیتابیس (MariaDB/MySQL) در دسترس است"
  else
    warn "نصب سرور دیتابیس ممکن نشد — به‌صورت هوشمند به sqlite تغییر می‌دهم"
    DB_ENGINE="sqlite"
  fi
fi

# 4.2) درایور python برای MySQL (فقط اگر mysql)
if [ "$DB_ENGINE" = "mysql" ]; then
  if ! python3 -c "import pymysql" >/dev/null 2>&1; then
    info "درایور pymysql نصب می‌شود ..."
    if ! python3 -m pip --version >/dev/null 2>&1; then
      pkg_install python3-pip python3-venv >/dev/null 2>&1 || true
    fi
    if python3 -m pip install --quiet pymysql >/dev/null 2>&1 \
       || python3 -m pip install --quiet --break-system-packages pymysql >/dev/null 2>&1; then
      ok "pymysql نصب شد"
    else
      warn "نصب pymysql ممکن نشد — به‌صورت هوشمند به sqlite تغییر می‌دهم"
      DB_ENGINE="sqlite"
    fi
  else
    ok "pymysql از قبل در دسترس است"
  fi
fi

if [ "$DB_ENGINE" = "sqlite" ]; then
  ok "دیتابیس: SQLite (بدون نیاز به سرور جداگانه) — فایل: $DATA_DIR/vizitor.db"
fi

# ---------------------------- راه‌اندازی دیتابیس MySQL ---------------------
start_mariadb() {
  if [ "$HAS_SYSTEMD" -eq 1 ]; then
    systemctl start mariadb >/dev/null 2>&1 || systemctl start mysql >/dev/null 2>&1 || true
  elif command -v service >/dev/null 2>&1; then
    service mariadb start >/dev/null 2>&1 || service mysql start >/dev/null 2>&1 || true
  elif command -v mariadbd-safe >/dev/null 2>&1; then
    mariadbd-safe --skip-syslog >/dev/null 2>&1 &
  elif command -v mysqld_safe >/dev/null 2>&1; then
    mysqld_safe --skip-syslog >/dev/null 2>&1 &
  fi
}

wait_mariadb() {
  local i
  for i in $(seq 1 30); do
    if mysqladmin ping --silent >/dev/null 2>&1; then return 0; fi
    sleep 1
  done
  return 1
}

setup_mysql_db() {
  step "ساخت و تنظیم دیتابیس MySQL"
  if ! mysqladmin ping --silent >/dev/null 2>&1; then
    info "شروع سرور دیتابیس ..."
    start_mariadb
    if ! wait_mariadb; then
      return 1
    fi
  fi
  ok "سرور دیتابیس پاسخ می‌دهد"

  local dbq="${DB_PASS//\'/\'\'}"
  if ! MYSQL_PWD="$dbq" mysql -e "CREATE DATABASE IF NOT EXISTS \`${DB_NAME//\`/\\\`}\` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;" 2>/dev/null; then
    # اگر با گذرواژه شکست خورد (مثلاً کاربر قدیمی)، بدون گذرواژه امتحان کن
    mysql -e "CREATE DATABASE IF NOT EXISTS \`${DB_NAME//\`/\\\`}\` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;" 2>/dev/null || return 1
  fi

  if ! MYSQL_PWD="$dbq" mysql -e "
        CREATE USER IF NOT EXISTS '$DB_USER'@'localhost' IDENTIFIED BY '$dbq';
        CREATE USER IF NOT EXISTS '$DB_USER'@'127.0.0.1' IDENTIFIED BY '$dbq';
        GRANT ALL PRIVILEGES ON \`${DB_NAME//\`/\\\`}\`.* TO '$DB_USER'@'localhost';
        GRANT ALL PRIVILEGES ON \`${DB_NAME//\`/\\\`}\`.* TO '$DB_USER'@'127.0.0.1';
        FLUSH PRIVILEGES;" 2>/dev/null; then
    return 1
  fi
  ok "دیتابیس $DB_NAME و کاربر $DB_USER آماده است"

  if ! mysql "$DB_NAME" < "$APP_HOME/database/schema_mysql.sql" 2>/dev/null; then
    warn "بارگذاری اسکیم با mysql CLI ممکن نشد؛ لایه API خودش جدول‌ها را می‌سازد"
  else
    ok "اسکیم دیتابیس وارد شد"
  fi
  return 0
}

# ---------------------------- انتشار فایل‌ها ------------------------------
step "انتشار فایل‌ها و ساخت فایل تنظیمات"

mkdir -p "$APP_HOME" "$DATA_DIR"
cp -r "$SCRIPT_DIR/api" "$APP_HOME/api"
cp -r "$SCRIPT_DIR/database" "$APP_HOME/database"
ok "فایل‌های برنامه در $APP_HOME منتشر شدند"

# ساخت/به‌روزرسانی config.json (ادغام با مقادیر قبلی)
write_config() {
  DATA_DIR="$DATA_DIR" \
  BIND_IP="0.0.0.0" PORT="$PORT" API_URL="$API_URL" \
  DB_ENGINE="$DB_ENGINE" DB_HOST="$DB_HOST" DB_DPORT="$DB_DPORT" \
  DB_NAME="$DB_NAME" DB_USER="$DB_USER" DB_PASS="$DB_PASS" \
  ADMIN_USER="$ADMIN_USER" ADMIN_PASS="$ADMIN_PASS" ACT_CODE="$ACT_CODE" \
  PUBLIC_IP="$PUBLIC_IP" PRIVATE_IP="$PRIVATE_IP" \
  python3 - <<'PYEOF'
import json, os, datetime

data_dir = os.environ["DATA_DIR"]
cfg_path = os.path.join(data_dir, "config.json")
cfg = {}
if os.path.exists(cfg_path):
    try:
        with open(cfg_path, encoding="utf-8") as f:
            cfg = json.load(f)
    except Exception:
        cfg = {}

cfg.setdefault("app", {"name": "Vizitor", "version": "1.0.0"})
cfg["app"]["name"] = cfg["app"].get("name", "Vizitor")
cfg["app"]["version"] = cfg["app"].get("version", "1.0.0")

api = cfg.setdefault("api", {})
api["bind_ip"] = os.environ["BIND_IP"]
api["port"] = int(os.environ["PORT"])
api["url"] = os.environ["API_URL"]

db = cfg.setdefault("db", {})
db["engine"] = os.environ["DB_ENGINE"]
if os.environ["DB_ENGINE"] == "mysql":
    db["host"] = os.environ["DB_HOST"]
    db["port"] = int(os.environ["DB_DPORT"])
    db["name"] = os.environ["DB_NAME"]
    db["user"] = os.environ["DB_USER"]
    db["password"] = os.environ["DB_PASS"]
else:
    db["path"] = os.path.join(data_dir, "vizitor.db")

adm = cfg.setdefault("admin", {})
adm["username"] = os.environ["ADMIN_USER"]
adm["password"] = os.environ["ADMIN_PASS"]

act = cfg.setdefault("activation", {})
if os.environ["ACT_CODE"]:
    act["code"] = os.environ["ACT_CODE"]

meta = cfg.setdefault("meta", {})
meta.setdefault("installed_at", datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S"))
meta["last_check_at"] = datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S")
meta["public_ip"] = os.environ["PUBLIC_IP"]
meta["private_ip"] = os.environ["PRIVATE_IP"]
meta["installer_version"] = "1.0.0"

os.makedirs(data_dir, exist_ok=True)
with open(cfg_path, "w", encoding="utf-8") as f:
    json.dump(cfg, f, ensure_ascii=False, indent=2)
print(cfg_path)
PYEOF
}

if [ "$MODE" = "keep" ]; then
  # فقط محدثیت فایل تنظیمات: فقط زمان بازرسی را تازه کنیم (مقادیر دست‌نخورده بمانند)
  DATA_DIR="$DATA_DIR" python3 - <<'PYEOF'
import json, os, datetime
p = os.path.join(os.environ["DATA_DIR"], "config.json")
try:
    with open(p, encoding="utf-8") as f:
        c = json.load(f)
except Exception:
    raise SystemExit(1)
c.setdefault("meta", {})["last_check_at"] = datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S")
with open(p, "w", encoding="utf-8") as f:
    json.dump(c, f, ensure_ascii=False, indent=2)
PYEOF
  ok "فایل تنظیمات موجود حفظ شد: $CONFIG_FILE"
else
  write_config >/dev/null && ok "فایل تنظیمات نوشته شد: $CONFIG_FILE"
fi

# ---------------------------- سرویس ---------------------------------------
install_service_files() {
  if [ "$HAS_SYSTEMD" -eq 1 ]; then
    cat > "$UNIT_FILE" <<EOF
[Unit]
Description=Vizitor API
After=network.target mariadb.service mysql.service

[Service]
Type=simple
User=root
WorkingDirectory=$APP_HOME/api
Environment=VIZITOR_CONFIG=$CONFIG_FILE
ExecStart=/usr/bin/env python3 $APP_HOME/api/server.py --config $CONFIG_FILE
Restart=always
RestartSec=3

[Install]
WantedBy=multi-user.target
EOF
    systemctl daemon-reload >/dev/null 2>&1 || true
    systemctl enable vizitor >/dev/null 2>&1 || true
    ok "سرویس systemd ساخته شد: vizitor"
  else
    cat > "$INIT_FILE" <<'EOF'
#!/bin/sh
### BEGIN INIT INFO
# Provides:          vizitor
# Required-Start:    $local_fs $network
# Required-Stop:     $local_fs
# Default-Start:     2 3 4 5
# Default-Stop:      0 1 6
# Short-Description: Vizitor API
### END INIT INFO
PIDFILE=$PID_FILE
LOGFILE=$LOG_FILE
APP=$APP_HOME/api/server.py
CONF=$CONFIG_FILE
case "$1" in
  start)
    if [ -f "$PIDFILE" ] && kill -0 "$(cat "$PIDFILE")" 2>/dev/null; then
      echo "vizitor already running"; exit 0
    fi
    mkdir -p "$DATA_DIR"
    nohup python3 "$APP" --config "$CONF" >> "$LOGFILE" 2>&1 &
    echo $! > "$PIDFILE"
    echo "vizitor started"
    ;;
  stop)
    if [ -f "$PIDFILE" ]; then
      kill "$(cat "$PIDFILE")" 2>/dev/null || true
      rm -f "$PIDFILE"
      echo "vizitor stopped"
    else
      echo "vizitor not running"
    fi
    ;;
  restart)
    "$0" stop; sleep 1; "$0" start
    ;;
  status)
    if [ -f "$PIDFILE" ] && kill -0 "$(cat "$PIDFILE")" 2>/dev/null; then
      echo "vizitor running (pid $(cat "$PIDFILE"))"
    else
      echo "vizitor stopped"
      exit 3
    fi
    ;;
  *)
    echo "Usage: $0 {start|stop|restart|status}"; exit 1
    ;;
esac
EOF
    chmod +x "$INIT_FILE"
    command -v update-rc.d >/dev/null 2>&1 && update-rc.d vizitor defaults >/dev/null 2>&1 || true
    ok "اسکریپت init.d ساخته شد: $INIT_FILE"
  fi
}

svc_start_direct() {
  [ -f "$PID_FILE" ] && kill "$(cat "$PID_FILE")" 2>/dev/null || true
  rm -f "$PID_FILE"
  sleep 1
  mkdir -p "$DATA_DIR"
  nohup python3 "$APP_HOME/api/server.py" --config "$CONFIG_FILE" >> "$LOG_FILE" 2>&1 &
  echo $! > "$PID_FILE"
}

svc_start() {
  if [ "$HAS_SYSTEMD" -eq 1 ] && systemctl start vizitor >/dev/null 2>&1; then
    return 0
  fi
  # جایگزین هوشمند: init.d یا اجرای مستقیم
  if [ -f "$INIT_FILE" ]; then
    sh "$INIT_FILE" start >/dev/null 2>&1
  else
    svc_start_direct
  fi
  # اگر با هیچ‌کدام بالا نیامد، دوباره مستقیم امتحان کن
  sleep 2
  if [ "$(port_up "$PORT")" != "up" ]; then
    svc_start_direct
  fi
}

svc_stop() {
  if [ "$HAS_SYSTEMD" -eq 1 ]; then
    systemctl stop vizitor >/dev/null 2>&1 || true
  fi
  if [ -f "$PID_FILE" ]; then
    kill "$(cat "$PID_FILE")" 2>/dev/null || true
    rm -f "$PID_FILE"
  fi
}

svc_restart() {
  svc_stop
  sleep 1
  svc_start
}

seed_db() {
  python3 "$APP_HOME/api/seed.py" --config "$CONFIG_FILE" >/dev/null 2>&1
}

if [ "$MODE" != "keep" ]; then
  install_service_files
fi

# ---------------------------- فایروال / SELinux (بهتر اگر) -------------------
step "بررسی فایروال و SELinux"
if [ "$HAS_SYSTEMD" -eq 1 ] && systemctl is-active firewalld >/dev/null 2>&1; then
  if command -v firewall-cmd >/dev/null 2>&1; then
    if [ "$PORT" != "80" ] && ! firewall-cmd --query-port="$PORT/tcp" >/dev/null 2>&1; then
      firewall-cmd --permanent --add-port="$PORT/tcp" >/dev/null 2>&1 && firewall-cmd --reload >/dev/null 2>&1
      ok "پورت $PORT/tcp به firewalld اضافه شد"
    fi
  fi
elif command -v ufw >/dev/null 2>&1 && ufw status 2>/dev/null | grep -q "Status: active"; then
  if [ "$PORT" != "80" ]; then
    ufw allow "$PORT/tcp" >/dev/null 2>&1 && ok "پورت $PORT/tcp به ufw اضافه شد" || warn "افزودن پورت به ufw ممکن نشد"
  fi
else
  info "فایروال فعالی یافت نشد"
fi

if command -v getenforce >/dev/null 2>&1 && [ "$(getenforce 2>/dev/null)" = "Enforcing" ]; then
  if command -v semanage >/dev/null 2>&1 && [ "$PORT" != "80" ]; then
    semanage port -l 2>/dev/null | grep -qw "$PORT" || semanage port -a -t http_port_t -p tcp "$PORT" >/dev/null 2>&1
    ok "SELinux: پورت $PORT برای HTTP مجاز شد"
  fi
else
  info "SELinux در حالت Enforcing نیست"
fi

# ---------------------------- راه‌اندازی + بازرسی --------------------------
step "راه‌اندازی سرویس API"

# در حالت keep، مقادیر را از فایل تنظیمات بخوان (برای چک‌ها و تعمیرات)
if [ "$MODE" = "keep" ]; then
  eval "$(python3 - "$CONFIG_FILE" <<'PY'
import json, sys

def esc(v):
    s = str(v)
    for ch in ("\\", '"', "$", "`"):
        s = s.replace(ch, "\\" + ch)
    return s

c = json.load(open(sys.argv[1], encoding="utf-8"))
api = c.get("api", {}); db = c.get("db", {}); ac = c.get("activation", {})
print('BIND_IP="%s"' % esc(api.get("bind_ip", "0.0.0.0")))
print('PORT="%s"' % esc(api.get("port", 9595)))
print('API_URL="%s"' % esc(api.get("url", "")))
print('DB_ENGINE="%s"' % esc(db.get("engine", "sqlite")))
print('DB_HOST="%s"' % esc(db.get("host", "127.0.0.1")))
print('DB_DPORT="%s"' % esc(db.get("port", 3306)))
print('DB_NAME="%s"' % esc(db.get("name", "vizitor")))
print('DB_USER="%s"' % esc(db.get("user", "vizitor")))
print('DB_PASS="%s"' % esc(db.get("password", "")))
print('ADMIN_USER="%s"' % esc(c.get("admin", {}).get("username", "")))
print('ADMIN_PASS="%s"' % esc(c.get("admin", {}).get("password", "")))
print('ACT_CODE="%s"' % esc(ac.get("code", "")))
PY
)"
fi

# اگر mysql انتخاب شد، دیتابیس آماده باشد
if [ "$DB_ENGINE" = "mysql" ] && [ "$MODE" != "keep" ]; then
  if ! setup_mysql_db; then
    warn "راه‌اندازی MySQL ممکن نشد — به‌صورت هوشمند به sqlite تغییر می‌دهم"
    DB_ENGINE="sqlite"
    write_config >/dev/null
    ok "config.json به sqlite به‌روزرسانی شد"
  fi
fi

# در حالت keep، فایل سرویس اگر نباشد بساز
[ -f "$UNIT_FILE" ] || [ -f "$INIT_FILE" ] || install_service_files

# درج اولیه داده‌ها (حساب ادمین + کد فعال‌سازی + تنظیمات پایه) — ای‌دی‌ام‌پتنت
seed_db

svc_restart
sleep 2

# ---- حلقه چک و تعمیر -------------------------------------------------------
step "بازرسی کامل و بازبینی خودکار تنظیمات (تا ۴ دور)"

CHECK_FAILURES=()

run_checks() {
  CHECK_FAILURES=()
  local base="http://127.0.0.1:$PORT"
  local r

  # ۱) سرویس در حال listen باشد
  if [ "$(port_up "$PORT")" != "up" ]; then
    CHECK_FAILURES+=("process")
    return 1
  fi

  # ۲) ping
  r="$(http_get "$base/api/ping")"
  if ! printf '%s' "$r" | grep -q '"status": *"ok"'; then
    CHECK_FAILURES+=("ping")
    return 1
  fi

  # ۳) api/config باید آدرس درست را برمی‌گرداند
  r="$(http_get "$base/api/config")"
  if ! printf '%s' "$r" | grep -qF "$API_URL"; then
    CHECK_FAILURES+=("config")
  fi

  # ۴) دیتابیس
  r="$(http_get "$base/api/health")"
  if printf '%s' "$r" | grep -q '"status": *"error"'; then
    CHECK_FAILURES+=("db")
  fi

  # ۵) وضعیت فعال‌سازی
  local expect_act="false"
  [ -n "$ACT_CODE" ] && expect_act="true"
  if ! printf '%s' "$r" | grep -q "\"activated\": *$expect_act"; then
    CHECK_FAILURES+=("activation")
  fi

  # ۶) لاگین ادمین
  r="$(http_post "$base/api/login" "{\"username\":\"$ADMIN_USER\",\"password\":\"$ADMIN_PASS\"}")"
  if ! printf '%s' "$r" | grep -q '"ok": *true'; then
    CHECK_FAILURES+=("login")
  fi

  # ۷) قابل‌دستی‌بودن از بیرون (فقط هشدار)
  if [ -n "$PUBLIC_IP" ] && [ "$PUBLIC_IP" != "$PRIVATE_IP" ]; then
    local ext_base="$PROTO://$ADDR"
    [ "$PORT" != "80" ] && [ "$PORT" != "443" ] && ext_base="$ext_base:$PORT"
    if ! http_get "$ext_base/api/ping" >/dev/null 2>&1; then
      warn "آدرس خارجی $ext_base/api در این لحظه پاسخ نداد (ممکن است فایروال ابر، پورت‌فوروردینگ یا DNS مشکل داشته باشد — اتصال داخلی درست است)"
    else
      ok "آدرس خارجی برای اینترنت: $ext_base/api"
    fi
  fi
  return 0
}

repair() {
  local f
  for f in "${CHECK_FAILURES[@]}"; do
    case "$f" in
      process|ping)
        info "تعمیر: شروع/راه‌اندازی مجدد سرویس ..."
        svc_restart
        sleep 3
        if [ "$(port_up "$PORT")" != "up" ]; then
          # پورت درگیر است؛ چند پورت جایگزین هوشمند امتحان کن
          for TRY_PORT in 8090 8180 8280 8081 8082; do
            if [ "$(port_free "$TRY_PORT")" = "free" ]; then
              info "پورت $PORT درگیر است؛ به‌صورت هوشمند پورت $TRY_PORT انتخاب می‌شود"
              PORT="$TRY_PORT"
              API_URL="$PROTO://$ADDR"
              [ "$PORT" != "80" ] && [ "$PORT" != "443" ] && API_URL="$API_URL:$PORT"
              API_URL="$API_URL/api"
              write_config >/dev/null
              svc_restart
              sleep 3
              break
            fi
          done
        fi
        ;;
      config)
        info "تعمیر: بازنویسی config.json و راه‌اندازی مجدد ..."
        write_config >/dev/null
        seed_db
        svc_restart
        sleep 3
        ;;
      db)
        info "تعمیر: بازبینی دیتابیس ..."
        if [ "$DB_ENGINE" = "mysql" ]; then
          start_mariadb >/dev/null 2>&1 || true
          wait_mariadb || true
          mysql "$DB_NAME" < "$APP_HOME/database/schema_mysql.sql" >/dev/null 2>&1 || true
        fi
        seed_db
        svc_restart
        sleep 3
        ;;
      activation|login)
        info "تعمیر: دوباره درج حساب ادمین و کد فعال‌سازی ..."
        seed_db
        svc_restart
        sleep 3
        ;;
    esac
  done
}

VERIFY_OK=0
for ROUND in 1 2 3 4; do
  run_checks
  NFAIL=${#CHECK_FAILURES[@]}
  if [ "$NFAIL" -eq 0 ]; then
    VERIFY_OK=1
    ok "بازرسی دور $ROUND: همه موارد، موفق ✓"
    break
  fi
  warn "بازرسی دور $ROUND: $NFAIL مورد ناموفق (${CHECK_FAILURES[*]}) — تنظیمات بازبینی و تعمیر می‌شوند ..."
  repair
done

# ---------------------------- گزارش نهایی ---------------------------------
step "گزارش نهایی"

# محدود کردن عرض خروجی
hr() { printf '%s\n' "------------------------------------------------------------"; }

if [ "$VERIFY_OK" -eq 1 ]; then
  printf '\n%s%s  ✓  نصب Vizitor با موفقیت انجام شد و همه بازرسی‌ها پاس شدند%s\n\n' "$C_BOLD" "$C_OK" "$C_END"
else
  printf '\n%s%s  !!  نصب انجام شد اما برخی بازرسی‌ها هنوز ناموفق‌اند%s\n' "$C_BOLD" "$C_ERR" "$C_END"
  info "فهرست مشکلات: ${CHECK_FAILURES[*]:-بررسی کنید}"
  info "لاگ سرویس: $LOG_FILE"
  info "برای بازرسی مجدد:  sudo bash $0 --recheck"
  printf '\n'
fi

hr
printf '  %sآدرس API برای برنامه اندروید:%s\n' "$C_BOLD" "$C_END"
printf '     %s%s%s\n' "$C_OK" "$API_URL" "$C_END"
printf '  %sآدرس دریافت خودکار تنظیمات (برای اپ):%s\n' "$C_BOLD" "$C_END"
printf '     %s%s%s\n' "$C_INFO" "$(echo "$API_URL" | sed 's#/api$#/api/config#')" "$C_END"
hr
printf '  دیتابیس      : %s' "$DB_ENGINE"
if [ "$DB_ENGINE" = "mysql" ]; then
  printf '  (%s @ %s:%s ، کاربر: %s)' "$DB_NAME" "$DB_HOST" "$DB_DPORT" "$DB_USER"
else
  printf '  (%s)' "$DATA_DIR/vizitor.db"
fi
printf '\n'
printf '  حساب ادمین  : %s / %s\n' "$ADMIN_USER" "$ADMIN_PASS"
if [ -n "$ACT_CODE" ]; then
  printf '  کد فعال‌سازی: %s (فعال)\n' "$ACT_CODE"
else
  printf '  کد فعال‌سازی: %sخالی است — از برنامه اندروید وارد کنید یا:%s\n' "$C_WARN" "$C_END"
  printf '     curl -X POST %s/activate -d '"'"'{"code":"CODE-شما"}'"'"'\n' "$API_URL"
fi
hr
printf '  فایل تنظیمات : %s\n' "$CONFIG_FILE"
printf '  لاگ سرویس   : %s\n' "$LOG_FILE"
printf '  فایل‌های برنامه: %s\n' "$APP_HOME"
printf '  مدیریت سرویس : service vizitor {start|stop|restart|status}'
[ "$HAS_SYSTEMD" -eq 1 ] && printf '  (یا: systemctl status vizitor)'
printf '\n'
hr
printf '  %sراهنمای اندروید:%s در بخش اتصال API برنامه، آدرس %s%s%s را وارد کنید.\n' "$C_BOLD" "$C_END" "$C_BOLD" "$API_URL" "$C_END"
if [ -z "$PUBLIC_IP" ]; then
  printf '  %s⚠ آی‌پی عمومی شناسایی نشد؛ %s داخل شبکه محلی کار می‌کند. برای دسترسی از بیرون،\n     یک دامنه یا IP ثابت + پورت‌فورورد روی %s تنظیم کنید.%s\n' "$C_WARN" "$API_URL" "$PORT" "$C_END"
fi
printf '%s\n\n' "$C_END"

if [ "$VERIFY_OK" -eq 1 ]; then
  exit 0
fi
exit 1
