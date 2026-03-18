package com.tencent.kuiklybase

import com.tencent.kuikly.core.module.CallbackFn
import com.tencent.kuikly.core.module.Module
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject

/**
 * KRCameraModule 跨端相机控制模块
 *
 * 负责相机控制逻辑（拍照、权限申请、闪光灯控制等），
 * 通过 Kuikly Module Bridge 机制实现跨端到原生的方法调用。
 *
 * 使用示例：
 * ```kotlin
 * val cameraModule = acquireModule<KRCameraModule>(KRCameraModule.MODULE_NAME)
 * cameraModule.requestPermission { granted ->
 *     if (granted) {
 *         cameraModule.takePhoto { result ->
 *             // 处理拍照结果
 *         }
 *     }
 * }
 * ```
 */
class KRCameraModule : Module() {

    override fun moduleName(): String {
        return MODULE_NAME
    }

    // ---- 权限相关 ----

    /**
     * 请求相机权限
     * @param callback 回调，返回 JSONObject 包含 "granted": true/false
     */
    fun requestPermission(callback: CallbackFn) {
        callNativeMethod(METHOD_REQUEST_PERMISSION, null, callback)
    }

    /**
     * 检查相机权限状态
     * @return 权限状态字符串："granted", "denied", "not_determined"
     */
    fun checkPermission(): String {
        return syncCallNativeMethod(METHOD_CHECK_PERMISSION, null, null)
    }

    // ---- 拍照相关 ----

    /**
     * 拍照
     * @param callback 回调，返回 JSONObject 包含 "filePath": 照片路径
     */
    fun takePhoto(callback: CallbackFn) {
        callNativeMethod(METHOD_TAKE_PHOTO, null, callback)
    }

    /**
     * 带参数拍照
     * @param quality 图片质量 0-100
     * @param callback 回调，返回 JSONObject 包含 "filePath": 照片路径
     */
    fun takePhoto(quality: Int, callback: CallbackFn) {
        val params = JSONObject().apply {
            put("quality", quality)
        }
        callNativeMethod(METHOD_TAKE_PHOTO, params, callback)
    }

    // ---- 相机控制 ----

    /**
     * 切换前后摄像头
     * @param callback 回调（可选）
     */
    fun switchCamera(callback: CallbackFn? = null) {
        callNativeMethod(METHOD_SWITCH_CAMERA, null, callback)
    }

    /**
     * 启动相机预览
     */
    fun startPreview() {
        callNativeMethod(METHOD_START_PREVIEW, null, null)
    }

    /**
     * 停止相机预览
     */
    fun stopPreview() {
        callNativeMethod(METHOD_STOP_PREVIEW, null, null)
    }

    /**
     * 释放相机资源
     */
    fun releaseCamera() {
        callNativeMethod(METHOD_RELEASE, null, null)
    }

    /**
     * 设置闪光灯模式
     * @param mode "on", "off", "auto"
     */
    fun setFlashMode(mode: String) {
        val params = JSONObject().apply {
            put("mode", mode)
        }
        callNativeMethod(METHOD_SET_FLASH_MODE, params, null)
    }

    /**
     * 设置缩放倍数
     * @param level 缩放倍数
     */
    fun setZoom(level: Float) {
        val params = JSONObject().apply {
            put("level", level)
        }
        callNativeMethod(METHOD_SET_ZOOM, params, null)
    }

    // ---- 内部方法 ----

    private fun callNativeMethod(methodName: String, data: JSONObject?, callbackFn: CallbackFn?) {
        toNative(
            false,
            methodName,
            data?.toString(),
            callbackFn,
            false
        )
    }

    private fun syncCallNativeMethod(
        methodName: String,
        data: JSONObject?,
        callbackFn: CallbackFn?
    ): String {
        return toNative(
            false,
            methodName,
            data?.toString(),
            callbackFn,
            true
        ).toString()
    }

    companion object {
        const val MODULE_NAME = "KRCameraModule"

        // 方法名常量
        const val METHOD_REQUEST_PERMISSION = "requestPermission"
        const val METHOD_CHECK_PERMISSION = "checkPermission"
        const val METHOD_TAKE_PHOTO = "takePhoto"
        const val METHOD_SWITCH_CAMERA = "switchCamera"
        const val METHOD_START_PREVIEW = "startPreview"
        const val METHOD_STOP_PREVIEW = "stopPreview"
        const val METHOD_RELEASE = "release"
        const val METHOD_SET_FLASH_MODE = "setFlashMode"
        const val METHOD_SET_ZOOM = "setZoom"
    }
}
