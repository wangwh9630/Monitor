# Monitor - Alertmanager 语音告警服务

基于 Spring Boot 的 Prometheus Alertmanager Webhook 服务，接收告警后通过阿里云语音服务（DYVMS API）自动拨打电话通知值班人员。

## 快速开始

### 环境要求

- Java 21+
- Maven 3.9+（已内置 Maven Wrapper）

### 配置环境变量

启动前必须设置阿里云凭证：

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

服务默认监听 `8001` 端口，Webhook 地址：`http://localhost:8001/alert`

### 运行测试

```bash
./mvnw.cmd test
```

## 配置说明

配置文件位于 `src/main/resources/application.properties`：

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| `server.port` | 服务端口 | `8001` |
| `aliyun.access-key-id` | 阿里云 AK（环境变量注入） | - |
| `aliyun.access-key-secret` | 阿里云 SK（环境变量注入） | - |
| `aliyun.region` | 阿里云区域 | `cn-hangzhou` |
| `aliyun.voice.template-code` | TTS 语音模板编码 | `TTS_313570112` |
| `aliyun.voice.called-numbers` | 被叫号码列表（逗号分隔） | - |
| `alert.webhook.api-key` | Webhook 认证密钥（可选） | 空（不校验） |
| `alert.rate-limit.permits-per-second` | 每秒最大请求数 | `5` |

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
Alertmanager  -->  POST /alert  -->  AlertController
                                        |
                                        v
                                   验证请求 & 认证
                                        |
                                        v
                                   速率限制 (Semaphore)
                                        |
                                        v
                                   提取告警信息
                                        |
                                   +----+----+
                                   |         |
                                   v         v
                             批量呼叫    单号码呼叫
                           (BatchCall)  (SingleCall)
                                   |
                                   v (失败时降级)
                             逐个单号码呼叫
```

## 阿里云语音服务配置

1. 开通 [阿里云语音服务](https://dyvms.console.aliyun.com/)
2. 创建 TTS 语音模板，获取模板编码（如 `TTS_313570112`）
3. 模板中可使用变量：`${alertname}`、`${description}`
4. 确保 AccessKey 拥有 `DYVMS` 的调用权限

## 项目结构

```
src/main/java/com/example/phone/
  PhoneApplication.java     # Spring Boot 入口
  AppConfig.java            # Bean 配置（客户端、限流器）
  AlertController.java      # 核心控制器（Webhook 处理、语音呼叫）
src/main/resources/
  application.properties    # 应用配置
src/test/java/com/example/phone/
  PhoneApplicationTests.java  # 上下文加载测试
  AlertControllerTest.java    # 控制器单元测试
```

## 参考文档

- [Spring Boot Reference](https://docs.spring.io/spring-boot/3.4.4/reference/)
- [阿里云语音服务 API](https://help.aliyun.com/document_detail/110771.html)
- [Prometheus Alertmanager Webhook](https://prometheus.io/docs/alerting/latest/configuration/#webhook_config)
