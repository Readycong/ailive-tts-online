# 登录功能说明

## 功能概述

本项目已经添加了完整的手机号+密码登录功能，包含以下特性：

### 1. 登录页面
- 手机号输入验证（中国大陆手机号格式）
- 密码输入（隐藏显示）
- 登录按钮加载状态
- 错误信息提示
- 注册/忘记密码链接（占位）

### 2. 状态管理
- 使用DataStore持久化存储登录状态
- ViewModel管理登录逻辑和状态
- 自动登录检查

### 3. 注销功能
- 在"我的"页面添加注销按钮
- 清除本地存储的登录信息

## 使用说明

### 测试账号
- 手机号：`13800138000`
- 密码：`123456`

### 其他测试用例
- 错误手机号：会提示"手机号格式不正确"
- 密码过短：会提示"密码长度不能少于6位"
- 错误凭证：会提示"手机号或密码错误"

## 文件结构

```
app/src/main/java/com/danshuang/AiLive/
├── ui/screens/
│   ├── LoginScreen.kt          # 登录页面UI
│   ├── LoginScreenPreview.kt   # 登录页面预览
│   └── ProfileScreen.kt        # 个人中心（添加注销功能）
├── data/
│   └── LoginRepository.kt      # 登录状态持久化
├── viewmodel/
│   └── LoginViewModel.kt       # 登录逻辑管理
└── navigation/
    └── AppNavigation.kt        # 导航逻辑（登录状态检查）
```

## 扩展建议

1. **实际API集成**：替换`LoginViewModel`中的模拟登录逻辑为真实API调用
2. **注册功能**：实现注册页面和注册API
3. **忘记密码**：实现密码重置流程
4. **记住密码**：添加"记住我"功能
5. **验证码登录**：添加短信验证码登录选项
6. **第三方登录**：集成微信、QQ等第三方登录

## 依赖项

项目已添加以下依赖：
- `androidx.datastore:datastore-preferences:1.1.1` - 用于数据持久化
- `androidx.lifecycle:lifecycle-viewmodel-compose` - 用于ViewModel集成

## 构建和运行

1. 同步Gradle依赖
2. 构建项目
3. 运行应用，将首先显示登录页面
4. 使用测试账号登录后，将跳转到主界面
5. 在"我的"页面可以点击"退出登录"返回登录页面