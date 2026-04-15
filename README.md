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
    - `TodoUserMessageMapper.kt`

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
    - `TodoDao.kt`（本地缓存查询接口）
    - `AppDatabase.kt`（Room 数据库入口）
    - `TodoEntity.kt`（本地缓存实体）
    - `TodoLocalMapper.kt`（本地/远端模型映射）
    - `TodoApiService.kt`（远程接口封装）
    - `TodoRemoteDataSource.kt`（远端数据源抽象）
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

1. `HomeScreen` 触发加载、重试或切换编号事件
2. `HomeViewModel.loadTodo()`
3. 调用 `GetTodoUseCase`
4. 调用 `TodoRepository` 接口
5. `TodoRepositoryImpl` 先请求远端数据源，再决定写入/读取 Room 缓存
6. `TodoApiService` 使用 `HttpClient` 发起网络请求
7. 成功时写入 Room；失败时尝试回退本地缓存
8. 返回 `NetworkResult` 给 ViewModel
9. ViewModel 更新 `HomeUiState`，UI 自动响应渲染

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

首页当前已经从“单次请求示例”扩展为一个轻量 Todo 列表页，具备以下交互能力：

- 首次进入页面时自动加载远程 Todo 列表；
- 顶部展示当前结果数量与列表状态提示；
- 支持标题关键字搜索，输入后立即在本地列表中过滤；
- 支持“全部 / 已完成 / 未完成”三态筛选；
- 支持手动刷新列表，刷新期间按钮禁用，避免重复请求；
- 首次加载时展示 `CircularProgressIndicator`；
- 请求成功后以列表形式展示 Todo 标题与完成状态；
- 当数据来自本地缓存时，页面展示“网络失败，已展示本地缓存列表”提示；
- 请求失败后展示用户可读的错误文案，并提供“重新获取列表”按钮；
- 当筛选结果为空时，展示“无匹配结果”的空状态提示。

### 5.3 状态管理能力

首页状态由 `HomeUiState` 统一维护，当前包含：

- `loading`：控制刷新按钮可用状态与加载指示器显示；
- `allTodos`：记录当前已加载的完整 Todo 列表；
- `visibleTodos`：记录按搜索与筛选条件计算后的展示列表；
- `searchQuery`：记录当前搜索关键字；
- `selectedFilter`：记录当前完成状态筛选值；
- `statusMessage`：提示当前数据来自网络还是本地缓存；
- `errorMessage`：控制错误提示展示与隐藏；
- `hasLoadedOnce`：区分首次加载态与空数据态。

`HomeViewModel` 使用：

- `MutableStateFlow` 持有内部状态；
- `StateFlow` 对外暴露只读状态；
- `collectAsStateWithLifecycle()` 在 Compose 页面中安全订阅状态；
- 本地过滤函数统一处理关键字搜索与完成状态筛选；
- 通过 `loadTodos / onSearchQueryChange / onFilterChange / retryLoad` 收敛页面交互入口。

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

当前示例已完成“远端接口 + Room 缓存”结合的数据流，并同时支持单条 Todo 与列表 Todo 的读取：

1. `TodoApiService` 既可请求单条接口 `/todos/{id}`，也可请求列表接口 `/todos`；
2. `HttpClient` 返回原始响应字符串；
3. `TodoDto.fromJson(raw)` 使用 `JSONObject` 解析单条 JSON；
4. `TodoDto.listFromJson(raw)` 使用 `JSONArray` 解析列表 JSON；
5. `TodoRepositoryImpl` 在远端成功时将 `TodoDto` 或 `List<TodoDto>` 写入 `Room`；
6. 若远端失败，则通过 `TodoDao` 查询单条或全量本地缓存并回退返回；
7. 最终再转换为 `domain` 层的 `TodoLoadResult` 或 `TodoListLoadResult` 暴露给上层。

这意味着后续新增接口时，只需按相同模式补充 `Dto`、`Entity`、`Dao`、`ApiService`、`RepositoryImpl` 和 `UseCase` 即可。

### 5.6 Room 本地缓存能力

当前项目已经引入 `Room` 作为最小可用缓存层，具备如下行为：

