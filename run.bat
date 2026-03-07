@echo off

:menu
echo.
echo ==============================
echo    SMART BUILDING - MTIS P2
echo ==============================
echo 1. Levantar TODO
echo 2. Levantar Central (Java)
echo 3. Levantar Oficina 1 (Python)
echo 4. Levantar Oficina 2 (Node.js)
echo 0. Salir
echo.
set /p opcion="Selecciona una opcion: "

if "%opcion%"=="1" goto all
if "%opcion%"=="2" goto central
if "%opcion%"=="3" goto oficina1
if "%opcion%"=="4" goto oficina2
if "%opcion%"=="0" goto fin
goto menu

:all
call :central
timeout /t 2 /nobreak >nul
call :oficina1
call :oficina2
goto menu

:central
echo Compilando Central...
javac -cp "%~dp0activemq-all-5.15.8.jar" "%~dp0central\Central.java"
if %errorlevel% neq 0 (
    echo ERROR: Fallo la compilacion de Central.
    pause
    goto :eof
)
echo Compilacion exitosa. Levantando Central...
start "Central (Java)" cmd /k "cd /d %~dp0 && java -cp .;activemq-all-5.15.8.jar central.Central"
goto :eof

:oficina1
start "Oficina 1 (Python)" cmd /k "cd /d %~dp0oficina1 && python oficina1.py"
goto :eof

:oficina2
start "Oficina 2 (Node.js)" cmd /k "cd /d %~dp0oficina2 && node oficina2.js"
goto :eof

:fin
exit
