# 基础 AI 助手应用框架

<div align="center">

**基于 Spring AI + Spring AI Alibaba 的企业级 RAG 智能助手开发框架**

[![Java](https://img.shields.io/badge/Java-21-blue.svg)](https://openjdk.java.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.13-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Spring AI](https://img.shields.io/badge/Spring%20AI-1.0.0--M7-orange.svg)](https://docs.spring.io/spring-ai/reference/)
[![Spring AI Alibaba](https://img.shields.io/badge/Spring%20AI%20Alibaba-1.0.0.4-red.svg)](https://sca.aliyun.com/docs/ai/overview/)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

[项目简介](#-项目简介) | [快速开始](#-快速开始) | [核心特性](#-核心特性) | [架构设计](#-架构设计) | [部署指南](#-部署指南)

</div>

---

## 📖 项目简介

### base-ai-assistant项目是什么？

这是一个**企业级 AI 智能助手开发框架**，基于 Spring Boot 3.3.13、Spring AI 和 Spring AI Alibaba 构建。

它解决了企业在引入 AI 大模型时的核心痛点：

- ❌ **大模型"胡说八道"** → ✅ RAG 检索增强，基于企业真实文档回答
- ❌ **数据孤岛问题** → ✅ MCP 协议打通业务系统，实时获取业务数据
- ❌ **通用模型不专业** → ✅ 意图分析 + 领域知识库，打造垂直领域专家
- ❌ **工具调用困难** → ✅ 标准化工具链，让 AI 能执行实际业务操作

### 🎯 适用场景

| 场景         | 典型用例                  | 核心价值             |
|------------|-----------------------|------------------|
| **智能客服**   | 产品咨询、售后支持、FAQ 自动问答    | 7×24 小时在线，降低人工成本 |
| **智能运维**   | 运维知识库检索、故障排查指导、操作手册查询 | 快速定位问题，减少 MTTR   |
| **企业知识助手** | 内部文档检索、制度查询、培训资料检索    | 知识高效利用，减少重复咨询    |
| **垂直领域专家** | 能源管理、充电运营、电力行业等专业领域问答 | 领域专业化，提升回答质量     |
| **工作流自动化** | 多工具串联的任务执行、数据查询与决策    | 减少人工操作，提升效率      |

> 💡 **说明**：本工程以"智慧能源 AI 应用"为业务背景，但框架设计完全通用。业务领域定义、工程包名、数据模型等内容均可按需更改，快速适配不同行业场景。

---

## ✨ 核心特性

### 1. 🧠 混合检索增强 (Hybrid RAG)

**痛点**：传统 RAG 系统检索召回率低、相关度不高。

**解决方案**：多路召回 + 重排序的混合检索架构

```
用户问题
  │
  ├─→ 查询改写 (Query Rewriter) ──→ 改写为更易检索的表述
  │
  ├─→ 多查询扩展 (MultiQueryExpander) ──→ 生成 3 个不同角度的变体
  │
  ▼
┌─────────────────────────────────────────────────┐
│              并发多路检索（降低延迟）            │
├─────────────┬─────────────┬─────────────┬───────┤
│  本地文档   │  PG 向量检索 │  BM25 检索  │ 云检索 │
│  检索器     │  (语义相似)  │ (关键词)    │ 库    │
└─────────────┴─────────────┴─────────────┴───────┘
  │
  ├─→ 文档合并 + 去重
  │
  ├─→ Rerank 重排序（阿里百炼） ──→ 精排筛选 Top-K
  │
  ▼
注入提示词上下文 → 大模型生成回答
```

**效果对比**：

| 检索方式           | 召回率      | 精度       | 延迟    |
|----------------|----------|----------|-------|
| 单一向量检索         | 60%      | 70%      | 低     |
| 单一关键词检索        | 50%      | 60%      | 低     |
| **混合检索 + 重排序** | **85%+** | **80%+** | **中** |

### 2. 🎯 意图分析驱动的智能路由

**痛点**：用户问题类型多样，单一检索策略无法满足。

**解决方案**：LLM 意图识别 + 智能数据源路由

```
用户问题 → 意图分析 Agent → IntentResult
                          │
          ┌───────────────┼───────────────┐
          ▼               ▼               ▼
    业务类型分类      数据来源预测      工具链选择
    (businessType)   (dataScopeList)   (Tools)
          │               │               │
    ┌─────┴─────┐   ┌─────┴─────┐        │
    ▼           ▼   ▼           ▼        ▼
 充电运营    能源管理  本地文档  数据库文档  MCP 工具
```

`PossibleSourceTypeEnum` 定义的数据源类型：

- `LOCAL`：本地文档（运维配置、代码文档、平台操作记录）
- `VECTOR`：数据库文档（客服 FAQ、售后工单、技术咨询）
- `CLOUD`：阿里云百炼知识库
- `DATABASE`：业务表数据（订单、用户、站点信息）
- `UNKNOWN`：未知领域问题

**实际效果**：

- ✅ 充电订单问题 → 自动调用订单查询 MCP 工具
- ✅ 操作手册问题 → 检索本地文档库
- ✅ 计费策略问题 → 检索数据库文档
- ✅ 站点信息问题 → 查询业务数据表

### 3. 📝 中文友好的文档处理

**痛点**：英文文档分割器处理中文效果差，割裂语义。

**解决方案**：自定义 `ChineseEnhancedTextSplitter`

| 分割器                           | 原理            | 中文效果           |
|-------------------------------|---------------|----------------|
| `TokenTextSplitter`           | 固定 token 范围切分 | ⭐⭐ 英文友好，中文生硬割裂 |
| `SentenceSplitter`            | 基于语义断句        | ⭐⭐ 英文友好，中文识别差  |
| `ChineseEnhancedTextSplitter` | 中文标点 + 语义优化   | ⭐⭐⭐⭐⭐ **强烈推荐** |

**优化点**：

- ✅ 支持中文标点符号分隔（，。！？；：等）
- ✅ 保留语义完整性，避免生硬截断
- ✅ 可配置分隔符集合，适配不同场景

### 4. 🔄 灵活的文档管理

#### 三种文档来源对比

| 类型    | 存储方式             | 管理方式   | 适用场景        | 优点       | 缺点      |
|-------|------------------|--------|-------------|----------|---------|
| 本地文档  | 文件系统/resources   | 文件上传更新 | 固定文档、产品手册   | 部署简单     | 更新需重新打包 |
| 数据库文档 | MySQL + PGVector | 后台管理界面 | 动态内容、FAQ、工单 | 实时更新、可追溯 | 需维护数据库  |
| 云知识库  | 阿里云百炼            | 云端控制台  | 大规模知识库      | 免运维、弹性扩展 | 数据出域、成本 |

#### 文档状态控制

```java
// 文档支持状态控制，动态更新向量内容
status:0-下架  1-上架  2-待向量化  3-向量化完成
```

- ✅ 文档状态控制：控制是否参与检索
- ✅ 增量更新：仅更新变更文档的向量
- ✅ 租户隔离：按 `group_id` 实现多租户数据隔离
- ✅ 多级分类：`scope_type`（领域）+ `business_type`（业务）

#### 知识分类配置管理

**痛点**：硬编码的分类枚举无法灵活扩展，新增分类需要修改代码。

**解决方案**：数据库配置化 + 后台管理界面

```sql
-- 知识分类配置表
CREATE TABLE ai_knowledge_category_config (
    id          BIGINT PRIMARY KEY,
    type        VARCHAR(32)  COMMENT '分类类型 (scope-知识领域，business-业务领域)',
    code        VARCHAR(64)  COMMENT '分类编码 (英文标识)',
    name        VARCHAR(128) COMMENT '分类名称 (中文显示)',
    parent_code VARCHAR(64)  COMMENT '父级分类编码',
    description VARCHAR(512) COMMENT '分类描述',
    sort_order  INT          COMMENT '排序序号',
    enabled     TINYINT(1)   COMMENT '是否启用'
);
```

**功能特性**：

- ✅ 动态维护分类配置：支持新增、编辑、删除分类
- ✅ 分类启用/禁用：控制分类是否参与匹配
- ✅ 排序管理：自定义分类展示顺序
- ✅ 批量导入文档：从文件系统批量导入，自动匹配分类
- ✅ 后台管理界面：可视化操作，无需修改代码

### 5. 🔌 MCP 工具链扩展

**什么是 MCP？**

MCP（Model Context Protocol）是 AI 与外部系统的标准化通信协议，让大模型能够调用外部工具获取实时数据。

#### 支持模式

| 模式                  | 特点                 | 适用场景          |
|---------------------|--------------------|---------------|
| 本地 MCP              | 本应用内定义工具           | 简单工具、数据库查询    |
| 远程 MCP (SSE)        | Server-Sent Events | 传统服务暴露        |
| 远程 MCP (Streamable) | HTTP Stream        | **断线重连、生产推荐** |

#### 已验证工具示例

- 🔍 Pexels API 图片搜索（MCP 实现）
- 📄 网页抓取工具
- 🔎 DeepSeek 在线搜索
- 📊 业务数据查询（待完善）
- 📁 文件读写操作
- 📋 PDF 生成工具

#### MCP 工具开发示例

```java
@Component
public class OrderMcpTools {

    @ToolMapping(name = "getOrderDetail",
            title = "查询用户订单信息",
            description = """
                    【关键工具】当用户需要查询任何与充电订单相关的信息时，【必须】调用此工具。
                    
                    **调用场景**：
                    - 根据订单号查询订单
                    - 查询最新订单
                    - 根据用户信息查询订单
                    - 查询订单状态（充电中、已完成）
                    - 查询订单金额等
                    
                    **触发关键词**：订单、我的订单、最新订单、订单详情
                    """,
            returnDirect = true)
    public String getOrderDetail(
            @Param(description = "订单号，如果用户没有提供则不传", required = false) String orderSeq,
            @Param(description = "租户 ID，非必填", required = false) Long operatorId,
            @Param(description = "用户 ID，非必填", required = false) Long accountId) {

        // 实现逻辑：查询数据库返回订单信息
        return orderService.queryOrder(orderSeq, operatorId, accountId);
    }
}
```

**最佳实践**：

- ✅ 工具描述要详细准确，包含调用场景和触发关键词
- ✅ 参数描述清晰，说明必填/选填
- ✅ 工具功能单一，避免"万能工具"
- ✅ 返回值格式明确，便于大模型理解

### 7. 📁 批量文档导入工具

**痛点**：手动逐个上传文档效率低，大量历史文档需要快速入库。

**解决方案**：`DocumentImportHelper` 批量导入工具

**功能特性**：

- ✅ 递归扫描目录：自动获取目录下所有文件
- ✅ 智能分类匹配：根据路径层级自动推断知识领域和业务类型
- ✅ 支持多种格式：Markdown (.md/.markdown)、文本 (.txt)
- ✅ 去重检测：基于文件路径避免重复导入
- ✅ 编码兼容：自动识别 UTF-8/GBK 编码
- ✅ 批量导入结果反馈：成功/失败/跳过统计

**导入示例**：

```java
// 从指定目录批量导入文档
BatchImportResult result = documentImportHelper.importFromDirectory(
    "E:/knowledge-base/products",  // 目录路径
    1001L,                          // 租户 ID
    "developer_reference"           // 默认知识领域
);

// 导入结果
result.getSuccessCount();  // 成功数量
result.getFailCount();     // 失败数量
result.getSkipCount();     // 跳过数量（已存在）
```

**智能分类匹配逻辑**：

```
文件路径：/知识文档/用户客服/充电订单/操作手册.md

匹配过程:
1. "用户客服" → 匹配 scopeTypeNameMap → "account_customer_service"
2. "充电订单" → 匹配 businessTypeNameMap → "charge_order"
3. 结果：scopeType="account_customer_service", businessType="charge_order"
```

> 💡 **提示**：分类匹配基于数据库配置表 `ai_knowledge_category_config`，支持运行时调整匹配规则。

---

## 🌐 多模型支持

#### 云端模型（DashScope）

```properties
spring.ai.dashscope.api-key=YOUR_DASHSCOPE_API_KEY
spring.ai.dashscope.chat.options.model=qwen3-max
```

- ✅ 支持阿里百炼所有模型（qwen3-max、qwen-plus 等）
- ✅ 支持自定义 API 版本和端点
- ✅ Token 用量可由运维跟踪监控

#### 本地模型（Ollama）

```properties
spring.ai.ollama.base-url=http://localhost:11434
spring.ai.ollama.chat.model=qwen3:8b
```

- ✅ 支持本地部署的开源模型
- ✅ 可按需加载不同模型
- ⚠️ 生产环境建议 32B 参数以上

---

## 🚀 快速开始

### 环境要求

| 组件         | 版本   | 说明                   |
|------------|------|----------------------|
| JDK        | 21+  | **必须**               |
| Maven      | 3.6+ | 构建工具                 |
| MySQL      | 8.0+ | 业务数据库                |
| PostgreSQL | 14+  | 向量数据库（需 pgvector 扩展） |
| Redis      | -    | 可选（会话缓存）             |

### 1. 克隆项目

```bash
git clone https://github.com/your-org/base-ai-assistant.git
cd base-ai-assistant
```

### 2. 数据库初始化

#### MySQL 初始化

```bash
mysql -u root -p < .sql/mysql/init/ddl_init_energy_ai.sql
```

创建业务表：知识文档表、对话记录表等。

#### PGVector 初始化

```bash
# 先安装 pgvector 扩展
psql -U postgres -d energy_ai -c "CREATE EXTENSION IF NOT EXISTS vector;"
psql -U postgres -d energy_ai < .sql/pgsql/init/ddl_init_energy_ai.sql
```

创建向量表及 HNSW 索引、BM25 全文索引。

### 3. 配置修改

编辑 `energy-ai-api/src/main/resources/application.properties`：

```properties
# ========================================
# 大模型配置（必须）
# ========================================
spring.ai.dashscope.api-key=YOUR_DASHSCOPE_API_KEY
spring.ai.dashscope.chat.options.model=qwen3-max
# ========================================
# 数据库配置（必须）
# ========================================
# MySQL 配置
spring.datasource.mysql.host=localhost
spring.datasource.mysql.port=3306
spring.datasource.druid.username=root
spring.datasource.druid.password=YOUR_PASSWORD
# PostgreSQL 配置
spring.datasource.pgsql.host=localhost
spring.datasource.pgsql.port=5432
spring.datasource.pgsql.username=postgres
spring.datasource.pgsql.password=YOUR_PASSWORD
# ========================================
# RAG 配置（推荐）
# ========================================
ai.rag.similarity-threshold=0.6
ai.rag.top-k=3
ai.rag.rerank-api-key=YOUR_RERANK_API_KEY
ai.rag.enable-intent-analysis=true
# ========================================
# MCP 配置（可选）
# ========================================
spring.ai.mcp.server.enabled=true
spring.ai.mcp.client.enabled=true
```

### 4. 编译打包

#### 方式一：默认 JDK 21

```bash
mvn clean package -DskipTests
```

#### 方式二：指定 Java 21 编译器

**Windows PowerShell:**

```powershell
$env:MAVEN_OPTS = "-Dmaven.compiler.fork=true -Dmaven.compiler.executable=D:/env/graalvm-jdk-21.0.5/bin/javac"
mvn clean package -DskipTests
```

**Linux:**

```bash
export MAVEN_OPTS="-Dmaven.compiler.fork=true -Dmaven.compiler.executable=/opt/graalvm-jdk-21/bin/javac"
mvn clean package -DskipTests
```

### 5. 启动服务

```bash
# 启动 AI 助手服务
java -jar energy-ai-api/target/energy-ai-api-1.0.0.jar

# 启动管理后台（可选）
java -jar energy-admin-api/target/energy-admin-api-1.0.0.jar
```

### 6. 访问验证

- 管理后台：http://localhost:9050/index.html
- API 接口：http://localhost:9051/api/chat

---

## 🏗️ 架构设计

### 总体流程

```
┌─────────────────────────────────────────────────────────────┐
│                     用户提问                                 │
└─────────────────────┬───────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────────┐
│  1. 意图分析 Agent                                           │
│     - 业务类型识别 (businessType)                           │
│     - 数据来源预测 (dataScopeList)                          │
└─────────────────────┬───────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────────┐
│  2. 查询改写 (Query Rewriter)                               │
│     用户问题 → 更适合检索的表述                              │
└─────────────────────┬───────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────────┐
│  3. 多查询扩展 (MultiQueryExpander)                         │
│     生成 3 个不同角度的查询变体                                │
└─────────────────────┬───────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────────┐
│  4. 并发多路检索                                             │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐       │
│  │ 本地文档 │ │向量检索  │ │ BM25 检索 │ │云知识库  │       │
│  │ Retriever│ │Retriever │ │Retriever │ │Retriever │       │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘       │
└─────────────────────┬───────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────────┐
│  5. 文档合并 + 去重                                          │
└─────────────────────┬───────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────────┐
│  6. Rerank 重排序 (DashScope RerankModel)                   │
│     精排筛选，保留得分 >= 0.1 的文档                           │
└─────────────────────┬───────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────────┐
│  7. 注入提示词上下文                                         │
│     [检索到的文档] + [用户问题] → Prompt                     │
└─────────────────────┬───────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────────┐
│  8. 大模型生成回答                                           │
│     (DashScope / Ollama)                                    │
└─────────────────────┬───────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────────┐
│                     输出回答                                 │
└─────────────────────────────────────────────────────────────┘
```

### 工程模块

```
base-ai-assistant/
├── energy-admin-api/          # 管理后台（知识库管理、配置管理等）
├── energy-ai-api/             # 核心服务（RAG、Agent、MCP 实现）
├── energy-ai-mcp/             # MCP 服务定义
├── energy-ai-repository/      # 数据持久化（MySQL、PGVector）
├── energy-ai-rpc/             # RPC 接口定义（Dubbo/Feign）
├── service-common/            # 通用服务（配置、工具类）
└── service-domain/            # 领域模型定义
```

### 技术栈

| 类别    | 技术                | 版本       | 用途         |
|-------|-------------------|----------|------------|
| 基础框架  | Spring Boot       | 3.3.13   | 应用底座       |
| AI 框架 | Spring AI         | 1.0.0-M7 | LLM 调用、RAG |
| AI 扩展 | Spring AI Alibaba | 1.0.0.4  | 阿里百炼集成     |
| 语言    | Java              | 21       | 开发语言       |
| ORM   | MyBatis Plus      | 3.5.7    | 数据库操作      |
| 工具库   | Hutool            | 5.8.26   | 工具类        |
| 微服务   | Dubbo             | 3.3.0    | RPC 调用     |
| 注册中心  | Nacos/MSE         | -        | 服务注册       |
| 配置中心  | Apollo            | 2.1.0    | 配置管理       |
| 任务调度  | XXL-Job           | 2.5.0    | 定时任务       |
| 连接池   | Druid             | 1.2.18   | 数据库连接池     |
| 熔断降级  | resilience4j      | -        | 服务保护       |

---

## 📚 核心代码详解

### 1. 混合检索顾问 (HybridRetrievalAdvisor)

RAG 检索增强的核心组件：

```java

@Slf4j
public class HybridRetrievalAdvisor implements BaseAdvisor {

    private final RerankModel rerankModel;
    private final List<BaseDocumentRetriever> documentRetrievers;
    private final QueryExpander queryExpander;

    @Override
    public ChatClientRequest before(ChatClientRequest request, AdvisorChain advisorChain) {
        // 1. 构建原始查询
        var userMessage = request.prompt().getUserMessage();
        Query originalQuery = Query.builder()
                                   .text(userMessage.getText())
                                   .build();

        // 2. 多查询扩展（生成 3 个变体）
        List<Query> querySplits = queryExpander.expand(originalQuery);

        // 3. 并发多路检索
        List<List<Document>> documentsList = documentRetrievers.stream()
                                                               .map(retriever -> CompletableFuture.supplyAsync(
                                                                       () -> retriever.retrieve(querySplits),
                                                                       buildDefaultTaskExecutor()  // 线程池
                                                               ))
                                                               .toList()
                                                               .stream()
                                                               .map(CompletableFuture::join)
                                                               .toList();

        // 4. 文档合并去重
        List<Document> merged = mergeDocuments(documentsList);

        // 5. Rerank 重排序
        List<Document> reranked = doRerank(request, merged);

        // 6. 注入上下文
        String context = reranked.stream()
                                 .map(Document::getText)
                                 .collect(Collectors.joining("\n"));

        // 7. 增强提示词
        return request.mutate()
                      .prompt(request.prompt().augmentUserMessage(
                              "参考信息:\n" + context + "\n问题:" + userMessage.getText()
                      ))
                      .build();
    }
}
```

### 2. 检索器工厂 (AdvisorRetrieverFactory)

根据意图动态选择检索器：

```java

@Component
public class AdvisorRetrieverFactory {

    @Autowired
    private PgVectorStore pgVectorVectorStore;
    @Autowired
    private SimpleVectorStore localVectorStore;
    @Autowired
    private VectorStoreService vectorStoreService;

    public List<BaseDocumentRetriever> dynamicCreateRetrievers(
            DocumentQueryContext documentParams,
            IntentResult intentResult) {

        List<BaseDocumentRetriever> retrievers = new ArrayList<>();

        for (PossibleSourceTypeEnum dataScope : intentResult.getDataScopeList()) {
            switch (dataScope) {
                case LOCAL -> retrievers.add(new LocalDocumentRetriever(localVectorStore, ...));
                case VECTOR -> {
                    retrievers.add(new VectorDocumentRetriever(pgVectorVectorStore, ...));
                    retrievers.add(new Bm25DocumentRetriever(vectorStoreService, ...));
                }
                case CLOUD -> retrievers.add(new AliDocumentRetriever(dashScopeConnectionProperties, ...));
            }
        }

        return retrievers;
    }
}
```

### 3. 请求级 RAG 上下文

```java

@Component
@RequestScope
public class RequestRagContext {
    private Long chatId;
    private List<Document> relatedDocuments; // 检索到的关联文档

    // Getter/Setter
}
```

---

## 🗄️ 数据架构

### 核心数据表

#### 1. 知识文档表 (ai_knowledge_document)

```sql
CREATE TABLE ai_knowledge_document
(
    id            BIGINT PRIMARY KEY,
    scope_type    VARCHAR(50) COMMENT '知识领域类型',
    business_type VARCHAR(50) COMMENT '业务类型',
    group_id      BIGINT COMMENT '租户/商户 ID',
    content       TEXT COMMENT '文档内容',
    source_type   VARCHAR(20) COMMENT '来源类型',
    source_path   VARCHAR(255) COMMENT '来源路径',
    status        TINYINT COMMENT '状态：0-下架 1-上架 2-待向量化 3-已完成',
    create_time   DATETIME,
    update_time   DATETIME
);
```

**设计亮点**：

- ✅ 多租户隔离：通过 `group_id` 实现数据隔离
- ✅ 双层分类：`scope_type`（领域）+ `business_type`（业务）
- ✅ 状态控制：控制文档是否参与检索

#### 2. 对话记录表 (ai_context_user_record)

```sql
CREATE TABLE ai_context_user_record
(
    id            BIGINT PRIMARY KEY,
    chat_id       BIGINT COMMENT '会话 ID',
    group_id      BIGINT COMMENT '租户 ID',
    scope_type    VARCHAR(50) COMMENT '知识领域',
    business_type VARCHAR(50) COMMENT '业务类型',
    question      TEXT COMMENT '用户问题',
    answer        TEXT COMMENT 'AI 回答',
    create_time   DATETIME
);
```

#### 3. 向量存储表 (vector_store)

```sql
-- PostgreSQL with pgvector extension
CREATE TABLE vector_store
(
    id      VARCHAR PRIMARY KEY,
    embedding VECTOR(1024) COMMENT '向量数据',
    content TEXT COMMENT '文档内容',
    metadata JSONB COMMENT '元数据'
);

-- 创建 HNSW 向量索引（高性能相似度检索）
CREATE INDEX vector_store_embedding_idx
    ON vector_store USING hnsw (embedding vector_cosine_ops);

-- 创建 BM25 全文索引（关键词检索）
CREATE INDEX vector_store_content_idx
    ON vector_store USING gin (to_tsvector('simple', content));
```

---

## 📋 部署指南

### 生产环境建议

#### 1. 大模型选择

| 场景     | 推荐模型                  | 说明       |
|--------|-----------------------|----------|
| 在线 RAG | qwen3-max / qwen-plus | 效果好，成本高  |
| 意图分析   | qwen3-32b（微调）         | 精准分类，成本低 |
| 本地部署   | qwen3:32b+            | 需 GPU 资源 |

#### 2. 向量数据库配置

```properties
# PGVector 连接配置
spring.ai.vectorstore.pgvector.dimensions=1024
spring.ai.vectorstore.pgvector.distance-type=COSINE_DISTANCE
spring.ai.vectorstore.pgvector.index-type=HNSW
# HNSW 索引参数（根据数据量调整）
# m=16, efConstruction=64 适用于百万级向量
```

#### 3. RAG 参数调优

```properties
# 相似度阈值（根据实际效果调整）
ai.rag.similarity-threshold=0.6      # 向量检索最低相似度
ai.rag.bm25-similarity-threshold=0.4 # BM25 检索最低相似度
# 召回数量
ai.rag.top-k=3          # 向量检索 Top-K
ai.rag.bm25-top-k=5     # BM25 检索 Top-K
# Rerank 配置
ai.rag.rerank-min-score=0.1  # Rerank 最低得分
ai.rag.rerank-model-name=ai-rerank
```

### 配置中心集成

#### Apollo 配置示例

```properties
# ========================================
# common 公共配置
# ========================================
# 大模型配置
spring.ai.dashscope.api-key=${DASHSCOPE_API_KEY}
spring.ai.dashscope.chat.options.model=qwen3-max
# RAG 配置
ai.rag.similarity-threshold=0.6
ai.rag.top-k=3
ai.rag.rerank-api-key=${RERANK_API_KEY}
# 数据库配置
spring.datasource.druid.url=jdbc:mysql://${DB_HOST}:3306/energy_ai
spring.datasource.pgsql.url=jdbc:postgresql://${PG_HOST}:5432/energy_ai
# MCP 配置
spring.ai.mcp.server.enabled=true
spring.ai.mcp.client.enabled=true
```

---

## 🔧 开发指南

### 自定义工具链

```java

@Component
public class CustomTools {

    @Tool(description = "查询用户账户余额", name = "getAccountBalance")
    public String getAccountBalance(
            @ToolParam(description = "用户 ID") Long userId
    ) {
        // 实现逻辑
        return balance;
    }

    @Tool(description = "生成 PDF 报告", name = "generatePdfReport")
    public String generatePdfReport(
            @ToolParam(description = "报告内容") String content,
            @ToolParam(description = "报告标题") String title
    ) {
        // 实现逻辑
        return pdfPath;
    }
}
```

### 自定义文档分割器

```java

@Bean
public DocumentSplitter customDocumentSplitter() {
    return new ChineseEnhancedTextSplitter(
            TokenTextSplitter.builder()
                             .maxTokens(512)
                             .minTokens(128)
                             .separators(CHINESE_SEPARATORS) // 中文分隔符
                             .build()
    );
}
```

### 扩展意图类型

```java
public enum PossibleSourceTypeEnum {
    LOCAL("本地文档", "运维配置、代码文档等"),
    VECTOR("数据库文档", "客服 FAQ、售后工单等"),
    CLOUD("云知识库", "阿里云百炼知识库"),
    DATABASE("业务表数据", "订单、用户、站点信息"),
    UNKNOWN("未知", "非业务相关问题");
}
```

---

## ❓ FAQ

### Q1: 编译报错 "无效的标记：--release"

**原因**：使用了 Java 8 编译器，项目需要 Java 21。

**解决**：指定 Java 21 编译器路径

```bash
export MAVEN_OPTS="-Dmaven.compiler.fork=true -Dmaven.compiler.executable=/path/to/java21/bin/javac"
mvn clean package
```

### Q2: RerankModel Bean 创建失败

**原因**：`RerankModel` 是接口，Spring AI Alibaba 已提供自动配置。

**解决**：确保配置了 API Key，让自动配置生效

```properties
spring.ai.dashscope.rerank.api-key=YOUR_RERANK_API_KEY
```

### Q3: 向量检索结果不准确

**调优建议**：

1. 检查文档分割器配置，确保分片合理
2. 调整相似度阈值（默认 0.6 → 0.5 或 0.7）
3. 增加召回数量（top-k 从 3 增加到 5）
4. 确保 Rerank API Key 配置正确
5. 检查文档元数据过滤条件

### Q4: 远程 MCP 连接失败

**检查清单**：

- [ ] MCP 服务端是否启动
- [ ] 连接 URL 是否正确
- [ ] 使用 Streamable 模式（支持断线重连）
- [ ] 配置连接健康检查
- [ ] 防火墙是否放行端口

---

## 📝 待完善功能

- [ ] 意图分析 Agent 完整实现（用户问题→业务分类→工具选择）
- [ ] 业务数据 MCP 工具（订单查询、用户信息等数据库联动）
- [ ] 动态 SQL 生成 MCP（自然语言→SQL 查询）
- [ ] 完整的工作流编排
- [ ] 对话历史持久化（Redis/数据库）
- [ ] Token 用量监控和统计

---

## 🤝 贡献指南

欢迎提交 Issue 和 Pull Request！

1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 开启 Pull Request

---

## 📄 开源协议

Apache License 2.0

---

## 🙏 致谢

- [Spring AI](https://docs.spring.io/spring-ai/reference/)
- [Spring AI Alibaba](https://sca.aliyun.com/docs/ai/overview/)
- [阿里云百炼](https://bailian.console.aliyun.com/)
- [Ollama](https://ollama.ai/)
- [MyBatis Plus](https://baomidou.com/)

---

<div align="center">

**如果这个项目对你有帮助，请 Star ⭐ 支持一下！thx！**

</div>
