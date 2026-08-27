# 给 Z code 的提示词

```text
你负责“小确幸 Android”的重要与复杂任务：Room 基线复核、媒体导入、拍照和录音可靠性。

仓库：https://github.com/skydream9527-ctrl/happy_with_life
基线：最新 main
建议分支：z-code-media-reliability

开始前必须阅读：

- AGENTS.md
- agent-prompts/README.md
- docs/reviews/completed-work-audit-2026-08-27.md
- docs/adr/ADR-001-domain-rules.md
- docs/adr/ADR-002-dependency-injection.md
- docs/room-v2-schema.md
- app/schemas/com.xiaoquexing.app.data.db.AppDatabase/2.json
- GitHub Actions 最近一次 main run

硬边界：

1. 只能在克隆后的仓库根目录内读取、修改和执行命令，禁止越过目录。
2. 禁止创建任何新目录；任务需要的目录若不存在，停止并报告。
3. 不修改 MiniMax 负责的 Profile、Settings、Privacy、About 页面。
4. 不修改 docs/server/，不实现服务端、画册、共享空间、支付或 AI。
5. 禁止本地运行 Gradle；测试、Lint 和 APK 只通过 GitHub Actions。

任务 A：Room v2 与数据可靠性复核

- 对照已提交的 2.json、Entity 和 MIGRATION_1_2，确认 17 张表、外键、索引和默认值完全一致。
- 补齐仍缺失的 DST 日期边界、查询计划或失败恢复测试；不得使用 destructive migration。
- 发现 schema 漂移时修正源码并让 GitHub Actions 重新生成；只提交经过核对的 schema 文件。
- 更新 docs/room-v2-schema.md 的真实完成状态和剩余风险。

任务 B：媒体与录音可靠性

- MediaPicker 必须使用生命周期安全的 Activity Result 接线，不在 Composable 生命周期中直接 registerForActivityResult。
- Photo Picker 内容由 MediaImporter 复制到应用私有目录，失败可重试；历史记录不能依赖短期 content URI。
- 相机取消或失败时清理空临时文件。
- 录音最长 60 秒自动停止；过短、stop 失败、旋转、退后台和离页时安全释放并清理损坏文件。
- 使用现有 Media3 依赖完成播放/暂停、进度和结束状态，同时只允许一个播放器。
- 在现有测试目录中补 JVM/Robolectric 测试；需要设备验证的内容写入现有 docs/smoke-checklist.md。

允许修改的现有范围：

- .github/workflows/build.yml（只有确有必要时）
- app/build.gradle.kts（只有确有必要时）
- app/schemas/
- app/src/main/java/com/xiaoquexing/app/data/
- app/src/main/java/com/xiaoquexing/app/di/
- app/src/main/java/com/xiaoquexing/app/media/
- app/src/main/java/com/xiaoquexing/app/viewmodel/
- app/src/main/java/com/xiaoquexing/app/ui/components/VoiceRecorder.kt
- 与媒体接线直接相关的 app/src/main/java/com/xiaoquexing/app/ui/record/ 文件
- app/src/test/、docs/room-v2-schema.md、docs/smoke-checklist.md

禁止：关闭 Lint、跳过/删除失败测试、使用 git add . 或 git add -A、提交密钥或真实数据。

交付：PR/commit、修改文件清单、故障原因、Room 结论、媒体状态机、GitHub Actions run URL 与三个 Job 结果、剩余设备测试和风险。
```
