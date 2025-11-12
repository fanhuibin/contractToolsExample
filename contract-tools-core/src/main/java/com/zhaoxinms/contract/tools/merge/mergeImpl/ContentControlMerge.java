package com.zhaoxinms.contract.tools.merge.mergeImpl;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFSDT;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.apache.xmlbeans.XmlCursor;
import org.apache.xmlbeans.XmlObject;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTP;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTR;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSdtBlock;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSdtContentBlock;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSdtContentRun;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSdtRun;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTText;

import com.zhaoxinms.contract.tools.merge.Merge;
import com.zhaoxinms.contract.tools.merge.model.DocContent;
import com.zhaoxinms.contract.tools.merge.util.HtmlUtils;

public class ContentControlMerge implements Merge {

	/**
	 * 调试开关 - 控制是否输出调试信息
	 * 设置为 false 可以关闭所有调试输出
	 */
	private static final boolean DEBUG_ENABLED = false;
	
	/**
	 * 调试输出方法 - 只有在调试开关打开时才输出
	 */
	private void debugPrint(String message) {
		if (DEBUG_ENABLED) {
			System.out.println(message);
		}
	}
	
	/**
	 * 调试异常输出方法 - 只有在调试开关打开时才输出
	 */
	private void debugPrintException(String message, Exception e) {
		if (DEBUG_ENABLED) {
			System.out.println(message);
			e.printStackTrace();
		}
	}

	/**
	 * SDT信息类，用于存储找到的SDT信息
	 */
	private static class SdtInfo {
		private String tag;
		private String type;
		private String location;
		
		public SdtInfo(String tag, String type, String location) {
			this.tag = tag;
			this.type = type;
			this.location = location;
		}
		
		@Override
		public String toString() {
			return String.format("Tag: %s, Type: %s, Location: %s", tag, type, location);
		}
	}

	@Override
	public void doMerge(String inputFile, String outputFile, List<DocContent> contents) {
		try (FileInputStream fis = new FileInputStream(inputFile); XWPFDocument document = new XWPFDocument(fis)) {

			// 收集所有SDT信息
			List<SdtInfo> allSdtInfo = new ArrayList<>();
			
			debugPrint("=== 开始扫描文档中的所有SDT控件 ===");
			
			// 遍历文档中的所有内容控件
			for (int i = 0; i < document.getBodyElements().size(); i++) {
				IBodyElement bodyElement = document.getBodyElements().get(i);
				
				if (bodyElement instanceof XWPFParagraph) {
					XWPFParagraph paragraph = (XWPFParagraph) bodyElement;
					
					// 处理段落级别的块级SDT
					CTP ctp = paragraph.getCTP();
					
					// 检查段落中的块级SDT
					for (int j = 0; j < ctp.getSdtList().size(); j++) {
						CTSdtRun sdtRun = ctp.getSdtList().get(j);
						String tag = extractTagFromSdtRun(sdtRun);
						if (tag != null && !tag.isEmpty()) {
							allSdtInfo.add(new SdtInfo(tag, "行内SDT", "段落" + i));
							debugPrint("发现行内SDT - " + allSdtInfo.get(allSdtInfo.size() - 1));
						}
						replaceContentInSDTRun(sdtRun, paragraph, contents);
					}
					
				} else if (bodyElement instanceof XWPFTable) {
					// 处理表格中的内容控件
					XWPFTable table = (XWPFTable) bodyElement;
					processTableSDTs(table, allSdtInfo, contents, "正文表格" + i);
				}
			}
			
			// 处理文档级别的块级SDT
			processDocumentLevelBlockSdt(document, allSdtInfo, contents);
			
			// 处理页眉中的SDT控件
			processHeadersAndFooters(document, allSdtInfo, contents);
			
			// 输出汇总信息
			if (DEBUG_ENABLED) {
				debugPrint("\n=== SDT控件扫描完成 ===");
				debugPrint("共找到 " + allSdtInfo.size() + " 个SDT控件：");
				for (SdtInfo info : allSdtInfo) {
					debugPrint("  " + info);
				}
				debugPrint("========================\n");
			}

			// 保存修改后的文档
			try (FileOutputStream fos = new FileOutputStream(outputFile)) {
				document.write(fos);
			}
		} catch (IOException e) {
			debugPrintException("文件操作异常: " + e.getMessage(), e);
		}
	}
	
	/**
	 * 处理文档级别的块级SDT
	 */
	private void processDocumentLevelBlockSdt(XWPFDocument document, List<SdtInfo> allSdtInfo, List<DocContent> contents) {
		try {
			// 获取文档的底层XML结构
			org.openxmlformats.schemas.wordprocessingml.x2006.main.CTDocument1 ctDocument = document.getDocument();
			if (ctDocument != null && ctDocument.getBody() != null) {
				org.openxmlformats.schemas.wordprocessingml.x2006.main.CTBody body = ctDocument.getBody();
				
				// ===== 第一步：先收集所有需要处理的SDT（只读操作）=====
				List<CTSdtBlock> sdtBlocksToProcess = new ArrayList<>();
				for (int i = 0; i < body.getSdtList().size(); i++) {
					CTSdtBlock sdtBlock = body.getSdtList().get(i);
					String tag = extractTagFromSdtBlock(sdtBlock);
					if (tag != null && !tag.isEmpty()) {
						allSdtInfo.add(new SdtInfo(tag, "块级SDT", "文档级别-块" + i));
						debugPrint("发现块级SDT - " + allSdtInfo.get(allSdtInfo.size() - 1));
						sdtBlocksToProcess.add(sdtBlock);
					}
				}
				
				// ===== 第二步：遍历收集好的列表，修改文档内容 =====
				for (CTSdtBlock sdtBlock : sdtBlocksToProcess) {
					replaceContentInSDTBlock(sdtBlock, contents);
				}
			}
		} catch (Exception e) {
			debugPrintException("处理块级SDT时发生异常: " + e.getMessage(), e);
		}
	}
	
	/**
	 * 处理页眉和页脚中的SDT控件
	 */
	private void processHeadersAndFooters(XWPFDocument document, List<SdtInfo> allSdtInfo, List<DocContent> contents) {
		try {
			debugPrint("=== 开始处理页眉和页脚中的SDT控件 ===");
			
			// 处理页眉
			processHeaders(document, allSdtInfo, contents);
			
			// 处理页脚
			processFooters(document, allSdtInfo, contents);
			
			debugPrint("=== 页眉和页脚SDT控件处理完成 ===");
		} catch (Exception e) {
			debugPrintException("处理页眉页脚SDT时发生异常: " + e.getMessage(), e);
		}
	}
	
	/**
	 * 处理页眉中的SDT控件
	 */
	private void processHeaders(XWPFDocument document, List<SdtInfo> allSdtInfo, List<DocContent> contents) {
		try {
			// 获取所有页眉
			java.util.List<org.apache.poi.xwpf.usermodel.XWPFHeader> headers = document.getHeaderList();
			debugPrint("找到 " + headers.size() + " 个页眉");
			
			for (int headerIdx = 0; headerIdx < headers.size(); headerIdx++) {
				org.apache.poi.xwpf.usermodel.XWPFHeader header = headers.get(headerIdx);
				debugPrint("处理页眉 " + (headerIdx + 1));
				
				// 处理页眉中的段落
				java.util.List<XWPFParagraph> paragraphs = header.getParagraphs();
				for (int paraIdx = 0; paraIdx < paragraphs.size(); paraIdx++) {
					XWPFParagraph paragraph = paragraphs.get(paraIdx);
					
					// 处理段落中的行内SDT
					CTP ctp = paragraph.getCTP();
					for (int sdtIdx = 0; sdtIdx < ctp.getSdtList().size(); sdtIdx++) {
						CTSdtRun sdt = ctp.getSdtList().get(sdtIdx);
						String tag = extractTagFromSdtRun(sdt);
						if (tag != null && !tag.isEmpty()) {
							allSdtInfo.add(new SdtInfo(tag, "页眉行内SDT", 
								String.format("页眉%d-段落%d", headerIdx + 1, paraIdx + 1)));
							debugPrint("发现页眉行内SDT - " + allSdtInfo.get(allSdtInfo.size() - 1));
							replaceContentInSDTRun(sdt, paragraph, contents);
						}
					}
				}
				
				// 处理页眉中的表格
				java.util.List<XWPFTable> tables = header.getTables();
				for (int tableIdx = 0; tableIdx < tables.size(); tableIdx++) {
					XWPFTable table = tables.get(tableIdx);
					processTableSDTs(table, allSdtInfo, contents, 
						String.format("页眉%d-表格%d", headerIdx + 1, tableIdx + 1));
				}
			}
		} catch (Exception e) {
			debugPrintException("处理页眉SDT时发生异常: " + e.getMessage(), e);
		}
	}
	
	/**
	 * 处理页脚中的SDT控件
	 */
	private void processFooters(XWPFDocument document, List<SdtInfo> allSdtInfo, List<DocContent> contents) {
		try {
			// 获取所有页脚
			java.util.List<org.apache.poi.xwpf.usermodel.XWPFFooter> footers = document.getFooterList();
			debugPrint("找到 " + footers.size() + " 个页脚");
			
			for (int footerIdx = 0; footerIdx < footers.size(); footerIdx++) {
				org.apache.poi.xwpf.usermodel.XWPFFooter footer = footers.get(footerIdx);
				debugPrint("处理页脚 " + (footerIdx + 1));
				
				// 处理页脚中的段落
				java.util.List<XWPFParagraph> paragraphs = footer.getParagraphs();
				for (int paraIdx = 0; paraIdx < paragraphs.size(); paraIdx++) {
					XWPFParagraph paragraph = paragraphs.get(paraIdx);
					
					// 处理段落中的行内SDT
					CTP ctp = paragraph.getCTP();
					for (int sdtIdx = 0; sdtIdx < ctp.getSdtList().size(); sdtIdx++) {
						CTSdtRun sdt = ctp.getSdtList().get(sdtIdx);
						String tag = extractTagFromSdtRun(sdt);
						if (tag != null && !tag.isEmpty()) {
							allSdtInfo.add(new SdtInfo(tag, "页脚行内SDT", 
								String.format("页脚%d-段落%d", footerIdx + 1, paraIdx + 1)));
							debugPrint("发现页脚行内SDT - " + allSdtInfo.get(allSdtInfo.size() - 1));
							replaceContentInSDTRun(sdt, paragraph, contents);
						}
					}
				}
				
				// 处理页脚中的表格
				java.util.List<XWPFTable> tables = footer.getTables();
				for (int tableIdx = 0; tableIdx < tables.size(); tableIdx++) {
					XWPFTable table = tables.get(tableIdx);
					processTableSDTs(table, allSdtInfo, contents, 
						String.format("页脚%d-表格%d", footerIdx + 1, tableIdx + 1));
				}
			}
		} catch (Exception e) {
			debugPrintException("处理页脚SDT时发生异常: " + e.getMessage(), e);
		}
	}
	
	/**
	 * 处理表格中的SDT控件（通用方法）
	 */
	private void processTableSDTs(XWPFTable table, List<SdtInfo> allSdtInfo, List<DocContent> contents, String location) {
		try {
			for (int rowIdx = 0; rowIdx < table.getRows().size(); rowIdx++) {
				XWPFTableRow row = table.getRows().get(rowIdx);
				for (int cellIdx = 0; cellIdx < row.getTableCells().size(); cellIdx++) {
					XWPFTableCell cell = row.getTableCells().get(cellIdx);
					for (int paraIdx = 0; paraIdx < cell.getParagraphs().size(); paraIdx++) {
						XWPFParagraph paragraph = cell.getParagraphs().get(paraIdx);
						for (int sdtIdx = 0; sdtIdx < paragraph.getCTP().getSdtList().size(); sdtIdx++) {
							CTSdtRun sdt = paragraph.getCTP().getSdtList().get(sdtIdx);
							String tag = extractTagFromSdtRun(sdt);
							if (tag != null && !tag.isEmpty()) {
								allSdtInfo.add(new SdtInfo(tag, "表格行内SDT", 
									String.format("%s-行%d-单元格%d-段落%d", location, rowIdx + 1, cellIdx + 1, paraIdx + 1)));
								debugPrint("发现表格行内SDT - " + allSdtInfo.get(allSdtInfo.size() - 1));
							}
							replaceContentInSDTRun(sdt, paragraph, contents);
						}
					}
				}
			}
		} catch (Exception e) {
			debugPrintException("处理表格SDT时发生异常: " + e.getMessage(), e);
		}
	}
	
	/**
	 * 从CTSdtRun中提取tag
	 */
	private String extractTagFromSdtRun(CTSdtRun sdt) {
		try {
			XWPFSDT xwpfsdt = new XWPFSDT(sdt, null);
			return xwpfsdt.getTag();
		} catch (Exception e) {
			return null;
		}
	}
	
	/**
	 * 从CTSdtBlock中提取tag
	 */
	private String extractTagFromSdtBlock(CTSdtBlock sdt) {
		try {
			if (sdt.getSdtPr() != null && sdt.getSdtPr().getTag() != null) {
				return sdt.getSdtPr().getTag().getVal();
			}
		} catch (Exception e) {
			debugPrintException("提取块级SDT标签时发生异常: " + e.getMessage(), e);
		}
		return null;
	}

	/**
	 * 更新行内SDT属性，确保插入内容后可编辑
	 * 
	 * 关键修复：当ContentControl原本为空时，插入内容后需要更新showingPlcHdr属性，
	 * 否则在Word中会显示为不可编辑状态。
	 */
	private void updateSdtPropertiesAfterInsert(CTSdtRun sdt) {
		try {
			if (sdt == null) {
				return;
			}
			
			// 获取或创建SDT属性
			org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSdtPr sdtPr = sdt.getSdtPr();
			if (sdtPr == null) {
				sdtPr = sdt.addNewSdtPr();
			}
			
			// 关键修复：确保showingPlcHdr属性不存在或设置为false
			// 这个属性如果为true，表示正在显示占位符，内容不可编辑
			// 删除该属性或设置为false，表示显示的是正常内容，可编辑
			if (sdtPr.isSetShowingPlcHdr()) {
				sdtPr.unsetShowingPlcHdr();
				debugPrint("已清除行内SDT的showingPlcHdr属性，内容现在可编辑");
			}
			
		} catch (Exception e) {
			debugPrintException("更新行内SDT属性时发生异常: " + e.getMessage(), e);
		}
	}
	
