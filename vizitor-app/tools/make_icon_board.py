#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
ساخت «تختهٔ طراحی آیکن» نسخهٔ ۲٫۱۳٫۷ برای بازبینی بصری سریع.
خروجی: vizitor-app/design/icon-board-2.13.7.png
(متن‌های تخته لاتین‌اند تا بدون موتور شکل‌دهی فارسی هم درست رندر شوند.)
"""
import os
import subprocess
import sys

REPO = "/home/user/Vizitor"
RES = f"{REPO}/vizitor-app/app/src/main/res"
DESIGN = f"{REPO}/vizitor-app/design"
TMP = "/tmp/icon_build/board"
OUT = f"{DESIGN}/icon-board-2.13.7.png"

OLD = f"{DESIGN}/icon-m-v5-1024.png"            # آیکن نسخهٔ ۲٫۱۳٫۶ (نسخهٔ قبلی)
NEW = f"{DESIGN}/icon-m-v6-1024.png"            # آیکن نسخهٔ ۲٫۱۳٫۷
ADAPT_FG = f"{RES}/drawable-nodpi/ic_launcher_foreground.png"
ADAPT_BG = f"{RES}/drawable-nodpi/ic_launcher_background.png"
LEGACY = f"{RES}/mipmap-xxxhdpi/ic_launcher.png"
ROUND = f"{RES}/mipmap-xxxhdpi/ic_launcher_round.png"
MONO = f"{RES}/mipmap-xxxhdpi/ic_launcher_monochrome.png"

BG = "#0B0E13"
CARD = "#12161D"
GOLD = "#FFD166"
TXT = "#F2F4F8"
MUT = "#9AA3B2"


def run(*args):
    cmd = ["convert"] + [str(a) for a in args]
    r = subprocess.run(cmd, capture_output=True)
    if r.returncode != 0:
        print("FAILED:", " ".join(cmd[:10]), "…")
        print(r.stderr.decode()[:1200])
        sys.exit(1)


os.makedirs(TMP, exist_ok=True)
W, H = 1600, 1215
run("-size", f"{W}x{H}", f"xc:{BG}", f"{TMP}/bg.png")

# ── کارت‌ها ────────────────────────────────────────────────────────────────
def card(x, y, w, h, title, out):
    run("-size", f"{w}x{h}", f"xc:{CARD}", "-fill", "none",
        "-stroke", "#2A3344", "-strokewidth", "2",
        "-draw", f"roundrectangle 1,1,{w-2},{h-2},22,22", f"{TMP}/{out}_card.png")
    run(f"{TMP}/{out}_card.png", "-font", "DejaVu-Sans-Bold", "-pointsize", "26",
        "-fill", GOLD, "-gravity", "north", "-annotate", "+0+18", title,
        f"{TMP}/{out}.png")


card(60, 150, 700, 560, "NEW 2.13.7  —  jewel plate + 3D M", "c_new")
card(840, 150, 700, 560, "PREVIOUS 2.13.6  —  flat plate", "c_old")
card(60, 740, 1480, 380, "ADAPTIVE ICON LAYERS  +  LEGACY / ROUND / MONOCHROME", "c_adapt")

# ── محتوای کارت‌ها ─────────────────────────────────────────────────────────
run(NEW, "-resize", "420x420", f"{TMP}/new420.png")
run(f"{TMP}/c_new.png", f"{TMP}/new420.png", "-gravity", "center",
    "-geometry", "+0+40", "-compose", "over", "-composite", f"{TMP}/c_new2.png")

run(OLD, "-resize", "420x420", f"{TMP}/old420.png")
run(f"{TMP}/c_old.png", f"{TMP}/old420.png", "-gravity", "center",
    "-geometry", "+0+40", "-compose", "over", "-composite", f"{TMP}/c_old2.png")

# لایه‌های آیکن تطبیقی + قدیمی + دایره‌ای + تک‌رنگ
run(ADAPT_BG, "-resize", "220x220", f"{TMP}/bg220.png")
run(f"{TMP}/bg220.png", ADAPT_FG, "-resize", "220x220", "-gravity", "center",
    "-compose", "over", "-composite", f"{TMP}/adaptive220.png")
run(LEGACY, "-resize", "140x140", f"{TMP}/legacy140.png")
run(ROUND, "-resize", "140x140", f"{TMP}/round140.png")
run(MONO, "-resize", "140x140", f"{TMP}/mono140.png")
# سایزهای واقعی لانچر
run(LEGACY, "-resize", "48x48", f"{TMP}/s48.png")
run(LEGACY, "-resize", "72x72", f"{TMP}/s72.png")
run(LEGACY, "-resize", "96x96", f"{TMP}/s96.png")
run(LEGACY, "-resize", "144x144", f"{TMP}/s144.png")

run(f"{TMP}/c_adapt.png", f"{TMP}/adaptive220.png", "-geometry", "+70+62",
    "-compose", "over", "-composite",
    f"{TMP}/legacy140.png", "-geometry", "+340+98", "-compose", "over", "-composite",
    f"{TMP}/round140.png", "-geometry", "+510+98", "-compose", "over", "-composite",
    f"{TMP}/mono140.png", "-geometry", "+680+98", "-compose", "over", "-composite",
    f"{TMP}/s144.png", "-geometry", "+880+96", "-compose", "over", "-composite",
    f"{TMP}/s96.png", "-geometry", "+1050+120", "-compose", "over", "-composite",
    f"{TMP}/s72.png", "-geometry", "+1180+132", "-compose", "over", "-composite",
    f"{TMP}/s48.png", "-geometry", "+1290+144", "-compose", "over", "-composite",
    "-font", "DejaVu-Sans", "-pointsize", "20", "-fill", MUT,
    "-annotate", "+74+300", "adaptive 1024",
    "-annotate", "+344+302", "square 192",
    "-annotate", "+514+302", "round 192",
    "-annotate", "+684+302", "monochrome",
    "-annotate", "+884+288", "192", "-annotate", "+1054+312", "144",
    "-annotate", "+1184+324", "96", "-annotate", "+1294+336", "48",
    f"{TMP}/c_adapt2.png")

# ── سرصفحه و پاصفحه ────────────────────────────────────────────────────────
run(f"{TMP}/bg.png", f"{TMP}/c_new2.png", "-geometry", "+60+150", "-compose", "over", "-composite",
    f"{TMP}/c_old2.png", "-geometry", "+840+150", "-compose", "over", "-composite",
    f"{TMP}/c_adapt2.png", "-geometry", "+60+740", "-compose", "over", "-composite",
    "-font", "DejaVu-Sans-Bold", "-pointsize", "46", "-fill", GOLD,
    "-gravity", "northwest", "-annotate", "+60+50", "ATIRAN VIZITOR  —  3D APP ICON  2.13.7",
    "-font", "DejaVu-Sans", "-pointsize", "24", "-fill", TXT,
    "-annotate", "+60+105", "purple jewel plate  ·  double gold ring  ·  violet glow  ·  glossy gold M with pistachio  ·  safe-zone sized",
    "-font", "DejaVu-Sans", "-pointsize", "20", "-fill", MUT,
    "-gravity", "southwest", "-annotate", "+60+78",
    "Milad Yaghoobi  ·  Meelano Studio Design",
    "-annotate", "+60+50",
    "every density: mdpi 48 / hdpi 72 / xhdpi 96 / xxhdpi 144 / xxxhdpi 192",
    "-annotate", "+60+22", "regenerate:  python3 vizitor-app/tools/make_icon.py",
    OUT)

run(OUT, "-strip", OUT)
print(f"OK  {os.path.getsize(OUT)} B  {OUT}")
