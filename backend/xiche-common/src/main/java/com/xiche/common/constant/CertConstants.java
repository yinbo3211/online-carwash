package com.xiche.common.constant;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 洗车工认证口径（业务口径 5）：
 * C1 外观 / C2 标准 / C3 精细；D 取送车专项、O 上门专项。
 * 未通过对应考试不可接对应订单。
 */
public final class CertConstants {
    private CertConstants() {
    }

    public enum CertCode {
        C1("C1", "外观清洁", 1),
        C2("C2", "标准清洁", 2),
        C3("C3", "精细清洁", 3),
        D("D", "取送车专项", 0),
        O("O", "上门专项", 0);

        private final String code;
        private final String desc;
        private final int level;

        CertCode(String code, String desc, int level) {
            this.code = code;
            this.desc = desc;
            this.level = level;
        }

        public String getCode() {
            return code;
        }

        public String getDesc() {
            return desc;
        }

        public int getLevel() {
            return level;
        }
    }

    public static final int PASS_SCORE = 80;
    public static final String EXAM_PASSED = "PASSED";
    public static final String EXAM_FAILED = "FAILED";

    /** 服务等级 -> 必需技能认证 */
    public static CertCode skillCertOf(int serviceLevel) {
        switch (serviceLevel) {
            case 1: return CertCode.C1;
            case 2: return CertCode.C2;
            case 3: return CertCode.C3;
            default: throw new IllegalArgumentException("未知服务等级:" + serviceLevel);
        }
    }

    /**
     * 按服务等级与履约模式算出该单全部必需认证。
     * 门店模式 STORE 只需技能认证；上门 ONSITE 加 O；取送车 DELIVERY 加 D。
     */
    public static Set<CertCode> requiredCerts(int serviceLevel, String fulfillMode) {
        Set<CertCode> set = new LinkedHashSet<CertCode>();
        set.add(skillCertOf(serviceLevel));
        if ("ONSITE".equals(fulfillMode)) {
            set.add(CertCode.O);
        } else if ("DELIVERY".equals(fulfillMode)) {
            set.add(CertCode.D);
        }
        return Collections.unmodifiableSet(set);
    }

    public static Set<CertCode> allCerts() {
        return Collections.unmodifiableSet(new LinkedHashSet<CertCode>(Arrays.asList(CertCode.values())));
    }
}
