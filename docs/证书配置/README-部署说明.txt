SSL证书部署文档说明
====================

文件列表：
----------
1. SSL证书部署步骤.md         - 详细部署文档（工程人员必读）
2. nginx.conf                  - Nginx HTTPS配置文件
3. application-https.yml       - application.yml 的 HTTPS 配置示例
4. 部署检查清单.txt            - 部署检查清单（打印使用）
5. 一键部署.sh                 - Linux/Mac 一键部署脚本
6. 一键部署.bat                - Windows 一键部署脚本

证书文件：
----------
- onlyoffice.crt              - 证书文件
- onlyoffice.key              - 私钥文件
- onlyoffice.csr              - 证书签名请求
- dhparam.pem                 - DH参数文件

快速开始：
----------

方式1：使用一键脚本（推荐）
Windows:  双击运行 一键部署.bat
Linux:    bash 一键部署.sh 192.168.0.100

方式2：手动执行
1. 阅读 SSL证书部署步骤.md
2. 按照步骤1-8依次执行
3. 使用 部署检查清单.txt 进行验证

重要提醒：
----------
1. 部署后需要修改 /docker/server1/application.yml
   - 将所有 OnlyOffice 相关的 http:// 改为 https://
   - 参考 application-https.yml 示例

2. 修改 application.yml 后必须重启应用：
   docker restart zxcm-tool

3. 浏览器访问会显示"不安全"警告（自签名证书正常）
   点击"高级" → "继续访问"即可

验证方法：
----------
浏览器访问：
- 主站: https://192.168.0.100
- OnlyOffice: https://192.168.0.100:8082

功能测试：
- 登录系统
- 创建文档
- 编辑文档
- 保存文档

故障排查：
----------
如果遇到问题，请查看：
1. docker logs nginx-web --tail 50
2. docker logs docServer --tail 50
3. docker logs zxcm-tool --tail 50

技术支持：
----------
参考官方文档：
https://doc.zhaoxinms.com/pages/fee1a2/#_3-2-1-%E7%94%9F%E6%88%90%E8%87%AA%E7%AD%BE%E5%90%8D%E8%AF%81%E4%B9%A6

