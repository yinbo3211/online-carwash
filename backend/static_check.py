#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""全量静态一致性检查：Mapper接口/XML、SQL、配置、缺陷项残留。"""
import os, re, sys

ROOT = os.path.dirname(os.path.abspath(__file__))
errors = []
warns = []
def err(m): errors.append(m)
def warn(m): warns.append(m)

# ---------- 1. Mapper 接口方法 vs XML statement 对应 ----------
dao_java = os.path.join(ROOT, "xiche-dao/src/main/java")
mapper_xml = os.path.join(ROOT, "xiche-dao/src/main/resources/mapper")
xml_texts = {}
for dp, _, fns in os.walk(mapper_xml):
    for fn in fns:
        if fn.endswith(".xml"):
            p = os.path.join(dp, fn)
            xml_texts[open(p, encoding="utf-8").read()] = p
all_xml = "\n".join(re.findall(r"<(select|insert|update|delete)\s+id=\"([^\"]+)\"", t)[1] and
                    re.findall(r"<(?:select|insert|update|delete)\s+id=\"([^\"]+)\"", t)[0] or ""
                    for t in [])
# 简单收集所有 statement id
stmt_ids = set()
for t in xml_texts:
    stmt_ids.update(re.findall(r"<(?:select|insert|update|delete)\s+id=\"([^\"]+)\"", t))
ns_namespaces = set()
for t in xml_texts:
    ns_namespaces.update(re.findall(r"namespace=\"([^\"]+)\"", t))

mapper_count = 0
for dp, _, fns in os.walk(dao_java):
    for fn in fns:
        if not fn.endswith(".java"): continue
        p = os.path.join(dp, fn)
        txt = open(p, encoding="utf-8").read()
        m = re.search(r"package\s+([\w.]+);", txt)
        if not m or ".dao" not in m.group(1): continue
        mapper_count += 1
        fqcn = m.group(1) + "." + fn[:-5]
        if fqcn not in ns_namespaces:
            err(f"Mapper接口 {fqcn} 无对应XML namespace")
        # 接口方法名
        methods = re.findall(r"\b(?:int|void|List<[^>]+>|[A-Z]\w+)\s+(\w+)\s*\(", txt)
        for meth in methods:
            if meth not in stmt_ids:
                err(f"Mapper方法 {fqcn}.{meth} 在XML中无对应 statement id")
print(f"[1] Mapper接口数={mapper_count}, XML statement数={len(stmt_ids)}")

# ---------- 2. XML statement 是否有接口对应（反向，允许部分XML辅助statement） ----------
# 跳过

# ---------- 3. 缺陷项残留检查 ----------
checks = [
    ("P0-01 硬编码身份 setUserId(2L)", r"setUserId\(2L\)", "fail"),
    ("P0-01 硬编码 currentWorker return 1L", r"currentWorker\(\)\s*\{\s*return\s+1L", "fail"),
    ("P1-18 assert 校验", r"\bassert\s+.*compareTo", "fail"),
    ("P1-16 硬编码收入 6420", r"6420", "fail"),
    ("P1-15 硬编码看板假指标", r"95\.2|p95LatencyMs.*86", "fail"),
    ("P1-03 前端答案全A提示", r"正确答案均为", "fail"),
    ("P0-14 工位产能误配B类", r"\('store','B'", "fail"),
    ("P1-01 multipart位于web-app直接子级", r"</servlet-mapping>\s*<multipart-config>", "fail"),
]
def strip_comments(text):
    text = re.sub(r"/\*.*?\*/", "", text, flags=re.S)   # 块注释
    text = re.sub(r"//[^\n]*", "", text)                 # 行注释
    text = re.sub(r"<!--.*?-->", "", text, flags=re.S)    # html注释
    text = re.sub(r"--[^\n]*", "", text)                  # sql注释
    return text
code_src = ""
for dp, _, fns in os.walk(ROOT):
    if "target" in dp or "/.git" in dp: continue
    for fn in fns:
        if fn.endswith((".java", ".js")):   # 仅代码文件
            try: code_src += strip_comments(open(os.path.join(dp, fn), encoding="utf-8").read())
            except: pass
for desc, pat, level in checks:
    if re.search(pat, code_src):
        err(f"残留缺陷: {desc}")

# ---------- 4. 关键修复存在性检查 ----------
presence = [
    ("P0-03 保单出单", "xiche-service", "issuePolicyIfAbsent"),
    ("P0-04 考试写认证", "xiche-web", "certificationMapper.upsert"),
    ("P0-08 代垫款返还", "xiche-service", "workerLaborNet"),
    ("P0-13 缓存服务", "xiche-common", "class LocalCacheService"),
    ("P0-02 RBAC拦截器", "xiche-web", "class AuthInterceptor"),
    ("P0-17 审计服务", "xiche-service", "class AuditServiceImpl"),
    ("P0-18 三方台账", "xiche-service", "writeLedger"),
    ("P1-02 前端API层", "frontend", "XicheAPI"),
]
for desc, sub, token in presence:
    found = False
    for dp, _, fns in os.walk(os.path.join(ROOT, "..", "frontend") if sub=="frontend" else os.path.join(ROOT, sub)):
        if "target" in dp: continue
        for fn in fns:
            try:
                if token in open(os.path.join(dp, fn), encoding="utf-8").read(): found=True; break
            except: pass
        if found: break
    if not found: err(f"缺失修复: {desc} (未找到 {token})")

# ---------- 5. SQL 脚本表数量 ----------
schema = open(os.path.join(ROOT, "sql/01_schema.sql"), encoding="utf-8").read()
tables = re.findall(r"CREATE TABLE\s+`?(\w+)`?", schema)
print(f"[5] schema表数量={len(tables)}")
if len(tables) < 34: err(f"schema表数不足: {len(tables)}")

# ---------- 6. web.xml multipart 位置 ----------
webxml = open(os.path.join(ROOT, "xiche-web/src/main/webapp/WEB-INF/web.xml"), encoding="utf-8").read()
servlet_start = webxml.find("<servlet>")
servlet_end = webxml.find("</servlet>")
if "<multipart-config>" in webxml:
    mc = webxml.find("<multipart-config>")
    if not (servlet_start < mc < servlet_end):
        err("P1-01: multipart-config 不在 servlet 元素内")

print("\n===== 静态检查结果 =====")
print(f"错误: {len(errors)}  警告: {len(warns)}")
for e in errors: print("  [ERROR]", e)
for w in warns: print("  [WARN]", w)
sys.exit(1 if errors else 0)
