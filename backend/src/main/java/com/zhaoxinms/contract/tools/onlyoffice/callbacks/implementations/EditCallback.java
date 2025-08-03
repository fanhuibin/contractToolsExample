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

import org.springframework.stereotype.Component;

import com.zhaoxinms.contract.tools.onlyoffice.callbacks.Callback;
import com.zhaoxinms.contract.tools.onlyoffice.callbacks.Status;
import com.zhaoxinms.contract.tools.onlyoffice.dto.Action; 
import com.zhaoxinms.contract.tools.onlyoffice.dto.Track;

@Component
public class EditCallback implements Callback {
    @Override
    public int handle(Track body, String fileId, String contractId) { // handle the callback when the document is being
                                                                      // edited
        int result = 0;
        Action action = body.getActions().get(0); // get the user ID who is editing the document
        if (action.getType().equals(com.zhaoxinms.contract.tools.onlyoffice.enums.Action.edit)) { // if this value is not equal to the user
                                                                               // ID
            String user = action.getUserid(); // get user ID
            if (!body.getUsers().contains(user)) { // if this user is not specified in the body
                String key = body.getKey(); // get document key
                try {
                    // 验证合同的状态是否可以编辑
                    // 保存文件
                } catch (Exception e) {
                    e.printStackTrace();
                    result = 1;
                }
            }
        }
        return result;
    }

    @Override
    public int getStatus() { // get document status
        return Status.EDITING.getCode(); // return status 1 - document is being edited
    }
}
