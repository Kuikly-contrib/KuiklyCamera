# KuiklyCamera

基于 [Kuikly](https://github.com/Tencent-TDS/KuiklyUI) 跨端框架的 **相机组件库**，采用 Kotlin Multiplatform (KMP) 架构，提供 Android、iOS、鸿蒙（HarmonyOS）三端统一的相机预览与拍照能力。



---

## ✨ 特性

-  **相机预览** — 实时预览，支持自动启动
-  **拍照** — 支持自定义质量参数
-  **前后摄像头切换**
-  **闪光灯控制** — 关闭 / 开启 / 自动
-  **缩放控制** — 支持动态调节缩放倍数
-  **分辨率设置** — 高 / 中 / 低三档
-  **权限管理** — 请求与检查相机权限
-  **跨平台** — Android (CameraX) / iOS (AVFoundation) / 鸿蒙 (ArkTS Camera Kit) 原生实现


##  接入指引

### 第一步：添加 Maven 仓库

在项目根目录的 `settings.gradle.kts` 中添加 Maven 仓库：

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://mirrors.tencent.com/nexus/repository/maven-tencent/") }
    }
}
```

### 第二步：添加跨端依赖（KMP commonMain）

在你的 KMP shared 模块的 `build.gradle.kts` 中，为 `commonMain` 添加 KuiklyCamera 跨端核心依赖：

```kotlin
kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("com.tencent.kuiklybase:KuiklyCamera:1.0.0-2.0.21")
            }
        }
    }
}
```

> **版本号规则**：`{基础版本}-{Kotlin版本}`
> - 标准平台（Android / iOS）：`1.0.0-2.0.21`
> - 鸿蒙平台：`1.0.0-2.0.21-KBA-010`

### 第三步：各平台原生实现接入

#### Android

在你的 Android App 模块的 `build.gradle.kts` 中添加 Android 原生实现依赖：

```kotlin
dependencies {
    // KuiklyCamera Android 原生实现（基于 CameraX）
    implementation("com.tencent.kuiklybase:KuiklyCameraAndroid:1.0.0-2.0.21")
}
```

在 `AndroidManifest.xml` 中添加相机权限：

```xml
<uses-permission android:name="android.permission.CAMERA" />
<uses-feature android:name="android.hardware.camera" android:required="false" />
<uses-feature android:name="android.hardware.camera.autofocus" android:required="false" />
```

#### iOS

**通过 CocoaPods 引入**

在你的 iOS 项目 `Podfile` 中添加:

```ruby
pod 'KuiklyCameraIOS', :git => 'https://github.com/Kuikly-contrib/KuiklyCamera.git', :branch => 'main'
```

然后执行:
```bash
pod install
```

> 注意：确保你的 `Podfile` 中已引入：
> ```ruby
> pod 'OpenKuiklyIOSRender', '~> 2.7.0'
> ```

在 `Info.plist` 中添加相机权限描述：

```xml
<key>NSCameraUsageDescription</key>
<string>需要访问相机以进行拍照</string>
```

#### 鸿蒙（HarmonyOS）

在你的鸿蒙项目 KMP shared 模块的 `build.ohos.gradle.kts` 中，使用鸿蒙专用版本：

```kotlin
kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("com.tencent.kuiklybase:KuiklyCamera:1.0.0-2.0.21-KBA-010")
            }
        }
    }
}
```

**鸿蒙原生实现**

鸿蒙端有独立的原生实现（基于 ArkTS Camera Kit），位于：

```
ohosApp/entry/src/main/ets/kuikly/
├── components/
│   └── KRCameraView.ets       # 相机视图组件
└── modules/
    └── KRCameraModule.ets     # 相机模块
```

使用时需要将这些文件复制到你的鸿蒙项目对应位置，或者直接使用本仓库的 `ohosApp` 作为参考。

> 注意：鸿蒙端使用 ArkTS 开发，文件格式为 `.ets`，不同于 Android/iOS 的 Kotlin/Objective-C。

### 源码依赖方式（可选）

如果不使用 Maven 远程依赖，也可以将源码模块直接引入项目：

1. 将 `KuiklyCamera/`、`KuiklyCameraAndroid/`、`KuiklyCameraIOS/` 目录复制到项目中

2. 在 `settings.gradle.kts` 中注册模块：

```kotlin
include(":KuiklyCamera")
include(":KuiklyCameraAndroid")
```

3. 在 `buildSrc` 中配置版本号：

```kotlin
object Version {
    private const val KUIKLY_VERSION = "2.7.0"
    private const val KOTLIN_VERSION = "2.0.21"
    fun getKuiklyVersion(): String = "$KUIKLY_VERSION-$KOTLIN_VERSION"
}
```

4. 添加模块依赖：

```kotlin
// shared 模块的 build.gradle.kts (commonMain)
implementation(project(":KuiklyCamera"))

