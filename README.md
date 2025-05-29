# LHM Client Demo

一个基于 [LibreHardwareMonitor](https://github.com/LibreHardwareMonitor/LibreHardwareMonitor) 的 Android 客户端（Demo）。本项目使用 Kotlin + Jetpack Compose 编写，通过 LHM 的 Web Server 实时获取硬件状态信息并进行可视化展示。适用于需要在手机端查看远程或本地计算机硬件信息的用户。当前实现了基本的数据获取与展示功能，并计划添加更多功能（还在研究功能实现）。

## 原理

LibreHardwareMonitor 启动 Web Server 功能后，会生成一个 `data.json` 接口（如：`https://<URL>/data.json`）。本客户端定期 fetch 此 JSON 数据并进行处理展示。由于 LHM 限制最小刷新频率为 1 秒，因此Demo的刷新周期为 1000ms。

## 当前功能

* 设备 IP/域名 使用 SharedPreference 存储

> 格式支持：域名 or IP:Port 。

* 实时获取 JSON 数据并格式化展示

* 使用 Material Design 3，动态取色

* 简单过渡动画

* 仅一个 `MainActivity`，后续将进行模块化

## 已知问题

* 网络速度无法显示

## 技术栈

* Kotlin + AndroidX
* Jetpack Compose + MD3
* ViewModel
* SharedPreferences
* JSON 解析（OkHttp + Gson）

## TODO

- [ ] 支持亮/暗主题切换

- [ ] 支持 Monet 自定义主题色

- [ ] 自适配不同屏幕尺寸

- [ ] 多语言支持

- [ ] 支持缓存（广域网不稳定时使用缓存并提示）

- [ ] 完善过渡动画

- [ ] 所有设置实时保存

- [ ] 数据缺失时自动隐藏无效模块

- [ ] 完善GUI

- [ ] 支持图表可视化、最大/最小/平均值展示

>双指缩放 & 滑动查看历史
>
>默认展示 10 分钟数据
>
>点击数据点展示 Tooltip（值 + 时间戳）
>
>环形进度条展示内存/显存使用率（>80% 红色提示）
>
>折线图展示（比如CPU各线程）

- [ ] 支持模块隐藏与排序（拖拽）

- [ ] 添加高级筛选功能

- [ ] 支持下拉刷新

- [ ] 展示所有已配置设备及其基本信息

- [ ] 设备列表

>点击查看断线原因
>
>支持设备列表拖动重新排序
>
>设备长按弹出菜单（编辑、删除、详情）

- [ ] 添加设备 FAB：打开对话框自定义参数

>设备名称（可选）
>
>数据源（必填）
>
>端口（默认 8085）
>
>JSON 路径（默认 `/data.json`）
>
>刷新周期（默认 1000ms）
>
>扫描线程（默认每 1500ms 扫描 150 个线程）
>
>平均数据时间范围（默认 10 分钟）
>
>是否启用 HTTP auth
>
>设备备注

- [ ] 局域网设备扫描（扫描 8085 端口是否存在 `/data.json`）

- [ ] 设置项

>折线图时间范围（秒）
>
>全局刷新周期（≥1000ms）
>
>全局扫描线程设置
>
>平均值时间范围
>
>显示最大/最小/平均值的开关
>
>一键恢复默认设置（保留设备列表）
>
>导入/导出配置（JSON，含设备列表，文件名加时间戳，如 `config_19831106.json`）

- [ ] 自动重连（默认 30 秒）

- [ ] FPS：不自动判断是否在玩游戏，由用户设置是否显示（设置中添加开关）

## License

[GPLv3 License](LICENSE)
