#import "KRCameraView.h"
#import <AVFoundation/AVFoundation.h>
#import <OpenKuiklyIOSRender/KRComponentDefine.h>

@interface KRCameraView () <AVCapturePhotoCaptureDelegate>

@property (nonatomic, strong) AVCaptureSession *captureSession;
@property (nonatomic, strong) AVCaptureVideoPreviewLayer *previewLayer;
@property (nonatomic, strong) AVCapturePhotoOutput *photoOutput;
@property (nonatomic, strong) AVCaptureDeviceInput *currentInput;

@property (nonatomic, assign) AVCaptureDevicePosition currentPosition;
@property (nonatomic, assign) AVCaptureFlashMode currentFlashMode;
@property (nonatomic, assign) CGFloat currentZoom;
@property (nonatomic, assign) BOOL autoStart;
@property (nonatomic, assign) BOOL isCameraStarted;

// 事件回调 (KuiklyRenderCallback 类型, 由框架通过 hrv_setPropWithKey 传入)
@property (nonatomic, copy) KuiklyRenderCallback onErrorCallback;
@property (nonatomic, copy) KuiklyRenderCallback onCameraReadyCallback;
@property (nonatomic, copy) KuiklyRenderCallback onPhotoCapturedCallback;
// 拍照方法回调
@property (nonatomic, copy) KuiklyRenderCallback photoCaptureMethodCallback;

@end

@implementation KRCameraView

@synthesize hr_rootView;

- (instancetype)init {
    if (self = [super init]) {
        _currentPosition = AVCaptureDevicePositionBack;
        _currentFlashMode = AVCaptureFlashModeOff;
        _currentZoom = 1.0;
        _autoStart = YES;
        _isCameraStarted = NO;
        [self setupCaptureSession];
    }
    return self;
}

- (void)dealloc {
    [self stopCamera];
}

#pragma mark - Setup

- (void)setupCaptureSession {
    self.captureSession = [[AVCaptureSession alloc] init];
    self.captureSession.sessionPreset = AVCaptureSessionPresetHigh;
    
    // 预览层
    self.previewLayer = [AVCaptureVideoPreviewLayer layerWithSession:self.captureSession];
    self.previewLayer.videoGravity = AVLayerVideoGravityResizeAspectFill;
    [self.layer addSublayer:self.previewLayer];
    
    // 照片输出
    self.photoOutput = [[AVCapturePhotoOutput alloc] init];
    if ([self.captureSession canAddOutput:self.photoOutput]) {
        [self.captureSession addOutput:self.photoOutput];
    }
}

- (void)layoutSubviews {
    [super layoutSubviews];
    self.previewLayer.frame = self.bounds;
}

#pragma mark - KuiklyRenderViewExportProtocol

- (void)hrv_setPropWithKey:(NSString *)propKey propValue:(id)propValue {
    // 先处理通用 CSS 样式
    KUIKLY_SET_CSS_COMMON_PROP
    
    if ([propKey isEqualToString:@"cameraFacing"]) {
        NSString *facing = [NSString stringWithFormat:@"%@", propValue];
        self.currentPosition = [facing isEqualToString:@"front"] ?
            AVCaptureDevicePositionFront : AVCaptureDevicePositionBack;
        if (self.isCameraStarted) {
            [self configureCameraInput];
        }
    }
    else if ([propKey isEqualToString:@"flashMode"]) {
        NSString *mode = [NSString stringWithFormat:@"%@", propValue];
        if ([mode isEqualToString:@"on"]) {
            self.currentFlashMode = AVCaptureFlashModeOn;
        } else if ([mode isEqualToString:@"auto"]) {
            self.currentFlashMode = AVCaptureFlashModeAuto;
        } else {
            self.currentFlashMode = AVCaptureFlashModeOff;
        }
    }
    else if ([propKey isEqualToString:@"resolution"]) {
        NSString *res = [NSString stringWithFormat:@"%@", propValue];
        if ([res isEqualToString:@"high"]) {
            self.captureSession.sessionPreset = AVCaptureSessionPresetHigh;
        } else if ([res isEqualToString:@"low"]) {
            self.captureSession.sessionPreset = AVCaptureSessionPresetLow;
        } else {
            self.captureSession.sessionPreset = AVCaptureSessionPresetMedium;
        }
    }
    else if ([propKey isEqualToString:@"zoom"]) {
        self.currentZoom = [propValue floatValue];
        [self applyZoom];
    }
    else if ([propKey isEqualToString:@"autoStart"]) {
        self.autoStart = [[NSString stringWithFormat:@"%@", propValue] isEqualToString:@"true"];
    }
    // 事件绑定: Kuikly 框架通过 hrv_setPropWithKey 传入 KuiklyRenderCallback 类型的 block
    else if ([propKey isEqualToString:@"onError"]) {
        self.onErrorCallback = (KuiklyRenderCallback)propValue;
    }
    else if ([propKey isEqualToString:@"onCameraReady"]) {
        self.onCameraReadyCallback = (KuiklyRenderCallback)propValue;
    }
    else if ([propKey isEqualToString:@"onPhotoCaptured"]) {
        self.onPhotoCapturedCallback = (KuiklyRenderCallback)propValue;
    }
}

