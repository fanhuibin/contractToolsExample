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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference; 
import com.zhaoxinms.contract.tools.onlyoffice.configurations.Review;
import com.zhaoxinms.contract.tools.onlyoffice.enums.Action;
import com.zhaoxinms.contract.tools.onlyoffice.enums.Mode;
import com.zhaoxinms.contract.tools.onlyoffice.filemodel.EditorConfig;
import com.zhaoxinms.contract.tools.onlyoffice.filemodel.Plugins;
import com.zhaoxinms.contract.tools.onlyoffice.services.configurers.EditorConfigConfigurer;
import com.zhaoxinms.contract.tools.onlyoffice.services.configurers.wrappers.DefaultCustomizationWrapper;
import com.zhaoxinms.contract.tools.onlyoffice.services.configurers.wrappers.DefaultFileWrapper;
import com.zhaoxinms.contract.tools.onlyoffice.util.file.FileUtility;

import lombok.SneakyThrows;

@Service
@Primary
public class DefaultEditorConfigConfigurer implements EditorConfigConfigurer<DefaultFileWrapper> {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DefaultCustomizationConfigurer defaultCustomizationConfigurer;

    // @Autowired
    // private DefaultEmbeddedConfigurer defaultEmbeddedConfigurer;

    @Autowired
    private FileUtility fileUtility;
    @Value("${onlyoffice.callback.url}")
    private String onlyofficeCallbackUrl;

    @SneakyThrows
    public void configure(EditorConfig config, DefaultFileWrapper wrapper) { // define the editorConfig configurer
        if (wrapper.getActionData() != null) { // check if the actionData is not empty in the editorConfig wrapper
            config.setActionLink(objectMapper.readValue(wrapper.getActionData(),
                new TypeReference<HashMap<String, Object>>() {})); // set actionLink to the editorConfig
        }
        String fileName = wrapper.getFileName(); // set the fileName parameter from the editorConfig wrapper
        String fileExt = fileUtility.getFileExtension(fileName);
        boolean userIsAnon = wrapper.getUser().getName().equals("Anonymous"); // check if the user from the editorConfig
                                                                              // wrapper is anonymous or not

        config.setCallbackUrl(wrapper.getCallbackUrl()); // set the callback URL to the editorConfig
        // config.setCreateUrl(userIsAnon ? null :
        // documentManager.getCreateUrl(fileName, false)); // set the document URL where
        // it will be created to the editorConfig if the user is not anonymous
        config.setLang(wrapper.getLang()); // set the language to the editorConfig
        Boolean canEdit = wrapper.getCanEdit(); // check if the file of the specified type can be edited or not
        Action action = wrapper.getAction(); // get the action parameter from the editorConfig wrapper
        Boolean canReview = wrapper.getCanReview();
        config.setCoEditing(action.equals(Action.view) && userIsAnon ? new HashMap<String, Object>() {
            {
                put("mode", "strict");
                put("change", false);
            }
        } : null);
        Review review = new Review();
        if (canReview) {
            review.setTrackChanges(true);
        }

        defaultCustomizationConfigurer.configure(config.getCustomization(), DefaultCustomizationWrapper.builder() // define
                                                                                                                  // the
                                                                                                                  // customization
                                                                                                                  // configurer
            .action(action).user(userIsAnon ? null : wrapper.getUser()).review(review).build());
        config.setMode(canEdit && !action.equals(Action.view) ? Mode.edit : Mode.view);

        config.setUser(wrapper.getUser());

        Plugins p = new Plugins();
        if (wrapper.getPluginsData().size() > 0) {
            p.setPluginsData(wrapper.getPluginsData());
        }
        config.setPlugins(p);
        // 不使用embed模式
        // defaultEmbeddedConfigurer.configure(config.getEmbedded(), DefaultEmbeddedWrapper.builder()
        // .type(wrapper.getType())
        // .fileName(fileName)
        // .build());
    }
}
