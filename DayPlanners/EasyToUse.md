Day Planner 与前文提到的 Tasks ，都能对笔记库中的 To-do 内容进行集中管理，Tasks 插件会更自由灵活，它能够扫描到笔记库内**任意地方**的 To-do 内容；而 **Day Planner 则只能扫描特定的文件夹**，但换来了更加精细、更加有趣的日程/任务管理方式。


- 文档顶部自动生成甘特图
- 右侧自动生成时间线
- 底部右下角有全局的任务提示
- 当任务到点时，还能弹出系统级的通知提醒


### 文档模式 File mode

安装完 Day Planner 插件后

1. 会默认开启 `File mode` 功能
    
2. 会在笔记列表根目录中自动创建名为 `Day Planner` 的文件夹
    
3. 在 `Day Planner` 文件夹内，每天都会自动创建一个新的日程文件
    
4. 你需要用下列语法格式来书写你的日程，才能被插件正确识别
    `## 任务标题-[]07:00 任务`

	- 你也可以使用 Ctrl+Enter 来快速创建 Checkbox
	- 时间需要 24 小时制，1:00 应写成 01:00    
	- 时间与任务描述之间需要空格


5. 如果右侧时间线没有正确显示，请使用下列操作：
	- 使用快捷键 Ctrl+P 唤出命令面板  
	- 搜索关键词 `show` ，然后选择 `Show the Day Planner Timeline`

### 命令模式 Command mode

在插件设置中，将 File mode 切换到 Command mode，**能够让你在任意一页文档中使用 Day Planner 插件**，而无需创建额外的 Day Planner 文件夹。任务书写语法与 `File mode` 一样，但你还需要执行下面的操作

1. 使用快捷键 Ctrl+P 唤出命令面板
2. 搜索关键词 `link` ，然后选择 `Link todays' Day Planner to the current note`