- 数据库入口：`AppDatabase`
- 缓存表：`todos`
- 主键：`TodoEntity.id`
- DAO：`TodoDao` 提供按编号查询、全量查询、单条覆盖写入和批量覆盖写入
- 缓存时间戳：`TodoEntity.cachedAt`
- 固定 TTL：`5 分钟`（`TodoRepositoryImpl.DEFAULT_CACHE_TTL_MS = 5 * 60 * 1000L`）
- 缓存策略：
    - 远端成功：写入缓存并返回最新数据
    - 远端失败：仅当本地缓存未过期时回退返回
    - 列表读取场景下，远端失败时会尝试回退整批未过期缓存数据
    - 无缓存或缓存已过期：继续向上抛出网络错误
- 表现层会根据数据来源显示“网络最新数据”或“本地缓存回退”提示

### 5.7 统一错误处理能力

当前网络异常已经完成分层封装，主要包括：

- `HttpError`：服务端返回非 2xx 状态码；
- `ParseError`：接口返回成功，但 JSON 解析失败；
- `ConnectionError`：网络连接失败或请求过程抛出异常。

上层业务统一处理 `NetworkResult.Success` 与 `NetworkResult.Error`，无需在每层重复 `try-catch`。

在表现层，新增 `TodoUserMessageMapper` 对底层异常做进一步转换，例如：

- `404`：提示当前编号不存在，可切换其他编号；
- `5xx`：提示服务器暂时不可用；
- 超时 / 断网：提示用户检查网络后重试；
- 解析失败：提示服务端返回数据格式异常。

### 5.8 轻量依赖注入能力

当前项目通过 `ServiceLocator` 完成最小可用依赖装配，已经串联如下对象：

- `HttpClient`
- `AppDatabase`
- `TodoDao`
- `TodoApiService`
- `TodoRepositoryImpl`
- `GetTodoUseCase`
- `GetTodoListUseCase`
- `HomeViewModelFactory`

这种方式虽然轻量，但已经足够支持当前单页面和列表读取链路示例开发。

---

## 6. 依赖与配置

### 6.1 权限

在 `AndroidManifest.xml` 增加：

- `android.permission.INTERNET`

### 6.2 当前 Android 配置

- `namespace`：`com.hk.word.gameboosterproject`
- `applicationId`：`com.hk.word.gameboosterproject`
- `compileSdk`：36
- `targetSdk`：35
- `minSdk`：23
- 当前启用 `Jetpack Compose`
- 当前 `jvmTarget` 为 `1.8`

### 6.3 当前依赖

在 Gradle 中新增：

