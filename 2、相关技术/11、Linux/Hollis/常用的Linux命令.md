
## 1、系统信息

### 1.1、top

**top**：实时显示 *==系统进程和资源使用情况==*；**==分为两部分：系统汇总信息（前几行）、进程列表（下方的表格）==**。

当线上报警CPU占用率过高，load飙高等的时候，我们通常会先上去使用top命令看一下具体哪些进程耗费了资源；关注信息：
1. **看负载**：先看第一行的 `load average`，结合CPU核心数判断系统是否繁忙。
2. **看CPU**：看第三行的 `%id`（空闲）和 `%wa`（I/O等待）。高 `%wa` 是I/O瓶颈的信号。
3. **看内存**：不要只看 `free`，重点看 `avail Mem`。如果 `avail Mem` 很小，同时 `swap used` 在增加，说明内存严重不足。
4. **找元凶**：在进程列表里，按 `P`（CPU）或 `M`（内存）排序，找到消耗资源最多的进程。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202508092009136.png)

#### 系统汇总信息

```shell
top - 10:30:00 up 10 days,  1:15,  1 user,  load average: 0.05, 0.10, 0.15
Tasks: 120 total,   1 running, 119 sleeping,   0 stopped,   0 zombie
%Cpu(s):  1.5 us,  0.5 sy,  0.0 ni, 98.0 id,  0.0 wa,  0.0 hi,  0.0 si,  0.0 st
MiB Mem :   7822.4 total,    512.3 free,   4096.1 used,   3214.0 buff/cache
MiB Swap:   2048.0 total,   2048.0 free,      0.0 used.   3426.2 avail Mem
```
**第一行：`top - 10:30:00 up 10 days, 1:15, 1 user, load average: 0.05, 0.10, 0.15`**
- `10:30:00`：当前系统时间。
- `up 10 days, 1:15`：系统已运行时间。这里表示系统已经连续运行了10天1小时15分钟。
- `1 user`：当前登录到系统的用户数量。
- `load average: 0.05, 0.10, 0.15`：**系统平均负载**，这是非常关键的指标。它显示的是过去1分钟、5分钟和15分钟的系统平均负载。
    - **如何理解**：对于单核CPU，1.00表示CPU刚好满负荷。如果超过1.00，表示有进程在排队等待CPU。
    - **多核CPU**：对于N核CPU，负载达到N.00才算满负荷。例如，4核CPU的满负荷值是4.00。
    - 上面的例子（0.05, 0.10, 0.15）表示系统非常空闲。
**第二行：`Tasks: 120 total, 1 running, 119 sleeping, 0 stopped, 0 zombie`**
- 显示了进程的总体状态。
- `total`：当前系统中的总进程数。
- `running`：正在运行或在运行队列中等待运行的进程数。
- `sleeping`：处于睡眠状态的进程数（等待某个事件发生，如I/O操作）。
- `stopped`：被停止的进程数（例如，通过 `Ctrl+Z` 暂停）。
- `zombie`：**僵尸进程**数。这是指已经终止但其父进程尚未回收其资源的进程。如果这个数值非零且持续增加，可能表示有程序存在问题。
**第三行：`%Cpu(s): 1.5 us, 0.5 sy, 0.0 ni, 98.0 id, 0.0 wa, 0.0 hi, 0.0 si, 0.0 st`**
- 显示了CPU时间的使用百分比。在CentOS 7中，默认是所有CPU核心的总体情况（按数字`1`可以切换到每个核心的视图）。
- `us`（user）：用户空间进程占用CPU的百分比。
- `sy`（system）：内核空间进程占用CPU的百分比。
- `ni`（nice）：被调整过优先级的用户进程（niced）占用CPU的百分比。
- `id`（idle）：CPU空闲百分比。**这个值越高，说明CPU越空闲**。
- `wa`（I/O wait）：CPU等待I/O操作完成的时间百分比。**这个值如果持续很高，通常表示磁盘或网络I/O存在瓶颈**。
- `hi`（hardware interrupts）：处理硬件中断所花费的CPU时间。
- `si`（software interrupts）：处理软件中断所花费的CPU时间。
- `st`（steal time）：在虚拟化环境中，被宿主机（Hypervisor）“偷走”的CPU时间。如果你的系统是虚拟机，这个值过高表示宿主机资源紧张。
**第四、五行：内存和交换空间**
```shell
MiB Mem :   7822.4 total,    512.3 free,   4096.1 used,   3214.0 buff/cache
MiB Swap:   2048.0 total,   2048.0 free,      0.0 used.   3426.2 avail Mem
```
- **Mem行**（物理内存）：
    - `total`：总物理内存。
    - `free`：完全未被使用的内存。
    - `used`：已使用的内存。
    - `buff/cache`：被用作**缓冲区**和**页面缓存**的内存。**在Linux中，这是为了提高性能而设计的，当应用程序需要时，这部分内存可以被快速回收利用。所以不要把 used 内存看得太可怕。**
