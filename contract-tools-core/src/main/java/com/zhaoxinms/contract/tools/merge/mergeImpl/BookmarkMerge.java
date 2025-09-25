package com.zhaoxinms.contract.tools.merge.mergeImpl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTBookmark;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTP;
import org.w3c.dom.Node;

import com.zhaoxinms.contract.tools.merge.Merge;
import com.zhaoxinms.contract.tools.merge.model.DocContent;
import com.zhaoxinms.contract.tools.merge.util.BookmarkUtils;

import cn.hutool.core.io.FileTypeUtil;

public class BookmarkMerge implements Merge {

	public static final String RUN_NODE_NAME = "w:r";
	public static final String TEXT_NODE_NAME = "w:t";
	public static final String BOOKMARK_START_TAG = "w:bookmarkstart";
	public static final String BOOKMARK_END_TAG = "w:bookmarkEnd";
	public static final String BOOKMARK_ID_ATTR_NAME = "w:id";
	public static final String STYLE_NODE_NAME = "w:rPr";

	@Override
	public void doMerge(String inputFile, String outputFile, List<DocContent> contents) {
		String type = FileTypeUtil.getTypeByPath(inputFile);
		if ("doc".equals(type)) {
			throw new RuntimeException("doc文档不支持");
		}

		if ("zip".equals(type) || "docx".equals(type)) {
			Map<String, String> map = new HashMap<String, String>();
			for (DocContent content : contents) {
				map.put(content.getKey(), content.getContent());
			}
			try {
				InputStream inputStream = Files.newInputStream(Paths.get(inputFile));
				OpenOption[] options = new OpenOption[] { StandardOpenOption.CREATE,
						StandardOpenOption.TRUNCATE_EXISTING };
				OutputStream out = Files.newOutputStream(Paths.get(outputFile), options);
				this.replaceBookmarksByDocx(inputStream, out, map);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	/**
	 * docx 文件中书签的替换
	 *
	 * @param inputStream
	 * @param outputStream
	 * @param dataMap
	 * @throws IOException
	 */
	public static void replaceBookmarksByDocx(InputStream inputStream, OutputStream outputStream,
			Map<String, String> dataMap) throws IOException {
		XWPFDocument document = new XWPFDocument(inputStream).getXWPFDocument();
		List<XWPFParagraph> paragraphList = document.getParagraphs();
		for (XWPFParagraph xwpfParagraph : paragraphList) {
			CTP ctp = xwpfParagraph.getCTP();
			
			for (int dwI = 0; dwI < ctp.sizeOfBookmarkStartArray(); dwI++) {
				CTBookmark bookmark = ctp.getBookmarkStartArray(dwI);
				XWPFRun r = BookmarkUtils.getFirstRunInBookmark(ctp, bookmark.getName());
				
				if (dataMap.containsKey(bookmark.getName())) {
					XWPFRun run = xwpfParagraph.createRun();
					run.setText(dataMap.get(bookmark.getName()));
					if (r!=null) {
						run.setBold(r.isBold());
						run.setUnderline(r.getUnderline());
					}
					
					Node firstNode = bookmark.getDomNode();
					Node nextNode = firstNode.getNextSibling();
					while (nextNode != null) {
						// 循环查找结束符
						String nodeName = nextNode.getNodeName();
						if (nodeName.equals(BOOKMARK_END_TAG)) {
							break;
						} 

						// 删除中间的非结束节点，即删除原书签内容
						Node delNode = nextNode;
						nextNode = nextNode.getNextSibling();
						ctp.getDomNode().removeChild(delNode);
					}

					if (nextNode == null) {
						// 始终找不到结束标识的，就在书签前面添加
						ctp.getDomNode().insertBefore(run.getCTR().getDomNode(), firstNode);
					} else {
						// 找到结束符，将新内容添加到结束符之前，即内容写入bookmark中间
						ctp.getDomNode().insertBefore(run.getCTR().getDomNode(), nextNode);
					}
				}
			}
		}

		document.write(outputStream);
		document.close();
	}
}