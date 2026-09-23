#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$ROOT"

grep -q 'port: Int = 1433' vizitor-app/app/src/main/java/ir/atiran/vizitor/sqldirect/SqlConnectionManager.kt
grep -q 'registerPreInvoice' vizitor-app/app/src/main/java/ir/atiran/vizitor/VizitorViewModel.kt
grep -q 'dbo.add_sail_pish' vizitor-app/app/src/main/java/ir/atiran/vizitor/sqldirect/MeelanoDataSource.kt
grep -q 'dbo.subsailtemp_pish' vizitor-app/app/src/main/java/ir/atiran/vizitor/sqldirect/MeelanoDataSource.kt
grep -q 'GRANT EXECUTE ON OBJECT::dbo.add_sail_pish' android-sql-direct/sql/01_setup_vizitor_user.sql
grep -q 'GRANT INSERT ON OBJECT::dbo.subsailtemp_pish' android-sql-direct/sql/01_setup_vizitor_user.sql
if grep -RIn --exclude-dir=build 'SeedData' vizitor-app/app/src/main/java >/tmp/vizitor_seed_refs.txt; then
  echo 'Unexpected SeedData reference:'; cat /tmp/vizitor_seed_refs.txt; exit 1
fi

echo 'FINAL_SANITY=PASS'
