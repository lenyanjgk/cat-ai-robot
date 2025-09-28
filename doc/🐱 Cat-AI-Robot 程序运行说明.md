# 🐱 Cat-AI-Robot 程序运行说明

制作人：吴光耀

制作时间：2025-09-27

版本号：v1.0.0

## 目录

- [1. 项目概述](#1-项目概述)
- [2. 系统环境要求](#2-系统环境要求)
  - [2.1 软件环境](#21-软件环境)
  - [2.2 硬件环境](#22-硬件环境)
- [3. 数据库部署（PostgreSQL + pgvector）](#3-数据库部署postgresql--pgvector)
- [4. 对象存储部署（Minio）](#4-对象存储部署minio)
- [5. AI 服务配置（阿里云百炼）](#5-ai-服务配置阿里云百炼)
- [6. 后端部署（SpringBoot）](#6-后端部署springboot)
- [7. 前端部署（Vue3 + Vite4）](#7-前端部署vue3--vite4)
- [8. 配置说明](#8-配置说明)
- [9. 联系方式](#9-联系方式)

------

## 1. 项目概述

- **项目名称**：`Cat-AI-Robot`
- **项目类型**：前后端分离网站
- **开发语言/框架**：
  - **后端**：Java, Spring, Spring MVC, Spring AI, SpringBoot, Mybatis, Mybatis-Plus, Minio, PostgreSQL
  - **前端**：Vue3, Vite4, Axios, Pinia, Ant Design Vue, Tailwind CSS, markdown-it, highlight.js
- **数据库**：PostgreSQL
- **对象存储服务**：Minio

### 功能简介

1. **AI 聊天**：文本对话，支持多轮上下文，智能生成回复。
2. **语音对话**：语音输入识别与语音合成，支持实时交互。
3. **角色管理**：自定义 AI 角色，包括名称、头像及系统提示，支持多角色切换。
4. **文件与媒体管理**：基于 Minio 对象存储上传、管理语音、图片等多媒体文件。
5. **用户会话管理**：支持用户查看历史会话与历史消息记录。

------

## 2. 系统环境要求

### 2.1 软件环境

| 软件       | 版本要求   |
| ---------- | ---------- |
| Java       | 21+        |
| SpringBoot | 3          |
| Maven      | 3.6+       |
| Node.js    | 22.11.0+   |
| PostgreSQL | 17         |
| Minio      | 最新稳定版 |

### 2.2 硬件环境

- CPU：2 核以上
- 内存：2 GB 以上
- 硬盘：50 GB 以上（根据数据量调整）

------

## 3. 数据库部署（PostgreSQL + pgvector）

```
# 拉取 pgvector 镜像
docker pull pgvector/pgvector:pg17

# 创建宿主机挂载目录
E:\docker\pgvector2\data

# 启动容器
docker run -d -p 5432:5432 \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -e POSTGRES_DB=robot \
  -v E:\docker\pgvector2\data:/var/lib/postgresql/data \
  --name pgvector2 pgvector/pgvector:pg17
```

- 创建数据库：

```
CREATE DATABASE robot;
```

- 导入初始化数据：

```
psql -U postgres -d robot -f init.sql
psql -U postgres -d robot -f default_roles_insert.sql
```

- 配置后端 `application.yml`：

```
spring:
  datasource:
    driver-class-name: com.p6spy.engine.spy.P6SpyDriver
    url: jdbc:p6spy:postgresql://localhost:5432/robot
    username: postgres
    password: postgres
```

------

## 4. 对象存储部署（Minio）

```
# 下载 Minio 镜像
docker pull minio/minio:RELEASE.2023-09-30T07-02-29Z

# 创建宿主机挂载目录
E:\docker\minio\data

# 启动 Minio 容器
docker run -d \
  -p 9000:9000 \
  -p 9090:9090 \
  --name minio \
  -v E:\docker\minio\data:/data \
  -e "MINIO_ROOT_USER=catairobot" \
  -e "MINIO_ROOT_PASSWORD=catairobot" \
  minio/minio:RELEASE.2023-09-30T07-02-29Z server /data --console-address ":9090"
```

- 访问控制台：

```
http://localhost:9090
用户名 / 密码: catairobot / catairobot
```

- 创建 Bucket `cat-ai-robot` 并设置公共读
- 后端配置：

```
minio:
  endpoint: http://127.0.0.1:9000
  accessKey: catairobot
  secretKey: catairobot
  bucketName: cat-ai-robot
```

------

## 5. AI 服务配置（阿里云百炼）

Cat-AI-Robot 后端使用阿里云百炼（Aliyun Bailian）提供的 AI 接口作为对话和语音服务。部署时需要配置 API Key 并指定服务地址。

### 5.1 获取 API Key

1. 访问 [阿里云百炼官网](https://bailian.console.aliyun.com/) 注册账号。
2. 登录后，进入 **模型管理**，创建或选择对应模型，获取 **API Key**。

### 5.2 后端配置示例（`application.yml`）

```
spring:
  ai:
    openai:
      base-url: https://dashscope.aliyuncs.com/compatible-mode  # 阿里云百炼 OpenAI 兼容服务地址
      api-key: your-api-key-here  # 替换为你申请的 API Key
```

### 5.3 注意事项

- **安全**：API Key 为敏感信息，建议通过环境变量或密钥管理工具加载，不要直接写入代码仓库。
- **性能**：百炼接口调用有并发和速率限制，请在生产环境做合理请求控制。
- **测试**：可先在本地开发环境测试 API Key 是否可用，再部署到正式环境。

------

## 6. 后端部署（SpringBoot）

```
# 克隆项目
git clone https://github.com/lenyanjgk/cat-ai-robot.git
cd cat-ai-robot-springboot

# 安装依赖并打包
mvn clean install

# 启动项目
mvn spring-boot:run
# 或使用打包后的 Jar
java -jar target/backend_project.jar
```

- 默认端口：`8081`（可在 `application.yml` 修改）

------

## 7. 前端部署（Vue3 + Vite4）

```
# 进入前端项目
cd cat-ai-robot-vue3

# 安装依赖
npm install

# 启动开发环境
npm run dev

# 生产环境打包
yarn build
```

- 默认访问地址：`http://localhost:5173`
- 打包后文件在 `dist/`，可通过 Nginx 或其他静态服务部署

------

## 8. 配置说明

| 配置文件          | 内容说明                                   |
| ----------------- | ------------------------------------------ |
| `application.yml` | 后端：数据库、Minio、AI 服务、日志、端口等 |
| `vite.config.js`  | 前端：API 地址、环境变量、构建参数等       |

------

## 9. 联系方式

| 姓名   | 邮箱                | 电话        |
| ------ | ------------------- | ----------- |
| 吴光耀 | 1582316589@qq.com   | 17734894660 |
| 江国凯 | 13534758041@163.com | 13534758041 |
| 李天宇 | 2543482922@qq.com   | 18171016991 |