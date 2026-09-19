@echo off
setlocal
rem ===== MineOps - Backend (Spring Boot API en el puerto 8080) =====
set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-25.0.4.101-hotspot"
set "JAVA=%JAVA_HOME%\bin\java.exe"
set "ROOT=%~dp0.."
if exist "%ROOT%\backend\target\minera-mvp-backend-0.1.0.jar" (
    start "MineOps Backend" /min "%JAVA%" -jar "%ROOT%\backend\target\minera-mvp-backend-0.1.0.jar"
) else (
    "%JAVA_HOME%\bin\javac.exe" -version >nul 2>&1
    start "MineOps Backend" /min cmd /c "pushd ""%ROOT%\backend"" & ""C:\Users\Usuario\dev-tools\apache-maven-3.9.16\bin\mvn.cmd"" -q spring-boot:run"
)
echo Backend iniciado en minimizado. API: http://localhost:8080/api
timeout /t 3 >nul