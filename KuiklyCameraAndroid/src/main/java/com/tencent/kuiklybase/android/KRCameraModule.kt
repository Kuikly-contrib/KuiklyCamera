package com.tencent.kuiklybase.android

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.tencent.kuikly.core.render.android.export.KuiklyRenderBaseModule
import com.tencent.kuikly.core.render.android.export.KuiklyRenderCallback
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Android 端相机 Module
 *
 * 实现相机控制方法（权限申请、拍照等），注册名为 "KRCameraModule"。
 * 在 registerExternalModule 中注册。
 */
class KRCameraModule : KuiklyRenderBaseModule() {

    override fun call(method: String, params: String?, callback: KuiklyRenderCallback?): Any? {
        return when (method) {
            "requestPermission" -> {
                requestPermission(callback)
                null
            }
            "checkPermission" -> {
                checkPermission()
            }
            "takePhoto" -> {
                takePhoto(params, callback)
                null
            }
            "switchCamera" -> {
                // Module 层的 switchCamera 需要与 View 配合使用
                callback?.invoke(mapOf("code" to 0, "message" to "Use view method instead"))
                null
            }
            "startPreview" -> {
                callback?.invoke(mapOf("code" to 0, "message" to "Use view method instead"))
                null
            }
            "stopPreview" -> {
                callback?.invoke(mapOf("code" to 0, "message" to "Use view method instead"))
                null
            }
            "release" -> {
                callback?.invoke(mapOf("code" to 0, "message" to "Use view method instead"))
                null
            }
            "setFlashMode" -> {
                callback?.invoke(mapOf("code" to 0, "message" to "Use view method instead"))
                null
            }
            "setZoom" -> {
                callback?.invoke(mapOf("code" to 0, "message" to "Use view method instead"))
                null
            }
            else -> {
                callback?.invoke(mapOf("code" to -1, "message" to "Unknown method: $method"))
                null
            }
        }
    }

    /**
     * 请求相机权限
     */
    private fun requestPermission(callback: KuiklyRenderCallback?) {
        val ctx = context ?: run {
            callback?.invoke(mapOf("granted" to false, "message" to "Context is null"))
            return
        }

        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            callback?.invoke(mapOf("granted" to true))
            return
        }

        // 通过 Activity 请求权限
        val act = activity
        if (act != null) {
            ActivityCompat.requestPermissions(
                act,
                arrayOf(Manifest.permission.CAMERA),
                PERMISSION_REQUEST_CODE
            )
            // 注意：实际的权限结果需要在 Activity 的 onRequestPermissionsResult 中处理
            // 这里先保存 callback，等待结果回调
            pendingPermissionCallback = callback
        } else {
            callback?.invoke(mapOf("granted" to false, "message" to "Activity is null"))
        }
    }

    /**
     * 检查相机权限状态
     * @return "granted", "denied", "not_determined"
     */
    private fun checkPermission(): String {
        val ctx = context ?: return "denied"
        return when {
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED -> "granted"
            else -> "denied"
        }
    }

    /**
     * 通过 Module 拍照（独立于 View 的拍照能力）
     */
    private fun takePhoto(params: String?, callback: KuiklyRenderCallback?) {
        val ctx = context ?: run {
            callback?.invoke(mapOf("code" to -1, "message" to "Context is null"))
            return
        }

        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            callback?.invoke(mapOf("code" to -3, "message" to "Camera permission not granted"))
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

        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
        cameraProviderFuture.addListener({
            try {
                val provider = cameraProviderFuture.get()
                val imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                provider.unbindAll()
                val lifecycleOwner = activity as? androidx.lifecycle.LifecycleOwner
                if (lifecycleOwner == null) {
                    callback?.invoke(mapOf("code" to -1, "message" to "LifecycleOwner not found"))
                    return@addListener
                }

                provider.bindToLifecycle(lifecycleOwner, cameraSelector, imageCapture)

                // 创建输出文件
                val photoDir = File(ctx.cacheDir, "kuikly_camera_photos")
                if (!photoDir.exists()) {
                    photoDir.mkdirs()
                }
                val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                    .format(System.currentTimeMillis())
                val photoFile = File(photoDir, "IMG_${timeStamp}.jpg")

                val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

                imageCapture.takePicture(
                    outputOptions,
                    ContextCompat.getMainExecutor(ctx),
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                            callback?.invoke(
                                mapOf(
                                    "code" to 0,
                                    "filePath" to photoFile.absolutePath
                                )
                            )
                            provider.unbindAll()
                        }

                        override fun onError(exception: ImageCaptureException) {
                            Log.e(TAG, "Module takePhoto failed: ${exception.message}", exception)
                            callback?.invoke(
                                mapOf(
                                    "code" to exception.imageCaptureError,
                                    "message" to (exception.message ?: "Photo capture failed")
                                )
                            )
                            provider.unbindAll()
                        }
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Module takePhoto error: ${e.message}", e)
                callback?.invoke(mapOf("code" to -1, "message" to (e.message ?: "Unknown error")))
            }
        }, ContextCompat.getMainExecutor(ctx))
    }

    companion object {
        private const val TAG = "KRCameraModule"
        const val MODULE_NAME = "KRCameraModule"
        const val PERMISSION_REQUEST_CODE = 10086

        // 权限请求回调（静态持有，供 Activity 回传权限结果）
        var pendingPermissionCallback: KuiklyRenderCallback? = null

        /**
         * 在 Activity 的 onRequestPermissionsResult 中调用此方法
         */
        fun handlePermissionResult(requestCode: Int, grantResults: IntArray) {
            if (requestCode == PERMISSION_REQUEST_CODE) {
                val granted = grantResults.isNotEmpty() &&
                        grantResults[0] == PackageManager.PERMISSION_GRANTED
                pendingPermissionCallback?.invoke(mapOf("granted" to granted))
                pendingPermissionCallback = null
            }
        }
    }
}
