import { KRAny, KuiklyRenderBaseView, KuiklyRenderCallback } from '@kuikly-open/render';
import { camera } from '@kit.CameraKit';
import { image } from '@kit.ImageKit';
import { BusinessError } from '@kit.BasicServicesKit';
import { fileIo as fs } from '@kit.CoreFileKit';
import { ComponentContent } from '@kit.ArkUI';
import { abilityAccessCtrl, common, Permissions } from '@kit.AbilityKit';

@Observed
export class KRCameraView extends KuiklyRenderBaseView {
  static readonly VIEW_NAME = 'KRCameraView';

  // 属性
  cssCameraFacing: string = 'back'; // 'front' | 'back'
  cssFlashMode: string = 'off';     // 'on' | 'off' | 'auto'
  cssResolution: string = 'high';   // 'high' | 'medium' | 'low'
  cssZoom: number = 1.0;
  cssAutoStart: boolean = true;

  // 状态
  surfaceId: string = '';
  isCameraStarted: boolean = false;

  // Camera Kit 对象
  private cameraManager: camera.CameraManager | null = null;
  private cameraInput: camera.CameraInput | null = null;
  private previewOutput: camera.PreviewOutput | null = null;
  private photoOutput: camera.PhotoOutput | null = null;
  private photoSession: camera.PhotoSession | null = null;
  private imageReceiver: image.ImageReceiver | null = null;

  // 事件回调
  private onErrorCallback: KuiklyRenderCallback | null = null;
  private onCameraReadyCallback: KuiklyRenderCallback | null = null;
  private onPhotoCapturedCallback: KuiklyRenderCallback | null = null;
  // 方法回调
  private photoMethodCallback: KuiklyRenderCallback | null = null;

  createArkUIView(): ComponentContent<KuiklyRenderBaseView> {
    const uiContext = this.getUIContext() as UIContext;
    return new ComponentContent<KuiklyRenderBaseView>(
      uiContext, wrapBuilder<[KuiklyRenderBaseView]>(createCameraView), this
    );
  }

  setProp(propKey: string, propValue: KRAny | KuiklyRenderCallback): boolean {
    switch (propKey) {
      case 'cameraFacing':
        this.cssCameraFacing = propValue as string;
        if (this.isCameraStarted) {
          this.restartCamera();
        }
        return true;
      case 'flashMode':
        this.cssFlashMode = propValue as string;
        this.applyFlashMode();
        return true;
      case 'resolution':
        this.cssResolution = propValue as string;
        return true;
      case 'zoom':
        this.cssZoom = parseFloat(propValue as string);
        this.applyZoom();
        return true;
      case 'autoStart':
        this.cssAutoStart = (propValue as string) === 'true';
        return true;
      case 'onError':
        this.onErrorCallback = propValue as KuiklyRenderCallback;
        return true;
      case 'onCameraReady':
        this.onCameraReadyCallback = propValue as KuiklyRenderCallback;
        return true;
      case 'onPhotoCaptured':
        this.onPhotoCapturedCallback = propValue as KuiklyRenderCallback;
        return true;
      default:
        return super.setProp(propKey, propValue);
    }
  }

  call(method: string, params: KRAny, callback: KuiklyRenderCallback | null): KRAny {
    switch (method) {
      case 'takePhoto':
        this.takePhoto(callback);
        return null;
      case 'switchCamera':
        this.switchCamera();
        if (callback) { callback(JSON.stringify({ code: 0 })); }
        return null;
      case 'startPreview':
        this.startCamera();
        if (callback) { callback(JSON.stringify({ code: 0 })); }
        return null;
      case 'stopPreview':
        this.stopCamera();
        if (callback) { callback(JSON.stringify({ code: 0 })); }
        return null;
      case 'release':
        this.releaseCamera();
        if (callback) { callback(JSON.stringify({ code: 0 })); }
        return null;
      case 'setZoom':
        this.cssZoom = parseFloat(params as string);
        this.applyZoom();
        if (callback) { callback(JSON.stringify({ code: 0 })); }
        return null;
      case 'setFlashMode':
        this.cssFlashMode = params as string;
        this.applyFlashMode();
        if (callback) { callback(JSON.stringify({ code: 0 })); }
        return null;
      default:
        return null;
    }
  }

  // 当 surfaceId 就绪后由 XComponent 回调
  onSurfaceReady(surfaceId: string): void {
    this.surfaceId = surfaceId;
    if (this.cssAutoStart) {
      this.startCamera();
    }
  }

