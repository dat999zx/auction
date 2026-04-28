@echo off
setlocal EnableDelayedExpansion

set "ROOT=%~dp0"
set "SERVER_DIR=%ROOT%server"
set "CLIENT_DIR=%ROOT%client"
set "SERVER_PID=%ROOT%server.pid"
set "CLIENT_PID=%ROOT%client.pid"
set "SERVER_PORT=5000"

if exist "%SERVER_PID%" del "%SERVER_PID%"
if exist "%CLIENT_PID%" del "%CLIENT_PID%"

echo Building common module...
call mvn -q -pl common -am install
if errorlevel 1 (
  echo Failed to build/install the common module.
  exit /b 1
)

cmd /c "netstat -an | find ":%SERVER_PORT%" >nul"
if not errorlevel 1 goto startclient

powershell -NoProfile -ExecutionPolicy Bypass -Command ^
  "$p = Start-Process 'cmd.exe' -ArgumentList '/c','mvn -q exec:java -Dexec.mainClass=com.bidify.server.ServerApp' -WorkingDirectory '%SERVER_DIR%' -PassThru; $p.Id | Out-File -Encoding ascii '%SERVER_PID%'"
if errorlevel 1 (
  echo Failed to start the server process.
  exit /b 1
)

set /a WAIT_SECONDS=0
:waitloop
powershell -NoProfile -Command "Start-Sleep -Seconds 1"
set /a WAIT_SECONDS+=1

netstat -an | find ":%SERVER_PORT%" >nul
if not errorlevel 1 goto startclient

if exist "%SERVER_PID%" (
  set /p SERVER_PID_VALUE=<"%SERVER_PID%"
  if defined SERVER_PID_VALUE (
    powershell -NoProfile -Command "if (Get-Process -Id !SERVER_PID_VALUE! -ErrorAction SilentlyContinue) { exit 0 } else { exit 1 }"
    if errorlevel 1 (
      echo Server process exited before opening port %SERVER_PORT%.
      if exist "%SERVER_LOG%" echo Server output log: "%SERVER_LOG%"
      if exist "%SERVER_ERR%" (
        echo Server error log: "%SERVER_ERR%"
        type "%SERVER_ERR%"
      ) else if exist "%SERVER_LOG%" (
        echo Check "%SERVER_LOG%" for the startup error.
        type "%SERVER_LOG%"
      )
      exit /b 1
    )
  )
)

if %WAIT_SECONDS% geq 60 (
  echo Timed out waiting for server port %SERVER_PORT%.
  if exist "%SERVER_LOG%" echo Server output log: "%SERVER_LOG%"
  if exist "%SERVER_ERR%" echo Server error log: "%SERVER_ERR%"
  exit /b 1
)
goto waitloop

:startclient
powershell -NoProfile -ExecutionPolicy Bypass -Command ^
  "$p = Start-Process 'cmd.exe' -ArgumentList '/c','mvn -q javafx:run' -WorkingDirectory '%CLIENT_DIR%' -PassThru; $p.Id | Out-File -Encoding ascii '%CLIENT_PID%'"
if errorlevel 1 (
  echo Failed to start the client process.
  exit /b 1
)

echo Server and client started.
