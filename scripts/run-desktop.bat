@echo off
setlocal
rem ===== MineOps - Aplicacion de escritorio (JavaFX) =====
set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-25.0.4.101-hotspot"
set "ROOT=%~dp0.."
cd /d "%ROOT%\desktop"
if not exist "%ROOT%\desktop\target\classes\com\minera\mvp\desktop\DesktopApp.class" (
    call "C:\Users\Usuario\dev-tools\apache-maven-3.9.16\bin\mvn.cmd" -q -DskipTests package
)
call "C:\Users\Usuario\dev-tools\apache-maven-3.9.16\bin\mvn.cmd" -q javafx:run
echo Cierre la app de escritorio para terminar.
timeout /t 3 >nul