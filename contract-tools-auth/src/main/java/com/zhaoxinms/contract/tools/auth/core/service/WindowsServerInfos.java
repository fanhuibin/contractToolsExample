package com.zhaoxinms.contract.tools.auth.core.service;

import com.zhaoxinms.contract.tools.auth.core.helper.LoggerHelper;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * Windows服务器硬件信息获取
 */
public class WindowsServerInfos extends AServerInfos {

    @Override
    public String getCPUSerial() throws Exception {
        String result = "";
        try {
            Process process = Runtime.getRuntime().exec("wmic cpu get processorid");
            process.getOutputStream().close();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("ProcessorId")) {
                    continue;
                }
                line = line.trim();
                if (!line.isEmpty()) {
                    result = line;
                    break;
                }
            }
            reader.close();
        } catch (Exception e) {
            LoggerHelper.error("获取CPU序列号失败", e);
        }
        return result;
    }

    @Override
    public String getMainBoardSerial() throws Exception {
        String result = "";
        try {
            Process process = Runtime.getRuntime().exec("wmic baseboard get serialnumber");
            process.getOutputStream().close();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("SerialNumber")) {
                    continue;
                }
                line = line.trim();
                if (!line.isEmpty()) {
                    result = line;
                    break;
                }
            }
            reader.close();
        } catch (Exception e) {
            LoggerHelper.error("获取主板序列号失败", e);
        }
        return result;
    }
}