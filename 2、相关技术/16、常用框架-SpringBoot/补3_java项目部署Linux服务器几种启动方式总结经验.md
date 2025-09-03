### 一：两种部署包：

部署之前先说下两种包，java项目部署到服务器一般有用war包的，也有用jar包的，微服务spring-cloud普及后大部分打包都是jar，部署之前先搞清楚自己要打war包还是jar包，下面小介绍两种包的区别： spring boot既可以打成war发布，也可以找成jar包发布。说一下区别：

**jar包**：直接通过内置tomcat运行，不需要额外安装tomcat。如需修改内置tomcat的配置，只需要在spring boot的配置文件中配置。内置tomcat没有自己的日志输出，全靠jar包应用输出日志。但是比较方便，快速，比较简单。

**war包**：传统的应用交付方式，需要安装tomcat，然后放到waeapps目录下运行war包，可以灵活选择tomcat版本，可以直接修改tomcat的配置，有自己的tomcat日志输出，可以灵活配置安全策略。相对打成jar包来说没那么快速方便。

> 个人比较偏向打成jar包的方式发布应用，因为spring boot已经内置了tomcat，无需额外配置。其实可以搜索下spring boot的特点，有个非常重要的特性就是spring boot把市面优秀的开源技术，都集合起来，方便快速应用。技术没有百分百这种好，也没有百分百那种不好，存在即合理，最主要还是看个人习惯和业务场景需求了。

### 二：jar包署部署（推荐）

先说下jar包怎么部署启动项目，这里的jar包前提是springboot项目打的，pom文件已经设置过了入口文件等相应设置，具体设置这里就不说了。

- 先把jar包上传到Linux服务器

> 1.安装 xshell 、xftp软件 Xshell功能简介 Xshell [1] 是一个强大的安全终端模拟软件，它支持SSH1, SSH2, 以及Microsoft Windows 平台的TELNET 协议。Xshell 通过互联网到远程主机的安全连接以及它创新性的设计和特色帮助用户在复杂的网络环境中享受他们的工作。 Xshell可以在Windows界面下用来访问远端不同系统下的服务器，从而比较好的达到远程控制终端的目的。除此之外，其还有丰富的外观配色方案以及样式选择。 Xftp 功能简介 是一个基于 MS windows 平台的功能强大的SFTP、FTP 文件传输软件。使用了 Xftp 以后，MS windows 用户能安全地在 UNIX/Linux 和 Windows PC 之间传输文件。Xftp 能同时适应初级用户和高级用户的需要。它采用了标准的 Windows 风格的向导，它简单的界面能与其他 Windows 应用程序紧密地协同工作，此外它还为高级用户提供了众多强劲的功能特性。 2.通过安装以上两个软件可以实现window电脑远程控制Linux服务器，这样就可以将我们打包好的jar文件传输到Linux服务器上进行项目的部署。

假设Linux服务上已经有了打好的jar包，下面介绍几种常用的部署方式：

#### 2.1、java -jar启动方式。

复制代码

`java -jar *.jar`

此中方式只会运行在当前窗口，当关闭窗口或断开连接，jar程序就会结束。

#### 2.2、nohup启动方式。（推荐）

```bash
# nohup: 不挂断的运行命令 
# &：后台运行 
# >: 日志重定向输出到 
nohup java -jar *.jar >jarLog.txt &
```

#### 2.3、注册为Linux服务（推荐）

- 首先需要现修改pom中spring-boot-maven-plugin配置，其实spring boot 打成jar包以后，是可以直接像shell脚本一样直接运行的，要实现这样可以直接运行，pom.xml 的build节点需要增加这样的配置：

```xml
<!--这样配置后，通过maven打出来的jar 可以直接 执行  ./aabb.jar  就能运行起来。 -->
<build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <executable>true</executable>
                </configuration>
            </plugin>
        </plugins>
    </build>

```

- 在Linux上注册服务（此处基于init.d注册部署）

将打好的jar包放到Linux任意目录 eg: /var/project/
```bash
sudo ln -s /var/project/demo.jar /etc/init.d/abc
（其中demo为jar包名称,abc为服务名称）

```

运行成功后会在init.d目录下面生成一个abc文件（青色的链接格式）

之后就可以用 service XXX start 命令来启动jar包

---

启动/关闭 服务: service abc start/stop

查看状态: service abc status

设置开机自启: chkconfig abc on

