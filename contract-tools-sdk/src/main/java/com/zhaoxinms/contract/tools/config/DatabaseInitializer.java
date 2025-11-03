package com.zhaoxinms.contract.tools.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

/**
 * 数据库初始化器
 * <p>系统启动时自动检查并创建数据库表结构</p>
 * 
 * @author 山西肇新科技有限公司
 */
@Slf4j
@Component
public class DatabaseInitializer implements ApplicationRunner {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 需要检查和创建的表列表
     */
    private static final List<String> REQUIRED_TABLES = Arrays.asList(
        "file_info",
        "template_design_record"
    );

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("╔══════════════════════════════════════════════════════════════╗");
        log.info("║          数据库初始化检查 - Database Initialization          ║");
        log.info("╚══════════════════════════════════════════════════════════════╝");

        try {
            // 检查是否需要初始化
            boolean needsInitialization = checkIfInitializationNeeded();

            if (needsInitialization) {
                log.info("📋 检测到缺失的数据库表，开始执行初始化...");
                initializeDatabase();
                log.info("✅ 数据库初始化完成！");
            } else {
                log.info("✅ 数据库表结构已存在，跳过初始化");
            }

            // 验证所有表是否创建成功
            verifyTables();

        } catch (Exception e) {
            log.error("❌ 数据库初始化失败！", e);
            throw e;
        }

        log.info("══════════════════════════════════════════════════════════════");
    }

    /**
     * 检查是否需要初始化数据库
     */
    private boolean checkIfInitializationNeeded() {
        log.info("🔍 检查数据库表状态...");
        
        int missingTableCount = 0;
        
        for (String tableName : REQUIRED_TABLES) {
            boolean exists = tableExists(tableName);
            if (!exists) {
                log.warn("   ⚠ 表 [{}] 不存在", tableName);
                missingTableCount++;
            } else {
                log.debug("   ✓ 表 [{}] 已存在", tableName);
            }
        }

        if (missingTableCount > 0) {
            log.info("📊 统计：需要创建 {} 个表", missingTableCount);
            return true;
        }

        return false;
    }

    /**
     * 检查表是否存在
     */
    private boolean tableExists(String tableName) {
        try {
            String sql = "SELECT COUNT(*) FROM information_schema.TABLES " +
                        "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?";
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class, tableName);
            return count != null && count > 0;
        } catch (Exception e) {
            log.debug("检查表 [{}] 时出错: {}", tableName, e.getMessage());
            return false;
        }
    }

    /**
     * 执行数据库初始化
     */
    private void initializeDatabase() throws Exception {
        log.info("📝 读取初始化脚本: schema.sql");
        
        // 读取 SQL 文件
        ClassPathResource resource = new ClassPathResource("schema.sql");
        String sqlScript = StreamUtils.copyToString(
            resource.getInputStream(), 
            StandardCharsets.UTF_8
        );

        log.info("🚀 开始执行初始化脚本...");
        
        // 清理 SQL 脚本：移除注释和空行
        String cleanedScript = cleanSqlScript(sqlScript);
        
        // 分割 SQL 语句（按分号分割）
        String[] sqlStatements = cleanedScript.split(";");
        
        int executedCount = 0;
        for (String sql : sqlStatements) {
            String trimmedSql = sql.trim();
            
            // 跳过空语句
            if (trimmedSql.isEmpty()) {
                continue;
            }

            try {
                jdbcTemplate.execute(trimmedSql);
                executedCount++;
                
                // 提取表名用于日志
                if (trimmedSql.toUpperCase().contains("CREATE TABLE")) {
                    String tableName = extractTableName(trimmedSql);
                    if (tableName != null) {
                        log.info("   ✓ 创建表: {}", tableName);
                    }
                }
            } catch (Exception e) {
                // 如果是表已存在的错误，忽略（因为使用了 IF NOT EXISTS）
                if (!e.getMessage().contains("already exists")) {
                    log.error("   ✗ 执行SQL失败: {}", trimmedSql.substring(0, Math.min(50, trimmedSql.length())));
                    throw e;
                }
            }
        }

        log.info("📊 成功执行 {} 条SQL语句", executedCount);
    }

    /**
     * 清理 SQL 脚本，移除注释和空行
     */
    private String cleanSqlScript(String sqlScript) {
        StringBuilder cleaned = new StringBuilder();
        String[] lines = sqlScript.split("\n");
        
        boolean inMultiLineComment = false;
        
        for (String line : lines) {
            String trimmedLine = line.trim();
            
            // 处理多行注释
            if (trimmedLine.startsWith("/*")) {
                inMultiLineComment = true;
            }
            if (inMultiLineComment) {
                if (trimmedLine.endsWith("*/")) {
                    inMultiLineComment = false;
                }
                continue;
            }
            
            // 跳过单行注释
            if (trimmedLine.startsWith("--")) {
                continue;
            }
            
            // 跳过空行
            if (trimmedLine.isEmpty()) {
                continue;
            }
            
            // 添加有效的 SQL 行
            cleaned.append(line).append("\n");
        }
        
        return cleaned.toString();
    }

    /**
     * 从 CREATE TABLE 语句中提取表名
     */
    private String extractTableName(String sql) {
        try {
            String upperSql = sql.toUpperCase();
            int start = upperSql.indexOf("CREATE TABLE");
            if (start == -1) {
                start = upperSql.indexOf("CREATE TABLE IF NOT EXISTS");
            }
            
            if (start != -1) {
                String afterCreate = sql.substring(start);
                String[] parts = afterCreate.split("\\s+");
                
                for (int i = 0; i < parts.length; i++) {
                    if (parts[i].toUpperCase().equals("EXISTS") && i + 1 < parts.length) {
                        return parts[i + 1].replace("`", "").replace("(", "");
                    } else if (parts[i].toUpperCase().equals("TABLE") && i + 1 < parts.length) {
                        String nextPart = parts[i + 1];
                        if (!nextPart.toUpperCase().equals("IF")) {
                            return nextPart.replace("`", "").replace("(", "");
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.debug("提取表名失败", e);
        }
        return null;
    }

    /**
     * 验证所有表是否创建成功
     */
    private void verifyTables() {
        log.info("🔍 验证表结构...");
        
        boolean allTablesExist = true;
        for (String tableName : REQUIRED_TABLES) {
            boolean exists = tableExists(tableName);
            if (!exists) {
                log.error("   ✗ 表 [{}] 验证失败！", tableName);
                allTablesExist = false;
            } else {
                log.debug("   ✓ 表 [{}] 验证通过", tableName);
            }
        }

        if (allTablesExist) {
            log.info("✅ 所有必需的表都已创建成功（共 {} 个表）", REQUIRED_TABLES.size());
        } else {
            log.error("❌ 部分表创建失败，请检查数据库配置和权限！");
        }
    }
}

