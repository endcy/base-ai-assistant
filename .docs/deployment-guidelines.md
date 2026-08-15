# 部署与配置指南

本文档面向**开发者与运维人员**，说明如何在本地/测试/生产环境部署 base-ai-assistant。

---

## 一、配置文件概览

| 文件                                                      | 服务        | 默认端口 | 主要职责                         |
|---------------------------------------------------------|-----------|------|------------------------------|
| `energy-ai-api/.../application.properties`              | 核心 AI 服务  | 9051 | AI 调用、RAG、Agent、MCP、工具、鉴权    |
| `energy-admin-api/.../application.properties`           | 管理后台      | 9050 | 知识库管理、用户记录、Agent 管理、Token 统计 |
| `energy-ai-api/.../META-INF/config/local.properties`    | AI 服务本地覆盖 | —    | 本地开发时覆盖生产配置（git 跟踪，敏感值留空）    |
| `energy-admin-api/.../META-INF/config/local.properties` | 管理后台本地覆盖  | —    | 同上                           |

> **两套配置文件的关系**：`application.properties` 定义全部默认值与占位符，`local.properties` 提供本地开发覆盖项（Apollo 关闭后生效）。生产部署通常通过 Apollo 覆盖。

---

## 二、必填配置（最小启动集）

要让两个服务都跑起来，**至少**要配置好以下项。其余均可保持占位符或默认值。

### 1. 数据库（必填）

两个服务共用同一 MySQL + PGVector。PGVector 用于向量检索，MySQL 用于业务表。

```properties
# ====== MySQL（业务表） ======
spring.datasource.druid.username=[你的 MySQL 用户名]
spring.datasource.druid.password=[你的 MySQL 密码]
spring.datasource.mysql.host=[MySQL IP]
spring.datasource.mysql.port=3306
# 库名：默认 energy_ai，DDL 见 .sql/mysql/

# ====== PostgreSQL + PGVector（向量库） ======
spring.datasource.pgsql.host=[PostgreSQL IP]
spring.datasource.pgsql.port=5432
spring.datasource.pgsql.username=[PG 用户名]
spring.datasource.pgsql.password=[PG 密码]
# 库名：默认 energy_ai，DDL 见 .sql/pgsql/
```

> 初始化顺序：先执行 `.sql/mysql/init/`，再依次按日期顺序执行 `20260318`/`20260611`/`20260722`/`20260808`/`20260813` 目录的迁移脚本；PG 同理先 `init/` 再 `20260808/`。

### 2. AI 大模型（必填）

使用阿里百炼 DashScope，至少需要一个 API Key。

```properties
spring.ai.dashscope.api-key=[你的 DashScope API Key]
spring.ai.dashscope.chat.options.model=qwen3.7-plus
spring.ai.dashscope.embedding.options.dimensions=1024      # 与 PGVector 表维度一致
spring.ai.dashscope.rerank.options.model=qwen3-rerank
```

### 3. Redis（必填）

服务启动、会话、缓存、限流均依赖 Redis。

```properties
spring.data.redis.host=[Redis IP]
spring.data.redis.port=6379
spring.data.redis.password=[Redis 密码，无则留空]
spring.data.redis.database=0
```

### 4. 鉴权 Token（必填）

服务对外接口的统一鉴权，调用方通过 Header `Authorization` 或 `token` 参数传递。

```properties
# 生产务必替换为强随机值（至少 32 字符），留空则所有请求被拒
ai.service.client.access-token=[你的鉴权 Token]
```

> 两个 `application.properties` 都要配置，且值必须相同（admin-api 通过 WebClient 调用 ai-api 时需要）。

---

## 三、可选组件及裁剪方法

base-ai-assistant 默认集成了 XXL-Job / RabbitMQ / Dubbo+Nacos / 远程 MCP 等组件。**单机或轻量部署时可全部关闭**，下面逐项说明。

### 3.1 XXL-Job 定时任务（可选）

**作用**：定时执行 Agent 任务清理、会话过期清理等定时作业。

**关闭方法**（两个服务都需操作）：

