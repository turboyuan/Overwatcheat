# Overwatcheat 项目说明文档

> **版本**: 5.1.0  
> **作者**: Thomas G. Nappo ([@Jire](https://github.com/Jire))  
> **许可证**: AGPL-3.0  
> **语言**: Kotlin 1.8.0 / JVM 19  

---

## 一、项目概述

Overwatcheat 是一个针对《守望先锋（Overwatch）》游戏的**开源颜色辅助瞄准程序（Color Aimbot）**。

**工作原理**：
1. 使用 FFmpeg GDI 截取游戏窗口中心区域的实时画面帧
2. 扫描帧内像素，识别血量条特有的**品红色（Magenta）**像素
3. 计算目标相对屏幕中心的位移（Delta X/Y）
4. 通过 **Interception 内核驱动**注入鼠标移动事件，绕过游戏的输入检测，实现自动瞄准

**核心特性**：
- 极低 CPU/内存占用，对游戏帧率影响极小
- 零垃圾（Zero-GC）像素扫描器
- 使用 Interception 驱动注入，规避鼠标事件检测
- 支持 OBS 全屏投影窗口模式（规避颜色封禁 error 5）
- 追踪（Tracking）和甩枪（Flicking）两种瞄准模式

---

## 二、系统要求

| 要求 | 说明 |
|------|------|
| 操作系统 | Windows（仅限） |
| JDK | 19 或更高版本 |
| 驱动 | 已安装 [Interception 驱动](https://github.com/oblitum/Interception) |
| 游戏模式 | 全屏窗口模式（或使用 OBS 全屏投影） |
| 灵敏度 | 游戏内灵敏度需与配置文件保持一致 |

---

## 三、快速开始

### 构建

```bat
build.bat
```

双击或运行 `build.bat`，Gradle 会自动完成编译、打包，并将可运行程序输出到 `build/overwatcheat/` 目录。

### 运行

进入 `build/overwatcheat/` 目录，运行：

```bat
run.bat
```

---

## 四、项目结构

```
Overwatcheat/
├── src/main/kotlin/org/jire/overwatcheat/
│   ├── Main.kt                  # 程序入口
│   ├── FastRandom.kt            # xor-shift 高性能随机数
│   ├── Keyboard.kt              # 键盘事件注入（Interception）
│   ├── Mouse.kt                 # 鼠标事件注入（Interception）
│   ├── Screen.kt                # 屏幕分辨率信息
│   │
│   ├── aimbot/                  # 自动瞄准核心逻辑
│   │   ├── AimBotState.kt       # 线程间共享状态（volatile）
│   │   ├── AimBotThread.kt      # 瞄准主循环线程
│   │   ├── AimColorMatcher.kt   # O(1) 颜色哈希匹配器
│   │   ├── AimFrameHandler.kt   # 帧像素扫描处理器
│   │   ├── AimMode.kt           # 瞄准模式枚举（追踪/甩枪）
│   │   └── ToggleUIThread.kt    # 游戏 UI 切换线程
│   │
│   ├── framegrab/               # 屏幕帧捕获
│   │   ├── FrameGrabber.kt      # FFmpeg/gdigrab 帧抓取器
│   │   ├── FrameGrabberThread.kt# 帧抓取线程（最高优先级）
│   │   ├── FrameHandler.kt      # 帧处理接口
│   │   └── FrameWindowFinder.kt # 游戏窗口查找
│   │
│   ├── nativelib/               # Windows 原生库封装
│   │   ├── NativeLib.kt         # 原生库接口
│   │   ├── DirectNativeLib.kt   # JNA 直接绑定基类
│   │   ├── Kernel32.kt          # 进程优先级设置
│   │   ├── User32.kt            # 窗口枚举（JNA）
│   │   ├── User32Panama.kt      # 按键状态检测（Panama FFI）
│   │   └── interception/        # Interception 驱动封装
│   │       ├── Interception.kt  # 驱动上下文与发送接口
│   │       ├── InterceptionFilter.kt
│   │       ├── InterceptionKeyState.kt
│   │       ├── InterceptionMouseFlag.kt
│   │       └── InterceptionStroke.kt
│   │
│   ├── settings/                # 配置文件解析
│   │   ├── Setting.kt           # 配置项接口
│   │   ├── ConfiguredSetting.kt # 自动注册基类
│   │   ├── Settings.kt          # 所有配置项声明与读取
│   │   ├── BooleanSetting.kt
│   │   ├── DoubleSetting.kt
│   │   ├── FloatSetting.kt
│   │   ├── IntSetting.kt
│   │   ├── LongSetting.kt
│   │   ├── StringSetting.kt
│   │   ├── IntArraySetting.kt
│   │   └── HexIntArraySetting.kt
│   │
│   └── util/                    # 通用工具
│       ├── FastAbs.kt           # 无分支快速绝对值（位运算）
│       ├── PreciseSleeper.kt    # 纳秒级高精度睡眠
│       └── Threads.kt           # CPU 线程信息检测
│
├── gradle-build/                # 自定义 Gradle 构建插件
│   ├── versions/                # 版本常量插件
│   ├── settings/                # Settings 插件（依赖管理）
│   └── projects/                # Kotlin JVM 项目插件
│
├── overwatcheat.cfg             # 用户配置文件
├── build.bat                    # 一键构建脚本
└── build.gradle.kts             # 主构建脚本
```

---

## 五、运行时架构

程序启动后运行三条并发线程：

```
主线程 (Main)
  │
  ├── FrameGrabberThread  [Thread.MAX_PRIORITY]
  │     FFmpeg gdigrab → 截取屏幕中心区域
  │        └─→ AimFrameHandler → 像素扫描
  │                └─→ AimBotState.aimData (volatile Long，位压缩存储坐标)
  │
  ├── AimBotThread  [MAX_PRIORITY - 1，CPU 亲和性绑定]
  │     轮询 aimKey 按下状态
  │        └─→ 解码 aimData → 计算 dX/dY（含灵敏度和抖动）
  │                └─→ Mouse.move() via Interception 驱动（内核层注入）
  │
  └── ToggleUIThread  [普通优先级]
        监控 toggleUI 标志
           └─→ Keyboard.pressKey(ALT+Z) via Interception 驱动
                  （切换游戏 HUD 以提升颜色扫描准确性）
```

**关键数据流：`aimData` Long 编码格式**

```
Bits [63:48] = xLow   (目标 X 最小值)
Bits [47:32] = xHigh  (目标 X 最大值)
Bits [31:16] = yLow   (目标 Y 最小值)
Bits [15:0]  = yHigh  (目标 Y 最大值)
```

---

## 六、颜色识别原理

### 初始化阶段

`AimColorMatcher.initializeMatchSet()` 将 `target_colors` 配置中的每个基础颜色，按 `target_color_tolerance` 容差在 R/G/B 三个通道各自±展开，预先构建一个 `IntOpenHashSet`（fastutil）。

### 运行时匹配

每帧扫描时，`AimFrameHandler` 对每个像素直接调用 `matchSet.contains(rgb)`，时间复杂度 O(1)，无分支判断。

### 目标坐标计算

扫描完整帧后，记录所有匹配像素的 xLow/xHigh/yLow/yHigh，取目标矩形中心，与捕获区域中心做差，得到瞄准偏移量 dX/dY。

---

## 七、配置文件说明

配置文件为 `overwatcheat.cfg`，位于程序运行目录下。

### 基础控制

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `aim_key` | `1` | 激活瞄准的虚拟键码（1 = 鼠标左键） |
| `aim_mode` | `0` | 瞄准模式：`0` = 追踪（Tracking），`1` = 甩枪（Flicking） |
| `sensitivity` | `10.0` | 灵敏度（必须与游戏内设置一致） |
| `fps` | `60` | 帧捕获速率（越高越精准，CPU 消耗越大） |

### 瞄准参数

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `aim_duration_millis` | `0.78` | 每次瞄准周期时长（ms），值越低越快 |
| `aim_duration_multiplier_base` | `0.97` | 睡眠时间基础乘数，值越高越流畅 |
| `aim_duration_multiplier_max` | `0.6` | 睡眠时间最大乘数 |
| `aim_max_move_pixels` | `3` | 每帧最大移动像素（甩枪模式建议 10+） |
| `aim_jitter_percent` | `64` | 随机抖动百分比（模拟人类行为，0 = 完美精度） |
| `aim_min_target_width` | `1` | 目标最小宽度（过滤噪点，越低越灵敏） |
| `aim_min_target_height` | `1` | 目标最小高度 |
| `aim_offset_x` | `1.1083` | X 轴瞄准偏移（基于 1440p，自动缩放） |
| `aim_offset_y` | `0.93` | Y 轴瞄准偏移 |

### 扫描区域

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `box_width` | `64` | 屏幕中心扫描区域宽度（像素） |
| `box_height` | `32` | 屏幕中心扫描区域高度（像素） |
| `max_snap_divisor` | `0.71` | 最大捕捉距离 = box尺寸 ÷ 此值 |

### 颜色识别

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `target_colors` | 大量品红色 hex | 目标颜色列表（十六进制 RGB，逗号分隔） |
| `target_color_tolerance` | `19` | 颜色容差（越大越宽松，误判风险越高） |

### 甩枪模式

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `flick_shoot_pixels` | `5` | 触发射击的最大像素距离 |
| `flick_pause_duration` | `200` | 两次射击间隔（ms），McCree=200，Ashe=350，Widow=270 |

### 窗口与设备

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `window_title_search` | `Overwatch` | 游戏窗口标题关键词，支持 OBS 投影 |
| `mouse_id` | `11` | Interception 鼠标设备 ID（范围 11~20） |
| `keyboard_id` | `1` | Interception 键盘设备 ID（范围 1~10） |

### UI 切换

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `toggle_in_game_ui` | `true` | 按下瞄准键时是否切换游戏 HUD |
| `toggle_key_codes` | `12,5A` | 切换 UI 的按键组合（十六进制，默认 ALT+Z） |

### 性能调优

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `aim_precise_sleeper_type` | `0` | 精确睡眠模式：`0`=YIELD（推荐），`1`=SPIN_WAIT（最精准高CPU），`2`=SLEEP（低CPU高抖动） |
| `aim_cpu_thread_affinity_index` | `2` | 瞄准线程绑定的 CPU 核心索引，`-1` 禁用；建议绑定物理核心（偶数编号），避免绑定 0 号核 |

---

## 八、依赖库

| 库 | 版本 | 用途 |
|----|------|------|
| Kotlin | 1.8.0 | 主要开发语言 |
| `org.bytedeco:javacv-platform` | 1.5.8 | FFmpeg Java 封装，gdigrab 屏幕捕获 |
| `net.java.dev.jna` + `jna-platform` | 5.12.1 | Windows API 绑定（窗口枚举） |
| `it.unimi.dsi:fastutil` | 8.5.11 | 高性能原始类型集合（颜色哈希匹配） |
| `net.openhft:affinity` | 3.23.2 | CPU 线程亲和性绑定 |
| `net.openhft:chronicle-core` | 2.24ea3 | JVM 底层工具 |
| `org.slf4j:slf4j-api` + `slf4j-simple` | 2.0.6 | 日志框架 |

---

## 九、构建系统

项目使用 Gradle Kotlin DSL，包含三个自定义插件（位于 `gradle-build/`）：

| 插件 ID | 类 | 作用 |
|---------|----|------|
| `overwatcheat-versions` | `VersionsPlugin` | 提供 Kotlin 版本常量 |
| `overwatcheat-settings` | `SettingsPlugin` | 配置 Maven Central 仓库与版本目录（Version Catalog） |
| `overwatcheat-kotlin-project` | `KotlinProjectPlugin` | 应用 `kotlin.jvm` 插件 |

**构建产物**（`build/overwatcheat/`）：

```
overwatcheat/
├── overwatcheat.jar    # Fat JAR（含所有依赖）
├── run.bat             # 启动脚本（含完整 JVM 参数）
├── overwatcheat.cfg    # 配置文件
├── LICENSE.txt
└── README.md
```

**JVM 启动参数**：

```
-Xmx4g -Xms1g
-XX:+UnlockExperimentalVMOptions -XX:+UseZGC
--enable-native-access=ALL-UNNAMED
--add-opens=java.base/java.lang=ALL-UNNAMED
--add-opens=java.base/java.lang.reflect=ALL-UNNAMED
--add-opens=java.base/java.io=ALL-UNNAMED
--add-opens=java.base/java.time=ALL-UNNAMED
--add-exports=java.base/sun.nio.ch=ALL-UNNAMED
--add-exports=jdk.unsupported/sun.misc=ALL-UNNAMED
```

---

## 十、关键技术实现

### 反检测：Interception 内核驱动

所有鼠标移动和键盘事件均通过 **Interception 驱动**（`interception.dll`）在内核层注入，而非使用 Win32 的 `SendInput`/`mouse_event` API。游戏的反作弊层无法区分其与真实硬件输入。

### 零 GC 设计

- `FastRandom`：基于 xor-shift 算法，避免 `java.util.Random` 产生 GC 对象
- `AimColorMatcher`：预计算展开整个颜色匹配集合，运行时零分配
- `FastAbs`：使用位运算（符号掩码异或）代替 `Math.abs()`，无分支

### 高精度计时

`PreciseSleeper` 枚举提供纳秒级精确睡眠，在 `System.nanoTime()` 自旋循环中使用可配置的等待策略（`yield`/`onSpinWait`/`sleep`），保证瞄准周期的时序精度。

### CPU 亲和性绑定

`AimBotThread` 使用 `AffinityLock` 将自身绑定到指定 CPU 物理核心，避免线程调度抖动，提升瞄准响应一致性。

### Panama FFI

`User32Panama` 和 `Interception` 使用 Java 19 的 Panama Foreign Function & Memory API（而非传统 JNI/JNA）直接调用 `User32.dll` 和 `interception.dll`，具有更低的调用开销。

---

## 十一、OBS 投影模式

若游戏触发颜色封禁（error 5），可改用 OBS 全屏投影窗口作为捕获源：

1. 在 OBS 中右键场景 → 全屏投影（Fullscreen Projector）
2. 将配置文件中 `window_title_search` 改为 `Fullscreen Projector`

---

## 十二、版权

```
Free, open-source undetected color cheat for Overwatch!
Copyright (C) 2017  Thomas G. Nappo

本程序遵循 GNU Affero General Public License v3.0 授权发布。
详见 LICENSE.txt 或 <http://www.gnu.org/licenses/>。
```
