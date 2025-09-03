远程 Debug，就是远程启动 java 进程，然后在自己的本地的IDE 中进行 debug，使用方式非常简单。

# 1、启动参数配置上 debug 相关内容

```shell
-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005
```

如：
```shell
java -jar -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005 nft-turbo-app-1.0.0-SNAPSHOT.jar
```

> ChatGPT解释：
> 
> 这个命令用于启动一个 Java 应用程序，并启用 Java 调试代理（JDWP），以便远程调试。具体的解释如下：
> 
> **命令结构**
> `java -jar -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005 nft-turbo-app-1.0.0-SNAPSHOT.jar`
> 
> **参数解释**
> 1. **`java -jar`**：
>    - 这是用于运行一个 Java 应用程序的命令。
>    - `-jar` 表示运行一个 JAR 文件。
> 
> 2. **`-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005`**：
>    - 这是用于配置 Java 调试代理（JDWP, Java Debug Wire Protocol）的参数。
>    - **`transport=dt_socket`**：
>      - 指定使用套接字（socket）进行通信。
>    - **`server=y`**：
>      - 表示当前 JVM 将作为调试服务器，允许调试客户端连接。
>    - **`suspend=n`**：
>      - 表示在启动时不挂起 JVM，应用程序会立即运行。
>    - **`address=*:5005`**：
>      - 指定调试服务器监听的地址和端口。`*` 表示监听所有网络接口，`5005` 是监听的端口号。
> 
> 3. **`nft-turbo-app-1.0.0-SNAPSHOT.jar`**：
>    - 这是要运行的 JAR 文件的名称。此文件包含了 Java 应用程序的所有代码和资源。
> 
> **作用**
> 
> - **启动应用程序**：该命令会启动 `nft-turbo-app-1.0.0-SNAPSHOT.jar` 应用程序。
> - **启用远程调试**：通过设置 JDWP 代理，允许开发人员使用远程调试工具（如 IDE）连接到该应用程序进行调试。具体来说：
>   - **监听端口**：调试服务器将监听所有网络接口上的 `5005` 端口。
>   - **调试模式**：允许调试客户端连接到该端口，并进行远程调试。
>   - **运行不中断**：应用程序在启动时不会挂起，而是直接运行，调试客户端可以随时连接进行调试。
> 
> **应用场景**
> 
> - **远程调试**：当应用程序部署在远程服务器上时，开发人员可以使用本地的 IDE（如 IntelliJ IDEA 或 Eclipse）连接到远程服务器进行调试。
> - **问题排查**：当生产环境或测试环境中出现难以复现的问题时，可以启用远程调试，实时查看应用程序的状态，帮助快速定位问题。
> 
> **使用方法**
> 
> 1. **启动应用程序**：在服务器上运行该命令，启动应用程序并开启调试模式。
> 2. **连接调试器**：在本地的 IDE 中配置远程调试，指定连接到服务器的 IP 地址和 `5005` 端口。然后启动调试，会自动连接到远程应用程序。
> 
> 总结，这个命令通过启用 JDWP 代理，允许远程调试工具连接到运行中的 Java 应用程序，从而进行调试和问题排查。

# 2、本地 IDEA 中配置 debug 连接

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202411072312439.png)

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202411072312761.png)

然后 debug 运行即可。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202411072319974.png)

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202411072319400.png)

需要注意的是，如果是用的云服务器，记得开启5005这个端口号，在安全组中配置上即可。