	/**
	 * 更新块级SDT属性，确保插入内容后可编辑
	 * 
	 * 关键修复：当ContentControl原本为空时，插入内容后需要更新showingPlcHdr属性，
	 * 否则在Word中会显示为不可编辑状态。
	 */
	private void updateSdtPropertiesAfterInsert(CTSdtBlock sdt) {
		try {
			if (sdt == null) {
				return;
			}
			
			// 获取或创建SDT属性
			org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSdtPr sdtPr = sdt.getSdtPr();
			if (sdtPr == null) {
				sdtPr = sdt.addNewSdtPr();
			}
			
			// 关键修复：确保showingPlcHdr属性不存在或设置为false
			// 这个属性如果为true，表示正在显示占位符，内容不可编辑
			// 删除该属性或设置为false，表示显示的是正常内容，可编辑
			if (sdtPr.isSetShowingPlcHdr()) {
				sdtPr.unsetShowingPlcHdr();
				debugPrint("已清除块级SDT的showingPlcHdr属性，内容现在可编辑");
			}
			
		} catch (Exception e) {
			debugPrintException("更新块级SDT属性时发生异常: " + e.getMessage(), e);
		}
	}

	/**
	 * 处理行内SDT内容替换
	 */
	private void replaceContentInSDTRun(CTSdtRun sdt, XWPFParagraph parent, List<DocContent> contents) {
		if (sdt == null) {
            return;
        }
		
		String tag = extractTagFromSdtRun(sdt);
		if (tag == null || tag.isEmpty()) {
			return;
		}
		
        for (DocContent docContent : contents) {
            if (docContent.getKey().equals(tag)) {
            	replaceContentInRun(sdt, parent, docContent.getContent());
                break;
            }
        }
	}
	
	/**
	 * 处理块级SDT内容替换
	 */
	private void replaceContentInSDTBlock(CTSdtBlock sdt, List<DocContent> contents) {
		if (sdt == null) {
            return;
        }
		
		String tag = extractTagFromSdtBlock(sdt);
		if (tag == null || tag.isEmpty()) {
			return;
		}
		
        for (DocContent docContent : contents) {
            if (docContent.getKey().equals(tag)) {
            	replaceContentInBlock(sdt, docContent.getContent());
                break;
            }
        }
	}

	/**
	 * 替换行内SDT的内容
	 */
	private void replaceContentInRun(CTSdtRun sdt, XWPFParagraph parent, String content) {
		// 第一步：使用游标找到 CTSdtContentRun 对象（只读操作，不复制）
		CTSdtContentRun targetContentRun = null;
		org.openxmlformats.schemas.wordprocessingml.x2006.main.CTRPr styleToSave = null;
		
		try (final XmlCursor cursor = sdt.newCursor()) {
            cursor.selectPath("./*");
            while (cursor.toNextSelection()) {
                XmlObject o = cursor.getObject();
                if (o instanceof CTR) {
                    XWPFRun run = new XWPFRun((CTR) o, parent);
                    debugPrint("处理CTR: " + run.toString());
                } else if (o instanceof CTSdtRun) {
                   //暂不支持嵌套sdtContent;
                	CTSdtRun run = (CTSdtRun) o;
                	debugPrint("发现嵌套SDT: " + run);
                } else if (o instanceof CTSdtContentRun) {
                	targetContentRun = (CTSdtContentRun) o;
                	
                	// ===== 只记录第一个运行对象的样式引用，不进行复制操作 =====
                	CTR[] ctrs = targetContentRun.getRArray();
                	if (ctrs != null && ctrs.length > 0) {
                		// 获取第一个运行对象的样式引用（不复制）
                		CTR firstRun = ctrs[0];
                		if (firstRun.getRPr() != null) {
                			// 只保存引用，不调用 copy()
                			styleToSave = firstRun.getRPr();
                			debugPrint("已记录原有样式引用");
                		}
                	}
                	
                	// 找到目标对象后退出循环，避免继续遍历
                	break;
                }
            }
        } // 游标在这里关闭
        
        // 第二步：游标关闭后，复制样式（复制操作也会修改文档）
        org.openxmlformats.schemas.wordprocessingml.x2006.main.CTRPr originalStyle = null;
        boolean isShowingPlaceholder = false; // 是否正在显示占位符
        
        // 检查SDT是否正在显示占位符
        try {
        	if (sdt.getSdtPr() != null && sdt.getSdtPr().isSetShowingPlcHdr()) {
        		// showingPlcHdr存在即表示正在显示占位符
        		isShowingPlaceholder = true;
        		debugPrint("检测到showingPlcHdr属性，正在显示占位符");
        	}
        } catch (Exception e) {
        	debugPrintException("检查showingPlcHdr属性时发生异常: " + e.getMessage(), e);
        }
        
        if (styleToSave != null) {
        	try {
        		originalStyle = (org.openxmlformats.schemas.wordprocessingml.x2006.main.CTRPr) styleToSave.copy();
        		debugPrint("已复制原有样式: " + originalStyle);
        		
        		// ===== 严谨的占位符颜色判断 =====
        		// 只有当showingPlcHdr=true且颜色是占位符灰色时，才修正颜色
        		if (isShowingPlaceholder) {
        			String originalColor = getStyleColorValue(originalStyle);
        			if (isPlaceholderGrayColor(originalColor)) {
        				debugPrint("[占位符颜色修正] showingPlcHdr=true 且颜色为占位符灰色(" + originalColor + ")，修正为黑色");
        				setStyleColorValue(originalStyle, "000000");
        			}
        		}
        		
        	} catch (Exception e) {
        		debugPrintException("复制样式时发生异常，将跳过样式保留: " + e.getMessage(), e);
        	}
        }
        
        // 第三步：游标关闭并完成样式复制后，才进行文档修改操作
        if (targetContentRun != null) {
        	CTR[] ctrs = targetContentRun.getRArray();
        	
        	// ===== 彻底清空现有内容 =====
        	if (ctrs != null && ctrs.length > 0) {
        		// 1. 先清空所有运行对象中的文本
        		for(CTR r : ctrs) {
        			if (r.getTList() != null) {
        				r.getTList().clear();
        			}
        			// 清空其他可能的内容元素
        			if (r.getTabList() != null) {
        				r.getTabList().clear();
        			}
        			if (r.getBrList() != null) {
        				r.getBrList().clear();
        			}
        			if (r.getCrList() != null) {
        				r.getCrList().clear();
        			}
        		}
        		
        		// 2. 删除所有现有的运行对象
        		for (int i = ctrs.length - 1; i >= 0; i--) {
        			targetContentRun.removeR(i);
        		}
        		
        		debugPrint("已清空ContentControl原有内容，运行对象数量: " + ctrs.length);
        	}
        	
        	// ===== 插入新内容并应用原有样式 =====
        	// 检查内容是否包含HTML标签
        	if (isHtmlContent(content)) {
        		// 使用HTML处理工具处理HTML内容
        		HtmlUtils.insertHtmlToContentControl(targetContentRun, parent, content);
        		debugPrint("已处理HTML内容 (行内SDT): " + content);
        		
        		// HTML内容也应用原有样式
        		if (originalStyle != null) {
        			applyStyleToContentRun(targetContentRun, originalStyle);
        		}
        	} else {
        		// 纯文本处理，应用原有样式
        		insertPlainTextToRun(targetContentRun, content, originalStyle);
        		debugPrint("已处理纯文本内容 (行内SDT): " + content + ", 已应用原有样式");
        	}
        	
        	// ===== 关键修复：更新ContentControl属性，确保内容可编辑 =====
        	updateSdtPropertiesAfterInsert(sdt);
        }
	}
	
	/**
	 * 替换块级SDT的内容
	 */
	private void replaceContentInBlock(CTSdtBlock sdt, String content) {
		try {
			CTSdtContentBlock sdtContent = sdt.getSdtContent();
			if (sdtContent != null) {
				// ===== 保存原有样式引用（不复制）=====
				org.openxmlformats.schemas.wordprocessingml.x2006.main.CTRPr styleToSave = null;
				if (sdtContent.getPList() != null && sdtContent.getPList().size() > 0) {
					// 获取第一个段落的第一个运行对象的样式引用
					CTP firstParagraph = sdtContent.getPList().get(0);
					if (firstParagraph.getRList() != null && firstParagraph.getRList().size() > 0) {
						CTR firstRun = firstParagraph.getRList().get(0);
						if (firstRun.getRPr() != null) {
							styleToSave = firstRun.getRPr();
							debugPrint("已记录块级SDT原有样式引用");
						}
					}
				}
				
				// 复制样式（在清空操作之前，但不在游标内）
				org.openxmlformats.schemas.wordprocessingml.x2006.main.CTRPr originalStyle = null;
				boolean isShowingPlaceholder = false; // 是否正在显示占位符
				
				// 检查块级SDT是否正在显示占位符
				try {
					if (sdt.getSdtPr() != null && sdt.getSdtPr().isSetShowingPlcHdr()) {
						// showingPlcHdr存在即表示正在显示占位符
						isShowingPlaceholder = true;
						debugPrint("检测到块级SDT的showingPlcHdr属性，正在显示占位符");
					}
				} catch (Exception e) {
					debugPrintException("检查块级SDT的showingPlcHdr属性时发生异常: " + e.getMessage(), e);
				}
				
				if (styleToSave != null) {
					try {
						originalStyle = (org.openxmlformats.schemas.wordprocessingml.x2006.main.CTRPr) styleToSave.copy();
						debugPrint("已复制块级SDT原有样式: " + originalStyle);
						
						// ===== 严谨的占位符颜色判断 =====
						// 只有当showingPlcHdr=true且颜色是占位符灰色时，才修正颜色
						if (isShowingPlaceholder) {
							String originalColor = getStyleColorValue(originalStyle);
							if (isPlaceholderGrayColor(originalColor)) {
								debugPrint("[占位符颜色修正] 块级SDT showingPlcHdr=true 且颜色为占位符灰色(" + originalColor + ")，修正为黑色");
								setStyleColorValue(originalStyle, "000000");
							}
						}
						
					} catch (Exception e) {
						debugPrintException("复制块级SDT样式时发生异常，将跳过样式保留: " + e.getMessage(), e);
					}
				}
				
				// ===== 彻底清空现有内容 =====
				debugPrint("清空现有内容 - 段落数: " + sdtContent.getPList().size() + ", 表格数: " + sdtContent.getTblList().size());
				
				// 清空所有段落
				for (int i = sdtContent.getPList().size() - 1; i >= 0; i--) {
					sdtContent.removeP(i);
				}
				
				// 清空所有表格
				for (int i = sdtContent.getTblList().size() - 1; i >= 0; i--) {
					sdtContent.removeTbl(i);
				}
				
				debugPrint("内容清空完成");
				
				// ===== 插入新内容并应用原有样式 =====
				// 检查内容是否包含HTML标签
				if (isHtmlContent(content)) {
					// 处理HTML内容到块级SDT
					insertHtmlToBlock(sdtContent, content);
					debugPrint("已处理HTML内容 (块级SDT): " + content);
					
					// HTML内容也应用原有样式
					if (originalStyle != null) {
						applyStyleToBlock(sdtContent, originalStyle);
					}
				} else {
					// 纯文本处理，应用原有样式
					insertPlainTextToBlock(sdtContent, content, originalStyle);
					debugPrint("已处理纯文本内容 (块级SDT): " + content + ", 已应用原有样式");
				}
				
				// ===== 关键修复：更新ContentControl属性，确保内容可编辑 =====
				updateSdtPropertiesAfterInsert(sdt);
			}
		} catch (Exception e) {
			debugPrintException("替换块级SDT内容时发生异常: " + e.getMessage(), e);
		}
	}
	
	/**
	 * 将HTML内容插入到块级SDT
	 */
	private void insertHtmlToBlock(CTSdtContentBlock sdtContent, String content) {
		try {
			// 检查是否包含表格
			if (content.toLowerCase().contains("<table")) {
				// 处理HTML表格
				insertHtmlTableToBlock(sdtContent, content);
				return;
			}
			
			// 创建新段落
			CTP paragraph = sdtContent.addNewP();
			
			// 使用简单的HTML处理
			if (content.contains("<p>")) {
				// 如果包含段落标签，分段处理
				String[] paragraphs = content.split("(?i)</?p[^>]*>");
				for (String para : paragraphs) {
					if (para.trim().isEmpty()) continue;
					
					if (sdtContent.getPList().size() > 0) {
						paragraph = sdtContent.addNewP();
					}
					
					// 创建运行对象并处理HTML
					CTR run = paragraph.addNewR();
					CTText text = run.addNewT();
					text.setStringValue(stripHtmlTags(para));
					
					// 应用简单的HTML格式
					applyHtmlFormattingToRun(run, para);
				}
			} else {
				// 单段落处理
				CTR run = paragraph.addNewR();
				CTText text = run.addNewT();
				text.setStringValue(stripHtmlTags(content));
				applyHtmlFormattingToRun(run, content);
			}
		} catch (Exception e) {
			debugPrintException("插入HTML到块级SDT时发生异常: " + e.getMessage(), e);
		}
	}
	
	/**
	 * 将HTML表格插入到块级SDT
	 */
	private void insertHtmlTableToBlock(CTSdtContentBlock sdtContent, String content) {
		try {
			debugPrint("开始处理HTML表格内容...");
			
			// 分离表格前的内容、表格内容和表格后的内容
			String[] parts = splitContentByTable(content);
			String beforeTable = parts[0];
			String tableContent = parts[1];
			String afterTable = parts[2];
			
			// 插入表格前的内容
			if (!beforeTable.trim().isEmpty()) {
				insertNonTableContent(sdtContent, beforeTable);
			}
			
			// 解析并插入表格
			if (!tableContent.trim().isEmpty()) {
				parseAndInsertTable(sdtContent, tableContent);
			}
			
			// 插入表格后的内容
			if (!afterTable.trim().isEmpty()) {
				insertNonTableContent(sdtContent, afterTable);
			}
			
			debugPrint("HTML表格处理完成");
			
		} catch (Exception e) {
			debugPrintException("插入HTML表格时发生异常: " + e.getMessage(), e);
			// 降级处理：作为普通文本插入
			insertPlainTextToBlock(sdtContent, stripHtmlTags(content));
		}
	}
	
