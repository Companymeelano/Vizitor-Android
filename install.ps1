<#
.SYNOPSIS
  Vizitor — نصب‌کننده هوشمند سرور برای ویندوز (Smart Windows Installer)

.DESCRIPTION
  این اسکریپت:
   1. ویندوز، Python و سرویس SQL Server را تشخیص می‌دهد
   2. پیش‌نیازهای کم (python3، pyodbc، درایور ODBC) را با هوش نصب می‌کند
   3. آی‌پی سرور (عمومی + داخلی) را شناسایی و آدرس API را بر اساس آن می‌سازد
   4. از کاربر می‌پرسد: آی‌پی/دامنه، پورت، مشخصات SQL Server (هاست، پورت،
      روش احراز، کاربر، گذرواژه، نام دیتابیس)، حساب ادمین و کد فعال‌سازی
   5. دیتابیس SQL Server را به‌صورت کاملاً غیرتلفیقی آماده می‌کند
      (فقط CREATE با IF-NOT-EXISTS؛ هیچ DROP/ALTER روی اشیاء موجود)
   6. فایل‌ها را منتشر می‌کند، config.json می‌سازد و سرویس (Task) را راه‌اندازی می‌کند
   7. همه‌چیز را چک می‌کند (API، دیتابیس، فعال‌سازی، لاگین) و در صورت مشکل
      تنظیمات را بازبینی/تعمیر کرده و دوباره چک می‌کند (تا ۴ دور)
   8. در پایان آدرس دقیق API برای برنامه اندروید را نمایش می‌دهد

.EXAMPLE
  powershell -ExecutionPolicy Bypass -File install.ps1
  powershell -ExecutionPolicy Bypass -File install.ps1 -Auto
  powershell -ExecutionPolicy Bypass -File install.ps1 -Recheck
  powershell -ExecutionPolicy Bypass -File install.ps1 -Port 9000 -Ip 1.2.3.4
#>
[CmdletBinding()]
param(
    [switch]$Auto,
    [switch]$Recheck,
    [int]$Port = 0,
    [string]$Ip = "",
    # --- پارامترهای نصب‌کنندهٔ گرافیکی (Vizitor-Setup.exe) --------------------
    [string]$Answers = "",            # فایل INI پاسخ‌ها (صفحه‌های نصب‌کننده)
    [string]$AppHome = "",            # پوشهٔ نصب (پیش‌فرض C:\Vizitor)
    [switch]$SkipPrerequisites,       # بخش «پیش‌نیازهای پایتون» تیک نخورده
    [switch]$SkipDatabase,            # بخش «دیتابیس سامانه» تیک نخورده
    [switch]$SkipFirewall,            # بخش «فایروال» تیک نخورده
    [switch]$SkipAndroidPrep,         # بخش «اتصال مستقیم اندروید» تیک نخورده
    [switch]$SkipSelfCheck,           # بخش «بازرسی و تعمیر» تیک نخورده
    [switch]$AndroidPrepOnly,         # فقط آماده‌سازی اتصال مستقیم اندروید
    [switch]$NoSqlRestart             # در اجرای خودکار، سرویس SQL را ری‌استارت نکن
)

$ErrorActionPreference = "Stop"
try {
    [Console]::OutputEncoding = [System.Text.Encoding]::UTF8
    [Console]::InputEncoding  = [System.Text.Encoding]::UTF8
} catch {}

# ---------------------------- مسیرهای ثابت ------------------------------
$script:AppHome    = "C:\Vizitor"
$script:DataDir    = "C:\ProgramData\Vizitor"
$script:ConfigFile = Join-Path $script:DataDir "config.json"
$script:LogFile    = Join-Path $script:DataDir "vizitor.log"
$script:PidFile    = Join-Path $script:DataDir "vizitor.pid"
$script:TaskName   = "VizitorAPI"
$script:BatFile    = Join-Path $script:AppHome "run.bat"
$script:ScriptDir  = $PSScriptRoot

# ---- پاسخ‌های نصب‌کنندهٔ گرافیکی (فایل INI) --------------------------------
# Vizitor-Setup.exe پاسخ صفحه‌ها را در یک فایل INI می‌نویسد و با -Answers
# می‌دهد. در این حالت هیچ پرسشی از کاربر پرسیده نمی‌شود و مقادیر همان فایل
# استفاده می‌شوند. هیچ‌کدام از این مقادیر چاپ نمی‌شوند (رمز SQL هرگز).
$script:Unattended     = $false
$script:Ans            = @{}
$script:ErpDb          = ""          # هیچ دیتابیسی اجباری نیست؛ کاربر از لیست انتخاب می‌کند
$script:AndroidLogin   = "vizitor_android"
$script:AndroidFirewall = $true
$script:AndroidExternal = $false     # اجازهٔ اتصال از بیرون شبکه (آی‌پی اختصاصی / فوروارد پورت)
$script:AndroidOk      = $false
$script:HealthWanted   = $true      # تیک «بررسی سلامت اتصال» در نصب‌کننده

function Get-Answer([string]$Key, [string]$Default = "") {
    if ($script:Ans.ContainsKey($Key)) {
        $v = [string]$script:Ans[$Key]
        if ($v -ne "") { return $v }
    }
    return $Default
}

if ($Answers) {
    $script:Unattended = $true
    if (Test-Path $Answers) {
        # فایل پاسخ‌ها را می‌تواند NSIS به‌صورت ANSI یا UTF-16 بنویسد؛ هر دو
        # حالت را از روی بایت‌های اول تشخیص می‌دهیم تا مقادیر درست خوانده شوند.
        $sec = ""
        $ansText = ""
        try {
            $bytes = [System.IO.File]::ReadAllBytes($Answers)
            if ($bytes.Length -ge 2 -and $bytes[0] -eq 0xFF -and $bytes[1] -eq 0xFE) {
                $ansText = [System.Text.Encoding]::Unicode.GetString($bytes, 2, $bytes.Length - 2)
            } elseif ($bytes.Length -ge 3 -and $bytes[0] -eq 0xEF -and $bytes[1] -eq 0xBB -and $bytes[2] -eq 0xBF) {
                $ansText = [System.Text.Encoding]::UTF8.GetString($bytes, 3, $bytes.Length - 3)
            } else {
                $ansText = [System.Text.Encoding]::UTF8.GetString($bytes)
                if ($ansText.IndexOf([char]0xFFFD) -ge 0) { $ansText = [System.Text.Encoding]::Default.GetString($bytes) }
            }
        } catch {
            $ansText = (Get-Content $Answers | Out-String)
        }
        foreach ($line in ($ansText -split "`r?`n")) {
            $t = ([string]$line).Trim()
            if ($t -eq "" -or $t.StartsWith(";") -or $t.StartsWith("#")) { continue }
            if ($t.StartsWith("[") -and $t.EndsWith("]")) { $sec = $t.Substring(1, $t.Length - 2).ToLower(); continue }
            $i = $t.IndexOf("=")
            if ($i -lt 1) { continue }
            $k = $t.Substring(0, $i).Trim().ToLower()
            $v = $t.Substring($i + 1).Trim()
            $script:Ans["$sec/$k"] = $v
        }
        Write-Info "پاسخ‌های نصب‌کنندهٔ گرافیکی خوانده شد ($($script:Ans.Count) مقدار)"
    } else {
        Write-Warn "فایل پاسخ‌ها پیدا نشد: $Answers — مقادیر پیش‌فرض استفاده می‌شوند"
    }
}

# ---- پوشهٔ نصب متفاوت (اگر نصب‌کنندهٔ گرافیکی پوشهٔ دیگری انتخاب کرده باشد) ---
if ($AppHome) {
    $script:AppHome = $AppHome
    $script:BatFile = Join-Path $script:AppHome "run.bat"
}

# -AndroidPrepOnly فقط بخش «اتصال مستقیم اندروید» را اجرا می‌کند:
# مثل -Recheck از تنظیمات موجود می‌خواند و پرسشی نمی‌پرسد.
if ($AndroidPrepOnly) {
    $Recheck = $true
    $script:Unattended = $true
}

# ---------------------------- رنگ‌ها و نمایش ----------------------------
$script:ColorsOk = $true
function Write-Ok([string]$Msg)   { if ($script:ColorsOk) { Write-Host "[ OK ] $Msg" -ForegroundColor Green } else { Write-Host "[ OK ] $Msg" } }
function Write-Warn([string]$Msg) { if ($script:ColorsOk) { Write-Host "[!!] $Msg" -ForegroundColor Yellow } else { Write-Host "[!!] $Msg" } }
function Write-Err([string]$Msg)  { if ($script:ColorsOk) { Write-Host "[ERR] $Msg" -ForegroundColor Red } else { Write-Host "[ERR] $Msg" } }
function Write-Info([string]$Msg) { Write-Host "     $Msg" }
$script:StepN = 0
function Write-Step([string]$Msg) {
    $script:StepN = $script:StepN + 1
    Write-Host ""
    Write-Host "==>  گام ${script:StepN}: $Msg" -ForegroundColor Cyan
}
function Write-Banner {
    Write-Host "============================================================" -ForegroundColor White
    Write-Host "   VIZITOR  —  نصب‌کننده هوشمند سرور (ویندوز)" -ForegroundColor White
    Write-Host "   (Smart Server Installer — Windows)" -ForegroundColor White
    Write-Host "============================================================" -ForegroundColor White
}

