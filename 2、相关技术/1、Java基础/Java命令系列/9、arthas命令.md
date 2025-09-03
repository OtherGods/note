- 官网地址：
- https://alibaba.github.io/arthas/index.html

这里只写了常用命令，详细信息看官网

# 帮助信息
```shell
java -jar D:\arthas\arthas-3.1.1-bin\arthas-boot.jar -h
```

# JVM参数
```shell
java -jar D:\arthas\arthas-3.1.1-bin\arthas-boot.jar -v
```

# 进入arthas

## 查看thread、内存、GC、参数等信息

示例：每间隔五秒打印一次内存、线程等信息，共打印五次
```shell
dashboard -i 5000 -n 5
```

## 查看线程信息

```shell
thread
```
1. `-n`：指定最忙的前 N 个线程并打印堆栈，示例：打印当前最忙的三个线程`thread -n 3`
2. `-b`：找出当前阻塞其他线程的线程，示例：`thread -b`
3. `-i`：指定 cpu 使用率统计的采样间隔，单位为毫秒，默认值为 200，示例：`thread -i 500`
4. `id`：打印指定id的线程

打印线程堆栈信息
```shell
thread 1
```
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202508061535937.png)

## class及classloader相关指令实操

### 反编译线上代码

反编译线上ID等于1的线程的`Main Class`（堆栈最底层类）
```shell
jad demo.MathGame
```

可以代替sc、sm

### 在线编译Java文件

编译Test文件，可能会失败，通常不用这个命令
```
mc /tmp/Test.java
```

### 热更新代码

线上紧急bug，可以直接替换字节码文件，即使arthas退出也不受影响

示例：在本地修复Test类中的bug，编译好后，将Test的字节码文件上传到服务器，在服务器执行替换字节码为最新文件：
```
redefine /源码位置/Test.class
```

注：在windows10中字节码文件位置中分隔符为`/`

#### 测试

可以用`mima-jvm-server`项目测试：
```shell
redefine D:/temp/mima-jvm-server/target/classes/com/mimaxueyuan/jvm/controller/MainController.class
```

#### 上传 .class 文件到服务器的技巧

使用`mc`命令来编译`jad`的反编译的代码有可能失败。可以在本地修改代码，编译好后再上传到服务器上。有的服务器不允许直接上传文件，可以使用`base64`命令来绕过。

1. 在本地先转换`.class`文件为 base64，再保存为 result.txt
    ```
    base64 < Test.class > result.txt
    ```
2. 到服务器上，新建并编辑`result.txt`，复制本地的内容，粘贴再保存
3. 把服务器上的 `result.txt`还原为`.class`
    ```
    base64 -d < result.txt > Test.class
    ```
4. 用 md5 命令计算哈希值，校验是否一致

#### 限制

- 不允许新增加 field/method
- 正在跑的函数，没有退出不能生效，比如没跑完的循环

## 对线上代码运行进行监控

`watch`、`stack`、`trace`、`stack` 这个四个命令都支持：
- `#cost`：在命令中用`'#cost>200'`过滤时长大于200毫秒的调用
- `-n`：监控/记录多少次

这几个命令都是实时的；在显示生产环境中想要触发这几个arthas的监控去查看controller类中的接口情况，需要实时调用`curl ip:port/path`去触发

### monitor

**==打印被监控的方法单位时间内（`-c`参数指定）被调用时的平均响应时长、被调用频次、成功个数、失败个数、成功率等==**

示例：每1秒打印一次类`demo.MathGame`中方法`primeFactors`的监控，只监控两次
```shell
monitor -c 1 demo.MathGame primeFactors -n 2
```

#### 测试

可以用`mima-jvm-server`项目测试：
```shell
monitor -c 1 -n 3 com.mimaxueyuan.jvm.controller.MainController getAllUsers
```

### watch

**==打印被监控的方法被调用时方法入参、返回值、当前对象信息、耗时等情况==**

示例：打印类`demo.MathGame`中方法`primeFactors`的监控(入参、出参、当前对象)，展示入参、出参的深度为3，只监控两次
```shell
watch demo.MathGame primeFactors "{params,returnObj,target}" -x 3 -n 2
```
注：`"{params,returnObj,target}"` 是ognl表达式
- `params`：入参
- `returnObj`：返回值
- `target`：当前对象（示例中是`MathGame`的对象），可以使用`target.field_name`展示当前对象的某个属性

通常可以配合以下参数：