	/**
	 * 按表格分割内容
	 */
	private String[] splitContentByTable(String content) {
		String beforeTable = "";
		String tableContent = "";
		String afterTable = "";
		
		// 首先检查是否包含表格标签
		if (!content.toLowerCase().contains("<table")) {
			// 没有表格标签，将所有内容作为非表格内容
			return new String[]{content, "", ""};
		}
		
		// 查找表格开始位置
		int tableStart = content.toLowerCase().indexOf("<table");
		if (tableStart == -1) {
			return new String[]{content, "", ""};
		}
		
		// 提取表格前的内容
		beforeTable = content.substring(0, tableStart);
		
		// 查找表格结束位置
		int tableEnd = findTableEndPosition(content, tableStart);
		if (tableEnd == -1) {
			// 没有找到表格结束标签，尝试自动补全
			debugPrint("警告：未找到表格结束标签，尝试自动补全");
			tableContent = content.substring(tableStart) + "</table>";
			afterTable = "";
		} else {
			// 提取表格内容
			tableContent = content.substring(tableStart, tableEnd + 8); // +8 for "</table>"
			// 提取表格后的内容
			afterTable = content.substring(tableEnd + 8);
		}
		
		debugPrint("表格分割结果：");
		debugPrint("  表格前内容长度: " + beforeTable.length());
		debugPrint("  表格内容长度: " + tableContent.length());
		debugPrint("  表格后内容长度: " + afterTable.length());
		
		return new String[]{beforeTable, tableContent, afterTable};
	}
	
	/**
	 * 查找表格结束位置，处理嵌套表格的情况
	 */
	private int findTableEndPosition(String content, int startPos) {
		int level = 0;
		int pos = startPos;
		String lowerContent = content.toLowerCase();
		
		while (pos < content.length()) {
			int tableStart = lowerContent.indexOf("<table", pos);
			int tableEnd = lowerContent.indexOf("</table>", pos);
			
			if (tableStart != -1 && (tableEnd == -1 || tableStart < tableEnd)) {
				// 找到嵌套的表格开始标签
				level++;
				pos = tableStart + 6;
			} else if (tableEnd != -1) {
				// 找到表格结束标签
				level--;
				if (level == 0) {
					// 找到最外层表格的结束位置
					return tableEnd;
				}
				pos = tableEnd + 8;
			} else {
				// 没有找到更多标签
				break;
			}
		}
		
		return -1; // 没有找到匹配的结束标签
	}
	
	/**
	 * 插入非表格内容
	 */
	private void insertNonTableContent(CTSdtContentBlock sdtContent, String content) {
		if (content == null || content.trim().isEmpty()) {
			return;
		}
		
		debugPrint("处理非表格内容: " + content);
		
		// 处理段落 - 改进的段落解析逻辑
		if (content.contains("<p>")) {
			// 使用正则表达式匹配完整的<p>标签内容 - 改进版本，支持更复杂的属性
			java.util.regex.Pattern pPattern = java.util.regex.Pattern.compile(
				"<p\\s*[^>]*>(.*?)</p\\s*>", 
				java.util.regex.Pattern.CASE_INSENSITIVE | java.util.regex.Pattern.DOTALL
			);
			java.util.regex.Matcher pMatcher = pPattern.matcher(content);
			
			boolean foundParagraphs = false;
			while (pMatcher.find()) {
				String paraContent = pMatcher.group(1).trim();
				if (!paraContent.isEmpty()) {
					debugPrint("创建段落，内容: " + paraContent);
					CTP paragraph = sdtContent.addNewP();
					CTR run = paragraph.addNewR();
					CTText text = run.addNewT();
					text.setStringValue(stripHtmlTags(paraContent));
					applyHtmlFormattingToRun(run, paraContent);
					foundParagraphs = true;
				}
			}
			
			// 如果没有找到完整的<p>标签，但包含<p>，可能是不完整的HTML，降级处理
			if (!foundParagraphs) {
				debugPrint("未找到完整的<p>标签，使用简单分割方法");
				// 改进的分割正则表达式，支持自闭合标签和复杂属性
				String[] paragraphs = content.split("(?i)</?p\\s*[^>]*>");
				for (String para : paragraphs) {
					if (para.trim().isEmpty()) continue;
					
					CTP paragraph = sdtContent.addNewP();
					CTR run = paragraph.addNewR();
					CTText text = run.addNewT();
					text.setStringValue(stripHtmlTags(para));
					applyHtmlFormattingToRun(run, para);
				}
			}
		} else {
			// 按换行分割处理非段落内容
			String[] lines = content.split("\\r?\\n");
			for (String line : lines) {
				if (line.trim().isEmpty()) continue;
				
				debugPrint("创建行，内容: " + line);
				CTP paragraph = sdtContent.addNewP();
				CTR run = paragraph.addNewR();
				CTText text = run.addNewT();
				text.setStringValue(stripHtmlTags(line));
				applyHtmlFormattingToRun(run, line);
			}
		}
		
		debugPrint("非表格内容处理完成");
	}
	
