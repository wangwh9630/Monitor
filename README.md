# Monitor - Alertmanager 语音告警服务

基于 Spring Boot 的 Prometheus Alertmanager Webhook 服务，接收告警后通过阿里云语音服务（DYVMS API）自动拨打电话通知值班人员。内置 Web 管理界面，支持在线管理号码、模板和查看呼叫记录。

## 快速开始

### 环境要求

- Java 21+
- Maven 3.9+（已内置 Maven Wrapper）

### 配置环境变量

启动前设置阿里云凭证：

```bash
export ALIYUN_ACCESS_KEY_ID=你的AccessKeyId
export ALIYUN_ACCESS_KEY_SECRET=你的AccessKeySecret
export ALERT_WEBHOOK_API_KEY=你的Webhook密钥   # 可选，留空则跳过认证
```

### 构建与运行

```bash
# 构建
./mvnw.cmd clean package

# 运行
java -jar target/phone-0.0.1-SNAPSHOT.jar

# 或直接 Maven 运行
./mvnw.cmd spring-boot:run
```

服务默认监听 `8001` 端口。

### 运行测试

```bash
./mvnw.cmd test
```

## Web 管理页面

启动后可通过浏览器访问以下页面：

| 页面 | URL | 功能 |
|------|-----|------|
| 仪表盘 | http://localhost:8001/ | 服务状态概览、呼叫记录查询（支持按告警名称搜索、分页） |
| 号码管理 | http://localhost:8001/phones | 在线添加、启用/禁用、删除被叫号码 |
| 模板管理 | http://localhost:8001/templates | 在线管理 TTS 语音模板，支持多模板切换 |
| 测试面板 | http://localhost:8001/test | 手动发送测试告警，验证呼叫是否正常 |
| 健康检查 | http://localhost:8001/health | 返回 JSON 格式的服务状态信息 |
| H2 Console | http://localhost:8001/h2-console | 内置数据库管理控制台（用户名 `sa`，密码为空） |

### 仪表盘

- 显示号码数量、呼叫记录数、模板数量统计卡片
- 呼叫记录表格：时间、告警名称、级别、描述、号码、状态、CallId
- 支持按告警名称模糊搜索
- 支持分页浏览

### 号码管理

- 添加新号码（自动校验手机号格式）
- 启用/禁用号码（禁用后不会被呼叫）
- 删除号码
- 号码数据持久化在 H2 数据库中

### 模板管理

- 添加 TTS 模板编码（如 `TTS_313570112`）
- 为模板设置名称便于识别
- 启用/禁用模板（系统使用第一个启用的模板）
- 删除模板

### 测试面板

- 填写告警名称、级别、描述
- 点击发送后会实际拨打电话
- 页面展示呼叫结果

## 配置说明

配置文件位于 `src/main/resources/application.properties`：

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| `server.port` | 服务端口 | `8001` |
| `aliyun.access-key-id` | 阿里云 AK（环境变量注入） | - |
| `aliyun.access-key-secret` | 阿里云 SK（环境变量注入） | - |
| `aliyun.region` | 阿里云区域 | `cn-hangzhou` |
| `aliyun.voice.template-code` | 默认 TTS 语音模板编码 | `TTS_313570112` |
| `alert.webhook.api-key` | Webhook 认证密钥（可选） | 空（不校验） |
| `alert.rate-limit.permits-per-second` | 每秒最大请求数 | `5` |
| `spring.datasource.url` | H2 数据库路径 | `jdbc:h2:file:./data/monitor` |
| `spring.h2.console.enabled` | 启用 H2 控制台 | `true` |
| `spring.thymeleaf.cache` | Thymeleaf 模板缓存 | `false` |

号码和模板通过 Web 管理页面在线管理，不再需要在配置文件中维护。

## API 接口

### POST /alert

接收 Prometheus Alertmanager 的 Webhook 回调。

**请求头：**

| Header | 必填 | 说明 |
|--------|------|------|
| `Content-Type` | 是 | `application/json` |
| `X-API-Key` | 否 | 当配置了 `alert.webhook.api-key` 时必填 |

**请求体（Alertmanager 格式）：**

