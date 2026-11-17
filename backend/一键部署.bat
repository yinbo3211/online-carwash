@echo off
chcp 65001 >nul 2>&1
setlocal enabledelayedexpansion
title 线上洗车平台 v6.0 - 一键部署（Windows）

REM ==============================================================================
REM  线上洗车平台 v6.0 一键部署脚本（Windows / cmd）
REM  自动化流程：环境检测 -> 建库授权 -> 导入3个SQL -> 改写jdbc.properties
REM              -> mvn clean package -> 复制war到Tomcat -> 启动Tomcat
REM  使用方式：双击运行本脚本（建议"以管理员身份运行"，避免权限问题）
REM  首次使用：先在下方"变量配置区"改成你本机的真实路径和密码，详见 DEPLOY.md
REM  重要约定：密码请使用字母/数字/@/_ 等，不要含 空格、%%、!、"' 等 cmd 特殊字符
REM ==============================================================================

REM ---------------------------- 变量配置区 START ----------------------------
REM JDK 8 安装目录（需含 bin\java.exe）。留空则自动从 PATH 探测
set "JAVA_HOME=C:\Program Files\Java\jdk1.8.0_202"

REM Maven 3.6+ 安装目录（需含 bin\mvn.cmd）。留空则自动从 PATH 探测
set "MAVEN_HOME=C:\Program Files\apache-maven-3.8.8"

REM Tomcat 9 解压目录（需含 bin\catalina.bat）。此项正确与否决定能否部署，务必核对
set "CATALINA_HOME=C:\Program Files\apache-tomcat-9.0.85"

REM MySQL 8 安装目录（需含 bin\mysql.exe）。留空则自动从 PATH 探测
set "MYSQL_HOME=C:\Program Files\MySQL\MySQL Server 8.0"

REM 数据库管理员账号（仅用于建库/建用户/授权，填你安装 MySQL 时设置的 root 密码）
set "DB_ROOT_USER=root"
set "DB_ROOT_PWD=Root@123456"

REM 应用专用账号（脚本自动创建并授权，应用运行时使用）
set "DB_USER=xiche"
set "DB_PWD=Xiche@2024"
set "DB_HOST=127.0.0.1"
set "DB_PORT=3306"
set "DB_NAME=xiche"

REM 部署形态：1=部署为 ROOT.war（访问 http://IP:8080/）；0=部署为 xiche-web.war（访问 http://IP:8080/xiche-web/）
set "DEPLOY_AS_ROOT=1"

REM 数据库重置：1=先 DROP 旧库再重建（会清空全部数据，慎用！）；0=库已存在则保留
set "DB_RESET=0"

REM 跳过数据库初始化：1=只重新打包部署、不动数据库；0=完整流程
set "SKIP_DB_INIT=0"
REM ----------------------------- 变量配置区 END -----------------------------

set "PROJECT_DIR=%~dp0"
set "MYSQL_EXE="
set "MVN_CMD="
set "ROOT_PWD_ARG=-p%DB_ROOT_PWD%"
set "APP_PWD_ARG=-p%DB_PWD%"
if "%DB_ROOT_PWD%"=="" set "ROOT_PWD_ARG="
if "%DB_PWD%"=="" set "APP_PWD_ARG="

echo.
echo ==============================================================================
echo  线上洗车平台 v6.0 一键部署
echo  工程目录：%PROJECT_DIR%
echo  数据库  ：%DB_HOST%:%DB_PORT%/%DB_NAME%
echo ==============================================================================
echo.

REM ============================== 1. 环境检测 ==============================
echo [1/7] 检测 JDK / Maven / Tomcat / MySQL ...

REM --- JDK ---
if not defined JAVA_HOME goto :java_try_path
if not exist "%JAVA_HOME%\bin\java.exe" goto :java_try_path
goto :java_ok
:java_try_path
set "JAVA_HOME="
for /f "delims=" %%i in ('where java 2^>nul') do (
  set "JAVA_EXE=%%i"
  goto :java_found
)
goto :java_missing
:java_found
for %%i in ("%JAVA_EXE%") do set "JAVA_HOME=%%~dpi..\.."
for %%i in ("%JAVA_HOME%") do set "JAVA_HOME=%%~fi"
if not exist "%JAVA_HOME%\bin\java.exe" goto :java_missing
:java_ok
"%JAVA_HOME%\bin\java.exe" -version >nul 2>&1
if errorlevel 1 goto :java_missing
echo       [OK] JDK      : %JAVA_HOME%
goto :after_java
:java_missing
echo       [错误] 未找到可用的 JDK 1.8+。
echo              处理：安装 JDK8，然后在脚本顶部设置 JAVA_HOME，或把 java.exe 加入 PATH。
echo              下载：https://adoptium.net/temurin/releases/?version=8
goto :fail
:after_java