1. **配置**：删除或注释 `xxl.job.*` 相关所有配置项（`XxlJobConfig` 会因属性缺失而跳过）。
2. **代码**（可选，更彻底）：注释掉配置类 `XxlJobConfig.java` 的 `@Configuration` 注解，以及 `ExServiceJob.java` 中的 `@XxlJob` 注解方法。

> 关闭后定时清理任务失效，`ai_agent_session`/`ai_agent_thought` 会持续增长，需要自行通过数据库脚本或人工清理。

### 3.2 RabbitMQ 消息队列（可选）

**作用**：异步消息消费（验证码处理、事件驱动 Agent 触发）。

**关闭方法**（两个服务都需操作）：

1. **配置**：删除或注释 `spring.rabbitmq.*` 相关所有配置项。
2. **代码**（可选）：注释掉 `AdminRabbitMqConfig` / `EnergyAiRabbitMqConfig` 的 `@Configuration`，以及 `AiMessageConsumer` 的 `@Component`。
3. **EventDrivenAgentConsumer**（energy-ai-api 独有）：同样注释 `@Component` 即可。

> 关闭后，事件驱动的 Agent 触发、异步 MQ 消费链路不再工作，同步问答和 SSE 流式问答不受影响。

### 3.3 Dubbo + Nacos 微服务注册（可选）

**作用**：多服务实例间通过 Dubbo RPC 通信（admin-api ↔ ai-api）、Nacos 服务发现。

**关闭方法**（推荐做法 — 框架已支持）：

1. **配置**：设置 `ai.rpc.enabled=false`（ai-api 的 `application.properties` 已有此开关）。
2. **本地调用**：admin-api 通过 WebClient 走 HTTP（`feign-energy-ai-api.url=http://localhost:9051`），无需改代码。
3. **彻底去除 Dubbo**（可选）：删除 `dubbo.*`、`spring.cloud.nacos.*`、`application.env`、`application.group` 等所有配置；`ces-ai-rpc` 模块中 `RpcComponentConfig` 的 `@Configuration` 注释；两个 Application
   类上移除 `@EnableDubbo`（如有）。

> 单机部署推荐关闭，避免不必要的端口占用（20990/20991）和注册中心依赖。

### 3.4 远程 MCP 服务（可选）

**作用**：调用远程 MCP 服务端暴露的工具（高德地图、天气、网页解析、网页搜索等）。

**关闭方法**（仅 ai-api 需操作）：

1. **配置**：注释或删除 `spring.ai.mcp.client.*` 所有配置；注释 `ai.mcp.*` 下所有 MCP 工具配置（amap/weather/websearch/webparser）及对应的 `ai.mcp.client.auth.rules`。
2. **本地 MCP 服务端不受影响**：`spring.ai.mcp.server.*` 保持启用，本应用作为 MCP Server 暴露 `/mcp` 端点。

### 3.5 IoTDB 时序数据库（可选）

**作用**：历史大数据查询（当前代码中未使用，仅配置保留）。

**关闭方法**：直接删除 `spring.datasource.iotdb.*` 所有配置项即可，无需改代码。

### 3.6 OSS 对象存储（可选）

**作用**：Excel/PDF 生成后上传阿里云 OSS。

**关闭方法**：删除 `oss.file.*` 所有配置项。`OssUploadManager` 在配置缺失时降级为不上传。

---

## 四、energy-ai-api 配置详解（核心 AI 服务）

按模块分组，按**启用顺序**阅读：

### 4.1 应用基础

```properties
server.port=9051                             # 服务端口
spring.application.name=energy-ai-api        # 服务名（Dubbo/Nacos 用）
spring.jackson.time-zone=GMT+8               # 时区
spring.servlet.multipart.max-file-size=100MB # 上传大小限制
```

### 4.2 Spring AI 大模型与多模态

```properties
spring.ai.dashscope.api-key=...              # 必填
spring.ai.dashscope.chat.options.model=qwen3.7-plus
spring.ai.dashscope.chat.options.timeout=120000
spring.ai.dashscope.chat.options.multi-model=true    # 开启多模态（图片/音频/视频）
spring.ai.dashscope.chat.options.enable-thinking=false
spring.ai.dashscope.embedding.options.dimensions=1024 # 必须与 PGVector 表维度一致
spring.ai.dashscope.rerank.options.model=qwen3-rerank
spring.ai.retry.max-attempts=3               # 失败重试次数
```

