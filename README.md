# Jumpster - 跳绳计数应用

![Jumpster Logo](app/src/main/res/mipmap-xxxhdpi/ic_launcher_round.png)

Jumpster 是一款简洁高效的跳绳计数应用，帮助用户轻松记录日常跳绳锻炼数据，追踪健身进度，并提供数据可视化分析。

## 功能特点

### 核心功能

- **跳绳计数**：快速记录每次跳绳次数，支持"追加"和"覆盖"两种记录模式
- **撤销操作**：支持撤销最近一次输入操作，防止误操作
- **智能输入校验**：超大值保护、前导零处理、空输入提示等
- **日历视图**：月度日历一目了然，查看每日跳绳记录
- **趋势图表**：周/月趋势图，支持柱状图和折线图两种展示方式
- **分享成绩**：生成精美的今日/本月成绩分享卡片，一键分享至社交媒体

### 技术特点

- **Material Design 3**：遵循最新的 Material Design 设计规范
- **深色模式**：完整支持深色模式，优化夜间使用体验
- **Room 数据库**：本地数据持久化，确保数据安全
- **响应式 UI**：流畅的用户界面和交互体验
- **高效性能**：优化的数据处理和 UI 渲染

## 应用截图

<table>
  <tr>
    <td><img src="screenshots/today_count.png" alt="今日计数" width="200"/></td>
    <td><img src="screenshots/month_calendar.png" alt="月度日历" width="200"/></td>
    <td><img src="screenshots/trend_chart.png" alt="趋势图表" width="200"/></td>
  </tr>
  <tr>
    <td><img src="screenshots/share_card.png" alt="分享卡片" width="200"/></td>
    <td><img src="screenshots/dark_mode.png" alt="深色模式" width="200"/></td>
    <td><img src="screenshots/settings.png" alt="设置" width="200"/></td>
  </tr>
</table>

## 使用指南

### 今日计数

1. 打开应用，进入今日计数页面
2. 输入跳绳次数，点击"追加"按钮将次数累加到今日总数
3. 或点击"覆盖"按钮直接设置今日总数
4. 如需撤销最近一次操作，点击右上角"撤销"按钮

### 月度统计

1. 点击顶部菜单栏的"月度统计"图标
2. 在日历视图中查看每日跳绳记录
3. 点击具体日期查看详细记录
4. 页面底部显示本月和本周累计数据

### 趋势图表

1. 点击顶部菜单栏的"趋势图"图标
2. 切换"周趋势"或"月趋势"标签查看不同时间段数据
3. 切换"柱状图"或"折线图"标签更改图表样式

### 分享成绩

1. 点击顶部菜单栏的"分享"图标
2. 系统会生成精美的成绩分享卡片
3. 选择分享平台，将成绩分享给朋友

## 技术架构

- **编程语言**：Kotlin
- **架构模式**：MVVM (Model-View-ViewModel)
- **数据库**：Room Persistence Library
- **UI 组件**：Material Components, MPAndroidChart
- **日历组件**：Kizitonwose CalendarView

## 开发环境

- Android Studio Iguana | 2023.2.1
- Kotlin 2.0.21
- Gradle 8.13.0
- minSdk 34
- targetSdk 36

## 未来计划

- [ ] 数据备份与恢复功能
- [ ] 目标设定与提醒
- [ ] 成就系统
- [ ] 桌面小部件
- [ ] 多设备同步

## 贡献指南

欢迎贡献代码、报告问题或提出新功能建议！请遵循以下步骤：

1. Fork 本仓库
2. 创建您的特性分支 (`git checkout -b feature/amazing-feature`)
3. 提交您的更改 (`git commit -m 'Add some amazing feature'`)
4. 推送到分支 (`git push origin feature/amazing-feature`)
5. 开启一个 Pull Request

## 许可证

本项目采用 MIT 许可证 - 详见 [LICENSE](LICENSE) 文件

## 联系方式

如有任何问题或建议，请通过以下方式联系我们：

- 项目主页：[GitHub](https://github.com/yourusername/jumpster)
- 电子邮件：your.email@example.com

---

**Jumpster** - 让跳绳记录更简单，健康生活每一天！
