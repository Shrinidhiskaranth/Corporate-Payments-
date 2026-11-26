@echo off
REM Starts everything needed for the Corporate Payments app on Windows:
REM Docker Desktop -> MySQL container -> Spring Boot app on http://localhost:8080
REM Double-click this file, or run it from a terminal. Close the window (or press Ctrl+C) to stop the app.

cd /d "%~dp0"

echo [1/3] Starting Docker Desktop...
docker info >nul 2>&1
if errorlevel 1 (
    start "" "C:\Program Files\Docker\Docker\Docker Desktop.exe"
    :waitdocker
    timeout /t 5 /nobreak >nul
    docker info >nul 2>&1
    if errorlevel 1 goto waitdocker
)

echo [2/3] Starting MySQL...
docker start corporate-mysql >nul 2>&1
if errorlevel 1 (
    echo MySQL container not found - creating it...
    docker run -d --name corporate-mysql -e MYSQL_ROOT_PASSWORD=your_password -e MYSQL_DATABASE=corporate_payments -p 3306:3306 mysql:8.0
)
:waitmysql
timeout /t 3 /nobreak >nul
docker exec corporate-mysql mysqladmin ping -uroot -pyour_password --silent >nul 2>&1
if errorlevel 1 goto waitmysql

REM Build a JAR and run that copy, so VS Code recompiling target\classes in the background
REM cannot break the running app.
echo [3/4] Building the app...
call mvnw.cmd -q -DskipTests clean package
if errorlevel 1 (
    echo Build failed - see the messages above.
    pause
    exit /b 1
)

echo [4/4] Starting the app - open http://localhost:8080 when you see "Started CorporatePaymentsApplication"
java -jar target\corporate-payments-0.0.1-SNAPSHOT.jar
pause