  // 启动相机
  async startCamera(): Promise<void> {
    try {
      // 检查权限
      const granted = await this.checkAndRequestPermission();
      if (!granted) {
        this.onErrorCallback?.(JSON.stringify({
          errorCode: -3,
          description: 'Camera permission denied'
        }));
        return;
      }

      if (!this.surfaceId) {
        return;
      }

      this.cameraManager = camera.getCameraManager(getContext(this) as common.BaseContext);
      const cameras = this.cameraManager.getSupportedCameras();
      if (cameras.length === 0) {
        this.onErrorCallback?.(JSON.stringify({
          errorCode: -1,
          description: 'No camera device available'
        }));
        return;
      }

      // 选择摄像头
      const cameraDevice = this.getCameraDevice(cameras);

      const sceneModes = this.cameraManager.getSupportedSceneModes(cameraDevice);
      const outputCapability = this.cameraManager.getSupportedOutputCapability(cameraDevice, camera.SceneMode.NORMAL_PHOTO);

      // 创建相机输入
      this.cameraInput = this.cameraManager.createCameraInput(cameraDevice);
      await this.cameraInput.open();

      // 创建预览输出
      const previewProfile = outputCapability.previewProfiles[0];
      this.previewOutput = this.cameraManager.createPreviewOutput(previewProfile, this.surfaceId);

      // 创建拍照输出（使用 ImageReceiver 获取拍照数据）
      const photoProfile = outputCapability.photoProfiles[0];
      this.photoOutput = this.cameraManager.createPhotoOutput(photoProfile);

      // 创建 PhotoSession
      this.photoSession = this.cameraManager.createSession(camera.SceneMode.NORMAL_PHOTO) as camera.PhotoSession;
      this.photoSession.beginConfig();
      this.photoSession.addInput(this.cameraInput);
      this.photoSession.addOutput(this.previewOutput);
      this.photoSession.addOutput(this.photoOutput);
      await this.photoSession.commitConfig();
      await this.photoSession.start();

      this.isCameraStarted = true;

      // 应用缩放和闪光灯
      this.applyZoom();
      this.applyFlashMode();

      this.onCameraReadyCallback?.(JSON.stringify({ status: 'ready' }));

    } catch (error) {
      const err = error as BusinessError;
      console.error(`KRCameraView startCamera error: ${err.message}`);
      this.onErrorCallback?.(JSON.stringify({
        errorCode: -1,
        description: err.message || 'Failed to start camera'
      }));
    }
  }

  // 停止相机
  async stopCamera(): Promise<void> {
    try {
      if (this.photoSession) {
        await this.photoSession.stop();
      }
      this.isCameraStarted = false;
    } catch (error) {
      console.error(`KRCameraView stopCamera error: ${(error as BusinessError).message}`);
    }
  }

  // 释放相机资源
  async releaseCamera(): Promise<void> {
    try {
      if (this.photoSession) {
        await this.photoSession.stop();
        await this.photoSession.release();
        this.photoSession = null;
      }
      if (this.cameraInput) {
        await this.cameraInput.close();
        this.cameraInput = null;
      }
      this.previewOutput = null;
      this.photoOutput = null;
      this.isCameraStarted = false;
    } catch (error) {
      console.error(`KRCameraView releaseCamera error: ${(error as BusinessError).message}`);
    }
  }

  // 重启相机（切换摄像头时使用）
  private async restartCamera(): Promise<void> {
    await this.releaseCamera();
    await this.startCamera();
  }

  // 切换摄像头
  private switchCamera(): void {
    this.cssCameraFacing = this.cssCameraFacing === 'back' ? 'front' : 'back';
    this.restartCamera();
  }

