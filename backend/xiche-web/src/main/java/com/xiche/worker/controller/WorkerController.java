package com.xiche.worker.controller;

import com.xiche.common.Result;
import com.xiche.common.constant.BusinessConstants;
import com.xiche.common.util.PageUtil;
import com.xiche.learning.dao.ExamRecordMapper;
import com.xiche.learning.entity.Exam;
import com.xiche.learning.entity.ExamRecord;
import com.xiche.learning.entity.Question;
import com.xiche.order.dao.OrderEvidenceMapper;
import com.xiche.order.dao.OrderMapper;
import com.xiche.order.entity.Order;
import com.xiche.order.entity.OrderEvidence;
import com.xiche.order.service.OrderService;
import com.xiche.settlement.dao.SettlementMapper;
import com.xiche.worker.dao.WorkerMapper;
import com.xiche.worker.entity.Certification;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.*;

/** 洗车工端 REST API（/api/worker/）。 */
@RestController
@RequestMapping("/api/worker")
public class WorkerController {
    @Resource private WorkerMapper workerMapper;
    @Resource private OrderMapper orderMapper;
    @Resource private OrderService orderService;
    @Resource private OrderEvidenceMapper evidenceMapper;
    @Resource private ExamRecordMapper examRecordMapper;
    @Resource private SettlementMapper settlementMapper;

    private Long currentWorker() { return 1L; } // V1 测试：固定王师傅

