/**
 * GPU OCR Canvas 图片管理相关函数
 */

/**
 * 图片管理器类
 */
export class ImageManager {
  private imageCache = new Map<string, HTMLImageElement>()
  private loadingImages = new Set<string>()

  /**
   * 加载图片
   * @param url 图片URL
   * @returns Promise<HTMLImageElement>
   */
  loadImage(url: string): Promise<HTMLImageElement> {
    return new Promise((resolve, reject) => {
      // 如果已缓存，直接返回
      if (this.imageCache.has(url)) {
        resolve(this.imageCache.get(url)!)
        return
      }

      // 如果正在加载，等待加载完成
      if (this.loadingImages.has(url)) {
        const checkLoaded = () => {
          if (this.imageCache.has(url)) {
            resolve(this.imageCache.get(url)!)
          } else if (this.loadingImages.has(url)) {
            setTimeout(checkLoaded, 100)
          } else {
            reject(new Error('Image loading failed'))
          }
        }
        checkLoaded()
        return
      }

      // 开始加载图片
      this.loadingImages.add(url)
      
      // 创建隐藏的img标签并添加到DOM中
      const img = document.createElement('img')
      img.style.display = 'none'
      img.style.position = 'absolute'
      img.style.left = '-9999px'
      img.style.top = '-9999px'
      
      // 不设置crossOrigin，避免file://协议的CORS问题
      // img.crossOrigin = 'anonymous'  // 注释掉这行
      
      img.onload = () => {
        this.imageCache.set(url, img)
        this.loadingImages.delete(url)
        resolve(img)
      }
      
      img.onerror = () => {
        this.loadingImages.delete(url)
        // 从DOM中移除失败的图片元素
        if (img.parentNode) {
          img.parentNode.removeChild(img)
        }
        reject(new Error(`Failed to load image: ${url}`))
      }
      
      // 先添加到DOM，再设置src
      document.body.appendChild(img)
      img.src = url
    })
  }

  /**
   * 清除缓存
   */
  clearCache(): void {
    this.imageCache.clear()
    this.loadingImages.clear()
  }

  /**
   * 获取缓存统计信息
   */
  getCacheStats(): { cached: number; loading: number } {
    return {
      cached: this.imageCache.size,
      loading: this.loadingImages.size
    }
  }
}

/**
 * 单例图片管理器
 */
export const imageManager = new ImageManager()
