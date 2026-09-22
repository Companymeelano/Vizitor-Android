@echo off
setlocal
REM  *** THIS IS A WINDOWS BATCH FILE. RUN THE FILE - DO NOT COPY ITS TEXT. ***
REM  *** It is NOT SQL: do not open/run it in SSMS, and do not paste it into ***
REM  *** PowerShell. In File Explorer: right-click -> "Run as administrator". ***
REM ===========================================================================
REM  Vizitor - run the read-only audits on the server and write them to files
REM  ---------------------------------------------------------------------------
REM  HOW TO USE
REM    1. In File Explorer right-click this file -> "Run as administrator"
REM    2. The folder then contains the output files - send those .txt files back
REM
REM  It works in two modes, so a missing script is never a dead end:
REM    * if the .sql scripts (02, 03, 03b, 04, 05) sit next to this file, they
REM      are run and write out_02_gaps.txt / out_03_bodies.txt / out_05_small.txt
REM    * if they are not there, this file still dumps the four procedure bodies
REM      by itself (sqlcmd -Q, straight from sys.sql_modules) into body_*.txt
REM  The quick check (out_00_quick.txt: login mode, TCP listener, database) is
REM  always written.
REM
REM  Nothing is written to the database: every query is read-only.
REM ===========================================================================

cd /d "%~dp0"

set "DBSERVER=localhost"
REM  database to audit. Default Meelano; an argument overrides it, e.g.
REM      run_audit.bat Atiran14050603
set "DBNAME=%~1"
if "%DBNAME%"=="" set "DBNAME=Meelano"
set "SQLCMD="
set "FOUND="

where sqlcmd >nul 2>nul
if not errorlevel 1 set "SQLCMD=sqlcmd"
if not defined SQLCMD if exist "%ProgramFiles%\Microsoft SQL Server\Client SDK\ODBC\110\Tools\Binn\sqlcmd.exe" set "SQLCMD=%ProgramFiles%\Microsoft SQL Server\Client SDK\ODBC\110\Tools\Binn\sqlcmd.exe"
if not defined SQLCMD if exist "%ProgramFiles%\Microsoft SQL Server\110\Tools\Binn\sqlcmd.exe" set "SQLCMD=%ProgramFiles%\Microsoft SQL Server\110\Tools\Binn\sqlcmd.exe"
if not defined SQLCMD if exist "%ProgramFiles(x86)%\Microsoft SQL Server\110\Tools\Binn\sqlcmd.exe" set "SQLCMD=%ProgramFiles(x86)%\Microsoft SQL Server\110\Tools\Binn\sqlcmd.exe"
if not defined SQLCMD goto nosqlcmd
goto run

:nosqlcmd
echo.
echo *** sqlcmd was not found on this computer - nothing was run.
echo *** Alternative: open 03b_bodies_file.sql in SSMS on database Meelano,
echo *** press Ctrl+Shift+F first (Results to File), then F5.
echo.
pause
exit /b 1

:run
echo Running Vizitor audits against %DBNAME% on %DBSERVER% ...
echo.

echo [1/3] quick check (login mode, TCP listener, database)
"%SQLCMD%" -S %DBSERVER% -d %DBNAME% -E -h -1 -W -y 0 -o out_00_quick.txt -Q "SET NOCOUNT ON; SELECT 'server_ip=' + ISNULL(local_net_address, '?') + ':' + ISNULL(CAST(local_tcp_port AS varchar(6)), '?') + ' (the address the Android app must use)' FROM sys.dm_exec_connections WHERE session_id = @@SPID; SELECT 'database=' + DB_NAME() + ' | compat=' + CAST(compatibility_level AS varchar(4)) + ' | collation=' + CAST(collation_name AS varchar(60)) FROM sys.databases WHERE name = DB_NAME(); SELECT 'LoginMode_IsIntegratedSecurityOnly=' + CAST(SERVERPROPERTY('IsIntegratedSecurityOnly') AS varchar(2)) + ' (0 = mixed mode: SQL logins work)'; SELECT 'listener=' + ip_address + ':' + CAST(port AS varchar(6)) + ' | ' + state_desc FROM sys.dm_tcp_listener_states WHERE type_desc = 'TSQL'; SELECT 'sal_mali_rows=' + CAST(COUNT(*) AS varchar(6)) + ' | current_db=' + ISNULL(MAX(CASE WHEN [Current] = 1 THEN nam_db END), '<none>') FROM dbo.sal_mali;"
echo   -^> out_00_quick.txt

