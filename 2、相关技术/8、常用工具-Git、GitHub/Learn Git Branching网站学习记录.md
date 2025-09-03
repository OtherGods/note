

# 1、主要

## 1.1、这个网站的几个简单命令：
	1. levels：展示出题目列表
	2. show goal / objective：展示出提示框
	3. show solution：展示正确的命令


## 1.2、基础篇

1. git log：可以查看提交记录；
2. git reflog：查看本地操作记录，可以看到本地的commit、merge、rebase等操作记录，并且带有版本号；


### 1.2.1、commit

Git 仓库中的提交记录保存的是你的目录下所有文件的快照，就像是把整个目录复制，然后再粘贴一样，但比复制粘贴优雅许多！

Git 希望提交记录尽可能地轻量，因此在你每次进行提交时，它并不会盲目地复制整个目录。条件允许的情况下，它会将当前版本与仓库中的上一个版本进行对比，并把所有的差异打包到一起作为一个提交记录。

Git 还保存了提交的历史记录。这也是为什么大多数提交记录的上面都有 parent 节点的原因 —— 我们会在图示中用箭头来表示这种关系。对于项目组的成员来说，维护提交历史对大家都有好处。

**示例**：当前有两个提交记录 —— 初始提交 `C0` 和其后可能包含某些有用修改的提交 `C1`。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/20230710225912.png)执行git commit -m "xxx"之后
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/20230710230038.png)
我们在修改了代码库，并commit命令后，会保存成一个提交记录 `C2`。`C2` 的 parent 节点是 `C1`， parent 节点是当前提交中变更的基础。

### 1.2.2、branch

Git 的分支也非常轻量。它们只是简单地指向某个提交记录 —— 仅此而已。也就是分支指向commit小节图中c1、c2提交记录。

**示例**：新建一个newImage分支
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/20230710230831.png)执行git branch newImage后
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/20230710230911.png)

### 1.2.3、checkout

【git checkout 目标分支名】切换分支就是让HEAD指向另一个分支（在切换分支时使用 哈希值 或 相对引用 就不是切换分支了，具体可以看第三章中在checkout的时候使用哈希值或相对引用让HEAD在 **<font color="red" size = 5>提交树(由提交记录组成的树)</font>** 上移动）；

**示例**：在 [1.2.2、branch](2、相关技术/8、常用工具-Git、GitHub/Learn%20Git%20Branching网站学习记录.md#1.2.2、branch) 的新建了newImage分支的基础上切换到newImage分支，之后提交
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/20230710231241.png)执行git checkout newImage; git commit -m "xxx"后
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/20230710231338.png)

可以在创建分支的时候就直接切换分支：git checkout -b newImage

#### 1.2.3.1、切换分支注意

在Git中切换分支是一项常见的操作，但为了确保工作不会丢失或产生冲突，有几个需要注意的事项。以下是切换分支时的注意事项和详细解释。

1. 确保工作目录干净

**解释**
在切换分支之前，确保你的工作目录没有未提交的修改。未提交的修改会跟随你切换到新的分支，这可能会导致冲突或丢失工作。

**检查方法**
使用 `git status` 查看当前工作目录的状态。

```sh
git status
```

**示例**

如果你有未提交的修改，`git status` 会显示类似以下内容：

```sh
On branch master
Changes not staged for commit:
  (use "git add <file>..." to update what will be committed)
  (use "git restore <file>..." to discard changes in working directory)
        modified:   file1.txt

no changes added to commit (use "git add" and/or "git commit -a")
```

2. 提交或暂存当前的修改

**解释**
如果有未提交的修改，可以选择提交这些修改，或者将它们暂存（stashing）起来，以便稍后恢复。

**提交修改**

```sh
git add .
git commit -m "Save changes before switching branches"
```

**暂存修改**

```sh
git stash
```

在完成分支切换后，可以恢复暂存的修改：

```sh
git stash pop
```

3. 处理未跟踪的文件

**解释**
未跟踪的文件不会自动跟随你切换分支。确保未跟踪的文件已经被处理（添加到暂存区或删除），以防在切换分支后丢失。

**检查方法**
`git status` 也会显示未跟踪的文件。

4. 合并或放弃当前分支的工作

**解释**
如果当前分支的工作已经完成，合并它到目标分支；否则，决定是否放弃这些修改。

**示例**
切换到目标分支：

```sh
git checkout target-branch
```

合并当前分支的工作：

```sh
git merge current-branch
```

5. 处理冲突

**解释**
在切换分支或合并时，可能会遇到冲突。解决冲突后再提交解决方案。

**示例**
如果切换分支时遇到冲突，Git 会提示你解决冲突并提交。

6. 检查新分支状态

**解释**
切换分支后，使用 `git status` 确认工作目录的状态，确保一切正常。

**示例总结**

假设你正在开发一个新功能，正在 `feature-branch` 上工作，需要切换回 `master` 分支进行其他工作：

1. **检查工作目录状态**：

    ```sh
    git status
    ```

2. **提交或暂存修改**：

    ```sh
    git add .
    git commit -m "WIP: working on new feature"
    ```

    或者

    ```sh
    git stash
    ```

3. **切换分支**：

    ```sh
    git checkout master
    ```

4. **检查新分支状态**：

    ```sh
    git status
    ```

5. **如果之前使用了 `git stash`，在需要时恢复修改**：

    ```sh
    git stash pop
    ```

通过遵循这些步骤，可以确保在切换分支时工作不会丢失，减少冲突并保持工作流程的顺畅。


### 1.2.4、分支与合并
#分支合并
分支合并就是：新建一个分支，在其上开发某个新功能，开发完成后再合并回主线。
分支合并的两种方式：
	1. **git merge 目标分支名** ：**<font color = "red">将目标分支指向的提交记录合并入当前分支指向的提交记录中，合并后会创建一个新的提交记录并由当前分支指向它，当前分支包含两个分支的提交内容，而目标分支指向的提交记录内容无变化；</font>**
	   合并两个分支时会产生一个特殊的提交记录，它有两个 parent 节点。翻译成自然语言相当于：“我要把这两个 parent 节点本身及它们所有的祖先都包含进来。”
	2. **git rebase 目标分支名**：**<font color = "red">将当前分支指向的提交记录对应的动作在目标分支上再做一次，重做后会创建一个新的提交记录，并由当前分支指向它，当前分支包含两个分支的提交内容，而目标分支指向的提交记录内容无变化；</font>**
	   合并两个分支时会产生一个特殊的提交记录，它只有一个 parent 节点，就是目标分支指向的提交记录。
	   【git rebase 目标分支】 <<<===>>> 【git rebase 当前分支 目标分支】
	   
	这两个命令同样可以使用【git merge 相对引用】/【git merge 哈希值】来合并。

