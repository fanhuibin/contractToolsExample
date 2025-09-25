package com.zhaoxinms.contract.tools.merge;

import java.util.List;

import com.zhaoxinms.contract.tools.merge.model.DocContent;

/**
 * 合同合成共用类 
 */
public interface Merge {
	/**
	 * 执行合同合成操作
	 */
	public void doMerge(String inputFile, String outputFile, List<DocContent> contents);
}