| 参数名称   | 参数说明                                 |
| ------ | ------------------------------------ |
| `[b]`  | 在**函数调用之前**观察                        |
| `[e]`  | 在**函数异常之后**观察                        |
| `[s]`  | 在**函数返回之后**观察                        |
| `[f]`  | 在**函数结束之后**(正常返回和异常返回)观察（默认）         |
| `[E]`  | 开启正则表达式匹配，默认为通配符匹配                   |
| `[x:]` | 指定输出结果的属性遍历深度，默认为 1，最大值是 4，值越大性能损耗越大 |
**如果想要观察方法的入参，直接使用`-b`**，其他参数展示的是方法执行后的结果，可能在方法执行过程中修改入参的值

还可以配合条件表达式：
```shell
watch demo.MathGame primeFactors '{params, returnObj}' '#cost>200' -x 2
```

#### 测试

可以用`mima-jvm-server`项目测试：
```shell
watch com.mimaxueyuan.jvm.controller.MainController getAllUsers "{params,returnObj}" -x 1  -n 3
```

### tt

类似于`watch`，但会将信息记录下来，会保存到一个Map中，默认大小100，使用完后需要我们手动释放内存（退出arthas也不会自动释放），不然会OOM，

示例：
```shell
tt -t -n 3
```

1. `-t`：表明希望记录下某个方法每次执行的情况
2. `-n`：表示希望记录多少次

记录结果中包含每次被调用的索引值，方便后续查看，可以使用`-l`查看Map中记录了哪些数据
```shell
tt -l
```
- `-l`：展示Map记录的数据

示例：Map中可能包含很多方法调用的记录，在Map中查询某个具体的方法
```shell
tt -s 'method.name=="primeFactors"'
```
- `-s`：对`-l`输出的列进行过滤

示例：可以使用`-i`指定索引，查看某次调用的详情，指定深度为3
```shell
tt -i 1111 -x 3
```
- `-i`：展示某索引对应记录的详细信息：时间戳、耗时、入参、返回值、

示例：对Map中记录的某条索引对应的调用重新发起调用，重新调用10次，每次间隔1.5秒
```shell
tt -i 1111 -p --replay-times 10 --replay-interval 1500
```
- `-p`：表示重新调用
- `--replay-times`：指定重新调用次数
- `--replay-interval`：指定多次调用间隔时间，默认1000毫秒

示例：**==清除所有的 tt 记录==**
```shell
tt --delete-all
```

#### 测试

可以用`mima-jvm-server`项目测试：
```shell
tt -t -n 3 com.mimaxueyuan.jvm.controller.MainController getAllUsers
tt -t -n 3 com.mimaxueyuan.jvm.controller.MainController addNewUser
```

### trace

**==打印当前被监控的方法内部调用了哪些方法，以及这些方法的耗时==**，耗时最长的会被标识出来；同时会打印出当前的线程信息

示例：打印类`demo.MathGame`中`run`方法执行耗时超过200毫秒的调用，只监控两次
```shell
trace demo.MathGame run '#cost>200' -n 2
```

#### 测试

可以用`mima-jvm-server`项目测试：
```shell
trace com.mimaxueyuan.jvm.controller.MainController getAllUsers '#cost>200'  -n 3
```

### stack

**==打印当前被监控的方法正在被哪些地方调用==**；同时会打印出当前的线程信息

示例：打印类`demo.MathGame`中`primeFactors`方法当前正在被哪里调用，且调用时长超过200毫秒，只监控两次
```shell
stack demo.MathGame primeFactors '#cost>200' -n 2
```

#### 测试

可以用`mima-jvm-server`项目测试：
```shell
stack com.mimaxueyuan.jvm.dao.UserRepository findAll '#cost>1'  -n 3
```

## 退出

如果只是退出当前的连接，可以用`quit`或者`exit`命令。Attach 到目标进程上的 arthas 还会继续运行，端口会保持开放，下次连接时可以直接连接上。

如果想完全退出 arthas，可以执行`shutdown`、`stop`命令。


## 其他

- 上述命令可以使用 `>>` 将输出保存到磁盘某位置
  示例：`tt -t -n 3 com.mimaxueyuan.jvm.controller.MainController getAllUsers >> D:/temp/2/1.log`
  - `>`：将标准信息重定向到指定文件（覆盖）
  - `>>`：同`>`（追加）
  - `2>`：将错误信息重定向指定文件（覆盖）
  - `2>>`：同`2>`（追加）
- 上述输出可以配合 `grep` 进行过滤，类似linux
- 监控的命令可以在结尾加 `&` 放在后台运行，可以通过 `jobs` 查看后台运行的任务
- `kill jobid`可以关闭 `jobs` 中列出来job