git rebase和git merge的异同：
1. 相同：两条指令在执行完之后，当前分支都会指向下一个新创建的提交记录
2. 不同：
	1. 【git rebase 目标分支/哈希值/相对引用】是把当前分支的提交记录在目标分支上重新提交一次，并且当前分支指向新的提交记录上
	2. 【git merge 目标分支/哈希值/相对引用】以当前分支和目标分值为基础新建一个提交记录，并且由当前分支指向这个提交记录

git rebase的优缺点：
1. 优点：Rebase 使你的 **<font color="red" size = 5>提交树(由提交记录组成的树)</font>** 变得很干净, 所有的提交都在一条线上
2. 缺点：Rebase 修改了 **<font color="red" size = 5>提交树(由提交记录组成的树)</font>** 的历史
   比如, 提交 C1 可以被 rebase 到 C3 之后。这看起来 C1 中的工作是在 C3 之后进行的，但实际上是在 C3 之前。
一些开发人员喜欢保留提交历史，因此更偏爱 merge。而其他人（比如我自己）可能更喜欢干净的 **<font color="red" size = 5>提交树(由提交记录组成的树)</font>** ，于是偏爱 rebase。

**示例1**：使用merge命令，将 `bugFix` 合并到 `main` 里
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/20230710233401.png)执行git merge bugFix后
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/20230710233426.png)

`main` 分支现在指向了一个拥有两个 parent 节点的提交记录，bugFix分支指向的提交记录无变化。
假如从 `main` 开始沿着箭头向上看，在到达起点的路上会经过所有的提交记录。这意味着 `main` 包含了对代码库的所有修改。

**示例2**：使用rebase命令，将bugFix分支里的工作直接移动到main分支上，移动后会使两个分支的功能看似是按顺序开发的，但是实际上它们是并行开发的
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/20230711001122.png)执行git rebase main后
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/20230711001209.png)
在执行rebase命令之后bugFix分支之前指向的c3提交记录不会消失，只是没有分支指向它了。
将当前分支切换到main分支上，再次执行git rebase bugFix后并不会再次创建一个提交记录，而是main分支和bugFix分支指向同一个提交记录：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/20230711001701.png)


### 1.2.5、git stash(补充)

`git stash` 命令用于将当前工作目录中的未提交更改保存到一个栈中，以便您可以切换分支或进行其他操作，而不必提交这些更改。以下是一些常用的 `git stash` 命令及其用途：

1. **保存当前工作目录和暂存区的修改：**

```sh
git stash
```

   这会将当前工作目录和暂存区的修改保存到栈中，并恢复工作目录到最后一次提交的状态。

2. **保存当前工作目录和暂存区的修改，并附加说明：**

```sh
git stash save "your message"
```

   您可以附加一条消息来描述这次储藏的内容。

3. **仅保存未暂存的修改：**

```sh
git stash -k
```

   这会将未暂存的修改保存到栈中，并保留已暂存的更改。

4. **仅保存暂存区的修改：**

```sh
git stash --staged
```

   这会将暂存区的修改保存到栈中，并保留未暂存的更改。

5. **查看已保存的存储：**

```sh
git stash list
```

   这会列出所有已保存的存储。

6. **应用最近一次的存储：**

```sh
git stash apply
```

   这会将最近一次存储的更改应用到当前工作目录，但不删除存储。

7. **应用特定的存储：**

```sh
git stash apply stash@{n}
   ```

   这会将特定编号的存储应用到当前工作目录，其中 `n` 是存储的编号。

8. **应用最近一次的存储并删除：**

```sh
git stash pop
```

   这会将最近一次存储的更改应用到当前工作目录，并从栈中删除该存储。

9. **删除特定的存储：**

```sh
git stash drop stash@{n}
```

   这会删除特定编号的存储。

10. **清除所有存储：**

```sh
git stash clear
```

    这会清除所有存储。

通过这些命令，您可以灵活地管理和应用未提交的更改。

## 1.3、高级篇

===**哈希值**：每一个提交记录都对应一个哈希值，使用git log命令可以看到提交记录，其中包含每次提交的哈希值；
**相对引用**：四种形式——>分支名^、`分支名~num`、`分支名~`（等价于分支名^）、HEAD^、`HEAD~num`、`HEAD~`（等价于HEAD^）===

### 1.3.1、HEAD简介
#HEAD
HEAD 默认是间接指向当前分支上最近一次提交记录，大多数修改 **<font color="red" size = 5>提交树(由提交记录组成的树)</font>** 的Git命令都是从改变 HEAD 的指向开始的。

HEAD是一个对当前所在分支的符号引用（也就是HEAD指向分支），HEAD通常情况下是指向分支的（代表我们当前使用的分支），但是可以通过命令将HEAD由指向分支改变为指向提交记录，表现出来的结果就是HEAD在 **<font color="red" size = 5>提交树(由提交记录组成的树)</font>** 上移动（不但能向前移动，也能向后移动）。

如果想要看HEAD的指向，可以去看.git/HEAD文件中的内容。

#### 1.3.1.1、来自ChatGPT解释
在Git中，`HEAD` 是一个非常重要的概念，它是指向当前正在检出的分支、提交或特定位置的一个指针。理解`HEAD`有助于更好地理解Git的版本控制操作。以下是关于`HEAD`的详细解释及其作用：

##### 1. `HEAD` 的基本概念
`HEAD` 是一个引用，通常指向当前分支的最新提交。换句话说，`HEAD` 指向你当前工作目录所基于的提交。它可以指向以下几种情况：

- **分支名**：例如，`HEAD` 可以指向 `master` 分支，这意味着当前检出的分支是 `master`，并且 `HEAD` 指向 `master` 分支的最新提交，`HEAD` 指向 `master` 分支的直接体现就是 `master` 使用 `git branch -a` 后展示出来 `master` 分支是 `master*`。
- **具体的提交**：在某些情况下，例如你在一个特定的提交上进行操作而不是在分支上（即“分离头指针”状态），`HEAD` 直接指向一个具体的提交对象。

##### 2. 作用
`HEAD` 的主要作用包括以下几个方面：

1. **跟踪当前分支**
`HEAD` 是当前分支的指示器。每次你在当前分支上进行提交时，`HEAD` 会自动更新为最新的提交对象。例如：

```sh
git checkout master
```
在这之后，`HEAD` 会指向 `master` 分支，并且 `master` 分支指向最新的提交。

2. **执行版本控制操作**
在执行许多Git操作时，`HEAD` 是默认的参考点。例如，以下命令会以 `HEAD` 为基础：

- `git commit`：创建一个新的提交，并将当前分支的 `HEAD` 更新为这个新的提交。
- `git reset`：将 `HEAD` 和当前分支的指针重置到指定的提交。
- `git rebase`：在变基操作过程中，`HEAD` 会指向新的基提交。