- **Swap行**（交换分区）：
    - `total`：总交换分区大小。
    - `free`：空闲的交换分区。
    - `used`：已使用的交换分区。如果这个值非零且持续增长，说明物理内存不足，系统正在频繁使用硬盘作为虚拟内存，这会严重影响性能。
    - `avail Mem`（Available Memory）：**这是一个非常重要的指标**。它表示在不发生交换的情况下，可供新应用程序使用的内存估计值。它是 `free` 内存加上大部分可回收的 `buff/cache` 内存。**你应该更多关注这个值，而不是 `free`。**

#### 进程列表

```shell
PID USER      PR  NI    VIRT    RES    SHR S  %CPU  %MEM     TIME+ COMMAND
1234 mysql     20   0  12.3g   2.1g   1.2g S   5.6  27.5 100:15.67 mysqld
5678 nginx     20   0   80.0m   10.5m   5.2m S   1.0   0.1   0:10.23 nginx
9012 john      20   0  162.8m   15.2m   8.1m R   0.7   0.2   0:00.05 top
```
- `PID`：进程ID。
- `USER`：进程所有者的用户名。
- `PR`（Priority）和 `NI`（Nice）：进程的优先级。`NI` 值可以从 -20（最高优先级）到 19（最低优先级）。`PR` 是内核看到的优先级。
- `VIRT`：进程使用的虚拟内存总量（单位 KiB 或 MiB）。它包括所有代码、数据和共享库，以及被交换出去的内存和映射但未使用的内存。
- `RES`（Resident Set Size）：进程使用的、未被换出的物理内存大小（单位 KiB 或 MiB）。**这个值更真实地反映了一个进程消耗的物理内存**。
- `SHR`：进程使用的共享内存大小。
- `S`（Status）：进程状态。
    - `R`：运行中或可运行（在运行队列中）
    - `S`：睡眠中（可中断）
    - `D`：不可中断的睡眠（通常与I/O有关）
    - `Z`：僵尸进程
    - `T`：已停止
- `%CPU`：进程自上次更新以来使用的CPU时间百分比。
- `%MEM`：进程使用的物理内存百分比（`RES / total Mem`）。
- `TIME+`：进程自启动以来使用的总CPU时间。
- `COMMAND`：启动该进程的命令行。

#### 常用交互命令（在 `top` 运行时按下的键）

- `q`：退出 `top`。
- `k`：终止一个进程。会提示你输入要终止的PID。
- `1`：在显示所有CPU核心的总体状态和每个核心的详细状态之间切换。
- `M`：按内存使用率（%MEM）降序排序。
- `P`：按CPU使用率（%CPU）降序排序（默认）。
- `T`：按进程占用CPU时间（TIME+）排序。
- `u`：然后输入用户名，只显示属于该用户的进程。
- `Shift + f`：进入字段管理界面，你可以选择显示或隐藏哪些列，以及改变排序的列。
- `Shift + m`：与按 `M` 键相同，按内存排序。
- `h`或`?`：显示帮助信息。

### 1.2、df

**df**：显示**磁盘空间使用情况**。当线上服务器报警磁盘满的时候，需要上去查看磁盘占用情况，可以使用这个命令
   `-h`（--human-readable）：以易于阅读的格式（如MB、GB）显示信息。
   ![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202508092029007.png)

