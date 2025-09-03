在 Linux CentOS 系统上安装完 MATLAB 后，为了使用方便，需要将 matlab 命令加到系统命令中，如果在没有添加到环境变量之前，执行“matlab”命令时，则会提示命令不存在的错误，如下所示：

![](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202307191102097.png)

下面我详细介绍一下在 linux 下将 MATLAB 加入到环境变量中的方法（MATLAB 安装在 /usr/local/MATLAB/R2013a/bin 目录下）。

方法一（暂时生效）

直接运行命令export PATH=$PATH:/usr/local/MATLAB/R2013a/bin ，使用这种方法，只会对当前回话生效，也就是说每当登出或注销系统以后，PATH 设置就会失效，只是临时生效。

方法二（只对当前登陆用户生效，永久生效）

执行 vim ~/.bash_profile 修改文件中 PATH 一行，将 /usr/local/MATLAB/R2013a/bin 加入到 PATH=$PATH:$HOME/bin 一行之后（注意以冒号分隔），保存文件并退出，执行 source ~/.bash_profile 使其生效，这种方法只对当前登陆用户生效。

![](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202307191103337.png)

看到如下图便可知环境变量加入成功：

![](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202307191103449.png)

方法三（对所有系统用户生效，永久生效）

修改 /etc/profile 文件，在文件末尾加上如下两行代码
PATH=$PATH:/usr/local/MATLAB/R2013a/bin
export PATH

最后执行命令 source /etc/profile 或执行点命令 ./profile 使其修改生效。

以上便是自己总结的三个在 linux 下添加环境变量的方法，可根据需求进行选择，事半功倍。


————————————————
版权声明：本文为CSDN博主「打工人小飞」的原创文章，遵循CC 4.0 BY-SA版权协议，转载请附上原文出处链接及本声明。
原文链接：https://blog.csdn.net/huangfei711/article/details/53044539