# ---------------------------- ادمین بودن --------------------------------
Write-Banner
$isAdmin = ([Security.Principal.WindowsPrincipal][Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)
if (-not $isAdmin) {
    Write-Info "اجرا با دسترسی Administrator نیاز دارد؛ دوباره با دسترسی بالا اجرا می‌شود ..."
    $flags = @()
    if ($Auto)    { $flags += "--Auto" }
    if ($Recheck) { $flags += "--Recheck" }
    if ($Port)    { $flags += "--Port `"$Port`"" }
    if ($Ip)      { $flags += "--Ip `"$Ip`"" }
    $arg = "-NoProfile -ExecutionPolicy Bypass -File `"$PSScriptRoot\install.ps1`" " + ($flags -join " ")
    Start-Process -FilePath "powershell.exe" -Verb RunAs -ArgumentList $arg
    exit 0
}

# ---------------------------- توابع کمکی ---------------------------------
function Get-OrDefault($Value, $Default) {
    if ($null -eq $Value) { return $Default }
    $s = [string]$Value
    if ([string]::IsNullOrWhiteSpace($s)) { return $Default }
    return $s
}

function New-RandomPassword([int]$Len = 16) {
    $chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789"
    $sb = New-Object System.Text.StringBuilder
    $rng = New-Object System.Random
    for ($i = 0; $i -lt $Len; $i++) { [void]$sb.Append($chars[$rng.Next($chars.Length)]) }
    return $sb.ToString()
}

# کد راه‌اندازی اپ اندروید (۸ کاراکتر A-Z و ۰-۹) — روی کارت اتصال/QR می‌آید
function New-SetupToken([int]$Len = 8) {
    $chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    $sb = New-Object System.Text.StringBuilder
    $rng = New-Object System.Security.Cryptography.RNGCryptoServiceProvider
    $buf = New-Object byte[] 1
    for ($i = 0; $i -lt $Len; $i++) {
        $rng.GetBytes($buf)
        [void]$sb.Append($chars[$buf[0] % $chars.Length])
    }
    return $sb.ToString()
}

# پرسش از کاربر با مقدار پیش‌فرض؛ در حالت -Auto یا ورودی بسته، پیش‌فرض برگردانده می‌شود
function Read-Prompt([string]$Prompt, [string]$Default = "") {
    if ($script:Unattended) {
        Write-Info "$Prompt [$Default]"
        return $Default
    }
    if ($Auto) {
        if ($Default) { Write-Info "$Prompt [$Default]" } else { Write-Info $Prompt }
        return $Default
    }
    try {
        if ($Default) { $ans = Read-Host "$Prompt [$Default]" } else { $ans = Read-Host $Prompt }
    } catch { return $Default }
    if ([string]::IsNullOrWhiteSpace($ans)) { return $Default }
    return $ans.Trim()
}

# پرسش راز (پسورد)
function Read-SecretPrompt([string]$Prompt, [string]$Default = "") {
    if ($script:Unattended) {
        Write-Info "$Prompt [*** مقدار از نصب‌کننده ***]"
        return $Default
    }
    if ($Auto) {
        Write-Info "$Prompt [*** پیش‌فرض (تولیدشده) ***]"
        return $Default
    }
    try {
        $secure = Read-Host $Prompt -AsSecureString
        $bstr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
        $plain = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($bstr)
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($bstr)
        if ([string]::IsNullOrEmpty($plain)) { return $Default }
        return $plain
    } catch { return $Default }
}

# آیا چیزی روی پورت در حال listen است؟
function Test-PortUp([int]$PortNum) {
    $client = New-Object System.Net.Sockets.TcpClient
    try {
        $result = $client.BeginConnect("127.0.0.1", $PortNum, $null, $null)
        if (-not $result.AsyncWaitHandle.WaitOne(3000, $false)) { return $false }
        $client.EndConnect($result)
        return $true
    } catch { return $false }
    finally { $client.Close() }
}

# آیا پورت آزاد است؟
function Test-PortFree([int]$PortNum) {
    $listener = New-Object System.Net.Sockets.TcpListener([System.Net.IPAddress]::Any, $PortNum)
    try { $listener.Start(); return $true }
    catch { return $false }
    finally { try { $listener.Stop() } catch {} }
}

function Invoke-JsonGet([string]$Url) {
    try { return (Invoke-WebRequest -UseBasicParsing -TimeoutSec 10 -Uri $Url).Content }
    catch { return $null }
}
function Invoke-JsonPost([string]$Url, [string]$Body) {
    try { return (Invoke-WebRequest -UseBasicParsing -TimeoutSec 10 -Method Post -ContentType "application/json; charset=utf-8" -Body ([System.Text.Encoding]::UTF8.GetBytes($Body)) -Uri $Url).Content }
    catch { return $null }
}

# ---------------------------- گام ۱: سیستم و ابزارها ----------------------
Write-Step "بررسی سیستم‌عامل و ابزارها"
$os = Get-CimInstance Win32_OperatingSystem
Write-Ok "سیستم‌عامل: $($os.Caption.Trim())"

function Find-Python {
    foreach ($n in @("python", "py", "python3")) {
        $cmd = Get-Command $n -ErrorAction SilentlyContinue
        if ($cmd -and $cmd.Source) {
            try {
                $ver = & $cmd.Source --version 2>&1 | Out-String
                if ($ver -match 'Python\s+(\d+)\.(\d+)') {
                    if ([int]$Matches[1] -ge 3) { return $cmd.Source }
                }
            } catch {}
        }
    }
    return $null
}

$script:PyExe = Find-Python
if (-not $script:PyExe) {
    Write-Warn "python3 یافت نشد؛ تلاش برای نصب با winget ..."
    $winget = Get-Command winget -ErrorAction SilentlyContinue
    if ($winget) {
        try { & winget install -e --id Python.Python.3.11 --accept-source-agreements --accept-package-agreements 2>$null | Out-Null } catch {}
        $env:Path = [System.Environment]::GetEnvironmentVariable("Path", "Machine") + ";" + [System.Environment]::GetEnvironmentVariable("Path", "User")
        $script:PyExe = Find-Python
    }
}
if (-not $script:PyExe) {
    Write-Err "python3 در دسترس نیست و نصبش ممکن نشد. API روی python3 اجرا می‌شود؛ لطفاً Python 3.8+ را از python.org نصب کنید و دوباره اجرا کنید."
    exit 1
}
$pyVer = & $script:PyExe --version 2>&1 | Out-String
Write-Ok "python3 ($($pyVer.Trim())) در: $script:PyExe"

# ---------------------------- گام ۲: تشخیص آی‌پی سرور ----------------------
Write-Step "تشخیص آی‌پی سرور"

function Get-PublicIp {
    $urls = @("https://api.ipify.org", "https://ifconfig.me/ip", "http://checkip.amazonaws.com", "https://icanhazip.com", "https://ip.sb")
    foreach ($u in $urls) {
        try {
            $r = (Invoke-WebRequest -UseBasicParsing -TimeoutSec 5 -Uri $u).Content.Trim()
            if ($r -match '^\d{1,3}(\.\d{1,3}){3}$') { return $r }
        } catch {}
    }
    return ""
}
function Get-LocalIp {
    try {
        $ip = (Get-NetIPAddress -AddressFamily IPv4 -ErrorAction Stop |
            Where-Object { $_.IPAddress -notlike "169.254.*" -and $_.IPAddress -notlike "127.*" } |
            Select-Object -First 1).IPAddress
        if ($ip) { return $ip }
    } catch {}
    $out = ipconfig 2>$null
    foreach ($line in $out) {
        if ($line -match 'IPv4[^:]*:\s*(\d+\.\d+\.\d+\.\d+)') {
            $m = $Matches[1]
            if ($m -notlike "169.254.*" -and $m -notlike "127.*") { return $m }
        }
    }
    return ""
}

$script:PublicIp = Get-PublicIp
$script:LocalIp  = Get-LocalIp
if ($script:PublicIp) { Write-Ok "آی‌پی عمومی (Public IP): $script:PublicIp" }
else { Write-Warn "آی‌پی عمومی شناسایی نشد (اتصال به بیرون ممکن نیست) — از آی‌پی داخلی استفاده می‌شود" }
if ($script:LocalIp)  { Write-Ok "آی‌پی داخلی (Local IP): $script:LocalIp" }
if (-not $script:PublicIp -and -not $script:LocalIp) {
    Write-Err "هیچ آی‌پی‌ای شناسایی نشد! نصب بدون آدرس ممکن نیست."
    exit 1
}
if (-not $script:PublicIp -and $Ip) { Write-Warn "آدرس توسط کاربر اجبار شد: $Ip" }

# ---------------------------- نصب موجود؟ --------------------------------
$script:Existing = Test-Path $script:ConfigFile
$script:Mode = "install"   # install | keep | reconfigure
if ($script:Existing -and -not $Recheck) {
    if (-not $Auto) {
        Write-Host ""
        Write-Host "نصب قبلی Vizitor یافت شد ($script:ConfigFile)"
        Write-Host "  [1] ادامه با تنظیمات فعلی (فقط بازرسی و تعمیر) — پیشنهادی"
        Write-Host "  [2] بازپیکربندی (دوباره از من می‌پرسد)"
        Write-Host "  [3] خروج"
        $pick = "1"
        try { $pick = Read-Host "انتخاب [1]" } catch {}
        if ($pick -eq "2") { $script:Mode = "reconfigure" }
        elseif ($pick -eq "3") { Write-Info "خروج."; exit 0 }
        else { $script:Mode = "keep" }
    }
    else {
        $script:Mode = "keep"
        Write-Info "نصب قبلی یافت شد؛ در حالت -Auto تنظیمات فعلی حفظ و فقط بازرسی/تعمیر انجام می‌شود"
    }
}
if ($Recheck) { $script:Mode = "keep" }

# ---------------------------- خواندن مقادیر قبلی ---------------------------
$script:PrevAddr = ""; $script:PrevPort = ""
$script:PrevDbEngine = ""; $script:PrevDbHost = ""; $script:PrevDbDPort = ""
$script:PrevDbName = ""; $script:PrevDbUser = ""; $script:PrevDbPass = ""; $script:PrevDbAuth = ""
$script:PrevAdminUser = ""; $script:PrevAdminPass = ""; $script:PrevActCode = ""
if ($script:Existing) {
    try {
        $c = Get-Content $script:ConfigFile -Raw -Encoding UTF8 | ConvertFrom-Json
        $url = [string]$c.api.url
        if ($url -match '^[a-z]+://([^:/]+)(?::(\d+))?') {
            $script:PrevAddr = $Matches[1]
            if ($Matches[2]) { $script:PrevPort = $Matches[2] }
        }
        $script:PrevDbEngine  = [string]$c.db.engine
        $script:PrevDbHost    = [string]$c.db.host
        $script:PrevDbDPort   = [string]$c.db.port
        $script:PrevDbName    = [string]$c.db.name
        $script:PrevDbUser    = [string]$c.db.user
        $script:PrevDbPass    = [string]$c.db.password
        $script:PrevDbAuth    = [string]$c.db.auth
        $script:PrevAdminUser = [string]$c.admin.username
        $script:PrevAdminPass = [string]$c.admin.password
        $script:PrevActCode   = [string]$c.activation.code
    } catch {
        Write-Warn "خواندن config قبلی ممکن نشد؛ مقادیر پیش‌فرض استفاده می‌شوند"
    }
}

# ---------------------------- گام ۳: سؤالات تنظیمات ----------------------
Write-Step "دریافت اطلاعات ضروری از شما"

# مقادیر نصب‌کنندهٔ گرافیکی جای پیش‌فرض پرسش‌ها می‌نشینند (در حالت -Answers
# پرسشی پرسیده نمی‌شود و همین مقادیر استفاده می‌شوند).
$Ip = Get-Answer "server/addr" $Ip
if ([int]$Port -eq 0) { $Port = [int](Get-Answer "server/port" "0") }
if (-not $script:PrevAddr)      { $script:PrevAddr      = Get-Answer "server/addr" "" }
if (-not $script:PrevDbEngine)  { $script:PrevDbEngine  = Get-Answer "db/engine" "" }
if (-not $script:PrevDbHost)    { $script:PrevDbHost    = Get-Answer "db/host" "" }
if (-not $script:PrevDbDPort)   { $script:PrevDbDPort   = Get-Answer "db/port" "" }
if (-not $script:PrevDbAuth)    { $script:PrevDbAuth    = Get-Answer "db/auth" "" }
if (-not $script:PrevDbUser)    { $script:PrevDbUser    = Get-Answer "db/user" "" }
if (-not $script:PrevDbPass)    { $script:PrevDbPass    = Get-Answer "db/password" "" }
if (-not $script:PrevDbName)    { $script:PrevDbName    = Get-Answer "db/name" "" }
if (-not $script:PrevAdminUser) { $script:PrevAdminUser = Get-Answer "admin/username" "" }
if (-not $script:PrevAdminPass) { $script:PrevAdminPass = Get-Answer "admin/password" "" }
if (-not $script:PrevActCode)   { $script:PrevActCode   = Get-Answer "activation/code" "" }
$script:HealthWanted    = ((Get-Answer "db/health" "1") -ne "0")
$script:HostLan         = Get-Answer "server/lanip" ""
$script:HostPublic      = Get-Answer "server/publicip" ""
$script:ErpDb           = Get-Answer "android/erpdb" ""
$script:AndroidLogin    = Get-Answer "android/login" "vizitor_android"
$script:AndroidFirewall = ((Get-Answer "android/openfirewall" "1") -ne "0")
$script:AndroidExternal = ((Get-Answer "android/external" "0") -ne "0")

# آدرس پیش‌فرض هوشمند: اجبار کاربر > آدرس قبلی > IP عمومی > IP داخلی
if ($Ip) { $script:DefaultAddr = $Ip }
elseif ($script:PrevAddr) { $script:DefaultAddr = $script:PrevAddr }
elseif ($script:PublicIp) { $script:DefaultAddr = $script:PublicIp }
else { $script:DefaultAddr = $script:LocalIp }

$script:Addr = Read-Prompt "آدرس سرور برای اتصال برنامه اندروید (IP یا دامنه)" $script:DefaultAddr
if (-not $script:Addr) { $script:Addr = $script:DefaultAddr }

if ($script:Addr -match '^\d{1,3}(\.\d{1,3}){3}$') {
    $script:Proto = "http"
    Write-Info "پروتکل: http (برای IP ساده)"
} else {
    $p = Read-Prompt "پروتکل (http / https)" "http"
    if ($p -ne "https") { $p = "http" }
    $script:Proto = $p
}

if ($Port -gt 0) {
    $script:Port = $Port
    Write-Info "پورت (اجباری با -Port): $script:Port"
} else {
    $defaultPort = $script:PrevPort
    if (-not $defaultPort) {
        # پورت پیش‌فرض سامانه 9595 است؛ اگر اشغال بود پورت‌های نزدیک را امتحان می‌کنیم
        foreach ($p in @(9595, 9596, 9597, 9598, 9600)) {
            if (Test-PortFree $p) { $defaultPort = [string]$p; break }
        }
        if (-not $defaultPort) { $defaultPort = "9595" }
    }
    $portAns = Read-Prompt "پورت سامانه (پیش‌فرض 9595)" $defaultPort
    $parsedPort = 0
    [int]::TryParse($portAns, [ref]$parsedPort)
    if ($parsedPort -le 0) { $parsedPort = [int]$defaultPort }
    $script:Port = $parsedPort
}

# تشخیص هوشمند SQL Server
function Get-SqlServices { Get-Service -Name "MSSQLSERVER", "MSSQL$*" -ErrorAction SilentlyContinue }
function Test-SqlServerRunning { ((Get-SqlServices | Where-Object { $_.Status -eq "Running" }).Count -gt 0) }

# پروب TCP به پورت SQL Server (بدون نیاز به اعتبارنامه)
function Test-SqlServerReachable {
    $dbPortNum = 1433
    [int]::TryParse($script:DbDPort, [ref]$dbPortNum)
    if (-not $script:DbHost) { return $false }
    $client = New-Object System.Net.Sockets.TcpClient
    try {
        $result = $client.BeginConnect($script:DbHost, $dbPortNum, $null, $null)
        if (-not $result.AsyncWaitHandle.WaitOne(5000, $false)) { return $false }
        $client.EndConnect($result)
        return $true
    } catch { return $false }
    finally { $client.Close() }
}

# فهرست درایورهای ODBC از رجیستری ویندوز (بدون نیاز به pyodbc)
function Get-WindowsOdbcDrivers {
    $names = @()
    $keys = @(
        "HKLM:\SOFTWARE\ODBC\ODBCINST.INI\ODBC Drivers",
        "HKLM:\SOFTWARE\WOW6432Node\ODBC\ODBCINST.INI\ODBC Drivers"
    )
    foreach ($k in $keys) {
        try {
            $props = Get-ItemProperty -Path $k -ErrorAction SilentlyContinue
            if ($props) { $names += @($props.PSObject.Properties.Name | Where-Object { $_ -ne "PS*" }) }
        } catch {}
    }
    return @($names | Select-Object -Unique)
}

function Get-OdbcDrivers {
    $out = & $script:PyExe -c "import pyodbc; print('|'.join(pyodbc.drivers()))" 2>$null
    if ($LASTEXITCODE -ne 0) { return @() }
    return @($out -split '\|' | Where-Object { $_ })
}
function Test-OdbcDriverPresent {
    $drivers = Get-OdbcDrivers
    foreach ($p in @("ODBC Driver 18 for SQL Server", "ODBC Driver 17 for SQL Server", "ODBC Driver 13 for SQL Server", "SQL Server Native Client 11.0")) {
        if ($drivers -contains $p) { return $true }
    }
    return ($drivers.Count -gt 0)
}

# تشخیص هوشمند: سرویس SQL Server روی این ماشین + درایور ODBC موجود در ویندوز
$smartDb = "sqlite"
$hasLocalSql = ((Get-SqlServices).Count -gt 0 -and (Test-SqlServerRunning))
$hasOdbcDriver = ((Get-WindowsOdbcDrivers) -contains "ODBC Driver 18 for SQL Server") -or
                 ((Get-WindowsOdbcDrivers) -contains "ODBC Driver 17 for SQL Server") -or
                 ((Get-WindowsOdbcDrivers) -contains "ODBC Driver 13 for SQL Server") -or
                 ((Get-WindowsOdbcDrivers) -contains "SQL Server Native Client 11.0")
if ($hasLocalSql -and $hasOdbcDriver) { $smartDb = "sqlserver" }
$engineDefault = Get-OrDefault $script:PrevDbEngine $smartDb
$script:DbEngine = Read-Prompt "موتور دیتابیس (sqlserver / sqlite) — هوشمند: $smartDb" $engineDefault
if ($script:DbEngine -ne "sqlserver") { $script:DbEngine = "sqlite" }

$script:DbHost = "localhost"; $script:DbDPort = "1433"; $script:DbAuth = "sql"
$script:DbName = "vizitor";  $script:DbUser = "vizitor"; $script:DbPass = ""
$script:AdminUser = "admin"; $script:AdminPass = New-RandomPassword
$script:ActCode = ""

if ($script:DbEngine -eq "sqlserver") {
    $script:DbHost  = Read-Prompt "آدرس سرور SQL Server" (Get-OrDefault $script:PrevDbHost "localhost")
    $script:DbDPort = Read-Prompt "پورت SQL Server" (Get-OrDefault $script:PrevDbDPort "1433")
    $script:DbAuth  = Read-Prompt "روش احراز SQL Server (sql = کاربر/گذرواژه، windows = احراز ویندوز)" (Get-OrDefault $script:PrevDbAuth "sql")
    if ($script:DbAuth -ne "windows") { $script:DbAuth = "sql" }
    if ($script:DbAuth -ne "windows") {
        $script:DbUser = Read-Prompt "نام کاربری SQL Server (پیش‌فرض: sa)" (Get-OrDefault $script:PrevDbUser "sa")
        $genDbPass = New-RandomPassword
        $script:DbPass = Read-SecretPrompt "گذرواژه SQL Server (خالی بگذارید = تولید خودکار: $genDbPass)" (Get-OrDefault $script:PrevDbPass $genDbPass)
        if (-not $script:DbPass) { $script:DbPass = $genDbPass }
    }
    $script:DbName = Read-Prompt "نام دیتابیس" (Get-OrDefault $script:PrevDbName "vizitor")
}

$script:AdminUser = Read-Prompt "نام کاربری ادمین" (Get-OrDefault $script:PrevAdminUser "admin")
$genAdminPass = New-RandomPassword
$script:AdminPass = Read-SecretPrompt "گذرواژه ادمین (خالی بگذارید = تولید خودکار: $genAdminPass)" (Get-OrDefault $script:PrevAdminPass $genAdminPass)
if (-not $script:AdminPass) { $script:AdminPass = $genAdminPass }

$script:ActCode = Read-Prompt "کد فعال‌سازی (Activation Code) — می‌توانید بعداً از برنامه اندروید وارد کنید" $script:PrevActCode
if (-not $script:ActCode -and -not $Auto) {
    $confirm = Read-Prompt "  ⚠ کد فعال‌سازی خالی است؛ برنامه تا ورود کد «غیرفعال» می‌ماند. ادامه؟ [آ/خ]" "آ"
    if ($confirm -eq "خ" -or $confirm -eq "x" -or $confirm -eq "n") { Write-Info "خروج."; exit 0 }
}

# --- پورت سامانه: API و پنل مستقیماً روی همین پورت گوش می‌دهند (بدون IIS) ---
$script:InternalPort = $script:Port
$script:PublicPort   = $script:Port


if ($script:PublicPort -ne 80 -and $script:PublicPort -ne 443) { $script:ApiUrl = "${script:ApiUrl}:$($script:PublicPort)" }
$script:ApiUrl = "$script:ApiUrl/api"

Write-Host ""
Write-Host "خلاصه انتخاب‌ها:"
Write-Info "آدرس API (اندروید): $script:ApiUrl"
Write-Info "دیتابیس: $script:DbEngine"
if ($script:DbEngine -eq "sqlserver") { Write-Info "  $script:DbName @ ${script:DbHost}:$script:DbDPort (احراز: $script:DbAuth، کاربر: $script:DbUser)" }
Write-Info "ادمین: $script:AdminUser"
if ($script:ActCode) { Write-Info "کد فعال‌سازی: وارد شده ✓" } else { Write-Info "کد فعال‌سازی: خالی (فعال‌سازی بعدی)" }

# ---------------------------- گام ۴: پیش‌نیازها ----------------------------
Write-Step "بررسی و نصب پیش‌نیازهای نرم‌افزاری"

function Install-PyOdbc {
    try {
        & $script:PyExe -m pip --version *> $null
        if ($LASTEXITCODE -ne 0) { & $script:PyExe -m ensurepip *> $null }
        & $script:PyExe -m pip install --quiet pyodbc *> $null
        if ($LASTEXITCODE -ne 0) { & $script:PyExe -m pip install --quiet --user pyodbc *> $null }
        return ($LASTEXITCODE -eq 0)
    } catch {
        return $false
    }
}
function Test-PyOdbcInstalled {
    & $script:PyExe -c "import pyodbc" 2>$null
    return ($LASTEXITCODE -eq 0)
}

if ($script:DbEngine -eq "sqlserver") {
    # 4.1) سرویس SQL Server روی همین ماشین
    # (در حالت keep، بخش 4.4 «توافق برای جایگزینی sqlite» اجرا نمی‌شود تا نصب موجود به‌اشتباه دگرنده نشود)
    $sqlSvc = Get-SqlServices
    if ($sqlSvc.Count -eq 0) {
        Write-Warn "سرویس SQL Server روی این ماشین یافت نشد (اگر SQL Server روی سرور دیگری است، بقیهٔ مراحل روی همان آدرس انجام می‌شود)"
    } else {
        $running = $sqlSvc | Where-Object { $_.Status -eq "Running" }
        if (-not $running) {
            Write-Info "سرویس SQL Server خاموش است؛ تلاش برای شروع ..."
            foreach ($s in $sqlSvc) { try { Start-Service -Name $s.Name -ErrorAction Stop; break } catch {} }
            Start-Sleep -Seconds 3
        }
        if (Test-SqlServerRunning) { Write-Ok "سرویس SQL Server در حال اجرا است" }
        else { Write-Warn "شروع سرویس SQL Server انجام نشد — اگر روی سرور دیگری است، مطمئن شوید آنجا فعال و در دسترس است" }
    }

    # 4.2) pyodbc
    if ($SkipPrerequisites) {
        Write-Info "بخش «پیش‌نیازهای پایتون» تیک نخورده بود؛ نصب pyodbc انجام نمی‌شود"
    } elseif (-not (Test-PyOdbcInstalled)) {
        Write-Info "درایور python (pyodbc) نصب می‌شود ..."
        if (Install-PyOdbc) { Write-Ok "pyodbc نصب شد" }
        else { Write-Warn "نصب pyodbc ممکن نشد" }
    } else { Write-Ok "pyodbc از قبل در دسترس است" }

    # 4.3) درایور ODBC
    if ($SkipPrerequisites) {
        Write-Info "بخش «پیش‌نیازهای پایتون» تیک نخورده بود؛ نصب درایور ODBC انجام نمی‌شود"
    } elseif (-not (Test-OdbcDriverPresent)) {
        Write-Info "درایور ODBC برای SQL Server یافت نشد؛ تلاش برای نصب با winget ..."
        $winget = Get-Command winget -ErrorAction SilentlyContinue
        if ($winget) {
            try { & winget install -e --id Microsoft.Msodbcsql170 --accept-source-agreements --accept-package-agreements 2>$null | Out-Null } catch {}
        }
        if (Test-OdbcDriverPresent) { Write-Ok "درایور ODBC نصب شد" }
        else { Write-Warn "درایور ODBC در دسترس نیست — می‌توانید 'Microsoft ODBC Driver 17 for SQL Server' را دستی نصب کنید (لینک در INSTALL.md)" }
    }

    # 4.4) در صورت نقص پیش‌نیاز یا عدم دسترسی به SQL Server: توافق کاربر برای جایگزینی
    # (فقط در نصب جدید/بازپیکربندی؛ در حالت keep تنظیمات موجود دست‌نخورده می‌ماند)
    $needFallback = $false
    $fallbackReason = ""
    if ($script:Mode -ne "keep") {
        if (-not (Test-PyOdbcInstalled) -or -not (Test-OdbcDriverPresent)) {
            $needFallback = $true
            $fallbackReason = "پیش‌نیازهای SQL Server (pyodbc/درایور ODBC) کامل در دسترس نیستند"
        } elseif (-not (Test-SqlServerReachable)) {
            $needFallback = $true
            $fallbackReason = "اتصال TCP به SQL Server در ${script:DbHost}:$script:DbDPort برقرار نشد"
        }
    }
    if ($needFallback) {
        Write-Warn "$fallbackReason"
        if ($script:Unattended) {
            Write-Err "نصب متوقف شد: $fallbackReason"
            Write-Info "۱) سرویس SQL Server در حال اجرا باشد  ۲) پورت $script:DbDPort باز باشد  ۳) درایور ODBC نصب باشد"
            Write-Info "یا در نصب‌کننده، تیک بخش «دیتابیس سامانه» را بردارید تا سامانه با sqlite نصب شود."
            exit 1
        }
        $answer = "آ"
        if (-not $Auto) {
            $answer = Read-Prompt "به‌جای SQL Server از sqlite استفاده شود؟ [آ/خ] (با «خ» نصب متوقف می‌شود)" "آ"
        }
        if ($answer -eq "آ" -or $answer -eq "y" -or $answer -eq "yes" -or [string]::IsNullOrEmpty($answer)) {
            Write-Warn "به‌صورت هوشمند به sqlite تغییر می‌دهم (بعداً با -Recheck می‌توانید SQL Server را برگردانید)"
            $script:DbEngine = "sqlite"
        } else {
            Write-Err "نصب متوقف شد. برای ادامه: درایور 'Microsoft ODBC Driver 17 for SQL Server' را نصب کنید و مطمئن شوید SQL Server در دسترس است."
            exit 1
        }
    } else {
        if ($script:Mode -ne "keep") { Write-Ok "اتصال به SQL Server برقرار است (${script:DbHost}:$script:DbDPort)" }
    }
}

if ($script:DbEngine -eq "sqlite") {
    Write-Ok "دیتابیس: SQLite (بدون نیاز به سرور جداگانه) — فایل: $(Join-Path $script:DataDir 'vizitor.db')"
}

# ---------------------------- گام ۵: انتشار فایل‌ها و config ---------------
Write-Step "انتشار فایل‌ها و ساخت فایل تنظیمات"

New-Item -ItemType Directory -Force -Path $script:AppHome | Out-Null
New-Item -ItemType Directory -Force -Path $script:DataDir  | Out-Null
# حذف مقصد قبلی و کپی مجدد (جلوگیری از تودرتو شدن پوشه‌ها در نصب‌های تکراری)
foreach ($sub in @("api", "database")) {
    $src = Join-Path $script:ScriptDir $sub
    $dst = Join-Path $script:AppHome $sub
    if (Test-Path $dst) { Remove-Item -Recurse -Force $dst }
    Copy-Item -Recurse $src $dst
}
Write-Ok "فایل‌های برنامه در $script:AppHome منتشر شدند"

function Write-ConfigJson {
    $env:VIZ_DATA_DIR   = $script:DataDir
    $env:VIZ_BIND_IP    = "0.0.0.0"
    $env:VIZ_PORT       = [string]$script:InternalPort
    $env:VIZ_API_URL    = $script:ApiUrl
    $env:VIZ_DB_ENGINE  = $script:DbEngine
    $env:VIZ_DB_HOST    = $script:DbHost
    $env:VIZ_DB_DPORT   = $script:DbDPort
    $env:VIZ_DB_NAME    = $script:DbName
    $env:VIZ_DB_USER    = $script:DbUser
    $env:VIZ_DB_PASS    = $script:DbPass
    $env:VIZ_DB_AUTH    = $script:DbAuth
    $env:VIZ_ADMIN_USER = $script:AdminUser
    $env:VIZ_ADMIN_PASS = $script:AdminPass
    $env:VIZ_ACT_CODE   = $script:ActCode
    $env:VIZ_PUBLIC_IP  = $script:PublicIp
    $env:VIZ_LOCAL_IP   = $script:LocalIp
    $pyScript = @'
import json, os, datetime
data_dir = os.environ["VIZ_DATA_DIR"]
cfg_path = os.path.join(data_dir, "config.json")
cfg = {}
if os.path.exists(cfg_path):
    try:
        with open(cfg_path, encoding="utf-8") as f:
            cfg = json.load(f)
    except Exception:
        cfg = {}
cfg.setdefault("app", {"name": "Vizitor", "version": "1.0.0"})
api = cfg.setdefault("api", {})
api["bind_ip"] = os.environ["VIZ_BIND_IP"]
api["port"] = int(os.environ["VIZ_PORT"])
api["url"] = os.environ["VIZ_API_URL"]
db = cfg.setdefault("db", {})
db["engine"] = os.environ["VIZ_DB_ENGINE"]
if os.environ["VIZ_DB_ENGINE"] == "sqlserver":
    db["host"] = os.environ["VIZ_DB_HOST"]
    db["port"] = int(os.environ["VIZ_DB_DPORT"] or 1433)
    db["name"] = os.environ["VIZ_DB_NAME"]
    db["user"] = os.environ["VIZ_DB_USER"]
    db["password"] = os.environ["VIZ_DB_PASS"]
    db["auth"] = os.environ["VIZ_DB_AUTH"] or "sql"
else:
    db["path"] = os.path.join(data_dir, "vizitor.db")
adm = cfg.setdefault("admin", {})
adm["username"] = os.environ["VIZ_ADMIN_USER"]
adm["password"] = os.environ["VIZ_ADMIN_PASS"]
act = cfg.setdefault("activation", {})
if os.environ["VIZ_ACT_CODE"]:
    act["code"] = os.environ["VIZ_ACT_CODE"]
meta = cfg.setdefault("meta", {})
meta.setdefault("installed_at", datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S"))
meta["last_check_at"] = datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S")
meta["public_ip"] = os.environ["VIZ_PUBLIC_IP"]
meta["local_ip"] = os.environ["VIZ_LOCAL_IP"]
meta["os"] = "windows"
meta["installer_version"] = "1.0.0"
os.makedirs(data_dir, exist_ok=True)
with open(cfg_path, "w", encoding="utf-8") as f:
    json.dump(cfg, f, ensure_ascii=False, indent=2)
'@
    $pyScript | & $script:PyExe -
    return ($LASTEXITCODE -eq 0)
}

if ($script:Mode -eq "keep") {
    $ok = Write-ConfigJson
    if ($ok) { Write-Ok "فایل تنظیمات به‌روزرسانی شد (مقادیر قبلی حفظ شدند): $script:ConfigFile" }
    else { Write-Warn "به‌روزرسانی config ممکن نشد" }
}
else {
    if (Write-ConfigJson) { Write-Ok "فایل تنظیمات نوشته شد: $script:ConfigFile" }
    else { Write-Err "نوشتن config.json ناموفق بود"; exit 1 }
}

# ---------------------------- سرویس (Task) --------------------------------
function Install-VizitorTask {
    if (Get-ScheduledTask -TaskName $script:TaskName -ErrorAction SilentlyContinue) { return }
    $batContent = "@echo off`r`n`"$script:PyExe`" `"$script:AppHome\api\server.py`" --config `"$script:ConfigFile`" --panel `"$script:AppHome\panel`" >> `"$script:LogFile`" 2>&1"
    Set-Content -Path $script:BatFile -Value $batContent -Encoding ASCII
    try {
        $action = New-ScheduledTaskAction -Execute "cmd.exe" -Argument "/c `"$script:BatFile`"" -WorkingDirectory $script:AppHome
        $principal = New-ScheduledTaskPrincipal -UserId "SYSTEM" -LogonType ServiceAccount -RunLevel Highest
        $settings = New-ScheduledTaskSettingsSet -AllowStartIfOnBatteries -DontStopIfGoingOnBatteries -RestartCount 3 -RestartInterval (New-TimeSpan -Minutes 1) -ExecutionTimeLimit ([TimeSpan]::Zero) -StartWhenAvailable
        $trigger = New-ScheduledTaskTrigger -AtStartup
        Register-ScheduledTask -TaskName $script:TaskName -Action $action -Principal $principal -Settings $settings -Trigger $trigger -Description "Vizitor API server" | Out-Null
        Write-Ok "سرویس ویندوز (Scheduled Task) ساخته شد: $script:TaskName"
    } catch {
        Write-Warn "ساخت Scheduled Task ممکن نشد؛ از اجرای مستقیم (Hidden process) استفاده می‌شود"
    }
}

function Start-VizitorService {
    try {
        Start-ScheduledTask -TaskName $script:TaskName -ErrorAction Stop
        return
    } catch {}
    $p = Start-Process -FilePath $script:PyExe -ArgumentList "`"$script:AppHome\api\server.py`" --config `"$script:ConfigFile`" --panel `"$script:AppHome\panel`"" -WorkingDirectory "$script:AppHome\api" -WindowStyle Hidden -PassThru
    if ($p) { $p.Id | Out-File -FilePath $script:PidFile -Encoding ascii }
}

function Stop-VizitorService {
    try { Stop-ScheduledTask -TaskName $script:TaskName -ErrorAction SilentlyContinue } catch {}
    if (Test-Path $script:PidFile) {
        try { $oldPid = [int](Get-Content $script:PidFile -Raw).Trim(); Stop-Process -Id $oldPid -Force -ErrorAction SilentlyContinue } catch {}
        Remove-Item $script:PidFile -Force -ErrorAction SilentlyContinue
    }
}

function Restart-VizitorService {
    Stop-VizitorService
    Start-Sleep -Seconds 1
    Start-VizitorService
}

function Seed-Db {
    & $script:PyExe "$script:AppHome\api\seed.py" --config "$script:ConfigFile" *> $null
}

if ($script:Mode -ne "keep") { Install-VizitorTask }

# ---------------------------- گام ۶: فایروال ----------------------------------

Write-Step "بررسی فایروال ویندوز"
if ($SkipFirewall) {
    Write-Info "بخش «قاعدهٔ فایروال» تیک نخورده بود؛ این مرحله رد شد."
    Write-Info "دستی:  New-NetFirewallRule -DisplayName 'Vizitor API' -Direction Inbound -Action Allow -Protocol TCP -LocalPort $script:PublicPort"
} else {
    try {
        if (-not (Get-NetFirewallRule -DisplayName "Vizitor API" -ErrorAction SilentlyContinue)) {
            # فقط شبکهٔ محلی — سامانه طوری طراحی شده که روی اینترنت باز نشود
            New-NetFirewallRule -DisplayName "Vizitor API" -Direction Inbound -Action Allow -Protocol TCP -LocalPort $script:PublicPort -RemoteAddress LocalSubnet -Profile Any | Out-Null
            Write-Ok "قانون فایروال برای پورت $($script:PublicPort)/TCP فقط برای شبکهٔ محلی ساخته شد (Vizitor API)"
        } else {
            Write-Info "قانون فایروال Vizitor API از قبل وجود دارد"
        }
    } catch {
        Write-Warn "ساخت قانون فایروال خودکار ممکن نشد — در صورت نیاز دستی: New-NetFirewallRule -DisplayName 'Vizitor API' -Direction Inbound -Action Allow -Protocol TCP -LocalPort $script:PublicPort"
    }
}

# ---------------- آماده‌سازی اتصال مستقیم اندروید به SQL Server -------------
function Test-SqlTcpListener {
    <#
      بررسی می‌کند که SQL Server واقعاً روی TCP (پورت ۱۴۳۳) گوش می‌دهد یا نه.
      علت: اگر TCP/IP غیرفعال باشد، سرویس ویزیتور روی همان ماشین با named pipes وصل
      می‌شود و همه‌چیز سالم به نظر می‌رسد، ولی گوشی/برنامهٔ اندروید هرگز وصل نمی‌شود.
      این بررسی فقط می‌خواند: sys.dm_tcp_listener_states
    #>
    if ($script:DbEngine -ne "sqlserver") { return $true }
    $out = Join-Path $script:DataDir "sql_listener.json"
    try {
        $prevEap = $ErrorActionPreference
        $ErrorActionPreference = "Continue"
        $raw = & $script:PyExe "$script:AppHome\api\sql_admin_tools.py" listener `
               --config "$script:ConfigFile" --port 1433 --out "$out" 2>&1
        $ErrorActionPreference = $prevEap
    } catch {
        Write-Warn "بررسی شنوندهٔ TCP ممکن نشد: $($_.Exception.Message)"
        return $true        # نبود ابزار نباید نصب را متوقف کند
    }
    $json = $null
    if (Test-Path $out) {
        try { $json = Get-Content $out -Raw -Encoding UTF8 | ConvertFrom-Json } catch { $json = $null }
    }
    if ($null -eq $json) { return $true }

    switch ($json.verdict) {
        "tcp_ok_all_ips" {
            Write-Ok "پروتکل TCP/IP فعال است و SQL Server روی پورت ۱۴۳۳ به همهٔ کارت‌های شبکه گوش می‌دهد ✓"
            return $true
        }
        "tcp_ok_single_ip" {
            Write-Warn "TCP روی پورت ۱۴۳۳ فعال است، ولی SQL Server فقط روی یک آی‌پی گوش می‌دهد."
            Write-Info "  شنونده‌های فعال: $(($json.listeners | Where-Object { $_.state -eq 'Online' } | ForEach-Object { "$($_.ip):$($_.port)" }) -join ' ، ')"
            Write-Info "  اگر گوشی به همان کارت شبکه وصل نیست، در SQL Server Configuration Manager"
            Write-Info "  → Protocols for <instance> → TCP/IP → تب IP Addresses → IPAll → TCP Port = 1433"
            Write-Info "  و در همان تب، برای کارت شبکهٔ درست TCP Dynamic Ports را خالی کنید، بعد سرویس را ری‌استارت کنید."
            return $true
        }
        "tcp_on_other_port" {
            Write-Warn "SQL Server روی TCP فعال است ولی روی پورت دیگری: $($json.detail)"
            Write-Info "  در نصب‌کننده پورت ۱۴۳۳ وارد شد؛ یا پورت فعال را در برنامهٔ اندروید وارد کنید،"
            Write-Info "  یا در SQL Server Configuration Manager پورت ۱۴۳۳ را تنظیم و سرویس را ری‌استارت کنید."
            return $true
        }
        "tcp_disabled" {
            Write-Warn "پروتکل TCP/IP در SQL Server غیرفعال است — برنامهٔ اندروید نمی‌تواند وصل شود."
            Write-Info "  فعال‌سازی (راه گرافیکی): SQL Server Configuration Manager"
            Write-Info "     → SQL Server Network Configuration → Protocols for <instance> → TCP/IP"
            Write-Info "     → Enabled = Yes ، سپس تب IP Addresses → IPAll → TCP Port = 1433"
            Write-Info "     → بعد SQL Server (سرویس) را Restart کنید."
            Write-Info "  فعال‌سازی سریع با PowerShell (نیازمند دسترسی مدیر و ری‌استارت سرویس):"
            Write-Info "     $script:ConfigMgrHint"
            return $false
        }
        default {
            if ($json.error) { Write-Warn "بررسی شنوندهٔ TCP: $($json.error) $($json.detail)" }
            return $true
        }
    }
}

function Enable-SqlTcp {
    <#
      فعال‌سازی سریع TCP/IP روی پورت ۱۴۳۳ با تغییر کلیدهای رجیستری همان نسخهٔ SQL Server،
      سپس یک‌بار ری‌استارت سرویس. هیچ تغییری در دیتابیس‌ها داده نمی‌شود.
    #>
    if (-not $script:SqlInstanceRegKey) { return $false }
    try {
        $nets = "$($script:SqlInstanceRegKey)\SuperSocketNetLib\Tcp"
        if (-not (Test-Path $nets)) { Write-Warn "کلید رجیستری TCP پیدا نشد: $nets"; return $false }
        New-ItemProperty -Path $nets -Name "Enabled" -Value 1 -PropertyType DWord -Force | Out-Null
        New-ItemProperty -Path "$nets\IPAll" -Name "TcpPort" -Value "1433" -PropertyType String -Force | Out-Null
        Remove-ItemProperty -Path "$nets\IPAll" -Name "TcpDynamicPorts" -ErrorAction SilentlyContinue
        Write-Ok "TCP/IP فعال شد و پورت ۱۴۳۳ تنظیم شد (کلید: $nets)"
        $svc = if ($script:SqlServiceName) { $script:SqlServiceName } else { "MSSQLSERVER" }
        Write-Info "ری‌استارت سرویس $svc ..."
        try { Restart-Service -Name $svc -Force -ErrorAction Stop; Write-Ok "سرویس $svc ری‌استارت شد." }
        catch {
            try { & net stop $svc /y | Out-Null; & net start $svc | Out-Null; Write-Ok "سرویس $svc ری‌استارت شد (net)." }
            catch { Write-Warn "ری‌استارت خودکار سرویس ممکن نشد؛ دستی: Restart-Service $svc" }
        }
        Start-Sleep -Seconds 2
        return $true
    } catch {
        Write-Warn "فعال‌سازی خودکار ناموفق بود: $($_.Exception.Message)"
        return $false
    }
}

function Get-SqlAuthMode {
    <#
      فقط خواندن: آیا سرور ورود با کاربر SQL را می‌پذیرد؟
      اگر سرور «فقط ویندوزی» باشد، هیچ کاربر SQL (از جمله کاربری که همین نصب‌کننده
      می‌سازد) نمی‌تواند از گوشی وارد شود، هرچند پورت ۱۴۳۳ باز و سبز باشد.
    #>
    if ($script:DbEngine -ne "sqlserver") { return $null }
    $out = Join-Path $script:DataDir "sql_authmode.json"
    try {
        $prevEap = $ErrorActionPreference
        $ErrorActionPreference = "Continue"
        & $script:PyExe "$script:AppHome\api\sql_admin_tools.py" authmode --config "$script:ConfigFile" --out "$out" 2>&1 | Out-Null
        $ErrorActionPreference = $prevEap
    } catch { return $null }
    if (-not (Test-Path $out)) { return $null }
    try { return (Get-Content $out -Raw -Encoding UTF8 | ConvertFrom-Json) } catch { return $null }
}

function Enable-SqlMixedMode {
    <#
      حالت احراز هویت را به «SQL Server and Windows» تغییر می‌دهد (LoginMode=2 در
      رجیستری همان نسخهٔ SQL Server) و سرویس را یک‌بار ری‌استارت می‌کند.
      هیچ تغییری در دیتابیس‌ها و کاربران داده نمی‌شود.
    #>
    if (-not $script:SqlInstanceRegKey) { return $false }
    try {
        $key = $script:SqlInstanceRegKey
        if (-not (Test-Path $key)) { Write-Warn "کلید رجیستری سرور پیدا نشد: $key"; return $false }
        $before = (Get-ItemProperty -Path $key -Name "LoginMode" -ErrorAction SilentlyContinue).LoginMode
        New-ItemProperty -Path $key -Name "LoginMode" -Value 2 -PropertyType DWord -Force | Out-Null
        $after = (Get-ItemProperty -Path $key -Name "LoginMode" -ErrorAction SilentlyContinue).LoginMode
        Write-Ok "حالت احراز هویت سرور به «SQL Server and Windows» تغییر کرد (LoginMode: $before → $after)"
        if ($after -ne 2) { Write-Warn "مقدار ثبت‌شده تأیید نشد ($after) — کلید: $key" }
        $svc = if ($script:SqlServiceName) { $script:SqlServiceName } else { "MSSQLSERVER" }
        Write-Info "ری‌استارت سرویس $svc برای اعمال تغییر ..."
        try { Restart-Service -Name $svc -Force -ErrorAction Stop; Write-Ok "سرویس $svc ری‌استارت شد." }
        catch {
            try { & net stop $svc /y | Out-Null; & net start $svc | Out-Null; Write-Ok "سرویس $svc ری‌استارت شد (net)." }
            catch { Write-Warn "ری‌استارت خودکار ممکن نشد؛ دستی: Restart-Service $svc" }
        }
        Start-Sleep -Seconds 3
        return $true
    } catch {
        Write-Warn "تغییر حالت احراز هویت ناموفق بود: $($_.Exception.Message)"
        return $false
    }
}

function Invoke-AndroidPrep {
    if ($script:DbEngine -ne "sqlserver") {
        Write-Warn "اتصال مستقیم اندروید فقط برای SQL Server معنا دارد (دیتابیس فعلی: $script:DbEngine) — رد شد"
        return $false
    }
    # مسیر رجیستری همان نمونهٔ نصب‌شدهٔ SQL Server باید همین‌جا معلوم شود؛
    # در حالت «فقط آماده‌سازی اندروید» (میان‌بر اتصال مستقیم SQL) این تابع هنوز
    # اجرا نشده بود و در نتیجه فعال‌سازی TCP/حالت احراز هویت عملاً بی‌اثر می‌شد.
    if (-not $script:SqlInstanceRegKey) {
        try { Resolve-SqlInstanceInfo } catch { Write-Warn "شناسایی نمونهٔ SQL Server ممکن نشد: $($_.Exception.Message)" }
    }
    $erp   = $script:ErpDb
    $login = $script:AndroidLogin
    if (-not $login) { $login = "vizitor_android" }
    if (-not $erp) {
        Write-Warn "هیچ دیتابیس حسابداری انتخاب نشده بود؛ این بخش رد شد."
        Write-Info "پس از نصب، میان‌بر «اتصال مستقیم SQL» را اجرا کنید و دیتابیس را از لیست انتخاب کنید."
        return $false
    }

    $pwFile  = Join-Path $script:DataDir "android_app_password.txt"
    $jsonOut = Join-Path $script:DataDir "android_sql_info.json"
    $pw = ""
    $generated = $false
    if (Test-Path $pwFile) { try { $pw = (Get-Content $pwFile -Raw).Trim() } catch {} }
    if (-not $pw) {
        # رمز از پیش تعیین‌شده در فایل نیست؛ یکی می‌سازیم و فقط در فایل با
        # دسترسی محدود ذخیره می‌کنیم (هرگز چاپ/لاگ نمی‌شود)
        try { $pw = New-RandomPassword 24 } catch { $pw = "" }
        $generated = $true
    }

    # ---- حالت احراز هویت سرور: شرط اول ورود از گوشی --------------------------
    Write-Step "بررسی حالت احراز هویت SQL Server (ورود با کاربر SQL)"
    $auth = Get-SqlAuthMode
    if ($null -eq $auth) {
        Write-Info "این بررسی ممکن نشد (اتصال مدیر به سرور برقرار نشد) — ادامه می‌دهیم."
    } elseif ($auth.windows_only) {
        Write-Warn "سرور فقط ورود «ویندوزی» را می‌پذیرد — با این حالت هیچ کاربر SQL نمی‌تواند از گوشی وارد شود."
        $answer = "n"
        if (-not $Unattended) {
            $answer = Read-Prompt "حالت احراز هویت را به «SQL + ویندوز» تغییر دهم و سرویس SQL را یک‌بار ری‌استارت کنم؟ (بله/n)" "بله"
        } else {
            # اجرای خودکار: این تنها موردی است که بدون آن، اتصال گوشی هرگز کار نمی‌کند
            # (پورت باز است، اما SQL Server کاربر SQL را رد می‌کند → خطای 18456).
            # چون آماده‌سازی «اتصال مستقیم اندروید» صریحاً درخواست شده، خودکار اعمال می‌شود.
            $answer = "بله"
            Write-Info "اجرای خودکار: این تغییر برای کار کردن اپ لازم است، بنابراین خودکار اعمال می‌شود."
            Write-Info "برای جلوگیری: نصب را با -NoSqlRestart اجرا کنید (تغییر ثبت می‌شود، ری‌استارت با شما)."
        }
        if ($answer -match "^(بله|yes|y|ب)$") {
            if (Enable-SqlMixedMode) {
                $auth = Get-SqlAuthMode
                if ($auth -and (-not $auth.windows_only)) {
                    Write-Ok "اکنون ورود با کاربر SQL هم ممکن است ✓"
                } else {
                    Write-Warn "تغییر اعمال شد ولی تأیید نشد؛ اگر سرویس ری‌استارت نشده، دستی ری‌استارت کنید."
                }
            }
        } else {
            Write-Warn "بدون این تغییر، برنامهٔ اندروید «نام کاربری یا رمز اشتباه» می‌گیرد (خطای 18456)."
            Write-Info "دستی: SSMS → راست‌کلیک سرور → Properties → Security → «SQL Server and Windows Authentication mode» → OK"
            Write-Info "      سپس:  Restart-Service $($script:SqlServiceName)"
        }
    } else {
        Write-Ok "سرور ورود با کاربر SQL را می‌پذیرد (Mixed Mode) ✓"
    }

    Write-Info "کاربر محدود ($login) روی دیتابیس [$erp] بررسی/ساخته می‌شود ..."
    $script:AndroidOk = $false
    try {
        if ($pw) { $env:VIZ_ANDROID_SQL_PASSWORD = $pw }
        $prevEap = $ErrorActionPreference
        $ErrorActionPreference = "Continue"     # خروجی stderr پایتون نباید نصب را متوقف کند
        $out = & $script:PyExe "$script:AppHome\api\provision_android_sql.py" --config "$script:ConfigFile" --erp-db $erp --login $login --json-out $jsonOut --enable-sql-auth --self-test 2>&1
        $rc = $LASTEXITCODE
        $ErrorActionPreference = $prevEap
        foreach ($l in $out) { Write-Info $l }
        if ($rc -eq 0) { $script:AndroidOk = $true }
    } catch {
        Write-Warn "اجرای آماده‌سازی اتصال مستقیم ناموفق بود: $($_.Exception.Message)"
    } finally {
        Remove-Item Env:\VIZ_ANDROID_SQL_PASSWORD -ErrorAction SilentlyContinue
    }
    if (-not $script:AndroidOk) {
        Write-Warn "آماده‌سازی اتصال مستقیم اندروید کامل نشد (جزئیات در پیام‌های بالا)"
        return $false
    }

    if ($generated) {
        Set-Content -Path $pwFile -Value $pw -Encoding ASCII
        try { & icacls $pwFile /inheritance:r /grant:r "*S-1-5-18:F" "*S-1-5-32-544:F" | Out-Null } catch {}
        Write-Ok "رمز کاربر دیتابیس ساخته و در فایل زیر ذخیره شد (دسترسی: فقط سیستم/مدیر):"
        Write-Info "   $pwFile"
        Write-Info "   همین رمز را یک‌بار در صفحهٔ تنظیمات برنامهٔ اندروید وارد کنید."
    } else {
        Write-Ok "رمز کاربر دیتابیس از قبل موجود بود (تغییر داده نشد)."
        Write-Info "   محل نگه‌داری رمز: $pwFile"
    }

    # ---- خودآزمایی: دقیقاً همان کاری که گوشی می‌کند -------------------------
    $info = $null
    if (Test-Path $jsonOut) {
        try { $info = Get-Content $jsonOut -Raw -Encoding UTF8 | ConvertFrom-Json } catch { $info = $null }
    }
    if ($info) {
        if ($info.restart_needed) {
            Write-Warn "برای اعمال حالت احراز هویت، SQL Server باید یک‌بار ری‌استارت شود."
            $answer = "n"
            if (-not $Unattended) {
                $answer = Read-Prompt "همین حالا سرویس SQL را ری‌استارت کنم؟ (بله/n)" "بله"
            } elseif (-not $NoSqlRestart) {
                # بدون ری‌استارت، تغییر حالت احراز هویت اعمال نمی‌شود و اپ «رمز اشتباه» می‌گیرد
                $answer = "بله"
                Write-Info "اجرای خودکار: سرویس SQL برای اعمال تغییر یک‌بار ری‌استارت می‌شود (چند ثانیه)."
            } else {
                Write-Warn "طبق -NoSqlRestart ری‌استارت انجام نشد — تا ری‌استارت دستی، ورود کاربر SQL کار نمی‌کند."
                Write-Info "دستی:  Restart-Service $($script:SqlServiceName)"
            }
            if ($answer -match "^(بله|yes|y|ب)$") {
                $svc = if ($script:SqlServiceName) { $script:SqlServiceName } else { "MSSQLSERVER" }
                try { Restart-Service -Name $svc -Force -ErrorAction Stop; Write-Ok "سرویس $svc ری‌استارت شد." } catch { Write-Warn "ری‌استارت خودکار ممکن نشد؛ دستی: Restart-Service $svc" }
                Start-Sleep -Seconds 3
            }
        }
        $st = [string]$info.self_test
        if ($st -eq "ok") {
            Write-Ok "خودآزمایی ورود: کاربر «$login» با موفقیت وارد دیتابیس [$erp] شد ✓ (همان کاری که برنامهٔ اندروید می‌کند)"
        } elseif ($st -eq "no-access") {
            Write-Warn "خودآزمایی: ورود انجام شد ولی این کاربر به دیتابیس [$erp] دسترسی ندارد."
        } elseif ($st -like "failed*") {
            Write-Warn "خودآزمایی ورود ناموفق بود — جزئیات در $jsonOut"
            Write-Info "   $st"
        }
    }

    if ($script:AndroidFirewall) {
        if ($script:AndroidExternal) {
            Write-Warn "طبق انتخاب شما، پورت 1433 برای اتصال از بیرون شبکه (آی‌پی اختصاصی) باز می‌شود."
            Write-Info "توصیه: حتماً کاربر محدود $login با رمز قوی + VPN یا محدودسازی IP در فایروال/روتر."
        } else {
            Write-Info "قاعدهٔ فایروال پورت 1433 فقط برای شبکهٔ محلی ..."
        }
        $done = $false
        $remote = if ($script:AndroidExternal) { "Any" } else { "LocalSubnet" }
        try {
            if (-not (Get-NetFirewallRule -DisplayName "Vizitor SQL 1433" -ErrorAction SilentlyContinue)) {
                New-NetFirewallRule -DisplayName "Vizitor SQL 1433" -Direction Inbound -Action Allow -Protocol TCP -LocalPort 1433 -RemoteAddress $remote -Profile Any | Out-Null
            }
            $done = $true
        } catch { $done = $false }
        if (-not $done) {
            try {
                $r = & netsh advfirewall firewall show rule name="Vizitor SQL 1433" 2>&1 | Out-String
                if ($r -notmatch "Vizitor SQL 1433") {
                    & netsh advfirewall firewall add rule name="Vizitor SQL 1433" dir=in action=allow protocol=TCP localport=1433 remoteip=localsubnet profile=any | Out-Null
                }
                $done = $true
            } catch { $done = $false }
        }
        if ($done) {
            if ($script:AndroidExternal) { Write-Ok "پورت 1433 برای شبکهٔ محلی و آی‌پی اختصاصی باز شد (Vizitor SQL 1433)" }
            else { Write-Ok "پورت 1433 فقط برای شبکهٔ محلی باز شد (Vizitor SQL 1433)" }
        }
        else { Write-Warn "قاعدهٔ فایروال ساخته نشد؛ دستی:  netsh advfirewall firewall add rule name=`"Vizitor SQL 1433`" dir=in action=allow protocol=TCP localport=1433 remoteip=localsubnet" }
    }
    # --- کارت اتصال + کد QR برای برنامهٔ اندروید ---------------------------
    # همهٔ چیزی که اپ لازم دارد در یک تصویر/فایل؛ ویزیتور فقط نام کاربری و
    # کلمهٔ عبور خودش را وارد می‌کند.
    $pkgDir = Join-Path $script:AppHome "setup"
    if (-not (Test-Path $pkgDir)) { New-Item -ItemType Directory -Force -Path $pkgDir | Out-Null }
    try {
        $prevEap2 = $ErrorActionPreference
        $ErrorActionPreference = "Continue"
        $cardArgs = @("$script:AppHome\api\android_connect.py", "--config", "$script:ConfigFile",
                      "--package-dir", "$pkgDir", "--erp-db", $erp, "--login", $login,
                      "--password-file", $pwFile, "--update-config")
        if ($script:HostLan)    { $cardArgs += @("--host-lan", $script:HostLan) }
        elseif ($script:LocalIp) { $cardArgs += @("--host-lan", $script:LocalIp) }
        if ($script:HostPublic) { $cardArgs += @("--host-public", $script:HostPublic) }
        elseif ($script:PublicIp) { $cardArgs += @("--host-public", $script:PublicIp) }
        $cardOut = & $script:PyExe @cardArgs 2>&1
        $ErrorActionPreference = $prevEap2
        foreach ($l in $cardOut) { Write-Info $l }
        if (Test-Path (Join-Path $pkgDir "android-connect.png")) {
            Write-Ok "کارت اتصال اندروید ساخته شد:"
            Write-Info "   تصویر QR  : $(Join-Path $pkgDir 'android-connect.png')"
            Write-Info "   کارت متنی  : $(Join-Path $pkgDir 'android-connect.txt')"
            Write-Info "   فایل JSON  : $(Join-Path $pkgDir 'android-connect.json')"
        }
    } catch {
        Write-Warn "ساخت کارت اتصال اندروید ممکن نشد: $($_.Exception.Message)"
    }

    # ---- بررسی TCP/IP ۱۴۳۳: تنها چیزی که بین «سرور سالم» و «گوشی وصل می‌شود» فاصله است
    Write-Step "بررسی شنوندهٔ شبکهٔ SQL Server روی پورت ۱۴۳۳"
    if (-not (Test-SqlTcpListener)) {
        $answer = "n"
        if (-not $Unattended) {
            $answer = Read-Prompt "همین حالا TCP/IP را فعال کنم و سرویس SQL را یک‌بار ری‌استارت کنم؟ (بله/n)" "بله"
        } elseif (-not $NoSqlRestart) {
            # بدون شنوندهٔ ۱۴۳۳ هیچ اتصالی از گوشی ممکن نیست؛ در اجرای خودکار اعمال می‌شود
            $answer = "بله"
            Write-Info "اجرای خودکار: TCP/IP روی ۱۴۳۳ فعال می‌شود (ری‌استارت کوتاه سرویس SQL)."
        } else {
            Write-Warn "طبق -NoSqlRestart، فعال‌سازی TCP انجام نشد."
        }
        if ($answer -match "^(بله|yes|y|ب)$") {
            if (Enable-SqlTcp) {
                Start-Sleep -Seconds 3
                if (Test-SqlTcpListener) {
                    Write-Ok "حالا SQL Server روی پورت ۱۴۳۳ گوش می‌دهد و اتصال اندروید آماده است ✓"
                } else {
                    Write-Warn "پس از فعال‌سازی هم شنوندهٔ ۱۴۳۳ دیده نشد؛ دستی از Configuration Manager بررسی کنید."
                }
            }
        } else {
            $req = "دستور زیر را به‌عنوان Administrator اجرا کنید (پس از آن سرویس SQL را ری‌استارت کنید):"
            Write-Warn "بدون فعال‌سازی TCP/IP، برنامهٔ اندروید وصل نخواهد شد."
            Write-Info $req
            Write-Info "  $script:RegEnableHint"
            Write-Info "  Restart-Service $($script:SqlServiceName)   (یا ری‌استارت از services.msc)"
        }
    }
    Write-Ok "اتصال مستقیم اندروید آماده است (سرور: $script:Addr ، پورت: 1433 ، دیتابیس: $erp ، کاربر: $login)"
    Write-Info "جزئیات (بدون رمز): $jsonOut"

    return $true
}

if ($AndroidPrepOnly) {
    Write-Step "آماده‌سازی اتصال مستقیم اندروید به SQL Server"
    Invoke-AndroidPrep | Out-Null
    Write-Host ""
    Write-Info "پایان. (این حالت فقط همین بخش را اجرا می‌کند)"
    exit 0
}

# ---------------------------- گام ۷: راه‌اندازی + بازرسی --------------------
Write-Step "راه‌اندازی سرویس API"

# در حالت keep، مقادیر را از فایل تنظیمات بخوان (برای چک‌ها و تعمیرات)
if ($script:Mode -eq "keep") {
    $c = Get-Content $script:ConfigFile -Raw -Encoding UTF8 | ConvertFrom-Json
    $script:Port      = [int]$c.api.port
    $script:ApiUrl    = [string]$c.api.url
    $script:DbEngine  = [string]$c.db.engine
    $script:DbHost    = [string]$c.db.host
    $script:DbDPort   = [string]$c.db.port
    $script:DbName    = [string]$c.db.name
    $script:DbUser    = [string]$c.db.user
    $script:DbPass    = [string]$c.db.password
    $script:DbAuth    = [string]$c.db.auth
    $script:AdminUser = [string]$c.admin.username
    $script:AdminPass = [string]$c.admin.password
    $script:ActCode   = [string]$c.activation.code
    $script:Addr      = $script:PrevAddr
    if (-not $script:Addr) {
        if ($script:ApiUrl -match '^[a-z]+://([^:/]+)') { $script:Addr = $Matches[1] }
    }
    $script:Proto = "http"
    if ($script:ApiUrl -match '^https://') { $script:Proto = "https" }
    $script:InternalPort = $script:Port
    $script:PublicPort = $script:Port
}

# اگر sqlserver انتخاب شد، دیتابیس به‌صورت غیرتلفیقی آماده شود
function Provision-SqlServer {
    if ($script:DbEngine -ne "sqlserver") { return $true }
    & $script:PyExe "$script:AppHome\api\provision_sqlserver.py" --config "$script:ConfigFile"
    return ($LASTEXITCODE -eq 0)
}

if ($SkipDatabase) {
    Write-Info "بخش «دیتابیس سامانه» تیک نخورده بود؛ ساخت جداول رد شد (سامانه انتظار دارد جداول از قبل موجود باشند)."
} elseif ($script:DbEngine -eq "sqlserver") {
    if (-not (Provision-SqlServer)) {
        Write-Warn "آماده‌سازی SQL Server ممکن نشد — به‌صورت هوشمند به sqlite تغییر می‌دهم"
        $script:DbEngine = "sqlite"
        Write-ConfigJson | Out-Null
        Write-Ok "config.json به sqlite به‌روزرسانی شد"
    } else {
        Write-Ok "SQL Server بدون تغییر اشیاء موجود آماده شد (فقط CREATE در صورت نبود)"
    }
}

# ---- اطلاعات لازم برای بررسی/فعال‌سازی TCP/IP روی همین نسخهٔ SQL Server ---------

function Resolve-SqlInstanceInfo {
    $inst = ""
    try {
        $inst = (Get-ItemProperty "HKLM:\SOFTWARE\Microsoft\Microsoft SQL Server\Instance Names\SQL" -ErrorAction SilentlyContinue).PSObject.Properties |
                Where-Object { $_.Name -eq "MSSQLSERVER" -or $_.Name -like "MSSQL*" } |
                Select-Object -First 1 -ExpandProperty Value
    } catch { $inst = "" }
    if (-not $script:SqlServiceName) {
        $script:SqlServiceName = if ($script:SqlInstance -and $script:SqlInstance -ne "MSSQLSERVER") { "MSSQL`$$script:SqlInstance" } else { "MSSQLSERVER" }
    }
    try {
        $svc = Get-Service -Name $script:SqlServiceName -ErrorAction SilentlyContinue
        if ($svc) {
            $id = (Get-ItemProperty "HKLM:\SYSTEM\CurrentControlSet\Services\$($script:SqlServiceName)" -ErrorAction SilentlyContinue).ImagePath
            # نمونهٔ پیش‌فرض: MSSQL15.MSSQLSERVER — نمونهٔ نام‌دار: MSSQL15.SQLEXPRESS
            if ($id -match "(MSSQL\d+\.[A-Za-z0-9_\-]+)") {
                $script:SqlInstanceRegPath = "$($Matches[1])"
            }
        }
    } catch { }
    if (-not $script:SqlInstanceRegPath) {
        # جست‌وجوی همهٔ نمونه‌های نصب‌شده (پیش‌فرض و نام‌دار) در رجیستری
        $base = "HKLM:\SOFTWARE\Microsoft\Microsoft SQL Server"
        try {
            foreach ($k in (Get-ChildItem $base -ErrorAction SilentlyContinue | Where-Object { $_.PSChildName -match "^MSSQL\d+\." })) {
                if (Test-Path "$($k.PSPath)\MSSQLServer\SuperSocketNetLib\Tcp") {
                    $script:SqlInstanceRegPath = $k.PSChildName
                    break
                }
            }
        } catch { }
    }
    # کلید کامل: ریشهٔ نسخهٔ SQL Server + نام نمونه + پوشهٔ MSSQLServer
    # (قبلاً پیشوند «SOFTWARE\Microsoft\Microsoft SQL Server» جا افتاده بود؛
    #  در نتیجه Test-Path همیشه شکست می‌خورد و فعال‌سازی TCP و حالت احراز هویت
    #  هرگز اجرا نمی‌شد — همان چیزی که اپ اندروید را بی‌دلیل ناتوان می‌کرد.)
    if ($script:SqlInstanceRegPath) {
        $script:SqlInstanceRegKey = "HKLM:\SOFTWARE\Microsoft\Microsoft SQL Server\$($script:SqlInstanceRegPath)\MSSQLServer"
    }
    $script:ConfigMgrHint = "SQL Server Configuration Manager → SQL Server Network Configuration → Protocols → TCP/IP → Enabled = Yes"
    if ($script:SqlInstanceRegKey) {
        $key = "$($script:SqlInstanceRegKey)\SuperSocketNetLib\Tcp"
        $script:RegEnableHint = "Set-ItemProperty '$key' Enabled 1; New-ItemProperty '$key\IPAll' TcpPort '1433' -Force"
        $script:RegEnableHint = $script:RegEnableHint.Replace("HKLM:\", "HKLM:\")
    } else {
        $script:RegEnableHint = "(کلید رجیستری نسخهٔ SQL Server پیدا نشد — از SQL Server Configuration Manager استفاده کنید)"
    }
}
Resolve-SqlInstanceInfo
if (-not (Get-ScheduledTask -TaskName $script:TaskName -ErrorAction SilentlyContinue)) { Install-VizitorTask }

# درج اولیه داده‌ها (حساب ادمین + کد فعال‌سازی + تنظیمات پایه) — ای‌دی‌ام‌پتنت
Seed-Db

Restart-VizitorService
Start-Sleep -Seconds 3

# ---- حلقه چک و تعمیر -------------------------------------------------------
Write-Step "بازرسی کامل و بازبینی خودکار تنظیمات (تا ۴ دور)"
if ($SkipSelfCheck) {
    Write-Info "بخش «بازرسی و تعمیر خودکار» تیک نخورده بود؛ این مرحله رد شد."
    Write-Info "هر زمان خواستید:  powershell -ExecutionPolicy Bypass -File `"$PSScriptRoot\install.ps1`" -Recheck"
    $verifyOk = $true
} else {

$script:CheckFailures = @()

function Run-Checks {
    $script:CheckFailures = @()
    $base = "http://127.0.0.1:$($script:InternalPort)"

    # ۱) سرویس در حال listen باشد
    if (-not (Test-PortUp $script:InternalPort)) { $script:CheckFailures += "process"; return }

    # ۲) ping
    $r = Invoke-JsonGet "$base/api/ping"
    if ($null -eq $r -or $r -notmatch '"status": *"ok"') { $script:CheckFailures += "ping"; return }

    # ۳) api/config باید آدرس درست را برمی‌گرداند
    $r = Invoke-JsonGet "$base/api/config"
    if ($null -eq $r -or $r -notmatch [regex]::Escape($script:ApiUrl)) { $script:CheckFailures += "config" }

    # ۴) دیتابیس
    $r = Invoke-JsonGet "$base/api/health"
    if ($null -ne $r -and $r -match '"status": *"error"') { $script:CheckFailures += "db" }

    # ۵) وضعیت فعال‌سازی
    $expect = "false"
    if ($script:ActCode) { $expect = "true" }
    if ($null -ne $r -and $r -notmatch ("`"activated`: *" + $expect)) { $script:CheckFailures += "activation" }

    # ۶) لاگین ادمین
    $body = @{ username = $script:AdminUser; password = $script:AdminPass } | ConvertTo-Json -Compress
    $r = Invoke-JsonPost "$base/api/login" $body
    if ($null -eq $r -or $r -notmatch '"ok": *true') { $script:CheckFailures += "login" }

    # ۷) صفحهٔ پنل مدیریت باید سرو شود (HTML)
    try {
        $panelHtml = (Invoke-WebRequest -UseBasicParsing -TimeoutSec 10 -Uri "$base/").Content
        if ($panelHtml -notmatch "سامانهٔ ویزیتور|<title>") { $script:CheckFailures += "panel" }
    } catch { $script:CheckFailures += "panel" }

    # ۸) تنظیمات اتصال (بدون رمز) باید خوانده شود
    $r = Invoke-JsonGet "$base/api/db/info"
    if ($null -eq $r -or $r -notmatch '"ok": *true') { $script:CheckFailures += "dbinfo" }

    # بررسی اتصال از بیرون (فقط هشدار)
    if ($script:PublicIp -and $script:PublicIp -ne $script:LocalIp) {
        $ext = "$($script:Proto)://$($script:Addr)"
        if ($script:Port -ne 80 -and $script:Port -ne 443) { $ext = "${ext}:$($script:Port)" }
        if (-not (Invoke-JsonGet "$ext/api/ping")) {
            Write-Warn "آدرس خارجی $ext/api در این لحظه پاسخ نداد (ممکن است فایروال ابر، پورت‌فوروردینگ یا DNS مشکل داشته باشد — اتصال داخلی درست است)"
        } else {
            Write-Ok "آدرس خارجی برای اینترنت: $ext/api"
        }
    }
}

function Repair {
    foreach ($f in $script:CheckFailures) {
        switch -Regex ($f) {
            'process|ping' {
                Write-Info "تعمیر: راه‌اندازی مجدد سرویس ..."
                Restart-VizitorService
                Start-Sleep -Seconds 3
                if (-not (Test-PortUp $script:InternalPort)) {
                    foreach ($tryPort in @(9597, 9598, 9600, 9601, 9602)) {
                        if (Test-PortFree $tryPort) {
                            Write-Info "پورت $($script:PublicPort) درگیر است؛ پورت $tryPort انتخاب می‌شود"
                            $script:InternalPort = $tryPort
                            $script:PublicPort = $tryPort
                            $script:ApiUrl = "$($script:Proto)://$($script:Addr):$($script:PublicPort)/api"
                            Write-ConfigJson | Out-Null
                            Restart-VizitorService
                            Start-Sleep -Seconds 3
                            break
                        }
                    }
                }
                break
            }
            'config' {
                Write-Info "تعمیر: بازنویسی config.json و راه‌اندازی مجدد ..."
                Write-ConfigJson | Out-Null
                Seed-Db
                Restart-VizitorService
                Start-Sleep -Seconds 3
                break
            }
            'db' {
                Write-Info "تعمیر: بازبینی دیتابیس ..."
                if ($script:DbEngine -eq "sqlserver") {
                    Provision-SqlServer | Out-Null
                }
                Seed-Db
                Restart-VizitorService
                Start-Sleep -Seconds 3
                break
            }
            'activation|login' {
                Write-Info "تعمیر: دوباره درج حساب ادمین و کد فعال‌سازی ..."
                Seed-Db
                Restart-VizitorService
                Start-Sleep -Seconds 3
                break
            }
        }
    }
}

$verifyOk = $false
for ($round = 1; $round -le 4; $round++) {
    Run-Checks
    $nFail = $script:CheckFailures.Count
    if ($nFail -eq 0) {
        $verifyOk = $true
        Write-Ok "بازرسی دور ${round}: همه موارد، موفق ✓"
        break
    }
    Write-Warn "بازرسی دور ${round}: $nFail مورد ناموفق ($($script:CheckFailures -join ', ')) — تنظیمات بازبینی و تعمیر می‌شوند ..."
    Repair
}
}   # پایان گزینهٔ «بازرسی و تعمیر خودکار»

# ---------------- بررسی سلامت اتصال به دیتابیس حسابداری -------------------
function Test-VizitorSqlHealth {
    if ($script:DbEngine -ne "sqlserver") { return }
    $erp = $script:ErpDb
    if (-not $erp) {
        Write-Info "دیتابیس حسابداری انتخاب نشده بود؛ بررسی سلامت رد شد (میان‌بر «اتصال مستقیم SQL» را بعداً اجرا کنید)."
        return
    }
    $jsonOut = Join-Path $script:DataDir "sql_health.json"
    Write-Info "بررسی سلامت اتصال و وجود جدول‌های لازم در دیتابیس [$erp] ..."
    $script:HealthOk = $false
    try {
        $prevEap = $ErrorActionPreference
        $ErrorActionPreference = "Continue"
        $out = & $script:PyExe "$script:AppHome\api\sql_admin_tools.py" health --config "$script:ConfigFile" --db $erp --out $jsonOut 2>&1
        $ErrorActionPreference = $prevEap
        $obj = $null
        if (Test-Path $jsonOut) { try { $obj = Get-Content $jsonOut -Raw | ConvertFrom-Json } catch {} }
        if ($obj) {
            if ($obj.ok) {
                $script:HealthOk = $true
                Write-Ok "اتصال به دیتابیس [$erp] سالم است (DB_NAME=$($obj.db_name) — سرور پاسخ داد)"
            } else {
                Write-Warn "بررسی سلامت: $($obj.error) $(if ($obj.detail) { "- $($obj.detail)" })"
            }
            if ($obj.checks) {
                $missing = @()
                foreach ($k in $obj.checks.PSObject.Properties.Name) {
                    if (-not $obj.checks.$k) { $missing += $k }
                }
                if ($missing.Count -eq 0) { Write-Ok "همهٔ جدول‌ها/پروسیجرهای لازم موجودند ✓" }
                else { Write-Warn "این موارد در دیتابیس [$erp] پیدا نشد: $($missing -join ', ')" }
            }
        } else {
            foreach ($l in $out) { Write-Info "$l" }
            Write-Warn "خروجی بررسی سلامت خوانده نشد"
        }
    } catch {
        Write-Warn "بررسی سلامت اتصال انجام نشد: $($_.Exception.Message)"
    }
    Write-Info "نتیجهٔ کامل (بدون رمز): $jsonOut"
}

# ---------------------------- فایل اتصال (بدون رمز) -----------------------
try {
    $connectFile = Join-Path $script:AppHome "connect.txt"
    $lines = @()
    $lines += "[connection]"
    $lines += "apiurl=$script:ApiUrl"
    $lines += "configurl=$($script:ApiUrl -replace '/api$', '/api/config')"
    $lines += "server=$script:Addr"
    $lines += "port=$script:PublicPort"
    $lines += "internalport=$script:InternalPort"
    $lines += "dbengine=$script:DbEngine"
    if ($script:DbEngine -eq "sqlserver") {
        $lines += "sqldatabase=$script:DbName"
        $lines += "sqlserver=$script:DbHost,$script:DbDPort"
    }
    if ($script:AndroidOk) {
        $lines += "androidsqlserver=$script:Addr,1433"
        if ($script:HostPublic -or $script:PublicIp) {
            $lines += "androidsqlserverpublic=$(if ($script:HostPublic) { $script:HostPublic } else { $script:PublicIp }),1433"
        }
        $lines += "androidexternal=$([int]$script:AndroidExternal)"
        $lines += "androidsqldatabase=$script:ErpDb"
        $lines += "androidsqllogin=$script:AndroidLogin"
        $lines += "androidsqlpasswordfile=$script:DataDir\android_app_password.txt"
    }
    $lines += "datadir=$script:DataDir"
    $lines += "apphome=$script:AppHome"
    $lines += "installed=$(Get-Date -Format 'yyyy-MM-dd HH:mm')"
    # بدون BOM نوشته می‌شود تا NSIS بتواند با ReadINIStr بخواند (مقادیر همه ASCII هستند)
    $enc = New-Object System.Text.UTF8Encoding($false)
    [System.IO.File]::WriteAllText($connectFile, (($lines -join "`r`n") + "`r`n"), $enc)
    Write-Info "فایل اتصال (بدون رمز) نوشته شد: $connectFile"
} catch {
    Write-Warn "نوشتن فایل اتصال ممکن نشد: $($_.Exception.Message)"
}

# بررسی سلامت اتصال (تیک «بررسی سلامت اتصال» در نصب‌کننده)
$script:HealthOk = $false
if ($script:HealthWanted) { Test-VizitorSqlHealth }

# سخت‌تر کردن دسترسی پوشهٔ تنظیمات (رمز دیتابیس داخل config.json است)
try { & icacls $script:DataDir /inheritance:r /grant:r "*S-1-5-18:(OI)(CI)F" "*S-1-5-32-544:(OI)(CI)F" | Out-Null } catch {}

# ---------------------------- گزارش نهایی ---------------------------------
Write-Step "گزارش نهایی"

function Write-Hr { Write-Host "------------------------------------------------------------" }

if ($verifyOk) {
    Write-Host ""
    Write-Host "  ✓  نصب Vizitor با موفقیت انجام شد و همه بازرسی‌ها پاس شدند" -ForegroundColor Green
    Write-Host ""
} else {
    Write-Host ""
    Write-Host "  !!  نصب انجام شد اما برخی بازرسی‌ها هنوز ناموفق‌اند" -ForegroundColor Red
    Write-Info "فهرست مشکلات: $($script:CheckFailures -join ', ')"
    Write-Info "لاگ سرویس: $script:LogFile"
    Write-Info "برای بازرسی مجدد:  powershell -ExecutionPolicy Bypass -File `"$PSScriptRoot\install.ps1`" -Recheck"
    Write-Host ""
}

Write-Hr
Write-Host "  اتصال برنامهٔ اندروید: مستقیم به SQL Server روی پورت 1433 (بدون IIS و بدون API)"
Write-Host "     سرور: $script:Addr   |   دیتابیس حسابداری: $script:ErpDb   |   کاربر: $script:AndroidLogin" -ForegroundColor Green
Write-Host "  پنل مدیریت و وضعیت سامانه (اختیاری):"
Write-Host "     پنل مدیریت  : $($script:ApiUrl -replace '/api$', '')" -ForegroundColor Cyan
Write-Host "     بررسی سلامت : $($script:ApiUrl -replace '/api$', '/api/health')" -ForegroundColor Cyan
Write-Hr
if ($script:DbEngine -eq "sqlserver") {
    Write-Host "  دیتابیس      : sqlserver  ($($script:DbName) @ $($script:DbHost):$($script:DbDPort) ، احراز: $($script:DbAuth) ، کاربر: $($script:DbUser))"
} else {
    Write-Host "  دیتابیس      : sqlite  ($(Join-Path $script:DataDir 'vizitor.db'))"
}
Write-Host "  حساب ادمین  : $($script:AdminUser) / $($script:AdminPass)"
if ($script:ActCode) {
    Write-Host "  کد فعال‌سازی: $($script:ActCode) (فعال)"
} else {
    Write-Host "  کد فعال‌سازی: خالی است — از برنامه اندروید وارد کنید یا:" -ForegroundColor Yellow
    Write-Host "     curl -X POST $script:ApiUrl/activate -d '{""code"":""CODE-شما""}'"
}
Write-Hr
if ($script:AndroidOk) {
    Write-Host "  اتصال مستقیم اندروید به SQL Server:"
    Write-Host "     $($script:Addr),1433  /  دیتابیس $($script:ErpDb)  /  کاربر $($script:AndroidLogin)" -ForegroundColor Green
    Write-Host "     رمز: در فایل $script:DataDir\android_app_password.txt (فقط مدیر؛ در گزارش‌ها چاپ نمی‌شود)"
}
Write-Host "  کارت اتصال  : $(Join-Path $script:AppHome 'setup\android-connect.txt')"
Write-Host "  تصویر QR    : $(Join-Path $script:AppHome 'setup\android-connect.png')"
Write-Host "  فایل اتصال  : $(Join-Path $script:AppHome 'connect.txt')"
if ($script:DbEngine -eq "sqlserver") {
    if ($script:HealthOk) { Write-Host "  سلامت اتصال : تایید شد ✓ (دیتابیس $($script:ErpDb))" -ForegroundColor Green }
    else { Write-Host "  سلامت اتصال : تایید نشد — جزئیات در $(Join-Path $script:DataDir 'sql_health.json')" -ForegroundColor Yellow }
}
Write-Host "  فایل تنظیمات : $script:ConfigFile"
Write-Host "  لاگ سرویس   : $script:LogFile"
Write-Host "  فایل‌های برنامه: $script:AppHome"
Write-Host "  مدیریت سرویس : Start-ScheduledTask -TaskName VizitorAPI"
Write-Host "                  Stop-ScheduledTask -TaskName VizitorAPI"
Write-Host "                  Get-ScheduledTask -TaskName VizitorAPI"
Write-Hr
Write-Host "  راهنمای اندروید: در بخش اتصال API برنامه، آدرس $script:ApiUrl را وارد کنید."
if (-not $script:PublicIp) {
    Write-Host "  ⚠ آی‌پی عمومی شناسایی نشد؛ $script:ApiUrl داخل شبکه محلی کار می‌کند. برای دسترسی از بیرون،" -ForegroundColor Yellow
    Write-Host "     یک دامنه/IP ثابت + پورت‌فورورد روی پورت $($script:PublicPort) تنظیم کنید." -ForegroundColor Yellow
}
Write-Host ""

if ($verifyOk) { exit 0 } else { exit 1 }
