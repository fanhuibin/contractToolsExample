/**
 * RiskLocator.js - OnlyOffice插件，用于定位风险位置
 * 
 * 这个插件处理从外部发送的定位请求，可以根据页码、段落或文本内容定位到文档中的特定位置。
 * 使用方法：
 * 1. 将此文件放置在OnlyOffice插件目录中
 * 2. 通过postMessage发送定位请求，格式如下：
 *    {
 *      action: 'locate',
 *      position: {
 *        page: 1,       // 页码
 *        paragraph: 3,  // 段落索引
 *        text: '风险文本' // 要搜索的文本
 *      },
 *      highlight: true  // 是否高亮显示
 *    }
 */

(function(window, undefined) {
  window.Asc.plugin.init = function() {
    // 注册消息处理函数
    window.addEventListener('message', function(event) {
      try {
        const data = typeof event.data === 'string' ? JSON.parse(event.data) : event.data;
        
        if (data && data.action === 'locate') {
          handleLocateAction(data);
        } else if (data && data.action === 'createContentControl') {
          createContentControl(data);
        } else if (data && data.action === 'createBlockContentControl') {
          createBlockContentControl(data);
        }
      } catch (error) {
        console.error('处理消息时出错:', error);
      }
    });

    // 通知父窗口插件已加载
    window.parent.postMessage('loaded', '*');
    window.parent.postMessage(JSON.stringify({ action: 'loaded' }), '*');
  };

  // 处理定位请求
  function handleLocateAction(data) {
    const position = data.position || {};
    const highlight = data.highlight === true;
    
    // 根据不同的定位信息执行不同的定位策略
    if (position.text) {
      // 如果提供了文本，尝试搜索文本
      searchAndLocate(position.text, highlight);
    } else if (position.page && position.paragraph) {
      // 如果提供了页码和段落，尝试定位到特定段落
      locateByPageAndParagraph(position.page, position.paragraph, highlight);
    } else {
      console.warn('定位信息不完整，无法定位');
    }
  }

  // 搜索文本并定位
  function searchAndLocate(text, highlight) {
    window.Asc.plugin.executeMethod('SearchText', [text, { matchCase: false, wholeWords: false }], function(result) {
      if (result && result.length > 0) {
        // 搜索成功，选中第一个匹配项
        window.Asc.plugin.executeMethod('SelectText', [result[0].Start, result[0].End], function() {
          if (highlight) {
            // 如果需要高亮，添加高亮样式
            window.Asc.plugin.executeMethod('SetTextHighlight', ['#FFFF00']);
          }
        });
      } else {
        console.warn('未找到匹配的文本:', text);
      }
    });
  }

  // 根据页码和段落定位
  function locateByPageAndParagraph(page, paragraph, highlight) {
    // 这里的实现依赖于OnlyOffice的API，可能需要根据实际API调整
    window.Asc.plugin.executeMethod('GetDocumentStructure', [], function(structure) {
      try {
        if (structure && structure.pages && structure.pages.length >= page) {
          const targetPage = structure.pages[page - 1];
          if (targetPage && targetPage.paragraphs && targetPage.paragraphs.length >= paragraph) {
            const targetParagraph = targetPage.paragraphs[paragraph - 1];
            if (targetParagraph) {
              // 定位到段落
              window.Asc.plugin.executeMethod('GoToPosition', [targetParagraph.start], function() {
                if (highlight) {
                  // 选中整个段落
                  window.Asc.plugin.executeMethod('SelectText', [targetParagraph.start, targetParagraph.end], function() {
                    window.Asc.plugin.executeMethod('SetTextHighlight', ['#FFFF00']);
                  });
                }
              });
              return;
            }
          }
        }
        console.warn('未找到指定的页码或段落:', { page, paragraph });
      } catch (error) {
        console.error('定位到页码和段落时出错:', error);
      }
    });
  }

  // 创建内联内容控件
  function createContentControl(data) {
    try {
      const id = data.Id || `cc_${Date.now()}`;
      const tag = data.Tag || '';
      const alias = data.Alias || '';
      const readOnly = data.ReadOnly === true;
      
      window.Asc.plugin.executeMethod('CreateContentControl', [0, { Id: id, Tag: tag, Lock: readOnly ? 1 : 0, Alias: alias }]);
    } catch (error) {
      console.error('创建内容控件时出错:', error);
    }
  }

  // 创建块级内容控件
  function createBlockContentControl(data) {
    try {
      const id = data.Id || `block_${Date.now()}`;
      const tag = data.Tag || '';
      const alias = data.Alias || '';
      const rich = data.Rich !== false;
      const readOnly = data.ReadOnly === true;
      
      window.Asc.plugin.executeMethod('CreateContentControl', [1, { 
        Id: id, 
        Tag: tag, 
        Lock: readOnly ? 1 : 0, 
        Alias: alias,
        Appearance: rich ? 0 : 1
      }]);
    } catch (error) {
      console.error('创建块级内容控件时出错:', error);
    }
  }

  window.Asc.plugin.button = function() {
    // 插件按钮点击事件处理
    this.executeCommand("close", "");
  };
})(window);
