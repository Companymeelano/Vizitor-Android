#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
ساخت آیکن سه‌بعدی لاکچری برنامه — نسخهٔ ۲٫۱۳٫۷
================================================
طرح: پلاک جواهری بنفش (گرادیان شعاعی + وینیت + براقیت شیشه‌ای) + دو حلقهٔ نازک
طلایی + نشان سه‌بعدی طلایی «M» با مغز پسته (هالهٔ بنفش، سایهٔ تماس، براقیت).

قواعد ایمنی آیکن تطبیقی اندروید (مهم):
  • ناحیهٔ امن = دایرهٔ مرکزی به شعاع ۳۱۰ از ۱۰۲۴ (۳۰٪) که هیچ ماسکی آن را نمی‌برد
  • نشان ۵۴۰ پیکسلی: نصف قطر قطرِ جعبه = ۳۳۷ < ۳۴۱ ⇒ گوشه‌های نشان هم سالم می‌مانند
  • حلقهٔ طلایی روی شعاع ۳۱۰ رسم می‌شود تا زیر ماسک دایره/مربع گوشه‌گرد کامل دیده شود
  • هاله/سایه/براقیت با ماسک تمام‌قاب ساخته می‌شوند (بدون لکهٔ مستطیلی)

اجرا:  python3 vizitor-app/tools/make_icon.py
پیش‌نیاز: ImageMagick 6 (`convert`) و تصویر خام نشان در /tmp/icon_build/art_src.png
"""
import os
import subprocess
import sys

REPO = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
RES = f"{REPO}/vizitor-app/app/src/main/res"
DESIGN = f"{REPO}/vizitor-app/design"
TMP = "/tmp/icon_build"

SRC_ART = f"{TMP}/art_src.png"          # نشان خام (۴۳۲×۳۲۶، با آلفا)
PLATE_OUT = f"{RES}/drawable-nodpi/ic_launcher_background.png"

# پالت برنامه (Royal Dark): بنفش نئونی + طلایی
P_TOP = "rgba(180,112,255,1)"
P_BOT = "rgba(22,11,40,1)"
GOLD_HI = "rgb(255,242,190)"
GOLD_DK = "rgba(176,127,22,0.92)"

S = 1024            # اندازهٔ سند اصلی
ART_W = 540         # نشانِ لایهٔ تطبیقی (داخل ناحیهٔ امن)
ART_LEGACY = 560    # نشانِ آیکن مربع/دایره‌ای قدیمی (بزرگ‌تر)


def run(*args):
    cmd = ["convert"] + [str(a) for a in args]
    r = subprocess.run(cmd, capture_output=True)
    if r.returncode != 0:
        print("FAILED:", " ".join(cmd[:12]), "…")
        print(r.stderr.decode()[:1200])
        sys.exit(1)


def colored_from_mask(color, mask_expr, out):
    """
    ساخت یک لایهٔ تمام‌قاب که رنگش [color] است و شفافیتش از [mask_expr]
    (یک عبارت ImageMagick با پرانتز) می‌آید — بدون هیچ لبهٔ مستطیلی.
    """
    run("-size", f"{S}x{S}", "xc:black", "-gravity", "center", *mask_expr,
        "-compose", "over", "-composite", f"{TMP}/_mgray.png")
    run("-size", f"{S}x{S}", f"xc:{color}", f"{TMP}/_mgray.png", "-alpha", "off",
        "-compose", "CopyOpacity", "-composite", out)


def softened_mask(resize, blur):
    return ("(", f"{TMP}/art_m.png", "-alpha", "extract",
            "-resize", resize, "-blur", blur, ")")


os.makedirs(TMP, exist_ok=True)
if not os.path.exists(SRC_ART):
    print(f"نشان خام پیدا نشد: {SRC_ART}")
    sys.exit(2)

# ── ۰) آماده‌سازی نشان ──────────────────────────────────────────────────────
run(SRC_ART, "-trim", "+repage", f"{TMP}/art.png")
run(f"{TMP}/art.png", "-alpha", "extract", f"{TMP}/art_mask.png")
run(f"{TMP}/art.png", "-resize", f"{ART_W}x{ART_W}", f"{TMP}/art_m.png")
run(f"{TMP}/art.png", "-resize", f"{ART_LEGACY}x{ART_LEGACY}", f"{TMP}/art_legacy.png")

# ── ۱) پلاک جواهری ─────────────────────────────────────────────────────────
run("-size", f"{S*2}x{S*2}", f"radial-gradient:{P_TOP}-{P_BOT}",
    "-crop", f"{S}x{S}+{int(S*0.42)}+{int(S*0.18)}", "+repage", f"{TMP}/p_base.png")
run(f"{TMP}/p_base.png",
    "(", "-size", f"{S}x{S}", "radial-gradient:rgba(6,3,12,0)-rgba(6,3,12,0.50)", ")",
    "-compose", "over", "-composite", f"{TMP}/p_vig.png")
run("-size", f"{S}x{S}", "gradient:rgba(255,255,255,0.20)-rgba(255,255,255,0)",
    "-resize", f"{S}x{int(S*0.52)}!", "-background", "none", "-gravity", "north",
    "-extent", f"{S}x{S}", "-blur", "30", f"{TMP}/p_sheen.png")
run(f"{TMP}/p_vig.png", f"{TMP}/p_sheen.png", "-compose", "over", "-composite",
    "-modulate", "106,120,100", f"{TMP}/p_sheen2.png")
run(f"{TMP}/p_sheen2.png",
    "-fill", "none", "-stroke", "rgba(255,209,102,0.28)", "-strokewidth", "9",
    "-draw", f"circle {S//2},{S//2} {S//2},182", "-blur", "7", f"{TMP}/p_r0.png")
run(f"{TMP}/p_r0.png",
    "-fill", "none", "-stroke", GOLD_DK, "-strokewidth", "9",
    "-draw", f"circle {S//2},{S//2} {S//2},202", "-blur", "6", f"{TMP}/p_r1.png")
run(f"{TMP}/p_r1.png",
    "-fill", "none", "-stroke", GOLD_HI, "-strokewidth", "3.6",
    "-draw", f"circle {S//2},{S//2} {S//2},202", "-blur", "1.2", f"{TMP}/plate.png")
run(f"{TMP}/plate.png", "-alpha", "remove", "-strip", PLATE_OUT)

# ── ۲) هاله، سایه و براقیت (همه تمام‌قاب) ──────────────────────────────────
run("-size", f"{S}x{S}", "radial-gradient:rgba(205,150,255,0.32)-rgba(205,150,255,0)",
    f"{TMP}/glow_grad.png")
colored_from_mask("none", softened_mask("130%", "44"), f"{TMP}/glow_raw.png")
run(f"{TMP}/glow_grad.png", f"{TMP}/glow_raw.png", "-compose", "over",
    "-composite", "-blur", "10", f"{TMP}/glow.png")

colored_from_mask("#150A24", softened_mask("122%", "26"), f"{TMP}/shadow.png")
run(f"{TMP}/shadow.png", "-evaluate", "multiply", "0.85", f"{TMP}/shadow2.png")

run("-size", f"{S}x{S}", "gradient:rgba(255,255,255,0.42)-rgba(255,255,255,0)",
    f"{TMP}/gloss_grad.png")
colored_from_mask("none", ("(", f"{TMP}/art_m.png", "-alpha", "extract", ")"),
                  f"{TMP}/gloss_raw.png")
run(f"{TMP}/gloss_grad.png", f"{TMP}/gloss_raw.png", "-compose", "over",
    "-composite", "-evaluate", "multiply", "0.55", f"{TMP}/gloss.png")


# ── ۳) ترکیب نهایی سطوح ────────────────────────────────────────────────────
def compose_final(plate, out, art, mask=None, gloss_amount="32"):
    """پلاک ← هاله ← سایه ← نشان ← براقیت روی نشان. همه لایه‌ها هم‌اندازه."""
    run(plate, f"{TMP}/glow.png", "-compose", "over", "-composite", f"{TMP}/c1.png")
    run(f"{TMP}/c1.png", f"{TMP}/shadow2.png", "-compose", "over", "-composite", f"{TMP}/c2.png")
    run(f"{TMP}/c2.png", art, "-gravity", "center", "-compose", "over",
        "-composite", f"{TMP}/c3.png")
    run(f"{TMP}/c3.png", f"{TMP}/gloss.png", "-compose", "Dissolve",
        "-define", f"compose:args={gloss_amount}", "-composite", f"{TMP}/c4.png")
    if mask == "round":
        run("-size", f"{S}x{S}", "xc:none", "-fill", "white",
            "-draw", f"roundrectangle 0,0,{S-1},{S-1},{int(S*0.235)},{int(S*0.235)}",
            f"{TMP}/mask.png")
        run(f"{TMP}/c4.png", f"{TMP}/mask.png", "-alpha", "off",
            "-compose", "CopyOpacity", "-composite", "-strip", out)
    elif mask == "circle":
        run("-size", f"{S}x{S}", "xc:none", "-fill", "white",
            "-draw", f"circle {S//2},{S//2} {S//2},0", f"{TMP}/mask.png")
        run(f"{TMP}/c4.png", f"{TMP}/mask.png", "-alpha", "off",
            "-compose", "CopyOpacity", "-composite", "-strip", out)
    else:
        run(f"{TMP}/c4.png", "-strip", out)


compose_final(f"{TMP}/plate.png", f"{DESIGN}/icon-m-v6-1024.png", f"{TMP}/art_legacy.png")
compose_final(f"{TMP}/plate.png", f"{TMP}/master_square.png", f"{TMP}/art_legacy.png", mask="round")
compose_final(f"{TMP}/plate.png", f"{TMP}/master_round.png", f"{TMP}/art_legacy.png", mask="circle")

# ── ۴) لایهٔ پیش‌زمینهٔ تطبیقی (بدون پلاک) ─────────────────────────────────
run("-size", f"{S}x{S}", "xc:none", f"{TMP}/fg_c0.png")
run(f"{TMP}/fg_c0.png", f"{TMP}/glow.png", "-compose", "over", "-composite", f"{TMP}/fg_c1.png")
run(f"{TMP}/fg_c1.png", f"{TMP}/shadow2.png", "-compose", "over", "-composite", f"{TMP}/fg_c2.png")
run(f"{TMP}/fg_c2.png", f"{TMP}/art_m.png", "-gravity", "center", "-compose", "over",
    "-composite", f"{TMP}/fg_c3.png")
run(f"{TMP}/fg_c3.png", f"{TMP}/gloss.png", "-compose", "Dissolve",
    "-define", "compose:args=32", "-composite", f"{TMP}/fg_master.png")

# ── ۵) تولید چگالی‌ها ──────────────────────────────────────────────────────
DENS = {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}
FG = {"mdpi": 108, "hdpi": 162, "xhdpi": 216, "xxhdpi": 324, "xxxhdpi": 432}

for d, sz in DENS.items():
    run(f"{TMP}/master_square.png", "-resize", f"{sz}x{sz}", "-strip",
        f"{RES}/mipmap-{d}/ic_launcher.png")
    run(f"{TMP}/master_round.png", "-resize", f"{sz}x{sz}", "-strip",
        f"{RES}/mipmap-{d}/ic_launcher_round.png")

for d, sz in FG.items():
    run(f"{TMP}/fg_master.png", "-resize", f"{sz}x{sz}", "-strip",
        f"{RES}/mipmap-{d}/ic_launcher_foreground.png")
    # لایهٔ تک‌رنگ اندروید ۱۳+ (تم‌شده): سیلوئت سفیدِ نشان با آلفای خود نشان
    # idiom مستند: تصویر سفید + ماسک خاکستری، خودِ شدت ماسک به آلفا تبدیل می‌شود
    half = int(sz * 0.52)
    run("-size", f"{sz}x{sz}", "xc:white",
        "(", f"{TMP}/art.png", "-resize", f"{half}x{half}", "-alpha", "extract",
        "-gravity", "center", "-background", "black", "-extent", f"{sz}x{sz}", ")",
        "-alpha", "off", "-compose", "CopyOpacity", "-composite", "-strip",
        f"{RES}/mipmap-{d}/ic_launcher_monochrome.png")

# لایهٔ تطبیقی + نشان درون‌برنامه‌ای (drawable-nodpi)
run(f"{TMP}/fg_master.png", "-resize", "432x432", "-strip",
    f"{RES}/drawable-nodpi/ic_launcher_foreground.png")
run(f"{TMP}/fg_c3.png", "-resize", "512x512", "-strip",
    f"{RES}/drawable-nodpi/brand_mark.png")

# ── ۶) پاک‌سازی فایل‌های موقت سنگین (فضای کاری سبک بماند) ───────────────────
for name in ("p_base.png", "p_sheen.png", "p_vig.png", "glow_grad.png",
             "gloss_grad.png", "glow_raw.png", "gloss_raw.png", "_mgray.png",
             "c1.png", "c2.png", "c3.png", "c4.png"):
    f = f"{TMP}/{name}"
    if os.path.exists(f):
        os.remove(f)

print("=== آیکن‌ها ساخته شد ===")
for p in [PLATE_OUT, f"{RES}/drawable-nodpi/ic_launcher_foreground.png",
          f"{RES}/drawable-nodpi/brand_mark.png",
          f"{DESIGN}/icon-m-v6-1024.png",
          f"{RES}/mipmap-xxxhdpi/ic_launcher.png"]:
    print(f"{os.path.getsize(p):>9} B  {p}")
