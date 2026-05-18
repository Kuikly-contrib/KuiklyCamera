package com.kuikly.kuiklycamera

import com.tencent.kuiklybase.CameraView
import com.tencent.kuiklybase.KRCameraModule
import com.tencent.kuiklybase.KRCameraView
import com.tencent.kuiklybase.KRCameraViewAttr
import com.kuikly.kuiklycamera.base.BasePager
import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.*
import com.tencent.kuikly.core.module.Module
import com.tencent.kuikly.core.module.RouterModule
import com.tencent.kuikly.core.reactive.handler.*
import com.tencent.kuikly.core.views.*
import com.tencent.kuikly.core.views.compose.Button

/**
 * 相机 Demo 测试页面
 *
 * 展示 KRCameraView 组件和 KRCameraModule 的完整使用方式，包括：
 * - 相机预览
 * - 拍照
 * - 切换前后摄像头
 * - 闪光灯控制
 * - 缩放控制
 * - 权限管理
 *
 * 在路由中使用 "camera" 跳转到此页面
 */
@Page("router", supportInLocal = true)
internal class CameraPage : BasePager() {

    // 状态管理
    private var cameraReady: Boolean by observable(false)
    private var currentFacing: String by observable(KRCameraViewAttr.FACING_BACK)
    private var currentFlash: String by observable(KRCameraViewAttr.FLASH_OFF)
    private var currentZoom: Float by observable(1.0f)
    private var statusText: String by observable("等待相机初始化...")
    private var photoPath: String by observable("")

    // 相机 View 引用
    private lateinit var cameraViewRef: ViewRef<KRCameraView>

    // 相机 Module
    private val cameraModule: KRCameraModule
        get() = acquireModule(KRCameraModule.MODULE_NAME)

    override fun createExternalModules(): Map<String, Module>? {
        val modules = super.createExternalModules()?.toMutableMap() ?: hashMapOf()
        modules[KRCameraModule.MODULE_NAME] = KRCameraModule()
        return modules
    }

    override fun created() {
        super.created()
        // 进入页面主动申请相机权限，避免 Android 端因无运行时权限导致黑屏
        cameraModule.requestPermission { result ->
            val granted = result.toString().contains("true")
            if (!granted) {
                statusText = "相机权限未授权，请在设置中开启"
            }
        }
    }

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr {
                backgroundColor(Color.BLACK)
            }

            // 顶部状态栏占位
            View {
                attr {
                    height(pagerData.statusBarHeight)
                    backgroundColor(Color(0xCC000000))
                }
            }

            // 顶部导航栏
            View {
                attr {
                    height(44f)
                    flexDirectionRow()
                    alignItemsCenter()
                    backgroundColor(Color(0xCC000000))
                    paddingLeft(12f)
                    paddingRight(12f)
                }

                // 返回按钮
                View {
                    attr {
                        size(30f, 30f)
                        allCenter()
                    }
                    Text {
                        attr {
                            text("←")
                            fontSize(20f)
                            color(Color.WHITE)
                        }
                    }
                    event {
                        click {
                            ctx.acquireModule<RouterModule>(RouterModule.MODULE_NAME).closePage()
                        }
                    }
                }

                // 标题
                Text {
                    attr {
                        flex(1f)
                        text("相机")
                        fontSize(17f)
                        fontWeightSemisolid()
                        color(Color.WHITE)
                        textAlignCenter()
                    }
                }

                // 闪光灯按钮
                View {
                    attr {
                        size(30f, 30f)
                        allCenter()
                    }
                    Text {
                        attr {
                            text(ctx.getFlashIcon())
                            fontSize(18f)
                            color(Color.WHITE)
                        }
                    }
                    event {
                        click {
                            ctx.toggleFlash()
                        }
                    }
                }
            }

            // 相机预览区域
            CameraView {
                ref {
                    ctx.cameraViewRef = it
                }
                attr {
                    flex(1f)
                    cameraFacing(ctx.currentFacing)
                    flashMode(ctx.currentFlash)
                    resolution(KRCameraViewAttr.RESOLUTION_HIGH)
                    zoom(ctx.currentZoom)
                    autoStart(true)
                }
                event {
                    onCameraReady {
                        ctx.cameraReady = true
                        ctx.statusText = "相机就绪"
                    }
                    onError { errorCode, description ->
                        ctx.statusText = "错误[$errorCode]: $description"
                    }
                    onPhotoCaptured { filePath ->
                        ctx.photoPath = filePath
                        ctx.statusText = "拍照完成"
                    }
                }
            }

