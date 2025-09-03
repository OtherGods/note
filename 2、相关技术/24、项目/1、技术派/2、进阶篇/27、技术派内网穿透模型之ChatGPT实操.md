
本文作为一个扩展篇，给大家介绍一下如何内网穿透，既可以是个人的内网穿透，也可以是程序代码的内网穿透。

# 1、搭建代理

下面的教程主要针对有海外服务器的小伙伴，如何搭建一个自己专属的代理

## 1.1、v2ray安装
### 1.1.1、服务器配置

对于v2ray不太熟的小伙伴，可以自行搜索，下面简单介绍下，如何在自己的服务器上安装v2ray，给自己搭建一个可以专属的跳板

> 以下内容来自 https://github.com/v2fly/fhs-install-v2ray/blob/master/README.zh-Hans-CN.md

安装命令
```
// 安装可执行文件和 .dat 数据文件
# bash <(curl -L https://raw.githubusercontent.com/v2fly/fhs-install-v2ray/master/install-release.sh)
```

一个简单的配置文件如下， 放在/usr/local/et/v2ray/config.json
```json
{
    "log": {
        "loglevel": "warning"
    },
    "routing": {
        "domainStrategy": "AsIs",
        "rules": [
            {
                "type": "field",
                "ip": [
                    "geoip:private"
                ],
                "outboundTag": "block"
            }
        ]
    },
    "inbounds": [
        {
            "listen": "0.0.0.0",
            "port": 1234,
            "protocol": "vmess",
            "settings": {
                "clients": [
                    {
                        "id": "7966c347-b5f5-46a0-b720-ef2d76e1836a"
                    }
                ]
            },
            "streamSettings": {
                "network": "tcp"
            }
        }
    ],
    "outbounds": [
        {
            "protocol": "freedom",
            "tag": "direct"
        },
        {
            "protocol": "blackhole",
            "tag": "block"
        }
    ]
}
```

关于配置文件相关，推荐看这个 https://github.com/v2fly/v2ray-examples
![1.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410031200485.png)

配置修改完之后，可以进行测试
```shell
/usr/local/bin/v2ray test --config /usr/local/etc/v2ray/config.json
```

安装完毕之后，执行service v2ray restart确保正常启动

其他相关命令如下
```shell
service v2ray restart | force-reload  |start|stop|status|reload
```

### 1.1.2、代理使用

我个人直接使用的[v2rayN](https://github.com/2dust/v2rayN)作为笔记本上的科学辅助工具，具体使用配置姿势直接看官方说明吧，比较简单

## 1.2、http代理服务器

上面介绍的更多的是给我们来使用的，对于经常写爬虫，对代理有诉求的小伙伴，可以考虑基于tinyproxy来搭建代理，构建以及使用起来相比起上面的更加简单

安装脚本
```shell
sudo yum install tinyproxy -y
```

调整基本配置，设置自定义的端口，接受的ip等
```shell
vim /etc/tinyproxy/tinyproxy.conf

# 端口
Port 18888

# 允许的ip，如果不配置allow，那么默认所有的ip都可以进来
Allow 127.0.0.1
```

服务启动关闭等命令
```shell
# 启动
systemctl start tinyproxy.service
# 重启
systemctl restart tinyproxy.service
# 关闭
systemctl stop tinyproxy.service
```

查看代理执行日志
```shell
tail -f /var/log/tinyproxy/tinyproxy.log
```

测试验证，则可以直接使用curl来测试
```shell
curl -x "127.0.0.1:18888" https://qifu-api.baidubce.com/ip/local/geo/v1/district
```

# 2、技术派的网络模型

前面介绍的适用于自己有海外服务器的场景，如果我们没有海外服务器，或者不想自建，毕竟有些折腾和麻烦；直接花点RMB，也一样可以见识到外面的风景

比如我用的[JustMySocks](https://justmysocks2.net/members/cart.php)，速度挺快，也很稳定，流量也不少，就是有点小贵
![1.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410031206260.png)

接下来介绍一个小技巧，让大家自用的梯子，来作为后台服务器的代理使用；以技术派当前访问ChatGPT的运行模式为例
![1.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410031206401.png)

## 2.1、命令行代理客户端

接下来我们看一下怎么把自己购买的梯子释放给自己的服务程序来使用，这里主要借助命令行的ShadowSocks来实现

安装命令如下 * [Linux 使用 Shadowsocks 设置教程 | Shadowsocks](https://shadowsockshelp.github.io/Shadowsocks/linux.html#%E5%91%BD%E4%BB%A4%E8%A1%8C%E5%AE%A2%E6%88%B7%E7%AB%AF)

Debian / Ubuntu:
```shell
apt-get install python-pip
pip install git+https://github.com/shadowsocks/shadowsocks.git@master
```

CentOS:
```shell
yum install python-setuptools && easy_install pip
pip install git+https://github.com/shadowsocks/shadowsocks.git@master
```

For CentOS 7, if you need AEAD ciphers, you need install libsodium
```shell
dnf install libsodium python34-pip
pip3 install  git+https://github.com/shadowsocks/shadowsocks.git@master
```

Linux distributions with snap:
```shell
snap install shadowsocks
```

创建一个 /etc/shadowsocks.json 文件，格式如下，如果是购买的上面的JustMySocket，就可以直接从账单信息中，后去下面的必要信息
```json
{
    "server":"服务器 IP 或是域名",
    "server_port":端口号,
    "local_address": "127.0.0.1",
    "local_port":1080,
    "password":"密码",
    "timeout":300,
    "method":"加密方式 (chacha20-ietf-poly1305 / aes-256-cfb)",
    "fast_open": false
}
```

启动命令
```shell
/usr/local/bin/sslocal -c /etc/shadowsocks.json -d start
```

校验方式
```shell
curl -x socks5://127.0.0.1:1080 https://qifu-api.baidubce.com/ip/local/geo/v1/district
```

## 2.2、技术派的代理配置使用

最后再简单给大家看一下，技术派中的代理使用姿势，设计了一个基础的代理管理工具，可以基于访问的host进行简单的路由
```java
public class ProxyCenter {

    /**
     * 记录每个source使用的proxy索引
     */
    private static final Cache<String, Integer> HOST_PROXY_INDEX = Caffeine.newBuilder().maximumSize(16).build();
    /**
     * proxy
     */
    private static List<ProxyProperties.ProxyType> PROXIES = new ArrayList<>();


    public static void initProxyPool(List<ProxyProperties.ProxyType> proxyTypes) {
        PROXIES = proxyTypes;
    }

    /**
     * get proxy
     *
     * @return
     */
    static ProxyProperties.ProxyType getProxy(String host) {
        Integer index = HOST_PROXY_INDEX.getIfPresent(host);
        if (index == null) {
            index = -1;
        }

        ++index;
        if (index >= PROXIES.size()) {
            index = 0;
        }
        HOST_PROXY_INDEX.put(host, index);
        return PROXIES.get(index);
    }

    public static Proxy loadProxy(String host) {
        ProxyProperties.ProxyType proxyType = getProxy(host);
        if (proxyType == null) {
            return null;
        }
        return new Proxy(proxyType.getType(), new InetSocketAddress(proxyType.getIp(), proxyType.getPort()));
    }
}
```

使用场景主要有两块：

ChatGPT的访问
![1.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410031501035.png)

http工具类的代理访问
![1.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410031502244.png)