REM --- Maven ---
if not defined MAVEN_HOME goto :mvn_try_path
if not exist "%MAVEN_HOME%\bin\mvn.cmd" goto :mvn_try_path
set "MVN_CMD=%MAVEN_HOME%\bin\mvn.cmd"
goto :mvn_ok
:mvn_try_path
for /f "delims=" %%i in ('where mvn.cmd 2^>nul') do (
  set "MVN_CMD=%%i"
  goto :mvn_found
)
goto :mvn_missing
:mvn_found
for %%i in ("%MVN_CMD%") do set "MAVEN_HOME=%%~dpi..\.."
for %%i in ("%MAVEN_HOME%") do set "MAVEN_HOME=%%~fi"
:mvn_ok
call "%MVN_CMD%" -v >nul 2>&1
if errorlevel 1 goto :mvn_missing
echo       [OK] Maven    : %MAVEN_HOME%
goto :after_mvn
:mvn_missing
echo       [错误] 未找到 Maven 3.6+（mvn.cmd）。
echo              处理：安装 Maven 后在脚本顶部设置 MAVEN_HOME，或把 bin 目录加入 PATH。
echo              下载：https://maven.apache.org/download.cgi
goto :fail
:after_mvn

REM --- Tomcat ---
if not exist "%CATALINA_HOME%\bin\catalina.bat" goto :tomcat_missing
echo       [OK] Tomcat   : %CATALINA_HOME%
goto :after_tomcat
:tomcat_missing
echo       [错误] 未找到 Tomcat 9：%CATALINA_HOME%\bin\catalina.bat
echo              【必须人工】首次部署请先安装 Tomcat 9：
echo              1) 下载 https://tomcat.apache.org/download-90.cgi 的 64-bit Windows zip
echo              2) 解压到任意目录（如 C:\Program Files\apache-tomcat-9.0.85）
echo              3) 把本脚本顶部的 CATALINA_HOME 改成该解压目录
goto :fail
:after_tomcat

REM --- MySQL 客户端 ---
if defined MYSQL_HOME if exist "%MYSQL_HOME%\bin\mysql.exe" set "MYSQL_EXE=%MYSQL_HOME%\bin\mysql.exe"
if not defined MYSQL_EXE (
  for /f "delims=" %%i in ('where mysql.exe 2^>nul') do set "MYSQL_EXE=%%i"
)
if not defined MYSQL_EXE goto :mysql_missing
"%MYSQL_EXE%" --version >nul 2>&1
if errorlevel 1 goto :mysql_missing
echo       [OK] MySQL CLI: %MYSQL_EXE%
goto :after_mysql
:mysql_missing
echo       [错误] 未找到 mysql.exe。
echo              【必须人工】首次部署请先安装 MySQL 8，并把 bin 目录加入 PATH，
echo              或把本脚本顶部的 MYSQL_HOME 指向 MySQL 8 安装目录。
echo              下载：https://dev.mysql.com/downloads/installer/
goto :fail
:after_mysql

REM --- 校验 SQL 脚本 ---
for %%F in (01_schema.sql 02_init_data.sql 03_test_data.sql 04_bugfix_migration.sql 05_auth_fix.sql) do (
  if not exist "%PROJECT_DIR%sql\%%~F" (
    echo       [错误] 缺少 SQL 脚本：%PROJECT_DIR%sql\%%~F
    echo              请确认工程目录完整，SQL 脚本由后端工程师提供。
    goto :fail
  )
)
echo       [OK] SQL 脚本 : 01_schema.sql / 02_init_data.sql / 03_test_data.sql / 04_bugfix_migration.sql / 05_auth_fix.sql

REM --- 连通性检查 ---
"%MYSQL_EXE%" -h%DB_HOST% -P%DB_PORT% -u%DB_ROOT_USER% %ROOT_PWD_ARG% -e "SELECT 1" >nul 2>&1
if errorlevel 1 (
  echo       [错误] 无法连接 MySQL（%DB_ROOT_USER%@%DB_HOST%:%DB_PORT%）。
  echo              请确认：① MySQL 服务已启动（services.msc 中 MySQL80，或 net start MySQL80）
  echo                      ② DB_ROOT_USER / DB_ROOT_PWD 正确
  echo                      ③ 端口 %DB_PORT% 未被占用（netstat -ano ^| findstr :%DB_PORT%）
  goto :fail
)
echo       [OK] MySQL 连接正常

