package com.tencent.kuiklybase

import com.tencent.kuikly.core.base.DeclarativeBaseView
import com.tencent.kuikly.core.base.ViewContainer

/**
 * KRCameraView 跨端相机预览组件
 *
 * 使用 Kuikly 的 DeclarativeBaseView 模式，将各平台原生相机预览 View
 * 暴露为统一的跨端组件。原生端需注册名为 "KRCameraView" 的渲染视图实现。
 *
 * 使用示例：
 * ```kotlin
 * CameraView {
 *     attr {
 *         cameraFacing(KRCameraViewAttr.FACING_BACK)
 *         resolution(KRCameraViewAttr.RESOLUTION_HIGH)
 *         flashMode(KRCameraViewAttr.FLASH_OFF)
 *         zoom(1.0f)
 *     }
 *     event {
 *         onCameraReady {
 *             // 相机就绪
 *         }
 *         onPhotoCaptured { filePath ->
 *             // 拍照完成，filePath 为照片路径
 *         }
 *         onError { code, desc ->
 *             // 相机错误
 *         }
 *     }
 * }
 * ```
 */
class KRCameraView : DeclarativeBaseView<KRCameraViewAttr, KRCameraViewEvent>() {

    override fun createAttr(): KRCameraViewAttr {
        return KRCameraViewAttr()
    }

    override fun createEvent(): KRCameraViewEvent {
        return KRCameraViewEvent()
    }

    override fun viewName(): String {
        return VIEW_NAME
    }

    // ---- 命令式方法（通过 renderView?.callMethod 调用原生） ----

    /**
     * 拍照
     * 拍照结果通过 onPhotoCaptured 事件回调返回
     */
    fun takePhoto() {
        performTaskWhenRenderViewDidLoad {
            renderView?.callMethod("takePhoto", null)
        }
    }

    /**
     * 带参数拍照
     * @param quality 图片质量 0-100，默认 95
     */
    fun takePhoto(quality: Int) {
        performTaskWhenRenderViewDidLoad {
            renderView?.callMethod("takePhoto", "{\"quality\":$quality}")
        }
    }

    /**
     * 切换前后摄像头
     */
    fun switchCamera() {
        performTaskWhenRenderViewDidLoad {
            renderView?.callMethod("switchCamera", null)
        }
    }

    /**
     * 启动相机预览
     */
    fun startPreview() {
        performTaskWhenRenderViewDidLoad {
            renderView?.callMethod("startPreview", null)
        }
    }

    /**
     * 停止相机预览
     */
    fun stopPreview() {
        performTaskWhenRenderViewDidLoad {
            renderView?.callMethod("stopPreview", null)
        }
    }

    /**
     * 释放相机资源
     */
    fun release() {
        performTaskWhenRenderViewDidLoad {
            renderView?.callMethod("release", null)
        }
    }

    /**
     * 设置缩放倍数
     * @param level 缩放倍数
     */
    fun setZoom(level: Float) {
        performTaskWhenRenderViewDidLoad {
            renderView?.callMethod("setZoom", level.toString())
        }
    }

    /**
     * 设置闪光灯模式
     * @param mode "on", "off", "auto"
     */
    fun setFlashMode(mode: String) {
        performTaskWhenRenderViewDidLoad {
            renderView?.callMethod("setFlashMode", mode)
        }
    }

    companion object {
        /** 组件对应的原生视图名称，各端注册时使用此名称 */
        const val VIEW_NAME = "KRCameraView"
    }
}

/**
 * CameraView DSL 扩展函数
 * 在 ViewContainer 中使用 CameraView { ... } 构建相机预览组件
 */
fun ViewContainer<*, *>.CameraView(init: KRCameraView.() -> Unit) {
    addChild(KRCameraView(), init)
}