```json
{
  "alerts": [
    {
      "labels": {
        "alertname": "HighCPU",
        "severity": "critical"
      },
      "annotations": {
        "description": "CPU usage > 90%"
      }
    }
  ]
}
```

**响应：**

- 成功：返回呼叫结果 JSON
- 认证失败：`401 Unauthorized`
- 限流：`429 Too Many Requests`
- 参数无效：返回 `Error: ...` 描述

### GET /health

返回服务健康状态：

```json
{
  "status": "UP",
  "timestamp": "2026-05-21T11:55:10",
  "acsClient": "connected",
  "phoneNumbers": 3,
  "activeTemplate": "TTS_313570112"
}
```

## Alertmanager 配置示例

在 Alertmanager 的 `alertmanager.yml` 中添加 Webhook 接收器：

```yaml
receivers:
  - name: phone-alert
    webhook_configs:
      - url: 'http://<服务地址>:8001/alert'
        send_resolved: false
        http_config:
          authorization:
            type: Bearer
            credentials: '<你的API Key>'
```

## 架构说明

```
                    浏览器
                      |
        +-------------+-------------+
        |             |             |
   仪表盘 /       号码管理 /     测试面板 /
   呼叫记录       模板管理       发送告警
        |             |             |
        v             v             v
   Dashboard     Phone/Tpl      TestController
   Controller    Controller         |
        |             |             v
        v             v        POST /alert
   CallRecord    PhoneNumber/        |
   Service       TtsTemplate         v
        |         Service      AlertController
        v             |             |
     H2 Database <----+             v
                              PhoneNumberService
                              TtsTemplateService
                              CallRecordService
                                    |
                                    v
                              阿里云 DYVMS API
                             (SingleCall/BatchCall)
```

## 阿里云语音服务配置

1. 开通 [阿里云语音服务](https://dyvms.console.aliyun.com/)
2. 创建 TTS 语音模板，获取模板编码（如 `TTS_313570112`）
3. 模板中可使用变量：`${alertname}`、`${description}`
4. 确保 AccessKey 拥有 `DYVMS` 的调用权限

## 项目结构

```
src/main/java/com/example/phone/
  PhoneApplication.java          # Spring Boot 入口
  AppConfig.java                 # Bean 配置（客户端、限流器、RestTemplate）
  AlertController.java           # 核心控制器（Webhook 处理、语音呼叫、记录保存）
  entity/
    CallRecord.java              # 呼叫记录实体
    PhoneNumber.java             # 号码管理实体
    TtsTemplate.java             # TTS 模板实体
  repository/
    CallRecordRepository.java    # 呼叫记录查询
    PhoneNumberRepository.java   # 号码查询
    TtsTemplateRepository.java   # 模板查询
  service/
    CallRecordService.java       # 呼叫记录业务
    PhoneNumberService.java      # 号码 CRUD
    TtsTemplateService.java      # 模板 CRUD
  controller/
    DashboardController.java     # GET / 仪表盘
    PhoneController.java         # /phones 号码管理
    TemplateController.java      # /templates 模板管理
    TestController.java          # /test 测试面板
    HealthController.java        # /health 健康检查
src/main/resources/
  application.properties         # 应用配置
  templates/
    layout.html                  # 公共布局（侧边栏 + Bootstrap 5）
    dashboard.html               # 仪表盘页面
    phones.html                  # 号码管理页面
    templates.html               # 模板管理页面
    test.html                    # 测试面板页面
src/test/java/com/example/phone/
  PhoneApplicationTests.java     # 上下文加载测试
  AlertControllerTest.java       # 控制器单元测试
data/                            # H2 数据库文件（运行时自动生成，已 gitignore）
```

## 参考文档

- [Spring Boot Reference](https://docs.spring.io/spring-boot/3.4.4/reference/)
- [Thymeleaf](https://www.thymeleaf.org/documentation.html)
- [阿里云语音服务 API](https://help.aliyun.com/document_detail/110771.html)
- [Prometheus Alertmanager Webhook](https://prometheus.io/docs/alerting/latest/configuration/#webhook_config)
- [H2 Database](https://h2database.com/html/main.html)
