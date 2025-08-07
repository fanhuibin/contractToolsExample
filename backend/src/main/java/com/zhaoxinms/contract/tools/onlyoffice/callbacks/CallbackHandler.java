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

package com.zhaoxinms.contract.tools.onlyoffice.callbacks;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import com.zhaoxinms.contract.tools.onlyoffice.dto.Track;
import com.zhaoxinms.contract.tools.onlyoffice.callbacks.implementations.EditCallback;
import com.zhaoxinms.contract.tools.onlyoffice.callbacks.implementations.ForcesaveCallback;
import com.zhaoxinms.contract.tools.onlyoffice.callbacks.implementations.SaveCallback;

@Service
public class CallbackHandler {

    private Logger logger = LoggerFactory.getLogger(CallbackHandler.class);

    private Map<Integer, Callback> callbackHandlers = new HashMap<>();

    @Autowired
    private ApplicationContext applicationContext;

    public void register(int code, Callback callback) { // register a callback handler
        callbackHandlers.put(code, callback);
    }

    /**
     * 初始化默认的回调处理器
     */
    public void initializeDefaultCallbacks() {
        // 从ApplicationContext获取回调处理器
        EditCallback editCallback = applicationContext.getBean(EditCallback.class);
        SaveCallback saveCallback = applicationContext.getBean(SaveCallback.class);
        ForcesaveCallback forcesaveCallback = applicationContext.getBean(ForcesaveCallback.class);
        
        // 注册默认的回调处理器
        register(1, editCallback);      // 编辑状态
        register(2, saveCallback);      // 保存状态
        register(3, forcesaveCallback); // 强制保存状态
        
        logger.info("Default callback handlers registered");
    }

    public int handle(Track body, String fileId) { // handle a callback
        // 确保回调处理器已初始化
        if (callbackHandlers.isEmpty()) {
            initializeDefaultCallbacks();
        }
        
        Callback callback = callbackHandlers.get(body.getStatus());
        if (callback == null) {
            logger.warn("Callback status " + body.getStatus() + " is not supported yet");
            return 0;
        }

        int result = callback.handle(body, fileId);
        return result;
    }
}