	/**
	 * 解析并插入表格
	 */
	private void parseAndInsertTable(CTSdtContentBlock sdtContent, String tableHtml) {
		try {
			debugPrint("解析表格HTML: " + tableHtml);
			
			// 创建Word表格
			org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTbl table = sdtContent.addNewTbl();
			
			// 设置表格属性
			org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblPr tblPr = table.addNewTblPr();
			
			// 解析<table>起始标签内联样式，提取表级颜色/边框颜色
			String tableOpenTag = null;
			try {
				java.util.regex.Matcher openTagMatcher = java.util.regex.Pattern.compile("<table\\s*[^>]*>", java.util.regex.Pattern.CASE_INSENSITIVE).matcher(tableHtml);
				if (openTagMatcher.find()) {
					tableOpenTag = openTagMatcher.group(0);
				}
			} catch (Exception ignore) {}
			String tableTextHex = null; // 文字颜色（来自<table> style color）
			Integer tableFontSizeHalfPts = null; // 表级默认字体大小（半磅）
			String tableBorderHex = null; // 边框颜色（来自<table> style border/border-color）
			if (tableOpenTag != null) {
				try {
					java.util.regex.Matcher colorM = java.util.regex.Pattern.compile("(?i)style\\s*=\\s*['\"][^'\"]*?color\\s*:\\s*([^;\"']+)").matcher(tableOpenTag);
					if (colorM.find()) {
						tableTextHex = convertColorToHex(colorM.group(1).trim());
					}
				} catch (Exception ignore) {}
				// 提取表级字体大小
				try {
					java.util.regex.Matcher fontM = java.util.regex.Pattern.compile("(?i)font-size\\s*:\\s*(\\d+(?:\\.\\d+)?)\\s*(px|pt|em|rem)?").matcher(tableOpenTag);
					if (fontM.find()) {
						double fontSize = Double.parseDouble(fontM.group(1));
						String unit = fontM.group(2);
						int pt;
						if (unit == null || "px".equalsIgnoreCase(unit)) {
							pt = (int) Math.round(fontSize * 0.75);
						} else if ("pt".equalsIgnoreCase(unit)) {
							pt = (int) Math.round(fontSize);
						} else if ("em".equalsIgnoreCase(unit) || "rem".equalsIgnoreCase(unit)) {
							pt = (int) Math.round(fontSize * 12);
						} else {
							pt = (int) Math.round(fontSize);
						}
						tableFontSizeHalfPts = Math.max(2, Math.min(3276, pt * 2));
					}
				} catch (Exception ignore) {}
				try {
					// 优先border-color，其次border中携带的颜色值
					java.util.regex.Matcher borderColorM = java.util.regex.Pattern.compile("(?i)border-color\\s*:\\s*([^;\"']+)").matcher(tableOpenTag);
					if (borderColorM.find()) {
						tableBorderHex = convertColorToHex(borderColorM.group(1).trim());
					} else {
						java.util.regex.Matcher borderM = java.util.regex.Pattern.compile("(?i)border\\s*:\\s*[^;]*?(#[0-9a-fA-F]{3,6}|rgb\\([^)]*\\))").matcher(tableOpenTag);
						if (borderM.find()) {
							tableBorderHex = convertColorToHex(borderM.group(1).trim());
						}
					} 
				} catch (Exception ignore) {}
			}
			
			// 设置表格对齐方式 - 改进版本，支持更复杂的样式属性
			java.util.regex.Pattern alignPattern = java.util.regex.Pattern.compile(
				"<table\\s*[^>]*?(?:align\\s*=\\s*[\"']([^\"']*)[\"']|style\\s*=\\s*[\"'][^\"']*?text-align\\s*:\\s*([^;\"']*)[^\"']*[\"'])[^>]*>",
				java.util.regex.Pattern.CASE_INSENSITIVE
			);
			java.util.regex.Matcher alignMatcher = alignPattern.matcher(tableHtml);
			String tableAlignStr = "left";
			if (alignMatcher.find()) {
				String alignValue = alignMatcher.group(1) != null ? alignMatcher.group(1) : alignMatcher.group(2);
				debugPrint("检测到表格对齐方式: " + alignValue);
				
				org.openxmlformats.schemas.wordprocessingml.x2006.main.CTJcTable tableAlign = tblPr.addNewJc();
				if ("center".equalsIgnoreCase(alignValue)) {
					tableAlign.setVal(org.openxmlformats.schemas.wordprocessingml.x2006.main.STJcTable.CENTER);
					tableAlignStr = "center";
				} else if ("right".equalsIgnoreCase(alignValue)) {
					tableAlign.setVal(org.openxmlformats.schemas.wordprocessingml.x2006.main.STJcTable.END);
					tableAlignStr = "right";
				} else {
					tableAlign.setVal(org.openxmlformats.schemas.wordprocessingml.x2006.main.STJcTable.START); // 默认左对齐
					tableAlignStr = "left";
				}
			} else {
				// 未显式声明时默认左对齐，避免越界
				org.openxmlformats.schemas.wordprocessingml.x2006.main.CTJcTable tableAlign = tblPr.addNewJc();
				tableAlign.setVal(org.openxmlformats.schemas.wordprocessingml.x2006.main.STJcTable.START);
				tableAlignStr = "left";
			}
			
			// 设置表格宽度（使用PCT百分比单位，防止超出页面宽度）
			org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblWidth tblWidth = tblPr.addNewTblW();
			tblWidth.setType(org.openxmlformats.schemas.wordprocessingml.x2006.main.STTblWidth.PCT);
			
			// 从HTML的style属性中提取宽度 - 改进版本，支持多种单位和复杂样式
			java.util.regex.Pattern widthPattern = java.util.regex.Pattern.compile(
				"<table\\s*[^>]*?style\\s*=\\s*[\"'][^\"']*?width\\s*:\\s*(\\d+(?:\\.\\d+)?)\\s*(%|px|em|rem|pt|cm|mm|in)?[^\"']*[\"'][^>]*>",
				java.util.regex.Pattern.CASE_INSENSITIVE
			);
			java.util.regex.Matcher widthMatcher = widthPattern.matcher(tableHtml);
			if (widthMatcher.find()) {
				double widthValue = Double.parseDouble(widthMatcher.group(1));
				String unit = widthMatcher.group(2);
				
				// 根据单位类型转换宽度
				int widthPercent;
				if (unit == null || "%".equals(unit)) {
					widthPercent = (int) widthValue;
				} else if ("px".equals(unit)) {
					// 将像素宽度近似映射到百分比，限制在[1,100]
					widthPercent = (int) Math.round(Math.min(100, Math.max(1, (widthValue / 1000.0) * 100)));
				} else {
					// 其他单位暂时按百分比处理
					widthPercent = (int) widthValue;
				}
				
				// 确保百分比在合理范围内
				widthPercent = Math.max(1, Math.min(100, widthPercent));
				// PCT单位：值 = 百分比 * 50
				tblWidth.setW(java.math.BigInteger.valueOf(widthPercent * 50));
				debugPrint("设置表格宽度(百分比): " + widthPercent + "% (原始值: " + widthValue + unit + ")");
			} else {
				// 默认宽度100%（PCT -> 5000）
				tblWidth.setW(java.math.BigInteger.valueOf(5000));
				debugPrint("使用默认表格宽度: 100%");
			}

			// 设置表格缩进为0，使其与正文左边距对齐
			try {
				org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblWidth tblInd = tblPr.addNewTblInd();
				tblInd.setType(org.openxmlformats.schemas.wordprocessingml.x2006.main.STTblWidth.DXA);
				tblInd.setW(java.math.BigInteger.valueOf(0));
			} catch (Exception e) {
				debugPrintException("设置表格缩进失败", e);
			}
			
			// 设置表格边框
			org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblBorders tblBorders = tblPr.addNewTblBorders();
			
			// 检查是否有border样式 - 改进版本，支持更复杂的边框样式写法
			java.util.regex.Pattern borderPattern1 = java.util.regex.Pattern.compile(
				"<table\\s*[^>]*?style\\s*=\\s*[\"'][^\"']*?(?:border|border-style|border-width|border-color)[^\"']*[\"'][^>]*>",
				java.util.regex.Pattern.CASE_INSENSITIVE
			);
			java.util.regex.Pattern borderPattern2 = java.util.regex.Pattern.compile(
				"<table\\s*[^>]*?(?:border\\s*=\\s*[\"'][^\"']*[\"']|border\\s*=\\s*[^\\s>]+)[^>]*>",
				java.util.regex.Pattern.CASE_INSENSITIVE
			);
			boolean hasBorder = borderPattern1.matcher(tableHtml).find() || borderPattern2.matcher(tableHtml).find();
			debugPrint("=== 边框检测调试信息 ===");
			debugPrint("边框检测结果: " + hasBorder);
			debugPrint("表格HTML内容: " + tableHtml);
			debugPrint("边框Pattern1匹配: " + borderPattern1.matcher(tableHtml).find());
			debugPrint("边框Pattern2匹配: " + borderPattern2.matcher(tableHtml).find());
			
			// 设置表格边框 - 改进的边框设置方法
			if (hasBorder) {
				debugPrint("检测到边框样式，设置表格边框...");
				try {
					String borderHex = tableBorderHex != null ? tableBorderHex : "000000";
					// 设置表格边框 - 使用addNew方法创建独立的边框对象
					org.openxmlformats.schemas.wordprocessingml.x2006.main.CTBorder topBorder = tblBorders.addNewTop();
					topBorder.setVal(org.openxmlformats.schemas.wordprocessingml.x2006.main.STBorder.SINGLE);
					topBorder.setSz(java.math.BigInteger.valueOf(4));
					topBorder.setColor(borderHex);
					
					org.openxmlformats.schemas.wordprocessingml.x2006.main.CTBorder bottomBorder = tblBorders.addNewBottom();
					bottomBorder.setVal(org.openxmlformats.schemas.wordprocessingml.x2006.main.STBorder.SINGLE);
					bottomBorder.setSz(java.math.BigInteger.valueOf(4));
					bottomBorder.setColor(borderHex);
					
					org.openxmlformats.schemas.wordprocessingml.x2006.main.CTBorder leftBorder = tblBorders.addNewLeft();
					leftBorder.setVal(org.openxmlformats.schemas.wordprocessingml.x2006.main.STBorder.SINGLE);
					leftBorder.setSz(java.math.BigInteger.valueOf(4));
					leftBorder.setColor(borderHex);
					
					org.openxmlformats.schemas.wordprocessingml.x2006.main.CTBorder rightBorder = tblBorders.addNewRight();
					rightBorder.setVal(org.openxmlformats.schemas.wordprocessingml.x2006.main.STBorder.SINGLE);
					rightBorder.setSz(java.math.BigInteger.valueOf(4));
					rightBorder.setColor(borderHex);
					
					org.openxmlformats.schemas.wordprocessingml.x2006.main.CTBorder insideHBorder = tblBorders.addNewInsideH();
					insideHBorder.setVal(org.openxmlformats.schemas.wordprocessingml.x2006.main.STBorder.SINGLE);
					insideHBorder.setSz(java.math.BigInteger.valueOf(4));
					insideHBorder.setColor(borderHex);
					
					org.openxmlformats.schemas.wordprocessingml.x2006.main.CTBorder insideVBorder = tblBorders.addNewInsideV();
					insideVBorder.setVal(org.openxmlformats.schemas.wordprocessingml.x2006.main.STBorder.SINGLE);
					insideVBorder.setSz(java.math.BigInteger.valueOf(4));
					insideVBorder.setColor(borderHex);
					
					debugPrint("表格边框设置完成");
				} catch (Exception borderEx) {
					debugPrintException("设置表格边框时发生异常: " + borderEx.getMessage(), borderEx);
				}
			} else {
				debugPrint("未检测到边框样式，跳过边框设置");
			}
			
			// 设置表格网格
			org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblGrid tblGrid = table.addNewTblGrid();
			
			// 解析表格行 - 改进版本，支持更复杂的属性
			java.util.regex.Pattern rowPattern = java.util.regex.Pattern.compile(
				"<tr\\s*[^>]*>(.*?)</tr\\s*>", 
				java.util.regex.Pattern.CASE_INSENSITIVE | java.util.regex.Pattern.DOTALL
			);
			
			java.util.regex.Matcher rowMatcher = rowPattern.matcher(tableHtml);
			int rowCount = 0;
			
			// 先计算列数
			int maxColumns = 0;
			java.util.regex.Pattern cellPattern = java.util.regex.Pattern.compile(
				"<(td|th)\\s*[^>]*>(.*?)</(td|th)\\s*>",
				java.util.regex.Pattern.CASE_INSENSITIVE | java.util.regex.Pattern.DOTALL
			);
			
			String tableContent = tableHtml.substring(
				tableHtml.indexOf("<table"),
				tableHtml.indexOf("</table>") + 8
			);
			
			java.util.regex.Matcher tempMatcher = rowPattern.matcher(tableContent);
			while (tempMatcher.find()) {
				java.util.regex.Matcher cellMatcher = cellPattern.matcher(tempMatcher.group(1));
				int columnCount = 0;
				while (cellMatcher.find()) {
					columnCount++;
				}
				maxColumns = Math.max(maxColumns, columnCount);
			}
			
			// 设置列宽
			int defaultColumnWidth = 9000 / maxColumns;
			for (int i = 0; i < maxColumns; i++) {
				org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblGridCol gridCol = tblGrid.addNewGridCol();
				gridCol.setW(java.math.BigInteger.valueOf(defaultColumnWidth));
			}
			
			// 提取 <thead> 和 <tbody> 的颜色设置
			String theadTextHex = null;
			String tbodyTextHex = null;
			try {
				java.util.regex.Matcher theadM = java.util.regex.Pattern.compile(
					"(?i)<thead[^>]*?style\\s*=\\s*['\"][^'\"]*?color\\s*:\\s*([^;\"']+)"
				).matcher(tableContent);
				if (theadM.find()) {
					theadTextHex = convertColorToHex(theadM.group(1).trim());
					debugPrint("检测到 <thead> 文字颜色: " + theadTextHex);
				}
				
				java.util.regex.Matcher tbodyM = java.util.regex.Pattern.compile(
					"(?i)<tbody[^>]*?style\\s*=\\s*['\"][^'\"]*?color\\s*:\\s*([^;\"']+)"
				).matcher(tableContent);
				if (tbodyM.find()) {
					tbodyTextHex = convertColorToHex(tbodyM.group(1).trim());
					debugPrint("检测到 <tbody> 文字颜色: " + tbodyTextHex);
				}
			} catch (Exception ignore) {}
			
			// 标记当前是否在 thead 区域
			boolean inThead = tableContent.toLowerCase().contains("<thead");
			int theadEndPos = -1;
			if (inThead) {
				int theadStartPos = tableContent.toLowerCase().indexOf("<thead");
				theadEndPos = tableContent.toLowerCase().indexOf("</thead>", theadStartPos);
			}
			
			while (rowMatcher.find()) {
				String rowContent = rowMatcher.group(1);
				String fullRowTag = rowMatcher.group(0);
				rowCount++;
				
				// 判断当前行是否在 <thead> 区域内
				int currentRowPos = rowMatcher.start();
				boolean isTheadRow = (theadEndPos > 0 && currentRowPos < theadEndPos);
				
				debugPrint("处理第" + rowCount + "行 (isTheadRow=" + isTheadRow + "): " + rowContent);
				
				// 尝试获取该行的背景色（常用于表头）
				String rowBgHex = null;
				try {
					java.util.regex.Matcher rowBgM = java.util.regex.Pattern.compile("(?i)style\\s*=\\s*['\"][^'\"]*?background(?:-color)?\\s*:\\s*([^;\"']+)").matcher(fullRowTag);
					if (rowBgM.find()) {
						rowBgHex = convertColorToHex(rowBgM.group(1).trim());
					}
				} catch (Exception ignore) {}
				
				// 尝试获取该行的文字颜色
				String rowTextHex = null;
				try {
					java.util.regex.Matcher rowTextM = java.util.regex.Pattern.compile("(?i)style\\s*=\\s*['\"][^'\"]*?color\\s*:\\s*([^;\"']+)").matcher(fullRowTag);
					if (rowTextM.find()) {
						rowTextHex = convertColorToHex(rowTextM.group(1).trim());
						debugPrint("检测到行文字颜色: " + rowTextHex);
					}
				} catch (Exception ignore) {}
				
				// 确定该行使用的文字颜色优先级：行级 > thead/tbody > 表级
				String effectiveTextHex = rowTextHex;
				if (effectiveTextHex == null) {
					effectiveTextHex = isTheadRow ? theadTextHex : tbodyTextHex;
				}
				if (effectiveTextHex == null) {
					effectiveTextHex = tableTextHex;
				}
				
				// 创建表格行
				org.openxmlformats.schemas.wordprocessingml.x2006.main.CTRow row = table.addNewTr();
				
				// 解析单元格
				java.util.regex.Matcher cellMatcher = cellPattern.matcher(rowContent);
				int cellCount = 0;
				
				while (cellMatcher.find()) {
					String cellContent = cellMatcher.group(2);
					boolean isHeader = "th".equalsIgnoreCase(cellMatcher.group(1));
					cellCount++;
					
					// 单元格完整标签字符串（用于样式解析：边框/对齐/颜色等）
					String fullCellTag = cellMatcher.group(0);
					debugPrint("  处理第" + cellCount + "列 (header=" + isHeader + "): " + cellContent);
					
					// 创建单元格
					org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTc cell = row.addNewTc();
					
					// 设置单元格属性
					org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTcPr tcPr = cell.addNewTcPr();
					
					// 检查并处理 colspan 属性
					java.util.regex.Pattern colspanPattern = java.util.regex.Pattern.compile(
						"<(td|th)\\s*[^>]*?colspan\\s*=\\s*[\"']?(\\d+)[\"']?[^>]*>",
						java.util.regex.Pattern.CASE_INSENSITIVE
					);
					java.util.regex.Matcher colspanMatcher = colspanPattern.matcher(fullCellTag);
					if (colspanMatcher.find()) {
						int colspan = Integer.parseInt(colspanMatcher.group(2));
						if (colspan > 1) {
							org.openxmlformats.schemas.wordprocessingml.x2006.main.CTDecimalNumber gridSpan = tcPr.addNewGridSpan();
							gridSpan.setVal(java.math.BigInteger.valueOf(colspan));
							debugPrint("  设置单元格合并: colspan=" + colspan);
						}
					}
					
					// 设置单元格宽度
					org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblWidth cellWidth = tcPr.addNewTcW();
					cellWidth.setType(org.openxmlformats.schemas.wordprocessingml.x2006.main.STTblWidth.DXA);
					
					// 检查单元格是否有宽度设置 - 改进版本，支持多种单位
					java.util.regex.Pattern cellWidthPattern = java.util.regex.Pattern.compile(
						"<(td|th)\\s*[^>]*?style\\s*=\\s*[\"'][^\"']*?width\\s*:\\s*(\\d+(?:\\.\\d+)?)\\s*(%|px|em|rem|pt|cm|mm|in)?[^\"']*[\"'][^>]*>",
						java.util.regex.Pattern.CASE_INSENSITIVE
					);
					java.util.regex.Matcher cellWidthMatcher = cellWidthPattern.matcher(fullCellTag);
					if (cellWidthMatcher.find()) {
						double widthValue = Double.parseDouble(cellWidthMatcher.group(2));
						String unit = cellWidthMatcher.group(3);
						
						// 根据单位类型转换宽度
						int widthPercent;
						if (unit == null || "%".equals(unit)) {
							widthPercent = (int) widthValue;
						} else if ("px".equals(unit)) {
							// 假设96px = 1英寸，转换为百分比
							widthPercent = (int) (widthValue / (96 * 8.5) * 100);
						} else {
							// 其他单位暂时按百分比处理
							widthPercent = (int) widthValue;
						}
						
						// 确保百分比在合理范围内
						widthPercent = Math.max(1, Math.min(100, widthPercent));
						
						// 将百分比转换为twips（1% = 100 twips）
						cellWidth.setW(java.math.BigInteger.valueOf(widthPercent * 100));
						debugPrint("设置单元格宽度: " + widthPercent + "% (原始值: " + widthValue + unit + ")");
					} else {
						cellWidth.setW(java.math.BigInteger.valueOf(defaultColumnWidth));
						debugPrint("使用默认单元格宽度");
					}
					
					// 设置单元格边框（无论表级是否声明border，均按单元格/表级颜色设置）
					org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTcBorders tcBorders = tcPr.addNewTcBorders();
					// 解析单元格自身的border颜色或border-color
					String cellBorderHex = null;
					try {
						java.util.regex.Matcher cellBorderColorM = java.util.regex.Pattern.compile(
							"(?i)<(?:td|th)\\s*[^>]*?style\\s*=\\s*['\"][^'\"]*?border-color\\s*:\\s*([^;\"']+)"
						).matcher(fullCellTag);
						if (cellBorderColorM.find()) {
							cellBorderHex = convertColorToHex(cellBorderColorM.group(1).trim());
						} else {
							java.util.regex.Matcher cellBorderM = java.util.regex.Pattern.compile(
								"(?i)<(?:td|th)\\s*[^>]*?style\\s*=\\s*['\"][^'\"]*?border\\s*:\\s*[^;]*?(#[0-9a-fA-F]{3,6}|rgb\\([^)]*\\))"
							).matcher(fullCellTag);
							if (cellBorderM.find()) {
								cellBorderHex = convertColorToHex(cellBorderM.group(1).trim());
							}
						}
					} catch (Exception ignore) {}
					String effectiveBorderHex = cellBorderHex != null ? cellBorderHex : (tableBorderHex != null ? tableBorderHex : "000000");
					
					// 为每个边框创建独立的对象
					org.openxmlformats.schemas.wordprocessingml.x2006.main.CTBorder cellTopBorder = 
						org.openxmlformats.schemas.wordprocessingml.x2006.main.CTBorder.Factory.newInstance();
					cellTopBorder.setVal(org.openxmlformats.schemas.wordprocessingml.x2006.main.STBorder.SINGLE);
					cellTopBorder.setSz(java.math.BigInteger.valueOf(6));
					cellTopBorder.setSpace(java.math.BigInteger.valueOf(0));
					cellTopBorder.setColor(effectiveBorderHex);
					
					org.openxmlformats.schemas.wordprocessingml.x2006.main.CTBorder cellBottomBorder = 
						org.openxmlformats.schemas.wordprocessingml.x2006.main.CTBorder.Factory.newInstance();
					cellBottomBorder.setVal(org.openxmlformats.schemas.wordprocessingml.x2006.main.STBorder.SINGLE);
					cellBottomBorder.setSz(java.math.BigInteger.valueOf(6));
					cellBottomBorder.setSpace(java.math.BigInteger.valueOf(0));
					cellBottomBorder.setColor(effectiveBorderHex);
					
					org.openxmlformats.schemas.wordprocessingml.x2006.main.CTBorder cellLeftBorder = 
						org.openxmlformats.schemas.wordprocessingml.x2006.main.CTBorder.Factory.newInstance();
					cellLeftBorder.setVal(org.openxmlformats.schemas.wordprocessingml.x2006.main.STBorder.SINGLE);
					cellLeftBorder.setSz(java.math.BigInteger.valueOf(6));
					cellLeftBorder.setSpace(java.math.BigInteger.valueOf(0));
					cellLeftBorder.setColor(effectiveBorderHex);
					
					org.openxmlformats.schemas.wordprocessingml.x2006.main.CTBorder cellRightBorder = 
						org.openxmlformats.schemas.wordprocessingml.x2006.main.CTBorder.Factory.newInstance();
					cellRightBorder.setVal(org.openxmlformats.schemas.wordprocessingml.x2006.main.STBorder.SINGLE);
					cellRightBorder.setSz(java.math.BigInteger.valueOf(6));
					cellRightBorder.setSpace(java.math.BigInteger.valueOf(0));
					cellRightBorder.setColor(effectiveBorderHex);
					
					tcBorders.setTop(cellTopBorder);
					tcBorders.setBottom(cellBottomBorder);
					tcBorders.setLeft(cellLeftBorder);
					tcBorders.setRight(cellRightBorder);
					
					// 设置单元格背景色（优先级：单元格自身 > 行级 > 区域级）
					String cellBgHex = null;
					
					// 1. 检查单元格自身的背景色
					try {
						java.util.regex.Matcher cellBgM = java.util.regex.Pattern.compile(
							"(?i)<(td|th)\\s*[^>]*?style\\s*=\\s*['\"][^'\"]*?background(?:-color)?\\s*:\\s*([^;\"']+)"
						).matcher(fullCellTag);
						if (cellBgM.find()) {
							cellBgHex = convertColorToHex(cellBgM.group(2).trim());
							debugPrint("  检测到单元格背景色: " + cellBgHex);
						}
					} catch (Exception ignore) {}
					
					// 2. 如果单元格没有背景色，使用行级背景色
					if (cellBgHex == null && rowBgHex != null) {
						cellBgHex = rowBgHex;
						debugPrint("  使用行背景色: " + cellBgHex);
					}
					
					// 3. 应用背景色
					if (cellBgHex != null) {
						org.openxmlformats.schemas.wordprocessingml.x2006.main.CTShd shd = tcPr.addNewShd();
						shd.setFill(cellBgHex);
						debugPrint("  最终应用背景色: " + cellBgHex);
					}
					
					// 创建单元格段落
					CTP cellParagraph = cell.addNewP();
					
					// 检测并设置单元格对齐方式 - 改进版本，支持更复杂的样式属性
					debugPrint("分析单元格对齐: " + fullCellTag);
					
					// 检查align属性和style中的text-align - 改进版本，支持单引号和双引号
					java.util.regex.Pattern cellAlignPattern1 = java.util.regex.Pattern.compile(
						"<(?:td|th)\\s*[^>]*?(?:align\\s*=\\s*[\"']([^\"']*)[\"']|style\\s*=\\s*[\"'][^\"']*?text-align\\s*:\\s*([^;\"']*)[^\"']*[\"'])[^>]*>",
						java.util.regex.Pattern.CASE_INSENSITIVE
					);
					java.util.regex.Matcher cellAlignMatcher = cellAlignPattern1.matcher(fullCellTag);
					
					String cellAlign = null;
					if (cellAlignMatcher.find()) {
						cellAlign = cellAlignMatcher.group(1) != null ? cellAlignMatcher.group(1) : cellAlignMatcher.group(2);
					}
					
					// 设置段落对齐方式
					org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPPr pPr = cellParagraph.addNewPPr();
					org.openxmlformats.schemas.wordprocessingml.x2006.main.CTJc jc = pPr.addNewJc();
					
					if (cellAlign != null) {
						debugPrint("检测到单元格对齐方式: " + cellAlign);
						if ("center".equalsIgnoreCase(cellAlign)) {
							jc.setVal(org.openxmlformats.schemas.wordprocessingml.x2006.main.STJc.CENTER);
						} else if ("right".equalsIgnoreCase(cellAlign)) {
							jc.setVal(org.openxmlformats.schemas.wordprocessingml.x2006.main.STJc.END);
						} else {
							jc.setVal(org.openxmlformats.schemas.wordprocessingml.x2006.main.STJc.START); // 左对齐
						}
					} else if (isHeader) {
						// 如果是表头且没有明确指定对齐方式，默认居中
						jc.setVal(org.openxmlformats.schemas.wordprocessingml.x2006.main.STJc.CENTER);
						debugPrint("表头默认居中对齐");
					} else {
						// 普通单元格默认左对齐
						jc.setVal(org.openxmlformats.schemas.wordprocessingml.x2006.main.STJc.START);
					}
					
					// 处理单元格内容（应用表级默认字体大小和颜色）
					if (cellContent.trim().isEmpty()) {
						// 空单元格
						CTR run = cellParagraph.addNewR();
						CTText text = run.addNewT();
						text.setStringValue("");
					} else if (isHtmlContent(cellContent)) {
						// 包含HTML格式的单元格内容
						java.math.BigInteger fontSizeBigInt = tableFontSizeHalfPts != null ? 
							java.math.BigInteger.valueOf(tableFontSizeHalfPts.longValue()) : null;
						insertHtmlToCellParagraph(cellParagraph, cellContent, isHeader, effectiveTextHex, fontSizeBigInt);
					} else {
						// 纯文本单元格内容
						CTR run = cellParagraph.addNewR();
						
						// 设置运行属性
						if (run.getRPr() == null) {
							run.addNewRPr();
						}
						
						// 如果是表头，设置粗体
						if (isHeader) {
							run.getRPr().addNewB().setVal(true);
						}
						
						// 先应用表级字体大小（作为默认值）
						Integer finalFontSizeHalfPts = tableFontSizeHalfPts;
						
						// 检查单元格是否有自己的font-size设置 - 优先级：单元格 > 表级
						java.util.regex.Pattern fontSizePattern = java.util.regex.Pattern.compile(
							"<(td|th)\\s*[^>]*?style\\s*=\\s*[\"'][^\"']*?font-size\\s*:\\s*(\\d+(?:\\.\\d+)?)\\s*(px|pt|em|rem)?[^\"']*[\"'][^>]*>",
							java.util.regex.Pattern.CASE_INSENSITIVE
						);
						java.util.regex.Matcher fontSizeMatcher = fontSizePattern.matcher(fullCellTag);
						if (fontSizeMatcher.find()) {
							try {
								double fontSize = Double.parseDouble(fontSizeMatcher.group(2));
								String unit = fontSizeMatcher.group(3);
								int pt;
								if (unit == null || "px".equalsIgnoreCase(unit)) {
									pt = (int) Math.round(fontSize * 0.75);
								} else if ("pt".equalsIgnoreCase(unit)) {
									pt = (int) Math.round(fontSize);
								} else if ("em".equalsIgnoreCase(unit) || "rem".equalsIgnoreCase(unit)) {
									pt = (int) Math.round(fontSize * 12);
								} else {
									pt = (int) Math.round(fontSize);
								}
								finalFontSizeHalfPts = Math.max(2, Math.min(3276, pt * 2));
								debugPrint("检测到单元格字体大小: " + fontSize + unit + " -> " + pt + "pt");
							} catch (Exception ignore) {
								debugPrint("解析单元格字体大小失败: " + ignore.getMessage());
							}
						}
						
						// 应用最终确定的字体大小
						if (finalFontSizeHalfPts != null) {
							run.getRPr().addNewSz().setVal(java.math.BigInteger.valueOf(finalFontSizeHalfPts));
							run.getRPr().addNewSzCs().setVal(java.math.BigInteger.valueOf(finalFontSizeHalfPts));
						}
						
						// 检查是否有颜色设置 - 优先级：单元格 > 行 > thead/tbody > 表级
						java.util.regex.Pattern colorPattern = java.util.regex.Pattern.compile(
							"<(td|th)\\s*[^>]*?style\\s*=\\s*[\"'][^\"']*?color\\s*:\\s*([^;\"']*)[^\"']*[\"'][^>]*>",
							java.util.regex.Pattern.CASE_INSENSITIVE
						);
						java.util.regex.Matcher colorMatcher = colorPattern.matcher(fullCellTag);
						String resolvedHex = null;
						if (colorMatcher.find()) {
							// 单元格级别的颜色设置
							String color = colorMatcher.group(2).trim();
							debugPrint("检测到单元格颜色: " + color);
							resolvedHex = convertColorToHex(color);
						} else if (effectiveTextHex != null) {
							// 使用行级/区域级/表级文字颜色作为默认
							resolvedHex = effectiveTextHex;
							debugPrint("使用继承的文字颜色: " + resolvedHex);
						}
						if (resolvedHex != null) {
							org.openxmlformats.schemas.wordprocessingml.x2006.main.CTColor ctColor = run.getRPr().addNewColor();
							ctColor.setVal(resolvedHex);
							debugPrint("最终应用颜色值: " + resolvedHex);
						}
						
						CTText text = run.addNewT();
						text.setStringValue(cellContent.trim());
					}
				}
				
				// 如果没有找到单元格，创建一个空单元格
				if (cellCount == 0) {
					org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTc cell = row.addNewTc();
					CTP cellParagraph = cell.addNewP();
					CTR run = cellParagraph.addNewR();
					CTText text = run.addNewT();
					text.setStringValue("");
				}
			}
			
			debugPrint("表格创建完成，共" + rowCount + "行");
			
		} catch (Exception e) {
			debugPrintException("解析表格时发生异常: " + e.getMessage(), e);
		}
	}
	