  // 拍照
  private async takePhoto(callback: KuiklyRenderCallback | null): Promise<void> {
    if (!this.photoOutput) {
      callback?.(JSON.stringify({ code: -2, message: 'PhotoOutput not initialized' }));
      return;
    }

    try {
      // 注册拍照回调
      this.photoOutput.on('photoAvailable', (errData: BusinessError, photo: camera.Photo) => {
        if (errData) {
          callback?.(JSON.stringify({ code: -1, message: errData.message }));
          return;
        }
        const mainImage = photo.main;
        mainImage.getComponent(image.ComponentType.JPEG, async (err: BusinessError, component: image.Component) => {
          if (err || !component) {
            callback?.(JSON.stringify({ code: -1, message: 'Failed to get photo data' }));
            photo.release();
            return;
          }
          const buffer = component.byteBuffer;
          // 保存到临时目录
          const context = getContext(this) as common.UIAbilityContext;
          const tempDir = context.tempDir;
          const timestamp = new Date().getTime();
          const filePath = `${tempDir}/IMG_${timestamp}.jpg`;
          const file = fs.openSync(filePath, fs.OpenMode.CREATE | fs.OpenMode.WRITE_ONLY);
          fs.writeSync(file.fd, buffer);
          fs.closeSync(file.fd);
          photo.release();
          // 事件回调
          this.onPhotoCapturedCallback?.(JSON.stringify({ filePath: filePath }));
          // 方法回调
          callback?.(JSON.stringify({ code: 0, filePath: filePath }));
        });
      });

      // 执行拍照
      const photoSettings: camera.PhotoCaptureSetting = {
        quality: camera.QualityLevel.QUALITY_LEVEL_HIGH,
        rotation: camera.ImageRotation.ROTATION_0
      };
      this.photoOutput.capture(photoSettings);
    } catch (error) {
      const err = error as BusinessError;
      callback?.(JSON.stringify({ code: -1, message: err.message }));
    }
  }

  // 应用缩放
  private applyZoom(): void {
    if (this.photoSession && this.isCameraStarted) {
      try {
        const zoomRange = this.photoSession.getZoomRatioRange();
        if (zoomRange && zoomRange.length >= 2) {
          const minZoom = zoomRange[0];
          const maxZoom = zoomRange[1];
          const zoom = Math.max(minZoom, Math.min(this.cssZoom, maxZoom));
          this.photoSession.setZoomRatio(zoom);
        }
      } catch (error) {
        console.error(`KRCameraView applyZoom error: ${(error as BusinessError).message}`);
      }
    }
  }

  // 应用闪光灯模式
  private applyFlashMode(): void {
    if (this.photoSession && this.isCameraStarted) {
      try {
        let flashMode: camera.FlashMode;
        switch (this.cssFlashMode) {
          case 'on':
            flashMode = camera.FlashMode.FLASH_MODE_ALWAYS_OPEN;
            break;
          case 'auto':
            flashMode = camera.FlashMode.FLASH_MODE_AUTO;
            break;
          default:
            flashMode = camera.FlashMode.FLASH_MODE_CLOSE;
        }
        if (this.photoSession.isFlashModeSupported(flashMode)) {
          this.photoSession.setFlashMode(flashMode);
        }
      } catch (error) {
        console.error(`KRCameraView applyFlashMode error: ${(error as BusinessError).message}`);
      }
    }
  }

  // 选择摄像头设备
  private getCameraDevice(cameras: Array<camera.CameraDevice>): camera.CameraDevice {
    const targetPosition = this.cssCameraFacing === 'front'
      ? camera.CameraPosition.CAMERA_POSITION_FRONT
      : camera.CameraPosition.CAMERA_POSITION_BACK;

    for (const cam of cameras) {
      if (cam.cameraPosition === targetPosition) {
        return cam;
      }
    }
    return cameras[0];
  }

  // 检查和请求相机权限
  private async checkAndRequestPermission(): Promise<boolean> {
    const permission: Permissions = 'ohos.permission.CAMERA';
    const atManager = abilityAccessCtrl.createAtManager();
    const context = getContext(this) as common.UIAbilityContext;

    try {
      const grantStatus = atManager.checkAccessTokenSync(
        context.applicationInfo.accessTokenId, permission
      );
      if (grantStatus === abilityAccessCtrl.GrantStatus.PERMISSION_GRANTED) {
        return true;
      }
      const result = await atManager.requestPermissionsFromUser(context, [permission]);
      return result.authResults[0] === abilityAccessCtrl.GrantStatus.PERMISSION_GRANTED;
    } catch (error) {
      console.error(`KRCameraView permission error: ${(error as BusinessError).message}`);
      return false;
    }
  }
}

// ArkUI 组件
@Component
export struct KRCamera {
  @ObjectLink renderView: KRCameraView;
  private xComponentController: XComponentController = new XComponentController();

  build() {
    Column() {
      XComponent({
        id: 'KRCameraXComponent',
        type: XComponentType.SURFACE,
        controller: this.xComponentController
      })
        .onLoad(() => {
          const surfaceId = this.xComponentController.getXComponentSurfaceId();
          this.renderView.onSurfaceReady(surfaceId);
        })
        .width('100%')
        .height('100%')
    }
    .width('100%')
    .height('100%')
    .backgroundColor(this.renderView.cssBackgroundColor)
  }
}

@Builder
function createCameraView(view: KuiklyRenderBaseView) {
  KRCamera({ renderView: view as KRCameraView })
}
