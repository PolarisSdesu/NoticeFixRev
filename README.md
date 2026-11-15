# Notice Fix Rev

次世代的通知图标修复模块。原作 [NoticeFix](https://github.com/xudazhu1/NoticeFix)（AGPL-3.0）用 Java + Xposed API 82 编写，
在 Android 16/17 的 SystemUI 上大量反射失配（`NoSuchFieldException: mContext`、`ClassNotFoundError: HeadsUpManager`），
并且每帧抛异常拖垮 SystemUI。

Rev 用 **libxposed API 101 + Kotlin + Jetpack Compose（Material 3）** 重写：

* 不再依赖 `XposedHelpers` 反射字符串，改用标准 `java.lang.reflect` + 明确的 101 生命周期回调
* 配置通过 libxposed **remote preferences / remote files** 跨进程传递，不需要 root，不需要 world-readable 文件
* 模块 UI 与框架通过 `libxposed:service` 通信，可实时申请作用域、读取框架状态
* 每个 hook 都包在 `ExceptionMode.PROTECTIVE` 里，失败只记日志，绝不把异常抛进 SystemUI

## 结构

```
app/src/main/java/com/xeasy/noticefixrev/
├── NoticeFixRevApp.kt              # Application：绑定 XposedService
├── data/
│   ├── PrefKeys.kt                 # UI / 被注入进程共用的键名
│   └── ModuleRepository.kt         # 写配置、图标写入 remote file
├── module/                         # 只有这里会进 SystemUI 进程
│   ├── ModuleEntry.kt              # java_init.list 指向的入口
│   ├── ConfigSnapshot.kt           # 配置快照 + 变更监听
│   └── NotificationIconHook.kt     # 唯一 hook 点
└── ui/
    ├── MainActivity.kt / AppRoot.kt
    ├── AppListScreen.kt            # 应用列表 + 图标选择底部弹层
    ├── SettingsScreen.kt
    ├── MainViewModel.kt
    └── theme/{Theme,Type}.kt
```

`META-INF/xposed/` 下三个文件由框架直接读取：

| 文件 | 作用 |
|---|---|
| `module.prop` | `minApiVersion=101`、`staticScope=true`、`autoHotReload=true` |
| `java_init.list` | `com.xeasy.noticefixrev.module.ModuleEntry` |
| `scope.list` | `com.android.systemui` |

## 工作原理

1. SystemUI 启动 → libxposed 实例化 `ModuleEntry` → `onPackageReady()` 里安装 hook。
2. Hook `com.android.systemui.statusbar.NotificationListener#onNotificationPosted`。
3. 通知到达时，如果该应用已启用且存在自定义图标，就把 `notification.smallIcon` 换成
   `Icon.createWithBitmap(...)`；状态栏、通知栏、锁屏同时生效。
4. 模块 UI 任意时刻改配置 → 框架把变更推送给 SystemUI 里的 prefs 监听器 → 快照重载，无需重启。

## 构建

```bash
./gradlew :app:assembleRelease
```

需要 Android SDK 35 + JDK 17+。已知签名可用仓库根目录外的 `keystore.jks`，
在 `app/build.gradle.kts` 里加 `signingConfigs` 即可。

## 路线图

- [ ] 自动单色化：没有自定义图标的通知，按亮度转 alpha，彻底解决「彩色小图标被涂成白块」
- [ ] 离线图标库：合并 fankes 的 AndroidNotifyIconAdapt
- [ ] 应用内通知预览（不依赖系统通知栏）
- [ ] 图标裁切 / 颜色 / 内边距编辑
- [ ] 配置导入导出（走 remote files）

## License

AGPL-3.0，与原作一致。
