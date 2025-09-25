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

package com.zhaoxinms.contract.tools.onlyoffice.configurations;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;
import com.zhaoxinms.contract.tools.config.ZxcmConfig;

@Component
@Scope("prototype")
@Getter
@Setter
public class Logo { // the image file at the top left corner of the Editor header
    @Autowired
    private ZxcmConfig zxcmConfig;
    
    // the path to the image file used to show in common work mode
    public String getImage() {
        return zxcmConfig.getOnlyOffice().getLogo().getLogo();
    }
    
    // the path to the image file used to show in the embedded mode
    public String getImageEmbedded() {
        return zxcmConfig.getOnlyOffice().getLogo().getLogoEmbedded();
    }
    
    // the absolute URL which will be used when someone clicks the logo image
    public String getUrl() {
        return zxcmConfig.getOnlyOffice().getLogo().getLogoUrl();
    }
}
