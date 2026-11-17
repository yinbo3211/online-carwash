package com.xiche.user.controller;

import com.xiche.common.BizException;
import com.xiche.common.Result;
import com.xiche.common.constant.BusinessConstants;
import com.xiche.common.constant.OrderStatusEnum;
import com.xiche.common.context.LoginContext;
import com.xiche.common.util.PageUtil;
import com.xiche.auth.service.AuditService;
import com.xiche.order.dao.OrderMapper;
import com.xiche.order.entity.Order;
import com.xiche.order.service.OrderService;
import com.xiche.payment.entity.PaymentOrder;
import com.xiche.payment.service.PaymentService;
import com.xiche.settlement.service.SettlementService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.*;

/** 车主端 REST API（/api/user/）。金额对外一律「分」，内部 BigDecimal 元。 */
@RestController
@RequestMapping("/api/user")
public class UserController {
    @Resource private OrderMapper orderMapper;
    @Resource private OrderService orderService;
    @Resource private PaymentService paymentService;
    @Resource private SettlementService settlementService;
    @Resource private AuditService auditService;

    @GetMapping("/home")
    public Result<Map<String, Object>> home() {
        List<Map<String, Object>> tiers = new ArrayList<Map<String, Object>>();
        tiers.add(tier("basic", "外观清洁", BusinessConstants.PRICE_STORE_APPEARANCE, "车身/轮毂外侧/玻璃外表面", false));
        tiers.add(tier("standard", "标准清洁", BusinessConstants.PRICE_STORE_STANDARD, "外观+内饰吸尘/脚垫/中控", true));
        tiers.add(tier("premium", "精细清洁", BusinessConstants.PRICE_STORE_DETAIL, "边缝/轮毂内侧/座椅缝隙/养护", false));
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("tiers", tiers);
        data.put("banners", Collections.emptyList());
        data.put("envFlag", BusinessConstants.ENV_FLAG);
        return Result.success(data);
    }

    private Map<String, Object> tier(String key, String name, java.math.BigDecimal yuan, String desc, boolean rec) {
        Map<String, Object> m = new HashMap<String, Object>();
        m.put("key", key); m.put("name", name);
        m.put("priceFen", yuan.multiply(new java.math.BigDecimal(100)).intValue());
        m.put("desc", desc); m.put("recommend", rec);
        return m;
    }

    /** 下单：L1 强制不可取消，requestId 幂等（P1-12：缺失时用稳定业务键，不退化成字符串 "null"） */
    @PostMapping("/orders")
    public Result<Map<String, Object>> createOrder(@RequestBody Map<String, Object> body) {
        String tier = String.valueOf(body.get("tier"));
        String mode = body.get("serviceMode") == null ? "STORE" : String.valueOf(body.get("serviceMode"));
        Object ridObj = body.get("requestId");
        if (ridObj == null || String.valueOf(ridObj).trim().isEmpty() || "null".equals(String.valueOf(ridObj))) {
            throw new BizException(40000, "requestId 幂等键必填，防止重复下单");
        }
        String requestId = String.valueOf(ridObj);
        int level = "basic".equals(tier) ? 1 : "premium".equals(tier) ? 3 : 2;
        java.math.BigDecimal price = BusinessConstants.priceOf(level, mode);
        if (price == null) return Result.fail(40000, "该档位不支持当前服务形态");
        String orderNo = "XC-T-" + requestId;
        if (orderMapper.selectByOrderNo(orderNo) != null) {
            return Result.success(orderResp(orderMapper.selectByOrderNo(orderNo)));
        }

        // P0-01：身份来自登录上下文，不再硬编码
        Long userId = LoginContext.userId();
        // P1-20：ONSITE 需地址
        Object addrObj = body.get("addressId");
        Long addressId = addrObj == null ? null : Long.valueOf(String.valueOf(addrObj));
        if ((BusinessConstants.MODE_ONSITE.equals(mode) || BusinessConstants.MODE_DELIVERY.equals(mode))
                && addressId == null) {
            return Result.fail(40000, "上门/取送车订单必须提供服务地址 addressId");
        }

        java.math.BigDecimal l1 = BusinessConstants.INSURANCE_L1_PREMIUM;
        Order o = new Order();
        o.setOrderNo(orderNo); o.setUserId(userId); o.setStoreId(1L); o.setServiceSkuId((long) level);
        o.setServiceLevel(level); o.setServiceMode(mode); o.setFulfillMode(mode);
        o.setStatus(OrderStatusEnum.PENDING_PAYMENT.getCode());
        o.setPaidAmount(price); o.setPlatformSubsidy(java.math.BigDecimal.ZERO);
        o.setStoreBayFee(mode.equals("STORE") ? new java.math.BigDecimal(level == 3 ? 54 : level == 2 ? 24 : 12) : java.math.BigDecimal.ZERO);
        o.setInsuranceFee(l1); o.setAdvanceAmount(java.math.BigDecimal.ZERO);
        o.setEnvFlag(BusinessConstants.ENV_FLAG); o.setRemark("V1测试下单");
        o.setAddressId(addressId);
        // P0-11：固化三级快照（价格/费率/卡状态）+ 免佣卡 card_snapshot
        o.setPriceSnapshot("PV-V1-20261101|L" + level + "|" + mode + "|" + price.toPlainString());
        o.setRateSnapshot(BusinessConstants.commissionRateOf(mode).toPlainString());
        Object cardObj = body.get("cardSnapshot");
        o.setCardSnapshot(cardObj == null ? "NONE" : String.valueOf(cardObj));
        orderMapper.insert(o);
        auditService.record("USER", userId, "ORDER_CREATE", "ORDER", orderNo, "下单 tier=" + tier + " mode=" + mode, null);
        return Result.success(orderResp(o));
    }

