# @yuki8273/camera

KuiklyCamera 鸿蒙（HarmonyOS）原生模块，为 Kuikly 框架提供相机视图和相机功能模块的 ArkTS 实现。

## 安装

```bash
ohpm install @yuki8273/camera
```

或使用简写：

```bash
ohpm i @yuki8273/camera
```

也可以在 `oh-package.json5` 中手动添加依赖：

```json5
"dependencies": {
  "@yuki8273/camera": "1.0.0"
}
```

然后执行：

```bash
ohpm install
```

## 权限配置

在 `module.json5` 中添加相机权限：

```json5
"requestPermissions": [
  {
    "name": "ohos.permission.CAMERA",
    "reason": "$string:camera_reason",
    "usedScene": {
      "abilities": ["EntryAbility"],
      "when": "inuse"
    }
  }
]
```

## 依赖

- `@kuikly-open/render`: ^2.7.0

## 许可证

MIT
