package com.zhaoxinms.contract.tools.config;

import lombok.Data;

/**
 * 肇新合同工具集统一配置类
 * 所有配置项都使用zxcm前缀
 */
@Data
public class ZxcmConfig {
    
    /**
     * OnlyOffice配置
     */
    private OnlyOffice onlyOffice = new OnlyOffice();
    
    /**
     * 文件上传配置
     */
    private FileUpload fileUpload = new FileUpload();
    
    /**
     * 服务器配置
     */
    private Server server = new Server();
    
    /**
     * OnlyOffice配置
     */
    @Data
    public static class OnlyOffice {
        /**
         * OnlyOffice服务器域名
         */
        private String domain = "localhost";
        
        /**
         * OnlyOffice服务器端口
         */
        private String port = "80";
        
        /**
         * 回调配置
         */
        private Callback callback = new Callback();
        
        /**
         * 插件列表
         */
        private String[] plugins = {};
        
        /**
         * JWT密钥
         */
        private String secret = "your-secret-key-here";
        
        /**
         * Logo配置
         */
        private Logo logo = new Logo();
        
        /**
         * 权限配置
         */
        private Permissions permissions = new Permissions();
        
        /**
         * 回调配置
         */
        @Data
        public static class Callback {
            /**
             * 回调URL
             */
            private String url = "http://localhost:8081/onlyoffice/callback";
        }
        
        /**
         * Logo配置
         */
        @Data
        public static class Logo {
            /**
             * Logo图片
             */
            private String logo = "";
            
            /**
             * Logo是否嵌入
             */
            private String logoEmbedded = "";
            
            /**
             * Logo URL
             */
            private String logoUrl = "";
        }
        
        /**
         * 权限配置
         */
        @Data
        public static class Permissions {
            /**
             * 查看模式权限
             */
            private View view = new View();
            
            /**
             * 编辑模式权限
             */
            private Edit edit = new Edit();
            
            /**
             * 查看模式权限
             */
            @Data
            public static class View {
                /**
                 * 是否允许打印
                 */
                private boolean print = true;
            }
            
            /**
             * 编辑模式权限
             */
            @Data
            public static class Edit {
                /**
                 * 是否允许打印
                 */
                private boolean print = true;
                
                /**
                 * 是否允许下载
                 */
                private boolean download = true;
                
                /**
                 * 是否允许评论
                 */
                private boolean comment = true;
                
                /**
                 * 是否允许聊天
                 */
                private boolean chat = true;
                
                /**
                 * 是否允许审阅
                 */
                private boolean review = true;
                
                /**
                 * 是否允许填写表单
                 */
                private boolean fillForms = true;
                
                /**
                 * 是否允许修改内容控件
                 */
                private boolean modifyContentControl = true;
                
                /**
                 * 是否允许修改过滤器
                 */
                private boolean modifyFilter = true;
            }
        }
    }
    
    /**
     * 文件上传配置
     */
    @Data
    public static class FileUpload {
        /**
         * 文件上传根路径
         */
        private String rootPath = "./uploads";
    }
    
    /**
     * 服务器配置
     */
    @Data
    public static class Server {
        /**
         * 服务器端口
         */
        private String port = "8080";
        
        /**
         * 上下文路径
         */
        private Servlet servlet = new Servlet();
        
        /**
         * Servlet配置
         */
        @Data
        public static class Servlet {
            /**
             * 上下文路径
             */
            private String contextPath = "";
        }
    }
} 