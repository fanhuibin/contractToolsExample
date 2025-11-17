@echo off
chcp 936 >nul
title Zhaoxin Tools Demo Build Tool
cls

echo ==========================================
echo    Zhaoxin Tools Demo Build Tool
echo ==========================================
echo.

:: Check environment
echo [CHECK] Build environment...
where mvn >nul 2>&1 || (echo [ERROR] Maven not found, please install Maven & pause & exit /b 1)
where node >nul 2>&1 || (echo [ERROR] Node.js not found, please install Node.js & pause & exit /b 1)
where java >nul 2>&1 || (echo [ERROR] Java not found, please install JDK 11+ & pause & exit /b 1)
echo [PASS] Environment check completed (Java 11+, Maven, Node.js)
echo.

:: Select build mode
echo Please select build mode:
echo   [1] Full Build (clean+build+package)
echo   [2] Quick Build (build only)
echo   [3] Clean Cache
echo   [0] Exit
echo.
set /p "choice=Please enter choice (0-3): "

if "%choice%"=="1" goto full_build
if "%choice%"=="2" goto quick_build
if "%choice%"=="3" goto clean_only
if "%choice%"=="0" exit /b 0
echo [ERROR] Invalid choice & pause & exit /b 1

:clean_only
echo.
echo [CLEAN] Cleaning build cache...
if exist "backend\target" rmdir /s /q "backend\target"
if exist "frontend\dist" rmdir /s /q "frontend\dist"
if exist "dist" rmdir /s /q "dist"
echo [DONE] Clean completed
pause
exit /b 0

:quick_build
echo.
echo [BUILD] Quick build mode...
goto build_projects

:full_build
echo.
echo [BUILD] Full build mode...

:: Clean old files
echo [CLEAN] Cleaning old build files...
if exist "backend\target" rmdir /s /q "backend\target"
if exist "frontend\dist" rmdir /s /q "frontend\dist"
if exist "dist" rmdir /s /q "dist"
echo [DONE] Clean completed

:build_projects
:: Build backend
echo.
echo [BACKEND] Building backend project...
cd backend
call mvn clean package -DskipTests -q
if %errorlevel% neq 0 (
    echo [ERROR] Backend build failed
    cd ..
    pause
    exit /b 1
)
cd ..
echo [DONE] Backend build completed

:: Build frontend
echo [FRONTEND] Building frontend project...
cd frontend
if not exist "node_modules" (
    echo [INSTALL] Installing frontend dependencies...
    call npm install --silent
)
call npm run build --silent
if %errorlevel% neq 0 (
    echo [ERROR] Frontend build failed
    cd ..
    pause
    exit /b 1
)
cd ..
echo [DONE] Frontend build completed

:: If quick build, exit directly
if "%choice%"=="2" (
    echo.
    echo [SUCCESS] Quick build completed!
    echo   - Backend: backend\target\*.jar
    echo   - Frontend: frontend\dist\
    pause
    exit /b 0
)

:: Full build: create deployment package
echo.
echo [PACKAGE] Creating deployment package...
mkdir dist 2>nul
mkdir dist\backend 2>nul
mkdir dist\frontend 2>nul
mkdir dist\config 2>nul

:: Copy files
xcopy "backend\target\*.jar" "dist\backend\" /y >nul
xcopy "frontend\dist\*" "dist\frontend\" /s /e /y >nul
copy "backend\src\main\resources\application.yml" "dist\config\" >nul
copy "backend\src\main\resources\application-prod.yml" "dist\config\" >nul 2>nul || echo [INFO] No production config found

:: Create startup script
echo @echo off > "dist\start.bat"
echo title Zhaoxin Tools Demo Backend >> "dist\start.bat"
echo echo Starting Zhaoxin Tools Demo Backend... >> "dist\start.bat"
echo echo Access URL: http://localhost:8091 >> "dist\start.bat"
echo echo Press Ctrl+C to stop service >> "dist\start.bat"
echo echo. >> "dist\start.bat"
echo java -jar backend\*.jar >> "dist\start.bat"

:: Create README file
echo # Zhaoxin Tools Demo Deployment Package > "dist\README.md"
echo. >> "dist\README.md"
echo ## How to Start >> "dist\README.md"
echo 1. Double click start.bat >> "dist\README.md"
echo 2. Or command line: java -jar backend\*.jar >> "dist\README.md"
echo. >> "dist\README.md"
echo ## Access URLs >> "dist\README.md"
echo - Backend API: http://localhost:8091 >> "dist\README.md"
echo - Frontend needs separate web server deployment >> "dist\README.md"

echo [SUCCESS] Deployment package created

echo.
echo ==========================================
echo Build Completed Successfully!
echo ==========================================
echo.
echo Build artifacts:
echo   - Complete package: dist\
echo   - Backend JAR: backend\target\*.jar  
echo   - Frontend files: frontend\dist\
echo.
echo Deployment options:
echo   1. Local start: cd dist ^&^& start.bat
echo   2. Docker deploy: deploy-docker.bat  
echo   3. Linux deploy: package-for-linux.bat
echo.
echo Access URLs:
echo   - Local: http://localhost:8091
echo   - Docker: http://localhost:81
echo   - Linux: http://server-ip:81
echo.
pause
