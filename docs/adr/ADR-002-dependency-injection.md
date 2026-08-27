# ADR-002：依赖注入方式——手工 AppContainer（暂不引入 Hilt）

- **状态**：已接受（Accepted）
- **日期**：2026-08-27
- **决策人**：Z code（任务 Z0-03）
- **关联**：`docs/product/architecture-v0.1.md` §1.2（长期选型 Hilt）、`docs/plans/iteration-plan-v1.0.md` I0/Z0-03

## 背景

Z0-03 验收要求「repository 可替换 fake；ViewModel 可做 JVM 测试」。现状是 6 个 ViewModel 全部继承 `AndroidViewModel` 并向下转型 `(application as XiaoQueXingApp)` 取仓库，且 `XiaoQueXingApp` 暴露全局单例 `instance`，任何单元测试都必须拉起整个 Application。

架构方案长期选型是 Hilt，但本项目约定**只通过 GitHub Actions 构建验证、不本地执行 Gradle**；引入 Hilt 意味着新增 KAPT/KSP、hilt-android-gradle-plugin 与 Kotlin 1.9.20 / AGP 8.3.2 的版本耦合，在无法本地编译验证的情况下首投红盘风险高。

## 决策

1. **采用手工 DI**：`di/AppContainer`（database + 三个 repository 的 lazy 容器）+ `di/XiaoQueXingViewModelFactory`。
2. **全部 ViewModel 改为构造注入**普通 `ViewModel`（`ShareViewModel` 额外显式接收 Context），删除全部 `application as XiaoQueXingApp` 向下转型与全局 `instance`。
3. **注入点**：`rememberXiaoQueXingViewModelFactory()` Compose 帮助函数，各 Screen 默认参数 `viewModel(factory = ...)` 传入。**不**覆盖 MainActivity 的 `defaultViewModelProviderFactory`——NavBackStackEntry 有自己的默认工厂，导航目的地内 `viewModel()` 不会走 Activity 工厂，覆盖是无效的。
4. `AppDatabase` 移除 `fallbackToDestructiveMigration()`（ADR-001 K4 的硬性约束，本轮顺手落地），并开启 `exportSchema = true` + `room.schemaLocation`（Z0-02 要求的 schema JSON 导出）。

## 后果

- Repository/DAO 可用 fake 直接构造 ViewModel 做纯 JVM 测试（Z0-05 的 `FakeRecordDao` 即第一个样例）。
- 新增 ViewModel 时必须在工厂 `when` 里登记一行；漏登记会在运行时抛 `IllegalArgumentException`，CI 无法静态发现——PR review 检查项。
- **Hilt 迁移路径（M4 前后择机）**：`@HiltAndroidApp` 替换 Application、`@HiltViewModel + @Inject constructor` 替换工厂、AppContainer 的 lazy 提供者逐个搬进 `@Module`；Screen 侧删掉 `factory = rememberXiaoQueXingViewModelFactory()` 即可，构造注入的 ViewModel 签名不变，迁移是机械操作。
