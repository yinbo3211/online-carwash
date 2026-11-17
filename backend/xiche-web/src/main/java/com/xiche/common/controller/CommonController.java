package com.xiche.common.controller;

import com.xiche.common.BizException;
import com.xiche.common.Result;
import com.xiche.common.cache.LocalCacheService;
import com.xiche.common.constant.BusinessConstants;
import com.xiche.common.constant.CacheConstants;
import com.xiche.common.context.LoginContext;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import javax.annotation.Resource;

import java.io.File;
import java.util.*;

/**
 * 公共接口：证据图上传。
 * P2-06：由空壳改为真实接收 MultipartFile 并校验、落本地目录（V1 测试环境），
 * 生产应替换为对象存储 + GPS/时间水印叠加。
 */
@RestController
@RequestMapping("/api/common")
public class CommonController {
    private static final String UPLOAD_DIR = System.getProperty("java.io.tmpdir") + "/xiche-evidence/";
    @Resource private LocalCacheService cacheService;

    /** P1-05：缓存健康看板（各域条目数 + A/B/C 分级声明；不含 C 级数据） */
    @GetMapping("/cache/stats")
    public Result<Map<String, Object>> cacheStats() {
        Map<String, Object> d = new HashMap<String, Object>();
        d.put("entries", cacheService.stats());
        Map<String, String> levels = new LinkedHashMap<String, String>();
        for (String domain : new String[]{"catalog", "learning", "auth", "store", "order",
                "payment", "settlement", "insurance", "coupon"}) {
            levels.put(domain, CacheConstants.levelOf(domain).name());
        }
        d.put("levels", levels);
        d.put("note", "A/B 级可缓存；C 级(资金/订单/产能/保单)fail-closed 禁止缓存");
        return Result.success(d);
    }

    @PostMapping("/upload")
    public Result<Map<String, Object>> upload(@RequestParam(value = "file", required = false) MultipartFile file,
                                              @RequestParam(value = "orderNo", required = false) String orderNo,
                                              @RequestParam(value = "stage", required = false) String stage) {
        if (file == null || file.isEmpty()) throw new BizException(40000, "上传文件不能为空");
        if (orderNo == null || orderNo.trim().isEmpty()) throw new BizException(40000, "orderNo 必填");
        // BUG-P1-15：orderNo/stage 白名单校验，杜绝目录穿越（../ 或绝对路径注入文件名）
        if (!orderNo.matches("[A-Za-z0-9_-]{1,64}")) throw new BizException(40000, "orderNo 格式非法");
        if (stage != null && !stage.matches("E[1-4]")) throw new BizException(40000, "证据阶段非法");
        if (file.getSize() > 5 * 1024 * 1024) throw new BizException(40000, "单文件不得超过5MB");
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BizException(40000, "仅允许上传图片");
        }
        try {
            File dir = new File(UPLOAD_DIR);
            if (!dir.exists()) dir.mkdirs();
            String ext = contentType.substring(contentType.indexOf('/') + 1);
            String name = orderNo + "_" + (stage == null ? "E" : stage) + "_" + LoginContext.workerId() + "." + ext;
            File dest = new File(dir, name);
            file.transferTo(dest); // 真实落盘（V1 本地；生产换对象存储）
            Map<String, Object> d = new HashMap<String, Object>();
            d.put("url", "/evidence/" + name);
            d.put("path", dest.getAbsolutePath());
            d.put("size", file.getSize());
            d.put("envFlag", BusinessConstants.ENV_FLAG);
            d.put("note", "V1测试环境：文件落本地临时目录；生产需对象存储+水印");
            return Result.success(d);
        } catch (Exception e) {
            throw new BizException(50000, "文件保存失败：" + e.getMessage());
        }
    }
}
