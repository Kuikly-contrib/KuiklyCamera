#import <UIKit/UIKit.h>
#import <OpenKuiklyIOSRender/KuiklyRenderViewExportProtocol.h>

NS_ASSUME_NONNULL_BEGIN

/**
 * iOS 端相机预览 View
 *
 * 基于 AVFoundation (AVCaptureSession + AVCaptureVideoPreviewLayer) 实现相机预览、拍照等功能。
 * 实现 KuiklyRenderViewExportProtocol 协议。
 * 类名 "KRCameraView" 即为 Kuikly 框架的注册名（通过 NSClassFromString 查找）。
 */
@interface KRCameraView : UIView <KuiklyRenderViewExportProtocol>

@end

NS_ASSUME_NONNULL_END