### 4.3 MCP 服务端 + 客户端

```properties
# === 作为 MCP Server 暴露工具（默认开启）===
spring.ai.mcp.server.enabled=true
spring.ai.mcp.server.name=energy-ai-mcp-server
spring.ai.mcp.server.protocol=streamable
spring.ai.mcp.server.streamable-http.mcp-endpoint=/mcp

# === 连接远程 MCP Server（无需则整块注释）===
spring.ai.mcp.client.enabled=true
spring.ai.mcp.client.initialized=true
spring.ai.mcp.client.streamable-http.fix.connections.base-ai-mcp-server.url=http://localhost:8004
spring.ai.mcp.client.streamable-http.fix.connections.base-ai-mcp-server.endpoint=/mcp/sse

# === MCP 客户端认证（按 MCP Server 实际 token 配置）===
ai.mcp.client.auth.rules[0].url-prefix=http://localhost:8004
ai.mcp.client.auth.rules[0].token=your-token-here

# === 外部 MCP 工具密钥（复用 DashScope API Key 作为百炼 MCP 网关密钥）===
ai.mcp.amap.api-key=${spring.ai.dashscope.api-key}
ai.mcp.weather.api-key=${spring.ai.dashscope.api-key}
ai.mcp.websearch.api-key=${spring.ai.dashscope.api-key}
ai.mcp.webparser.api-key=${spring.ai.dashscope.api-key}
```

### 4.4 Agent 执行器

```properties
ai.agent.executor.enabled=true   # Agent 多步执行总开关
```

### 4.5 工具系统

```properties
# HTTP 工具（调用内部 API）
ai.tools.http.enabled=true
ai.tools.http.url-whitelist=http://127.0.0.1,http://localhost  # 仅允许白名单
ai.tools.http.allowed-methods=GET,POST
ai.tools.http.timeout-ms=8000
ai.tools.http.max-response-length=4000
```

### 4.6 RAG 检索

```properties
ai.rag.similarity-threshold=0.6       # 向量相似度阈值（低于此分数不入上下文）
ai.rag.similarity-top-k=3              # Top K
ai.rag.rerank-api-key=...              # 百炼 Rerank API Key（空则禁用 rerank）
ai.rag.rerank-model-name=ai-rerank
ai.rag.rerank-min-score=0.1
ai.rag.bm25-similarity-threshold=0.4   # BM25 文本检索阈值
ai.rag.bm25-top-k=5
ai.rag.enable-local-document=false     # 本地文档检索开关
ai.rag.local-document-paths=...        # 本地文档绝对路径
ai.rag.resource-document-path=document-exp  # classpath 资源文档路径
ai.rag.enable-intent-analysis=true     # 意图分析（LLM 智能路由数据源）
ai.rag.ali-dash-scope-app-id=...       # 百炼应用 ID（云端知识库）
ai.rag.ali-dash-scope-knowledge-index=...  # 百炼知识库名
```

### 4.7 鉴权与跨域

```properties
ai.service.client.access-token=${AI_SERVICE_ACCESS_TOKEN:}  # 必填，留空=拒绝所有请求
api.cors.allowOrigin=true               # 开发环境跨域（生产建议收紧）
```

---

## 五、energy-admin-api 配置详解（管理后台）

admin-api 相对精简，大部分配置与 ai-api 相同（数据库、Redis、MyBatis-Plus），**仅列出差异项**：

### 5.1 服务端口与线程池

```properties
server.port=9050
dubbo.protocol.port=20990
dubbo.application.qosPort=33390

# admin-api 自有业务线程池
task.pool.core-pool-size=10
task.pool.max-pool-size=30
task.pool.keep-alive-seconds=60
task.pool.queue-capacity=50
```

### 5.2 AI 服务客户端（admin 调 ai）

```properties
ai.service.client.enabled=true
ai.service.energy-ai-api=http://localhost:9051    # 指向 ai-api 地址
ai.service.client.access-token=[与 ai-api 同值]
```

### 5.3 Dubbo/Feign 测试（单机可不配）

```properties
# 单机部署可删除以下配置
dubbo-energy-ai-api.url=dubbo://localhost:20991
feign-energy-ai-api.url=http://localhost:9051
```

