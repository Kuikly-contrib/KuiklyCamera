package com.tencent.kuiklybase.android

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.tencent.kuikly.core.render.android.export.IKuiklyRenderViewExport
import com.tencent.kuikly.core.render.android.export.KuiklyRenderCallback
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Android 端相机预览 View
 *
 * 基于 CameraX 实现相机预览、拍照、切换摄像头等功能。
 * 注册名为 "KRCameraView"，在 registerExternalRenderView 中注册。
 */
class KRCameraView(context: Context) : FrameLayout(context), IKuiklyRenderViewExport {

    private var previewView: PreviewView? = null
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var imageCapture: ImageCapture? = null
    private var preview: Preview? = null
    private var cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    // 当前相机状态
    private var currentFacing = CameraSelector.LENS_FACING_BACK
    private var currentFlashMode = ImageCapture.FLASH_MODE_OFF
    private var currentZoom = 1.0f
    private var currentResolution = "medium"
    private var autoStart = true
    private var isCameraStarted = false

    // 事件回调
    private var onCameraReadyCallback: KuiklyRenderCallback? = null
    private var onErrorCallback: KuiklyRenderCallback? = null
    private var onPhotoCapturedCallback: KuiklyRenderCallback? = null

    init {
        previewView = PreviewView(context).apply {
            layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT
            )
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
        addView(previewView)
    }

    override fun setProp(propKey: String, propValue: Any): Boolean {
        return when (propKey) {
            "cameraFacing" -> {
                val facing = propValue.toString()
                currentFacing = if (facing == "front") {
                    CameraSelector.LENS_FACING_FRONT
                } else {
                    CameraSelector.LENS_FACING_BACK
                }
                if (isCameraStarted) {
                    startCamera()
                }
                true
            }
            "flashMode" -> {
                currentFlashMode = when (propValue.toString()) {
                    "on" -> ImageCapture.FLASH_MODE_ON
                    "auto" -> ImageCapture.FLASH_MODE_AUTO
                    else -> ImageCapture.FLASH_MODE_OFF
                }
                imageCapture?.flashMode = currentFlashMode
                true
            }
            "resolution" -> {
                currentResolution = propValue.toString()
                if (isCameraStarted) {
                    startCamera()
                }
                true
            }
            "zoom" -> {
                currentZoom = propValue.toString().toFloatOrNull() ?: 1.0f
                camera?.cameraControl?.setZoomRatio(currentZoom)
                true
            }
            "autoStart" -> {
                autoStart = propValue.toString() == "true"
                true
            }
            "onCameraReady" -> {
                onCameraReadyCallback = propValue as? KuiklyRenderCallback
                true
            }
            "onError" -> {
                onErrorCallback = propValue as? KuiklyRenderCallback
                true
            }
            "onPhotoCaptured" -> {
                onPhotoCapturedCallback = propValue as? KuiklyRenderCallback
                true
            }
            else -> false
        }
    }