- (void)hrv_callWithMethod:(NSString *)method
                    params:(NSString *)params
                  callback:(KuiklyRenderCallback)callback {
    if ([method isEqualToString:@"takePhoto"]) {
        [self takePhotoWithParams:params callback:callback];
    }
    else if ([method isEqualToString:@"switchCamera"]) {
        [self switchCamera];
        if (callback) { callback(@{@"code": @(0)}); }
    }
    else if ([method isEqualToString:@"startPreview"]) {
        [self startCamera];
        if (callback) { callback(@{@"code": @(0)}); }
    }
    else if ([method isEqualToString:@"stopPreview"]) {
        [self stopCamera];
        if (callback) { callback(@{@"code": @(0)}); }
    }
    else if ([method isEqualToString:@"release"]) {
        [self releaseCamera];
        if (callback) { callback(@{@"code": @(0)}); }
    }
    else if ([method isEqualToString:@"setZoom"]) {
        self.currentZoom = [params floatValue];
        [self applyZoom];
        if (callback) { callback(@{@"code": @(0)}); }
    }
    else if ([method isEqualToString:@"setFlashMode"]) {
        if ([params isEqualToString:@"on"]) {
            self.currentFlashMode = AVCaptureFlashModeOn;
        } else if ([params isEqualToString:@"auto"]) {
            self.currentFlashMode = AVCaptureFlashModeAuto;
        } else {
            self.currentFlashMode = AVCaptureFlashModeOff;
        }
        if (callback) { callback(@{@"code": @(0)}); }
    }
}

- (void)didMoveToWindow {
    [super didMoveToWindow];
    if (self.window && self.autoStart && !self.isCameraStarted) {
        [self startCamera];
    }
}

#pragma mark - Camera Control

- (void)startCamera {
    AVAuthorizationStatus status = [AVCaptureDevice authorizationStatusForMediaType:AVMediaTypeVideo];
    if (status == AVAuthorizationStatusAuthorized) {
        [self configureCameraInput];
        KR_WEAK_SELF
        dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^{
            [weakSelf.captureSession startRunning];
            dispatch_async(dispatch_get_main_queue(), ^{
                KR_STRONG_SELF_RETURN_IF_NIL
                strongSelf.isCameraStarted = YES;
                if (strongSelf.onCameraReadyCallback) {
                    strongSelf.onCameraReadyCallback(@{@"status": @"ready"});
                }
            });
        });
    } else if (status == AVAuthorizationStatusNotDetermined) {
        KR_WEAK_SELF
        [AVCaptureDevice requestAccessForMediaType:AVMediaTypeVideo completionHandler:^(BOOL granted) {
            if (granted) {
                dispatch_async(dispatch_get_main_queue(), ^{
                    [weakSelf startCamera];
                });
            } else {
                dispatch_async(dispatch_get_main_queue(), ^{
                    KR_STRONG_SELF_RETURN_IF_NIL
                    if (strongSelf.onErrorCallback) {
                        strongSelf.onErrorCallback(@{
                            @"errorCode": @(-3),
                            @"description": @"Camera permission denied"
                        });
                    }
                });
            }
        }];
    } else {
        if (self.onErrorCallback) {
            self.onErrorCallback(@{
                @"errorCode": @(-3),
                @"description": @"Camera permission denied"
            });
        }
    }
}

- (void)stopCamera {
    KR_WEAK_SELF
    dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^{
        [weakSelf.captureSession stopRunning];
        dispatch_async(dispatch_get_main_queue(), ^{
            KR_STRONG_SELF_RETURN_IF_NIL
            strongSelf.isCameraStarted = NO;
        });
    });
}

- (void)releaseCamera {
    [self stopCamera];
    for (AVCaptureInput *input in self.captureSession.inputs) {
        [self.captureSession removeInput:input];
    }
    for (AVCaptureOutput *output in self.captureSession.outputs) {
        [self.captureSession removeOutput:output];
    }
}

- (void)switchCamera {
    self.currentPosition = (self.currentPosition == AVCaptureDevicePositionBack) ?
        AVCaptureDevicePositionFront : AVCaptureDevicePositionBack;
    [self configureCameraInput];
}

