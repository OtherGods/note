## 摘要

大家在使用Git时，都会选择一种Git客户端，在IDEA中内置了这种客户端，可以让你不需要使用Git命令就可以方便地进行操作，本文将讲述IDEA中的一些常用Git操作。

## 环境准备

- 使用前需要安装一个远程的Git仓库和本地的Git客户端，具体参考：[10分钟搭建自己的Git仓库](https://link.juejin.cn?target=https%3A%2F%2Fmp.weixin.qq.com%2Fs%2F6GyYlR9lpVcjgYmHMYLi0w "https://mp.weixin.qq.com/s/6GyYlR9lpVcjgYmHMYLi0w")。
- 由于IDEA中的Git插件需要依赖本地Git客户端，所以需要进行如下配置：
![](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/16cd8630f2ed5278~tplv-t2oaga2asx-zoom-in-crop-mark:3024:0:0:0.awebp)


## 操作流程

> 我们这里使用mall-tiny项目的源代码来演示，尽可能还原一个正式的操作流程。

### 在Gitlab中创建一个项目并添加README文件

![](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/16cd8630f2f90f89~tplv-t2oaga2asx-zoom-in-crop-mark:3024:0:0:0.awebp)

### clone项目到本地

- 打开从Git检出项目的界面：

![](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/16cd8630f59452e8~tplv-t2oaga2asx-zoom-in-crop-mark:3024:0:0:0.awebp)

- 输入Git地址进行检出：
![](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/16cd8630f78bfb2a~tplv-t2oaga2asx-zoom-in-crop-mark:3024:0:0:0.awebp)


- 暂时不生成IDEA项目，因为项目还没初始化：
![](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/16cd8630f774208c~tplv-t2oaga2asx-zoom-in-crop-mark:3024:0:0:0.awebp)


### 初始化项目并提交代码

- 将mall-tiny的代码复制到该目录中：
![](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/16cd8630f9a3834b~tplv-t2oaga2asx-zoom-in-crop-mark:3024:0:0:0.awebp)


- 这里我们需要一个.gitignore文件来防止一些IDEA自动生成的代码被提交到Git仓库去：

```
# Maven #
target/

# IDEA #
.idea/
*.iml

# Eclipse #
.settings/
.classpath
.project
```


- 使用IDEA打开项目：
![](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/16cd863124fa7ac7~tplv-t2oaga2asx-zoom-in-crop-mark:3024:0:0:0.awebp)


- 右键项目打开菜单，将所有文件添加到暂存区中：
![](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/16cd863128fe591b~tplv-t2oaga2asx-zoom-in-crop-mark:3024:0:0:0.awebp)


- 添加注释并提交代码：
![](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/16cd8631279a6df0~tplv-t2oaga2asx-zoom-in-crop-mark:3024:0:0:0.awebp)

### 将代码推送到远程仓库

- 点击push按钮推送代码：
![](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/16cd8631294e78f6~tplv-t2oaga2asx-zoom-in-crop-mark:3024:0:0:0.awebp)

- 确认推送内容：

![](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/16cd86312d1f9c15~tplv-t2oaga2asx-zoom-in-crop-mark:3024:0:0:0.awebp)

- 查看远程仓库发现已经提交完成：
![](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/16cd86312ce7feef~tplv-t2oaga2asx-zoom-in-crop-mark:3024:0:0:0.awebp)


### 从远程仓库拉取代码

- 在远程仓库添加一个README-TEST.md文件：
![](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/16cd863151cbad86~tplv-t2oaga2asx-zoom-in-crop-mark:3024:0:0:0.awebp)


- 从远程仓库拉取代码：
![](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/16cd863151bc08f2~tplv-t2oaga2asx-zoom-in-crop-mark:3024:0:0:0.awebp)

- 确认拉取分支信息：
![](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/16cd86315395719d~tplv-t2oaga2asx-zoom-in-crop-mark:3024:0:0:0.awebp)


### 从本地创建分支并推送到远程

- 在本地创建dev分支，点击右下角的Git:master按钮：
![](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/16cd8631538b19d4~tplv-t2oaga2asx-zoom-in-crop-mark:3024:0:0:0.awebp)

- 使用push将本地dev分支推送到远程：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/20230714205906.png)


- 确认推送内容：
![](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/16cd863153b61fa3~tplv-t2oaga2asx-zoom-in-crop-mark:3024:0:0:0.awebp)


- 查看远程仓库发现已经创建了dev分支：
![](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/16cd8631559fb39a~tplv-t2oaga2asx-zoom-in-crop-mark:3024:0:0:0.awebp)

### 分支切换

- 从dev分支切换回master分支：
![](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/16cd86317cd522db~tplv-t2oaga2asx-zoom-in-crop-mark:3024:0:0:0.awebp)


### Git文件冲突问题解决

- 修改远程仓库代码：
![](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/16cd863182a863a3~tplv-t2oaga2asx-zoom-in-crop-mark:3024:0:0:0.awebp)

- 修改本地仓库代码：
![](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/16cd86317ccb7ed8~tplv-t2oaga2asx-zoom-in-crop-mark:3024:0:0:0.awebp)

- 提交本地仓库代码并拉取，发现代码产生冲突，点击Merge进行合并：
![](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/16cd863182de2fb1~tplv-t2oaga2asx-zoom-in-crop-mark:3024:0:0:0.awebp)

- 点击箭头将左右两侧代码合并到中间区域：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/20230714210409.png)

- 冲突合并完成后，点击Apply生效：
![](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/16cd8631909e9f8f~tplv-t2oaga2asx-zoom-in-crop-mark:3024:0:0:0.awebp)

- 提交代码并推送到远程。

### 从dev分支合并代码到master

- 在远程仓库修改dev分支代码：
![](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/16cd86319e72710b~tplv-t2oaga2asx-zoom-in-crop-mark:3024:0:0:0.awebp)

- 在本地仓库拉取代码，选择从dev分支拉取并进行合并：
![](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/16cd8631a92a1f96~tplv-t2oaga2asx-zoom-in-crop-mark:3024:0:0:0.awebp)

- 发现产生冲突，解决后提交并推送到远程仓库即可。
![](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/16cd8631ae0b4a3f~tplv-t2oaga2asx-zoom-in-crop-mark:3024:0:0:0.awebp)

### 查看Git仓库提交历史记录
![](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/16cd8631b0ef5f88~tplv-t2oaga2asx-zoom-in-crop-mark:3024:0:0:0.awebp)
  

作者：MacroZheng  
链接：https://juejin.cn/post/6844903926576185358  
来源：稀土掘金  
著作权归作者所有。商业转载请联系作者获得授权，非商业转载请注明出处。