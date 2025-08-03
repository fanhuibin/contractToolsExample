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

package com.zhaoxinms.contract.tools.onlyoffice.callbacks.implementations;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.zhaoxinms.contract.tools.common.entity.FileInfo;
import com.zhaoxinms.contract.tools.common.service.FileInfoService;
import com.zhaoxinms.contract.tools.onlyoffice.callbacks.Callback;
import com.zhaoxinms.contract.tools.onlyoffice.callbacks.Status;
import com.zhaoxinms.contract.tools.onlyoffice.dto.Track;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class SaveCallback implements Callback {
    
    @Autowired
    private FileInfoService fileInfoService;

    @Override
    public int handle(Track body, String fileId, String contractId) { // handle the callback when the saving request is performed
        int result = 0;
        try {
            String downloadUri = body.getUrl();
            String changesUri = body.getChangesurl();
            String key = body.getKey();

            // 获取文件信息
            FileInfo fileInfo = fileInfoService.getById(Long.valueOf(fileId));
            if (fileInfo == null) {
                log.error("文件数据不存在，文件ID: {}", fileId);
                throw new RuntimeException("文件数据不存在!");
            }

            // 下载文件并更新内容
            URL uri = new URL(downloadUri);
            java.net.HttpURLConnection connection = (java.net.HttpURLConnection)uri.openConnection();
            InputStream stream = connection.getInputStream();
            
            try {
                // 使用FileInfoService更新文件内容
                boolean success = fileInfoService.updateFileContent(Long.valueOf(fileId), stream);
                
                if (success) {
                    log.info("文件保存成功，文件ID: {}", fileId);
                } else {
                    log.error("文件保存失败，文件ID: {}", fileId);
                    result = 1;
                }
                
            } finally {
                stream.close();
            }

        } catch (Exception ex) {
            log.error("保存回调处理失败: {}", ex.getMessage(), ex);
            result = 1;
        }

        return result;
    }

    @Override
    public int getStatus() { // get document status
        return Status.SAVE.getCode(); // return status 2 - document is ready for saving
    }
}