3. **创建和管理分支**
`HEAD` 可以用于创建新的分支。例如：

```sh
git checkout -b new-branch
```
这将创建一个名为 `new-branch` 的新分支，并将 `HEAD` 切换到这个新分支上。

##### 3. 分离头指针状态（Detached HEAD）
分离头指针状态是指 `HEAD` 直接指向一个具体的提交，而不是分支。例如：

```sh
git checkout <commit-hash>
```
在这种状态下，`HEAD` 指向一个特定的提交对象（`<commit-hash>`），而不是分支。这意味着你在这个状态下所做的任何新提交不会与任何分支关联，而是孤立的。

如果你需要将这些孤立的提交保存到某个分支，可以创建一个新分支并将 `HEAD` 指向它：

```sh
git checkout -b new-branch
```

##### 4. `HEAD` 的实际表示
在Git仓库中，`HEAD` 实际上是一个包含当前引用信息的文件。你可以查看这个文件的内容来了解 `HEAD` 当前指向什么：

```sh
cat .git/HEAD
```
如果 `HEAD` 指向一个分支，你会看到类似于以下的内容：

```
ref: refs/heads/master
```
如果 `HEAD` 处于分离状态，你会看到具体的提交哈希值。

##### 总结
`HEAD` 是Git中用于跟踪当前检出分支或特定提交的指针。它在管理分支、提交以及执行各种版本控制操作时起着核心作用。理解 `HEAD` 及其行为有助于有效地使用Git进行版本控制。


### 1.3.2、HEAD在提交树上移动
#HEAD移动
分离的 HEAD 就是让其指向了某个具体的提交记录而不是分支名；分离后的HEAD可以指向分支或者 **<font color="red" size = 5>提交树(由提交记录组成的树)</font>** 中的提交记录。

分离HEAD的命令：
	1. **<font color = "red">git checkout 哈希值</font>**：前面几张图中圆圈中的C0、C1、C2等都是代表的哈希值；
	2. **<font color = "red">git checkout 相对引用</font>**：相对引用【main^】相当于main分支指向的提交记录的parent提交；
	   通过指定提交记录的哈希值的方式让HEAD在 **<font color="red" size = 5>提交树(由提交记录组成的树)</font>** 上移动不太方便。在Git中引入了相对引用（相对引用只能在 **<font color="red" size = 5>提交树(由提交记录组成的树)</font>** 中向上移动），相对引用的形式；
		1. **【git checkout 目标分支名】 / 【git checkout HEAD】**：将HEAD指向目标分支
		2. **【git checkout 目标分支名^】 / 【git checkout HEAD^】**：在 **<font color="red" size = 5>提交树(由提交记录组成的树)</font>** 上向上移动一个提交记录，将HEAD指向目标分支指向提交记录的parent提交；
		3. **【git checkout 目标分支名~number】 / 【git checkout HEAD~number】**：在 **<font color="red" size = 5>提交树(由提交记录组成的树)</font>** 上向上移动num个提交记录，将HEAD指向目标分支指向提交记录的parent提交；


**示例1**：使用哈希值来分离HEAD和分支，下图中main右上角的星代表HEAD指向main分支
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/20230711002824.png)
执行命令：
	1. git checkout C1：将HEAD由指向main分支改变为指向C1提交记录
	2. git checkout main：将HEAD由指向C1提交记录改变为指向main分支
	3. git commit -m "xxx"：在当前分支（HEAD指向的main分支）上执行一次提交
	4. git checkout C2：将HEAD由指向main分支改变为指向C2提交记录
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/20230711003530.png)


**示例2**：使用相对引用来分离HEAD和分支，
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/20230711100740.png)执行git chekcout main^后
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/20230711101303.png)



### 1.3.3、分支在提交树上移动
#分支移动
在3.1小节中使用【git checkout 哈希值】 / 【git checkout 相对引用】只是移动HEAD，如果想要移动分支，要使用【git branch -f 目标分支 相对引用】，这个命令会将目标分支强制指向相对引用指向的提交记录（只能在 **<font color="red" size = 5>提交树(由提交记录组成的树)</font>** 上向上移动），并且不会影响HEAD。

**示例**：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/20230711101949.png)使用git branch -f main HEAD~3后
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/20230711102229.png)
在移动main分支之后，HEAD指向的分支不会变化



### 1.3.4、撤销变更
#撤销
两个命令：
1. 【git reset 哈希值】 / 【git reset 相对引用】：将当前分支指向【哈希值】/【相对阴影】对应的提交记录；
   这个命令会**重置暂存区**，工作区的内容不变（关于Git中的四个区可以去看印象笔记中的内容），相当于撤销【哈希值】/【相对引用】对应的提交记录，撤销到执行git add之前，之后可以使用【git checkout -- 文件名】放弃工作区的修改。
   >    上面这种先执行【git reset xxx】再执行【git chekout -- 文件名】的方式等价于=====>>>【git reset --hard 哈希值】/【git reset --hard 相对引用】的方式**重置暂存区与工作区**。
   
   执行完上面两种方式之后，如果再执行git push会显示：
    ```git
    ! [rejected]        master -> master (non-fast-forward)
    error: failed to push some refs to xxx
    hint: Updates were rejected because the tip of your current branch is behind
    hint: its remote counterpart. Merge the remote changes (e.g. 'git pull')
    hint: before pushing again.
    hint: See the 'Note about fast-forwards' in 'git push --help' for details.
    ```
   
   这是由于本地分支和远程分支之间存在冲突导致的，具体解释如下：
   
   - 当你执行 `git reset --hard 123456` 时，你的本地 `master` 分支被重置到指定的提交 `123456`，可能导致本地分支历史记录与远程分支历史记录不一致。
   - 提示信息表明，当前本地分支的提交记录落后于远程分支。这意味着远程分支有一些本地分支没有的提交。
   - Git 默认情况下不允许推送这样的更改，因为这会导致远程分支丢失那些本地分支没有的提交记录。为了保护数据完整性，Git 阻止了这种非快进推送。

   **解决方法**：
   1. 执行 `git pull` 将远程更改拉取到本地，并处理可能的合并冲突。
   2. 如果你确定不需要保留远程分支上的更改，可以使用 `--force` 选项强制推送。
      **注意**：这会覆盖远程分支的历史记录，可能导致其他开发者的工作丢失。
   ===！！！注意：这个reset回滚命令只会回滚本地的 **<font color="red" size = 5>提交树(由提交记录组成的树)</font>** 上的提交记录，并不会回滚远程 **<font color="red" size = 5>提交树(由提交记录组成的树)</font>** 上的提交记录，即使使用git push -f之后也不能让远程提交书上的提交记录一起回滚；为了能将撤销更改分享给被人需要使用git revert。===
