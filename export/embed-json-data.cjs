const fs = require('fs');
const path = require('path');

/**
 * 将JSON数据嵌入到HTML文件中，避免file://协议的CORS问题
 * 适用于 export 项目的统一管理
 */
function embedJsonData(options = {}) {
    // 文件路径配置 - 基于 export 项目根目录
    const config = {
        htmlPath: options.htmlPath || './dist/index.html',
        taskStatusPath: options.taskStatusPath || './public/data/current/task-status.json',
        compareResultPath: options.compareResultPath || './public/data/current/compare-result.json',
        dataDir: options.dataDir || 'data/current',
        // 备用路径 - 如果 public 目录不存在，尝试 dist 目录
        fallbackTaskStatusPath: './dist/data/current/task-status.json',
        fallbackCompareResultPath: './dist/data/current/compare-result.json',
        ...options
    };

    // 输出当前配置
    console.log('📁 Export项目 - 文件路径配置:');
    console.log(`- HTML文件: ${config.htmlPath}`);
    console.log(`- 任务状态: ${config.taskStatusPath}`);
    console.log(`- 比对结果: ${config.compareResultPath}`);
    console.log(`- 数据目录: ${config.dataDir}`);
    console.log('');

    try {
        // 1. 验证并读取HTML模板文件
        if (!fs.existsSync(config.htmlPath)) {
            throw new Error(`HTML文件不存在: ${config.htmlPath}`);
        }
        let htmlContent = fs.readFileSync(config.htmlPath, 'utf8');
        console.log('✅ 读取HTML文件成功');

        // 2. 读取或生成JSON数据文件
        let taskStatusData = {};
        let compareResultData = {};

        // 读取任务状态数据 - 支持备用路径
        const taskStatusPath = fs.existsSync(config.taskStatusPath) ? 
            config.taskStatusPath : config.fallbackTaskStatusPath;
            
        if (fs.existsSync(taskStatusPath)) {
            taskStatusData = JSON.parse(fs.readFileSync(taskStatusPath, 'utf8'));
            console.log(`✅ 读取任务状态JSON成功: ${taskStatusPath}`);
        } else {
            console.warn('⚠️ 任务状态JSON文件不存在，使用默认数据');
            taskStatusData = generateDefaultTaskStatus();
        }

        // 读取比对结果数据 - 支持备用路径
        const compareResultPath = fs.existsSync(config.compareResultPath) ? 
            config.compareResultPath : config.fallbackCompareResultPath;
            
        if (fs.existsSync(compareResultPath)) {
            compareResultData = JSON.parse(fs.readFileSync(compareResultPath, 'utf8'));
            console.log(`✅ 读取比对结果JSON成功: ${compareResultPath}`);
        } else {
            console.warn('⚠️ 比对结果JSON文件不存在，使用默认数据');
            compareResultData = generateDefaultCompareResult(config.dataDir);
        }

        // 3. 创建内嵌脚本
        const inlineScript = `<script>
// 内联数据，避免file://协议的CORS问题
// 由 export/embed-json-data.cjs 自动生成
window.TASK_STATUS_DATA = ${JSON.stringify(taskStatusData, null, 2)};
window.COMPARE_RESULT_DATA = ${JSON.stringify(compareResultData, null, 2)};
console.log('内嵌数据已加载:', { taskStatus: window.TASK_STATUS_DATA, compareResult: window.COMPARE_RESULT_DATA });
</script>`;

        // 4. 检查是否已经包含内嵌数据
        if (htmlContent.includes('window.TASK_STATUS_DATA')) {
            console.log('⚠️ HTML文件已包含内嵌数据，将替换现有数据');
            // 移除现有的内嵌脚本
            htmlContent = htmlContent.replace(/<script>[\s\S]*?window\.TASK_STATUS_DATA[\s\S]*?<\/script>/g, '');
        }

        // 5. 将脚本插入到</head>标签之前
        const finalHtml = htmlContent.replace('</head>', inlineScript + '\n</head>');

        // 6. 写回HTML文件
        fs.writeFileSync(config.htmlPath, finalHtml, 'utf8');
        console.log('🎉 JSON数据内嵌完成！');
        
        // 7. 输出统计信息
        console.log('\n📊 数据统计:');
        console.log(`- 任务状态: ${taskStatusData.oldFileName} vs ${taskStatusData.newFileName}`);
        console.log(`- 页面总数: ${compareResultData.oldImageInfo?.totalPages || 0}`);
        console.log(`- 差异数量: ${compareResultData.differences?.length || 0}`);
        console.log(`- 失败页面: ${compareResultData.failedPagesCount || 0}`);
        console.log(`- 输出文件: ${path.resolve(config.htmlPath)}`);

    } catch (error) {
        console.error('❌ 嵌入JSON数据失败:', error.message);
        console.error('💡 请检查文件路径是否正确，或使用自定义配置');
        console.error('💡 确保在 export 项目根目录下运行此脚本');
        process.exit(1);
    }
}

/**
 * 生成默认任务状态数据
 */
function generateDefaultTaskStatus() {
    return {
        currentPageOld: 14,
        totalSteps: 8,
        oldFileName: "示例文档.pdf",
        newFileName: "示例文档 (1).pdf",
        remainingTime: "0秒",
        currentPageNew: 14
    };
}

/**
 * 生成默认比对结果数据
 */