### 1.3、du

**==du：显示目录或文件的磁盘使用量。==**

常用参数：
- `-s` （--summarize）：仅显示某目录总计大小，只列出最后加总的值
- `-h`（--human-readable）：以易于阅读的格式（如MB、GB）显示信息
- `-a` （--all）：显示目录中个别文件的大小
   
   示例：
   - `du -sh ./*/`：只显示当前目录下每个子目录的总大小
   - `du -h ./*`：查看指定目录下文件（文件、文件夹等）所占的空间
   - `du -sh .`：只显示当前目录总和的大小且易读
   - `du -ah .`：详细查看当前目录，子目录下的，所有文件和目录及其易读大小
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202508092148201.png)

| **维度**   | `df` (Disk Free)        | `du` (Disk Usage)  |
| -------- | ----------------------- | ------------------ |
| **核心功能** | 显示**文件系统整体**的空间使用       | 统计**文件/目录**占用的磁盘空间 |
| **数据来源** | 读取文件系统**超级块**(metadata) | 递归**遍历文件**计算实际大小   |
| **工作层级** | 文件系统级别                  | 文件/目录级别            |
| **速度**   | 极快（直接读元数据）              | 较慢（需遍历文件）          |
| **显示单位** | 默认1K块大小                 | 默认KB，但可人性化显示       |

### 1.4、date

`date`：详细查看当前目录，子目录下的，所有文件和目录

## 2、系统管理

1. `ps`：查看当前进程。通常用来查看Java进程的情况以及检查JVM参数：
```shell
//查找java进程，相当于jps命令，但是有的时候线上服务器没办法执行jps，可以用以下命令代替
ps aux|grep java
//查询java进程，并高亮显示Xmx参数部分
ps aux|grep java | grep --color Xmx

ps -f | grep java
```
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202508092150510.png)

2. `kill`：杀死进程，慎用，尤其是在生产环境中，尤其是`kill -9`。
   [4、对JDK进程执行kill -9有什么影响？](2、相关技术/3、JVM/Hollis/4、对JDK进程执行kill%20-9有什么影响？.md)
