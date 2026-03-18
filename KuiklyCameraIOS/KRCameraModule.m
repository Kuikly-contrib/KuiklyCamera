#import "KRCameraModule.h"
#import <AVFoundation/AVFoundation.h>
#import <OpenKuiklyIOSRender/NSObject+KR.h>

@implementation KRCameraModule

@synthesize hr_rootView;

// 使用 TDF_EXPORT_MODULE 宏注册 Module，名称需与 Kotlin 跨端层 KRCameraModule.MODULE_NAME 一致
TDF_EXPORT_MODULE(KRCameraModule)

#pragma mark - Permission

/// 请求相机权限
/// KRBaseModule 自动分发: kotlin 调用 "requestPermission" -> ObjC selector "requestPermission:"
- (void)requestPermission:(NSDictionary *)args {
    KuiklyRenderCallback callback = args[KR_CALLBACK_KEY];
    
    AVAuthorizationStatus status = [AVCaptureDevice authorizationStatusForMediaType:AVMediaTypeVideo];
    
    switch (status) {
        case AVAuthorizationStatusAuthorized:
            if (callback) {
                callback(@{@"granted": @(YES)});
            }
            break;
            
        case AVAuthorizationStatusNotDetermined: {
            [AVCaptureDevice requestAccessForMediaType:AVMediaTypeVideo completionHandler:^(BOOL granted) {
                dispatch_async(dispatch_get_main_queue(), ^{
                    if (callback) {
                        callback(@{@"granted": @(granted)});
                    }
                });
            }];
            break;
        }
            
        case AVAuthorizationStatusDenied:
        case AVAuthorizationStatusRestricted:
        default:
            if (callback) {
                callback(@{@"granted": @(NO), @"message": @"Camera permission denied or restricted"});
            }
            break;
    }
}

/// 检查相机权限（同步方法）
/// KRBaseModule 自动分发: kotlin 调用 "checkPermission" -> ObjC selector "checkPermission:"
- (NSString *)checkPermission:(NSDictionary *)args {
    AVAuthorizationStatus status = [AVCaptureDevice authorizationStatusForMediaType:AVMediaTypeVideo];
    switch (status) {
        case AVAuthorizationStatusAuthorized:
            return @"granted";
        case AVAuthorizationStatusNotDetermined:
            return @"not_determined";
        case AVAuthorizationStatusDenied:
        case AVAuthorizationStatusRestricted:
        default:
            return @"denied";
    }
}

#pragma mark - Camera Control

/// 通知通过 View 控制拍照等操作
/// KRBaseModule 自动分发: kotlin 调用 "takePhoto" -> ObjC selector "takePhoto:"
- (void)takePhoto:(NSDictionary *)args {
    KuiklyRenderCallback callback = args[KR_CALLBACK_KEY];
    if (callback) {
        callback(@{@"code": @(0), @"message": @"Please use KRCameraView's call method for takePhoto"});
    }
}

/// 切换摄像头
- (void)switchCamera:(NSDictionary *)args {
    KuiklyRenderCallback callback = args[KR_CALLBACK_KEY];
    if (callback) {
        callback(@{@"code": @(0), @"message": @"Please use KRCameraView's call method for switchCamera"});
    }
}

/// 开始预览
- (void)startPreview:(NSDictionary *)args {
    KuiklyRenderCallback callback = args[KR_CALLBACK_KEY];
    if (callback) {
        callback(@{@"code": @(0), @"message": @"Please use KRCameraView's call method for startPreview"});
    }
}

/// 停止预览
- (void)stopPreview:(NSDictionary *)args {
    KuiklyRenderCallback callback = args[KR_CALLBACK_KEY];
    if (callback) {
        callback(@{@"code": @(0), @"message": @"Please use KRCameraView's call method for stopPreview"});
    }
}

/// 设置闪光灯模式
- (void)setFlashMode:(NSDictionary *)args {
    KuiklyRenderCallback callback = args[KR_CALLBACK_KEY];
    if (callback) {
        callback(@{@"code": @(0), @"message": @"Please use KRCameraView's call method for setFlashMode"});
    }
}

/// 设置缩放
- (void)setZoom:(NSDictionary *)args {
    KuiklyRenderCallback callback = args[KR_CALLBACK_KEY];
    if (callback) {
        callback(@{@"code": @(0), @"message": @"Please use KRCameraView's call method for setZoom"});
    }
}

@end
