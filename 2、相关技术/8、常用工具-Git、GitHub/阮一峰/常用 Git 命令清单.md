

我每天使用 Git ，但是很多命令记不住。

一般来说，日常使用只要记住下图6个命令，就可以了。但是熟练使用，恐怕要记住60～100个命令。

<font color = "red" size=5>四个区域</font>

![1.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202406161717649.png)


<font color = "red" size=5>四种状态</font>
在工作目录下的每一个文件都不外乎两种状态：已跟踪、未跟踪。
- 已跟踪——>指的是工作目录中那些已经被纳入了版本控制的文件，在上一次快照中有它们的记录，在工作一段时间后，它们的状态可能是未修改、已修改、已放入暂存区；简而言之，已跟踪的文件就是Git已经知道的文件。
- 未跟踪——>工作目录中除了已跟踪文件之外的其他所有文件都属于未跟踪文件，它们既不存在于上此快照的记录中，也没有被放入暂存区；初次克隆某个仓库的时候，工作目录中的所有文件都属于已跟踪文件，并处于未修改状态，因为Git刚刚检出了它们，而你尚未编辑他们
![1.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202505311606309.png)


下面是我整理的常用 Git 命令清单。几个专用名词的译名如下。

- Workspace：工作区
- Index / Stage：暂存区
- Repository：仓库区（或本地仓库）
- Remote：远程仓库

## 一、新建代码库

```bash

# 在当前目录新建一个Git代码库
$ git init

# 新建一个目录，将其初始化为Git代码库
$ git init [project-name]

# 下载一个项目和它的整个代码历史
$ git clone [url]
```

## 二、配置

Git的设置文件为`.gitconfig`，它可以在用户主目录下（全局配置），也可以在项目目录下（项目配置）。

```bash

# 显示当前的Git配置
$ git config --list

# 编辑Git配置文件
$ git config -e [--global]

# 设置提交代码时的用户信息
$ git config [--global] user.name "[name]"
$ git config [--global] user.email "[email address]"
```

## 三、增加/删除文件

```bash

# 添加指定文件到暂存区
$ git add [file1] [file2] ...

# 添加指定目录到暂存区，包括子目录
$ git add [dir]

# 添加当前目录的所有文件到暂存区
$ git add .

# 添加每个变化前，都会要求确认
# 对于同一个文件的多处变化，可以实现分次提交
$ git add -p

# 删除工作区文件，并且将这次删除放入暂存区
$ git rm [file1] [file2] ...

# 停止追踪指定文件，但该文件会保留在工作区
$ git rm --cached [file]

# 改名文件，并且将这个改名放入暂存区
$ git mv [file-original] [file-renamed]
```

## 四、代码提交

```shell

# 提交暂存区到仓库区
$ git commit -m [message]

# 提交暂存区的指定文件到仓库区
$ git commit [file1] [file2] ... -m [message]

# 提交工作区自上次commit之后的变化，跳过进入暂存区直接到仓库区
$ git commit -a

# 提交时显示所有diff信息
$ git commit -v

# 使用一次新的commit，替代上一次提交
# 如果代码没有任何新变化，则用来改写上一次commit的提交信息
$ git commit --amend -m [message]

# 重做上一次commit，并包括指定文件的新变化
$ git commit --amend [file1] [file2] ...
```


### 4.1、ChatGPT补充

`git commit --amend` 命令用于修改上一次提交。这个命令非常有用，当你需要更改最近一次提交的提交消息，或者在最近一次提交中添加或删除文件时，可以使用 `git commit --amend` 来避免创建新的提交。以下是 `git commit --amend` 的详细介绍及示例。

#### 4.1.1、基本用法

1. **修改上一次提交的提交消息**
2. **添加或删除文件并修改提交**

#### 4.1.2、示例

1. **修改上一次提交的提交消息**

假设你已经进行了一个提交，但发现提交消息有误，需要修改提交消息：

```sh
# 查看提交历史
git log --oneline

# 输出：
# e1a2b3c (HEAD -> master) Initial commit

# 修改提交消息
git commit --amend
```

此时会打开默认的编辑器（如 `vim` 或 `nano`），你可以编辑提交消息。编辑完成后保存并退出，新的提交消息会覆盖上一次提交消息。

2. **添加或删除文件并修改提交**

假设你忘记在上一次提交中添加某些文件，现在需要将这些文件包含到上一次提交中：

```sh
# 创建并编辑一个新的文件
echo "Some new content" > newfile.txt

# 添加文件到暂存区
git add newfile.txt

# 使用 --amend 将新的更改包含到上一次提交中
git commit --amend
```

