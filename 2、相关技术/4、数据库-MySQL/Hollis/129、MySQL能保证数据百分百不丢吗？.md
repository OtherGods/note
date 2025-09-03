#两阶段提交中日志的丢失 

> ChatGPT补充
> *==MySQL中使用 `Direct I/O` 来读写数据文件（`.ibd`）==*，以避免其 `Buffer Pool` 和 OS `Page Cache` 之间的双缓冲。
> 
> 但同时，*==MySQL通常使用 `Buffered I/O` 来写重做日志（Redo Log）==*，因为日志是顺序追加写入，利用 Page Cache 的异步刷盘机制可以获得极高的吞吐量（*空间局部性原理*），然后定期调用 `fsync()` 来保证持久性。这完美展示了如何根据不同的需求混合使用两种 I/O 模式。

# 典型回答

这种绝对性的问题，答案肯定是不能的。

首先，MySQL有很多引擎，其中一部分是基于磁盘的，比如Innodb，Myisam等，但是也有基于内存的，比如Memory，而基于内存的这种如果断电了，可能就丢数据了。

那么，基于磁盘的就万无一失了么？其实也不是。

日志要写入磁盘要经过几个过程：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202507161852522.png)

如果你看过下面这篇文章，你会知道，MySQL为了保证数据不丢，在事务写入的时候做了2阶段提交。

[71、什么是事务的2阶段提交？](2、相关技术/4、数据库-MySQL/Hollis/71、什么是事务的2阶段提交？.md)

这里面画图的时候其实是考虑的 **默认情况** 的，啥意思呢，先来看2个MySQL的参数。

`innodb_flush_log_at_trx_commit`是MySQL InnoDB存储引擎独有的参数，用于控制InnoDB的<font color="red" size=5>Redo log日志记录方式</font>。取值范围为0、1、2：
- 0：只写入LogBuffer，不会把Redo日志写入磁盘，而是靠InnoDB的后台线程每秒写入磁盘。
- **1（默认值）：写入LogBuffer，并立即将LogBuffer数据写入磁盘缓冲区并刷盘。**
- 2：写入LogBuffer，并立即将Redo日志写入操作系统的磁盘缓冲区，每秒由操作系统调度刷盘一次。

`sync_binlog` 是MySQL 的 <font color="red" size=5>Binlog日志的重要参数</font>，用于控制Binlog的更新策略。取值范围 0、1 或 N（正整数）：
- `0`：事务提交后仅将Binlog写入文件系统缓存，依赖操作系统调度刷盘。
- **`1`（默认值）：每次事务提交后立即将Binlog写入磁盘。**
- `> 1`：每N个事务提交后，立即将Binlog写入磁盘。

那么也就是说，如果你了解操作系统的 **`write` 和 `fsync` 指令**的话，翻译一下就是这样的：
[write和fsync的区别是什么？](4、操作系统/Hollis/write和fsync的区别是什么？.md)

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202507162049676.png)

那么也就意味着，如果你想让数据不丢，至少要把 `innodb_flush_log_at_trx_commit` 和 `sync_binlog` 都设置为1，让他们立刻写入磁盘并刷盘。

**但是，这样只是能极大程度上保证不丢，在一些极端情况下还是会丢的**。

## 丢数据的情况

按照两阶段的介绍，两阶段中undolog与redolog中xid不同导致回滚也会发生数据丢失；**==下面是介绍两阶段中undolog与redolog中日志写入异常情况的原因==**：
1. **fsync 只是请求刷盘**，不一定真正落到“磁盘介质”
	- 有些硬盘或 RAID 控制器会“欺骗 fsync” —— 把数据写入自己的缓存里，但并未真正写入磁盘。
	- 如果掉电或主板故障，缓存数据就丢了。
2. **写缓存未关闭**
	- 如果设备层还是启用了缓存（如 write-back cache），操作系统或数据库认为“写入完成”，但数据仍只在硬件缓存中。
3. **电源断电 / 非正常宕机时**
	- 如果没有使用 UPS（不间断电源）或 BBU（电池保护的 RAID），哪怕你调用了 fsync，掉电依然可能导致数据丢失。
4. **磁盘损坏**
	- 如果磁盘本身发生物理损坏，比如坏道、芯片失效，那么写入其上的数据自然就丢失了。

## 如何提升不丢的保障

根据上面的情况，其实还是可以想办法解决和避免的。
1. 配置 `innodb_flush_log_at_trx_commit=1` 和 `sync_binlog=1`
2. 为数据库服务器**配备带电池备份 (BBU) 的RAID卡** 或 **支持掉电保护的SSD**。
3. 使用`RAID 10`来做备份和冗余。

RAID，Redundant Array of Independent Disks是一种通过将多个物理硬盘组合起来形成一个逻辑存储单元的技术  

BBU, Battery Backup Unit（电池备份单元） ，是 RAID 控制卡上的关键组件，用于 保护 RAID 缓存数据，防止因意外断电导致数据丢失或损坏。

`RAID 10` **==磁盘阵列==**（也称为 RAID 1+0）是 高性能 + 高可靠性 的存储方案，可以用来**防止因【==磁盘损坏==】而导致的==数据丢失和服务中断==**
