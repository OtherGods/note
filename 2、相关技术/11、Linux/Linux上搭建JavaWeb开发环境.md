## 1、安装JDK

![img](D:\Tyora\AssociatedPicturesInTheArticles\搭建JavaWeb开发环境\O1CN01JfAMoX1xiyPCBH5Dy_!!6000000006478-2-tps-1404-830.png)



\1. 执行以下命令，查看yum源中JDK版本。

```
yum list java*
```



\2. 执行以下命令，使用yum安装JDK1.8。

```
yum -y install java-1.8.0-openjdk*
```



\3. 执行以下命令，查看是否安装成功。

```
java -version
```



如果显示如下图内容，则表示JDK安装成功。

![img](D:\Tyora\AssociatedPicturesInTheArticles\搭建JavaWeb开发环境\TB1YEopIQL0gK0jSZFAXXcA9pXa-452-54.png)



## 2、安装MySQL

\1. 执行以下命令，下载并安装MySQL官方的Yum Repository。

```
wget http://dev.mysql.com/get/mysql57-community-release-el7-10.noarch.rpm
yum -y install mysql57-community-release-el7-10.noarch.rpm
yum -y install * --nogpgcheck mysql-community-server
```

![img](D:\Tyora\AssociatedPicturesInTheArticles\搭建JavaWeb开发环境\TB1_xIvXlBh1e4jSZFhXXcC9VXa-958-431.png)

\2. 执行以下命令，启动 MySQL 数据库。

```
systemctl start mysqld.service
```

\3. 执行以下命令，查看。MySQL初始密码。

```
grep "password" /var/log/mysqld.log
```

![img](D:\Tyora\AssociatedPicturesInTheArticles\搭建JavaWeb开发环境\TB1h93qIO_1gK0jSZFqXXcpaXXa-834-36.png)

\4. 执行以下命令，输入上条命令中MySQL初始密码，登录数据库。

```
mysql -uroot -p
```

![img](D:\Tyora\AssociatedPicturesInTheArticles\搭建JavaWeb开发环境\TB1.qXDeicKOu4jSZKbXXc19XXa-675-226.png)

\5. 执行以下命令，修改MySQL默认密码。

```
set global validate_password_policy=0;  #修改密码安全策略为低（只校验密码长度，至少8位）。
ALTER USER 'root'@'localhost' IDENTIFIED BY '12345678';
```

\6. 执行以下命令，授予root用户远程管理权限。

```
GRANT ALL PRIVILEGES ON *.* TO 'root'@'%' IDENTIFIED BY '12345678';
```

\7. 输入exit退出数据库。



## 3、安装Tomcat

\1. 执行以下命令，下载Tomcat压缩包。如果该镜像失效，[请查看tomcat最新版本](https://mirrors.tuna.tsinghua.edu.cn/apache/tomcat/tomcat-8/)，并进行替换。

```
wget --no-check-certificate https://labfileapp.oss-cn-hangzhou.aliyuncs.com/apache-tomcat-8.5.72.tar.gz
```



![img](D:\Tyora\AssociatedPicturesInTheArticles\搭建JavaWeb开发环境\TB1h4IPaj39YK4jSZPcXXXrUFXa-1651-151.png)

\2. 执行以下命令，解压刚刚下载Tomcat包。

```
tar -zxvf apache-tomcat-8.5.72.tar.gz
```

\3. 执行以下命令，修改Tomcat名字。

```
mv apache-tomcat-8.5.72 /usr/local/Tomcat8.5
```

\4. 执行以下命令，为Tomcat授权。

```
chmod +x /usr/local/Tomcat8.5/bin/*.sh
```

\5. 执行以下命令，修改Tomcat默认端口号为80。

说明：Tomcat默认端口号为8080。

```
sed -i 's/Connector port="8080"/Connector port="80"/' /usr/local/Tomcat8.5/conf/server.xml
```

![img](D:\Tyora\AssociatedPicturesInTheArticles\搭建JavaWeb开发环境\TB1t9QnIQL0gK0jSZFtXXXQCXXa-829-794.png)

\6. 启动Tomcat。

```
/usr/local/Tomcat8.5/bin/./startup.sh
```

## 4、访问Tomcat

【服务器需要提前关闭防火墙】

\1. 打开浏览器，在地址栏中输入公网地址，例如：139.0.0.1

如果显示如下界面，则表示Tomcat安装配置成功。

![img](D:\Tyora\AssociatedPicturesInTheArticles\搭建JavaWeb开发环境\TB1dVEzIRr0gK0jSZFnXXbRRXXa-1072-933.png)

\2. 至此，Java Web开发环境搭建完成。