这样，新文件 `newfile.txt` 会被添加到上一次提交中，同时你还可以修改提交消息。

3. 删除文件并修改提交

假设你在上一次提交中包含了不必要的文件，现在需要将其从提交中删除：

```sh
# 假设 newfile.txt 不应该包含在提交中
git rm --cached newfile.txt

# 使用 --amend 将删除的更改包含到上一次提交中
git commit --amend
```

这样，`newfile.txt` 会从上一次提交中删除。

#### 4.1.3、使用场景和注意事项

1. **修正错误**：
   - 如果提交中有拼写错误或遗漏了文件，可以用 `--amend` 进行修改。
   
2. **保持提交历史整洁**：
   - 使用 `--amend` 可以避免在历史记录中留下许多修正提交，使历史记录更清晰。

3. **注意事项**：
   - **不要在公共分支上使用 `--amend`**：如果你已经将提交推送到共享的远程仓库（如 GitHub 或 GitLab），最好不要使用 `--amend`，因为这会改变提交历史，可能会导致其他开发者的历史记录冲突。
   - **慎用**：`--amend` 修改的是 HEAD 指向的最近一次提交，因此在使用前要确保这正是你要修改的提交。


## 五、分支

```bash

# 列出所有本地分支
$ git branch

# 列出所有远程分支
$ git branch -r

# 列出所有本地分支和远程分支
$ git branch -a

# 新建一个分支，但依然停留在当前分支
$ git branch [branch-name]

# 新建一个分支，并切换到该分支
$ git checkout -b [branch]

# 新建一个分支，指向指定commit
$ git branch [branch] [commit]

# 新建一个分支，与指定的远程分支建立追踪关系
$ git branch --track [branch] [remote-branch]

# 切换到指定分支，并更新工作区
$ git checkout [branch-name]

# 切换到上一个分支
$ git checkout -

# 建立追踪关系，在现有分支与指定的远程分支之间
$ git branch --set-upstream [branch] [remote-branch]

# 合并指定分支到当前分支
$ git merge [branch]

# 选择一个commit，合并进当前分支
$ git cherry-pick [commit]

# 删除分支
$ git branch -d [branch-name]

# 删除远程分支
$ git push origin --delete [branch-name]
$ git branch -dr [remote/branch]
```

## 六、标签

```bash

# 列出所有tag
$ git tag

# 新建一个tag在当前commit
$ git tag [tag]

# 新建一个tag在指定commit
$ git tag [tag] [commit]

# 删除本地tag
$ git tag -d [tag]

# 删除远程tag
$ git push origin :refs/tags/[tagName]

# 查看tag信息
$ git show [tag]

# 提交指定tag
$ git push [remote] [tag]

# 提交所有tag
$ git push [remote] --tags

# 新建一个分支，指向某个tag
$ git checkout -b [branch] [tag]
```

## 七、查看信息

```bash

# 显示有变更的文件
$ git status

# 显示当前分支的版本历史，主要用于查看提交（commit）的详细信息。它包含提交哈希值、作者信息、提交日期和提交消息等
$ git log

# 显示commit历史，以及每次commit发生变更的文件
$ git log --stat

# 搜索提交历史，根据关键词
$ git log -S [keyword]

# 显示某个commit之后的所有变动，每个commit占据一行
$ git log [tag] HEAD --pretty=format:%s

# 显示某个commit之后的所有变动，其"提交说明"必须符合搜索条件
$ git log [tag] HEAD --grep feature

# 显示某个文件的版本历史，包括文件改名
$ git log --follow [file]
$ git whatchanged [file]

# 显示指定文件相关的每一次diff
$ git log -p [file]

# 显示过去5次提交
$ git log -5 --pretty --oneline

# 显示所有提交过的用户，按提交次数排序
$ git shortlog -sn

# 显示指定文件是什么人在什么时间修改过
$ git blame [file]

# 显示暂存区和工作区的差异
$ git diff

# 显示暂存区和上一个commit的差异
$ git diff --cached [file]

# 显示工作区与当前分支最新commit之间的差异
$ git diff HEAD

# 显示两次提交之间的差异
$ git diff [first-branch]...[second-branch]

# 显示今天你写了多少行代码
$ git diff --shortstat "@{0 day ago}"

# 显示某次提交的元数据和内容变化
$ git show [commit]

# 显示某次提交发生变化的文件
$ git show --name-only [commit]

# 显示某次提交时，某个文件的内容
$ git show [commit]:[filename]

# 显示对仓库的所有引用（reference）变动，包括提交（commit）、分支（branch）、合并（merge）、重置（reset）等操作。它包含所有 HEAD 的变动记录，可以用于找回丢失的提交。
$ git reflog
```

