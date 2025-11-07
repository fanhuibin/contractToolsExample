/**
 * OnlyOffice Document Editor API 类型定义
 */

declare global {
  interface Window {
    DocsAPI?: {
      DocEditor: new (containerId: string, config: any) => OnlyOfficeDocEditor
    }
  }
}

export interface OnlyOfficeDocEditor {
  destroyEditor: () => void
  downloadAs: () => void
  requestClose: () => void
  setHistoryData: (data: any) => void
  refreshHistory: (data: any) => void
  setRevisedFile: (file: any) => void
  setMailMergeRecipients: (recipients: any) => void
  showMessage: (message: string) => void
  setFavorite: (favorite: boolean) => void
  setUsers: (users: any[]) => void
  setSharingSettings: (settings: any) => void
  insertImage: (image: any) => void
  registerImagePlugin: (plugin: any) => void
  unregisterImagePlugin: (plugin: any) => void
}

export interface OnlyOfficeConfig {
  type?: string
  documentType?: string
  document?: {
    fileType?: string
    key?: string
    title?: string
    url?: string
    permissions?: {
      comment?: boolean
      copy?: boolean
      download?: boolean
      edit?: boolean
      fillForms?: boolean
      modifyContentControl?: boolean
      modifyFilter?: boolean
      print?: boolean
      review?: boolean
    }
  }
  editorConfig?: {
    mode?: string
    lang?: string
    callbackUrl?: string
    user?: {
      id?: string
      name?: string
    }
    customization?: any
    plugins?: {
      autostart?: string[]
      pluginsData?: any[]
    }
  }
  events?: {
    onReady?: () => void
    onDocumentStateChange?: (event: any) => void
    onError?: (event: any) => void
    onWarning?: (event: any) => void
    onRequestSaveAs?: (event: any) => void
  }
}

export {}

