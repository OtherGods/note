
最近在GitHub上fork了别人的一个项目，是关于数据科学学习的一个项目，Git地址如下：https://github.com/fengdu78/Data-Science-Notes.git（黄海广老师整理的数据科学笔记），但是如果黄海广老师在这个项目上改动了，比如加了一些知识点，我fork到我自己GitHub上的项目如何保持同步的更新呢？
其实只需要下面三步：

- 把fork的项目克隆到本地仓库中
- Configuring a remote for a fork
- Syncing a fork

下面操练一遍：

1. **把fork的项目克隆到本地仓库中**

```shell
git clone
```

首先，在本地GitHub库中开启一个shell，我本地的GitHub库放到了E盘
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202401222231149.png)


然后输入命令
```shell
git clone "https://github.com/zhongqiangwu960812/Data-Science-Notes.git"
```

后面这个地址是我fork项目在我GitHub的地址。这样完成之后，就会发现我的本地仓库中多了一个Data-Science-Notes的文件夹。说明你已经把仓库克隆到本地了。
PS: 如果不会使用GitHub怎么办？
见我笔记：快速上手GitHub大全

2. **Configuring a remote for a fork**

- 给 fork 配置一个 remote
- 主要使用 git remote -v查看远程状态。

克隆了fork项目到本地之后，进入那个项目文件夹，我就进入Data-Science-Notes的文件夹，然后输入命令

```shell
git remote -v
```

查看远程状态如下：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202401222232015.png)

- 添加一个将被同步给 fork 远程的上游仓库
```shell
git remote add upstream https://github.com/fengdu78/Data-Science-Notes.git
```

这里的地址是你fork的项目的源地址。

- 再次查看状态确认是否配置成功。
```shell
git remote -v
```

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202401222233851.png)

有了下面那两个upstream，说明第二步已经成功了。这就相当于有一个管道在源项目和你fork的项目之间建立了，下面看看如何通信更新。

3. **Syncing a fork**
- 从上游仓库 fetch 分支和提交点，传送到本地，并会被存储在一个本地分支 upstream/master
```shell
git fetch upstream
```

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202401222234112.png)

- 切换到本地主分支
```shell
git checkout master
# Switched to branch 'master'
```

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202401222235759.png)

- 把 upstream/master 分支合并到本地 master 上，这样就完成了同步，并且不会丢掉本地修改的内容。
```shell
git merge upstream/master
```

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202401222236634.png)

由于我是刚刚fork的，肯定是最新的项目了，所以提示Already up to date. 如果源项目有更新而你fork的项目没更新的话，这里就会显示不同了。
这样，就把源项目同步更新到你的本地仓库中了。 如果再想更新到远程仓库fork，只需要：

```shell
git push origin master
```


转载自：[https://blog.csdn.net/wuzhongqiang/article/details/103227170](https://blog.csdn.net/wuzhongqiang/article/details/103227170)

