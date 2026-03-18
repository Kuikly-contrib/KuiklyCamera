package com.tencent.kuiklybase

import com.tencent.kuikly.core.base.Attr

/**
 * KRCameraView 组件的属性定义
 *
 * 通过 "key" with value 将属性透传到各平台原生相机 View。
 * 原生端在 setProp 中根据 key 处理对应属性变化。
 */
class KRCameraViewAttr : Attr() {

    /**
     * 设置摄像头朝向
     * @param facing "back" 后置摄像头（默认）, "front" 前置摄像头
     */
    fun cameraFacing(facing: String): KRCameraViewAttr {
        "cameraFacing" with facing
        return this
    }

    /**
     * 设置闪光灯模式
     * @param mode "off" 关闭（默认）, "on" 开启, "auto" 自动
     */
    fun flashMode(mode: String): KRCameraViewAttr {
        "flashMode" with mode
        return this
    }

    /**
     * 设置预览分辨率
     * @param resolution "high" 高清, "medium" 中等（默认）, "low" 低
     */
    fun resolution(resolution: String): KRCameraViewAttr {
        "resolution" with resolution
        return this
    }

    /**
     * 设置缩放倍数
     * @param level 缩放倍数，1.0 为默认无缩放
     */
    fun zoom(level: Float): KRCameraViewAttr {
        "zoom" with level.toString()
        return this
    }

    /**
     * 设置是否自动启动预览（默认 true）
     * @param auto 是否自动启动
     */
    fun autoStart(auto: Boolean): KRCameraViewAttr {
        "autoStart" with if (auto) "true" else "false"
        return this
    }

    companion object {
        // 摄像头朝向常量
        const val FACING_BACK = "back"
        const val FACING_FRONT = "front"

        // 闪光灯模式常量
        const val FLASH_OFF = "off"
        const val FLASH_ON = "on"
        const val FLASH_AUTO = "auto"

        // 分辨率常量
        const val RESOLUTION_HIGH = "high"
        const val RESOLUTION_MEDIUM = "medium"
        const val RESOLUTION_LOW = "low"
    }
}
