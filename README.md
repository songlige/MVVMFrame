# GameBoosterProject 开发文档

## 1. 项目目标

当前项目已完成基础架构搭建，采用 MVVM 模式，并封装了基于 Android 原生能力的网络请求框架（`HttpURLConnection`），用于后续业务快速扩展。

---

## 2. 架构设计

采用经典分层架构：`presentation -> domain -> data -> core`。

### 2.1 presentation（表现层）

- 负责 UI 展示与用户交互（Jetpack Compose）。
- 通过 `ViewModel` 管理页面状态，避免将业务逻辑写入 UI。
- 当前示例：
    - `HomeScreen.kt`
    - `HomeViewModel.kt`
    - `HomeUiState.kt`

### 2.2 domain（领域层）

- 定义核心业务模型与业务规则。
- 不依赖 Android 具体实现，便于测试与复用。
- 当前示例：
    - `Todo.kt`（领域实体）
    - `TodoRepository.kt`（仓库接口）
    - `GetTodoUseCase.kt`（业务用例）

### 2.3 data（数据层）

- 负责远程/本地数据的获取与转换。
- 实现 `domain` 层定义的仓库接口。
- 当前示例：
    - `TodoApiService.kt`（远程接口封装）
    - `TodoDto.kt`（数据传输对象）
    - `TodoRepositoryImpl.kt`（仓库实现）

### 2.4 core（基础能力层）

- 提供通用能力：网络、依赖注入、通用模型。
- 当前示例：
    - `HttpClient.kt`（原生网络请求封装）
    - `HttpRequest.kt`（单次请求快照、`HttpInterceptor`、`HttpLogger` 声明）
    - `NetworkResult.kt`（统一结果模型）
    - `NetworkException.kt`（统一异常模型）
    - `ServiceLocator.kt`（轻量依赖注入）

---

## 3. MVVM 调用链路

页面点击请求后的完整链路如下：

1. `HomeScreen` 触发 `onLoadClick`
2. `HomeViewModel.loadTodo()`
3. 调用 `GetTodoUseCase`
4. 调用 `TodoRepository` 接口
5. `TodoRepositoryImpl` 转发到 `TodoApiService`
6. `TodoApiService` 使用 `HttpClient` 发起网络请求
7. 返回 `NetworkResult` 给 ViewModel
8. ViewModel 更新 `HomeUiState`，UI 自动响应渲染

---

## 4. 原生网络框架说明

## 4.1 设计目标

- 不依赖 Retrofit/OkHttp，采用系统原生 `HttpURLConnection`。
- 统一网络成功/失败模型，减少业务层 `try-catch` 嵌套。
- 支持协程与 IO 线程切换，避免阻塞主线程。

### 4.2 核心能力

- **HTTP 方法**：`get` / `post` / `put` / `delete` / `patch`；带 body 的方法支持可选请求体与 `bodyContentType`（默认 `application/json; charset=UTF-8`）。
- **Query / Header**：各方法支持 `queryParams: Map<String, String>?` 与 `headers: Map<String, String>?`；URL 已含 `?` 时用 `&` 追加参数，且保证 `#fragment` 不被破坏。
- **连接头顺序**（与 `HttpURLConnection` 设置顺序一致）：先 `Accept: application/json`，若有请求体再 `Content-Type`，最后应用业务 `headers`（可覆盖前述默认值）。
- **拦截器**：构造 `HttpClient` 时传入 `List<HttpInterceptor>`；请求前将调用参数收拢为 `HttpRequest`，经各拦截器依次 `intercept` 后再真正发起连接，便于注入 Token、公共 Query 等。
- **调试日志**：可选 `HttpLogger`；若注入，会打印合并后的请求信息与响应（敏感头 `Authorization`、`Cookie` 打码；响应体过长时截断）。`ServiceLocator` 在 **Debug** 构建（`BuildConfig.DEBUG`）下默认注入基于 `android.util.Log` 的实现。
- **PATCH 兼容**：部分系统对 `setRequestMethod("PATCH")` 受限时，使用反射回退以发送 PATCH（见 `HttpClient` 实现）。
- 自定义超时：
    - `connectTimeoutMs`（连接超时）
    - `readTimeoutMs`（读取超时）
- 统一解析流程：
    - 2xx：读取 `inputStream` 并解析
    - 非 2xx：读取 `errorStream` 并包装 `HttpError`

### 4.3 返回模型

- `NetworkResult.Success<T>`：请求成功并携带业务数据
- `NetworkResult.Error`：请求失败并携带异常类型

异常类型：