	/**
	 * 将HTML内容插入到单元格段落
	 */
	private void insertHtmlToCellParagraph(CTP paragraph, String content, boolean isHeader, String inheritedTextHex, java.math.BigInteger inheritedFontSize) {
		try {
			// 去除段落标签，单元格内容作为单行处理
			String cleanContent = content.replaceAll("(?i)</?p[^>]*>", " ").trim();
			
			// 使用 Jsoup 解析 HTML
			org.jsoup.nodes.Document doc = org.jsoup.Jsoup.parse(cleanContent);
			org.jsoup.nodes.Element body = doc.body();
			
			// 递归处理 body 中的所有节点
			processHtmlNodesForTableCell(paragraph, body, isHeader, inheritedTextHex, inheritedFontSize, null);
			
		} catch (Exception e) {
			debugPrintException("插入HTML到单元格时发生异常: " + e.getMessage(), e);
			// 降级处理：当作纯文本插入
						CTR run = paragraph.addNewR();
						
						if (run.getRPr() == null) {
							run.addNewRPr();
						}
						if (isHeader) {
							run.getRPr().addNewB().setVal(true);
						}
						if (inheritedFontSize != null) {
							run.getRPr().addNewSz().setVal(inheritedFontSize);
				run.getRPr().addNewSzCs().setVal(inheritedFontSize);
						}
						if (inheritedTextHex != null) {
							org.openxmlformats.schemas.wordprocessingml.x2006.main.CTColor ctColor = run.getRPr().addNewColor();
							ctColor.setVal(inheritedTextHex);
						}
						
						CTText text = run.addNewT();
			text.setStringValue(stripHtmlTags(content));
		}
	}
	
	/**
	 * 递归处理HTML节点并插入到表格单元格段落中
	 */
	private void processHtmlNodesForTableCell(CTP paragraph, org.jsoup.nodes.Element element, 
			boolean isHeader, String inheritedTextHex, java.math.BigInteger inheritedFontSize,
			org.openxmlformats.schemas.wordprocessingml.x2006.main.CTRPr parentStyle) {
		
		String tagName = element.tagName().toLowerCase();
		
		// 创建当前节点的样式（继承父节点样式）
		org.openxmlformats.schemas.wordprocessingml.x2006.main.CTRPr currentStyle = 
			org.openxmlformats.schemas.wordprocessingml.x2006.main.CTRPr.Factory.newInstance();
		
		// 继承父样式
		if (parentStyle != null) {
			try {
				currentStyle.set(parentStyle);
			} catch (Exception e) {
				// 忽略复制错误
			}
		}
		
		// 应用继承的表级样式
		if (isHeader && currentStyle.getBArray().length == 0) {
			currentStyle.addNewB().setVal(true);
		}
		if (inheritedFontSize != null && currentStyle.getSzArray().length == 0) {
			currentStyle.addNewSz().setVal(inheritedFontSize);
			currentStyle.addNewSzCs().setVal(inheritedFontSize);
		}
		if (inheritedTextHex != null && currentStyle.getColorList().isEmpty()) {
			org.openxmlformats.schemas.wordprocessingml.x2006.main.CTColor ctColor = currentStyle.addNewColor();
			ctColor.setVal(inheritedTextHex);
		}
		
		// 根据标签类型应用样式
		applyTagStyleToRunProperties(currentStyle, tagName);
		
		// 解析style属性
		String styleAttr = element.attr("style");
		if (styleAttr != null && !styleAttr.trim().isEmpty()) {
			applyInlineStyleToRunProperties(currentStyle, styleAttr);
		}
		
		// 处理特殊标签
		if ("br".equals(tagName)) {
				CTR run = paragraph.addNewR();
			run.addNewBr();
			return;
		}
		
		// 递归处理子节点
		for (org.jsoup.nodes.Node child : element.childNodes()) {
			if (child instanceof org.jsoup.nodes.TextNode) {
				org.jsoup.nodes.TextNode textNode = (org.jsoup.nodes.TextNode) child;
				String text = textNode.text();
				if (!text.trim().isEmpty()) {
					CTR run = paragraph.addNewR();
					if (currentStyle != null) {
						run.setRPr((org.openxmlformats.schemas.wordprocessingml.x2006.main.CTRPr) currentStyle.copy());
					}
					CTText ctText = run.addNewT();
					ctText.setStringValue(text);
				}
			} else if (child instanceof org.jsoup.nodes.Element) {
				org.jsoup.nodes.Element childElement = (org.jsoup.nodes.Element) child;
				processHtmlNodesForTableCell(paragraph, childElement, isHeader, inheritedTextHex, inheritedFontSize, currentStyle);
			}
		}
	}
	
	/**
	 * 根据HTML标签应用样式到运行属性
	 */
	private void applyTagStyleToRunProperties(org.openxmlformats.schemas.wordprocessingml.x2006.main.CTRPr runProperties, String tagName) {
		switch (tagName) {
			case "b":
			case "strong":
				if (runProperties.getBArray().length == 0) {
					runProperties.addNewB().setVal(true);
				}
				break;
			case "i":
			case "em":
				if (runProperties.getIArray().length == 0) {
					runProperties.addNewI().setVal(true);
				}
				break;
			case "u":
				if (runProperties.getUArray().length == 0) {
					runProperties.addNewU().setVal(org.openxmlformats.schemas.wordprocessingml.x2006.main.STUnderline.SINGLE);
				}
				break;
			case "s":
			case "strike":
			case "del":
				if (runProperties.getStrikeArray().length == 0) {
					runProperties.addNewStrike().setVal(true);
				}
				break;
			case "sup":
				if (runProperties.getVertAlignArray().length == 0) {
					runProperties.addNewVertAlign().setVal(org.openxmlformats.schemas.officeDocument.x2006.sharedTypes.STVerticalAlignRun.SUPERSCRIPT);
				}
				break;
			case "sub":
				if (runProperties.getVertAlignArray().length == 0) {
					runProperties.addNewVertAlign().setVal(org.openxmlformats.schemas.officeDocument.x2006.sharedTypes.STVerticalAlignRun.SUBSCRIPT);
				}
				break;
			case "mark":
				// HTML <mark> 标签表示高亮文本，默认使用黄色背景
				if (runProperties.getShdArray().length == 0) {
					org.openxmlformats.schemas.wordprocessingml.x2006.main.CTShd shd = runProperties.addNewShd();
					shd.setFill("FFFF00"); // 默认黄色
					shd.setVal(org.openxmlformats.schemas.wordprocessingml.x2006.main.STShd.CLEAR);
				}
				break;
		}
	}
	
