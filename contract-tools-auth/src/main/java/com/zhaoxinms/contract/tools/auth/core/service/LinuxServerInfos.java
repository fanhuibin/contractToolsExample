package com.zhaoxinms.contract.tools.auth.core.service;

import com.zhaoxinms.contract.tools.auth.core.helper.LoggerHelper;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * Linux服务器硬件信息获取
 */
public class LinuxServerInfos extends AServerInfos {

    @Override
    public String getCPUSerial() throws Exception {
        String result = "";
        try {
            Process process = Runtime.getRuntime().exec("dmidecode -t processor | grep 'ID'");
            process.getOutputStream().close();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("ID:")) {
                    result = line.split(":")[1].trim();
                    break;
                }
            }
            reader.close();
        } catch (Exception e) {
            LoggerHelper.error("获取CPU序列号失败", e);
            // 备用方法
            try {
                Process fallbackProcess = Runtime.getRuntime().exec("cat /proc/cpuinfo | grep 'processor'");
                fallbackProcess.getOutputStream().close();
                BufferedReader fallbackReader = new BufferedReader(new InputStreamReader(fallbackProcess.getInputStream()));
                result = fallbackReader.readLine();
                if (result != null) {
                    result = result.hashCode() + "";
                }
                fallbackReader.close();
            } catch (Exception ex) {
                LoggerHelper.error("备用获取CPU信息失败", ex);
            }
        }
        return result;
    }

    @Override
    public String getMainBoardSerial() throws Exception {
        String result = "";
        try {
            Process process = Runtime.getRuntime().exec("dmidecode -s baseboard-serial-number");
            process.getOutputStream().close();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            result = reader.readLine();
            if (result != null) {
                result = result.trim();
            }
            reader.close();
        } catch (Exception e) {
            LoggerHelper.error("获取主板序列号失败", e);
            // 备用方法
            try {
                Process fallbackProcess = Runtime.getRuntime().exec("dmidecode -t baseboard | grep 'Serial Number'");
                fallbackProcess.getOutputStream().close();
                BufferedReader fallbackReader = new BufferedReader(new InputStreamReader(fallbackProcess.getInputStream()));
                String line = fallbackReader.readLine();
                if (line != null && line.contains(":")) {
                    result = line.split(":")[1].trim();
                }
                fallbackReader.close();
            } catch (Exception ex) {
                LoggerHelper.error("备用获取主板信息失败", ex);
            }
        }
        return result;
    }
}