REM ============================== 2. 初始化数据库 ==============================
echo.
echo [2/7] 初始化数据库 ...
if "%SKIP_DB_INIT%"=="1" (
  echo       [跳过] SKIP_DB_INIT=1，保留现有数据库
  goto :after_db_init
)

set "INIT_DB_SQL=%TEMP%\xiche_init_db_%RANDOM%.sql"
if "%DB_RESET%"=="1" (
  > "%INIT_DB_SQL%" echo DROP DATABASE IF EXISTS `%DB_NAME%`;
) else (
  > "%INIT_DB_SQL%" echo SELECT 'KEEP_EXISTING_DB';
)
>> "%INIT_DB_SQL%" echo CREATE DATABASE IF NOT EXISTS `%DB_NAME%` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
>> "%INIT_DB_SQL%" echo CREATE USER IF NOT EXISTS '%DB_USER%'@'%%' IDENTIFIED BY '%DB_PWD%';
>> "%INIT_DB_SQL%" echo GRANT ALL PRIVILEGES ON `%DB_NAME%`.* TO '%DB_USER%'@'%%';
>> "%INIT_DB_SQL%" echo FLUSH PRIVILEGES;

"%MYSQL_EXE%" -h%DB_HOST% -P%DB_PORT% -u%DB_ROOT_USER% %ROOT_PWD_ARG% < "%INIT_DB_SQL%"
if errorlevel 1 (
  echo       [错误] 建库/建用户/授权失败，请检查 root 密码与权限。
  del /q "%INIT_DB_SQL%" >nul 2>&1
  goto :fail
)
del /q "%INIT_DB_SQL%" >nul 2>&1
echo       [OK] 数据库 %DB_NAME% 已就绪，用户 %DB_USER% 已授权

for %%F in (01_schema.sql 02_init_data.sql 03_test_data.sql 04_bugfix_migration.sql 05_auth_fix.sql) do (
  echo       导入 %%~F ...
  "%MYSQL_EXE%" -h%DB_HOST% -P%DB_PORT% -u%DB_USER% %APP_PWD_ARG% %DB_NAME% < "%PROJECT_DIR%sql\%%~F"
  if errorlevel 1 (
    echo       [错误] 导入 %%~F 失败。
    goto :fail
  )
)
echo       [OK] 三个 SQL 脚本导入完成
:after_db_init

REM ============================== 3. 改写 jdbc.properties ==============================
echo.
echo [3/7] 写入数据库连接配置 ...
set "JDBC_FILE=%PROJECT_DIR%xiche-web\src\main\resources\jdbc.properties"
if not exist "%JDBC_FILE%" (
  echo       [警告] 未找到 %JDBC_FILE%，跳过自动改写。
  echo              请按 DEPLOY.md 手工创建该文件后再重新打包。
  goto :after_jdbc
)
call :write_jdbc_ps
powershell -NoProfile -ExecutionPolicy Bypass -File "%JDBC_PS%" -Path "%JDBC_FILE%" -Url "jdbc:mysql://%DB_HOST%:%DB_PORT%/%DB_NAME%?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true" -User "%DB_USER%" -Pwd "%DB_PWD%"
if errorlevel 1 (
  echo       [错误] jdbc.properties 自动改写失败，请按 DEPLOY.md 手工修改。
  del /q "%JDBC_PS%" >nul 2>&1
  goto :fail
)
del /q "%JDBC_PS%" >nul 2>&1
echo       [OK] jdbc.properties 已写入：%DB_HOST%:%DB_PORT%/%DB_NAME%
:after_jdbc

REM ============================== 4. Maven 打包 ==============================
echo.
echo [4/7] 执行 mvn clean package -DskipTests ...
cd /d "%PROJECT_DIR%"
call "%MVN_CMD%" clean package -DskipTests
if errorlevel 1 (
  echo       [错误] Maven 打包失败，请根据上方 ERROR 日志修复后端代码/依赖后重试。
  goto :fail
)
set "WAR_FILE=%PROJECT_DIR%xiche-web\target\xiche-web-1.0.0.war"
if not exist "%WAR_FILE%" (
  for %%w in ("%PROJECT_DIR%xiche-web\target\*.war") do set "WAR_FILE=%%~fw"
)
if not exist "%WAR_FILE%" (
  echo       [错误] 未找到打包产物：%PROJECT_DIR%xiche-web\target\*.war
  goto :fail
)
echo       [OK] 打包完成：%WAR_FILE%

