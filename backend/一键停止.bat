@echo off
chcp 65001 >nul 2>&1
setlocal
title 线上洗车平台 v6.0 - 一键停止 Tomcat

REM ==============================================================================
REM  停止 Tomcat（配合"一键启动.bat"用于开发迭代）
REM  变量配置区：只需改 CATALINA_HOME
REM ==============================================================================
set "CATALINA_HOME=C:\Program Files\apache-tomcat-9.0.85"
REM ==============================================================================

echo.
echo ==============================================================================
echo  线上洗车平台 v6.0 - 停止 Tomcat
echo  Tomcat：%CATALINA_HOME%
echo ==============================================================================
echo.

if not exist "%CATALINA_HOME%\bin\shutdown.bat" (
  echo [错误] 未找到 Tomcat：%CATALINA_HOME%\bin\shutdown.bat
  echo        请修改本脚本顶部的 CATALINA_HOME。
  pause
  exit /b 1
)

echo 正在停止 Tomcat ...
call "%CATALINA_HOME%\bin\shutdown.bat"
timeout /t 3 /nobreak >nul 2>&1
echo.
echo [OK] 已发出停止命令。
echo.
echo 若 8080 端口仍被占用，可手工排查并结束进程：
echo     netstat -ano ^| findstr :8080
echo     taskkill /PID ^<查到的PID^> /F
echo.
pause
exit /b 0
