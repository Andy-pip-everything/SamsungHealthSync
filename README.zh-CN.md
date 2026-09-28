# SHealthSync — 三星健康同步

**将三星健康中的当天数据直接上传到你自己的服务器。**

[English](README.md) | 简体中文

SHealthSync 是一个轻量 Android 应用，通过三星官方的 **Samsung Health Data SDK** 读取每日健康汇总数据，并以 JSON 格式上传到你指定的 URL。支持手动上传和每日后台定时上传。

项目源于国行三星手机上的一个实际问题：数据已经存在于三星健康中，但依赖 Health Connect 读取数据的流程（包括 Tasker 插件）无法获取这些数据。SHealthSync 提供从三星健康直达服务器的数据通路，无需依赖三星健康先把数据同步到 Health Connect。

```text
三星健康 → Samsung Health Data SDK → SHealthSync → 你的 HTTP(S) 接口
```

本应用不读取或写入 Health Connect。它在获得授权后读取三星健康中的现有数据，不修改这些数据。

## 功能

- 点击 **Sync now**，上传当天可读取的健康汇总数据。
- 自定义本地时间，每天自动上传一次。
- 自定义 POST URL，可选 Bearer token 认证。
- 上传前预览 JSON 数据。
- 查看最近一次上传的 HTTP 状态、发送内容和服务器响应。
- 无需 Tasker 或 Health Connect 插件。

当前应用界面为英文。应用版本为 **0.2**（`versionCode = 2`）；包名沿用最初探针项目的 `com.example.shealthprobe`。

## 使用条件与兼容性

- 运行 **Android 10 / API 29 或更高版本**的实体手机。
- 安装 **三星健康 6.30.2 或更高版本**，且需要导出的数据已同步到手机。
- 具备 Samsung Health Data SDK 访问条件，并授予所需的读取权限。
- 一个能够接收下述数据格式的 HTTP(S) 接口。本项目不包含后端。

