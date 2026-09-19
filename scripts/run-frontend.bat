@echo off
setlocal
rem ===== MineOps - Frontend web (React + Vite, http://localhost:5173) =====
set "ROOT=%~dp0.."
set "PATH=C:\Program Files\nodejs;%PATH%"
cd /d "%ROOT%\frontend"
if not exist node_modules (
    call npm.cmd install
)
start "MineOps Web" cmd /c "cd /d ""%ROOT%\frontend"" & set ""PATH=C:\Program Files\nodejs;%PATH%"" & npm.cmd run dev"
echo Frontend iniciado. Abri el navegador en: http://localhost:5173
timeout /t 3 >nul