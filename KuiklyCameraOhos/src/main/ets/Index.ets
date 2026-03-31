/**
 * KuiklyCameraOhos - Kuikly Camera 鸿蒙原生组件
 *
 * 导出 KRCameraView 和 KRCameraModule，供使用方注册到 KuiklyViewDelegate 中。
 *
 * 使用方式：
 * 1. 在 oh-package.json5 中添加依赖：
 *    "@kuikly-open/camera": "file:../KuiklyCameraOhos"  (本地引用)
 *    或
 *    "@kuikly-open/camera": "x.x.x"                     (ohpm 远程引用)
 *
 * 2. 在 KuiklyViewDelegate.ets 中注册：
 *    import { KRCameraView, KRCameraModule } from '@kuikly-open/camera';
 *
 *    // 注册 View
 *    map.set(KRCameraView.VIEW_NAME, () => new KRCameraView());
 *
 *    // 注册 Module
 *    map.set(KRCameraModule.MODULE_NAME, () => new KRCameraModule());
 *
 * 3. 在 module.json5 中添加相机权限：
 *    "requestPermissions": [
 *      {
 *        "name": "ohos.permission.CAMERA",
 *        "reason": "$string:camera_reason",
 *        "usedScene": { "abilities": ["EntryAbility"], "when": "inuse" }
 *      }
 *    ]
 */

export { KRCameraView, KRCamera } from './KRCameraView';
export { KRCameraModule } from './KRCameraModule';
