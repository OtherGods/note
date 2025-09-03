# 1、简介

## 1、配置公私钥

可以将自己电脑上的git和github或者gitlab连接起来**<font color = "red">（我记得应该是在推送本地仓库中的某个分支到远程仓库对应分支的时候不需要再输入密码）</font>**

1. 在cmd中输入该命令生成公私钥：$ ssh-keygen -t rsa -C "自己的邮箱号码"；**<font color = "red">在生成公私钥的时候无脑回车就可以</font>**

2. 在cmd中会显示生成的公私钥的在本地的保存路径，将本地生成的公钥的内容添加到GitHub、Gitlab等中

   1. 以Github为例：登录 github 账号，选中并打开 setting，选择 SSH and GPG keys，选择 New SSH key，在 Title 中填入题目，在 Key 中填入id_rsa.pub 文件中的公钥。

      可用如下命令验证上述配置是否成功：

      ```less
      ssh -T git@github.com
      ```



## 2、配置本地git

全局配置，具体详情可以去git官网中看：https://git-scm.com/book/zh/v2/%E8%B5%B7%E6%AD%A5-%E5%88%9D%E6%AC%A1%E8%BF%90%E8%A1%8C-Git-%E5%89%8D%E7%9A%84%E9%85%8D%E7%BD%AE

```
# 配置用户名 
git config --global user.name "xxx"                       
# 配置邮件
git config --global user.email "xxx@xxx.com"
```



## 3、本地仓库

新建一个文件夹，用cmd打开该文件夹，git init，该命令执行完后会在当前目录生成一个 .git 目录



## 4、添加远程仓库（将本地仓库与远程仓库连接起来）

```
git remote add origin git@github.com/你的github用户名/仓库名.git
```

**<font color = "red">去git官网详细看一下相关内容，我记得在使用remote的时候还是可以指定分支，什么的</font>**

## 5、查看信息

1. git status：显示有变更的文件
2. git log：显示当前分支的版本历史
3. git log --stat：显示commit历史，以及每次commit发生变更的文件
4. git log -S [keyword]：搜索提交历史，根据关键词
5. git log --follow [file]：显示某个文件的版本历史，包括文件改名
6. git whatchanged [file]：显示某个文件的版本历史，包括文件改名
7. git diff 文件名：可以看到文件到底改动了什么内容

## 6、远程同步

1. git remote -v：显示所有远程仓库
2. git remote show [remote]：显示某个远程仓库的信息
3. git remote add [shortname] [url]：增加一个新的远程仓库，并命名
4. git pull [remote] [branch]：取回远程仓库的变化，并与本地分支合并
5. git push [remote] [branch]：上传本地指定分支到远程仓库
6. git push [remote] --all：推送所有分支到远程仓库

## 7、本地、远程分支

1. 本地分支

   1. 查看本地分支

      1. git branch
      2. git branch -a：列出所有本地分支和远程分支

   2. 增加本地分支

      1. git branch [branch-name]：新建一个分支，但依然停留在当前分支
      2. git branch --track [branch] [remote-branch]：新建一个分支，与指定的远程分支建立追踪关系
      3. git checkout -b [branch]：新建一个分支，并切换到该分支
      4. 

   3. 删除本地分支

      1. git branch -d [branch-name]：删除分支

   4. 切换分支

      1. git checkout [branch-name]：切换到指定分支，并更新工作区


         注意：要留意你的工作目录和暂存区里那些还没有被提交的修改，它可能会和你即将检出的分支产生冲突从而阻止 Git 切换到该分支。最好的方法是，在你切换分支之前，保持好一个干净的状态。有一些方 法可以绕过这个问题（即，保存进度（stashing） 和 修补提交（commit amending））

   5. 合并本地分支

      1. git merge [branch]：合并指定分支到当前分支，这个操作和git pull…… 都会产生冲突（在两个不同的分支中对相同的文件中相同的内容做了改动）

   6. 本地分支追踪远程分支

      1. git branch --set-upstream [branch] [remote-branch]：建立追踪关系，在现有分支与指定的远程分支之间
      2. git branch --track [branch] [remote-branch]：新建一个分支，与指定的远程分支建立追踪关系
      3. 

2. 远程分支

   1. 查看远程分支
      1. git branch -r：列出所有远程分支
      2. git branch -a：列出所有本地分支和远程分支
   2. 增加远程分支
      1. git push --set-upstream origin + 刚刚创建的分支名
   3. 删除远程分支
      1. git push origin --delete [branch-name]：删除远程分支
      2. git branch -dr [remote/branch]：删除远程分支

## 8、增加、删除文件

1. git add [file1] [file2] ...：添加指定文件到暂存区
2. git add [dir]：添加指定目录到暂存区，包括子目录
3. git add .：添加当前目录的所有文件到暂存区
4. git rm [file1] [file2] ...：删除工作区文件，并且将这次删除放入暂存区

## 9、撤销

1. git checkout [file]：恢复暂存区的指定文件到工作区

2. git checkout .：恢复暂存区的所有文件到工作区

3. git checkout [commit] [file]：恢复某个commit的指定文件到暂存区和工作区

4. git reset [file]：重置暂存区的指定文件，与上一次commit保持一致，但工作区不变

5. git reset --hard：重置暂存区与工作区，与上一次commit保持一致

6. 退回到某个版本：

   1. git reset --hard HEAD^：将当前本版回退到上一个版本；
   2. git reset --hard HEAD^^：如果想要将当前版本会推导上上个版本
   3. git reset --hard HEAD~100：如果要回退到前100个版本可以使用
   4. git reset --hard 版本号：退回到指定版本号

7. 暂时将未提交的变化移除，稍后再移入

   $ git stash

   $ git stash pop



# 2、总结的使用git流程

第一种：

1. 在本地创建一个文件夹用于存放从git上拉取下来的项目
2. 进入该目录，执行git init
3. git remote add添加远程仓库
4. git fetch 远程仓库名字简写，拉取远程仓库中所有内容到本地
5. git checkout 本地分支名（和远程仓库中的分支的名字一样），在本地创建本地分支，同时切换到这个分支上，同时将这个本地分支设置为跟踪远程仓库中同名的分支
6. git add .、git commit -m "xxxxxx"
7. git pull 远程仓库名 远程分支:本地分支
8. git push 远程仓库名 本地分支:远程分支

第二种：

1. 先在远程仓库（github或gitlab）上创建一个新的分支从master，假设分支名为2023-3-27
2. 在本地git clone xxx项目
3. git checkout 2023-3-27（本地切换到2023-3-27分支上，并且追踪远程2023-3-27分支）
4. git add .、git commit -m "xxxxxx"
5. git pull 远程仓库名 远程分支:本地分支
6. git push 远程仓库名 本地分支:远程分支



# 3、GitHub的token

PrivateToken——>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






