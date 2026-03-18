#import <Foundation/Foundation.h>
#import <UIKit/UIKit.h>
#import <OpenKuiklyIOSRender/KRBaseModule.h>

NS_ASSUME_NONNULL_BEGIN

/**
 * iOS 端相机控制 Module
 *
 * 提供相机权限管理等功能。
 * 继承 KRBaseModule，通过 TDF_EXPORT_MODULE 宏注册，模块名为 "KRCameraModule"。
 */
@interface KRCameraModule : KRBaseModule

@end

NS_ASSUME_NONNULL_END
