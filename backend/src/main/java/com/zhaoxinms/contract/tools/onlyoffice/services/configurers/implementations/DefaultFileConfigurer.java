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

package com.zhaoxinms.contract.tools.onlyoffice.services.configurers.implementations;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;


import com.zhaoxinms.contract.tools.onlyoffice.enums.Action;
import com.zhaoxinms.contract.tools.onlyoffice.enums.DocumentType;
import com.zhaoxinms.contract.tools.onlyoffice.filemodel.OnlyofficeFileModel;
import com.zhaoxinms.contract.tools.onlyoffice.filemodel.Permission;
import com.zhaoxinms.contract.tools.onlyoffice.services.configurers.FileConfigurer;
import com.zhaoxinms.contract.tools.onlyoffice.services.configurers.wrappers.DefaultDocumentWrapper;
import com.zhaoxinms.contract.tools.onlyoffice.services.configurers.wrappers.DefaultFileWrapper;
import com.zhaoxinms.contract.tools.onlyoffice.util.JwtManager;
import com.zhaoxinms.contract.tools.onlyoffice.util.file.FileUtility;

@Service
@Primary
public class DefaultFileConfigurer implements FileConfigurer<DefaultFileWrapper> {
    @Autowired
    private ObjectFactory<OnlyofficeFileModel> fileModelObjectFactory;

    @Autowired
    private FileUtility fileUtility;

    @Autowired
    private JwtManager jwtManager;

    @Autowired
    private DefaultDocumentConfigurer defaultDocumentConfigurer;

    @Autowired
    private DefaultEditorConfigConfigurer defaultEditorConfigConfigurer;

    // OnlyOffice权限配置
    @Value("${onlyoffice.permissions.view.print:true}")
    private boolean viewPrintEnabled;
    
    @Value("${onlyoffice.permissions.edit.print:true}")
    private boolean editPrintEnabled;
    
    @Value("${onlyoffice.permissions.edit.download:true}")
    private boolean editDownloadEnabled;
    
    @Value("${onlyoffice.permissions.edit.comment:true}")
    private boolean editCommentEnabled;
    
    @Value("${onlyoffice.permissions.edit.chat:true}")
    private boolean editChatEnabled;
    
    @Value("${onlyoffice.permissions.edit.review:true}")
    private boolean editReviewEnabled;
    
    @Value("${onlyoffice.permissions.edit.fillForms:true}")
    private boolean editFillFormsEnabled;
    
    @Value("${onlyoffice.permissions.edit.modifyContentControl:true}")
    private boolean editModifyContentControlEnabled;
    
    @Value("${onlyoffice.permissions.edit.modifyFilter:true}")
    private boolean editModifyFilterEnabled;

    public void configure(OnlyofficeFileModel fileModel, DefaultFileWrapper wrapper) { // define the file configurer
        if (fileModel != null) { // check if the file model is specified
            String fileName = wrapper.getFileName(); // get the fileName parameter from the file wrapper
            Action action = wrapper.getAction(); // get the action parameter from the file wrapper

            DocumentType documentType = fileUtility.getDocumentType(fileName); // get the document type of the specified
                                                                               // file
            fileModel.setDocumentType(documentType); // set the document type to the file model
            fileModel.setType(wrapper.getType()); // set the platform type to the file model

            Permission userPermissions = new Permission();

            // 判断是否是编辑状态
            if (wrapper.getCanEdit()) {
                userPermissions.setChat(editChatEnabled);
                userPermissions.setComment(editCommentEnabled);
                userPermissions.setDownload(editDownloadEnabled);
                userPermissions.setModifyContentControl(editModifyContentControlEnabled);
                userPermissions.setModifyFilter(editModifyFilterEnabled);
                userPermissions.setPrint(editPrintEnabled);
                userPermissions.setEdit(true);
                userPermissions.setFillForms(editFillFormsEnabled);
                userPermissions.setReview(editReviewEnabled);

            } else {
                userPermissions.setChat(false);
                userPermissions.setComment(false);
                userPermissions.setDownload(true);
                userPermissions.setModifyContentControl(false);
                userPermissions.setModifyFilter(false);
                userPermissions.setPrint(viewPrintEnabled);
                userPermissions.setEdit(false);
                userPermissions.setFillForms(true);
                userPermissions.setReview(false);
            }

            String fileExt = fileUtility.getFileExtension(wrapper.getFileName());

            DefaultDocumentWrapper documentWrapper = DefaultDocumentWrapper // define the document wrapper
                .builder().fileId(wrapper.getFileId()).key(wrapper.getKey()).fileName(fileName).url(wrapper.getUrl())
                .permission(updatePermissions(userPermissions, action, wrapper.getCanEdit())).build();

            defaultDocumentConfigurer.configure(fileModel.getDocument(), documentWrapper); // define the document
                                                                                           // configurer
            defaultEditorConfigConfigurer.configure(fileModel.getEditorConfig(), wrapper); // define the editorConfig
                                                                                           // configurer

            Map<String, Object> map = new HashMap<>();
            map.put("type", fileModel.getType());
            map.put("documentType", documentType);
            map.put("document", fileModel.getDocument());
            map.put("editorConfig", fileModel.getEditorConfig());

            fileModel.setToken(jwtManager.createToken(map)); // create a token and set it to the file model
        }
    }

    @Override
    public OnlyofficeFileModel getFileModel(DefaultFileWrapper wrapper) { // get file model
        OnlyofficeFileModel fileModel = fileModelObjectFactory.getObject();
        configure(fileModel, wrapper); // and configure it
        return fileModel;
    }

    private Permission updatePermissions(Permission userPermissions, Action action, Boolean canEdit) {
        userPermissions.setComment(!action.equals(Action.view) && !action.equals(Action.fillForms)
            && !action.equals(Action.embedded) && !action.equals(Action.blockcontent));

        userPermissions.setFillForms(!action.equals(Action.view) && !action.equals(Action.comment)
            && !action.equals(Action.embedded) && !action.equals(Action.blockcontent));

        userPermissions.setReview(canEdit && (action.equals(Action.review) || action.equals(Action.edit)));

        userPermissions.setEdit(canEdit && (action.equals(Action.view) || action.equals(Action.edit)
            || action.equals(Action.filter) || action.equals(Action.blockcontent)));

        return userPermissions;
    }

}