2. 【git revert 哈希值】 / 【git revert 相对引用】：新建一个提交记录，用来撤销【哈希值】/【相对引用】对应的提交记录。
   
   


**示例1**：使用git reset HEAD~1回退
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/20230711104140.png)执行git reset HEAD~1后
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/20230711104215.png)
执行完之后main分支移回到C1，C2所做的变更还在，但是处于未加入暂存区的状态，工作区有C2的变更

**示例2**：使用git revert HEAD回退
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/20230711141451.png)执行git revert HEAD后
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/20230711141543.png)
在撤销的提交记录后面居然多了一个新提交！这是因为新提交记录 `C2'` 引入了**更改** —— 这些更改刚好是用来撤销 `C2` 这个提交的。也就是说 `C2'` 的状态与 `C1` 是相同的。
revert 之后就可以把你的更改推送到远程仓库与别人分享啦。



## 1.4、移动提交记录

### 1.4.1、Git Cherry-pick
#移动提交记录
前两章的概念涵盖了 Git 90% 的功能，同样也足够满足开发者的日常需求。
然而, 剩余的 10% 在处理复杂的工作流时(或者当你陷入困惑时）可能就显得尤为重要了。接下来要讨论的这个话题是“整理提交记录” —— 开发人员有时会说“我想要把这个提交放到这里, 那个提交放到刚才那个提交的后面”, 而接下来就讲的就是它的实现方式，非常清晰、灵活，还很生动。
看起来挺复杂, 其实是个很简单的概念。

使用【git cherry-pick 一个或多个哈希值】/【git cherry-pick 一个或多个相对引用】，这个命令的作用是：将一些提交复制到当前所在的位置（`HEAD`）下

**示例**：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/20230711144915.png)执行git cherry-pick c2 c4
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/20230711144958.png)

### <font color = "green">1.4.2、交互式rebase</font>

当你知道你所需要的提交记录（**并且**还知道这些提交记录的哈希值）时, 用 cherry-pick 再好不过了 —— 没有比这更简单的方式了；
但是如果你不清楚你想要的提交记录的哈希值呢? 幸好 Git 帮你想到了这一点, 我们可以利用交互式的 rebase —— 如果你想从一系列的提交记录中找到想要的记录, 这就是最好的方法了。

git rebase -i 相对引用  ====>>>  弹出一个UI，让我们能调整提交记录的顺序【实际是弹出一个VIM让我们来编辑，但是网站上没有说怎么编辑，只有UI的操作，这个命令我不会】。

**示例**：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/20230711155636.png)执行git rebase -i HEAD~4后
在弹出的UI框中什么不执行任何动作直接点确认
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/20230711155842.png)
可以看到，HEAD间接指向的提交记录的前四个（c2、c3、c4、c5）提交记录会被处理


## 1.5、杂项

### 1.5.1、提交技巧1
情节描述：你之前在 `newImage` 分支上进行了一次提交，然后又基于它创建了 `caption` 分支，然后又提交了一次；此时你想对某个以前的提交记录进行一些小小的调整。比如设计师想修改一下 `newImage` 中图片的分辨率，尽管那个提交记录并不是最新的了。

思路如下：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/20230711152840.png)

初始 **<font color="red" size = 5>提交树(由提交记录组成的树)</font>** ：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/20230711153637.png)

1. git rebase -i HEAD~2 --solution-ordering C3,C2
2. git commit --amend
3. git rebase -i HEAD~2 --solution-ordering C2'',C3'
4. git rebase caption main
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/20230711153717.png)


### 1.5.2、提交技巧2
情节描述：同5.1
思路：使用【git cherry-pick 哈希值】/【git cherry-pick 相对引用】来解决
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/20230711162240.png)

1. git checkout main
2. git cherry-pick newImage
3. git commit --amend
4. git cherry-pick caption
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/20230711162307.png)


### 1.5.3、git tag
#git标签 

相信通过前面课程的学习你已经发现了：分支很容易被人为移动，并且当有新的提交时，它也会移动。分支很容易被改变，大部分分支还只是临时的，并且还一直在变。

你可能会问了：有没有什么可以_永远_指向某个提交记录的标识呢，比如软件发布新的大版本，或者是修正一些重要的 Bug 或是增加了某些新特性，有没有比分支更好的可以永远指向这些提交的方法呢？

当然有了！Git 的 tag 就是干这个用的啊，它们可以（在某种程度上 —— 因为标签可以被删除后重新在另外一个位置创建同名的标签）永久地将某个特定的提交命名为里程碑，然后就可以像分支一样引用了。

更难得的是，它们并不会随着新的提交而移动。你也不能切换到某个标签上面进行修改提交，它就像是 **<font color="red" size = 5>提交树(由提交记录组成的树)</font>** 上的一个锚点，标识了某个特定的位置。

**示例**：先建立一个标签，指向提交记录 `C1`，表示这是我们 1.0 版本。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/20230711162900.png)执行git tag v1 C1后
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/20230711162930.png)
很容易吧！我们将这个标签命名为 `v1`，并且明确地让它指向提交记录 `C1`，如果你不指定提交记录，Git 会用 `HEAD` 所指向的位置。


情节描述：在这个关卡中，按照目标建立两个标签，然后切换到 `v1` 上面，要注意你会进到分离 `HEAD` 的状态 —— 这是因为不能直接在`v1` 上面做 commit。

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202505312149996.png)
执行如下命令：
```shell
git tag v1 side~1
git tag v0 main~1
git checkout v1
```
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202505312150717.png)


### 1.5.4、git describe

由于标签在代码库中起着“锚点”的作用，Git 还为此专门设计了一个命令用来**描述**离你最近的锚点（也就是标签），它就是 `git describe`！

Git Describe 能帮你在提交历史中移动了多次以后找到方向；当你用 `git bisect`（一个查找产生 Bug 的提交记录的指令）找到某个提交记录时，或者是当你坐在你那刚刚度假回来的同事的电脑前时， 可能会用到这个命令。

`git describe` 的​​语法是：`git describe <ref>`
1. `<ref>` 可以是任何能被 Git 识别成提交记录的引用，如果你没有指定的话，Git 会使用你目前所在的位置（`HEAD`）。
2. 它输出的结果是这样的：`<tag>_<numCommits>_g<hash>`
	1. `tag` 表示的是离 `ref` 最近的标签；
	2. `numCommits` 是表示这个 `ref` 与 `tag` 相差有多少个提交记录；
	3. `hash` 表示的是你所给定的 `ref` 所表示的提交记录哈希值的前几位。

当 `ref` 提交记录上有某个标签时，则只输出标签名称

**示例**：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/20230711170719.png)
`git describe main` 会输出：`v1_2_gC2`
`git describe side` 会输出：`v2_1_gC4`



## 1.6、高级话题
### 1.6.1、选择parent提交记录
#选择父提交记录
操作符 `^` 与 `~` 符一样，后面也可以跟一个数字。

