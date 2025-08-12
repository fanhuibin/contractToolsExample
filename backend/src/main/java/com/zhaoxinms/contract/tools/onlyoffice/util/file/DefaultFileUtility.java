/**
 *
 * (c) Copyright Ascensio System SIA 2021
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 */

package com.zhaoxinms.contract.tools.onlyoffice.util.file;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.zhaoxinms.contract.tools.onlyoffice.enums.DocumentType;

@Component
@Qualifier("default")
public class DefaultFileUtility implements FileUtility {

    // document extensions
    private List<String> ExtsDocument =
        Arrays.asList(".doc", ".docx", ".docm", ".dot", ".dotx", ".dotm", ".odt", ".fodt", ".ott", ".rtf", ".txt",
            ".html", ".htm", ".mht", ".xml", ".pdf", ".djvu", ".fb2", ".epub", ".xps", ".oform");

    // spreadsheet extensions
    private List<String> ExtsSpreadsheet =
        Arrays.asList(".xls", ".xlsx", ".xlsm", ".xlsb", ".xlt", ".xltx", ".xltm", ".ods", ".fods", ".ots", ".csv");

    // presentation extensions
    private List<String> ExtsPresentation = Arrays.asList(".pps", ".ppsx", ".ppsm", ".ppt", ".pptx", ".pptm", ".pot",
        ".potx", ".potm", ".odp", ".fodp", ".otp");

    // get the document type
    public DocumentType getDocumentType(String fileName) {
        String ext = getFileExtension(fileName).toLowerCase(); // get file extension from its name
        // word type for document extensions
        if (ExtsDocument.contains(ext))
            return DocumentType.word;

        // cell type for spreadsheet extensions
        if (ExtsSpreadsheet.contains(ext))
            return DocumentType.cell;

        // slide type for presentation extensions
        if (ExtsPresentation.contains(ext))
            return DocumentType.slide;

        // default file type is word
        return DocumentType.word;
    }

    // get file name from its URL
    public String getFileName(String url) {
        if (url == null) return "";
        // try to extract from query parameter 'name'
        int qIndex = url.indexOf('?');
        if (qIndex >= 0) {
            String query = url.substring(qIndex + 1);
            String[] parts = query.split("&");
            for (String part : parts) {
                int eq = part.indexOf('=');
                if (eq > 0) {
                    String key = part.substring(0, eq);
                    if ("name".equalsIgnoreCase(key)) {
                        String value = part.substring(eq + 1);
                        try {
                            value = java.net.URLDecoder.decode(value, java.nio.charset.StandardCharsets.UTF_8.name());
                        } catch (Exception ignore) {}
                        return value;
                    }
                }
            }
        }
        // fallback: last path segment, strip query/fragment
        String fileName = url.substring(url.lastIndexOf('/') + 1);
        int hash = fileName.indexOf('#');
        if (hash >= 0) fileName = fileName.substring(0, hash);
        int qm = fileName.indexOf('?');
        if (qm >= 0) fileName = fileName.substring(0, qm);
        return fileName;
    }

    // get file name without extension
    public String getFileNameWithoutExtension(String url) {
        String fileName = getFileName(url);
        if (fileName == null) return null;
        int dot = fileName.lastIndexOf('.');
        if (dot < 0) return fileName; // no extension
        return fileName.substring(0, dot);
    }

    // get file extension from URL
    public String getFileExtension(String url) {
        String fileName = getFileName(url);
        if (fileName == null || fileName.isEmpty()) return "";
        int dot = fileName.lastIndexOf('.')
        ;
        if (dot < 0) return "";
        return fileName.substring(dot).toLowerCase();
    }

    // get an editor internal extension
    public String getInternalExtension(DocumentType type) {
        // .docx for word file type
        if (type.equals(DocumentType.word))
            return ".docx";

        // .xlsx for cell file type
        if (type.equals(DocumentType.cell))
            return ".xlsx";

        // .pptx for slide file type
        if (type.equals(DocumentType.slide))
            return ".pptx";

        // the default file type is .docx
        return ".docx";
    }

    // generate the file path from file directory and name
    public Path generateFilepath(String directory, String fullFileName) {
        String fileName = getFileNameWithoutExtension(fullFileName); // get file name without extension
        String fileExtension = getFileExtension(fullFileName); // get file extension
        Path path = Paths.get(directory + fullFileName); // get the path to the files with the specified name

        for (int i = 1; Files.exists(path); i++) { // run through all the files with the specified name
            fileName = getFileNameWithoutExtension(fullFileName) + "(" + i + ")"; // get a name of each file without
                                                                                  // extension and add an index to it
            path = Paths.get(directory + fileName + fileExtension); // create a new path for this file with the correct
                                                                    // name and extension
        }

        path = Paths.get(directory + fileName + fileExtension);
        return path;
    }
}
