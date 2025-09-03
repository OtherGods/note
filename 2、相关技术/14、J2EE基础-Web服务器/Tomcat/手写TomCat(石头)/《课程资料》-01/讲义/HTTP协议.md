## 1.HTTP 超文本传输协议

### 1.1 什么是协议

计算机 A 和计算机 B 之间在传送数据的时候，提前制定好的一种数据传送格式，计算机 A 向计算机 B 发送一个数据包，计算机 B 必须提前知道该数据包的数据格式，才可以成功的将该数据包中有价值的数据解析出来。

例如：小张和小王聊天，小张说：“你吃饭了吗？”，小王说：“吃了”。可见小张和小王可以正常沟通交流，他们为什么可以正常沟通交流呢？因为小张和小王都遵守同一套协议，该协议就是中国普通话协议，其实我们从幼儿园就开始学习这套协议了.

### 1.2 什么是 HTTP 协议

HTTP 协议是一种超文本传输协议，超文本表示不仅可以传送普通的文本，还可以传送一些二进制数据，例如：图片、声音、视频、流媒体等数据。

HTTP 协议是 W3C（万维网联盟组织）制定的，该协议规定了浏览器软件和 web 服务器软件之间在传送数据的时候采用什么样的格式。 这样就可以做到不同类型的浏览器访问不同类型的 Web 服务器。浏览器和 Web 服务器之间解耦合。

浏览器 Browser 向 Web 服务器发送数据，称为请求 request， Web 服务器向浏览器发送数据，称为响应 response。
### 1.3 HTTP 协议初步

#### 1.3.1 HTTP 协议之请求协议

（1） 请求协议包括的主要部分

```
A、 请求行
B、 消息报头
C、 空白行
D、 请求体
```
（2） 请求协议详细内容

![1618210049152](2、相关技术/14、J2EE基础-Web服务器/Tomcat/手写TomCat(石头)/《课程资料》-01/讲义/HTTP协议.assets/1618210049152.png)

#### 1.3.2 HTTP 协议之响应协议

（1） 响应协议包括的主要部分

```
A、 状态行
B、 响应报头
C、 空白行
D、 响应体
```
（2） 响应协议详细内容

![1618217343033](2、相关技术/14、J2EE基础-Web服务器/Tomcat/手写TomCat(石头)/《课程资料》-01/讲义/HTTP协议.assets/1618217343033.png)




## 2.Socket 编程

### 2.1 什么是 Socket 编程

网络上的两个程序通过一个双向的通信连接实现数据的交换，这个连接的一端称为一个 Socket。

Socket 的英文原义是“孔”或“插座”。通常也称作"套接字"，用于描述 IP 地址和端口，可以用来实现不同计算机之间的通信。在 Internet 上的主机一般运行了多个服务软件，同时提供几种服务。每种服务都打开一个 Socket，并绑定到一个端口上，不同的端口对应于不同的服务。 Socket 正如其英文原意那样，像一个多孔插座。一台主机犹如布满各种插座的房间，每个插座有一个编号，有的插座提供 220 伏交流电， 有的提供 110 伏交流电，有的则提供有线电视节目。客户软件将插头插到不同编号的插座，就可以得到不同的服务。

### 2.2 连接过程

根据连接启动的方式以及本地套接字要连接的目标，套接字之间的连接过程可以分为三个步骤：
服务器监听，客户端请求，连接确认。

```
第一步：服务器监听：是服务器端套接字并不定位具体的客户端套接字，而是处于等待连接的状态，实时监控网络状态。

第二步：客户端请求：是指由客户端的套接字提出连接请求，要连接的目标是服务器端的套接字。为此，客户端的套接字必须首先描述它要连接的服务器的套接字，指出服务器端套接字的地址和端口号，然后就向服务器端套接字提出连接请求。

第三步：连接确认：是指当服务器端套接字监听到或者说接收到客户端套接字的连接请求，它就响应客户端套接字的请求，建立一个新的线程，把服务器端套接字的描述发给客户端，一旦客户端确认了此描述，连接就建立好了。而服务器端套接字继续处于监听状态，继续接收其他客户端套接字的连接请求
```
### 2.3 Java 中如何实现 Socket 编程

JavaSE 中提供了实现 Socket 编程的 API，让网络编程变的更简单，更加面向对象。实现两台计算机（两个服务）之间的通讯，至少编写以下的代码：
##### 2.3.1 服务端 Server.java

（1） //创建服务端套接字，表示创建一个服务，并绑定端口号 8080

