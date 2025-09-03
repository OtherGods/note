之前在使用redis的case中，更多的只是单机的使用；随着业务的增长，为了更好的性能提供，集群是一个必然的发展趋势；下面记录一下搭建集群的步骤

> 单机安装手册，可以查看： [单机redis安装手册](https://blog.hhui.top/hexblog/2018/04/24/redis%E5%AE%89%E8%A3%85/)

# 1、redis集群搭建过程

## 1.1、获取项目并编译

首先是从官网获取最新稳定版的redis包，官网友链 -> [https://redis.io/](https://redis.io/)

```shell
# 下载redis包  
wget http://download.redis.io/releases/redis-5.0.5.tar.gz  
tar -zxvf redis-5.0.5  
  
# 开始编译  
make  
make test
```

通过上面执行完毕之后，在src目录下，会生成常见的操作命令，如`redis-cli` `redis-server`

## 1.2、开始配置

在redis目录下，配置文件`redis.conf`是我们需要关注的目标

我们这里在本机搭建三个节点，对应的端口号分别为7000, 7001, 7002

接下来，进入配置文件，进行修改
```shell
mkdir -p data/7000 data/7001 data/7002 log/7000 log/7001 log/7002   
  
# 下面的配置，一次操作三遍，分别获得r7000.conf r7001.conf r7002.conf  
cp redis.conf r7000.conf  
vim r7000.conf  
  
## 下面是我们需要修改的地方  
pidfile /var/run/redis_7000.pid # pid进程文件  
# 日志和数据存储路径  
logfile "/home/yihui/redis/log/7000/redis.log"  
dir "/home/yihui/redis/data/7000/"  

============== 基本配置 ==============
# 端口号
port 7000
# 后台启动
daemonize yes  
# 关闭保护模式，允许远程连接
protected-mode no
# 确保 Redis 绑定到正确的网络接口。通常情况下，应该包含服务器的 IP 地址
bind 0.0.0.0 127.0.0.1

============== 集群配置 ==============
# 开启集群
cluster-enabled yes
# 为每个实例指定唯一的集群配置文件
cluster-config-file nodes-7000.conf
# 设置节点超时时间（可选）
cluster-node-timeout 5000

# 通过公网地址连接 Redis 集群时，Redis 返回了私网地址，导致连接失败。这通常是因为 Redis 节点在集群配置中报告了其私网地址，而外部客户端无法通过该地址访问，要解决这个问题，需要确保 Redis 节点报告其公网地址而不是私网地址。这可以通过配置 `redis.conf` 文件中的 `cluster-announce-ip` 参数来实现
cluster-announce-ip 47.93.184.165 # 公网地址 
cluster-announce-port 7000 # 对应节点的实际端口 
cluster-announce-bus-port 17000 # 对应的集群总线端口，主端口+10000
```

## 1.3、启动并设置集群

上面设置完毕之后，开始启动redis
```shell
src/redis-server r7000.conf  
src/redis-server r7001.conf  
src/redis-server r7002.conf
```

启动完毕之后，可以查看到如下的进程
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202405142244888.png)

到这里，集群还没有设置完成，还需要通过`redis-cli`设置一下集群关系
```shell
redis/src/redis-cli --cluster create 127.0.0.1:7000 127.0.0.1:7001 127.0.0.1:7002 --cluster-replicas 1
```
> ChatGPT解释该命令：
> `redis-cli --cluster create` 命令用于创建Redis集群。下面对命令的各个部分进行详细解释：
> 
> - `redis/src/redis-cli`: 这是Redis命令行客户端的路径。如果你已经安装了Redis，`redis-cli` 应该可以直接从命令行调用，而不需要指定路径。
> - `--cluster create`: 指定`redis-cli`要执行的操作是创建一个Redis集群。
> - `127.0.0.1:7000 127.0.0.1:7001 127.0.0.1:7002`: 这是要创建集群的节点列表。在这个例子中，指定了三个Redis实例，分别运行在本地主机（127.0.0.1）的端口7000、7001和7002上。
> - `--cluster-replicas 1`: 指定每个主节点的从节点（副本）数量。在这个例子中，每个主节点会有一个从节点。也就是说，如果你有三个主节点，那么会有三个从节点，总共有六个节点。
> 

执行上面的命名，发现并不能成功，提示如下
```shell
*** ERROR: Invalid configuration for cluster creation.  
*** Redis Cluster requires at least 3 master nodes.  
*** This is not possible with 3 nodes and 1 replicas per node.  
*** At least 6 nodes are required.
```

上面表示redis集群必须有三个主节点，当我们设置主从时，最少需要六个节点；当然我们在本机测试的时候，搞六个必要性不大，这里直接不要从节点
```shell
redis/src/redis-cli --cluster create 127.0.0.1:7000 127.0.0.1:7001 127.0.0.1:7002
```

执行上面命令并确认之后，redis集群基本上就搭建完毕

## 1.4、测试

### 1.4.1、redis集群节点测试

**集群间网络测试**：
- **IP 测试**：使用 `ping` 检查各节点之间的连接：
    `ping 47.93.184.165`
- **端口测试**：使用 `telnet` 或 `nc` 检查 Redis 端口是否可达：
    `telnet 47.93.184.165 7000`

**验证集群状态**
确保所有节点都已正确加入并处于健康状态。可以通过以下命令查看集群节点的状态：
```shell
redis-cli -h 47.93.184.165 -p 7000 cluster nodes
```

**确保集群节点间可相互通信**
```shell
# 从7000节点检查连接到7001和7002节点 
redis-cli -h 47.93.184.165 -p 7000 cluster meet 47.93.184.165 7001
redis-cli -h 47.93.184.165 -p 7000 cluster meet 47.93.184.165 7002
```


### 1.4.2、redis集群测试

借助`redis-cli`进行集群的连接和测试
```shell
redis/src/redis-cli -c -p 7000  
127.0.0.1:7000> cluster nodes  
e1bd930c0b6f42da4af18f5aca551fd26d769330 127.0.0.1:7001@17001 master - 0 1569411511851 2 connected 5461-10922  
7b8b9ea9feab9dc1c052c4a6215f211c25776e38 127.0.0.1:7002@17002 master - 0 1569411512853 3 connected 10923-16383  
d7b8d578eedf9d1148009b6930e5da6bdbd90661 127.0.0.1:7000@17000 myself,master - 0 1569411512000 1 connected 0-5460  
127.0.0.1:7000> set test 123  
-> Redirected to slot [6918] located at 127.0.0.1:7001  
OK  
127.0.0.1:7001> set test2 1342  
OK  
127.0.0.1:7001> set test3 123  
-> Redirected to slot [13026] located at 127.0.0.1:7002  
OK  
127.0.0.1:7002> set test1 123  
-> Redirected to slot [4768] located at 127.0.0.1:7000  
OK  
127.0.0.1:7000> keys *  
1) "test1"  
127.0.0.1:7000>
```

通过`keys`命令查看，我们上面设置的几个值分布在三个实例上了


# 2、ChatGPT
## 2.1、redis客户端参数

**基本参数**
- `-h` 或 `--host`：指定 Redis 服务器的主机名或 IP 地址（默认是 `127.0.0.1`）。
    `redis-cli -h <hostname>`
- `-p` 或 `--port`：指定 Redis 服务器的端口号（默认是 `6379`）。
    `redis-cli -p <port>`
- `-a` 或 `--pass`：指定 Redis 服务器的密码（如果启用了密码保护）。
    `redis-cli -a <password>`
- `-u` 或 `--uri`：指定 Redis 服务器的 URI。
    `redis-cli -u redis://<username>:<password>@<hostname>:<port>/<database>`
- `-n` 或 `--dbnum`：指定数据库编号（默认是 `0`）。
    `redis-cli -n <database-number>`
- `-c` 或 `--cluster`：启用集群模式，这样客户端可以处理 `MOVED` 和 `ASK` 重定向。
    `redis-cli -c`
- `--raw`：以原始格式输出结果。
    `redis-cli --raw`
- `--csv`：以 CSV 格式输出结果。
    `redis-cli --csv`
- `-r` 或 `--repeat`：指定命令重复执行的次数。
    `redis-cli -r <count>`
- `-i` 或 `--interval`：指定每次重复命令之间的间隔（以秒为单位，可以是小数）。
    `redis-cli -r <count> -i <interval>`
- `-x`：从标准输入读取数据作为最后一个参数的值。
    `echo "myvalue" | redis-cli -x set mykey`
- `-d` 或 `--delimiter`：指定输出字段的分隔符（默认是空格）。
    `redis-cli --delimiter ";"`
- `--pipe`：以管道模式操作，用于批量处理命令。
    `redis-cli --pipe < commands.txt`

**集群相关参数**
- `--cluster create`：创建 Redis 集群。
    `redis-cli --cluster create <node1> <node2> <node3> --cluster-replicas <replicas>`
- `--cluster add-node`：向现有集群添加新节点。
    `redis-cli --cluster add-node <new-node> <existing-node>`
- `--cluster del-node`：从集群中删除节点。
    `redis-cli --cluster del-node <node> <node-id>`
- `--cluster info`：获取集群信息。
    `redis-cli --cluster info <node>`
- `--cluster check`：检查集群节点的健康状态。
    `redis-cli --cluster check <node>`
- `--cluster fix`：尝试修复集群状态。
    `redis-cli --cluster fix <node>`
- `--cluster reshard`：重新分配集群槽。
    `redis-cli --cluster reshard <node>`
- `--cluster rebalance`：重新平衡集群槽分配。
    `redis-cli --cluster rebalance <node>`

**示例命令**
1. **连接到 Redis 服务器**：
    `redis-cli -h 127.0.0.1 -p 6379`
2. **设置键值对**：
    `redis-cli set mykey "Hello, Redis!"`
3. **获取键值对**：
    `redis-cli get mykey`
4. **创建 Redis 集群**：
    `redis-cli --cluster create 47.93.184.165:7000 47.93.184.165:7001 47.93.184.165:7002 --cluster-replicas 1`

## 2.2、redis集群节点问题

Redis 集群通信使用的实际端口是在 16384 到 16383 + N 范围内的端口，其中 N 是你配置的 Redis 实例的端口号。例如，**如果 Redis 实例使用的端口号是 7000，那么集群消息将使用 7000 + 10000 = 17000 端口进行通信。**

**每个 Redis 节点除了使用其主端口（例如 7000, 7001, 7002）外，还使用其主端口加上 10000 的端口进行内部集群通信。因此，需要确保这些端口也被开放。**

**具体来说，对于redis服务器端口 7000, 7001, 7002 的 Redis 实例，集群通信端口分别是 17000, 17001, 17002。**


转载自：[190925-Redis集群搭建手册](https://blog.hhui.top/hexblog/2019/09/25/190925-Redis%E9%9B%86%E7%BE%A4%E6%90%AD%E5%BB%BA%E6%89%8B%E5%86%8C/)


