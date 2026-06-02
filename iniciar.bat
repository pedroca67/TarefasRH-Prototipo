@echo off
title TarefasRH Potiguar - Iniciar Sistema
echo ==========================================
echo    INICIANDO TAREFASRH POTIGUAR
echo ==========================================
echo.

:: Iniciar Backend em uma nova janela
echo [1/2] Iniciando API Backend (Spring Boot)...
start "Backend (API)" cmd /k "cd back && mvnw spring-boot:run"

:: Iniciar Frontend em uma nova janela
echo [2/2] Iniciando Interface Frontend (Node.js)...
start "Frontend (Web)" cmd /k "cd front && npm run dev"

echo.
echo Janelas de execucao abertas! 
echo Mantenha as janelas abertas enquanto utiliza o sistema.
echo.
echo API: http://localhost:8080
echo Web: http://localhost:3000
echo.
pause
