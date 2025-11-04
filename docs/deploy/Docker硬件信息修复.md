# Docker 硬件信息修复

## 问题
Docker容器中部署后，授权页面硬件信息显示"无法获取"。

## 原因
容器无法访问宿主机的硬件信息目录和设备文件。

## 解决方案

只需要在 `zxcm-tool` 服务配置中添加3处配置即可。

### 修改位置

在你的 `docker-compose.yml` 的 `zxcm-tool` 服务中：

**1. 在 `volumes` 下添加（挂载硬件信息目录）：**
```yaml
volumes:
  # ... 原有配置保持不变 ...
  # 新增以下3行
  - /sys/class/dmi/id:/host-dmi:ro
  - /sys/devices/virtual/dmi/id:/sys/devices/virtual/dmi/id:ro
  - /dev/mem:/dev/mem:ro
```

**2. 在 `privileged: true` 之前添加（设备访问）：**
```yaml
devices:
  - /dev/mem:/dev/mem

cap_add:
  - SYS_RAWIO
  - SYS_ADMIN
```

### 完整修改后的配置

```yaml
zxcm-tool:
  image: 13671354640/zxcm:0.9.1
  container_name: zxcm-tool
  environment:
    TZ: Asia/Shanghai
  volumes:
    # 原有配置
    - /docker/server1/logs/:/zxcm/server/logs/
    - /docker/server1/application.yml:/zxcm/server/application.yml
    - /docker/server1/license.lic:/zxcm/server/license.lic
    - /docker/server1/uploadPath:/zxcm/server/uploadPath
    - /docker/server1/contract-tools-sdk-encrypted.jar:/zxcm/server/app.jar
    - /docker/server1/runner:/zxcm/server/runner
    # ========== 新增：硬件信息目录 ==========
    - /sys/class/dmi/id:/host-dmi:ro
    - /sys/devices/virtual/dmi/id:/sys/devices/virtual/dmi/id:ro
    - /dev/mem:/dev/mem:ro
  # ========== 新增：设备和权限 ==========
  devices:
    - /dev/mem:/dev/mem
  cap_add:
    - SYS_RAWIO
    - SYS_ADMIN
  # ========================================
  privileged: true
  restart: always
  networks:
    ruoyi_net:
      ipv4_address: 172.30.0.60
```

## 应用修改

```bash
# 1. 备份
cd /docker/server1  # 或你的 docker-compose.yml 所在目录
cp docker-compose.yml docker-compose.yml.backup

# 2. 修改配置
vi docker-compose.yml
# 按照上面的说明添加配置

# 3. 重启容器
docker-compose down
docker-compose up -d
```

## 验证

```bash
# 查看日志
docker logs zxcm-tool --tail 50 | grep -i "硬件\|mac"

# 进入容器测试
docker exec -it zxcm-tool dmidecode -s baseboard-serial-number
docker exec -it zxcm-tool ip link show | grep link/ether
```

访问前端授权页面，硬件信息应该正常显示了。