但是该操作符后面的数字与 `~` 后面的不同，并不是用来指定向上返回几代，而是指定合并提交记录的某个 parent 提交。还记得前面提到过的一个合并提交有两个 parent 提交吧，所以遇到这样的节点时该选择哪条路径就不是很清晰了。

Git 默认选择合并提交的“第一个” parent 提交，在操作符 `^` 后跟一个数字可以改变这一默认行为。

**示例1**：这里有一个合并提交记录。如果不加数字修改符直接切换到 `main^`，会回到第一个 parent 提交记录。(在我们的图示中，第一个 parent 提交记录是指合并提交记录正上方的那个提交记录。)
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/20230711180110.png)
执行git checkout main^后
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/20230711180144.png)
执行git checkout main^2后
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/20230711180241.png)

**示例2**：支持链式操作
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/20230711180557.png)执行git checkout HEAD~^2~2后
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/20230711180645.png)


---


# 2、远程

## 2.1、Push & Pull ——Git远程仓库


### 2.1.1、远程分支（Git官网）
#Git官网 、 #远程引用 、 #远程跟踪分支( #远程分支 ) 、 #跟踪分支
这章的内容可以参考Git官网：[3.5 Git 分支 - 远程分支](https://git-scm.com/book/zh/v2/Git-%E5%88%86%E6%94%AF-%E8%BF%9C%E7%A8%8B%E5%88%86%E6%94%AF)，在官网的这一章中介绍了远程分支的相关概念，相关概念如下：
> 
>> 注意：这里说的概念都是来自Git官网，官网的概念更清晰，要和本文章的名词区分！！！
> 
> 1. <font color = "red">远程引用</font>：对远程仓库的引用（指针），包括分支、标签等等了可以通过git ls-remote 【远程仓库名】来显示的获取远程引用的完整列表，或者通过git remote show 【远程仓库名】获得远程分支的更多信息
> 2. <font color = "red">远程跟踪分支</font>：
> 	1. （也就是本小节说的远程分支，虽然名字不一样，但是概念相同）；
> 	2. 它们通常命名为：`<remote name>/<branch name>`；
> 	3. 远程跟踪分支是远程分支（也就是远程仓库中的分支）状态的引用，他们是你无法移动的本地引用，一旦你进行了网络通信，Git就会为你移动它们以精确的反应远程仓库的状态，这样可以提醒你：该分支在远程仓库中的位置就是你最后一次连接到远程仓库的位置。
> 	   例如：如果你想看到最后一次与远程仓库origin通信时master分支的状态，你可以查看origin/master分支。
> 3. <font color = "red">跟踪分支</font>：
>    **<font color = "blue">概念</font>**
>    从一个远程跟踪分支检出一个本地分支会自动创建所谓的“跟踪分支”（它跟踪的分支叫做<font color = "red">“上游分支”</font>），使用命令——>git checkout -b 【本地分支名】 【远程跟踪分支名】。
>    **跟踪分支是与远程分支有直接关系的本地分支。**
>    如果在一个跟踪分支上输入git pull，Git能自动识别去那个服务器上抓取、合并到那个分支。
>    
>    **<font color = "blue">设置跟踪分支</font>**
>    除了命令：git checkout -b 【跟踪分支名】 【远程跟踪分支名】可以创建跟踪分支外，还有两个相同的命令：
>    1. git checkout --tranck 【远程跟踪分支名】
>    2. git checkout 【跟踪分支名】：如果尝试检出的【跟踪分支名】分支在本地仓库不存在，并且在远程仓库中刚好有一个名字为【跟踪分支名】的远程分支，那么可以使用这个命令。
>    
>    **<font color = "blue">修改跟踪分支的上游分支</font>**
>    如果想要修改当前跟踪分支正在跟踪的上游分支，可以使用带有-u或--set-upstream-to选项的git branch显示设置：git branch -u 【远程跟踪分支】。
>    
>    <font color = "blue">取消当前分支或其他分支的上游分支</font>
>    取消当前分支的上游分支：git branch --unset-upstream
>    取消其他分支的上游分支：git branch --unset-upstream 【本地分支名】
>    
>    **<font color = "blue">查看跟踪分支</font>**
>    第一种方式：
>    如果想要查看设置的所有跟踪分支，可以使用git branch -vv，这会将所有的本地分支列出来并且包含更多信息，如每一个分支正在跟踪那个远程分支与我本地分支是否领先、落后或都有。
>    
>    第二种方式：来自[Git远程03：分支的upstream](https://higoge.github.io/2015/07/06/git-remote03/)
>    查看仓库目录下.git/config文件
>    ![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/20230713173440.png)
>    其中[branch "分支名"]下的信息就是upstream信息，remote项表示upstream的远程仓库名，merge项表示远程跟踪分支名。
>    
>    
>    **<font color = "blue">示例</font>**
>    ![](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/20230713165936.png)
>    这里可以看到 `iss53` 分支正在跟踪 `origin/iss53` 并且 “ahead” 是 2，意味着本地有两个提交还没有推送到服务器上。 也能看到 `master` 分支正在跟踪 `origin/master` 分支并且是最新的。 接下来可以看到 `serverfix` 分支正在跟踪 `teamone` 服务器上的 `server-fix-good` 分支并且领先 3 落后 1， 意味着服务器上有一次提交还没有合并入同时本地有三次提交还没有推送。 最后看到 `testing` 分支并没有跟踪任何远程分支。
>    
>    需要重点注意的一点是这些数字的值来自于你从每个服务器上最后一次抓取的数据。 这个命令并没有连接服务器，它只会告诉你关于本地缓存的服务器数据。 如果想要统计最新的领先与落后数字，需要在运行此命令前抓取所有的远程仓库。 可以像这样做：git fetch --all; git branch -vv
>    


### 2.1.2、远程分支（LGB网站）

在这个网站中说的远程分支的概念和[2.1.1、远程分支（Git官网）](2、相关技术/8、常用工具-Git、GitHub/Learn%20Git%20Branching网站学习记录.md#2.1.1、远程分支（Git官网）)中说的远程跟踪分支是一致的。

[Learn Git Branching](https://learngitbranching.js.org/)的内容如下

在执行git clone在本地创建一个远程仓库的拷贝之后，本地的仓库中就会有一个origin/master的分支，这种分支就叫远程分支（在本地仓库中，这个名字的含义和远程跟踪分支一样），远程分支反应了远程仓库中对应分支的状态。

远程分支有一个命名规范，它们的格式是：```<remote name>/<branch name>```，其中remote name是远程仓库的名字（在执行git clone的时候，Git会自动将这个名字设置为origin），在这个学习网站中给它起名为o，branch name是远程仓库中分支的名字（在示例中是main，在远程仓库中有一个main分支）。

远程分支有一个特别的属性，在你切换到远程分支时，自动进入分离 HEAD 状态。Git 这么做是出于不能直接在这些分支上进行操作的原因, 你必须在别的地方完成你的工作, （更新了远程分支之后）再用远程分享你的工作成果。

**<font color = "blue">本地仓库的远程分支在执行fetch/pull、push的时候会更新。</font>**

**示例**：示例中远程分支的名字是o/main，这是简写
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/20230711212008.png)
执行git checkout o/main；git commit后
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202406161333953.png)

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/20230711204536.png)
可以看到，当我们执行git checkout o/main的时候HEAD并没有指向o/main分支，Git变成了分离HEAD状态，当添加新的提交时 `o/main` 也不会更新。这是因为<font color = "red"> `o/main` 只有在远程仓库中相应的分支更新了以后才会更新，当我们从远程仓库获pull/fetch数据或者push数据时, 远程分支会更新以反映最新的远程仓库中的main分支的状态。</font>


### 2.1.3、git fetch
#fetch 
git fetch完成了仅有但是很重要的两步：
1. 从远程从仓库下载本地仓库中缺少的提交记录（所有分支）
2. 更新本地仓库中的远程分支（在这个学习网站中这个分支称之为o/main），让其指向第一步中拉取下来的最新提交记录（所有分支）
<font color = "red">
！！！注意：拉取最新提交记录、更新本地仓库的远程分支   这两步说的是   某个分支（假设是hhy分支）对应的远程分支（假设是origin/hhy分支）来说的；所有分支对应在本地仓库中的远程分支都是这样。
</font>

git fetch将本地仓库中的远程分支更新成远程仓库中相应分支的最新状态。这个命令是本地仓库与远程仓库通信的方式（因为这个命令虽然拉取了最新的提交记录，但是并不能直接在这些新的提交记录上做操作）。
git fetch并不会改变本地仓库的状态，它不会更新本地仓库中的main分支（追踪远程main分支），也不会修改磁盘上的文件；

上一节中说```当我们从远程仓库获取数据时, 远程分支也会更新以反映最新的远程仓库```，在这里以一个示例来描述：示例中本地的main分支追踪远程main分支
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/20230711212041.png)执行git fetch后
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/20230711212108.png)
执行git fetch之后，远程仓库中main分支上的c2、c3提交记录就会被下载到本地仓库，同时本地仓库中的远程分支o/main也反应了这一点。
我感觉这个命令不论HEAD指向哪个分支，都可以成功<font color = "red">获取到远程仓库中所有分支的引用</font>，可以随时手动合并或查看；


