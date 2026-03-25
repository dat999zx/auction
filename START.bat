@echo off

powershell -NoProfile -Command ^
  "$p = Start-Process mvn -ArgumentList 'exec:java','-Dexec.mainClass=com.bidify.server.ServerApp' -WorkingDirectory 'server' -PassThru; $p.Id | Out-File -Encoding ascii server.pid"

:: WAIT for server to boot
:waitloop
timeout /t 1 >nul
netstat -an | find ":5000" >nul
if errorlevel 1 goto waitloop

:: RUN CLIENT (blocking)
cd /d client
powershell -NoProfile -Command ^
  "$p = Start-Process mvn -ArgumentList 'javafx:run' -WorkingDirectory '%~dp0client' -PassThru; $p.Id | Out-File -Encoding ascii '%~dp0client.pid'"
