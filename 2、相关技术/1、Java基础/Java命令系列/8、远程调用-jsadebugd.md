使用`jsadebugd pid server-id`在远程服务器开启调用，jmap、jinfo、jstack作为RMI客户端使用

1. 服务端开启
   ![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202508060038539.png)
2. 本地shell中作为客户端连接，连接可能比较慢
   ![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202508060040746.png)

**==注意：本地JDK和远程JDK的版本必须完全一致==**