### 2.1.3、git pull
#pull
当从远程仓库中拉取下最新的提交记录到本地仓库的远程分支中后，我们可以像合并本地其他分支一样，将本地仓库的远程分支和本地的其他分支合并，可以执行以下命令：
1. git cherry-pick o/main
2. git rebase o/main
3. git merge o/main
4. ……
git pull命令：在[2.1.2、git fetch](2、相关技术/8、常用工具-Git、GitHub/Learn%20Git%20Branching网站学习记录.md#2.1.2、git%20fetch)小节中在本地仓库的main分支下，使用git fetch后会将远程仓库中最新的提交记录拉取到本地仓库，并更新本地仓库的远程分支o/main，但不会改变本地仓库中main分支，导致本地main分支和o/main分支指向的提交记录不同，需要我们手动执行合并，将本地的main分支和o/main分支合并；

git pull命令将这拉取和合并（merge）整合到了一起，不过在执行git pull的时候只会使用git merge合并当前分支与当前分支追踪的本地仓库中的远程分支（也就是远程跟踪分支【Git官网】），不会合并其他分支。

**命令git pull是fetch+merge的缩写，git pull --rebase是fetch和rebase的缩写。**

**<font color = "blue" size=5>总之：在执行git pull的时，对main分支执行拉取后，想让main分支和o/main远程分支合并的话，当前分支就需要是本地的main分支，也就是HEAD指向本地main分支，否则如果在hhy分支上执行git pull后，main分支和o/main分支也不会合并，只会将hhy与o/hhy分支合并。</font>**

**无参命令git pull和git fetch异同：**
1. 相同：**拉取的效果相同**——>无论HEAD指向的分支是不是我们想要拉取的分支，远程仓库上的所有分支的最新提交记录都会被拉取到本地
2. 不同：**合并的效果不同**——>git pull在拉取后会将当前分支（假设是main分支）与当前分支在本地仓库对应的远程分支（假设是o/main）进行合并<font color = "red">（注意：这里只会合并当前分支main和本地仓库的远程分支o/mian，而不会合并其他分支与其他分支对应在本地仓库中的远程分支）</font>，而git fetch不会合并当前分支与当前分支对应在本地仓库中的远程分支。


**示例**：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/20230711222841.png)执行git fetch; git merge o/main后
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/20230711222921.png)
执行git fetch后下载了c3提交记录，加入到本次的 **<font color="red" size = 5>提交树(由提交记录组成的树)</font>** 上，并且将o/main分支指向它；执行git merge后将main分支和o/main分支合并后，main分支指向合并后最新的提交记录。
将git fetch; git merge o/main这两个命令换成git pull后可以达到同样的效果。


### 2.1.4、git push
#push
git push命令不带任何参数也能执行成功，是因为与 Git 的一个名为 `push.default` 的配置有关，这个属性用于设置默认的推送行为，在Git2.0版本之后，这个默认值是simple，含义是：推送当前分支到 upstream 分支上，因此必须有 upstream 分支，并且必须保证本地分支与 upstream 分支同名，否则不能push。关于这个属性的更多信息可以去Git官网查看或参考这篇文章
[补1_Git中的push和pull的默认行为](2、相关技术/8、常用工具-Git、GitHub/补1_Git中的push和pull的默认行为.md)

**示例**：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/20230711230424.png)执行git push后
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/20230711230513.png)
可以看到，当执行push的时候，不但远程仓库中的main分支发生了变化，本地仓库中的远程分支o/main也发生了变化。

### 2.1.5、偏离的提交历史
#提交历史偏离
场景：  
> 假设你周一克隆了一个仓库，然后开始研发某个新功能。到周五时，你新功能开发测试完毕，可以发布了。但是 —— 天啊！你的同事这周写了一堆代码，还改了许多你的功能中使用的 API，这些变动会导致你新开发的功能变得不可用。但是他们已经将那些提交推送到远程仓库了，因此你的工作就变成了基于项目**旧版**的代码，与远程仓库最新的代码不匹配了。
> 
> 这种情况下, `git push` 就不知道该如何操作了。如果你执行 `git push`，Git 应该让远程仓库回到星期一那天的状态吗？还是直接在新代码的基础上添加你的代码，亦或由于你的提交已经过时而直接忽略你的提交？
> 
> 因为这情况（历史偏离）有许多的不确定性，Git 是不会允许你 `push` 变更的。实际上它会强制你先合并远程最新的代码，然后才能分享你的工作。

