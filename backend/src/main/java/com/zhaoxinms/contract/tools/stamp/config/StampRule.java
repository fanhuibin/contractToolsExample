package com.zhaoxinms.contract.tools.stamp.config;

import java.util.List;

public class StampRule {
	// 规则编码（如 company_seal / finance_seal 等）
	private String code;
	// 规则类型：normal（普通盖章），riding（骑缝章）
	private String type;
	// 当对应元素为空时，注入的可见替代值（可选）
	private String insertValue;
	// 规则自定义关键词（可选，若为空使用注入的 SEAL_ 前缀或默认关键词）
	private List<String> keywords;
	// 指定印章图片路径（可选，若为空使用默认 stamp.png）
	private String image;

	public String getCode() { return code; }
	public void setCode(String code) { this.code = code; }
	public String getType() { return type; }
	public void setType(String type) { this.type = type; }
	public String getInsertValue() { return insertValue; }
	public void setInsertValue(String insertValue) { this.insertValue = insertValue; }
	public List<String> getKeywords() { return keywords; }
	public void setKeywords(List<String> keywords) { this.keywords = keywords; }
	public String getImage() { return image; }
	public void setImage(String image) { this.image = image; }
}
