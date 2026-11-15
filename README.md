# Notice Fix Rev

次世代的通知图标修复模块。原作 [NoticeFix](https://github.com/xudazhu1/NoticeFix)（AGPL-3.0）用 Java + Xposed API 82 编写，
在 Android 16/17 的 SystemUI 上大量反射失配（`NoSuchFieldException: mContext`、`ClassNotFoundError: HeadsUpManager`），
并且每帧抛异常拖垮 SystemUI。

Rev 用 **libxposed API 101 + Kotlin + Jetpack Compose（Material 3）** 重写：

* 图标来自内置的 [AndroidNotifyIconAdapt](https://github.com/fankes/AndroidNotifyIconAdapt) 图标包，**不需要手动导入图片**
* 首次启动自动适配所有已安装的匹配应用，一个「一键应用」按钮可以随时重扫
* 配置通过 libxposed **remote preferences / remote files** 跨进程传递，不需要 root，不需要 world-readable 文件
* 模块 UI 与框架通过 `libxposed:service` 通信，可实时申请作用域、读取框架状态
* 每个 hook 都包在 `ExceptionMode.PROTECTIVE` 里，失败只记日志，绝不把异常抛进 SystemUI

## 图标包

`app/src/main/assets/iconpack/` 下三份 JSON 合并使用（app 680 条 + ColorOS 53 条 + MIUI 43 条，共 772 条）：

| 字段 | 用途 |
|---|---|
| `packageName` | 匹配已安装应用 |
| `iconBitmap` | base64 编码的 46–50px 纯白剪影 PNG |
| `iconColor` | 品牌色，写进 `Notification.color` 让 SystemUI 给剪影上色 |

列表只显示图标包里有条目的应用——没有条目的应用本来也无从适配。图标包按 AGPL-3.0 分发，
许可全文随包提供在 `assets/iconpack/LICENSE-AndroidNotifyIconAdapt.txt`。

## 结构

```
app/src/main/java/com/xeasy/noticefixrev/
├── NoticeFixRevApp.kt              # Application：绑定 XposedService
├── data/
│   ├── PrefKeys.kt                 # UI / 被注入进程共用的键名
│   ├── IconPack.kt                 # assets 图标包解析（packageName → 剪影 + 品牌色）
│   └── ModuleRepository.kt         # 写配置、图标写入 remote file
├── module/                         # 只有这里会进 SystemUI 进程
│   ├── ModuleEntry.kt              # java_init.list 指向的入口
│   ├── ConfigSnapshot.kt           # 配置快照 + 变更监听
│   └── NotificationIconHook.kt     # 唯一 hook 点
└── ui/
    ├── MainActivity.kt / AppRoot.kt
    ├── AppListScreen.kt            # 图标包应用列表 + 一键应用 + 详情弹层
    ├── SettingsScreen.kt
    ├── MainViewModel.kt
    ├── components/Common.kt        # 卡片 / 分区标题 / 图标底 / M3E 加载
    └── theme/{Theme,Color,Type}.kt
```

`META-INF/xposed/` 下三个文件由框架直接读取：

| 文件 | 作用 |
|---|---|
| `module.prop` | `minApiVersion=101`、`staticScope=true`、`autoHotReload=true` |
| `java_init.list` | `moe.gbxin.noticefixrev.module.ModuleEntry` |
| `scope.list` | `com.android.systemui` |

## 工作原理

1. SystemUI 启动 → libxposed 实例化 `ModuleEntry` → `onPackageReady()` 里安装 hook。
2. Hook `com.android.systemui.statusbar.NotificationListener#onNotificationPosted`。
3. 通知到达时，如果该应用已启用且有图标包条目，就：
   * 用反射把 `Notification.mSmallIcon` 换成 `Icon.createWithBitmap(packGlyph)`
     （API 37 起 `smallIcon` 是私有字段，`setSmallIcon(Icon)` 是 `@hide`，两者都是 `@UnsupportedAppUsage` 灰名单）
   * 把 `Notification.color` 设成图标包的品牌色，让 SystemUI 给白色剪影上色
4. 模块 UI 任意时刻改配置 → 框架把变更推送给 SystemUI 里的 prefs 监听器 → 快照重载，无需重启。

## 设计规范

UI 与 **SaltReader**（`PolarisSdesu/SaltReader`）保持同一套视觉语言，直接对齐而非近似：

| 维度 | 取值 |
|---|---|
| 主题 | `MaterialExpressiveTheme` + `MotionScheme.expressive()` |
| 主色 | M3 蓝种子 `#0061A4` 全量 light/dark color scheme（含 surfaceContainer 系列） |
| 圆角 | `Shapes` 4 / 8 / 12 / 16 / 28 dp |
| 字阶 | M3 baseline 按中文可读性放大（headline 34/42、title 24/30、body 15/22） |
| 顶栏 | `LargeFlexibleTopAppBar` + `exitUntilCollapsedScrollBehavior`，背景 `background`，滚动后 `surfaceContainer` |
| 卡片 | `surfaceContainerLow` + `extraLarge`(28dp) 圆角 |
| 图标 | 40dp 圆形 `primaryContainer` 底板 |
| 加载 | M3E `LoadingIndicator` |
| 边到边 | `Scaffold(contentWindowInsets = 0)`，列表自行处理手势条 inset |
| 深色 | 动态取色优先，可切 AMOLED 纯黑 |

字符串同步维护 `values` / `values-zh-rCN` / `values-zh-rTW` 三套。

## 构建

```bash
./gradlew :app:assembleRelease
```

工具链与 SaltReader 完全一致：JDK 21、Gradle 9.7.1、AGP 9.4.0、Kotlin 2.3.21、
`compileSdk 37` / `targetSdk 36` / `minSdk 29`，material3 固定 `1.5.0-beta02`（Expressive）。

* AGP 9 内置 Kotlin，需要 `android.builtInKotlin=false` + `android.newDsl=false` 才能用外部 KGP 2.3.21
* 签名读 git-ignored 的 `keystore.properties`（`storeFile` / `storePassword` / `keyAlias` / `keyPassword`），
  没有该文件时 release 保持未签名

## 安装注意

LSPosed 在 `pm install -r` 更新 APK 后会把 `modules_state.enabled` 重置为 0，需要重新启用并重启设备。

## 路线图

- [x] 离线图标库：合并 fankes 的 AndroidNotifyIconAdapt
- [x] 一键应用 + 只显示图标包覆盖的应用
- [ ] 自动单色化：图标包里没有的应用，按亮度转 alpha，解决「彩色小图标被涂成白块」
- [ ] 应用内通知预览（不依赖系统通知栏）
- [ ] 图标包在线更新（上游已迁移到 `BetterAndroid/android-notification-icon-project`）
- [ ] 配置导入导出（走 remote files）

## License

AGPL-3.0，与原作及图标包一致。
