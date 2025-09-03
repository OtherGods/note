在我们的预约功能中，我们针对一个用户的预约，会在 Redis 中存一份，数据库中也会存一份：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202506151602916.png)

为什么要怎么做？

因为 Redis 和 MySQL 的作用不一样，MySQL 的作用是持久化，确保预约数据不丢失。

而 Redis 的作用是缓存，提升插入和查询的性能，同时使用 BitSet 可以减少内存占用，实现时间和空间的双重优势。

但是 Redis 不能作为持久化，即使他有 AOF+RDB也不行，他就不是干持久化的，要持久化，就需要选择关系型数据库，避免 Redis 挂了的时候导致预约数据丢失。