    private Map<String, Object> orderResp(Order o) {
        Map<String, Object> m = new HashMap<String, Object>();
        m.put("orderId", o.getId()); m.put("orderNo", o.getOrderNo());
        m.put("status", o.getStatus()); m.put("statusIndex", statusIndex(o.getStatus()));
        m.put("amountFen", o.getPaidAmount() == null ? 0 : o.getPaidAmount().multiply(new java.math.BigDecimal(100)).intValue());
        m.put("envFlag", o.getEnvFlag());
        return m;
    }

    private int statusIndex(int code) {
        int[] codes = {10,20,30,40,50,60,70,80};
        for (int i = 0; i < codes.length; i++) if (codes[i] == code) return i;
        return 0;
    }

    @GetMapping("/orders/{id}")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        Order o = orderMapper.selectById(id);
        if (o == null) return Result.fail(40000, "订单不存在");
        // P0-20/归属校验：只能看自己的订单（管理员除外）
        if (!LoginContext.isAdmin() && !Objects.equals(o.getUserId(), LoginContext.userId())) {
            return Result.fail(40300, "无权查看他人订单");
        }
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("order", orderResp(o));
        data.put("settlement", settlementService.queryByOrderId(id));
        return Result.success(data);
    }

    @GetMapping("/orders")
    public Result<Map<String, Object>> list(@RequestParam(required = false) Integer page,
                                            @RequestParam(required = false) Integer pageSize) {
        PageUtil pu = PageUtil.of(page, pageSize);
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("userId", LoginContext.userId()); // P0-01
        params.put("offset", pu.getOffset()); params.put("limit", pu.getLimit());
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("list", orderMapper.selectList(params));
        data.put("total", orderMapper.countList(params));
        return Result.success(data);
    }

    /** 模拟支付：不接真实资金通道，成功文案橙色 */
    @PostMapping("/orders/{id}/simulate-pay")
    public Result<Map<String, Object>> simulatePay(@PathVariable Long id, @RequestBody(required = false) Map<String, Object> body) {
        PaymentOrder p = paymentService.mockPay(id, LoginContext.userId());
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("paid", true); data.put("paidAt", p.getPaidAt());
        data.put("paymentNo", p.getPaymentNo());
        data.put("amountFen", p.getAmount() == null ? 0 : p.getAmount().multiply(new java.math.BigDecimal(100)).intValue());
        data.put("successMessage", p.getSuccessMessage()); data.put("successColor", p.getSuccessColor());
        auditService.record("USER", LoginContext.userId(), "MOCK_PAY", "ORDER", String.valueOf(id), "模拟支付", null);
        return Result.success(data, "模拟支付成功·测试数据，不产生真实资金收付");
    }

    /** P0-09：取消走状态机，仅待支付/待接单可取消，且只能取消自己的订单 */
    @PostMapping("/orders/{id}/cancel")
    public Result<Map<String, Object>> cancel(@PathVariable Long id, @RequestBody(required = false) Map<String, Object> body) {
        Order o = orderMapper.selectById(id);
        if (o == null) return Result.fail(40000, "订单不存在");
        if (!LoginContext.isAdmin() && !Objects.equals(o.getUserId(), LoginContext.userId())) {
            return Result.fail(40300, "无权取消他人订单");
        }
        if (!OrderStatusEnum.canCancel(o.getStatus())) {
            return Result.fail(40900, "当前状态(" + OrderStatusEnum.descOf(o.getStatus()) + ")不可取消");
        }
        orderMapper.updateStatus(id, OrderStatusEnum.CANCELLED.getCode());
        auditService.record("USER", LoginContext.userId(), "ORDER_CANCEL", "ORDER", String.valueOf(id),
                "取消订单 原状态=" + OrderStatusEnum.descOf(o.getStatus()), null);
        Map<String, Object> data = new HashMap<String, Object>(); data.put("canceled", true);
        return Result.success(data);
    }
}