	/**
	 * 应用内联样式到运行属性
	 */
	private void applyInlineStyleToRunProperties(org.openxmlformats.schemas.wordprocessingml.x2006.main.CTRPr runProperties, String style) {
		if (style == null || style.trim().isEmpty()) {
			return;
		}
		
		String[] styles = style.split(";");
		for (String s : styles) {
			String[] keyValue = s.split(":");
			if (keyValue.length == 2) {
				String property = keyValue[0].trim().toLowerCase();
				String value = keyValue[1].trim().toLowerCase();
				
				switch (property) {
					case "font-weight":
						if ("bold".equals(value) || "700".equals(value) || "800".equals(value) || "900".equals(value)) {
							runProperties.addNewB().setVal(true);
						}
						break;
					case "font-style":
						if ("italic".equals(value)) {
							runProperties.addNewI().setVal(true);
						}
						break;
				case "text-decoration":
					// 支持多值组合，如 "underline line-through"
					// CSS允许同时设置多个文本装饰效果，用空格分隔
					if (value.contains("underline")) {
						runProperties.addNewU().setVal(org.openxmlformats.schemas.wordprocessingml.x2006.main.STUnderline.SINGLE);
					}
					if (value.contains("line-through")) {
						runProperties.addNewStrike().setVal(true);
					}
					// 注意：overline（上划线）在Word中不常用，暂不支持
					break;
					case "color":
						String hex = convertColorToHex(value);
						if (hex != null) {
							// 清除现有颜色
							if (runProperties.getColorList() != null && !runProperties.getColorList().isEmpty()) {
								runProperties.getColorList().clear();
							}
							org.openxmlformats.schemas.wordprocessingml.x2006.main.CTColor ctColor = runProperties.addNewColor();
							ctColor.setVal(hex);
						}
						break;
					case "background-color":
					case "background":
						String bgHex = convertColorToHex(value);
						if (bgHex != null) {
							// 清除现有背景色
							if (runProperties.getShdList() != null && !runProperties.getShdList().isEmpty()) {
								runProperties.getShdList().clear();
							}
							// 设置文字背景色（着色）
							org.openxmlformats.schemas.wordprocessingml.x2006.main.CTShd shd = runProperties.addNewShd();
							shd.setFill(bgHex);
							shd.setVal(org.openxmlformats.schemas.wordprocessingml.x2006.main.STShd.CLEAR);
						}
						break;
					case "font-size":
						try {
							String sizeStr = value.trim();
							double fontSize = 0;
							String unit = "px";
							
							java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*(px|pt|em|rem)?");
							java.util.regex.Matcher matcher = pattern.matcher(sizeStr);
							
							if (matcher.find()) {
								fontSize = Double.parseDouble(matcher.group(1));
								if (matcher.group(2) != null && !matcher.group(2).isEmpty()) {
									unit = matcher.group(2).toLowerCase();
								}
								
								int wordFontSize;
								if ("px".equals(unit)) {
									wordFontSize = (int) Math.round(fontSize * 0.75);
								} else if ("pt".equals(unit)) {
									wordFontSize = (int) Math.round(fontSize);
								} else if ("em".equals(unit) || "rem".equals(unit)) {
									wordFontSize = (int) Math.round(fontSize * 12);
								} else {
								wordFontSize = (int) Math.round(fontSize);
								}
								
								wordFontSize = Math.max(1, Math.min(1638, wordFontSize));
								
								// 清除已有的字体大小设置，避免冲突
								if (runProperties.getSzArray().length > 0) {
									for (int i = runProperties.getSzArray().length - 1; i >= 0; i--) {
										runProperties.removeSz(i);
									}
								}
								if (runProperties.getSzCsArray().length > 0) {
									for (int i = runProperties.getSzCsArray().length - 1; i >= 0; i--) {
										runProperties.removeSzCs(i);
									}
								}
								
								// 设置新的字体大小
								runProperties.addNewSz().setVal(java.math.BigInteger.valueOf(wordFontSize * 2));
								runProperties.addNewSzCs().setVal(java.math.BigInteger.valueOf(wordFontSize * 2));
							}
		} catch (Exception e) {
							// 忽略解析错误
						}
						break;
				}
			}
		}
	}
	
	/**
	 * 将纯文本插入到块级SDT（旧版本，保留兼容性）
	 */
	private void insertPlainTextToBlock(CTSdtContentBlock sdtContent, String content) {
		insertPlainTextToBlock(sdtContent, content, null);
	}
	
	/**
	 * 将纯文本插入到块级SDT，并应用样式
	 * 
	 * @param sdtContent 块级SDT内容对象
	 * @param content 文本内容
	 * @param originalStyle 原有样式（可为null）
	 */
	private void insertPlainTextToBlock(CTSdtContentBlock sdtContent, String content, 
			org.openxmlformats.schemas.wordprocessingml.x2006.main.CTRPr originalStyle) {
		try {
			// 按行分割处理
			String[] lines = content.split("\\r?\\n");
			for (int i = 0; i < lines.length; i++) {
				if (i == 0) {
					// 第一行使用现有段落或创建新段落
					CTP paragraph = sdtContent.addNewP();
					CTR run = paragraph.addNewR();
					
					// ===== 应用原有样式 =====
					if (originalStyle != null) {
						run.setRPr((org.openxmlformats.schemas.wordprocessingml.x2006.main.CTRPr) originalStyle.copy());
						debugPrint("已应用原有样式到块级SDT第一行");
					}
					
					CTText text = run.addNewT();
					text.setStringValue(lines[i]);
				} else {
					// 后续行创建新段落
					CTP paragraph = sdtContent.addNewP();
					CTR run = paragraph.addNewR();
					
					// ===== 应用原有样式 =====
					if (originalStyle != null) {
						run.setRPr((org.openxmlformats.schemas.wordprocessingml.x2006.main.CTRPr) originalStyle.copy());
					}
					
					CTText text = run.addNewT();
					text.setStringValue(lines[i]);
				}
			}
		} catch (Exception e) {
			debugPrintException("插入纯文本到块级SDT时发生异常: " + e.getMessage(), e);
		}
	}
	
	/**
	 * 将样式应用到块级SDT中的所有段落和运行对象
	 * 
	 * @param sdtContent 块级SDT内容对象
	 * @param originalStyle 要应用的样式
	 */
	private void applyStyleToBlock(CTSdtContentBlock sdtContent, 
			org.openxmlformats.schemas.wordprocessingml.x2006.main.CTRPr originalStyle) {
		if (sdtContent == null || originalStyle == null) {
			return;
		}
		
		try {
			int totalRuns = 0;
			// 遍历所有段落
			if (sdtContent.getPList() != null) {
				for (CTP paragraph : sdtContent.getPList()) {
					// 遍历段落中的所有运行对象
					if (paragraph.getRList() != null) {
						for (CTR run : paragraph.getRList()) {
							totalRuns++;
							// 如果运行对象还没有样式
							if (run.getRPr() == null) {
								// 复制并应用样式
								run.setRPr((org.openxmlformats.schemas.wordprocessingml.x2006.main.CTRPr) originalStyle.copy());
							} else {
								// 已有样式，尝试合并
								mergeStyles(run.getRPr(), originalStyle);
							}
						}
					}
				}
			}
			debugPrint("已应用样式到块级SDT的 " + totalRuns + " 个运行对象");
		} catch (Exception e) {
			debugPrintException("应用样式到块级SDT时发生异常: " + e.getMessage(), e);
		}
	}
	
	/**
	 * 应用HTML格式到运行对象 - 改进版本，支持更复杂的HTML标签
	 */
	private void applyHtmlFormattingToRun(CTR run, String htmlContent) {
		try {
			// 标记是否设置了颜色
			boolean colorSet = false;
			
			// 改进的HTML标签检测，支持更复杂的属性
			if (htmlContent.matches("(?i).*<(b|strong)\\s*[^>]*>.*")) {
				if (run.getRPr() == null) {
					run.addNewRPr();
				}
				run.getRPr().addNewB().setVal(true);
			}
			
			if (htmlContent.matches("(?i).*<(i|em)\\s*[^>]*>.*")) {
				if (run.getRPr() == null) {
					run.addNewRPr();
				}
				run.getRPr().addNewI().setVal(true);
			}
			
			if (htmlContent.matches("(?i).*<u\\s*[^>]*>.*")) {
				if (run.getRPr() == null) {
					run.addNewRPr();
				}
				run.getRPr().addNewU().setVal(org.openxmlformats.schemas.wordprocessingml.x2006.main.STUnderline.SINGLE);
			}
			
			if (htmlContent.matches("(?i).*<mark\\s*[^>]*>.*")) {
				if (run.getRPr() == null) {
					run.addNewRPr();
				}
				// 默认使用黄色背景
				org.openxmlformats.schemas.wordprocessingml.x2006.main.CTShd shd = run.getRPr().addNewShd();
				shd.setFill("FFFF00");
				shd.setVal(org.openxmlformats.schemas.wordprocessingml.x2006.main.STShd.CLEAR);
			}
			
			// 检测字体大小
			java.util.regex.Pattern fontSizePattern = java.util.regex.Pattern.compile(
				"(?i)<[^>]*style\\s*=\\s*[\"'][^\"']*?font-size\\s*:\\s*(\\d+(?:\\.\\d+)?)\\s*(px|pt|em|rem)?[^\"']*[\"'][^>]*>",
				java.util.regex.Pattern.CASE_INSENSITIVE
			);
			java.util.regex.Matcher fontSizeMatcher = fontSizePattern.matcher(htmlContent);
			if (fontSizeMatcher.find()) {
				if (run.getRPr() == null) {
					run.addNewRPr();
				}
				double fontSize = Double.parseDouble(fontSizeMatcher.group(1));
				String unit = fontSizeMatcher.group(2);
				
				// 转换字体大小到Word单位（半磅）
				int wordFontSize;
				if (unit == null || "px".equals(unit)) {
					wordFontSize = (int) (fontSize * 0.75); // 假设1px = 0.75pt
				} else if ("pt".equals(unit)) {
					wordFontSize = (int) fontSize;
				} else if ("em".equals(unit) || "rem".equals(unit)) {
					wordFontSize = (int) (fontSize * 12); // 假设1em = 12pt
				} else {
					wordFontSize = (int) fontSize;
				}
				
				// 确保字体大小在合理范围内
				wordFontSize = Math.max(1, Math.min(1638, wordFontSize));
				
				run.getRPr().addNewSz().setVal(java.math.BigInteger.valueOf(wordFontSize * 2)); // Word使用半磅单位
				debugPrint("设置字体大小: " + wordFontSize + "pt (原始值: " + fontSize + unit + ")");
			}
			
			// 检测字体颜色
			java.util.regex.Pattern fontColorPattern = java.util.regex.Pattern.compile(
				"(?i)<[^>]*style\\s*=\\s*[\"'][^\"']*?color\\s*:\\s*([^;\"']*)[^\"']*[\"'][^>]*>",
				java.util.regex.Pattern.CASE_INSENSITIVE
			);
			java.util.regex.Matcher fontColorMatcher = fontColorPattern.matcher(htmlContent);
			if (fontColorMatcher.find()) {
				if (run.getRPr() == null) {
					run.addNewRPr();
				}
				String color = fontColorMatcher.group(1).trim();
				org.openxmlformats.schemas.wordprocessingml.x2006.main.CTColor ctColor = run.getRPr().addNewColor();
				String hexColor = convertColorToHex(color);
				ctColor.setVal(hexColor);
				colorSet = true;
				debugPrint("设置字体颜色: " + hexColor + " (原始值: " + color + ")");
			}
			
		// 检测背景色
		java.util.regex.Pattern bgColorPattern = java.util.regex.Pattern.compile(
			"(?i)<[^>]*style\\s*=\\s*[\"'][^\"']*?background(?:-color)?\\s*:\\s*([^;\"']+)[^\"']*[\"'][^>]*>",
			java.util.regex.Pattern.CASE_INSENSITIVE
		);
		java.util.regex.Matcher bgColorMatcher = bgColorPattern.matcher(htmlContent);
		if (bgColorMatcher.find()) {
			if (run.getRPr() == null) {
				run.addNewRPr();
			}
			String bgColor = bgColorMatcher.group(1).trim();
			String hexBgColor = convertColorToHex(bgColor);
			if (hexBgColor != null) {
				// 设置文字背景色（着色）
				org.openxmlformats.schemas.wordprocessingml.x2006.main.CTShd shd = run.getRPr().addNewShd();
				shd.setFill(hexBgColor);
				shd.setVal(org.openxmlformats.schemas.wordprocessingml.x2006.main.STShd.CLEAR);
				debugPrint("设置背景色: " + hexBgColor + " (原始值: " + bgColor + ")");
			}
		}
		
		// 检测 font-weight（CSS属性）
		java.util.regex.Pattern fontWeightPattern = java.util.regex.Pattern.compile(
			"(?i)<[^>]*style\\s*=\\s*[\"'][^\"']*?font-weight\\s*:\\s*([^;\"']+)[^\"']*[\"'][^>]*>",
			java.util.regex.Pattern.CASE_INSENSITIVE
		);
		java.util.regex.Matcher fontWeightMatcher = fontWeightPattern.matcher(htmlContent);
		if (fontWeightMatcher.find()) {
			String weight = fontWeightMatcher.group(1).trim().toLowerCase();
			if ("bold".equals(weight) || "700".equals(weight) || "800".equals(weight) || "900".equals(weight)) {
				if (run.getRPr() == null) {
					run.addNewRPr();
				}
				run.getRPr().addNewB().setVal(true);
				debugPrint("检测到 font-weight: " + weight + "，设置粗体");
			}
		}
		
		// 检测 font-style（CSS属性）
		java.util.regex.Pattern fontStylePattern = java.util.regex.Pattern.compile(
			"(?i)<[^>]*style\\s*=\\s*[\"'][^\"']*?font-style\\s*:\\s*([^;\"']+)[^\"']*[\"'][^>]*>",
			java.util.regex.Pattern.CASE_INSENSITIVE
		);
		java.util.regex.Matcher fontStyleMatcher = fontStylePattern.matcher(htmlContent);
		if (fontStyleMatcher.find()) {
			String style = fontStyleMatcher.group(1).trim().toLowerCase();
			if ("italic".equals(style)) {
				if (run.getRPr() == null) {
					run.addNewRPr();
				}
				run.getRPr().addNewI().setVal(true);
				debugPrint("检测到 font-style: italic，设置斜体");
			}
		}
		
		// 检测 text-decoration（CSS属性）- 支持多值组合
		java.util.regex.Pattern textDecorationPattern = java.util.regex.Pattern.compile(
			"(?i)<[^>]*style\\s*=\\s*[\"'][^\"']*?text-decoration\\s*:\\s*([^;\"']+)[^\"']*[\"'][^>]*>",
			java.util.regex.Pattern.CASE_INSENSITIVE
		);
		java.util.regex.Matcher textDecorationMatcher = textDecorationPattern.matcher(htmlContent);
		if (textDecorationMatcher.find()) {
			String decoration = textDecorationMatcher.group(1).trim().toLowerCase();
			if (run.getRPr() == null) {
				run.addNewRPr();
			}
			// 支持多值组合，如 "underline line-through"
			if (decoration.contains("underline")) {
				run.getRPr().addNewU().setVal(org.openxmlformats.schemas.wordprocessingml.x2006.main.STUnderline.SINGLE);
				debugPrint("检测到 text-decoration: underline");
			}
			if (decoration.contains("line-through")) {
				run.getRPr().addNewStrike().setVal(true);
				debugPrint("检测到 text-decoration: line-through");
			}
		}
		
		// 如果没有设置颜色，默认设置为黑色，避免继承ContentControl的浅灰色（DCDCDC）
			if (!colorSet) {
				if (run.getRPr() == null) {
					run.addNewRPr();
				}
				// 检查是否已有颜色设置
				if (run.getRPr().getColorList().isEmpty()) {
					org.openxmlformats.schemas.wordprocessingml.x2006.main.CTColor ctColor = run.getRPr().addNewColor();
					ctColor.setVal("000000"); // 黑色
					debugPrint("未检测到颜色样式，设置默认黑色");
				}
			}
			
		} catch (Exception e) {
			debugPrintException("应用HTML格式时发生异常: " + e.getMessage(), e);
		}
	}
	
