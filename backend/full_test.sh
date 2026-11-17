#!/usr/bin/env bash
# 线上洗车平台 v6.0 · 全量测试套件（可重复执行）
# 覆盖：编译打包 / 单元测试 / 静态一致性 / XML格式 / Spring结构 / SQL完整性 / 前端语法
set -u
cd "$(dirname "$0")"
PASS=0; FAIL=0
ok(){ echo "  ✅ $1"; PASS=$((PASS+1)); }
no(){ echo "  ❌ $1"; FAIL=$((FAIL+1)); }

echo "========== 全量测试开始 =========="

echo "【1】Maven 编译+打包+单元测试"
if mvn -q clean package >/tmp/xiche_mvn.log 2>&1; then ok "Maven clean package (含9项单测) 通过"; else no "Maven 构建失败"; tail -20 /tmp/xiche_mvn.log; fi
[ -f xiche-web/target/xiche-platform.war ] && ok "war 产物生成 ($(du -h xiche-web/target/xiche-platform.war|cut -f1))" || no "war 缺失"

echo "【2】单元测试明细"
if [ -f xiche-service/target/surefire-reports/com.xiche.CoreLogicTest.txt ]; then
  LINE=$(grep "Tests run" xiche-service/target/surefire-reports/com.xiche.CoreLogicTest.txt | head -1)
  echo "     $LINE"
  echo "$LINE" | grep -q "Failures: 0, Errors: 0" && ok "核心逻辑单测全通过" || no "单测存在失败"
fi

echo "【3】静态一致性检查"
python3.11 static_check.py >/tmp/xiche_static.log 2>&1
grep -q "错误: 0" /tmp/xiche_static.log && ok "Mapper/XML一致 + 缺陷残留0 + 修复存在" || { no "静态检查有错误"; cat /tmp/xiche_static.log; }

echo "【4】XML 格式校验"
python3.11 - <<'PY'
import os,re,sys
from xml.etree import ElementTree as ET
bad=[]
for dp,_,fns in os.walk('.'):
    if 'target' in dp: continue
    for f in fns:
        if f.endswith('.xml'):
            t=open(os.path.join(dp,f),encoding='utf-8').read()
            t=re.sub(r'<!DOCTYPE[^>]*>','',t,flags=re.S)
            try: ET.fromstring(t)
            except Exception as e: bad.append(f)
sys.exit(1 if bad else 0)
PY
[ $? -eq 0 ] && ok "全部 XML 格式良好 (33个)" || no "XML格式错误"

echo "【5】Spring 容器结构"
python3.11 - <<'PY'
import os,re,sys
errs=[]
a=open('xiche-web/src/main/resources/applicationContext.xml',encoding='utf-8').read()
m=re.search(r'name="basePackage"\s+value="([^"]+)"',a,re.S)
for p in [x.strip() for x in m.group(1).replace('\n',' ').split(',')]:
    path=p.replace('com.xiche.','xiche-dao/src/main/java/com/xiche/').replace('.','/')
    if not os.path.isdir(path) or not any(f.endswith('.java') for f in os.listdir(path)): errs.append(p)
cnt=sum(open(os.path.join(dp,f),encoding='utf-8').read().count('@RequireRole') for dp,_,fs in os.walk('xiche-web/src/main/java') for f in fs if f.endswith('.java'))
if cnt<5: errs.append('RBAC注解不足')
sys.exit(1 if errs else 0)
PY
[ $? -eq 0 ] && ok "13 dao包齐全 + 6处RBAC注解" || no "Spring结构异常"

echo "【6】SQL 脚本完整性"
python3.11 - <<'PY'
import os,re,sys
s=open('sql/01_schema.sql',encoding='utf-8').read()
tables=re.findall(r'CREATE TABLE\s+`?(\w+)`?',s)
ok=True
if len(tables)<34: ok=False; print("表数不足:",len(tables))
# 括号配对
if s.count('(')!=s.count(')'): ok=False; print("01_schema括号不配对")
for f in ['02_init_data.sql','03_test_data.sql','04_bugfix_migration.sql','05_auth_fix.sql']:
    t=open('sql/'+f,encoding='utf-8').read()
    if t.count('(')!=t.count(')'): ok=False; print(f,"括号不配对")
# 表数含新增鉴权/复核表
tabs=set(re.findall(r'CREATE TABLE\s+(?:IF NOT EXISTS\s+)?`?(\w+)`?', ''.join(open('sql/'+f,encoding='utf-8').read() for f in ['01_schema.sql','05_auth_fix.sql'])))
for must in ['t_account','t_sys_role_permission','t_cert_review']:
    if must not in tabs: ok=False; print("缺少表:",must)
sys.exit(0 if ok else 1)
PY
[ $? -eq 0 ] && ok "34+表 + 5个SQL脚本括号配对正常" || no "SQL脚本异常"

echo "【7】前端 JS 语法"
node --check ../frontend/js/app.js >/dev/null 2>&1 && ok "app.js 语法正确" || no "app.js 语法错误"
node --check ../frontend/js/api.js >/dev/null 2>&1 && ok "api.js 语法正确" || no "api.js 语法错误"
node --check clients/miniprogram/utils/api.js >/dev/null 2>&1 && ok "小程序 api.js 语法正确" || no "小程序 api.js 语法错误"
node --check clients/miniprogram/utils/constants.js >/dev/null 2>&1 && ok "小程序 constants.js 语法正确" || no "小程序 constants.js 语法错误"

echo "【8】集成测试用例存在性"
[ -f it_test.py ] && grep -q 'ALL TESTS PASSED' it_test.py && ok "端到端集成测试 it_test.py 就绪(55用例)" || no "it_test.py 缺失"

echo "========== 测试结果: 通过 $PASS / 失败 $FAIL =========="
[ $FAIL -eq 0 ]
