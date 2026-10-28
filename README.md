# AiPet

一个支持 Windows 和 macOS 的 AI 桌宠项目。通过桌宠选择 AI 对话、发送问题，用动画提醒回答进度和结果。

当前是 **0.1 桌面主体原型**，使用 Java 21 + Swing，无第三方运行依赖。AI 部分为本地演示，没有连接、读取或向真实 AI 软件发送消息。

## 现在可以做什么

- 启动后直接显示桌宠与圆角悬浮输入栏，无需打开页面即可输入。
- Enter 或右侧箭头发送，Shift+Enter 换行；左侧“＋”选择已有 AI 对话，不再显示顶部信息栏。
- 拖动输入栏右下角调节宽高，也可在主页设置中调整，尺寸会保存。
- 拖动桌宠时输入栏跟随移动。
- 单击桌宠聚焦输入，双击打开主页；右键也可打开主页。启动和回答完成不会自动弹出主页。
- 折叠会话栏、搜索和切换示例对话。
- 发出演示提问，不同会话可以同时生成，发送目标不会随选择变化。
- 查看完成和出错动画，勾选“模拟一次出错”体验失败状态。
- 在输入栏右键菜单查看最近回答；主页中也可查看完整演示记录。
- 调整角色颜色、大小、置顶与提醒，设置自动保存在本机。
- 关闭聊天窗口后桌宠继续显示；点击“退出”或桌宠右键菜单退出。

## IntelliJ IDEA 运行

1. 将根目录 `pom.xml` 作为 Maven 项目导入（右键 → Add as Maven Project，或从该文件打开项目）。
2. Project SDK 选择 JDK 21 或更高版本，语言级别使用 21。
3. 打开 `src/main/java/com/lemanty/aipet/App.java`，点击 `main` 旁边的运行按钮。
4. Maven 模块导入后也可使用项目附带的 `AiPet` 运行配置。

不需要 API Key。Maven 首次导入可能需要下载构建插件，但应用运行不需要外部依赖。

也可以在项目终端运行（要求 `java` 和 `javac` 为 JDK 21+）：

```bash
# macOS
./run.sh

# Windows
run.bat
```

## 验证

```bash
# macOS：任务逻辑回归
./test.sh

# Windows：任务逻辑回归
test.bat

# 可选：桌面界面冒烟验证，需要图形环境；会临时显示测试窗口
java -cp out/test com.lemanty.aipet.UiSmokeTest
java -cp out/test com.lemanty.aipet.QuickChatSmokeTest
```

测试包含目标绑定、不同会话并发、重复提交拦截、错误状态和空输入处理。测试采用独立 main 入口，需运行上述脚本；`mvn test` 不会自动执行这些断言。

## 项目结构

```text
src/main/java/com/lemanty/aipet/
  App.java                  启动入口
  model/                    会话与任务状态
  service/AiAdapter.java    真实 AI 适配器接口
  service/DemoAdapter.java 本地演示适配器
  service/TaskManager.java  固定目标、任务状态与并发管理
  ui/                       主面板、悬浮桌宠、角色绘制与主题
src/test/java/              任务回归与界面冒烟检查
docs/product.md            简版项目功能说明
```

## 后续实现

- 浏览器扩展和本地桥接，接入首个真实 AI。
- Windows / macOS 客户端适配，验证后台和最小化操作。
- 真实历史会话、账号识别、答案定位与断线恢复。
- 角色素材导入、动作编辑与事件映射。

当前只实现演示所需的任务状态。接真实 AI 前，需要扩展提交结果未知、连接中断、停止等状态和目标核对逻辑，不可将演示状态机直接用于真实消息重试。

聊天内容只保存在本次运行内，外观设置使用 Java Preferences 保存。未加入自动更新或安装包，Windows 的实际运行仍需在 Windows 设备验证。

输入栏使用不透明窗口，在支持窗口形状的系统上显示圆角，否则使用矩形以保证输入稳定。角色采用离屏帧绘制，隐藏时暂停动画，待机只在眨眼时刷新。具体系统下的闪烁情况仍需实机观察。
