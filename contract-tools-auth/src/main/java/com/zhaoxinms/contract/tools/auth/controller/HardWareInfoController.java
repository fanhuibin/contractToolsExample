package com.zhaoxinms.contract.tools.auth.controller;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.zhaoxinms.contract.tools.api.common.ApiResponse;
import com.zhaoxinms.contract.tools.auth.core.service.AServerInfos;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

/**
 * 服务器硬件信息获取API
 * 
 * <p>提供获取服务器硬件信息的接口，用于授权绑定和验证</p>
 *
 * @author appleyk
 * @version V.0.2.1
 * @date 2020/8/21
 */
@Api(tags = "硬件信息管理")
@CrossOrigin
@RestController
@RequestMapping("/api/license")
@ConditionalOnProperty(prefix = "zhaoxin.auth", name = "enabled", havingValue = "true", matchIfMissing = false)
public class HardWareInfoController {

    /**
     * 获取服务器硬件信息
     * 
     * @param osName 操作系统类型，如果为空则自动判断
     * @return 服务器硬件信息（主板序列号、CPU序列号、MAC地址等）
     */
    @GetMapping("/getServerInfos")
    @ApiOperation(value = "获取服务器硬件信息", notes = "获取服务器的主板序列号、CPU序列号、MAC地址等硬件信息，用于授权绑定")
    public ApiResponse<Object> getServerInfos(
            @ApiParam(value = "操作系统类型", example = "Windows") 
            @RequestParam(value = "osName", required = false) String osName) {
        try {
            Object serverInfos = AServerInfos.getServer(osName).getServerInfos();
            return ApiResponse.success(serverInfos);
        } catch (Exception e) {
            return ApiResponse.serverError().errorDetail(e.getMessage());
        }
    }
}

