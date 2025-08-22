package com.zhaoxinms.contract.tools.merge.model;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
/**
 * 待合成的合同内容
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocContent {
	private String key;
	private String content;
}
