package com.zhaoxinms.contract.tools.merge.util;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.xmlbeans.XmlCursor;
import org.apache.xmlbeans.XmlObject;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTBookmark;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTBookmarkRange;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTR;

public class BookmarkUtils {

	/**
	 * 指定的段落内存查询bookmark内第一个run
	 * @param object
	 * @param bookmarkName
	 * @return
	 */
	public static XWPFRun getFirstRunInBookmark(XmlObject object, String bookmarkName) {
		if (StringUtils.isEmpty(bookmarkName)) {
			return null;
		}
		boolean start = false;
		try (XmlCursor c = object.newCursor()) {
			c.selectPath("child::*");
			while (c.toNextSelection()) {
				XmlObject o = c.getObject();
				if (o instanceof CTBookmark) {
					CTBookmark bookmark = (CTBookmark) o;
					if (StringUtils.isNotEmpty(bookmark.getName()) && bookmark.getName().equals(bookmarkName)) {
						start = true;
					}
				}
				if (o instanceof CTR) {
					if (start) {
						XWPFRun r = new XWPFRun((CTR) o, null);
						return r;
					}
				}
				if (o instanceof CTBookmarkRange) {
					//start之后的第一个end就结束
					if(start && o.getDomNode().getLocalName().equals("bookmarkEnd")) {
						break;
					}
				}
			}
		}
		return null;
	}
}