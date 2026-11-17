package com.xiche.worker.controller;

import com.xiche.common.Result;
import com.xiche.common.constant.BusinessConstants;
import com.xiche.common.constant.CertConstants;
import com.xiche.common.constant.OrderStatusEnum;
import com.xiche.common.context.LoginContext;
import com.xiche.common.util.PageUtil;
import com.xiche.learning.dao.ExamRecordMapper;
import com.xiche.auth.dao.CertReviewMapper;
import com.xiche.learning.entity.Exam;
import com.xiche.learning.entity.ExamRecord;
import com.xiche.learning.entity.Question;
import com.xiche.order.dao.OrderEvidenceMapper;
import com.xiche.order.dao.OrderMapper;
import com.xiche.order.entity.Order;
import com.xiche.order.entity.OrderEvidence;
import com.xiche.order.service.OrderService;
import com.xiche.settlement.dao.SettlementMapper;
import com.xiche.worker.dao.CertificationMapper;
import com.xiche.worker.dao.WorkerMapper;
import com.xiche.worker.entity.Certification;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.*;

/** 洗车工端 REST API（/api/worker/）。 */
@RestController
@RequestMapping("/api/worker")
public class WorkerController {
    @Resource private WorkerMapper workerMapper;
    @Resource private CertificationMapper certificationMapper;
    @Resource private OrderMapper orderMapper;
    @Resource private OrderService orderService;
    @Resource private OrderEvidenceMapper evidenceMapper;
    @Resource private ExamRecordMapper examRecordMapper;
    @Resource private CertReviewMapper certReviewMapper;
    @Resource private SettlementMapper settlementMapper;

    private Long currentWorker() { return LoginContext.workerId(); } // P0-01：来自登录上下文

    @GetMapping("/certs")
    public Result<Map<String, Object>> certs() {
        List<Certification> list = workerMapper.selectCerts(currentWorker());
        List<Map<String, Object>> out = new ArrayList<Map<String, Object>>();
        Date now = new Date();
        for (Certification c : list) {
            Map<String, Object> m = new HashMap<String, Object>();
            boolean active = "PASSED".equalsIgnoreCase(c.getStatus())
                    && !(c.getExpireAt() != null && c.getExpireAt().before(now)); // P0-16
            m.put("key", c.getCertCode()); m.put("name", certName(c.getCertCode()));
            m.put("passed", active); m.put("status", active ? "PASSED" : c.getStatus());
            if (c.getExpireAt() != null) m.put("expireAt", c.getExpireAt());
            out.add(m);
        }
        Map<String, Object> data = new HashMap<String, Object>(); data.put("list", out);
        return Result.success(data);
    }
    private String certName(String code) {
        if ("C1".equals(code)) return "外观清洁"; if ("C2".equals(code)) return "标准清洁";
        if ("C3".equals(code)) return "精细清洁"; if ("D".equals(code)) return "取送车"; if ("O".equals(code)) return "上门专项";
        return code;
    }

    /** 抢单大厅（P1-09/P1-10）：不合格订单不进抢单池；用与后端一致的六项谓词判定。 */
    @GetMapping("/hall")
    public Result<Map<String, Object>> hall(@RequestParam(required = false) Integer page,
                                            @RequestParam(required = false) Integer pageSize) {
        PageUtil pu = PageUtil.of(page, pageSize);
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("status", OrderStatusEnum.WAITING_ACCEPT.getCode());
        params.put("offset", pu.getOffset()); params.put("limit", pu.getLimit());
        List<Order> orders = orderMapper.selectList(params);
        List<Map<String, Object>> tasks = new ArrayList<Map<String, Object>>();
        for (Order o : orders) {
            if (!orderService.isEligible(o, currentWorker())) continue; // P1-09 服务端最终资格判定
            int lvl = o.getServiceLevel() == null ? 2 : o.getServiceLevel();
            Map<String, Object> m = new HashMap<String, Object>();
            m.put("orderId", o.getId()); m.put("orderNo", o.getOrderNo());
            m.put("tier", lvl == 1 ? "basic" : lvl == 3 ? "premium" : "standard");
            m.put("serviceName", certName("C" + lvl));
            m.put("modeText", BusinessConstants.MODE_ONSITE.equals(o.getServiceMode()) ? "上门" : "到店");
            m.put("feeFen", o.getPaidAmount() == null ? 0 : o.getPaidAmount().multiply(new java.math.BigDecimal(100)).intValue());
            m.put("requiredCert", "C" + lvl);
            m.put("eligible", true);
            m.put("countdown", 90);
            tasks.add(m);
        }
        Map<String, Object> data = new HashMap<String, Object>(); data.put("tasks", tasks);
        return Result.success(data);
    }

    @PostMapping("/orders/{id}/grab")
    public Result<Map<String, Object>> grab(@PathVariable Long id, @RequestBody(required = false) Map<String, Object> body) {
        orderService.acceptOrder(id, currentWorker());
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("grabbed", true); data.put("lockUntil", new Date(System.currentTimeMillis() + 90000));
        return Result.success(data);
    }