// Android App 模块的 build.gradle.kts
implementation(project(":KuiklyCameraAndroid"))
```

---

##  使用指引

### 基本用法 — 声明式 CameraView

```kotlin
import com.tencent.kuiklybase.CameraView
import com.tencent.kuiklybase.KRCameraViewAttr

// 在 Kuikly 页面的 body 中使用
CameraView {
    attr {
        flex(1f)                                          // 布局：撑满剩余空间
        cameraFacing(KRCameraViewAttr.FACING_BACK)        // 使用后置摄像头
        flashMode(KRCameraViewAttr.FLASH_OFF)             // 关闭闪光灯
        resolution(KRCameraViewAttr.RESOLUTION_HIGH)      // 高分辨率
        zoom(1.0f)                                        // 默认缩放
        autoStart(true)                                   // 自动启动预览
    }
    event {
        onCameraReady {
            // 相机初始化完成
        }
        onError { errorCode, description ->
            // 处理错误
        }
        onPhotoCaptured { filePath ->
            // 拍照完成，filePath 为照片本地路径
        }
        onPermissionResult { granted ->
            // 权限状态变化
        }
    }
}
```

### 完整示例 — 带控制按钮的相机页面

```kotlin
import com.tencent.kuiklybase.CameraView
import com.tencent.kuiklybase.KRCameraModule
import com.tencent.kuiklybase.KRCameraView
import com.tencent.kuiklybase.KRCameraViewAttr

class CameraPage : BasePager() {

    // 响应式状态
    val currentFacing by createReactiveField { KRCameraViewAttr.FACING_BACK }
    val currentFlash by createReactiveField { KRCameraViewAttr.FLASH_OFF }
    val currentZoom by createReactiveField { 1.0f }
    val cameraReady by createReactiveField { false }
    val statusText by createReactiveField { "初始化中..." }
    val photoPath by createReactiveField { "" }

    // 相机 View 引用（用于命令式调用）
    var cameraViewRef: KRCameraView? = null

    // 相机 Module（用于权限管理等）
    val cameraModule by lazy { acquireModule<KRCameraModule>() }

