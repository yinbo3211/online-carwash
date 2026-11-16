package com.xiche.common.controller;

import com.xiche.common.Result;
import com.xiche.common.constant.BusinessConstants;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

/** 公共接口：证据图上传（V1 模拟，返回占位 URL；真实实现需对象存储+水印叠加）。 */
@RestController
@RequestMapping("/api/common")
public class CommonController {
    @PostMapping("/upload")
    public Result<Map<String, Object>> upload(@RequestParam(value = "file", required = false) MultipartFile file,
                                              @RequestParam(value = "orderNo", required = false) String orderNo,
                                              @RequestParam(value = "stage", required = false) String stage) {
        Map<String, Object> d = new HashMap<String, Object>();
        d.put("url", "/mock/evidence/" + (orderNo == null ? "unknown" : orderNo) + "_" + (stage == null ? "E" : stage) + ".jpg");
        d.put("mock", true); d.put("envFlag", BusinessConstants.ENV_FLAG);
        d.put("note", "V1测试环境：模拟上传，图片不落真实存储");
        return Result.success(d);
    }
}
