<template>
  <div
    id="viewFileContainer"
    style="height: calc(100vh - 65px); width: calc(100vw - 0px)"
  />
</template>
<script>
import { getUrl } from '@/api/tool/wps'
import request from '@/utils/request'
import { getToken } from '@/utils/auth'
export default {
  data() {
    return {
      baseURL: process.env.VUE_APP_BASE_API,
      onlyofficeLoaded: false,
      pluginLoaded: false,
      isEdit: false,
      commentUser: [],
    }
  },
  mounted() {
    window.addEventListener('message', this.receiveMessage)
  },
  methods: {
    // type 分为 CONTRACT、TMPL_MAIN
    loadDoc(fileId, type, contractId, isEdit, isReview = false) {
      this.isEdit = isEdit
      getUrl(fileId, type, contractId, isEdit, isReview).then((res) => {
        // 获取最后一个.的位置
        var index = res.data.fileName.lastIndexOf('.')
        // 获取后缀
        var ext = res.data.fileName.substring(index + 1)
        if (!this.onlyofficeLoaded) {
          this.addonlyofficeHead(res.data)
        }
        this.loadonlyoffice(res.data.onlyofficeModel)
      })
    },
    receiveMessage(event) {
      if (event.data === 'loaded') {
        this.pluginLoaded = true
        this.$emit('pluginLoaded')

        // //如果是只读模式，添加水印
        // if (this.isEdit == 'false') {
        //   this.addWatermark();
        // }
      }
    },
    addWatermark() {
      const data = {
        action: 'addWatermark',
        text: '水印'
      }
      window.frames['frameEditor'].frames[0].postMessage(JSON.stringify(data), '*')
    },
    mergeData(dataList) {
      dataList.forEach(item => {
        item.Tag = 'templateElement' + item.docId
      })

      const data = {
        action: 'mergeContent',
        dataList: dataList
      }
      //console.log(data)
      window.frames['frameEditor'].frames[0].postMessage(JSON.stringify(data), '*')
    },
    forceSave() {
      const data = {
        action: 'forceSave'
      }
      window.frames['frameEditor'].frames[0].postMessage(JSON.stringify(data), '*')
    },
    addContentControl(id, alias) {
      const data = {
        action: 'createContentControl',
        Id: id,
        Tag: 'templateElement' + id,
        Alias: alias
      }
      window.frames['frameEditor'].frames[0].postMessage(JSON.stringify(data), '*')
    },
    selectContentControl(id) {
      const data = {
        action: 'getContentControl',
        Tag: 'templateElement' + id
      }
      window.frames['frameEditor'].frames[0].postMessage(JSON.stringify(data), '*')
    },
    deleteContentControl(id) {
      const data = {
        action: 'deleteContentControl',
        Tag: 'templateElement' + id
      }
      window.frames['frameEditor'].frames[0].postMessage(JSON.stringify(data), '*')
    },
    updateContentControl(id, alias) {
      const data = {
        action: 'updateContentControl',
        Tag: 'templateElement' + id,
        Alias: alias
      }
      window.frames['frameEditor'].frames[0].postMessage(JSON.stringify(data), '*')
    },
    createBlockContentControl(id, alias) {
      const data = {
        action: 'createBlockContentControl',
        Id: id,
        Tag: 'templateElement' + id,
        Alias: alias
      }
      window.frames['frameEditor'].frames[0].postMessage(JSON.stringify(data), '*')
    },
    setContentControlText(id, text) {
      const data = {
        action: 'setContentControlText',
        Tag: 'templateElement' + id,
        Text: text
      }
      window.frames['frameEditor'].frames[0].postMessage(JSON.stringify(data), '*')
    },
    async disableMore() {
      await this.jssdk.ready()
      const app = this.jssdk.Application
      // 更多菜单
      const moreMenus = await app.CommandBars('MoreMenus')
      const TabPrintPreview = await app.CommandBars('TabPrintPreview')
      const HistoryVersion = await app.CommandBars('HistoryVersion')
      // HistoryRecord
      // const HeaderHistoryMenuBtn = await app.CommandBars('TabPrintPreview');
      // const DownloadImg =  await app.CommandBars('TabPrintPreview');
      // 控制更多菜单隐藏
      HistoryVersion.Enabled = false
      moreMenus.Enabled = false
      TabPrintPreview.Enabled = false
    },
    addonlyofficeHead(data) {
      const script = document.createElement('script')
      script.type = 'text/javascript'
      script.src =
        data.onlyofficeDomain + '/web-apps/apps/api/documents/api.js'
      document.getElementsByTagName('head')[0].appendChild(script)

      // 添加消息提醒
      //   this.$notify({
      //     title: "提醒 ：编辑完文本后请关闭该页面",
      //     dangerouslyUseHTMLString: true,
      //     message:
      //       '1.关闭该页面10秒钟后，文档自动保存。<br/>2.多人协同时，<span style="color:red;">所有人</span>都关闭了页面,文档自动保存。</br>3.请在文档保存之后再发起审批或者下载文档。',
      //     duration: 0,
      //   });
    },
    updateCommentUser(data){
      this.commentUser = data;
    },
    loadonlyoffice(fileModel) {
      this.viewFile = true
      // 添加html元素

      this.$nextTick(() => {
        var html =
          ' <div id="viewFile" style="height: calc(100vh - 65px); width: calc(100vw - 0px)"></div>'
        document.getElementById('viewFileContainer').innerHTML = html
        let that = this;
        setTimeout(function() {
          function onRequestUsers(event) {
            docEditor.setUsers({
              c: event.data.c,
              users: that.commentUser
            })
          };
          function onRequestSendNotify(event) {
            console.log(event);
          };
          fileModel.events = {
              onRequestUsers,
              onRequestSendNotify,
            }
          const docEditor = new window.DocsAPI.DocEditor('viewFile', fileModel)
        }, 500)
      })
    }
  }
}
</script>
