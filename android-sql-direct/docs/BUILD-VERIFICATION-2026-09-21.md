# Build / Static Verification — 2026-09-21

## Passed
- Balanced Kotlin braces/parentheses/brackets for all modified Kotlin files.
- Targeted Kotlin parser checks on the core changed files produced no parser-level errors. Standalone `kotlinc` still reports unresolved Android/Compose symbols because Gradle dependencies are not loaded in this environment; these are classpath diagnostics, not syntax failures.
- Direct SQL path references verified for `1433`, SQL username/password, database selection, visitor login, `add_sail_pish`, and `subsailtemp_pish`.
- No remaining executable references to the old synthetic `SeedData` datasets in the Android main source tree.
- The old fake Cart submit toast is removed; the cart button now calls `registerPreInvoice()`.
- The final customer picker excludes `pendingApproval` customers from ERP pre-invoice selection.
- The SQL setup script grants only `EXECUTE` on `dbo.add_sail_pish` and `INSERT` on `dbo.subsailtemp_pish` in addition to `db_datareader`.

## Not runnable in this environment
The Gradle wrapper is present, but Gradle 8.9 is not cached locally. Running `vizitor-app/gradlew --offline --version` therefore cannot resolve the distribution, and online download fails because this execution environment cannot resolve `services.gradle.org`.

## Required on the target build machine
1. Run `vizitor-app/gradlew clean assembleDebug` (or `assembleRelease`) with network access or a cached Gradle 8.9 distribution.
2. Install the APK on a small Android phone and a large Android phone.
3. Connect to the real SQL Server at `host:1433` using the dedicated SQL login.
4. Select the target ERP database.
5. Log in with the real visitor credentials from `dbo.sys_users`.
6. Run one test pre-invoice and verify it in the ERP UI.
