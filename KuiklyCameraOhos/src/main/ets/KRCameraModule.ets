import { KRAny, KuiklyRenderBaseModule, KuiklyRenderCallback } from '@kuikly-open/render';
import { camera } from '@kit.CameraKit';
import { BusinessError } from '@kit.BasicServicesKit';
import { abilityAccessCtrl, common, Permissions } from '@kit.AbilityKit';

export class KRCameraModule extends KuiklyRenderBaseModule {
  static readonly MODULE_NAME = 'KRCameraModule';

  syncMode(): boolean {
    return false; // 异步模式，运行在 UI 线程
  }

  call(method: string, params: KRAny, callback: KuiklyRenderCallback | null): KRAny {
    switch (method) {
      case 'requestPermission':
        this.requestPermission(callback);
        return null;
      case 'checkPermission':
        return this.checkPermission();
      case 'takePhoto':
        // 建议通过 View 的 call 方法调用
        callback?.(JSON.stringify({ code: 0, message: 'Please use KRCameraView call method for takePhoto' }));
        return null;
      case 'switchCamera':
        callback?.(JSON.stringify({ code: 0, message: 'Please use KRCameraView call method for switchCamera' }));
        return null;
      case 'startPreview':
        callback?.(JSON.stringify({ code: 0, message: 'Please use KRCameraView call method for startPreview' }));
        return null;
      case 'stopPreview':
        callback?.(JSON.stringify({ code: 0, message: 'Please use KRCameraView call method for stopPreview' }));
        return null;
      case 'setFlashMode':
        callback?.(JSON.stringify({ code: 0, message: 'Please use KRCameraView call method for setFlashMode' }));
        return null;
      case 'setZoom':
        callback?.(JSON.stringify({ code: 0, message: 'Please use KRCameraView call method for setZoom' }));
        return null;
      default:
        return null;
    }
  }

  // 请求相机权限
  private async requestPermission(callback: KuiklyRenderCallback | null): Promise<void> {
    const permission: Permissions = 'ohos.permission.CAMERA';
    const atManager = abilityAccessCtrl.createAtManager();

    try {
      const context = getContext(this) as common.UIAbilityContext;
      const grantStatus = atManager.checkAccessTokenSync(
        context.applicationInfo.accessTokenId, permission
      );

      if (grantStatus === abilityAccessCtrl.GrantStatus.PERMISSION_GRANTED) {
        callback?.(JSON.stringify({ granted: true }));
        return;
      }

      const result = await atManager.requestPermissionsFromUser(context, [permission]);
      const granted = result.authResults[0] === abilityAccessCtrl.GrantStatus.PERMISSION_GRANTED;
      callback?.(JSON.stringify({ granted: granted }));
    } catch (error) {
      const err = error as BusinessError;
      console.error(`KRCameraModule requestPermission error: ${err.message}`);
      callback?.(JSON.stringify({ granted: false, message: err.message }));
    }
  }

  // 检查相机权限（同步方法）
  private checkPermission(): string {
    const permission: Permissions = 'ohos.permission.CAMERA';
    const atManager = abilityAccessCtrl.createAtManager();

    try {
      const context = getContext(this) as common.UIAbilityContext;
      const grantStatus = atManager.checkAccessTokenSync(
        context.applicationInfo.accessTokenId, permission
      );
      if (grantStatus === abilityAccessCtrl.GrantStatus.PERMISSION_GRANTED) {
        return 'granted';
      }
      return 'denied';
    } catch (error) {
      return 'denied';
    }
  }
}