**示例**：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/20230711231205.png)执行git fetch；git rebase o/main；git push后
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/20230711231300.png)
在合并的时候使用git merge o/main也可以成功push，只是 **<font color="red" size = 5>提交树(由提交记录组成的树)</font>** 有区别，第二条命令换成git merge后
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/20230711231549.png)

命令git pull是fetch+merge的缩写，git pull --rebase是fetch和rebase的缩写。

### 2.1.6、远程服务器拒绝！(Remote Rejected)
如果你是在一个大的合作团队中工作, 很可能是main被锁定了, 需要一些Pull Request流程来合并修改。如果你直接提交(commit)到本地main, 然后试图推送(push)修改, 你将会收到这样类似的信息:
```
! [远程服务器拒绝] main -> main (TF402455: 不允许推送(push)这个分支; 你必须使用pull request来更新这个分支.)
```

**为什么会被拒绝呢？**
远程服务器拒绝直接推送(push)提交到main, 因为策略配置要求 pull requests 来提交更新.
你应该按照流程,新建一个分支, 推送(push)这个分支并申请pull request,但是你忘记并直接提交给了main.现在你卡住并且无法推送你的更新.

知乎中对pull requests的解释：
1. 我改了你们的代码，你们拉回去看看吧 ！！！
2. 有一个仓库，叫Repo A。你如果要往里贡献代码，首先要Fork这个Repo，于是在你的Github账号下有了一个Repo A2,。然后你在这个A2下工作，Commit，push等。然后你希望原始仓库Repo A合并你的工作，你可以在Github上发起一个Pull Request，意思是请求Repo A的所有者从你的A2合并分支。如果被审核通过并正式合并，这样你就为项目A做贡献了


**解决办法**
新建一个分支feature, 推送到远程服务器. 然后reset你的main分支和远程服务器保持一致, 否则下次你pull并且他人的提交和你冲突的时候就会有问题.



## 2.2、关于Origin和它的周边——Git远程仓库高级操作


### 2.2.1、远程追踪
参考[2.1.1、远程分支（Git官网）](2、相关技术/8、常用工具-Git、GitHub/Learn%20Git%20Branching网站学习记录.md#2.1.1、远程分支（Git官网）)小节。
**远程跟踪分支**
这里说的追踪远程分支指的是：本地仓库某个分支追踪本地仓库中远程分支，例如：当我们执行git clone之后，在本地仓库会默认创建master分支，并且这个分支跟踪本地仓库中的远程分支origin/master（origin是git clone后git给远程仓库起的默认名字）。

在前几节课程中有件事儿挺神奇的，Git 好像知道 `main` 与 `o/main` 是相关的。当然这些分支的名字是相似的，可能会让你觉得是依此将远程分支 main 和本地的 main 分支进行了关联。这种关联在以下两种情况下可以清楚地得到展示：

- pull 操作时, 远程仓库中最新的提交记录会被先下载到 o/main 上，之后再合并到本地的 main 分支。隐含的合并目标由这个关联确定的。
- push 操作时, 我们把工作从 `main` 推到远程仓库中的 `main` 分支(同时会更新远程分支 `o/main`) 。这个推送的目的地也是由这种关联确定的！


**远程跟踪**
<font color = "red">直接了当地讲，`main` 和 `o/main` 的关联关系就是由分支的“remote tracking”属性决定的。`main` 被设定为跟踪 `o/main` —— 这意味着为 `main` 分支指定了<font color = "blue">推送的目的地</font>以及<font color = "blue">拉取后合并的目标</font>。</font>

你可能想知道 `main` 分支上这个属性是怎么被设定的，你并没有用任何命令指定过这个属性呀！好吧, 当你克隆仓库的时候, Git 就自动帮你把这个属性设置好了。

当你克隆时, Git 会为远程仓库中的每个分支在本地仓库中创建一个远程分支（比如 `o/main`）。然后再创建一个跟踪远程仓库中活动分支的本地分支，默认情况下这个本地分支会被命名为 `main`。

克隆完成后，你会得到一个本地分支（如果没有这个本地分支的话，你的目录就是“空白”的），但是可以查看远程仓库中所有的分支（如果你好奇心很强的话）。这样做对于本地仓库和远程仓库来说，都是最佳选择。

这也解释了为什么会在克隆的时候会看到下面的输出：

```
local branch "main" set to track remote branch "o/main"
```


**指定分支的“remote tracking”属性**
当然可以啦！你可以让任意分支跟踪 `o/main`, 然后该分支会像 `main` 分支一样得到隐含的 push 目的地以及 merge 的目标。 这意味着你可以在分支 `totallyNotMain` 上执行 `git push`，将工作推送到远程仓库的 `main` 分支上。

有两种方法设置这个属性：
1. 第一种就是通过远程分支切换到一个新的分支，执行:
   `git checkout -b totallyNotMain o/main`；
   就可以创建一个名为 `totallyNotMain` 的分支，它跟踪远程分支 `o/main`。
2. 第二种方法是使用：`git branch -u` 命令，执行：
   `git branch -u o/main foo`;4；
   这样 `foo` 就会跟踪 `o/main` 了。如果当前分支就是 foo 分支, 还可以省略 foo：
   `git branch -u o/main`


**示例1**：方法一设置追踪分支
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/20230712151045.png)执行git checkout -b foo o/main；git pull后
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/20230712151200.png)
在执行git pull之后，使用了隐含的目标 `o/main` 更新了 `foo` 分支，需要注意的是 main 并未被更新！因为执行git checkout -b后当前分支是foo，执行git pull后将远程仓库的所有分支的最新提交拉取到本地，并将foo分支与o/main分支合并。


**示例2**：方法二设置追踪分支
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/20230712171843.png)执行git branch -u o/main foo；git commit；git push后
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/20230712171951.png)



**示例3**：在不切换到main分支的前提下，将提交记录c3推送到远程仓库的mian分支上
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/20230712183703.png)
执行命令
1. git checkout -b side o/main：创建并切换到新分支side，远程追踪side分支
2. git commit -m "xxx"
3. git pull --rebase：拉取远程仓库中o/main分支对应的远程分支上最新的提交，并使用rebase合并
4. git push
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/20230712184110.png)



### 2.2.2、git push的参数
#push参数
在远程跟踪课程中，你已经学到了 Git 是通过当前所在分支的属性来确定远程仓库以及要 push 的目的地的。这是未指定参数时的行为，我们可以为 push 指定参数，语法是：`git push <remote> <place>`。

