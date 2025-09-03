#bufferPool #bufferPool刷盘 #bufferPool写过程 #bufferPool读过程 
# 典型回答

MySQL的`Buffer Pool`是一个**内存区域**，用于**缓存数据页**，从而提高查询性能。读写过程涉及到数据的从磁盘到内存的读取，以及在内存中的修改和写回磁盘。
[59、什么是buffer pool？](2、相关技术/4、数据库-MySQL/Hollis/59、什么是buffer%20pool？.md)

## 读过程

当我们在MySQL执行一个查询请求的时候，他的过程是这样的：
1. MySQL首先检查`Buffer Pool`中是否存在本次查询的数据。如果数据在Buffer Pool中，就直接返回结果。
2. 如果数据不在`Buffer Pool`中，MySQL会从磁盘读取数据。
3. 读取的数据页被放入`Buffer Pool`，同时MySQL会返回请求的数据给应用程序。

读的过程比较简单的，而`Buffer Pool`的写的过程就有点复杂了

## 写过程

当我们执行一次更新语句，如`INSERT`、`UPDATE`或`DELETE`等时，会进行以下过程
1. 当应用程序执行写操作时，MySQL首先将要修改的数据页加载到`Buffer Pool`中。
2. 在`Buffer Pool`中，对数据页进行修改，以满足写请求。这些修改只在内存中进行，不会立即写回磁盘。
3. 如果`Buffer Pool`中的数据页被修改过，MySQL会将这个页标记为“脏页”（Dirty Page）。
4. **脏页被写回磁盘，此时写入操作完成，数据持久化**。

但是需要注意的是，**==脏页写回磁盘是由一个后台线程进行的==**，在MySQL **==服务器空闲或负载较低时==**，InnoDB会进行脏页刷盘，以减少对用户线程的影响，降低对性能的影响（[https://dev.mysql.com/doc/refman/8.0/en/innodb-buffer-pool-flushing.html](https://dev.mysql.com/doc/refman/8.0/en/innodb-buffer-pool-flushing.html)）。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202507192126152.png)
[11、InnoDB的一次更新事务过程是怎么样的？](2、相关技术/4、数据库-MySQL/Hollis/11、InnoDB的一次更新事务过程是怎么样的？.md)

当脏页的百分比达到`innodb_max_dirty_pages_pct_lwm`变量定义的低水位标记时，将启动缓冲池刷新。缓冲池页的默认低水位标记为10%。`innodb_max_dirty_pages_pct_lwm`值设为0会禁用这种提前刷新行为。

InnoDB还使用了一种适应性刷新算法，根据redo log的生成速度和当前的刷新率动态调整刷新速度。其目的是通过确保刷新活动与当前工作负载保持同步，来平滑整体性能。

当然，我们也可以手动触发脏页的刷新到磁盘，例如通过执行 `SET GLOBAL innodb_buffer_pool_dump_now=ON` 来进行一次脏页刷新。

还有一种情况，就是在MySQL服务器正常关闭或重启时，所有的脏页都会被刷新到磁盘。这样才能保证数据可以持久化下来。

## `bufferPool` 脏页刷盘时机

1. **==脏页的百分比==** 达到`innodb_max_dirty_pages_pct_lwm`变量定义的低水位标记，缓冲池页的默认低水位标记为`10%`
2. **==适应性刷新算法==**，根据`redo log`的生成速度和当前的刷新率动态调整刷新速度。其目的是通过确保刷新活动与当前工作负载保持同步，来平滑整体性能
3. **==手动触发脏页的刷新到磁盘==** ，例如通过执行 `SET GLOBAL innodb_buffer_pool_dump_now=ON` 来进行一次脏页刷新
4. MySQL服务器 **==正常关闭或重启==** 时