	/**
	 * 去除HTML标签
	 */
	private String stripHtmlTags(String html) {
		if (html == null) {
			return "";
		}
		return html.replaceAll("<[^>]+>", "").trim();
	}
	
		/**
	 * 检查内容是否包含HTML标签 - 改进版本，支持更多HTML标签和属性
	 * 
	 * @param content 要检查的内容
	 * @return 如果包含HTML标签则返回true
	 */
	private boolean isHtmlContent(String content) {
		if (content == null || content.trim().isEmpty()) {
			return false;
		}
		
		// 检查常见的HTML标签 - 改进版本，支持更复杂的属性
		String lowerContent = content.toLowerCase();
		
		// 检查表格标签（优先级最高）
		if (lowerContent.contains("<table") || lowerContent.contains("</table>")) {
			debugPrint("检测到表格HTML内容");
			return true;
		}
		
		// 检查其他HTML标签 - 支持自闭合标签和复杂属性（包括删除线、上下标和高亮标记）
		return lowerContent.contains("<b") || lowerContent.contains("<strong") ||
			   lowerContent.contains("<i") || lowerContent.contains("<em") ||
			   lowerContent.contains("<u") || lowerContent.contains("<span") ||
			   lowerContent.contains("<s") || lowerContent.contains("<strike") || lowerContent.contains("<del") ||
			   lowerContent.contains("<sup") || lowerContent.contains("<sub") ||
			   lowerContent.contains("<mark") ||
			   lowerContent.contains("<p") || lowerContent.contains("<br") ||
			   lowerContent.contains("<tr") || lowerContent.contains("<td") ||
			   lowerContent.contains("<th") || lowerContent.contains("</tr>") ||
			   lowerContent.contains("</td>") || lowerContent.contains("</th>") ||
			   lowerContent.contains("</b>") || lowerContent.contains("</strong>") ||
			   lowerContent.contains("</i>") || lowerContent.contains("</em>") ||
			   lowerContent.contains("</u>") || lowerContent.contains("</span>") ||
			   lowerContent.contains("</s>") || lowerContent.contains("</strike>") || lowerContent.contains("</del>") ||
			   lowerContent.contains("</sup>") || lowerContent.contains("</sub>") ||
			   lowerContent.contains("</mark>") ||
			   lowerContent.contains("</p>") ||
			   // 检查样式属性
			   lowerContent.contains("style=") ||
			   // 检查其他常见HTML属性
			   lowerContent.contains("class=") || lowerContent.contains("id=") ||
			   lowerContent.contains("align=") || lowerContent.contains("width=") ||
			   lowerContent.contains("height=") || lowerContent.contains("color=") ||
			   lowerContent.contains("bgcolor=") || lowerContent.contains("border=") ||
			   // 检查CSS样式
			   lowerContent.contains("font-size:") || lowerContent.contains("font-weight:") ||
			   lowerContent.contains("text-align:") || lowerContent.contains("background-color:") ||
			   lowerContent.contains("border:") || lowerContent.contains("margin:") ||
			   lowerContent.contains("padding:") || lowerContent.contains("display:");
	}
	
	
	/**
	 * 插入纯文本内容到行内SDT，并应用样式
	 * 
	 * @param sdtContentRun ContentControl运行对象
	 * @param content 文本内容
	 * @param originalStyle 原有样式（可为null）
	 */
	private void insertPlainTextToRun(CTSdtContentRun sdtContentRun, String content, 
			org.openxmlformats.schemas.wordprocessingml.x2006.main.CTRPr originalStyle) {
		if (content == null || content.trim().isEmpty()) {
			return;
		}
		
		// 处理换行符
		String[] lines = content.split("\\r?\\n");
		
		for (int i = 0; i < lines.length; i++) {
			if (i > 0) {
				// 添加换行
				CTR breakRun = sdtContentRun.addNewR();
				// 换行符也应用样式（但不需要颜色）
				if (originalStyle != null) {
					org.openxmlformats.schemas.wordprocessingml.x2006.main.CTRPr cleanStyle = 
						(org.openxmlformats.schemas.wordprocessingml.x2006.main.CTRPr) originalStyle.copy();
					breakRun.setRPr(cleanStyle);
				}
				breakRun.addNewBr();
			}
			
			if (!lines[i].trim().isEmpty()) {
				CTR run = sdtContentRun.addNewR();
				
				// ===== 应用原有样式（占位符灰色已在复制样式时通过showingPlcHdr判断处理） =====
				if (originalStyle != null) {
					// 复制样式到新的运行对象
					org.openxmlformats.schemas.wordprocessingml.x2006.main.CTRPr copiedStyle = 
						(org.openxmlformats.schemas.wordprocessingml.x2006.main.CTRPr) originalStyle.copy();
					
					// 检查颜色，如果没有颜色，设置为黑色
					String originalColor = getStyleColorValue(copiedStyle);
					if (originalColor == null || originalColor.isEmpty()) {
						debugPrint("[颜色修正] 无颜色 -> 黑色, 文本: \"" + 
							(lines[i].length() > 30 ? lines[i].substring(0, 30) + "..." : lines[i]) + "\"");
						setStyleColorValue(copiedStyle, "000000");
					}
					
					run.setRPr(copiedStyle);
				} else {
					// 如果没有原有样式，创建新样式并设置黑色
					org.openxmlformats.schemas.wordprocessingml.x2006.main.CTRPr newStyle = run.addNewRPr();
					org.openxmlformats.schemas.wordprocessingml.x2006.main.CTColor color = newStyle.addNewColor();
					color.setVal("000000");
					debugPrint("[颜色修正] 无样式 -> 黑色, 文本: \"" + 
						(lines[i].length() > 30 ? lines[i].substring(0, 30) + "..." : lines[i]) + "\"");
				}
				
				CTText text = run.addNewT();
				text.setStringValue(lines[i]);
			}
		}
	}
	
	/**
	 * 将样式应用到 ContentRun 中的所有运行对象
	 * 
	 * @param sdtContentRun ContentControl运行对象
	 * @param originalStyle 要应用的样式
	 */
	private void applyStyleToContentRun(CTSdtContentRun sdtContentRun, 
			org.openxmlformats.schemas.wordprocessingml.x2006.main.CTRPr originalStyle) {
		if (sdtContentRun == null || originalStyle == null) {
			return;
		}
		
		try {
			CTR[] runs = sdtContentRun.getRArray();
			if (runs != null && runs.length > 0) {
				// 检测原始样式的颜色（占位符灰色已在复制样式时通过showingPlcHdr判断处理）
				String originalColor = getStyleColorValue(originalStyle);
				
				for (CTR run : runs) {
					// 如果运行对象还没有样式，或者需要覆盖样式
					if (run.getRPr() == null) {
						// 复制并应用样式
						org.openxmlformats.schemas.wordprocessingml.x2006.main.CTRPr copiedStyle = 
							(org.openxmlformats.schemas.wordprocessingml.x2006.main.CTRPr) originalStyle.copy();
						
						// 如果没有颜色，设置为黑色
						if (originalColor == null || originalColor.isEmpty()) {
							setStyleColorValue(copiedStyle, "000000");
						}
						
						run.setRPr(copiedStyle);
					} else {
						// 已有样式，尝试合并（保留HTML样式，补充ContentControl默认样式）
						mergeStyles(run.getRPr(), originalStyle);
					}
				}
			}
		} catch (Exception e) {
			debugPrintException("应用样式时发生异常: " + e.getMessage(), e);
		}
	}
	
	/**
	 * 合并样式：将 defaultStyle 中的属性合并到 currentStyle，但不覆盖 currentStyle 中已有的属性
	 * 
	 * 注意：此方法采用补充策略，从 defaultStyle 中补充 currentStyle 缺失的属性。
	 * HTML 内容自带的样式（如粗体、斜体、颜色）会被优先保留。
	 * 
	 * @param currentStyle 当前样式（会被修改）
	 * @param defaultStyle 默认样式（提供缺失的属性）
	 */
	private void mergeStyles(org.openxmlformats.schemas.wordprocessingml.x2006.main.CTRPr currentStyle,
			org.openxmlformats.schemas.wordprocessingml.x2006.main.CTRPr defaultStyle) {
		if (currentStyle == null || defaultStyle == null) {
			return;
		}
		
		try {
			// 1. 合并字体大小 (sz - 字体大小)
			if (currentStyle.getSzArray().length == 0 && defaultStyle.getSzArray().length > 0) {
				org.openxmlformats.schemas.wordprocessingml.x2006.main.CTHpsMeasure sz = currentStyle.addNewSz();
				sz.setVal(defaultStyle.getSzArray(0).getVal());
				debugPrint("[样式合并] 继承字体大小: " + defaultStyle.getSzArray(0).getVal());
			}
			
			// 2. 合并复杂字体大小 (szCs - 复杂文字字体大小)
			if (currentStyle.getSzCsArray().length == 0 && defaultStyle.getSzCsArray().length > 0) {
				org.openxmlformats.schemas.wordprocessingml.x2006.main.CTHpsMeasure szCs = currentStyle.addNewSzCs();
				szCs.setVal(defaultStyle.getSzCsArray(0).getVal());
			}
			
			// 3. 合并字体名称 (rFonts)
			if (currentStyle.getRFontsArray().length == 0 && defaultStyle.getRFontsArray().length > 0) {
				org.openxmlformats.schemas.wordprocessingml.x2006.main.CTFonts rFonts = 
					(org.openxmlformats.schemas.wordprocessingml.x2006.main.CTFonts) defaultStyle.getRFontsArray(0).copy();
				currentStyle.setRFontsArray(new org.openxmlformats.schemas.wordprocessingml.x2006.main.CTFonts[]{rFonts});
				debugPrint("[样式合并] 继承字体名称");
			}
			
			// 4. 合并粗体 (b) - 如果HTML没有设置粗体，但原样式有粗体，则继承
			if (currentStyle.getBArray().length == 0 && defaultStyle.getBArray().length > 0) {
				org.openxmlformats.schemas.wordprocessingml.x2006.main.CTOnOff b = currentStyle.addNewB();
				b.setVal(defaultStyle.getBArray(0).getVal());
			}
			
			// 5. 合并斜体 (i) - 如果HTML没有设置斜体，但原样式有斜体，则继承
			if (currentStyle.getIArray().length == 0 && defaultStyle.getIArray().length > 0) {
				org.openxmlformats.schemas.wordprocessingml.x2006.main.CTOnOff i = currentStyle.addNewI();
				i.setVal(defaultStyle.getIArray(0).getVal());
			}
			
			// 6. 合并下划线 (u) - 如果HTML没有设置下划线，但原样式有下划线，则继承
			if (currentStyle.getUArray().length == 0 && defaultStyle.getUArray().length > 0) {
				org.openxmlformats.schemas.wordprocessingml.x2006.main.CTUnderline u = 
					(org.openxmlformats.schemas.wordprocessingml.x2006.main.CTUnderline) defaultStyle.getUArray(0).copy();
				currentStyle.setUArray(new org.openxmlformats.schemas.wordprocessingml.x2006.main.CTUnderline[]{u});
			}
			
			// 7. 合并字符样式 (rStyle) - 如果HTML没有设置样式引用，但原样式有，则继承
			if (currentStyle.getRStyleArray().length == 0 && defaultStyle.getRStyleArray().length > 0) {
				org.openxmlformats.schemas.wordprocessingml.x2006.main.CTString rStyle = currentStyle.addNewRStyle();
				rStyle.setVal(defaultStyle.getRStyleArray(0).getVal());
			}
			
			// 8. 合并背景色/着色 (shd) - 如果HTML没有设置背景色，但原样式有背景色，则继承
			if (currentStyle.getShdArray().length == 0 && defaultStyle.getShdArray().length > 0) {
				org.openxmlformats.schemas.wordprocessingml.x2006.main.CTShd shd = 
					(org.openxmlformats.schemas.wordprocessingml.x2006.main.CTShd) defaultStyle.getShdArray(0).copy();
				currentStyle.setShdArray(new org.openxmlformats.schemas.wordprocessingml.x2006.main.CTShd[]{shd});
				debugPrint("[样式合并] 继承背景色");
			}
			
			// 注意：颜色 (color) 不合并，因为HTML可能已经设置了颜色，我们要保留HTML的颜色
			
		} catch (Exception e) {
			debugPrintException("合并样式时发生异常: " + e.getMessage(), e);
		}
	}
	