    override fun call(method: String, params: String?, callback: KuiklyRenderCallback?): Any? {
        return when (method) {
            "takePhoto" -> {
                takePhoto(params, callback)
                null
            }
            "switchCamera" -> {
                switchCamera()
                null
            }
            "startPreview" -> {
                startCamera()
                null
            }
            "stopPreview" -> {
                stopCamera()
                null
            }
            "release" -> {
                releaseCamera()
                null
            }
            "setZoom" -> {
                val zoom = params?.toFloatOrNull() ?: 1.0f
                currentZoom = zoom
                camera?.cameraControl?.setZoomRatio(zoom)
                null
            }
            "setFlashMode" -> {
                currentFlashMode = when (params) {
                    "on" -> ImageCapture.FLASH_MODE_ON
                    "auto" -> ImageCapture.FLASH_MODE_AUTO
                    else -> ImageCapture.FLASH_MODE_OFF
                }
                imageCapture?.flashMode = currentFlashMode
                null
            }
            else -> null
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (autoStart && hasCameraPermission()) {
            startCamera()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        releaseCamera()
    }

    // ---- IKuiklyRenderViewExport 实现 ----
    // kuiklyRenderContext 使用接口默认实现（从 view.context 中获取）

    override fun view(): View = this

    override fun onDestroy() {
        releaseCamera()
    }

    // ---- 内部实现 ----

    private fun startCamera() {
        val ctx = context ?: return
        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
        cameraProviderFuture.addListener({
            try {
                val provider = cameraProviderFuture.get()
                cameraProvider = provider

                // 预览
                preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView?.surfaceProvider)
                }

                // 拍照
                imageCapture = ImageCapture.Builder()
                    .setFlashMode(currentFlashMode)
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()

                // 摄像头选择
                val cameraSelector = CameraSelector.Builder()
                    .requireLensFacing(currentFacing)
                    .build()

                // 解绑所有用例后重新绑定
                provider.unbindAll()

                val lifecycleOwner = getLifecycleOwner()
                if (lifecycleOwner != null) {
                    camera = provider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageCapture
                    )
                    // 设置缩放
                    camera?.cameraControl?.setZoomRatio(currentZoom)
                    isCameraStarted = true

                    // 触发 onCameraReady 事件
                    onCameraReadyCallback?.invoke(mapOf<String, Any>())
                }
            } catch (e: Exception) {
                Log.e(TAG, "Camera start failed: ${e.message}", e)
                onErrorCallback?.invoke(mapOf(
                    "errorCode" to -1,
                    "description" to (e.message ?: "Camera start failed")
                ))
            }
        }, ContextCompat.getMainExecutor(ctx))
    }

    private fun stopCamera() {
        cameraProvider?.unbindAll()
        isCameraStarted = false
    }

    private fun releaseCamera() {
        stopCamera()
        cameraExecutor.shutdown()
    }

    private fun switchCamera() {
        currentFacing = if (currentFacing == CameraSelector.LENS_FACING_BACK) {
            CameraSelector.LENS_FACING_FRONT
        } else {
            CameraSelector.LENS_FACING_BACK
        }
        if (isCameraStarted) {
            startCamera()
        }
    }

    @SuppressLint("SimpleDateFormat")
    private fun takePhoto(params: String?, callback: KuiklyRenderCallback?) {
        val imageCapture = imageCapture ?: run {
            onErrorCallback?.invoke(mapOf(
                "errorCode" to -2,
                "description" to "ImageCapture not initialized"
            ))
            callback?.invoke(mapOf("code" to -2, "message" to "ImageCapture not initialized"))
            return
        }

        // 解析参数
        val quality = if (params != null) {
            try {
                JSONObject(params).optInt("quality", 95)
            } catch (e: Exception) {
                95
            }
        } else {
            95
        }

        // 创建输出文件
        val photoDir = File(context.cacheDir, "kuikly_camera_photos")
        if (!photoDir.exists()) {
            photoDir.mkdirs()
        }
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            .format(System.currentTimeMillis())
        val photoFile = File(photoDir, "IMG_${timeStamp}.jpg")

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val filePath = photoFile.absolutePath
                    // 触发 onPhotoCaptured 事件
                    onPhotoCapturedCallback?.invoke(mapOf("filePath" to filePath))
                    // 同时回调
                    callback?.invoke(
                        mapOf(
                            "code" to 0,
                            "filePath" to filePath
                        )
                    )
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exception.message}", exception)
                    onErrorCallback?.invoke(mapOf(
                        "errorCode" to exception.imageCaptureError,
                        "description" to (exception.message ?: "Photo capture failed")
                    ))
                    callback?.invoke(
                        mapOf(
                            "code" to exception.imageCaptureError,
                            "message" to (exception.message ?: "Photo capture failed")
                        )
                    )
                }
            }
        )
    }

    private fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun getLifecycleOwner(): LifecycleOwner? {
        var ctx = context
        while (ctx != null) {
            if (ctx is LifecycleOwner) {
                return ctx
            }
            ctx = if (ctx is android.content.ContextWrapper) {
                ctx.baseContext
            } else {
                null
            }
        }
        return null
    }

    companion object {
        private const val TAG = "KRCameraView"
        const val VIEW_NAME = "KRCameraView"
    }
}
