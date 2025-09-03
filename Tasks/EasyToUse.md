Tasks插件的功能是：将分散于各地的日志集中展示管理

下面是简单的使用示例：

## 所有未完成
在Kanban/、Templater/日报/、Templater/日记/、Templater/周报/文件夹下

### **Kanban/学习路线.md：**
```tasks
filter by function task.file.filename === "学习路线.md"
not done
sort by heading
```






### **Kanban/日常学习.md：**
```tasks
filter by function task.file.filename === "日常学习.md"
not done
sort by heading
```






### **Kanban/工作.md：**
```tasks
filter by function task.file.filename === "工作.md"
not done
sort by heading
```






### **Templater/日记/：**
```tasks
filter by function task.file.folder === "Templater/日记/"
not done
sort by due
```







### **Templater/日报/**
```tasks
filter by function task.file.folder === "Templater/日报/"
not done
sort by due
```






### **Templater/周报/**
```tasks
filter by function task.file.folder === "Templater/周报/"
not done
sort by due
```







## 明日到期
```tasks
due before tomorrow
sort by due
```






## 今日到期
```tasks
not done
due yesterday
sort by due
```






## 昨日未完成
```tasks
not done
due yesterday
sort by due
```






## 一周内到期
```tasks
not done
due before {{next week}}
```






## 一周后的任务
```tasks
not done
due after {{next week}}
```






## 本月任务
```tasks
not done
due before {{next month}}
sort by due
```






## 所有已完成
```tasks
done
```