            // 状态信息栏
            View {
                attr {
                    height(30f)
                    backgroundColor(Color(0xCC000000))
                    allCenter()
                }
                Text {
                    attr {
                        text(ctx.statusText)
                        fontSize(12f)
                        color(Color(0xAAFFFFFF))
                    }
                }
            }

            // 底部控制区域
            View {
                attr {
                    height(120f)
                    backgroundColor(Color(0xCC000000))
                    flexDirectionRow()
                    alignItemsCenter()
                    justifyContentSpaceAround()
                    paddingBottom(pagerData.safeAreaInsets.bottom)
                }

                // 缩小按钮
                View {
                    attr {
                        size(50f, 50f)
                        allCenter()
                    }
                    Text {
                        attr {
                            text("−")
                            fontSize(28f)
                            color(Color.WHITE)
                        }
                    }
                    event {
                        click {
                            ctx.zoomOut()
                        }
                    }
                }

                // 切换摄像头按钮
                View {
                    attr {
                        size(50f, 50f)
                        borderRadius(25f)
                        backgroundColor(Color(0x33FFFFFF))
                        allCenter()
                    }
                    Text {
                        attr {
                            text("⟲")
                            fontSize(24f)
                            color(Color.WHITE)
                        }
                    }
                    event {
                        click {
                            ctx.switchCamera()
                        }
                    }
                }

                // 拍照按钮
                View {
                    attr {
                        size(70f, 70f)
                        borderRadius(35f)
                        backgroundColor(Color.WHITE)
                        allCenter()
                    }
                    View {
                        attr {
                            size(62f, 62f)
                            borderRadius(31f)
                            border(Border(lineWidth = 3f, lineStyle = BorderStyle.SOLID, color = Color.BLACK))
                            backgroundColor(Color.WHITE)
                        }
                    }
                    event {
                        click {
                            ctx.takePhoto()
                        }
                    }
                }

                // 权限检查按钮
                View {
                    attr {
                        size(50f, 50f)
                        borderRadius(25f)
                        backgroundColor(Color(0x33FFFFFF))
                        allCenter()
                    }
                    Text {
                        attr {
                            text("⚙")
                            fontSize(24f)
                            color(Color.WHITE)
                        }
                    }
                    event {
                        click {
                            ctx.requestPermission()
                        }
                    }
                }

                // 放大按钮
                View {
                    attr {
                        size(50f, 50f)
                        allCenter()
                    }
                    Text {
                        attr {
                            text("+")
                            fontSize(28f)
                            color(Color.WHITE)
                        }
                    }
                    event {
                        click {
                            ctx.zoomIn()
                        }
                    }
                }
            }
        }
    }

    // ---- 操作方法 ----

    private fun takePhoto() {
        if (!cameraReady) {
            statusText = "相机未就绪，无法拍照"
            return
        }
        statusText = "拍照中..."
        cameraViewRef.view?.takePhoto()
    }

    private fun switchCamera() {
        currentFacing = if (currentFacing == KRCameraViewAttr.FACING_BACK) {
            KRCameraViewAttr.FACING_FRONT
        } else {
            KRCameraViewAttr.FACING_BACK
        }
        statusText = if (currentFacing == KRCameraViewAttr.FACING_FRONT) "前置摄像头" else "后置摄像头"
    }

    private fun toggleFlash() {
        currentFlash = when (currentFlash) {
            KRCameraViewAttr.FLASH_OFF -> {
                statusText = "闪光灯: 开启"
                KRCameraViewAttr.FLASH_ON
            }
            KRCameraViewAttr.FLASH_ON -> {
                statusText = "闪光灯: 自动"
                KRCameraViewAttr.FLASH_AUTO
            }
            else -> {
                statusText = "闪光灯: 关闭"
                KRCameraViewAttr.FLASH_OFF
            }
        }
    }

    private fun getFlashIcon(): String {
        return when (currentFlash) {
            KRCameraViewAttr.FLASH_ON -> "⚡"
            KRCameraViewAttr.FLASH_AUTO -> "⚡A"
            else -> "⚡✕"
        }
    }

    private fun zoomIn() {
        currentZoom = (currentZoom + 0.5f).coerceAtMost(10.0f)
        statusText = "缩放: ${currentZoom}x"
    }

    private fun zoomOut() {
        currentZoom = (currentZoom - 0.5f).coerceAtLeast(1.0f)
        statusText = "缩放: ${currentZoom}x"
    }

    private fun requestPermission() {
        cameraModule.requestPermission { result ->
            val granted = result.toString().contains("true")
            statusText = if (granted) "相机权限已授权" else "相机权限未授权"
        }
    }
}
