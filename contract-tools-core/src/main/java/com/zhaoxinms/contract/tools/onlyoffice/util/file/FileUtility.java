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

import java.nio.file.Path;

import com.zhaoxinms.contract.tools.onlyoffice.enums.DocumentType;

// specify the file utility functions
public interface FileUtility {
    DocumentType getDocumentType(String fileName); // get the document type

    String getFileName(String url); // get file name from its URL

    String getFileNameWithoutExtension(String url); // get file name without extension

    String getFileExtension(String url); // get file extension from URL

    String getInternalExtension(DocumentType type); // get an editor internal extension

    Path generateFilepath(String directory, String fullFileName); // generate the file path from file directory and name
}