    @GetMapping("/certs")
    public Result<Map<String, Object>> certs() {
        List<Certification> list = workerMapper.selectCerts(currentWorker());
        List<Map<String, Object>> out = new ArrayList<Map<String, Object>>();
        for (Certification c : list) {
            Map<String, Object> m = new HashMap<String, Object>();
            m.put("key", c.getCertCode()); m.put("name", certName(c.getCertCode()));
            m.put("passed", "PASSED".equalsIgnoreCase(c.getStatus())); m.put("status", c.getStatus());
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

    /** 抢单大厅：待接单订单；资质不足由前端置灰锁定，服务端在 grab 时最终校验 */
    @GetMapping("/hall")
    public Result<Map<String, Object>> hall(@RequestParam(required = false) Integer page,
                                            @RequestParam(required = false) Integer pageSize) {
        PageUtil pu = PageUtil.of(page, pageSize);
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("status", 20); params.put("offset", pu.getOffset()); params.put("limit", pu.getLimit());
        List<Order> orders = orderMapper.selectList(params);
        List<String> passed = new ArrayList<String>();
        for (Certification c : workerMapper.selectCerts(currentWorker())) {
            if ("PASSED".equalsIgnoreCase(c.getStatus())) passed.add(c.getCertCode());
        }
        List<Map<String, Object>> tasks = new ArrayList<Map<String, Object>>();
        for (Order o : orders) {
            Map<String, Object> m = new HashMap<String, Object>();
            m.put("orderId", o.getId()); m.put("orderNo", o.getOrderNo());
            int lvl = o.getServiceLevel() == null ? 2 : o.getServiceLevel();
            m.put("tier", lvl == 1 ? "basic" : lvl == 3 ? "premium" : "standard");
            m.put("serviceName", certName("C" + lvl));
            m.put("modeText", BusinessConstants.MODE_ONSITE.equals(o.getServiceMode()) ? "上门" : "到店");
            m.put("feeFen", o.getPaidAmount() == null ? 0 : o.getPaidAmount().multiply(new java.math.BigDecimal(100)).intValue());
            String req = "C" + lvl;
            m.put("requiredCert", req);
            m.put("eligible", passed.contains(req)); // 前端据此置灰锁定
            m.put("countdown", 90);
            m.put("address", "见订单详情");
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
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("order", o); data.put("evidence", evidenceMapper.selectByOrder(id));
        data.put("steps", Arrays.asList("E1","E2","E3","E4"));
        return Result.success(data);
    }

    /** 证据上传：E1~E4 顺序解锁（完成上一步才允许下一步） */
    @PostMapping("/evidence")
    public Result<Map<String, Object>> evidence(@RequestBody Map<String, Object> body) {
        String orderNo = String.valueOf(body.get("orderNo"));
        String stage = String.valueOf(body.get("stage"));
        Order o = orderMapper.selectByOrderNo(orderNo);
        if (o == null) return Result.fail(40000, "订单不存在");
        String[] seq = {"E1","E2","E3","E4"};
        int idx = Arrays.asList(seq).indexOf(stage);
        if (idx > 0) {
            String prev = seq[idx - 1];
            if (evidenceMapper.countByStage(o.getId(), prev) == 0)
                return Result.fail(409, "请先完成上一步" + prev + "证据上传");
        }
        OrderEvidence e = new OrderEvidence();
        e.setOrderId(o.getId()); e.setStage(stage);
        e.setImageUrl("/mock/evidence/" + orderNo + "_" + stage + ".jpg"); // V1 模拟水印图
        e.setOperatorId(currentWorker());
        evidenceMapper.insert(e);
        Map<String, Object> data = new HashMap<String, Object>(); data.put("saved", true); data.put("stage", stage);
        return Result.success(data);
    }

    @GetMapping("/exams")
    public Result<Map<String, Object>> exams() {
        Exam exam = examRecordMapper.selectExamById(1L);
        List<Question> qs = examRecordMapper.selectQuestions(1L);
        List<Map<String, Object>> out = new ArrayList<Map<String, Object>>();
        for (Question q : qs) {
            Map<String, Object> m = new HashMap<String, Object>();
            m.put("id", q.getId()); m.put("q", q.getTitle());
            m.put("options", Arrays.asList(q.getOptionA(), q.getOptionB(), q.getOptionC(), q.getOptionD()));
            out.add(m); // 不返回正确答案，交卷服务端判分
        }
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("questions", out);
        data.put("passScore", exam == null ? 80 : exam.getPassScore());
        return Result.success(data);
    }

    /** 交卷：服务端按题库判分，>=80 分解锁对应认证 */
    @PostMapping("/exams/submit")
    public Result<Map<String, Object>> submitExam(@RequestBody Map<String, Object> body) {
        String cert = body.get("cert") == null ? "C3" : String.valueOf(body.get("cert"));
        @SuppressWarnings("unchecked")
        Map<String, Object> answers = (Map<String, Object>) body.get("answers");
        List<Question> qs = examRecordMapper.selectQuestions(1L);
        int score = 0;
        for (Question q : qs) {
            String ans = answers == null ? null : String.valueOf(answers.get(String.valueOf(q.getId())));
            if (q.getCorrectOption() != null && q.getCorrectOption().equalsIgnoreCase(ans)) {
                score += q.getScore() == null ? 0 : q.getScore();
            }
        }
        boolean passed = score >= 80;
        ExamRecord rec = new ExamRecord();
        rec.setWorkerId(currentWorker()); rec.setExamId(1L); rec.setCertCode(cert);
        rec.setScore(score); rec.setResult(passed ? "PASS" : "FAIL"); rec.setSubmittedAt(new Date());
        examRecordMapper.insert(rec);
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("score", score); data.put("passed", passed); data.put("certUnlocked", passed);
        return Result.success(data);
    }

    @GetMapping("/income")
    public Result<Map<String, Object>> income(@RequestParam(required = false) Integer page,
                                              @RequestParam(required = false) Integer pageSize) {
        PageUtil pu = PageUtil.of(page, pageSize);
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("accountType", "WORKER"); params.put("accountId", currentWorker());
        params.put("offset", pu.getOffset()); params.put("limit", pu.getLimit());
        // 复用结算台账视图：此处返回结算单摘要（V1 测试）
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("bills", settlementMapper.selectList(params));
        data.put("totalFen", 6420);
        data.put("rateNote", "STORE抽成40% / ONSITE抽成35%，仅对劳务收入L计提");
        return Result.success(data);
    }
}
