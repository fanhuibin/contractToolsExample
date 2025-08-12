package com.zhaoxinms.contract.tools.compare.result;
import com.zhaoxinms.contract.tools.compare.DiffUtil;

import lombok.Data;

@Data
public class CompareResult {
    //当前结果在原文档的位置
    private Position oldPosition;
    //当前结果在新文档的位置
    private Position newPosition;
    //比对结果
    private DiffUtil.Diff diff;
    
    public CompareResult(Position oldPosition, Position newPosition, DiffUtil.Diff diff) {
        this.oldPosition = oldPosition;
        this.newPosition = newPosition;
        this.diff = diff;
    }
}