### 7.1、git log 和 git reflog 对比

`git reflog` 和 `git log` 是 Git 中两个非常有用的命令，它们提供了不同类型的历史记录信息。以下是详细解释和示例：

#### 7.1.1、`git log`

`git log` 命令显示提交历史记录，主要用于查看提交（commit）的详细信息。它包含提交哈希值、作者信息、提交日期和提交消息等。

1. **用法示例**

```sh
git log
```

2. **输出示例**

```bash
commit a3c6d52a5c1e9a1f4b5f7c1c2b4d9f8e5f7e3c1d
Author: John Doe <john.doe@example.com>
Date:   Fri Jun 14 22:15:13 2024 +0800

    Fix bug in search feature

commit f5b8c2a4b3e9a7d1c2f1a3b4c5d6e7f8g9h0i1j2
Author: Jane Smith <jane.smith@example.com>
Date:   Thu Jun 13 14:12:11 2024 +0800

    Add new feature for user profiles

commit d7e6f8a9c0b1a2d3e4f5g6h7i8j9k0l1m2n3o4p5
Author: John Doe <john.doe@example.com>
Date:   Wed Jun 12 10:05:09 2024 +0800

    Initial commit
```

#### 7.1.2、`git reflog`

`git reflog` 命令记录了对仓库的所有引用（reference）变动，包括提交（commit）、分支（branch）、合并（merge）、重置（reset）等操作。它包含所有 HEAD 的变动记录，可以用于找回丢失的提交。

1. **用法示例**

```sh
git reflog
```

2. **输出示例**

```bash
a3c6d52 HEAD@{0}: commit: Fix bug in search feature
f5b8c2a HEAD@{1}: checkout: moving from feature/new-feature to master
d7e6f8a HEAD@{2}: commit (initial): Initial commit
```

#### 7.1.3、区别

1. **显示内容**：
   - `git log` 主要显示提交历史，包括提交信息、作者和日期。
   - `git reflog` 显示 HEAD 的变动历史，包括所有对仓库引用的修改，不仅仅是提交，还包括检出（checkout）、重置（reset）等操作。

2. **用途**：
   - `git log` 用于查看提交历史，了解项目的演变。
   - `git reflog` 用于恢复丢失的提交和跟踪所有 HEAD 变动，帮助解决误操作导致的问题。

3. **持久性**：
   - `git log` 记录的提交历史是仓库的一部分，随着提交永久保留。
   - `git reflog` 是本地的引用日志，默认保留90天，并不会被推送到远程仓库。

#### 7.1.4、示例情境

假设你进行了以下操作：

1. 提交一个新功能 `git commit -m "Add new feature"`.
2. 检出 `master` 分支 `git checkout master`.
3. 重置到之前的提交 `git reset --hard HEAD~1`.

<font color = "red" size=5>git log 输出</font>

```sh
git log
```

```bash
commit d7e6f8a9c0b1a2d3e4f5g6h7i8j9k0l1m2n3o4p5
Author: John Doe <john.doe@example.com>
Date:   Wed Jun 12 10:05:09 2024 +0800

    Initial commit
```

由于重置，`git log` 中不再包含新功能的提交记录。


<font color = "red" size=5>git reflog 输出</font>

```sh
git reflog
```

```这个输出不对，少了一个切换到master分支的记录
d7e6f8a HEAD@{0}: reset: moving to HEAD~1
a3c6d52 HEAD@{1}: commit: Add new feature
d7e6f8a HEAD@{2}: commit (initial): Initial commit
```

`git reflog` 记录了重置前的所有 HEAD 变动，包括新功能的提交。你可以通过 `git reflog` 找到并恢复这个提交：

```sh
-- 这个命令会导致HEAD与现有分支分离
git checkout a3c6d52
```

或者重置到该提交：

```sh
git reset --hard a3c6d52
```

通过这些例子和解释，你可以看到 `git log` 和 `git reflog` 在 Git 历史管理中的不同用途和重要性。


## 八、远程同步

```bash

# 下载远程仓库的所有变动
$ git fetch [remote]

# 显示所有远程仓库
$ git remote -v

# 显示某个远程仓库的信息
$ git remote show [remote]

# 增加一个新的远程仓库，并命名
$ git remote add [shortname] [url]

# 取回远程仓库的变化，并与本地分支合并
$ git pull [remote] [branch]

# 上传本地指定分支到远程仓库
$ git push [remote] [branch]

# 强行推送当前分支到远程仓库，即使有冲突
$ git push [remote] --force

# 推送所有分支到远程仓库
$ git push [remote] --all
```


