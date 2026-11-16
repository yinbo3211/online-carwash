@echo off
chcp 65001 >nul 2>&1
setlocal enabledelayedexpansion
title 线上洗车平台 v6.0 - 数据库初始化

REM ==============================================================================
REM  仅做：建库 -> 建用户授权 -> 依次导入 sql\01_schema.sql / 02_init_data.sql / 03_test_data.sql
REM  不打包、不部署 Tomcat。可独立运行，也可在一键部署失败时单独排障。
REM  建议"以管理员身份运行"。
REM  重要约定：密码请使用字母/数字/@/_ 等，不要含 空格、%%、!、"' 等 cmd 特殊字符
REM ==============================================================================

REM ---------------------------- 变量配置区 START ----------------------------
REM MySQL 8 安装目录（需含 bin\mysql.exe）。留空则自动从 PATH 探测
set "MYSQL_HOME=C:\Program Files\MySQL\MySQL Server 8.0"

set "DB_ROOT_USER=root"
set "DB_ROOT_PWD=Root@123456"

set "DB_USER=xiche"
set "DB_PWD=Xiche@2024"
set "DB_HOST=127.0.0.1"
set "DB_PORT=3306"
set "DB_NAME=xiche"

REM 1=先 DROP 旧库再重建（会清空全部数据，慎用！）；0=库已存在则保留
set "DB_RESET=0"
REM ----------------------------- 变量配置区 END -----------------------------

set "PROJECT_DIR=%~dp0"
set "MYSQL_EXE="
set "ROOT_PWD_ARG=-p%DB_ROOT_PWD%"
set "APP_PWD_ARG=-p%DB_PWD%"
if "%DB_ROOT_PWD%"=="" set "ROOT_PWD_ARG="
if "%DB_PWD%"=="" set "APP_PWD_ARG="

echo.
echo ==============================================================================
echo  线上洗车平台 v6.0 - 数据库初始化
echo  工程目录：%PROJECT_DIR%
echo  目标库  ：%DB_HOST%:%DB_PORT%/%DB_NAME%
echo  注意    ：DB_RESET=%DB_RESET%（1 会清空旧库！）
echo ==============================================================================
echo.

REM ============================== 1. 检查 mysql 客户端 ==============================
echo [1/4] 检查 mysql.exe ...
if defined MYSQL_HOME if exist "%MYSQL_HOME%\bin\mysql.exe" set "MYSQL_EXE=%MYSQL_HOME%\bin\mysql.exe"
if not defined MYSQL_EXE (
  for /f "delims=" %%i in ('where mysql.exe 2^>nul') do set "MYSQL_EXE=%%i"
)
if not defined MYSQL_EXE goto :mysql_missing
"%MYSQL_EXE%" --version >nul 2>&1
if errorlevel 1 goto :mysql_missing
echo       [OK] MySQL CLI : %MYSQL_EXE%
goto :after_mysql
:mysql_missing
echo       [错误] 未找到 mysql.exe。
echo              【必须人工】安装 MySQL 8 并把 bin 目录加入 PATH，
echo              或把本脚本顶部的 MYSQL_HOME 指向 MySQL 8 安装目录。
echo              下载：https://dev.mysql.com/downloads/installer/
goto :fail
:after_mysql

REM ============================== 2. 检查 SQL 脚本 ==============================
echo.
echo [2/4] 检查 SQL 脚本 ...
for %%F in (01_schema.sql 02_init_data.sql 03_test_data.sql) do (
  if not exist "%PROJECT_DIR%sql\%%~F" (
    echo       [错误] 缺少：%PROJECT_DIR%sql\%%~F
    goto :fail
  )
)
echo       [OK] 三个 SQL 脚本齐全

"%MYSQL_EXE%" -h%DB_HOST% -P%DB_PORT% -u%DB_ROOT_USER% %ROOT_PWD_ARG% -e "SELECT 1" >nul 2>&1
if errorlevel 1 (
  echo       [错误] 无法用 root 连接 MySQL。请确认服务已启动（net start MySQL80）、
  echo              root 密码正确、端口 %DB_PORT% 未被占用。
  goto :fail
)
echo       [OK] MySQL 连接正常

REM ============================== 3. 建库 + 授权 ==============================
echo.
echo [3/4] 建库并授权 ...
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
  echo       [错误] 建库/授权失败，请检查 root 密码与权限。
  del /q "%INIT_DB_SQL%" >nul 2>&1
  goto :fail
)
del /q "%INIT_DB_SQL%" >nul 2>&1
echo       [OK] 库 %DB_NAME% 就绪，用户 %DB_USER% 已授权

REM ============================== 4. 导入三个 SQL ==============================
echo.
echo [4/4] 导入 SQL 脚本 ...
for %%F in (01_schema.sql 02_init_data.sql 03_test_data.sql) do (
  echo       导入 %%~F ...
  "%MYSQL_EXE%" -h%DB_HOST% -P%DB_PORT% -u%DB_USER% %APP_PWD_ARG% %DB_NAME% < "%PROJECT_DIR%sql\%%~F"
  if errorlevel 1 (
    echo       [错误] 导入 %%~F 失败。
    goto :fail
  )
)

echo.
echo ==============================================================================
echo  [完成] 数据库初始化成功！
echo  接下来：确认 xiche-web\src\main\resources\jdbc.properties 中的
echo          url / username / password 与本次填写一致，然后运行 一键部署.bat
echo ==============================================================================
echo.
pause
exit /b 0

:fail
echo.
echo ==============================================================================
echo  [初始化失败] 请根据上方错误提示处理后重试。详见 DEPLOY.md。
echo ==============================================================================
echo.
pause
exit /b 1
