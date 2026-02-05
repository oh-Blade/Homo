# Gradle 相关文件说明

## 一、文件清单与完整性

| 文件 | 状态 | 说明 |
|------|------|------|
| **build.gradle.kts**（根目录） | ✅ 完整 | 声明 Android、Kotlin 插件版本（apply false） |
| **settings.gradle.kts** | ✅ 完整 | 仓库、pluginManagement、include(":app")、rootProject.name |
| **app/build.gradle.kts** | ✅ 完整 | 应用插件、android 配置、依赖 |
| **gradle.properties** | ✅ 完整 | JVM 参数、AndroidX、Kotlin 风格、nonTransitiveRClass |
| **gradle/wrapper/gradle-wrapper.properties** | ✅ 完整 | 指定 Gradle 版本与分发 URL（如 8.13） |
| **gradlew** | ✅ 已补全 | Unix/macOS/Linux 下命令行构建脚本 |
| **gradlew.bat** | ✅ 已补全 | Windows 下命令行构建脚本 |
| **gradle/wrapper/gradle-wrapper.jar** | ⚠️ 需自备 | Wrapper 引导 JAR，脚本依赖此文件才能运行 |

---

## 二、各文件作用简述

### 根目录 build.gradle.kts

- 配置项目级插件：`com.android.application`、`org.jetbrains.kotlin.android`，版本与当前 Gradle 兼容即可。

### settings.gradle.kts

- **pluginManagement**：插件解析仓库（google、mavenCentral、gradlePluginPortal）。
- **dependencyResolutionManagement**：依赖解析仓库（google、mavenCentral），`FAIL_ON_PROJECT_REPOS` 保证统一从根配置拉依赖。
- **rootProject.name**：Gradle 项目名（如 "Homo"）。
- **include(":app")**：包含 app 模块。

### app/build.gradle.kts

- **plugins**：应用 Android 应用插件、Kotlin Android 插件。
- **android**：namespace、compileSdk、minSdk、targetSdk、Java 17、Compose、buildTypes、proguard 等。
- **dependencies**：Compose BOM、UI、Material3、Activity Compose、ViewModel、Retrofit、Gson、OkHttp、DataStore、Coil 等。

### gradle.properties

- **org.gradle.jvmargs**：JVM 堆等参数。
- **android.useAndroidX=true**：使用 AndroidX。
- **kotlin.code.style=official**：Kotlin 代码风格。
- **android.nonTransitiveRClass=true**：按模块生成 R，减小依赖。

### gradle-wrapper.properties

- **distributionUrl**：Gradle 分发包地址（如 `gradle-8.13-bin.zip`），与根 build.gradle.kts 中插件版本匹配即可。

### gradlew / gradlew.bat

- 调用 `gradle/wrapper/gradle-wrapper.jar`，根据 `gradle-wrapper.properties` 下载并运行对应 Gradle，实现无需本机安装 Gradle 的构建。

---

## 三、gradle-wrapper.jar 缺失时的处理

若仓库中**没有** `gradle/wrapper/gradle-wrapper.jar`，直接运行 `./gradlew` 或 `gradlew.bat` 会报错（找不到 jar）。

**做法一（推荐）**：在本机已安装 Gradle 的前提下，在项目根目录执行：

```bash
cd homo_android
gradle wrapper --gradle-version 8.13
```

会生成/更新 `gradlew`、`gradlew.bat` 和 `gradle/wrapper/gradle-wrapper.jar`（以及 `gradle-wrapper.properties` 若需更新）。生成后建议将 `gradle/wrapper/gradle-wrapper.jar` 一并提交到版本控制。

**做法二**：从任一使用同版本 Gradle 的 Android 项目中，将 `gradle/wrapper/gradle-wrapper.jar` 复制到本项目的 `gradle/wrapper/` 下。

**说明**：在 Android Studio 中打开项目时，IDE 使用自带的 Gradle 与 wrapper 配置，不依赖本仓库里的 `gradlew` 或 `gradle-wrapper.jar`，因此即使缺少 jar，在 IDE 里仍可正常同步与运行。只有需要在**命令行**执行 `./gradlew assembleDebug` 等时，才必须补全 `gradle-wrapper.jar`（以及已有 `gradlew`/`gradlew.bat`）。

---

## 四、结论

- **配置层面**：Gradle 相关配置（根 build、settings、app build、gradle.properties、wrapper 属性）已齐全且一致。
- **脚本**：已补全 `gradlew`、`gradlew.bat`，命令行构建可用（在补全 `gradle-wrapper.jar` 的前提下）。
- **可选**：若只使用 Android Studio 构建，可不补 jar；若需命令行构建或希望仓库“开箱即用”，请按上文补全 `gradle-wrapper.jar` 并提交。
