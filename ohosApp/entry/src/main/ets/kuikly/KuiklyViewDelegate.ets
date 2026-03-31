import {
  IKuiklyViewDelegate,
  KRRenderModuleExportCreator,
  KRRenderViewExportCreator
} from '@kuikly-open/render';
import { KRMyView } from './components/KRMyView';
import { KRCameraView, KRCameraModule } from '@kuikly-open/camera';
import { KRBridgeModule } from './modules/KRBridgeModule';
import { KRMyModule } from './modules/KRMyModule';

export class KuiklyViewDelegate extends IKuiklyViewDelegate {
  getCustomRenderViewCreatorRegisterMap(): Map<string, KRRenderViewExportCreator> {
    const map: Map<string, KRRenderViewExportCreator> = new Map();
    map.set(KRMyView.VIEW_NAME, () => new KRMyView());
    map.set(KRCameraView.VIEW_NAME, () => new KRCameraView());
    return map;
  }

  getCustomRenderModuleCreatorRegisterMap(): Map<string, KRRenderModuleExportCreator> {
    const map: Map<string, KRRenderModuleExportCreator> = new Map();
    map.set(KRBridgeModule.MODULE_NAME, () => new KRBridgeModule());
    map.set(KRMyModule.MODULE_NAME, () => new KRMyModule());
    map.set(KRCameraModule.MODULE_NAME, () => new KRCameraModule());
    return map;
  }
}