    @GetMapping("/orders/{id}")
    public Result<Map<String, Object>> task(@PathVariable Long id) {
        Order o = orderMapper.selectById(id);
        if (o == null) return Result.fail(40000, "订单不存在");
        if (!Objects.equals(o.getWorkerId(), currentWorker()) && !LoginContext.isAdmin()) {
            return Result.fail(40300, "非本人接单，无权查看");
        }
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("order", o); data.put("evidence", evidenceMapper.selectByOrder(id));
        // P1-05：E2=到店签到收车开始计时，E3=交车验收释放工位（不是两道洗车工序）
        data.put("steps", Arrays.asList("E1 取车验车拍照", "E2 到店签到收车(开始计时)", "E3 交车验收(释放工位)", "E4 完成确认"));
        return Result.success(data);
    }

    /** 证据上传（P1-19）：校验订单归属 + 订单状态 + E1~E4 顺序 */
    @PostMapping("/evidence")
    public Result<Map<String, Object>> evidence(@RequestBody Map<String, Object> body) {
        String orderNo = String.valueOf(body.get("orderNo"));
        String stage = String.valueOf(body.get("stage"));
        Order o = orderMapper.selectByOrderNo(orderNo);
        if (o == null) return Result.fail(40000, "订单不存在");
        if (!Objects.equals(o.getWorkerId(), currentWorker()) && !LoginContext.isAdmin()) {
            return Result.fail(40300, "非本人订单，禁止上传证据");
        }
        if (o.getStatus() == null || o.getStatus() < 30 || o.getStatus() > 70) {
            return Result.fail(40900, "订单当前状态不允许上传证据");
        }
        String[] seq = {"E1","E2","E3","E4"};
        int idx = Arrays.asList(seq).indexOf(stage);
        if (idx < 0) return Result.fail(40000, "未知证据阶段：" + stage);
        if (idx > 0) {
            String prev = seq[idx - 1];
            if (evidenceMapper.countByStage(o.getId(), prev) == 0)
                return Result.fail(409, "请先完成上一步" + prev + "证据上传");
        }
        OrderEvidence e = new OrderEvidence();
        e.setOrderId(o.getId()); e.setStage(stage);
        e.setImageUrl("/mock/evidence/" + orderNo + "_" + stage + ".jpg");
        e.setOperatorId(currentWorker());
        evidenceMapper.insert(e);
        // BUG-P0-07：E4 完成确认后推进订单至完成(80)，事务内触发结算/释放工位/重算评分
        Integer newStatus = null;
        if ("E4".equals(stage)) {
            Order done = orderService.progress(o.getId(), currentWorker(), OrderStatusEnum.COMPLETED.getCode());
            newStatus = done.getStatus();
        }
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("saved", true); data.put("stage", stage); data.put("orderStatus", newStatus);
        return Result.success(data);
    }

    /** BUG-P0-07：履约节点上报（取车/到店/开工/完工），按状态机推进，不可回退 */
    @PostMapping("/orders/{id}/progress")
    public Result<Map<String, Object>> progress(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        int toStatus = body.get("status") == null ? 0 : Integer.parseInt(String.valueOf(body.get("status")));
        Order o = orderService.progress(id, currentWorker(), toStatus);
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("orderId", o.getId()); data.put("status", o.getStatus());
        data.put("statusDesc", OrderStatusEnum.descOf(o.getStatus()));
        return Result.success(data);
    }

    @GetMapping("/exams")
    public Result<Map<String, Object>> exams(@RequestParam(required = false) String cert) {
        // P0-05：按 cert 找对应 exam，默认 C1 最低门槛
        String code = cert == null || cert.trim().isEmpty() ? "C1" : cert.toUpperCase();
        Long examId = examIdOf(code);
        Exam exam = examRecordMapper.selectExamById(examId);
        List<Question> qs = examRecordMapper.selectQuestions(examId);
        List<Map<String, Object>> out = new ArrayList<Map<String, Object>>();
        for (Question q : qs) {
            Map<String, Object> m = new HashMap<String, Object>();
            m.put("id", q.getId()); m.put("q", q.getTitle());
            m.put("options", Arrays.asList(q.getOptionA(), q.getOptionB(), q.getOptionC(), q.getOptionD()));
            out.add(m);
        }
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("cert", code); data.put("questions", out);
        data.put("passScore", exam == null ? CertConstants.PASS_SCORE : exam.getPassScore());
        return Result.success(data);
    }

    /** exam code -> id：C1=4 C2=5（P0-05 新增题库），C3=1 D=2 O=3 */
    private Long examIdOf(String cert) {
        if ("C1".equals(cert)) return 4L;
        if ("C2".equals(cert)) return 5L;
        if ("D".equals(cert)) return 2L;
        if ("O".equals(cert)) return 3L;
        return 1L; // C3
    }