echo.
echo [2/3] the scripts next to this file
if exist 02_fill_gaps.sql set "FOUND=1"
if exist 02_fill_gaps.sql goto do02
echo   [skip] 02_fill_gaps.sql is not in "%CD%"
goto after02
:do02
"%SQLCMD%" -S %DBSERVER% -d %DBNAME% -E -i 02_fill_gaps.sql -o out_02_gaps.txt -y 0 -W
echo   -^> out_02_gaps.txt
:after02

if exist 03_dump_proc_bodies.sql set "FOUND=1"
if exist 03_dump_proc_bodies.sql goto do03
echo   [skip] 03_dump_proc_bodies.sql is not in "%CD%"
goto after03
:do03
"%SQLCMD%" -S %DBSERVER% -d %DBNAME% -E -i 03_dump_proc_bodies.sql -o out_03_bodies.txt -y 0 -W
echo   -^> out_03_bodies.txt
:after03

if exist 05_gaps_small.sql set "FOUND=1"
if exist 05_gaps_small.sql goto do05
echo   [skip] 05_gaps_small.sql is not in "%CD%"
goto after05
:do05
"%SQLCMD%" -S %DBSERVER% -d %DBNAME% -E -i 05_gaps_small.sql -o out_05_small.txt -y 0 -W
echo   -^> out_05_small.txt
:after05

if exist 04_port_check.sql set "FOUND=1"
if exist 04_port_check.sql goto do04
echo   [skip] 04_port_check.sql is not in "%CD%"
goto after04
:do04
"%SQLCMD%" -S %DBSERVER% -d %DBNAME% -E -i 04_port_check.sql -o out_04_port.txt -y 0 -W
echo   -^> out_04_port.txt
:after04

echo.
echo [3/3] procedure bodies
if exist out_03_bodies.txt goto bodiesdone
echo   The 03 script did not run in this folder, so the four bodies are dumped
echo   straight from the database instead (no .sql file needed):
for %%P in (add_sail_pish AddInvoice new_cust FixMojodi) do "%SQLCMD%" -S %DBSERVER% -d %DBNAME% -E -h -1 -W -y 0 -Q "SET NOCOUNT ON; SELECT m.definition FROM sys.sql_modules m JOIN sys.objects o ON o.object_id = m.object_id WHERE o.name = '%%P'" -o "body_%%P.txt"
if exist body_add_sail_pish.txt if exist body_AddInvoice.txt if exist body_new_cust.txt if exist body_FixMojodi.txt echo   -^> body_add_sail_pish.txt, body_AddInvoice.txt, body_new_cust.txt, body_FixMojodi.txt
if not exist body_add_sail_pish.txt echo   *** the dump produced no file - check the login (see below)
goto done

:bodiesdone
echo   -^> out_03_bodies.txt (already written by the 03 script)
goto done

:done
echo.
echo Finished. Files to send back, in this order of importance:
echo     body_add_sail_pish.txt, body_AddInvoice.txt, body_new_cust.txt,
echo     body_FixMojodi.txt          (or out_03_bodies.txt if it exists)
echo     out_00_quick.txt, out_05_small.txt, out_02_gaps.txt
echo.
echo If a file is empty or says "Login failed for user", run this file again but
echo as a user that has rights on the database, or use sqlcmd with -U AdminAn
echo (sqlcmd asks for the password itself - do not write it into any file).
echo.
pause
