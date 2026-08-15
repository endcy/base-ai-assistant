# 核心 AI 服务模块

> 工程的核心业务模块，承载所有 AI 能力的实现与编排。

## 模块定位

本模块是整个智能助手框架的"大脑"，负责接收用户请求后完成从意图识别、知识检索、工具调用到大模型调用的全链路处理。所有 AI 相关的业务逻辑编排均在此模块完成。

## 核心能力

### 智能体（Agent）体系

- **意图分析 Agent**：通过大模型对用户问题进行结构化意图识别，输出业务分类、数据源预测和工具推荐
- **ReAct 推理 Agent**：基于"思考→行动→观察"循环的推理智能体，支持多步决策
- **工具调用 Agent**：自动识别并调用匹配的工具链完成用户任务
- **Manus 超级 Agent**：具备自主规划能力的编排型智能体，可拆解复杂任务为多步执行
- **SubAgent 子代理**：拥有独立对话记忆的子智能体，处理复杂任务时隔离上下文，不污染主对话

### 高级智能体框架（多模式）

- **多模式执行**：`AgentMode` 枚举支持 `SINGLE_SHOT`（单次 RAG）、`AGENTIC`（ReAct 循环）、`PLAN_AND_ACT`（先规划后执行）三种模式
- **会话状态机**：`AgentSession` / `AgentStateMachine` 管理 INITIALIZED → RUNNING → COMPLETED / FAILED / TERMINATED_BY_BUDGET / TERMINATED_BY_USER 生命周期
- **异步任务管理**：`AgentTaskService` 支持任务提交、状态查询、取消
- **思考过程持久化**：`AgentEventPublisher` SSE 流式发布思考过程、工具调用事件
- **规划与反思**：`PlanningAgent` / `ReflectionAgent` 支持多步规划与逐步反思

### Guardrails 护栏系统

- **输入护栏**：提示注入检测、越狱过滤、PII 脱敏（默认启用）、话题门控
- **输出护栏**：内容审核、Schema 校验、毒性过滤、系统提示词泄露检测
- **工具参数校验**：`ToolValidator` 调用前校验参数合法性
- **护栏包装**：`GuardedToolCallback` / `GuardedToolFactory` 将护栏包装到工具回调

### 混合 RAG 检索增强

- **多路召回**：支持本地内存向量库、PGVector 向量库、BM25 关键词检索、阿里云百炼云知识库四路并行召回
- **智能路由**：根据意图分析结果动态选择检索策略（本地文档 / 数据库文档 / 云知识库 / 业务数据表）
- **重排序**：召回结果经 Rerank 模型二次精排，提升最终相关性
- **查询改写**：LLM 驱动的多查询扩展，提升召回率
- **混合检索融合**：多路召回结果通过 RRF（倒数排序融合）合并排序

### 提示词与 Advisor 体系

- **按请求 Advisor 工厂**：RAG 增强 Advisor 按请求创建实例，天然线程安全
- **Token 用量追踪**：自动记录每次请求的 promptTokens 和 completionTokens
- **丰富提示词模板**：内置意图分析、RAG 推荐、系统提示词等多套模板，支持结构化输出
- **事实性校验**：对大模型回答进行幻觉检测

### 工具链与 MCP

- **MCP 协议支持**：本地 MCP 工具、远程 SSE 工具、远程 Streamable HTTP 工具三种模式
- **可插拔工具注册**：实现统一接口即可自动发现注册，无需修改注册代码
- **内置工具集**：网页搜索、网页抓取、文件操作、PDF 生成、终端操作、资源下载、GIS 地理查询等
- **Skill 技能系统**：Markdown 文件驱动的技能定义，大模型自主判断是否调用，零代码新增能力
- **Command 命令系统**：纯 Prompt 模板文件，用户通过 API 主动指定调用

### 流式问答与文档应用

- **SSE 流式输出**：支持大模型回答实时流式推送
- **文档智能问答**：基于 RAG 的文档问答，支持相关文档引用返回
- **DeepSeek 搜索**：集成 DeepSeek 在线搜索能力
- **Token 用量分块推送**：流式响应中自动附带 Token 消耗统计

### 对话记忆

- **智能三层压缩**：摘要压缩 → Assistant 裁剪 → 滑动窗口，递进式控制上下文长度
- **文件持久化**：对话记忆支持 Kryo 序列化持久化到本地文件
- **多轮上下文**：RAG 模式下正确传递历史对话，保证多轮连贯性
- **Memory Summary 服务**：`MemorySummaryService` 超长历史 LLM 摘要压缩，摘要失败自动降级

### 工具管理系统

- **工具注册表**：`ToolRegistry` 启动时自动同步 `@Tool` 方法与 MCP 工具
- **工具组**：MCP 自动组（只读）+ CUSTOM 自定义组（可编辑成员）
- **权限解析**：`ToolPermissionGate` 按领域类型 / 用户角色动态解析工具权限
- **工具中文名**：运行时维护工具中文名映射

### Prompt 版本管理

- **版本号 + 内容哈希**：`PromptVersionService` 维护版本号与 SHA-256 内容哈希
- **金丝雀发布**：canary percentage 控制新旧版本流量比例

### 插件 / 扩展系统

- **扩展点接口**：`EnergyAiExtension` 定义可插拔扩展（工具 / Agent 策略 / 模型提供方 / 数据源 / 端点）
- **注册中心**：`ExtensionRegistry` 运行时枚举 / 获取 / 启用 / 禁用

### Agent 评估框架

- **LLM-as-judge**：`AgentEvaluator` 基于 Golden QA 集多维度评分（正确性 / groundedness / 完整性 / 工具调用准确率）

### 中文文档处理

- **自定义中文分割器**：针对中文标点符号和语义优化的文档切分，远优于通用英文分割器
- **文档关键词富化**：LLM 自动提取文档关键词写入元数据，提升检索精度
- **标题拼接**：向量化时自动拼接文档标题到内容前，提升语义完整性

## 依赖模块

| 模块                   | 用途                      |
|----------------------|-------------------------|
| energy-ai-repository | 数据持久化（MySQL + PGVector） |
| energy-ai-mcp        | MCP 工具定义与发现             |
| ces-ai-rpc           | RPC 接口契约与 DTO 定义        |
| service-common       | 通用配置、工具类、中间件集成          |
| service-domain       | 领域模型、枚举、DTO 定义          |

## 服务端口

默认端口：`9051`

## 关键配置项

- 大模型配置（DashScope / Ollama）
- RAG 检索参数（相似度阈值、Top-K、Rerank 等）
- MCP 服务/客户端配置
- 向量库配置（维度、距离类型、索引类型）
- 意图分析开关与提示词模板
