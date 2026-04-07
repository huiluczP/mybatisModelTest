# MyBatis Mapper 自动测试工具

## 项目简介

一个基于反射和 Git 扫描的 MyBatis Mapper 自动测试工具。通过输入关键词扫描 Git 提交历史，自动定位被修改的 Mapper XML 文件，解析其中的 SQL 方法，生成测试数据并通过反射调用，最终输出测试报告。

## 基本思路

```
输入关键词 (e.g. RCS-4286)
    │
    ▼
[1] GitCommitDetector: 扫描 Git 提交，找出包含关键词的 commits
    │
    ▼
[2] 从 commits 中提取所有被修改的 *Mapper.xml → 去重得到 mapper 列表
    │
    ▼
[3] 对每个 Mapper:
    ├── XMLMethodParser: 解析 XML 得到方法列表 (MethodInfo)
    ├── POEntityRegistry: 解析 Mapper 名 → PO 实体类名
    ├── ReflectionTester: 为每个方法:
    │   ├── 根据方法参数类型自动生成测试数据
    │   ├── 通过反射调用 mapper 方法
    │   └── 记录 TestResult（成功/失败/耗时）
    └── 收集所有结果
    │
    ▼
[4] ReportGenerator: 打印控制台报告 + 生成 HTML 报告
```

## 核心模块

| 模块 | 说明 |
|------|------|
| `GitCommitDetector` | 使用 JGit 扫描 Git 提交，定位包含关键词的 commit，找出被修改的 Mapper XML |
| `XMLMethodParser` | 使用 dom4j 解析 Mapper XML，提取 insert/update/select/delete 方法 |
| `POEntityRegistry` | Mapper 名称到实体类的映射：XML parameterType > 自动推断 > 配置文件覆盖 |
| `ReflectionTester` | 反射测试器：根据方法签名自动生成测试数据并调用方法 |
| `ReportGenerator` | 生成测试报告（控制台 + HTML） |
| `MyBatisSessionFactory` | MyBatis SqlSessionFactory 工具类 |

## 技术栈

- **Java 11**
- **Maven** 单模块结构
- **MyBatis 3.5.x**
- **MySQL 8.0**
- **JGit** — Git 扫描
- **dom4j** — XML 解析
- **Slf4j + Logback** — 日志

## 环境要求

- JDK 11+
- Maven 3.6+
- MySQL 8.0，已创建 `test` 数据库
- 项目为 Git 仓库（包含至少一次提交）

## 使用前配置

编辑 `src/main/resources/mybatis-config.xml`，修改数据库连接信息：

```xml
<property name="username" value="你的用户名"/>
<property name="password" value="YOUR_PASSWORD"/>
```

## 如何启动

### 1. 编译

```bash
mvn compile
```

### 2. 运行

```bash
mvn exec:java -Dexec.mainClass="com.mybatis.test.core.Main"
```

程序会交互式提示输入：

```
请输入 Git 仓库路径（默认当前目录）:  ← 直接回车使用当前目录
请输入关键词（如 RCS-4286）:         ← 输入关键词，如 RCS-9527
```

### 3. 查看报告

- 控制台：执行完毕后直接显示
- HTML：`reports/test-report-{timestamp}.html`，用浏览器打开即可查看

## 测试场景

### 场景一：全部通过（关键词 RCS-9527）

所有 Mapper 方法均正常执行，无 SQL 错误。

```
Total: 8 | Passed: 8 | Failed: 0
汇总: 全部通过
```

> 查看 HTML 报告：[全部通过报告](reports/test-report-20260407_214844.html)

### 场景二：模拟 SQL 错误（关键词 RCS-0468）

在 `updateIgnoreNull` 方法中写入不存在的列名 `stock_quantity_wrong`，模拟 XML 中的 SQL 错误。

```
Total: 8 | Passed: 7 | Failed: 1
  [3] [FAIL] ProductInfoMapper.updateIgnoreNull
      错误: Unknown column 'stock_quantity_wrong' in 'field list'
汇总: 1 个测试失败
```

> 查看 HTML 报告：[含错误报告](reports/test-report-20260407_222801.html)

可见单个方法的 SQL 错误不会中断整体流程，其余方法继续执行，最终在报告中清晰标记失败原因。