    /**
     * 交卷（P0-04 写认证 / P0-06 防越级 / P2-04 及格线取 exam / P2-05 答卷强转防护）。
     * 认证等级由所考 exam 决定，忽略请求体中的 cert，防止越级伪造。
     */
    @PostMapping("/exams/submit")
    public Result<Map<String, Object>> submitExam(@RequestBody Map<String, Object> body) {
        String cert = body.get("cert") == null ? "C1" : String.valueOf(body.get("cert")).toUpperCase();
        Long workerId = currentWorker();
        Long examId = examIdOf(cert);
        Exam exam = examRecordMapper.selectExamById(examId);
        if (exam == null) return Result.fail(40000, "考试不存在：" + cert);
        String certCode = exam.getCertCode();
        int passScore = exam.getPassScore() == null ? CertConstants.PASS_SCORE : exam.getPassScore();

        // P1-10：递进校验——考 C2 须先持 C1，考 C3 须先持 C2（未通过高级认证不可跳级）
        String prerequisite = "C2".equals(certCode) ? "C1" : "C3".equals(certCode) ? "C2" : null;
        if (prerequisite != null) {
            Certification pre = certificationMapper.selectByWorkerAndCode(workerId, prerequisite);
            Date now = new Date();
            if (pre == null || !"PASSED".equalsIgnoreCase(pre.getStatus())
                    || (pre.getExpireAt() != null && pre.getExpireAt().before(now))) {
                return Result.fail(40300, "须先通过" + prerequisite + "认证方可报考" + certCode);
            }
        }

        // P1-10：24h 补考冷却 + 每期最多 3 次（服务端判定，不信任客户端）
        Date last = examRecordMapper.selectLastSubmitted(workerId, certCode);
        if (last != null && System.currentTimeMillis() - last.getTime() < 24L * 3600 * 1000) {
            return Result.fail(40900, "补考需间隔 24 小时");
        }
        int attempts = examRecordMapper.countByWorkerAndCert(workerId, certCode);
        if (attempts >= 3) {
            return Result.fail(40900, "本期考试次数已达上限（3 次）");
        }

        Map<String, Object> answers;
        Object raw = body.get("answers");
        if (!(raw instanceof Map)) return Result.fail(40000, "answers 必须为 {题号:选项} 对象");
        answers = (Map<String, Object>) raw;

        List<Question> qs = examRecordMapper.selectQuestions(examId);
        int score = 0;
        StringBuilder snapshot = new StringBuilder();
        for (Question q : qs) {
            String ans = String.valueOf(answers.get(String.valueOf(q.getId())));
            // 试卷快照：题号 + 提交答案（可回溯卷版本与作答，不存正确答案明文）
            snapshot.append(q.getId()).append(':').append(ans).append(';');
            if (q.getCorrectOption() != null && q.getCorrectOption().equalsIgnoreCase(ans)) {
                score += q.getScore() == null ? 0 : q.getScore();
            }
        }
        boolean passed = score >= passScore;
        int switchCount = body.get("switchCount") == null ? 0
                : Integer.parseInt(String.valueOf(body.get("switchCount")));

        ExamRecord rec = new ExamRecord();
        rec.setWorkerId(workerId); rec.setExamId(examId); rec.setCertCode(certCode);
        rec.setScore(score); rec.setResult(passed ? "PASS" : "FAIL"); rec.setSubmittedAt(new Date());
        rec.setSwitchCount(switchCount); rec.setPaperSnapshot(snapshot.toString());
        examRecordMapper.insert(rec);

        // 考试通过写认证表；认证等级来自 exam，忽略请求体 cert，防止越级
        if (passed) {
            Certification c = new Certification();
            c.setWorkerId(workerId);
            c.setCertCode(certCode);
            c.setExamScore(score);
            c.setStatus(CertConstants.EXAM_PASSED);
            c.setPassedAt(new Date());
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.MONTH, 12);
            c.setExpireAt(cal.getTime());
            certificationMapper.upsert(c);
            // P1-11：写入人工复核队列（48h 内复核；复核接口可撤销认证）
            certReviewMapper.insert(workerId, certCode, rec.getId());
        }
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("score", score); data.put("passed", passed); data.put("passScore", passScore);
        data.put("certUnlocked", passed); data.put("cert", certCode);
        data.put("attempts", attempts + 1); data.put("switchCount", switchCount);
        return Result.success(data);
    }

    @GetMapping("/income")
    public Result<Map<String, Object>> income(@RequestParam(required = false) Integer page,
                                              @RequestParam(required = false) Integer pageSize) {
        PageUtil pu = PageUtil.of(page, pageSize);
        // P0-20：按当前登录洗车工过滤，杜绝拉取全平台结算单
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("accountType", "WORKER"); params.put("accountId", currentWorker());
        params.put("workerId", currentWorker());
        params.put("offset", pu.getOffset()); params.put("limit", pu.getLimit());
        List<com.xiche.settlement.entity.Settlement> bills = settlementMapper.selectByWorker(params);
        // P1-16：收入总额由真实结算单汇总，不再硬编码 6420
        long totalFen = 0;
        for (com.xiche.settlement.entity.Settlement s : bills) {
            if (s.getWorkerNet() != null) totalFen += s.getWorkerNet().multiply(new BigDecimal(100)).longValue();
        }
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("bills", bills); data.put("totalFen", totalFen);
        data.put("rateNote", "STORE抽成40% / ONSITE抽成35%，仅对劳务收入L计提");
        return Result.success(data);
    }
}