3. chmod：更改文件或目录权限。
   [1、chmod](一、文件#1、chmod)
4. chown：更改文件或目录的所有者和群组。
   [3、chown](一、文件#3、chown)

## 3、文件操作

这里就是一些非常常用的文件操作命令了，每一个都不展开讲了，都比较简单，都是必会的。
1. `vi/vim`：[2. vim 编辑器使用方法](半小时搞会%20CentOS%20入门必备基础知识#2.%20vim%20编辑器使用方法)
2. `ls`：列出目录内容。当需要显示隐藏文件的时候用ls -a
3. `ll`：`ll`是`ls -l`命令的一个别名，用于以详细列表格式显示当前目录中的文件和目录。
   ![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202508092153910.png)
3. `cd`：更改当前目录。
4. `pwd`：显示当前目录路径。
5. `open`：直接打开当前文件夹，这个命令在linux中用的不多，但是我在mac中用的比较多，当我在idea中的时候，想要打开当前目录的文件夹，我就会Terminal中使用open命令。这个命令会通过文件管理器打开当前目录。
   ![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202508092154544.png)
6. `mkdir`：创建新目录。
7. `rmdir`：删除空目录。
8. `rm`：删除文件或目录。
9. `cp`：复制文件或目录。
10. `mv`：移动或重命名文件或目录。
11. `touch`：创建空文件或更新文件时间戳。
12. `find`：搜索文件和目录。find非常好用，介绍下我常见的用法：
```shell
// 在当前目录及其子目录中查找名为filename.txt的文件：
find . -name filename.txt

//查找所有目录
find . -type d

//查找所有普通文件
find . -type f

//查找大于10MB的文件
find . -size +10M

//查找修改时间在过去7天内的.log文件
find . -name "*.log" -mtime -7
```

## 4、日志查看

日志查看是一个非常高频的命令，我常用的日志查看命令有以下这些：
1. `cat`：查看文件内容。用于查看较小的文本文件
2. `more / less`：分页查看文件内容。`less`可以翻页，more不能翻页。查看较大的文本文件。
3. `tail`：查看文件末尾内容，通常用来实时监视日志文件的新增内容，默认只展示最后10行：
```shell
tail -f application.log

//只滚动输出ERROR的日志
tail -f application.log |grep ERROR
```

5. `head`：查看文件开始部分的内容。用于快速查看文件的开头部分。
6. `grep`：搜索文件中的文本行，并显示匹配的行。通常用来查找包含特定关键词的日志条目。
   - `-A`：打印匹配本身以及随后的几个行由NUM控制
   - `-B`：打印匹配本身以及前面的几个行由NUM控制
   - `-C`：打印匹配本身以及随后，前面的几个行由NUM控制
   - `-c`：显示匹配的个数，不显示内容
   - `-e`：`grep -e '^\(root\|zhang\)'`正则匹配，`-e`可以省去
   - `-n`：在匹配的行前面加上该行在文件中，或者输出中所在的行号
```shell
//查询日志中有ERROR的行
grep "ERROR" application.log

//查询日志中有ERROR和Biz的行
grep "ERROR" application.log | grep "Biz"
```

## 5、网络和通信

1. `ping`：检测网络到另一台主机的连接。
2. `curl / wget`：从网络上下载文件。
3. `netstat`：显示网络连接、路由表、接口统计、端口等信息。
   - `-t`（--tcp）：显示TCP传输协议的连线状况
   - `-u`（--udp）：显示UDP传输协议的连线状况；
   - `-l`（--listening）：只显示正在侦听的套接字(这是默认的选项)
   - `-n`（--numeric）：直接使用ip地址，而不通过域名服务器
   - `-p`（--programs）：显示套接字所属进程的PID和名称
4. `ssh`：安全远程登录。
5. `scp`：通过SSH复制远程文件。
6. `telnet`：主要被用于创建到远程主机的终端会话，或者测试远程主机上特定端口的可达性和服务的响应性。（我之所以这个命令用的多，是因为我们自己的web容器会在本地起一个端口记录我们的应用提供了哪些RPC服务和暴露了哪些RPC服务，所以有时候检查服务的时候需要用到它。）
7. `ifconfig`：查看和更改网络接口的配置，例如IP地址、子网掩码和广播地址。有的时候我们需要做远程debug，需要知道远程机器的ip地址，就可以通过这个命令来查看
   ![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202508092221461.png)

## 6、压缩与解压

1. `tar`：压缩和解压tar文件。
2. `gzip / gunzip`：压缩和解压gzip文件。
3. `zip / unzip`：压缩和解压zip文件。

## 7、包管理

1. `apt-get`（Debian系）、`yum`（RedHat系）：软件包的安装、更新和管理（根据你的Linux发行版而定）。

## 8、Git&Maven

1. `git`：版本控制工具，常用于代码管理。
2. `maven`：包管理，仲裁管理
```shell
//删除之前构建生成的所有文件（例如，target目录下的文件）
mvn clean

//将最终的包（如JAR、WAR等）部署到配置的远程仓库
mvn deploy

//先清理项目，然后执行构建并安装到本地仓库，同时跳过测试。
mvn clean install -Dmaven.test.skip=true

//-U参数会强制Maven更新依赖，即检查远程仓库中是否有更新的snapshot版本，并下载更新。
mvn clean install -Dmaven.test.skip=true -U

//生成项目依赖树，并将输出重定向到名为tree的文件
mvn dependency:tree > tree
```

## 9、系统监控和性能分析

1. `top`：[系统信息](2、相关技术/11、Linux/Hollis/常用的Linux命令.md#系统信息)
2. `vmstat`：显示虚拟内存统计信息。
3. `iostat`：显示CPU和输入/输出统计信息。
4. `dmesg`：显示内核相关的日志信息。

掌握这些基本命令可以帮助你更高效地管理和维护Linux环境，对于Java Web开发来说尤其重要。随着经验的积累，你可能还会需要学习更多高级命令和脚本来处理复杂的任务。