三星官方 SDK 文档说明支持三星及其他品牌的 Android 手机，不支持模拟器。本项目针对国行三星场景开发，尚未逐一验证所有机型、地区和三星健康版本的兼容性。参见[三星 SDK 使用条件](https://developer.samsung.com/health/data/guide/introduction.html)。

本项目的开发测试流程使用**三星健康的数据读取开发者模式**，它与 Android 系统的 USB 调试设置不同。三星将该模式定位为测试／调试功能；常规合作伙伴访问与注册的应用包名、签名证书关联。参见[三星开发者模式文档](https://developer.samsung.com/health/data/guide/developer-mode.html)。仅安装 APK 不会自动获得健康数据访问权限。

## 开始使用

1. 安装通过正常构建／导出方式生成的 APK，或按下文从源码构建。
2. 打开三星健康，确认需要的数据已经存在。如果数据来自手表，先让手表同步到手机。
3. 开发测试时，按上述官方文档启用三星健康的数据读取开发者模式。
4. 打开 **Samsung Health Sync**，点击 **Grant Samsung Health permissions**，授予全部请求的权限。
5. 填写完整的 POST URL。如服务器需要认证，在 Bearer token 输入框中仅填写 token，不要添加 `Bearer ` 前缀。
6. 点击 **Save settings** 保存，然后点击 **Preview today's payload** 检查数据。
7. 点击 **Sync now**，等待几秒后点击 **Refresh last result**。应用将 `2xx` 响应视为上传成功。
8. 如需每日上传，勾选 **Enable daily automatic sync**，选择本地时间，再点击 **Save settings**。默认时间为 **21:30**；自动同步默认关闭。

如果服务器运行在电脑上，请填写手机能够访问的地址，例如：

```text
http://192.168.1.20:5173/
```

这只是示例，接口路径并未写死。手机上的 `localhost` 指手机自身。局域网测试时，确认服务器监听可访问的网络接口，并允许相应防火墙连接。部署后的接口请使用 HTTPS。

### 权限说明

应用请求读取步数、活动汇总、睡眠、爬楼层数、饮水、营养和心率这七类数据。

**当前版本的预览、手动同步和定时同步都要求授予全部七类权限。** 虽然不会上传心率字段，但现有权限检查仍要求心率读取权限。通过初始权限检查之后，各项指标的读取失败会分别处理。

## 数据与接口约定

每次上传包含手机当前本地日期的数据快照。大部分指标的读取范围是本地时间零点至读取时刻；睡眠使用 SDK 按当前本地日期提供的汇总结果。

| 字段 | 含义 | 单位／格式 |
| --- | --- | --- |
| `date` | 手机当前本地日期 | `YYYY-MM-DD` |
| `source` | 固定来源标识 | `"samsung"` |
| `steps` | 汇总步数 | 步 |
| `distance_m` | 活动汇总中的总距离 | 米 |
| `active_minutes` | 总活动时长，转换为整数分钟 | 分钟 |
| `active_energy` | 总活动消耗热量 | 千卡（kcal） |
| `sleep_hours` | 当前日期的睡眠时长，由整数分钟换算 | 小时 |
| `floors` | 汇总爬楼层数 | 层 |
| `water_ml` | 汇总饮水量 | 毫升 |
| `sugar_g` | SDK 返回的营养记录中糖含量之和 | 克 |

只要生成了数据包，就会包含 `date` 和 `source`。某项指标读取抛出异常时，该字段会被省略。读取成功但返回空结果时，求和结果为 **0**，因此零值不一定代表实际测得的数值。数据包不会列出各项指标的读取错误。

当前版本未实现 `resting_hr` 和 `caffeine_mg`，也不导出原始心率采样、运动记录或历史日期数据。

以下数值仅用于展示格式：

```json
{
  "date": "2026-09-29",
  "source": "samsung",
  "steps": 8240,
  "distance_m": 6120.5,
  "active_minutes": 63,
  "active_energy": 384.2,
  "sleep_hours": 7.5,
  "floors": 8.0,
  "water_ml": 1800.0,
  "sugar_g": 32.4
}
```

请求格式：

```http
POST /your-endpoint
Content-Type: application/json; charset=utf-8
Authorization: Bearer <your-token>
```

token 为空时不发送 `Authorization` 请求头。连接超时和读取超时均为 15 秒；HTTP `200–299` 视为成功；界面最多显示响应正文的 800 个字符。应用不要求服务器返回特定的 JSON 结构。

### 服务端应如何处理

服务器应接受部分字段，并更新当前认证用户对应的 `(date, source)` 每日记录。**对本次提供的每日汇总值执行覆盖更新，不要叠加到旧值上。** 多次上传会包含重叠的数据快照，失败后也可能重复请求。新数据中未出现的字段应保留原值，同时明确制定对零值的处理方式。

JSON 不包含用户／设备标识，也没有幂等键。如果服务端接收多个用户的数据，需要通过认证信息或不同接口确定数据归属。

## 定时机制与限制

- 使用 Android WorkManager，为下一个指定的本地时间创建一次性任务。保存设置会替换待执行的每日任务；每次尝试后，如自动同步仍开启，工作任务会安排下一次每日任务。
- Android 后台和电池管理可能推迟执行，因此不保证准点，也不是持续实时同步。
- 每次执行读取的是**实际执行当天**的数据。如果任务延迟到午夜之后，会读取新一天的数据，不会补传前一天。21:30 的上传也不包含当晚此后产生的数据，除非午夜前再次上传。
- URL 为空或权限缺失时，本次尝试直接失败。HTTP 错误和异常会请求 WorkManager 重试，但每日调度会替换同名任务，因此不保证持续重试直至成功送达。
- 没有用于保存待发送数据包的持久队列。每次重新执行都会重新读取当时的数据。

## 隐私与本地存储

应用向你配置的 URL 发送健康数据。URL 和 token 初始均为空，应用代码没有配置固定的上传服务器。

设置、token 和最近一次同步状态保存在应用私有的 **SharedPreferences 中，没有额外加密**。保存的状态可能包含健康数据和服务器响应。Manifest 开启了 Android 备份。为了局域网测试，应用允许明文 HTTP；在可信测试网络之外传输真实数据时请使用 HTTPS，分享状态截图或日志前请移除个人数据。

## 从源码构建

使用 Android Studio 打开包含 `settings.gradle.kts` 的项目目录。IDE 中的项目名称可能显示为 **SHealthProbe**。

当前源码配置如下：

| 组件 | 版本／设置 |
| --- | --- |
| Samsung Health Data SDK | `1.1.0` |
| Android Gradle Plugin | `8.7.3` |
| Kotlin | `2.0.21` |
| Gradle wrapper | `9.3.0` |
| Java／Kotlin 字节码目标 | `17` |
| 编译／目标 SDK | `35` |
| 最低 SDK | `29` |

确认以下位置存在三星 SDK 库：

```text
app/libs/samsung-health-data-api-1.1.0.aar
```

提供的源码文件夹中已包含该 AAR。如果下载的仓库中没有，请从 [Samsung Developer](https://developer.samsung.com/health/data/overview.html) 获取对应版本。安装 Android SDK Platform 35，并在 Android Studio 中配置兼容的 Gradle JDK。

上表反映提供的文件，并不表示已经重新验证构建环境。该源码快照搭配了 AGP 8.7.3 和 Gradle 9.3.0；如果同步失败，请先检查版本兼容性。Android 的 [AGP 8.7 文档](https://developer.android.com/build/releases/past-releases/agp-8-7-0-release-notes)列出的基准是 Gradle 8.9 和 JDK 17。本文档未修改工具链配置。

Gradle 同步成功后，在项目根目录构建 debug APK：

```powershell
# Windows PowerShell
.\gradlew.bat assembleDebug
```

```sh
# macOS / Linux
bash ./gradlew assembleDebug
```

常规输出位置为 `app/build/outputs/apk/debug/app-debug.apk`。如需用于分发的 APK，在 Android Studio 中选择 **Build → Generate Signed Bundle / APK → APK**，选择 release 变体，并妥善保留签名密钥用于后续更新。参见 [Android 签名指南](https://developer.android.com/studio/publish/app-signing)。

通过 Android Studio 的 **Run** 操作生成的 APK 可能带有 `testOnly` 标记，需要通过 ADB 安装。供他人下载的 APK 应通过构建／导出操作生成，参见 [Android APK 构建文档](https://developer.android.google.cn/build/build-for-release?hl=en)。

## 常见问题

| 现象 | 排查方向 |
| --- | --- |
| 请求权限失败 | 检查三星健康版本、SDK 访问条件／开发者模式及读取权限。 |
| 为什么需要心率权限 | 当前全部权限检查包含心率，虽然不上传心率数据。 |
| 某个字段缺失或为零 | 确认三星健康中已有数据、手表已同步；读取失败会省略字段，空结果可能得到零值。 |
| 无法连接服务器 | 检查 URL、手机到服务器的网络、防火墙，以及是否误用了 `localhost`。 |
| HTTP `401` 或 `403` | 检查 token 和服务端认证配置；token 输入框不应包含 `Bearer `。 |
| 定时上传延迟或未执行 | 检查自动同步开关、保存的时间、电池／后台限制和最近结果。 |
| 界面仍显示上一次结果 | 等待排队任务执行后，点击 **Refresh last result**。 |
| 下载的 APK 无法安装 | 检查是否为 test-only 包，以及签名证书是否与已安装版本一致。 |

## 致谢与许可

三星健康数据访问由三星官方 **Samsung Health Data SDK** 提供。SHealthSync 是独立项目，并非三星官方同步服务。

提供的源码快照未包含项目 `LICENSE` 文件。本 README 不为应用或三星 SDK 指定许可证。SDK 及任何复用的三星材料仍受其适用条款约束；准备公开仓库时请保留原始声明。
