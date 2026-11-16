@echo off
chcp 65001 >nul 2>&1
setlocal
title 线上洗车平台 v6.0 - 一键启动 Tomcat

REM ==============================================================================
REM  仅启动 Tomcat，不重新打包、不动数据库（适合开发迭代）
REM  变量配置区：只需改 CATALINA_HOME
REM ==============================================================================
set "CATALINA_HOME=C:\Program Files\apache-tomcat-9.0.85"
REM ==============================================================================

echo.
echo ==============================================================================
echo  线上洗车平台 v6.0 - 启动 Tomcat
echo  Tomcat：%CATALINA_HOME%
echo ==============================================================================
echo.

if not exist "%CATALINA_HOME%\bin\startup.bat" (
  echo [错误] 未找到 Tomcat：%CATALINA_HOME%\bin\startup.bat
  echo        请修改本脚本顶部的 CATALINA_HOME，指向 Tomcat 9 解压目录。
  echo        下载：https://tomcat.apache.org/download-90.cgi
  pause
  exit /b 1
)

echo 正在启动 Tomcat ...
call "%CATALINA_HOME%\bin\startup.bat"
echo.
echo [OK] 启动命令已发出。
echo     访问地址：http://localhost:8080/
echo     若部署为 xiche-web.war，则访问 http://localhost:8080/xiche-web/
echo     控制台日志：%CATALINA_HOME%\logs\catalina.out
echo.
echo 如需在前台观察实时日志（关窗即停），可改用：
echo     "%CATALINA_HOME%\bin\catalina.bat" run
echo.
pause
exit /b 0