---

## 六、精简部署示例（单机最小集）

以下给出**只保留核心 AI 问答能力**的精简配置示例（关闭 XXL-Job/MQ/Dubbo/远程 MCP）：

### energy-ai-api/application.properties 精简版

保留项：

- 应用基础（server/servlet/jackson）
- Spring AI（dashscope.*）
- MCP Server（`spring.ai.mcp.server.*`，本应用作为 MCP Server）
- **删除**：`spring.ai.mcp.client.*`、`ai.mcp.*`、`ai.agent.executor.enabled`、`ai.tools.http.*`、`ai.rag.ali-dash-scope-*`、`xxl.job.*`、`spring.rabbitmq.*`、`dubbo.*`、`spring.cloud.nacos.*`、
  `application.env/group`、`spring.datasource.iotdb.*`、`oss.file.*`
- **保留但留空**：`ai.service.client.access-token` 必填
- **设置**：`ai.rpc.enabled=false`

### energy-admin-api/application.properties 精简版

保留项：

- 应用基础、数据库（MySQL + PGVector）、Redis、MyBatis-Plus
- **删除**：`xxl.job.*`、`spring.rabbitmq.*`、`dubbo.*`、`spring.cloud.nacos.*`、`application.env/group`
- **设置**：`ai.rpc.enabled=false`（如 ai-api 端配置了的话），`ai.service.client.access-token` 与 ai-api 同值

---

## 七、敏感信息配置建议

| 配置项                                | 建议方式                                          |
|------------------------------------|-----------------------------------------------|
| `spring.ai.dashscope.api-key`      | 环境变量：`${DASHSCOPE_API_KEY}`                   |
| `ai.service.client.access-token`   | 环境变量：`${AI_SERVICE_ACCESS_TOKEN}`             |
| `spring.datasource.druid.password` | 环境变量：`${MYSQL_PASSWORD}`                      |
| `spring.datasource.pgsql.password` | 环境变量：`${PG_PASSWORD}`                         |
| `spring.data.redis.password`       | 环境变量：`${REDIS_PASSWORD}`                      |
| `xxl.job.accessToken`              | 环境变量：`${XXL_JOB_ACCESS_TOKEN}`（已默认如此）         |
| 各种 `ai.mcp.*.api-key`              | 引用 DashScope：`${spring.ai.dashscope.api-key}` |

> 生产环境建议通过 **Apollo** 或 **K8s Secret / 环境变量** 下发敏感配置，避免写入代码库。

---

## 八、本地开发要点

1. **Apollo 关闭**：`apollo.bootstrap.enabled=false`（默认已关闭），所有配置读取 `application.properties` + `local.properties`。
2. **local.properties 覆盖**：数据库、Redis、API Key 等敏感项写在 `local.properties` 中，便于本地调试（注意 `.gitignore` 是否已排除）。
3. **Maven 编译**：`JAVA_HOME=<JDK 21> mvn clean compile -DskipTests`
4. **启动顺序**：先启动 MySQL + PGVector + Redis；再启动 `energy-ai-api`（9051）；最后 `energy-admin-api`（9050）。
5. **管理后台访问**：`http://localhost:9050/` 首页；Agent 实验室 `http://localhost:9050/agent-lab.html`。

---

## 九、端口清单

| 端口    | 服务               | 用途                          |
|-------|------------------|-----------------------------|
| 9050  | energy-admin-api | 管理后台 HTTP                   |
| 9051  | energy-ai-api    | 核心 AI HTTP                  |
| 20990 | energy-admin-api | Dubbo RPC（单机可关闭）            |
| 20991 | energy-ai-api    | Dubbo RPC（单机可关闭）            |
| 33390 | energy-admin-api | Dubbo QOS（单机可关闭）            |
| 33391 | energy-ai-api    | Dubbo QOS（单机可关闭）            |
| 18020 | energy-ai-api    | XXL-Job Executor（按需）        |
| 18021 | energy-admin-api | XXL-Job Executor（按需）        |
| /mcp  | energy-ai-api    | MCP Server（Streamable HTTP） |

---

> **参考**：完整的项目介绍、架构图与功能清单见 [`README.md`](./README.md) / [`README_EN.md`](./README_EN.md)。
