# RPC 服务定义

- 接口调用参考 base-ai-assistant 工程中 energy-admin-api 的 `TestDocumentFeignController` 和 `TestEnergyAiFeignController`

## 模块概述

- 本 Module 定义 rpc 接口和数据结构
- 可选 feign/dubbo，由于不需要外网交互，其他工程中使用 dubbo 交互即可
- **本模块已兼容 JDK 1.8**，JDK 1.8 或更高版本的工程均可直接引用 jar

## 定义内容范围

- 外部文档管理同步，新增、修改、查询
- 文档语义相似检索
- 文档关键词匹配检索
- 完整检索增强 AI 问答

## 外部依赖引用

- 本模块不兼容的更新需及时变更 SNAPSHOT 版本号
- 如外部需依赖最新版本，请联系运维打包推送到 maven 仓库

## 打包说明

本模块已针对 JDK 1.8 兼容性做了特殊处理，支持在 JDK 1.8 或 JDK 21 环境下打包。

### 场景一：JDK 1.8 环境下独立打包（推荐，产出的 jar 可被任意 JDK 8+ 工程引用）

```bash
cd ces-ai-rpc
mvn clean package -DskipTests
# 或推送到 maven 仓库
mvn clean deploy -DskipTests
```

**注意**：不要使用 `MAVEN_OPTS` 方式指定 JDK 路径，`MAVEN_OPTS` 仅传递 JVM 参数，不会改变使用的 JDK 版本。应确保当前默认 JDK 为 1.8，或通过设置 `JAVA_HOME` 环境变量切换到 JDK 1.8。

### 场景二：全工程 JDK 21 环境下打包

```bash
# Windows PowerShell
$env:JAVA_HOME = "D:\env\graalvm-jdk-21.0.5"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
cd ces-ai-rpc
mvn clean package -DskipTests
```

```bash
# Linux
export JAVA_HOME=/opt/graalvm-jdk-21.0.5
export PATH=$JAVA_HOME/bin:$PATH
cd ces-ai-rpc
mvn clean package -DskipTests
```

上述两种方式产出的 jar 字节码版本均为 JDK 1.8（major version 52），可被 JDK 8+ 工程直接引用。