## 九、撤销

```bash

# 恢复暂存区的指定文件到工作区
$ git checkout [file]

# 恢复某个commit的指定文件到暂存区和工作区
$ git checkout [commit] [file]

# 恢复暂存区的所有文件到工作区
$ git checkout .

# 重置暂存区的指定文件，与上一次commit保持一致，但工作区不变
$ git reset [file]

# 重置暂存区与工作区，与上一次commit保持一致
$ git reset --hard

# 重置当前分支的指针为指定commit，同时重置暂存区，但工作区不变
$ git reset [commit]

# 重置当前分支的HEAD为指定commit，同时重置暂存区和工作区，与指定commit一致
$ git reset --hard [commit]

# 重置当前HEAD为指定commit，但保持暂存区和工作区不变
$ git reset --keep [commit]

# 新建一个commit，用来撤销指定commit
# 后者的所有变化都将被前者抵消，并且应用到当前分支
$ git revert [commit]

# 第四节的commit
git commit --amend

# 暂时将未提交的变化移除，稍后再移入
$ git stash
$ git stash pop
```

### 9.1、ChatGPT补充

在Git中，有多种方法可以退回到之前的状态，每种方法适用于不同的场景和需求。以下是常用的Git退回命令及其详细解释和使用场景。

#### 9.1.1、 `git checkout`

1. **用途**
	- 切换到另一个分支或特定的提交。
	- 将文件恢复到某个提交时的状态，也就是撤销工作区未提交的对某个文件的修改（我测试完后发现只能撤销未暂存的，也就是工作区中对文件的修改），类似于git restore。

2. **示例**
```sh
# 切换到另一个分支
git checkout develop

# 切换到特定的提交（分离头指针状态）
git checkout 9e0fe63

# 恢复某个文件到特定提交的状态
git checkout 9e0fe63 -- file1.txt

# 恢复某些文件到特定提交的状态
git checkout 9e0fe63 -- file1.txt file2.txt

# 撤销整个工作目录的修改
# 注意：这里的 `.` 表示当前目录及其所有子目录中的所有文件。
git checkout -- . 效果和 git checkout . 类似
```

3. **场景**
	- 想要查看或修改某个分支的内容。
	- 想要查看特定提交的内容。
	- 恢复文件到之前的状态以放弃当前的修改。

#### 9.1.2、 `git reset`

1. **用途**
	- 移动分支指针(HEAD)到指定的提交
	- 撤销提交或未提交的更改
		- 撤销指定提交（可以用hash或则相对引用）到  <font color = "red">暂存区</font> 或 <font color = "red">工作区</font>
		- 撤销未提交的更改到 <font color = "red">暂存区</font> 或 <font color = "red">工作区</font>

2. **类型**

	- **`--soft`**：重置 HEAD 到指定的提交，索引和工作目录不变。这意味着 =》 **指定提交后的提交 或 未提交的变更仍然保留在索引和工作目录中，可以进行新的提交**。
	- **`--mixed`**（默认）：重置 HEAD 到指定的提交，索引重置到该提交状态，但工作目录不变。这意味着 =》 **指定提交后的提交 或 未提交的变更 从索引中移除，但仍保留在工作目录中**。
	- **`--hard`**：重置 HEAD 到指定的提交，同时索引和工作目录都重置到该提交状态。这意味着 =》 **指定提交后的提交 或 未提交的变更 都会被丢弃**。

3. **示例**

<font color = "blue">重置指定提交的特定文件</font>

```bash
git reset HEAD file1.txt
```

<font color = "blue">重置HEAD，指定提交后的【提交（Commit C）或未提交的更改】 将保存在暂存区</font>

```sh
# reset --soft

# 查看当前的提交历史
git log --oneline

# 假设当前的提交历史如下：
# d1e2f3a (HEAD -> master) Commit C
# a1b2c3d Commit B
# e1f2g3h Commit A

# 使用 --soft 重置到 Commit B
git reset --soft a1b2c3d

# 现在 HEAD 指向 Commit B，但 Commit C 的更改仍然保留在索引和工作目录中
git status
```

<font color = "blue">重置HEAD、并重置暂存区到指定提交的状态，指定提交后的【提交（Commit C）或未提交的更改】 将保存在工作区</font>

```bash
# reset

# 查看当前的提交历史
git log --oneline

# 假设当前的提交历史如下：
# d1e2f3a (HEAD -> master) Commit C
# a1b2c3d Commit B
# e1f2g3h Commit A

# 使用 --mixed 重置到 Commit B
git reset --mixed a1b2c3d

# 现在 HEAD 指向 Commit B，索引重置到 Commit B，但工作目录保留了 Commit C 的更改
git status
```