REM ============================== 5. 部署 war ==============================
echo.
echo [5/7] 部署 war 到 Tomcat ...
if not exist "%CATALINA_HOME%\webapps" mkdir "%CATALINA_HOME%\webapps"
if exist "%CATALINA_HOME%\bin\shutdown.bat" (
  call "%CATALINA_HOME%\bin\shutdown.bat" >nul 2>&1
  timeout /t 3 /nobreak >nul 2>&1
)
if "%DEPLOY_AS_ROOT%"=="1" (
  if exist "%CATALINA_HOME%\webapps\ROOT" rd /s /q "%CATALINA_HOME%\webapps\ROOT" >nul 2>&1
  copy /y "%WAR_FILE%" "%CATALINA_HOME%\webapps\ROOT.war" >nul
  set "APP_URL=http://localhost:8080/"
) else (
  if exist "%CATALINA_HOME%\webapps\xiche-web" rd /s /q "%CATALINA_HOME%\webapps\xiche-web" >nul 2>&1
  copy /y "%WAR_FILE%" "%CATALINA_HOME%\webapps\xiche-web.war" >nul
  set "APP_URL=http://localhost:8080/xiche-web/"
)
if errorlevel 1 (
  echo       [错误] 复制 war 失败：请确认 Tomcat 已停止、且对 webapps 目录有写权限（建议管理员运行）。
  goto :fail
)
echo       [OK] war 已复制到 %CATALINA_HOME%\webapps

REM ============================== 6. 启动 Tomcat ==============================
echo.
echo [6/7] 启动 Tomcat ...
start "xiche-tomcat" "%CATALINA_HOME%\bin\startup.bat"
echo       [OK] 已发出启动命令

REM ============================== 7. 完成 ==============================
echo.
echo [7/7] 部署完成！
echo.
echo ==============================================================================
echo  访问地址：!APP_URL!
echo  日志位置：%CATALINA_HOME%\logs\catalina.out
echo  首次部署 war 解压与 Spring 初始化可能需要 10~60 秒，请稍候再访问。
echo  测试账号：admin / admin123（V1 内部测试数据）
echo  合规提醒：V1 为内部测试版，模拟支付不产生真实资金收付，不得对外经营。
echo ==============================================================================
echo.
pause
exit /b 0

REM ============================== 子程序 ==============================
:write_jdbc_ps
set "JDBC_PS=%TEMP%\xiche_jdbc_%RANDOM%.ps1"
> "%JDBC_PS%" echo param([string]$Path,[string]$Url,[string]$User,[string]$Pwd)
>> "%JDBC_PS%" echo $ErrorActionPreference = 'Stop'
>> "%JDBC_PS%" echo $lines = Get-Content -LiteralPath $Path -Encoding UTF8
>> "%JDBC_PS%" echo $hasUrl = $false; $hasUser = $false; $hasPwd = $false
>> "%JDBC_PS%" echo $out = foreach ($line in $lines) {
>> "%JDBC_PS%" echo   if ($line -match '^^\s*(jdbc\.)?(url^|jdbcUrl)\s*=') { $hasUrl = $true; "jdbc.url=$Url" }
>> "%JDBC_PS%" echo   elseif ($line -match '^^\s*(jdbc\.)?(username^|user)\s*=') { $hasUser = $true; "jdbc.username=$User" }
>> "%JDBC_PS%" echo   elseif ($line -match '^^\s*(jdbc\.)?(password^|pwd)\s*=') { $hasPwd = $true; "jdbc.password=$Pwd" }
>> "%JDBC_PS%" echo   else { $line }
>> "%JDBC_PS%" echo }
>> "%JDBC_PS%" echo if (-not $hasUrl) { $out += "jdbc.url=$Url" }
>> "%JDBC_PS%" echo if (-not $hasUser) { $out += "jdbc.username=$User" }
>> "%JDBC_PS%" echo if (-not $hasPwd) { $out += "jdbc.password=$Pwd" }
>> "%JDBC_PS%" echo [IO.File]::WriteAllLines($Path, $out, (New-Object System.Text.UTF8Encoding $false))
exit /b 0

:fail
echo.
echo ==============================================================================
echo  [部署中止] 请根据上方错误提示处理后重新运行本脚本。
echo  更多帮助见 DEPLOY.md 的"常见问题排查"章节。
echo ==============================================================================
echo.
pause
exit /b 1