---

#### 2.4、systemctl启动方式。

- 在/usr/lib/systemd/system目录新增’abc.service’文件（文件名自己定义我这里例子是abc.service），具体内容如下：

```ini


[Unit]
Description=abc.service
Requires=mysql.service mongod.service redis.service
Wants=abc.service
After=syslog.target network.target mysql.service mongod.service redis.service abc.service

[Service]
User=manager
Group=manager
EnvironmentFile=/home/.bash_profile
WorkingDirectory=/home/tomcat
ExecStart=/usr/bin/java -Xms512m -Xmx512m -jar /home/你的项目名.jar --spring.profiles.active=test

[Install]
WantedBy=multi-user.target




```

- 更改service之后要：systemctl daemon-reload，上述文件中用到的.bash_profile文件如下：

```bash
# .bash_profile

# Get the aliases and functions
if [ -f ~/.bashrc ]; then
	. ~/.bashrc
fi

# User specific environment and startup programs

PATH=$PATH:$HOME/.local/bin:$HOME/bin

LOG_PATH=/home/logs
export LOG_PATH
export PATH


```

其中上述中的LOG_PATH可以在项目中引用，例如：

![](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202308142209766.webp)

#### 2.5、tomcat启动方式。

直接将*.jar文件拷贝到tomcat\webapps\目录下，启动tomcat，访问localhost：8080/jar包名即可。 注意：启动tomcat的时候优先用服务方式启动tomcat如下： `nohup ./startup.sh &`（&可以用于后台运行）

注意：用tomcat启动jar需要注意的是打包的时候需要把包里面tomcat排除掉：

```xml
<dependency>
	<groupId>org.springframework.boot</groupId>
	<artifactId>spring-boot-starter-web</artifactId>
	<exclusions>
		<exclusion>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-tomcat</artifactId>
		</exclusion>
	</exclusions>
</dependency>
```

```xml
//我在另一个地方看到的，拿到这里作为一个补充

<!-- 多模块排除内置tomcat -->
<dependency>
	<groupId>org.springframework.boot</groupId>
	<artifactId>spring-boot-starter-web</artifactId>
	<exclusions>
		<exclusion>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-tomcat</artifactId>
		</exclusion>
	</exclusions>
</dependency>
		
<!-- 单应用排除内置tomcat -->		
<exclusions>
	<exclusion>
		<artifactId>spring-boot-starter-tomcat</artifactId>
		<groupId>org.springframework.boot</groupId>
	</exclusion>
</exclusions>
```

#### 2.6、基于docker云部署。 [www.jianshu.com/p/ec477d84f…](https://link.juejin.cn?target=http%3A%2F%2Fwww.jianshu.com%2Fp%2Fec477d84fc7d "http://www.jianshu.com/p/ec477d84fc7d")
转载到[补4_基于docker部署jar项目](2、相关技术/16、常用框架-SpringBoot/补4_基于docker部署jar项目.md)
最后补充一些常规命令：

> 在Linux项目上对项目进行操作的命令符如下： 后台暂时运行：java -jar /root/yyxx/cloud-yyxx-web-1.0-exec.jar（后台暂时运行） 后台永久运行，想要停止需杀死后台进程：nohup java -jar /root/yyxx/cloud-yyxx-web-1.0-exec.jar &（） 查看jar进程：ps aux|grep cloud-yyxx-web-1.0-exec.jar 杀掉进程： kill -9 进程号

## 三：war包署部署

SpringBoot项目中可以不去出内嵌Tomcat，不排除也能在容器中部署war包。

最简单，常见的部署方法，直接将war包放到tomcat的wabapp目录下，运行tomcat就行。 具体步骤如下： 
a. 把项目打包到wabapp目录下。如下图

![](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202308142210730.webp)

b.在bin目录下运行命令 startup.bat 启动项目。（在bin目录里按 shift+右键 即可调出命令框。关闭项目 shutdown.bat）如下图

![](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202308142210003.png)

你放在wabapp下的所有项目就会自启动，自启动伴随着解压缩包的动作，启动完成后在wabapp下会看到解压后的项目文件夹。运行成功的命令显示如下

![](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202308142211686.png)


这样就表示项目启动成功！打开浏览器访问下
![](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202308142211936.png)

  

转载自：[https://juejin.cn/post/6844904052153647111](https://juejin.cn/post/6844904052153647111)