<font color = "blue">重置HEAD、重置暂存区和工作区到指定提交的状态，指定提交后的【提交（Commit C）或未提交的更改】 将被丢弃</font>

```bash
#reset --hard

# 假设当前的提交历史如下：
# d1e2f3a (HEAD -> master) Commit C
# a1b2c3d Commit B
# e1f2g3h Commit A

# 使用 --hard 重置到 Commit B
git reset --hard a1b2c3d

# 现在 HEAD、索引和工作目录都重置到 Commit B 的状态，所有未提交的更改被丢弃
git status

```


4. **场景**
	- 想要撤销最近的一次或多次提交（软重置）。
	- 撤销提交并同时撤销暂存区的修改（混合重置）。
	- 完全恢复到某个提交状态，放弃所有更改（暂存区和工作区）（硬重置）。

#### 9.1.3、 `git revert`

1. **用途**
	- 创建一个新提交来撤销某个特定的提交。

2. **示例**

```sh
# 撤销特定的提交：git revert <commit-hash>
git revert 9e0fe63

# 撤销多个提交：git revert <commit-hash-1>..<commit-hash-2>
git revert HEAD~2..HEAD
```

3. **场景**
	- 想要撤销某个提交或某些提交，但需要保留历史记录，尤其是在已经推送到共享仓库的情况下。

4. 注意事项
	- **提交历史不可改变**：`git revert` 不会修改现有的提交历史。它会创建新的提交，以反转先前提交引入的更改。
	- **冲突解决**：如果撤销提交时出现冲突，需要手动解决冲突后再提交撤销的结果。
	- **撤销合并提交**：对于合并提交，`git revert` 只会撤销合并引入的更改，而不是撤销整个合并操作。

#### 9.1.4、 `git clean`

1. **用途**
	- 删除未跟踪的文件和目录。

2. **示例**

```sh
# 删除未跟踪的文件
git clean -f

# 删除未跟踪的文件和目录
git clean -fd

# 进行删除操作之前进行预览
git clean -n
```

3. **场景**
	- 清理工作目录中的未跟踪文件和目录，通常在需要一个干净的工作目录时使用。

#### 9.1.5、 `git restore`

1. **用途**
	- 撤销  **工作区**  或  **暂存区**  未提交的对某个文件的修改，类似于git chekout撤销文件的语法

2. **示例**

```sh
# 撤销工作区单个文件的修改
git restore file1.txt

# 撤销工作区多个文件的修改
git restore file1.txt file2.txt

# 撤销工作区中当前工作目录的修改
git restore .

# 恢复暂存区中的文件到最后一次提交的状态
git restore --source=HEAD file1.txt
```

3. **场景**
	- 放弃对文件的修改，并恢复到暂存区或最后一次提交的状态。

4. **总结**
`git checkout` 和 `git restore` 都可以用于撤销对工作目录中文件的修改。虽然 `git checkout` 依旧可用，但 `git restore` 提供了更清晰的语法，建议在新项目中使用。无论使用哪种方式，撤销操作都会将文件恢复到指定的提交状态，从而丢弃未提交的更改。

#### 9.1.6、 `git reflog`

1. **用途**
	- 查看HEAD和分支的移动历史，用于恢复丢失的提交和跟踪所有 HEAD 变动，帮助解决误操作导致的问题。

2. **示例**

```sh
# 查看HEAD的变更记录
git reflog

# 使用reflog记录重置分支
git reset --hard HEAD@{2}
```

3. 场景
	- 恢复由于错误操作导致丢失的提交或分支。

#### 9.1.7、总结

- **`git checkout`**：用于切换分支、查看特定提交、恢复文件。
- **`git reset`**：用于撤销提交并重置HEAD指针、暂存区和工作目录。
- **`git revert`**：用于通过新提交来撤销特定提交，适用于已推送到远程仓库的情况。
- **`git clean`**：用于删除未跟踪的文件和目录。
- **`git restore`**：用于恢复文件到暂存区或提交的状态。
- **`git reflog`**：用于查看HEAD和分支的历史变动，并可以恢复丢失的提交或分支。

通过选择合适的命令，可以在不同场景下安全有效地管理代码版本和历史。


## 十、其他

```bash

# 生成一个可供发布的压缩包
$ git archive
```

（完）



转载自：[https://www.ruanyifeng.com/blog/2015/12/git-cheat-sheet.html](https://www.ruanyifeng.com/blog/2015/12/git-cheat-sheet.html)