function generateDefaultCompareResult(dataDir = 'data/current') {
    return {
        failedPages: [],
        failedPagesCount: 0,
        differences: [],
        oldFileName: "示例文档.pdf",
        newFileName: "示例文档 (1).pdf",
        startTime: Date.now(),
        oldImageInfo: {
            totalPages: 14,
            pages: Array.from({length: 14}, (_, i) => ({
                pageNum: i + 1,
                imageUrl: `./${dataDir}/images/old/page-${i + 1}.png`,
                width: 1322,
                height: 1870
            }))
        },
        newImageInfo: {
            totalPages: 14,
            pages: Array.from({length: 14}, (_, i) => ({
                pageNum: i + 1,
                imageUrl: `./${dataDir}/images/new/page-${i + 1}.png`,
                width: 1322,
                height: 1870
            }))
        },
        oldImageBaseUrl: `./${dataDir}/images/old`,
        newImageBaseUrl: `./${dataDir}/images/new`
    };
}

/**
 * 自动检测并创建必要的目录结构
 */
function ensureDirectoryStructure(config) {
    const dirs = [
        path.dirname(config.taskStatusPath),
        path.dirname(config.compareResultPath),
        path.join(path.dirname(config.compareResultPath), 'images', 'old'),
        path.join(path.dirname(config.compareResultPath), 'images', 'new')
    ];

    dirs.forEach(dir => {
        if (!fs.existsSync(dir)) {
            fs.mkdirSync(dir, { recursive: true });
            console.log(`📁 创建目录: ${dir}`);
        }
    });
}

/**
 * 从后端导出的数据复制到 export 项目
 */
function copyFromBackendExport(backendZipPath, options = {}) {
    try {
        const AdmZip = require('adm-zip');
        
        if (!fs.existsSync(backendZipPath)) {
            throw new Error(`后端导出ZIP文件不存在: ${backendZipPath}`);
        }

        const zip = new AdmZip(backendZipPath);
        const zipEntries = zip.getEntries();

        console.log('📦 从后端导出数据复制到 export 项目...');

        zipEntries.forEach(entry => {
            if (entry.entryName === 'index.html') {
                // 复制HTML文件到 dist 目录
                const htmlPath = './dist/index.html';
                fs.writeFileSync(htmlPath, entry.getData());
                console.log(`✅ 复制HTML文件: ${htmlPath}`);
            } else if (entry.entryName.startsWith('data/')) {
                // 复制数据文件到对应目录
                const targetPath = `./dist/${entry.entryName}`;
                const targetDir = path.dirname(targetPath);
                
                if (!fs.existsSync(targetDir)) {
                    fs.mkdirSync(targetDir, { recursive: true });
                }
                
                fs.writeFileSync(targetPath, entry.getData());
                console.log(`✅ 复制数据文件: ${targetPath}`);
            }
        });

        console.log('🎉 后端数据复制完成！');
        
        // 可选：自动嵌入数据
        if (options.autoEmbed !== false) {
            console.log('\n🔄 自动嵌入JSON数据...');
            embedJsonData();
        }

    } catch (error) {
        console.error('❌ 复制后端数据失败:', error.message);
        if (error.message.includes('Cannot find module')) {
            console.error('💡 请安装 adm-zip: npm install adm-zip');
        }
        process.exit(1);
    }
}

// 如果直接运行此脚本
if (require.main === module) {
    console.log('🔄 Export项目 - 开始嵌入JSON数据到HTML文件...');
    
    // 支持命令行参数
    const args = process.argv.slice(2);
    const options = {};
    
    // 解析命令行参数
    for (let i = 0; i < args.length; i += 2) {
        const key = args[i]?.replace('--', '');
        const value = args[i + 1];
        if (key && value) {
            options[key] = value;
        }
    }
    
    // 显示使用说明
    if (args.includes('--help') || args.includes('-h')) {
        console.log(`
📖 Export项目 - JSON数据嵌入工具使用说明:
  node embed-json-data.cjs [选项]

🔧 选项:
  --htmlPath <路径>           HTML文件路径 (默认: ./dist/index.html)
  --taskStatusPath <路径>     任务状态JSON路径 (默认: ./public/data/current/task-status.json)
  --compareResultPath <路径>  比对结果JSON路径 (默认: ./public/data/current/compare-result.json)
  --dataDir <路径>           数据目录 (默认: data/current)
  --help, -h                 显示此帮助信息

💡 使用示例:
  # 在 export 项目根目录下使用默认配置
  cd export
  node embed-json-data.cjs
  
  # 自定义配置
  node embed-json-data.cjs --htmlPath ./custom/index.html
  
  # 从项目根目录运行（指定完整路径）
  node export/embed-json-data.cjs --htmlPath export/dist/index.html

📁 目录结构:
  export/
  ├── embed-json-data.cjs    # 本脚本（CommonJS格式）
  ├── dist/
  │   ├── index.html         # 目标HTML文件
  │   └── data/current/      # 备用数据目录
  └── public/
      └── data/current/      # 主数据目录
          ├── task-status.json
          ├── compare-result.json
          └── images/
              ├── old/
              └── new/

🔧 注意事项:
  - 此脚本使用 .cjs 扩展名以兼容 ES 模块项目
  - 确保先运行 'npm run build' 生成 dist/index.html
  - 支持从 public/ 和 dist/ 目录读取数据文件
        `);
        process.exit(0);
    }
    
    embedJsonData(options);
}

module.exports = { 
    embedJsonData, 
    generateDefaultTaskStatus, 
    generateDefaultCompareResult, 
    ensureDirectoryStructure,
    copyFromBackendExport
};
