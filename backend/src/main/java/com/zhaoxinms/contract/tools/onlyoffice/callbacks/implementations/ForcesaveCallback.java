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

import java.io.InputStream;
import java.net.URL;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.zhaoxinms.contract.tools.common.entity.FileInfo;
import com.zhaoxinms.contract.tools.common.exception.OnlyOfficeCallbackException;
import com.zhaoxinms.contract.tools.common.service.FileInfoService;
import com.zhaoxinms.contract.tools.onlyoffice.callbacks.Callback;
import com.zhaoxinms.contract.tools.onlyoffice.callbacks.Status;
import com.zhaoxinms.contract.tools.onlyoffice.dto.Track;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class ForcesaveCallback implements Callback {
    
    @Autowired
    private FileInfoService fileInfoService;

    @Override
    public int handle(Track body, String fileId) { // handle the callback when the force saving request is performed
        int result = 0;
        try {
            // 参数校验
            if (body == null) {
                log.error("回调参数为空");
                return 1;
            }
            
            if (fileId == null || fileId.trim().isEmpty()) {
                log.error("文件ID为空");
                return 1;
            }
            
            String downloadUri = body.getUrl();
            String changesUri = body.getChangesurl();
            String key = body.getKey();
            
            if (downloadUri == null || downloadUri.trim().isEmpty()) {
                log.error("下载URL为空，文件ID: {}", fileId);
                return 1;
            }

            // 获取文件信息
            FileInfo fileInfo = fileInfoService.getById(fileId);
            if (fileInfo == null) {
                log.error("文件数据不存在，文件ID: {}", fileId);
                return 1;
            }

            // 验证OnlyOffice key
            if (key != null && !key.equals(fileInfo.getOnlyofficeKey())) {
                log.warn("OnlyOffice key不匹配，文件ID: {}, 期望: {}, 实际: {}", 
                        fileId, fileInfo.getOnlyofficeKey(), key);
            }

            // 下载文件并更新内容
            URL uri = new URL(downloadUri);
            java.net.HttpURLConnection connection = (java.net.HttpURLConnection)uri.openConnection();
            connection.setConnectTimeout(30000); // 30秒连接超时
            connection.setReadTimeout(300000);   // 5分钟读取超时
            
            // 检查HTTP响应状态
            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                log.error("下载文件失败，HTTP状态码: {}, URL: {}, 文件ID: {}", responseCode, downloadUri, fileId);
                return 1;
            }
            
            InputStream stream = connection.getInputStream();
            
            try {
                // 使用FileInfoService更新文件内容
                boolean success = fileInfoService.saveFile(fileId, stream);
                
                if (success) {
                    log.info("强制保存文件成功，文件ID: {}, 下载URL: {}", fileId, downloadUri);
                } else {
                    log.error("强制保存文件失败，文件ID: {}", fileId);
                    result = 1;
                }
                
            } finally {
                stream.close();
                connection.disconnect();
            }
        } catch (IllegalArgumentException e) {
            log.error("参数错误: {}, 文件ID: {}", e.getMessage(), fileId);
            result = 1;
            // 抛出业务异常供外部处理
            throw new OnlyOfficeCallbackException(fileId, "FORCE_SAVE", "参数错误: " + e.getMessage(), e);
        } catch (java.io.IOException e) {
            log.error("IO异常: {}, 文件ID: {}", e.getMessage(), fileId, e);
            result = 1;
            // 抛出业务异常供外部处理
            throw new OnlyOfficeCallbackException(fileId, "FORCE_SAVE", "IO异常: " + e.getMessage(), e);
        } catch (Exception ex) {
            log.error("强制保存回调处理失败: {}, 文件ID: {}", ex.getMessage(), fileId, ex);
            result = 1;
            // 抛出业务异常供外部处理
            throw new OnlyOfficeCallbackException(fileId, "FORCE_SAVE", "强制保存回调处理失败: " + ex.getMessage(), ex);
        }

        return result;
    }

    @Override
    public int getStatus() { // get document status
        return Status.MUST_FORCE_SAVE.getCode(); // return status 6 - document is being edited, but the current document
                                                 // state is saved
    }
}