	/**
	 * 将颜色值转换为十六进制格式 - 改进版本，支持更多颜色格式
	 * 支持多种颜色格式：颜色名称、十六进制、RGB、RGBA、HSL、HSLA等
	 * 
	 * @param color 颜色值
	 * @return 十六进制颜色值（不含#）
	 */
	private String convertColorToHex(String color) {
		if (color == null || color.trim().isEmpty()) {
			return "000000"; // 默认黑色
		}
		
		String cleanColor = color.trim().toLowerCase();
		
		// 如果已经是十六进制格式（带或不带#）
		if (cleanColor.startsWith("#")) {
			return cleanColor.substring(1);
		}
		if (cleanColor.matches("^[0-9a-f]{6}$")) {
			return cleanColor;
		}
		if (cleanColor.matches("^[0-9a-f]{3}$")) {
			// 3位十六进制转换为6位
			return String.valueOf(cleanColor.charAt(0)) + cleanColor.charAt(0) +
				   String.valueOf(cleanColor.charAt(1)) + cleanColor.charAt(1) +
				   String.valueOf(cleanColor.charAt(2)) + cleanColor.charAt(2);
		}
		
		// 常见颜色名称映射 - 扩展版本
		switch (cleanColor) {
			case "red": return "FF0000";
			case "green": return "00FF00";
			case "blue": return "0000FF";
			case "black": return "000000";
			case "white": return "FFFFFF";
			case "yellow": return "FFFF00";
			case "cyan": return "00FFFF";
			case "magenta": return "FF00FF";
			case "gray": case "grey": return "808080";
			case "orange": return "FFA500";
			case "purple": return "800080";
			case "brown": return "A52A2A";
			case "pink": return "FFC0CB";
			case "lime": return "00FF00";
			case "navy": return "000080";
			case "teal": return "008080";
			case "silver": return "C0C0C0";
			case "gold": return "FFD700";
			case "maroon": return "800000";
			case "olive": return "808000";
			case "aqua": return "00FFFF";
			case "fuchsia": return "FF00FF";
			case "darkred": return "8B0000";
			case "darkgreen": return "006400";
			case "darkblue": return "00008B";
			case "darkgray": case "darkgrey": return "404040";
			case "lightgray": case "lightgrey": return "D3D3D3";
			case "lightblue": return "ADD8E6";
			case "lightgreen": return "90EE90";
			case "lightyellow": return "FFFFE0";
			case "lightpink": return "FFB6C1";
			case "lightcoral": return "F08080";
			case "lightsalmon": return "FFA07A";
			case "lightseagreen": return "20B2AA";
			case "lightskyblue": return "87CEFA"; 
			case "lightsteelblue": return "B0C4DE";
			case "lightcyan": return "E0FFFF";
			case "lightgoldenrodyellow": return "FAFAD2";
			case "limegreen": return "32CD32";
			case "linen": return "FAF0E6";
			case "mediumaquamarine": return "66CDAA";
			case "mediumblue": return "0000CD";
			case "mediumorchid": return "BA55D3";
			case "mediumpurple": return "9370DB";
			case "mediumseagreen": return "3CB371";
			case "mediumslateblue": return "7B68EE";
			case "mediumspringgreen": return "00FA9A";
			case "mediumturquoise": return "48D1CC";
			case "mediumvioletred": return "C71585";
			case "midnightblue": return "191970";
			case "mintcream": return "F5FFFA";
			case "mistyrose": return "FFE4E1";
			case "moccasin": return "FFE4B5";
			case "navajowhite": return "FFDEAD";
			case "oldlace": return "FDF5E6";
			case "olivedrab": return "6B8E23";
			case "orangered": return "FF4500";
			case "orchid": return "DA70D6";
			case "palegoldenrod": return "EEE8AA";
			case "palegreen": return "98FB98";
			case "paleturquoise": return "AFEEEE";
			case "palevioletred": return "DB7093";
			case "papayawhip": return "FFEFD5";
			case "peachpuff": return "FFDAB9";
			case "peru": return "CD853F";
			case "plum": return "DDA0DD";
			case "powderblue": return "B0E0E6";
			case "rosybrown": return "BC8F8F";
			case "royalblue": return "4169E1";
			case "saddlebrown": return "8B4513";
			case "salmon": return "FA8072";
			case "sandybrown": return "F4A460";
			case "seagreen": return "2E8B57";
			case "seashell": return "FFF5EE";
			case "sienna": return "A0522D";
			case "skyblue": return "87CEEB";
			case "slateblue": return "6A5ACD";
			case "slategray": case "slategrey": return "708090";
			case "snow": return "FFFAFA";
			case "springgreen": return "00FF7F";
			case "steelblue": return "4682B4";
			case "tan": return "D2B48C";
			case "thistle": return "D8BFD8";
			case "tomato": return "FF6347";
			case "turquoise": return "40E0D0";
			case "violet": return "EE82EE";
			case "wheat": return "F5DEB3";
			case "whitesmoke": return "F5F5F5";
			case "yellowgreen": return "9ACD32";
		}
		
		// 尝试解析RGB格式 rgb(r, g, b)
		if (cleanColor.startsWith("rgb(") && cleanColor.endsWith(")")) {
			try {
				String rgbValues = cleanColor.substring(4, cleanColor.length() - 1);
				String[] parts = rgbValues.split(",");
				if (parts.length == 3) {
					int r = Integer.parseInt(parts[0].trim());
					int g = Integer.parseInt(parts[1].trim());
					int b = Integer.parseInt(parts[2].trim());
					return String.format("%02X%02X%02X", r, g, b);
				}
			} catch (Exception e) {
				debugPrintException("解析RGB颜色时发生异常: " + e.getMessage(), e);
			}
		}
		
		// 尝试解析RGBA格式 rgba(r, g, b, a) - 忽略透明度
		if (cleanColor.startsWith("rgba(") && cleanColor.endsWith(")")) {
			try {
				String rgbaValues = cleanColor.substring(5, cleanColor.length() - 1);
				String[] parts = rgbaValues.split(",");
				if (parts.length == 4) {
					int r = Integer.parseInt(parts[0].trim());
					int g = Integer.parseInt(parts[1].trim());
					int b = Integer.parseInt(parts[2].trim());
					return String.format("%02X%02X%02X", r, g, b);
				}
			} catch (Exception e) {
				debugPrintException("解析RGBA颜色时发生异常: " + e.getMessage(), e);
			}
		}
		
		// 尝试解析HSL格式 hsl(h, s%, l%)
		if (cleanColor.startsWith("hsl(") && cleanColor.endsWith(")")) {
			try {
				String hslValues = cleanColor.substring(4, cleanColor.length() - 1);
				String[] parts = hslValues.split(",");
				if (parts.length == 3) {
					double h = Double.parseDouble(parts[0].trim());
					double s = Double.parseDouble(parts[1].trim().replace("%", "")) / 100.0;
					double l = Double.parseDouble(parts[2].trim().replace("%", "")) / 100.0;
					
					// HSL转RGB的简单实现
					double c = (1 - Math.abs(2 * l - 1)) * s;
					double x = c * (1 - Math.abs((h / 60) % 2 - 1));
					double m = l - c / 2;
					
					double r = 0, g = 0, b = 0;
					if (h >= 0 && h < 60) {
						r = c; g = x; b = 0;
					} else if (h >= 60 && h < 120) {
						r = x; g = c; b = 0;
					} else if (h >= 120 && h < 180) {
						r = 0; g = c; b = x;
					} else if (h >= 180 && h < 240) {
						r = 0; g = x; b = c;
					} else if (h >= 240 && h < 300) {
						r = x; g = 0; b = c;
					} else if (h >= 300 && h < 360) {
						r = c; g = 0; b = x;
					}
					
					return String.format("%02X%02X%02X", 
						(int)((r + m) * 255), 
						(int)((g + m) * 255), 
						(int)((b + m) * 255));
				}
			} catch (Exception e) {
				debugPrintException("解析HSL颜色时发生异常: " + e.getMessage(), e);
			}
		}
		
		// 尝试解析HSLA格式 hsla(h, s%, l%, a) - 忽略透明度
		if (cleanColor.startsWith("hsla(") && cleanColor.endsWith(")")) {
			try {
				String hslaValues = cleanColor.substring(5, cleanColor.length() - 1);
				String[] parts = hslaValues.split(",");
				if (parts.length == 4) {
					double h = Double.parseDouble(parts[0].trim());
					double s = Double.parseDouble(parts[1].trim().replace("%", "")) / 100.0;
					double l = Double.parseDouble(parts[2].trim().replace("%", "")) / 100.0;
					
					// HSL转RGB的简单实现（与上面相同）
					double c = (1 - Math.abs(2 * l - 1)) * s;
					double x = c * (1 - Math.abs((h / 60) % 2 - 1));
					double m = l - c / 2;
					
					double r = 0, g = 0, b = 0;
					if (h >= 0 && h < 60) {
						r = c; g = x; b = 0;
					} else if (h >= 60 && h < 120) {
						r = x; g = c; b = 0;
					} else if (h >= 120 && h < 180) {
						r = 0; g = c; b = x;
					} else if (h >= 180 && h < 240) {
						r = 0; g = x; b = c;
					} else if (h >= 240 && h < 300) {
						r = x; g = 0; b = c;
					} else if (h >= 300 && h < 360) {
						r = c; g = 0; b = x;
					}
					
					return String.format("%02X%02X%02X", 
						(int)((r + m) * 255), 
						(int)((g + m) * 255), 
						(int)((b + m) * 255));
				}
			} catch (Exception e) {
				debugPrintException("解析HSLA颜色时发生异常: " + e.getMessage(), e);
			}
		}
		
		// 如果无法解析，返回默认黑色
		debugPrint("无法解析颜色值: " + color + "，使用默认黑色");
		return "000000";
	}
	
	/**
	 * 从样式中获取颜色值
	 * 
	 * @param style 样式对象
	 * @return 颜色的十六进制值（不含#），如果没有颜色则返回null
	 */
	private String getStyleColorValue(org.openxmlformats.schemas.wordprocessingml.x2006.main.CTRPr style) {
		if (style == null) {
			return null;
		}
		
		try {
			// 检查是否有颜色列表
			if (style.getColorList() != null && !style.getColorList().isEmpty()) {
				org.openxmlformats.schemas.wordprocessingml.x2006.main.CTColor ctColor = style.getColorList().get(0);
				if (ctColor != null && ctColor.getVal() != null) {
					// getVal() 返回 Object，实际是 byte[]，需要转换为十六进制字符串
					Object valObj = ctColor.getVal();
					String colorVal;
					
					if (valObj instanceof byte[]) {
						colorVal = bytesToHex((byte[]) valObj);
					} else {
						// 如果不是byte[]，直接转换为字符串
						colorVal = valObj.toString();
					}
					
					// 移除可能的 # 前缀并转为大写
					if (colorVal.startsWith("#")) {
						colorVal = colorVal.substring(1);
					}
					return colorVal.toUpperCase();
				}
			}
		} catch (Exception e) {
			debugPrintException("获取颜色值时发生异常: " + e.getMessage(), e);
		}
		
		return null;
	}
	
	/**
	 * 将字节数组转换为十六进制字符串
	 * 
	 * @param bytes 字节数组
	 * @return 十六进制字符串
	 */
	private String bytesToHex(byte[] bytes) {
		if (bytes == null || bytes.length == 0) {
			return "";
		}
		
		StringBuilder hexString = new StringBuilder();
		for (byte b : bytes) {
			// 将字节转换为对应的ASCII字符
			hexString.append((char) (b & 0xFF));
		}
		return hexString.toString();
	}
	
	/**
	 * 设置样式的颜色值
	 * 
	 * @param style 样式对象
	 * @param colorHex 颜色的十六进制值（不含#）
	 */
	private void setStyleColorValue(org.openxmlformats.schemas.wordprocessingml.x2006.main.CTRPr style, String colorHex) {
		if (style == null || colorHex == null) {
			return;
		}
		
		try {
			// 清除现有颜色
			if (style.getColorList() != null && !style.getColorList().isEmpty()) {
				style.getColorList().clear();
			}
			
			// 设置新颜色
			org.openxmlformats.schemas.wordprocessingml.x2006.main.CTColor ctColor = style.addNewColor();
			ctColor.setVal(colorHex.toUpperCase());
		} catch (Exception e) {
			debugPrintException("设置颜色值时发生异常: " + e.getMessage(), e);
		}
	}
	
	/**
	 * 判断颜色是否为占位符的灰色
	 * 
	 * 常见的占位符灰色包括：
	 * - DCDCDC (Gainsboro)
	 * - D3D3D3 (LightGray)
	 * - C0C0C0 (Silver)
	 * - A9A9A9 (DarkGray)
	 * - 808080 (Gray)
	 * 以及接近这些颜色的值
	 * 
	 * @param colorHex 颜色的十六进制值（不含#）
	 * @return 如果是占位符灰色返回true，否则返回false
	 */
	private boolean isPlaceholderGrayColor(String colorHex) {
		if (colorHex == null || colorHex.isEmpty()) {
			return false;
		}
		
		try {
			// 转为大写并移除可能的 # 前缀
			String cleanColor = colorHex.toUpperCase().trim();
			if (cleanColor.startsWith("#")) {
				cleanColor = cleanColor.substring(1);
			}
			
			// 检查常见的占位符灰色
			String[] placeholderGrays = {
				"DCDCDC", // Gainsboro - Word默认占位符颜色
				"D3D3D3", // LightGray
				"C0C0C0", // Silver
				"A9A9A9", // DarkGray
				"808080", // Gray
				"999999", // Medium Gray
				"CCCCCC", // Light Gray
				"B0B0B0", // Another common placeholder gray
			};
			
			for (String grayColor : placeholderGrays) {
				if (cleanColor.equals(grayColor)) {
					return true;
				}
			}
			
			// 进一步检查：如果RGB三个分量都相近（表示灰色），且在浅灰色范围内（128-220）
			if (cleanColor.length() == 6) {
				try {
					int r = Integer.parseInt(cleanColor.substring(0, 2), 16);
					int g = Integer.parseInt(cleanColor.substring(2, 4), 16);
					int b = Integer.parseInt(cleanColor.substring(4, 6), 16);
					
					// RGB差异小于20，且都在浅灰色范围内（128-220）
					int maxDiff = Math.max(Math.abs(r - g), Math.max(Math.abs(g - b), Math.abs(r - b)));
					boolean isGrayish = maxDiff <= 20;
					boolean isLightGray = r >= 128 && r <= 220 && g >= 128 && g <= 220 && b >= 128 && b <= 220;
					
					return isGrayish && isLightGray;
				} catch (NumberFormatException e) {
					// 解析失败，忽略
				}
			}
			
		} catch (Exception e) {
			debugPrintException("判断占位符灰色时发生异常: " + e.getMessage(), e);
		}
		
		return false;
	}

}