- (void)configureCameraInput {
    // 移除当前输入
    if (self.currentInput) {
        [self.captureSession removeInput:self.currentInput];
    }
    
    // 获取对应位置的摄像头
    AVCaptureDevice *device = [self cameraWithPosition:self.currentPosition];
    if (!device) {
        if (self.onErrorCallback) {
            self.onErrorCallback(@{
                @"errorCode": @(-1),
                @"description": @"Camera device not available"
            });
        }
        return;
    }
    
    NSError *error = nil;
    AVCaptureDeviceInput *input = [AVCaptureDeviceInput deviceInputWithDevice:device error:&error];
    if (error) {
        if (self.onErrorCallback) {
            self.onErrorCallback(@{
                @"errorCode": @(-1),
                @"description": error.localizedDescription ?: @"Failed to create camera input"
            });
        }
        return;
    }
    
    if ([self.captureSession canAddInput:input]) {
        [self.captureSession addInput:input];
        self.currentInput = input;
    }
    
    [self applyZoom];
}

- (AVCaptureDevice *)cameraWithPosition:(AVCaptureDevicePosition)position {
    NSArray<AVCaptureDevice *> *devices = [AVCaptureDevice devicesWithMediaType:AVMediaTypeVideo];
    for (AVCaptureDevice *device in devices) {
        if (device.position == position) {
            return device;
        }
    }
    return [AVCaptureDevice defaultDeviceWithMediaType:AVMediaTypeVideo];
}

- (void)applyZoom {
    AVCaptureDevice *device = self.currentInput.device;
    if (device && [device lockForConfiguration:nil]) {
        CGFloat maxZoom = device.activeFormat.videoMaxZoomFactor;
        CGFloat zoom = MIN(MAX(self.currentZoom, 1.0), maxZoom);
        device.videoZoomFactor = zoom;
        [device unlockForConfiguration];
    }
}

#pragma mark - Photo Capture

- (void)takePhotoWithParams:(NSString *)params callback:(KuiklyRenderCallback)callback {
    if (!self.photoOutput) {
        if (callback) {
            callback(@{@"code": @(-2), @"message": @"PhotoOutput not initialized"});
        }
        return;
    }
    
    self.photoCaptureMethodCallback = callback;
    
    AVCapturePhotoSettings *settings = [AVCapturePhotoSettings photoSettings];
    
    // 设置闪光灯
    if ([self.photoOutput.supportedFlashModes containsObject:@(self.currentFlashMode)]) {
        settings.flashMode = self.currentFlashMode;
    }
    
    [self.photoOutput capturePhotoWithSettings:settings delegate:self];
}

#pragma mark - AVCapturePhotoCaptureDelegate

- (void)captureOutput:(AVCapturePhotoOutput *)output
    didFinishProcessingPhoto:(AVCapturePhoto *)photo
                       error:(NSError *)error {
    
    if (error) {
        if (self.onErrorCallback) {
            self.onErrorCallback(@{
                @"errorCode": @(-1),
                @"description": error.localizedDescription ?: @"Photo capture failed"
            });
        }
        if (self.photoCaptureMethodCallback) {
            self.photoCaptureMethodCallback(@{
                @"code": @(-1),
                @"message": error.localizedDescription ?: @"Photo capture failed"
            });
            self.photoCaptureMethodCallback = nil;
        }
        return;
    }
    
    NSData *imageData = [photo fileDataRepresentation];
    if (!imageData) {
        if (self.photoCaptureMethodCallback) {
            self.photoCaptureMethodCallback(@{@"code": @(-1), @"message": @"No image data"});
            self.photoCaptureMethodCallback = nil;
        }
        return;
    }
    
    // 保存到临时目录
    NSString *tempDir = NSTemporaryDirectory();
    NSString *fileName = [NSString stringWithFormat:@"IMG_%@.jpg",
                          [self currentTimestampString]];
    NSString *filePath = [tempDir stringByAppendingPathComponent:fileName];
    
    [imageData writeToFile:filePath atomically:YES];
    
    // 触发事件回调
    if (self.onPhotoCapturedCallback) {
        self.onPhotoCapturedCallback(@{@"filePath": filePath});
    }
    
    // 方法回调
    if (self.photoCaptureMethodCallback) {
        self.photoCaptureMethodCallback(@{@"code": @(0), @"filePath": filePath});
        self.photoCaptureMethodCallback = nil;
    }
}

#pragma mark - Helpers

- (NSString *)currentTimestampString {
    NSDateFormatter *formatter = [[NSDateFormatter alloc] init];
    formatter.dateFormat = @"yyyyMMdd_HHmmss";
    return [formatter stringFromDate:[NSDate date]];
}

@end