- `NetworkException.HttpError(code, body)`：HTTP 状态码异常
- `NetworkException.ParseError(raw, error)`：数据解析失败
- `NetworkException.ConnectionError(error)`：连接失败或未知网络异常

---

## 5. 当前已实现功能

当前项目除基础架构外，已经具备一套可运行的最小业务闭环，适合作为后续功能开发模板。

### 5.1 应用启动与页面装载

- 启动入口为 `MainActivity`。
- 进入应用后使用 `GameBoosterProjectTheme` 渲染界面。
- 首页通过 `Scaffold` 承载主内容区域，并启用 `EdgeToEdge`。
- `HomeViewModel` 通过 `ServiceLocator.homeViewModelFactory` 注入，避免页面直接 new 依赖。

### 5.2 首页交互能力

首页当前提供一个“请求远程数据”按钮，对应以下功能：

- 初始文案为“点击加载远程任务”；
- 点击按钮后进入 `loading` 状态；
- `loading` 期间按钮禁用，避免重复请求；
- 页面显示 `CircularProgressIndicator` 提示用户正在加载；
- 请求成功后展示 Todo 内容，格式为：`#id title（完成：true/false）`；
- 请求失败后展示错误文案，并使用错误色高亮显示。

### 5.3 状态管理能力

首页状态由 `HomeUiState` 统一维护，当前包含：

- `loading`：控制按钮可用状态与加载指示器显示；
- `todoText`：控制首页主文案；
- `errorMessage`：控制错误提示展示与隐藏。

`HomeViewModel` 使用：

- `MutableStateFlow` 持有内部状态；
- `StateFlow` 对外暴露只读状态；
- `collectAsStateWithLifecycle()` 在 Compose 页面中安全订阅状态。

### 5.4 网络请求能力

当前已经实现一个可复用的原生网络层，具备如下功能：

- 基于 `HttpURLConnection` 发起 GET 及 **POST / PUT / DELETE / PATCH** 请求；
- 支持 **Query 拼接**、**动态 Header**、**可选 JSON 请求体**；
- 支持 **拦截器链**（`HttpInterceptor` + `HttpRequest`）与 **可插拔日志**（`HttpLogger`）；
- 请求自动切换到 `Dispatchers.IO` 执行；
- 支持连接超时与读取超时配置；
- 默认设置 `Accept: application/json`（可被业务 `headers` 覆盖）；
- 成功响应读取 `inputStream`；
- 失败响应读取 `errorStream`；
- 请求结束后统一调用 `disconnect()` 释放连接。

### 5.5 数据解析与模型转换

当前示例已完成从远程 JSON 到领域模型的完整转换流程：

1. `TodoApiService` 请求远程接口；
2. `HttpClient` 返回原始响应字符串；
3. `TodoDto.fromJson(raw)` 使用 `JSONObject` 完成 JSON 解析；
4. `TodoRepositoryImpl` 将 `TodoDto` 转为 `domain` 层的 `Todo`；
5. `GetTodoUseCase` 向表现层暴露统一业务入口。

这意味着后续新增接口时，只需按相同模式补充 `Dto`、`ApiService`、`RepositoryImpl` 和 `UseCase` 即可。

### 5.6 统一错误处理能力

当前网络异常已经完成分层封装，主要包括：

- `HttpError`：服务端返回非 2xx 状态码；
- `ParseError`：接口返回成功，但 JSON 解析失败；
- `ConnectionError`：网络连接失败或请求过程抛出异常。

上层业务统一处理 `NetworkResult.Success` 与 `NetworkResult.Error`，无需在每层重复 `try-catch`。

### 5.7 轻量依赖注入能力

当前项目通过 `ServiceLocator` 完成最小可用依赖装配，已经串联如下对象：

- `HttpClient`
- `TodoApiService`
- `TodoRepositoryImpl`
- `GetTodoUseCase`
- `HomeViewModelFactory`

这种方式虽然轻量，但已经足够支持当前单页面和单链路示例开发。

---

## 6. 依赖与配置

### 6.1 权限

在 `AndroidManifest.xml` 增加：

- `android.permission.INTERNET`

### 6.2 当前 Android 配置

- `namespace`：`com.hk.word.gameboosterproject`
- `applicationId`：`com.hk.word.gameboosterproject`
- `compileSdk`：34
- `targetSdk`：34
- `minSdk`：23
- 当前启用 `Jetpack Compose`
- 当前 `jvmTarget` 为 `1.8`

### 6.3 当前依赖

在 Gradle 中新增：

- `androidx.lifecycle:lifecycle-viewmodel-ktx`
- `androidx.lifecycle:lifecycle-viewmodel-compose`
- `androidx.lifecycle:lifecycle-runtime-compose`
- `org.jetbrains.kotlinx:kotlinx-coroutines-android`
- `androidx.activity:activity-compose`
- Compose BOM
- `androidx.compose.ui:ui`
- `androidx.compose.material3:material3`

