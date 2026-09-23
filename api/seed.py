#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Seeds the Vizitor database from config.json:
  - admin user (create or update password)
  - activation code + activated flag (if a code is present in the config)
  - base settings (api_url, app_name)
Usage: python3 seed.py [--config /path/to/config.json]
"""
import argparse
import os
import sys
import time

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from db import DB, default_config_path, hash_password, load_config  # noqa: E402


def main():
    parser = argparse.ArgumentParser(description="Vizitor DB seeder")
    parser.add_argument("--config", default=None)
    args = parser.parse_args()

    cfg = load_config(args.config or default_config_path())
    db = DB(cfg)

    # admin user -----------------------------------------------------------
    admin = cfg.get("admin") or {}
    username = str(admin.get("username", "admin")).strip() or "admin"
    password = str(admin.get("password", ""))
    if not password:
        print("seed: warning — no admin password in config; admin password not set")
    else:
        rows = db.q("SELECT id FROM users WHERE username = ?", (username,))
        if rows:
            db.execute(
                "UPDATE users SET password_hash = ?, is_admin = 1 WHERE id = ?",
                (hash_password(password), rows[0]["id"]),
            )
        else:
            db.execute(
                "INSERT INTO users (username, password_hash, is_admin) VALUES (?, ?, 1)",
                (username, hash_password(password)),
            )

    # activation ------------------------------------------------------------
    activation = cfg.get("activation") or {}
    code = str(activation.get("code", "")).strip()
    if code:
        db.upsert_setting("activation_code", code)
        db.upsert_setting("activated", "1")
        if not db.get_setting("activated_at"):
            db.upsert_setting("activated_at", time.strftime("%Y-%m-%d %H:%M:%S"))
    else:
        if db.get_setting("activated") != "1":
            db.upsert_setting("activated", "0")

    # base settings ----------------------------------------------------------
    db.upsert_setting("api_url", (cfg.get("api") or {}).get("url", ""))
    db.upsert_setting("app_name", (cfg.get("app") or {}).get("name", "Vizitor"))

    print(
        "seed ok: admin=%s activated=%s engine=%s"
        % (username, db.get_setting("activated"), (cfg.get("db") or {}).get("engine"))
    )


if __name__ == "__main__":
    main()
