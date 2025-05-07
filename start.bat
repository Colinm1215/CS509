@echo off

echo Starting Backend...
start cmd /k "mvn spring-boot:run"

echo Starting Frontend...
start cmd /k "cd frontend && npm run dev"

echo Both backend and frontend are starting in separate terminals.