> 来自Git官网的总结：推送本地分支的更新到远程分支
> 推送的格式：git push <远程主机名> <来源地>:<目的地>  ===>>>git push 【远程仓库名】 【本地分支名】:【远程分支名】
> 
> 推送命令需要关注的是：推送到远程仓库中的那个分支上。
> 推送命令的形式：
> 	1. git push 【远程仓库名】 【本地分支名】:【远程分支名】：与方式二类似，但是比第二种形式更加灵活；
> 	   
> 	   这种方式推送时，<font color = "blue">可以有追踪关系，也可以没有追踪关系</font>，<font color = "red">【本地分支名】和【远程分支名】可以不相同</font>，<font color = "green">如果【远程分支名】在远程仓库中不存在，那么会在远程仓库中创建相应名字的分支</font>。
> 	   
> 	   **！！！注意：** 当执行这条命令的时候，当前分支不是必须指向命令中的【本地分支】。
> 	   
> 	2. git push 【远程仓库名】 【本地分支名】：这种方式推送时，<font color = "blue">可以有追踪关系，也可以没有追踪关系</font>，<font color = "red">将【本地分支名】推送到与之**同名**的远程仓库的分支上</font>，<font color = "green">如果远程仓库中不存在这样的分支，则会在远程仓库中创建名为【本地分支名】的分支</font>；
> 	   
> 	   **！！！注意：** 当执行这条命令的时候，当前分支不是必须指向命令中的【本地分支】。
> 	   
> 	3. git push 【远程仓库名】 :【远程分支名】：删除指定的远程分支，因为这等同于推送一个空的本地分支到远程分支。
> 	   
> 	4. git push 【远程仓库】：**当前分支**与本地仓库中的远程分支有追踪关系，可以省略参数直接推到远程仓库。
> 	   
> 	   是方式二的简写，与方式二的区别：
> 	   1. 当前分支与本地仓库中的远程分支<font color = "blue">必须有追踪关系</font>，如果没有的话会显示：`fatal: The current branch cshhy has no upstream branch.`
> 	   2. 当前分支与追踪的远程仓库中的分支需要<font color = "red">分支名字相同</font>，如果不同的话会显示：
> 	      `fatal: The upstream branch of your current branch does not match the name of your current branch.`
> 	      也就是说远程仓库中<font color = "green">必须存在</font>和当前分支<font color = "red">同名的分支</font>
> 	   4. 在方式二中当前分支名无需指向【本地分支名】，这种方式下，当前分支需指向【本地分支名】
> 	   
> 	5. git push：**当前分支**与本地仓库中的远程分支有追踪关系，并且这样的追踪关系只有一个，可以省略参数直接推送到远程仓库
> 	   与方式四基本类似。


命令：`git push origin main`把这个命令翻译过来就是：切到本地仓库中的“main”分支，获取所有的提交，再到远程仓库“origin”中找到“main”分支，将远程仓库中没有的提交记录都添加上去，搞定之后告诉我。

我们通过“place”参数来告诉 Git 提交记录来自于 main, 要推送到远程仓库中的 main。它实际就是要同步的两个仓库的位置。

需要注意的是，因为我们通过指定参数告诉了 Git 所有它需要的信息, 所以它就忽略了我们所切换分支的属性！

**示例1**：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/20230714002722.png)

执行命令：
1. git push origin main^:foo：（这里不但可以使用分支名还可以使用哈希值、相对引用）
2. git push origin foo:main：（这里不但可以使用分支名还可以使用哈希值、相对引用）
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/20230714002809.png)



**示例2**：执行git push时，来源地为空
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/20230712212749.png)执行git push origin :foo后
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/20230712212824.png)
可以看到，我们通过给 push 传空值 source，成功删除了远程仓库中的 `foo` 分支。


### 2.2.3、git fetch的参数
#fetch参数
我们刚学习了 git push 的参数，很酷的 `<place>` 参数，还有用冒号分隔的 refspecs（`<source>:<destination>`）。 这些参数可以用于 `git fetch` 吗？

你猜中了！`git fetch` 的参数和 `git push` 极其相似。他们的概念是相同的，只是方向相反罢了（因为现在你是下载，而非上传），但是开发人员很少这样做，更常用的还是直接使用git fetch（如果还想要查看更加灵活的用法，可以去网站上或者官网上看看）。

如果你像这个命令这样为 git fetch 设置 的话：`git fetch origin foo`，Git 会到远程仓库的 `foo` 分支上，然后获取所有本地不存在的提交，放到本地的 `o/foo` 上。
**示例**：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/20230712205659.png)执行git fetch origin foo后
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/20230712205733.png)
可以看到只下载了远程仓库中 `foo` 分支中的最新提交记录，并更新了 o/foo。


### 2.2.4、git pull参数
#pull参数
git pull的语法和git fetch、git push的语法一样，都是：
git pull <远程主机名> <来源地>:<目的地>  ===>>>git push 【远程仓库名】 【远程分支名】:【本地分支名】

> 来自Git官网的总结：拉取所有远程分支的最新提交到本地，同时某个远程分支再与当前分支合并
> 拉取的格式：git pull <远程主机名> <来源地>:<目的地>  ===>>>git pull 【远程仓库名】 【远程分支名】:【本地分支名】
> 
> 拉取命令的唯一关注是：合并到本地仓库的哪个分支。
> 拉取命令的形式：
> 	1. git pull 【远程仓库名】 【远程分支名】:【本地分支名】：比第二种形式更灵活，看示例1
> 	2. git pull 【远程仓库名】 【远程分支名】：拉取远程仓库中所有分支的最新提交后，将本地仓库中的某个远程分支和**当前分支**合并，无论当前分支是那个，看示例2。
> 	3. git pull 【远程仓库名】：拉取远程仓库中所有分支的最新提交后，将与**当前分支**存在追踪关系的本地仓库中的远程分支合并
> 	4. git pull：拉取远程仓库中所有分支的最新提交后，如果当前分支在只追踪了本地仓库中一个远程分支，那么将与**当前分支**存在追踪关系的本地仓库中的远程分支合并
> 


**示例1**：执行git pull 【远程仓库名】 【远程分支名】:【本地分支名】形式的拉取
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/20230712233850.png)执行git pull origin main:foo后
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/20230712235040.png)
这个命令做了三件事情：
1. 先在本地创建了一个叫 `foo` 的分支，
2. 再从远程仓库中的 main 分支中下载提交记录，并合并到 `foo`，
3. 最后再 merge 到我们的当前所在的分支 `bar` 上。



**示例2**：执行git pull 【远程仓库名】 【远程分支名】形式的拉取
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/20230712233018.png)
执行git pull origin main后
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/20230712233050.png)
可以看到，在执行git pull的时候，当前分支是bar分支，而指定的远程仓库的分支是main分支，但是在合并的时候，无论本地的当前分支是不是main分支，是不是追踪本地仓库的远程分支o/main，都会进行合并。





