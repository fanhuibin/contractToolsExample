package com.zhaoxinms.contract.tools.ruleextract.engine.matcher;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;

/**
 * 位置匹配器
 * 规则格式：
 * {
 *   "type": "position",
 *   "page": 1,
 *   "area": {
 *     "x": 100,
 *     "y": 200,
 *     "width": 300,
 *     "height": 50
 *   }
 * }
 * 
 * 注意：此匹配器需要配合OCR的位置信息使用
 * 
 * @author zhaoxin
 * @since 2024-10-09
 */
@Slf4j
public class PositionMatcher {

    /**
     * 使用位置匹配
     * 此方法需要在有位置信息的情况下使用
     * 
     * @param content 文本内容（当前简化实现，暂不支持）
     * @param ruleConfig 规则配置
     * @return 匹配结果
     */
    public MatchResult match(String content, JSONObject ruleConfig) {
        // 位置匹配需要配合OCR的位置信息
        // 当前简化实现，返回失败
        // 实际使用时需要传入charBoxes等位置信息
        log.warn("位置匹配器需要配合OCR位置信息使用，当前版本暂不支持");
        return MatchResult.failed();
    }

    /**
     * 使用位置匹配（带位置信息）
     * 
     * @param charBoxes 字符框列表（OCR提供）
     * @param ruleConfig 规则配置
     * @return 匹配结果
     */
    public MatchResult matchWithPosition(Object charBoxes, JSONObject ruleConfig) {
        try {
            Integer page = ruleConfig.getInteger("page");
            if (page == null) {
                page = 1;
            }
            
            JSONObject area = ruleConfig.getJSONObject("area");
            
            if (area == null) {
                log.warn("位置规则缺少area参数");
                return MatchResult.failed();
            }

            Integer x = area.getInteger("x");
            if (x == null) x = 0;
            Integer y = area.getInteger("y");
            if (y == null) y = 0;
            Integer width = area.getInteger("width");
            if (width == null) width = 0;
            Integer height = area.getInteger("height");
            if (height == null) height = 0;

            // TODO: 实现基于charBoxes的位置匹配
            // 1. 筛选指定页面的字符
            // 2. 筛选指定区域内的字符
            // 3. 拼接成文本返回

            log.info("位置匹配: page={}, area=[{},{},{},{}]", page, x, y, width, height);
            
            return MatchResult.failed();

        } catch (Exception e) {
            log.error("位置匹配失败", e);
            return MatchResult.failed();
        }
    }
}