    override fun body() {
        // 顶部标题栏
        View {
            attr {
                width(MATCH_PARENT)
                height(56f)
                backgroundColor(Color.BLACK)
                allCenter()
            }
            Text {
                attr {
                    text("相机")
                    fontSize(18f)
                    color(Color.WHITE)
                }
            }
        }

        // 相机预览区域
        CameraView {
            ref { cameraViewRef = it }
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

        // 底部控制栏
        View {
            attr {
                width(MATCH_PARENT)
                height(120f)
                flexDirection(FlexDirection.ROW)
                justifyContent(JustifyContent.SPACE_AROUND)
                alignItems(AlignItems.CENTER)
                backgroundColor(Color.BLACK)
            }

            // 切换摄像头按钮
            Button {
                attr { text("切换") }
                event {
                    click {
                        cameraViewRef?.switchCamera()
                    }
                }
            }

            // 拍照按钮
            Button {
                attr { text("拍照") }
                event {
                    click {
                        cameraViewRef?.takePhoto()
                    }
                }
            }

            // 闪光灯按钮
            Button {
                attr { text("闪光灯") }
                event {
                    click {
                        val newMode = when (ctx.currentFlash) {
                            KRCameraViewAttr.FLASH_OFF -> KRCameraViewAttr.FLASH_ON
                            KRCameraViewAttr.FLASH_ON -> KRCameraViewAttr.FLASH_AUTO
                            else -> KRCameraViewAttr.FLASH_OFF
                        }
                        ctx.currentFlash = newMode
                    }
                }
            }
        }
    }
}
```

### 自定义预览形状

#### 圆角矩形预览

```kotlin
CameraView {
    attr {
        flex(1f)
        marginLeft(16f)
        marginRight(16f)
        marginTop(8f)
        marginBottom(8f)
        borderRadius(16f)
        overflow(true)           // 裁剪圆角外的内容
        cameraFacing(KRCameraViewAttr.FACING_BACK)
        autoStart(true)
    }
}
```

#### 圆形预览（如头像拍摄场景）

```kotlin
View {
    attr {
        flex(1f)
        allCenter()
    }
    CameraView {
        attr {
            size(300f, 300f)
            borderRadius(150f)   // 半径 = 宽高的一半
            overflow(true)       // 裁剪为圆形
            cameraFacing(KRCameraViewAttr.FACING_FRONT)
            autoStart(true)
        }
    }
}
```

---

##  API 参考

### KRCameraView — 视图组件

#### 属性（Attr）

| 属性 | 参数 | 说明 | 默认值 |
|------|------|------|--------|
| `cameraFacing(facing)` | `"back"` / `"front"` | 摄像头朝向 | `"back"` |
| `flashMode(mode)` | `"off"` / `"on"` / `"auto"` | 闪光灯模式 | `"off"` |
| `resolution(res)` | `"high"` / `"medium"` / `"low"` | 预览分辨率 | `"medium"` |
| `zoom(level)` | `Float`（1.0 起） | 缩放倍数 | `1.0f` |
| `autoStart(auto)` | `Boolean` | 是否自动启动预览 | `true` |

**常量定义**（`KRCameraViewAttr`）：

```kotlin
// 摄像头朝向
KRCameraViewAttr.FACING_BACK      // "back"
KRCameraViewAttr.FACING_FRONT     // "front"

// 闪光灯模式
KRCameraViewAttr.FLASH_OFF        // "off"
KRCameraViewAttr.FLASH_ON         // "on"
KRCameraViewAttr.FLASH_AUTO       // "auto"

// 分辨率
KRCameraViewAttr.RESOLUTION_HIGH     // "high"
KRCameraViewAttr.RESOLUTION_MEDIUM   // "medium"
KRCameraViewAttr.RESOLUTION_LOW      // "low"
```

#### 事件（Event）

| 事件 | 回调参数 | 说明 |
|------|---------|------|
| `onCameraReady { }` | 无 | 相机初始化完成 |
| `onError { errorCode, description -> }` | `Int`, `String` | 相机发生错误 |
| `onPhotoCaptured { filePath -> }` | `String` | 拍照完成，返回照片本地路径 |
| `onPermissionResult { granted -> }` | `Boolean` | 相机权限状态变化 |

#### 方法（Method）— 通过 ref 引用调用

| 方法 | 参数 | 说明 |
|------|------|------|
| `takePhoto()` | 无 | 拍照（默认质量） |
| `takePhoto(quality)` | `Int`（0-100） | 指定质量拍照 |
| `switchCamera()` | 无 | 切换前后摄像头 |
| `startPreview()` | 无 | 启动相机预览 |
| `stopPreview()` | 无 | 停止相机预览 |
| `release()` | 无 | 释放相机资源 |
| `setZoom(level)` | `Float` | 动态设置缩放倍数 |
| `setFlashMode(mode)` | `String` | 动态设置闪光灯模式 |

### KRCameraModule — 功能模块

通过 `acquireModule<KRCameraModule>()` 获取实例，适合在逻辑层独立调用。

| 方法 | 参数 | 类型 | 说明 |
|------|------|------|------|
| `requestPermission(callback)` | `CallbackFn` | 异步 | 请求相机权限 |
| `checkPermission()` | 无 | 同步 | 返回 `"granted"` / `"denied"` / `"not_determined"` |
| `takePhoto(callback)` | `CallbackFn` | 异步 | 拍照 |
| `takePhoto(quality, callback)` | `Int`, `CallbackFn` | 异步 | 指定质量拍照 |
| `switchCamera(callback?)` | `CallbackFn?` | 异步 | 切换摄像头 |
| `startPreview()` | 无 | 调用 | 启动预览 |
| `stopPreview()` | 无 | 调用 | 停止预览 |
| `releaseCamera()` | 无 | 调用 | 释放相机资源 |
| `setFlashMode(mode)` | `String` | 调用 | 设置闪光灯模式 |
| `setZoom(level)` | `Float` | 调用 | 设置缩放倍数 |

---


##  License

MIT License