---

## 7. 目录结构（核心）

```text
app/src/main/java/com/hk/word/gameboosterproject/
├─ ui/
│  └─ theme/
│     ├─ Color.kt
│     ├─ Theme.kt
│     └─ Type.kt
├─ core/
│  ├─ di/ServiceLocator.kt
│  └─ network/
│     ├─ HttpClient.kt
│     ├─ HttpRequest.kt
│     ├─ NetworkException.kt
│     └─ NetworkResult.kt
├─ data/
│  ├─ remote/
│  │  ├─ api/TodoApiService.kt
│  │  └─ dto/TodoDto.kt
│  └─ repository/TodoRepositoryImpl.kt
├─ domain/
│  ├─ model/Todo.kt
│  ├─ repository/TodoRepository.kt
│  └─ usecase/GetTodoUseCase.kt
├─ presentation/
│  └─ home/
│     ├─ HomeScreen.kt
│     ├─ HomeUiState.kt
│     └─ HomeViewModel.kt
└─ MainActivity.kt
```

---

## 8. 后续功能补充建议

如果要把该项目从“架构示例”继续扩展成真正可用的业务工程，建议补充以下功能：

### 8.1 网络层增强

以下能力已在当前代码中落地（详见第 4.2、5.4 节）：

1. **HTTP 方法**：`HttpClient` 已支持 `GET/POST/PUT/DELETE/PATCH`，含可选请求体与默认 `Content-Type`。
2. **Query / Header / Body**：各方法支持 `queryParams`、`headers`；`POST/PUT/DELETE/PATCH` 支持可选 `body` 与 `bodyContentType`。
3. **日志**：`HttpLogger` + `ServiceLocator` 在 Debug 下输出到 Logcat；敏感头打码、响应体超长截断。
4. **拦截器**：`HttpInterceptor` 链式处理 `HttpRequest`，便于统一 Token、公共参数等。

**仍可按需扩展**：自动重试、证书固定、请求/响应压缩、网络质量监控与埋点等。

### 8.2 业务层增强

1. 抽象更多 `UseCase`，避免 ViewModel 直接感知过多仓库细节。
2. 补充更多领域实体与仓库接口，支持多模块业务扩展。
3. 增加错误码到用户提示文案的映射层，避免直接展示底层异常信息。

### 8.3 数据层增强

1. 引入 Room 作为本地缓存层。
2. 支持“本地优先”或“网络优先”的数据策略。
3. 增加 DTO 与 Domain 的 Mapper，避免仓库内手写重复转换逻辑。

### 8.4 表现层增强

1. 为首页补充空状态、失败重试、下拉刷新等体验能力。
2. 增加多页面导航，形成真正的业务流程。
3. 抽离公共 UI 组件，减少页面重复代码。

### 8.5 工程化增强

1. 引入 Hilt 替代 `ServiceLocator`。
2. 增加单元测试、集成测试、Compose UI 测试。
3. 配置 CI，在提交后自动执行构建与测试。
4. 根据业务规模拆分为多个 feature module。

---

## 9. 运行与验证方式

- 建议使用 JDK 17 进行 Android 构建。
- 确保本机已正确配置 Android SDK 与 Gradle 运行环境。
- 如需验证工程是否可正常编译，可执行：
    - `gradlew.bat assembleDebug`
- 如需执行基础测试，可执行：
    - `gradlew.bat test`

### 9.1 验证重点

完成构建后，建议重点验证以下内容：

1. 应用是否能正常启动并进入首页。
2. 点击“请求远程数据”后，是否正确出现加载态。
3. 网络成功时，是否能正确展示 Todo 数据。
4. 断网或接口异常时，是否能正确展示错误信息。
5. （可选）使用 **Debug** 包运行时，在 Logcat 中过滤标签 `HttpClient`，确认请求与响应日志是否符合预期；**Release** 包默认不输出上述网络日志。

---

## 10. 总结

当前项目已经完成了一个清晰、可运行、可扩展的 Android MVVM 基础模板，具备以下价值：

- 架构分层明确，方便后续团队协作；
- 网络层不依赖第三方重型框架，便于理解底层原理；并已支持多方法、Query/Header、拦截器与调试日志；
- UI、状态、用例、仓库、网络形成完整闭环；
- 适合作为后续接入真实业务接口、扩展页面与能力的脚手架。

后续在此基础上继续补充真实业务功能时，建议优先完善依赖注入方案、测试体系与业务错误文案映射等工程化能力。