```java
ServerSocket serverSocket = new ServerSocket(8080);
```
（2） //开始监听网络，准备接收客户端消息，程序在此等待，客户端发送
请求之后，接收客户端套接字

```java
Socket clientSocket = serverSocket.accept();
```
（3） //接收客户端消息

```java
BufferedReader br = new BufferedReader(new
InputStreamReader(clientSocket.getInputStream()));
```

（4） //读取客户端消息

```java
String temp = null;
	While((temp = br.readLine()) != null){
	System.out.println(temp);
}
```
（5） 关闭流，关闭客户端套接字，关闭服务端套接字

```java
package com.bruceliu.demo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * @Auther: bruceliu
 * @Date: 2020/2/5 15:42
 * @QQ:1241488705
 * @Description:服务器端
 */
public class Server {

    public static void main(String[] args) {

        ServerSocket serverSocket = null;
        Socket clientSocket = null;
        BufferedReader br = null;

        try {
            //1.创建服务器端套接字对象，表示创建一个服务，并且将该服务绑定到8080端口上
            serverSocket = new ServerSocket(8080);
            System.out.println("服务器启动了，端口8080...");
            //2.开始监听网络，准备接收客户端的请求，程序在此等待，当客户端请求发起后，接收客户端套接字
            clientSocket = serverSocket.accept();
            System.out.println("clientSocket:" + clientSocket);
            //3.接收client消息
            br = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            //4.读取client的消息
            String temp = null;
            while ((temp = br.readLine()) != null) {
                System.out.println(temp);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            //关闭流
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            //关闭客户端套接字
            if (clientSocket != null) {
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            //关闭服务器套接字
            if (serverSocket != null) {
                try {
                    serverSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
```
#### 2.3.2 客户端 Client.java

（1） //1.创建客户端套接字，指向某台电脑的某台服务

```java
Socket clientSocket = new Socket(“192.168.0.160”,8080);
```
（2） //2.发送消息

```java
String msg = “Hello World!”;
PrintWriter out = new PrintWriter(clientSocket.getOutputStream());
```
（3） //发送

```java
out.print(msg);
out.flush();
```
（4） //关闭流，关闭客户端套接字

```java
package com.bruceliu.demo;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * @Auther: bruceliu
 * @Date: 2020/2/5 15:57
 * @QQ:1241488705
 * @Description:客户端
 */
public class Client {

    public static void main(String[] args) {
        Socket clientSocket=null;
        PrintWriter out=null;
        try {
            //1.创建客户端套接字，指向某台计算机的某台服务器
            clientSocket=new Socket("127.0.0.1",8080);
            //2.发送消息
            String msg="hello,world!";
            out=new PrintWriter(clientSocket.getOutputStream());
            //3.发送
            out.print(msg);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            //关闭流
            if(out!=null){
                out.close();
            }
            //关闭客户端套接字
            if(clientSocket!=null){
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
```
### 2.4 读取从浏览器发送的 HTTP 请求协议

##### 4.4.1 上面的客户端程序不再使用 java 代码，我们尝试将“客户端程序”改为“浏览器客户端软件”，编写以下服务器端程序：

```java
package com.bruceliu.demo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * @Auther: bruceliu
 * @Date: 2020/2/5 15:42
 * @QQ:1241488705
 * @Description:服务器端
 */
public class Server {

    public static void main(String[] args) {

        ServerSocket serverSocket = null;
        Socket clientSocket = null;
        BufferedReader br = null;

        try {
            //1.创建服务器端套接字对象，表示创建一个服务，并且将该服务绑定到8080端口上
            serverSocket = new ServerSocket(8080);
            System.out.println("服务器启动了，端口8080...");
            //2.开始监听网络，准备接收客户端的请求，程序在此等待，当客户端请求发起后，接收客户端套接字
            clientSocket = serverSocket.accept();
            System.out.println("clientSocket:" + clientSocket);
            //3.接收client消息
            br = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            //4.读取client的消息
            String temp = null;
            while ((temp = br.readLine()) != null) {
                System.out.println(temp);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            //关闭流
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            //关闭客户端套接字
            if (clientSocket != null) {
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            //关闭服务器套接字
            if (serverSocket != null) {
                try {
                    serverSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
```
##### 4.4.2 打开 FF 浏览器（标准浏览器），在地址栏上输入以下 URL，然后敲回车键：

##### 4.4.3 Java 控制台读取到请求协议的全部内容，如下图：



