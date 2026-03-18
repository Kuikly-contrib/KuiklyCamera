package com.tencent.kuiklybase

import com.tencent.kuikly.core.base.event.Event
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject

/**
 * KRCameraView 组件的事件定义
 *
 * 原生侧通过 KuiklyRenderCallback 触发这些事件，
 * 跨端侧通过 register 注册监听。
 */
class KRCameraViewEvent : Event() {

    /**
     * 相机就绪事件，相机初始化完成并开始预览时触发
     * @param handler 回调
     */
    fun onCameraReady(handler: () -> Unit) {
        register(EVENT_CAMERA_READY) {
            handler()
        }
    }

    /**
     * 相机错误事件
     * @param handler 回调，参数为错误码和错误描述
     */
    fun onError(handler: (errorCode: Int, description: String) -> Unit) {
        register(EVENT_ERROR) {
            val params = it as? JSONObject ?: JSONObject()
            handler(
                params.optInt("errorCode", -1),
                params.optString("description", "Unknown error")
            )
        }
    }

    /**
     * 拍照完成事件
     * @param handler 回调，参数为照片文件路径
     */
    fun onPhotoCaptured(handler: (filePath: String) -> Unit) {
        register(EVENT_PHOTO_CAPTURED) {
            val params = it as? JSONObject ?: JSONObject()
            handler(params.optString("filePath", ""))
        }
    }

    /**
     * 权限状态变化事件
     * @param handler 回调，参数为是否已授权
     */
    fun onPermissionResult(handler: (granted: Boolean) -> Unit) {
        register(EVENT_PERMISSION_RESULT) {
            val params = it as? JSONObject ?: JSONObject()
            handler(params.optBoolean("granted", false))
        }
    }

    companion object {
        const val EVENT_CAMERA_READY = "onCameraReady"
        const val EVENT_ERROR = "onError"
        const val EVENT_PHOTO_CAPTURED = "onPhotoCaptured"
        const val EVENT_PERMISSION_RESULT = "onPermissionResult"
    }
}