- `androidx.lifecycle:lifecycle-viewmodel-ktx`
- `androidx.lifecycle:lifecycle-viewmodel-compose`
- `androidx.lifecycle:lifecycle-runtime-compose`
- `org.jetbrains.kotlinx:kotlinx-coroutines-android`
- `androidx.room:room-runtime`
- `androidx.room:room-ktx`
- `androidx.room:room-compiler`
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
│  ├─ local/
│  │  ├─ dao/TodoDao.kt
│  │  ├─ db/AppDatabase.kt
│  │  ├─ entity/TodoEntity.kt
│  │  └─ mapper/TodoLocalMapper.kt
│  ├─ remote/
│  │  ├─ api/TodoApiService.kt
│  │  ├─ api/TodoRemoteDataSource.kt
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
│     ├─ HomeViewModel.kt
│     └─ TodoUserMessageMapper.kt
└─ MainActivity.kt
```

---

## 8. 尚未实现的功能设计

当前工程已经具备可运行的最小闭环，但以下能力仍处于“设计建议 / 下一步规划”阶段，代码中尚未正式落地。为避免与“已实现功能”混淆，本节仅描述建议方案，不代表当前版本已经支持。

### 8.1 网络层下一步增强

以下基础能力已经具备：多 HTTP 方法、Query/Header/Body、拦截器链、Debug 日志、统一异常模型。基于此，建议继续补充：

1. **统一重试策略**
   - 目标：对超时、弱网、偶发 5xx 等临时性错误提供有限次自动重试。
   - 建议做法：在 `HttpClient` 外层增加 retry 包装器，按异常类型区分是否可重试，并加入指数退避。
   - 建议限制：默认仅重试幂等请求（如 `GET`），避免 `POST` 重复提交带来副作用。

2. **认证与会话管理**
   - 目标：为后续接入真实业务接口预留登录态、Token 注入与失效处理能力。
   - 建议做法：通过 `HttpInterceptor` 统一注入 `Authorization` 头；当接口返回 `401` 时，统一触发登录失效流程或刷新令牌逻辑。
   - 当前状态：项目中已有拦截器扩展点，但尚未接入真实鉴权链路。

3. **接口描述与请求封装统一化**
   - 目标：减少各 `ApiService` 手写 URL、Header、解析逻辑时的重复代码。
   - 建议做法：抽象出请求配置对象，例如 `Endpoint` / `RequestSpec`，让 `ApiService` 更聚焦于业务入参与结果映射。
   - 适用场景：当接口数量从单个 Todo 示例扩展到多个业务域时价值会明显提升。

4. **上传、下载与大响应处理**
   - 目标：支持文件上传、文件下载、流式读取等更接近真实业务的场景。
   - 建议做法：在现有 `String` 响应读取之外，增加字节流处理入口；必要时增加进度回调。
   - 当前限制：当前网络层主要面向 JSON 文本接口。

### 8.2 业务层下一步增强

当前业务层已经具备“获取单条 Todo + 获取 Todo 列表”的最小读链路。若继续扩展为真正的业务工程，建议优先补充：

1. **更细粒度的业务 UseCase**
   - 当前已实现：`GetTodoUseCase`、`GetTodoListUseCase`。
   - 后续可继续拆分：`SearchTodoUseCase`、`FilterTodoUseCase`，或在列表规模增大时将筛选逻辑从 `ViewModel` 下沉到 `domain` 层。

2. **写操作 UseCase**
   - 例如：`RefreshTodoUseCase`、`UpdateTodoStatusUseCase`、`CreateTodoUseCase`。
   - 价值：把“读取示例”扩展为“可读可写”的完整业务链路。
   - 设计建议：写操作与读操作分离，避免 ViewModel 直接拼接仓库调用细节。

3. **领域规则下沉**
   - 目标：把“搜索条件组合”“筛选规则复用”“是否允许重复刷新”“业务状态转换规则”等逻辑沉淀到 `domain` 层。
   - 当前状态：搜索与筛选逻辑当前仍主要位于 `HomeViewModel`，后续可逐步下沉为可测试的领域规则。

4. **统一用户提示规范**
   - 当前已有 `TodoUserMessageMapper` 将底层异常映射为用户文案。
   - 后续可继续升级为：
     - 统一错误码到文案的映射表；
     - 接入字符串资源以支持国际化；
     - 区分 Toast、Snackbar、页面内提示等不同消息级别。

### 8.3 数据层下一步增强

当前数据层已经实现“远端获取 -> 成功写缓存 -> 失败读缓存”的单条与列表数据策略，但距离可扩展数据架构还有一些关键能力未实现：

1. **缓存有效期与过期策略**
   - 目标：避免本地缓存永久有效，导致用户长期看到陈旧数据。
   - 当前状态：`TodoRepositoryImpl` 已基于 `TodoEntity.cachedAt` 落地固定 TTL 判断；网络失败时仅回退未过期缓存，过期缓存视为无缓存。
   - 后续可继续扩展：将固定 TTL 演进为按业务场景可配置策略，或继续补充静默刷新、强制刷新等更细粒度的数据策略。

2. **批量同步与列表缓存**
   - 当前已实现：`TodoDao` 已支持批量插入与全量查询，`TodoRepositoryImpl` 已支持列表缓存回退。
   - 后续目标：进一步支持分页页、首页聚合数据场景。
   - 后续建议做法：继续扩展 `Dao` 的条件查询能力，必要时增加分页键或同步游标字段。

3. **可切换的数据获取策略**
   - 例如：
     - `network-first`
     - `local-first`
     - `cache-then-network`
   - 价值：不同页面可按时效性与体验要求选择不同策略，而不是全项目共用同一套规则。

4. **更通用的 Mapper 组织方式**
   - 当前已经存在 `TodoLocalMapper`，但仍偏向单业务示例。
   - 后续建议统一约束：
     - `Dto -> Entity`
     - `Entity -> Domain`
     - `Dto -> Domain`
   - 这样在业务增多后，模型转换边界会更清晰，也更利于测试。

### 8.4 表现层下一步增强

当前首页已具备加载态、错误态、缓存提示、列表展示、关键字搜索与完成状态筛选，但仍属于单页演示结构。未实现、但值得继续补充的能力包括：

1. **列表页 + 详情页导航**
   - 目标：从“单页示例”扩展为真正的页面流转。
   - 建议页面：Todo 列表页、Todo 详情页、设置页。
   - 建议技术：引入 `Navigation Compose`，统一管理 route、参数传递和返回栈。

2. **更完整的加载体验**
   - 当前已实现：首次加载指示器、手动刷新、空列表态与无搜索结果态区分。
   - 可继续补充能力：
     - 下拉刷新；
     - 骨架屏；
     - 首次加载与局部刷新进一步分离。
   - 价值：让界面更接近真实线上应用体验。

3. **公共组件与页面状态规范化**
   - 建议抽离：
     - 通用错误提示组件；
     - 通用加载组件；
     - 通用空状态组件；
     - 页面顶部操作栏或按钮组组件。
   - 目标：减少页面重复代码，并统一视觉与交互行为。

4. **事件模型统一**
   - 当前页面交互主要围绕按钮点击直接触发 ViewModel 方法。
   - 后续可演进为 `UiEvent` + `reduce` 风格，便于复杂页面收敛交互入口，也更方便测试状态变化。

### 8.5 工程化下一步增强

工程层面目前已经有基础测试依赖，并存在少量单元测试与示例仪器测试；但完整的工程化体系仍未形成，建议继续补充：

1. **依赖注入升级**
   - 当前：使用 `ServiceLocator` 手动装配依赖，轻量直观，适合当前规模。
   - 后续：可迁移到 `Hilt`，降低对象创建样板代码，并提升多页面、多模块下的可维护性。

2. **测试体系扩展**
   - 当前已有：
     - `TodoRepositoryImplTest`
     - `HomeViewModelTest`
     - `TodoUserMessageMapper` 相关单元测试
     - 示例 `androidTest`
   - 尚未完善：
     - `HttpClient` 拦截器与异常分支测试；
     - `Room` 数据层集成测试；
     - Compose UI 交互测试；
     - 列表分页、复杂筛选条件等更细粒度用例测试。

3. **CI 与质量门禁**
   - 建议在代码托管平台配置自动化流程，至少包含：
     - `assembleDebug`
     - `test`
     - Lint 或静态检查
   - 价值：避免回归问题在本地遗漏，提升多人协作稳定性。

4. **模块化拆分**
   - 当前工程仍是单 `app` 模块，适合起步阶段。
   - 后续可根据业务增长拆分为：
     - `core-common`
     - `core-network`
     - `feature-home`
     - `feature-todo`
     - `data-*`
   - 价值：降低编译耦合，明确团队边界，便于按功能并行开发。

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
2. 点击“加载当前编号”后，是否正确出现加载态。
3. 点击“上一个 / 下一个”后，是否会切换请求目标编号。
4. 网络成功时，是否能正确展示 Todo 标题与完成状态。
5. 首次联网成功后再断网，是否仍可通过相同编号读取本地缓存。
6. 断网且无缓存时，是否能正确展示错误信息并支持重试。
7. （可选）使用 **Debug** 包运行时，在 Logcat 中过滤标签 `HttpClient`，确认请求与响应日志是否符合预期；**Release** 包默认不输出上述网络日志。

---

## 10. 总结

当前项目已经完成了一个清晰、可运行、可扩展的 Android MVVM 基础模板，具备以下价值：

- 架构分层明确，方便后续团队协作；
- 网络层不依赖第三方重型框架，便于理解底层原理；并已支持多方法、Query/Header、拦截器与调试日志；
- UI、状态、用例、仓库、网络与本地缓存形成完整闭环，首页已具备空状态、编号切换、失败重试、缓存回退与用户友好错误提示；
- 适合作为后续接入真实业务接口、扩展页面与能力的脚手架。

后续在此基础上继续补充真实业务功能时，建议优先完善依赖注入方案、测试体系、缓存策略细化与导航等工程化能力。
  