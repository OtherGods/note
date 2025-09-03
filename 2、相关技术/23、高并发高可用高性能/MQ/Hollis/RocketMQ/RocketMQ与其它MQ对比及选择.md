
- Kafka、ActiveMQ、RabbitMQ和RocketMQ对比：[2、Kafka、ActiveMQ、RabbitMQ和RocketMQ都有哪些区别？](2、相关技术/23、高并发高可用高性能/MQ/Hollis/Kafka/2、Kafka、ActiveMQ、RabbitMQ和RocketMQ都有哪些区别？.md)

选择RocketMQ的几个原因：
1. 同步刷盘（默认异步）
2. 事务消息
3. 延时消息
4. 消息过滤（Tag）
   - 不像kafka使用零拷贝中的`sendfile`，RocketMQ使用`mmap`，可以将消息读取到用户态内存用Tag过滤，再将过滤后的消息写入网卡发出去
5. 死信队列
6. 零拷贝

