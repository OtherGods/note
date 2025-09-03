
大家好，我是二哥呀。今天我来带大家体验一下技术派是如何通过 ngrok 完成在微信浏览器访问本地 127.0.0.1 服务的，这对于那些想通过手机端访问本地服务的小伙伴来说，可能非常重要。

大家都知道，手机上的网络和本地服务之间的网络是互不相同的，但有了内网穿透 ngrok 后，就可以联得通，他们之间的桥梁就建立了起来。
![1.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410032151381.png)

# 1、ngrok的安装和配置

Ngrok 是一个非常流行的内网穿透工具，可以将本地服务暴露到公共的互联网上。它提供了一个临时的公共 URL，在任何设备上都可以访问得到，这样就可以来测试我们的本地项目了。

> 官方网址：https://ngrok.com/

第一步，安装 ngrok

我用的 macOS，可以通过 [brew 安装](https://javabetter.cn/gongju/brew.html)（不了解的小伙伴可以戳链接了解），命令如下：
```shell
brew install ngrok/ngrok/ngrok
```

如果无法安装的话，可以下载 [zip 安装包](https://dashboard.ngrok.com/get-started/setup/macos)，网站会根据你的操作系统为你推荐对应的安装包链接。
![1.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410032156812.png)

然后在本地通过 unzip 命令解压到 bin 目录。
![1.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410032156780.png)

第二步，获取授权码并配置

通过这个链接[https://dashboard.ngrok.com/get-started/your-authtoken](https://dashboard.ngrok.com/get-started/your-authtoken) 获取到你的授权码。

然后通过 ngrok config add-authtoken 配置授权码。
![1.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410032157063.png)

第三步，通过 ngrok http 8080 启动 ngrok，注意这里的 8080 端口要替换为你实际的服务运行端口。
![1.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410032158026.png)

我本地技术派用的端口正是 8080。
![1.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410032158808.png)

OK，https://7803-178-128-26-168.ngrok-free.app 是 ngrok 给我暴漏出来的地址，我们来访问试一下，OK，确认是可以的。
![1.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410032159114.png)

放在微信浏览器试一下，会有一个提示，点击「visit site」就好。
![1.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410032200986.png)

确认也是可以的。
![1.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410032200776.png)

同时确认一下手机端的微信，也是 OK 的。
![1.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410032201842.png)

# 2、ngrok的工作原理

其实说起来也很简单，主要有下面这几个步骤：

第一步，当我们通过 ngrok http 8080 启动 ngrok 客户端后，就会与 ngrok 的服务器端建立一个长连接，这个长连接里有我们之前的授权码，可以保证通信是安全的。

第二步，通过这个长连接，ngrok 帮我们创建了一个公共访问的 URL，这个 URL 与本地的服务端口，也就是 127.0.0.01:8080 端口相关联，算是一个“隧道”。

第三步，当在浏览器访问这个 URL 的时候，ngrok 服务器会接收到这个请求，然后通过长连接转发到本地服务上。

第四步，本地服务器接收到了 ngrok 的请求，就像处理普通的本地请求一样，然后响应会先传回 ngrok 服务器，然后再相应给最初发起的请求。

整个过程，其实也很好理解，ngrok 帮我们建立了一个中转站，所有的请求和响应都经过了一次 ngrok。

所谓的内网穿透，其实就是这么一种技术，它允许外部网络（如互联网）访问位于内部网络（如本地或公司局域网）中的服务器或设备。

尤其是当我们需要在手机端对本地服务进行测试时，会非常方便。

# 3、技术派如何利用ngrok修复手机端微信不刷新的bug

那在 PC 端，技术派的首页是可以下拉刷新的，但到了手机端的微信浏览器中，就无法完成下拉刷新，要解决这个 bug，就需要利用 ngrok 来完成。

因为手机端的微信浏览器没有调试模式，我们只能通过肉眼的去判断问题的发生位置，然后去猜问题的发生原因，然后再在本地尝试我们设想的解决方案，然后再来解决问题。

首先来看下拉刷新的前端代码，我们主要是利用 scrollTop、clientHeight、scrollHeight 这三个高度之间的差距来进行判断是否要刷新。

代码在 loadMore.js 这个文件当中。逻辑也非常简单，当 `crollTop + windowHeight + 100 >= scrollHeight` 时，就触发下拉刷新的动作，去请求下一页的数据。
```js
/**
 * 触底加载下一页的通用方法
 * @param loadMoreSelector 选择器
 * @param url 向后端请求下一页的url
 * @param params 传参
 * @param listId 存放下一页内容的标签id
 * @param callback 回调函数
 */
const loadMore = function (loadMoreSelector, url, params, listId, callback) {
  let lastReqCondition = "" // 上一次请求条件
  let isNeedMore = true // 是否需要加载更多的标志

  // 滚动事件处理函数
  const handleScroll = () => {
    const scrollEle = document.querySelector("html") // 获取滚动元素

    const scrollTop = scrollEle.scrollTop // 已滚动的距离
    const windowHeight = scrollEle.clientHeight // 可视区域的高度
    const scrollHeight = scrollEle.scrollHeight // 滚动条的总高度

    if (!isNeedMore) return false // 如果不需要加载更多，直接返回

    // 当滚动到底部时触发加载
    if (scrollTop + windowHeight + 100 >= scrollHeight) {
      // 生成本次请求的条件字符串
      let newReqCondition = params["category"] + "_" + params["page"]
      if (newReqCondition === lastReqCondition) {
        // 如果本次请求条件与上次相同，则不重复请求
        return
      }
      lastReqCondition = newReqCondition // 更新最后一次请求条件

      // 请求下一页数据
      nextPageText(
          url,
          params,
          listId,
          (hasMore) => {
            if (!hasMore) {
              isNeedMore = false // 更新是否需要加载更多的标志
            }
            params["page"] = params["page"] + 1 // 更新请求参数中的页码
            if (callback) {
              callback() // 执行回调函数
            }
          },
          () => {
            lastReqCondition = "" // 请求失败时重置最后一次请求条件
          }
      )
    }
  }
  window.addEventListener("scroll", handleScroll, true) // 添加滚动事件监听器
}
```

我的初步判断，手机端微信浏览器之所以无法下拉刷新，就是因为 100 这个变量太小，导致无法触发。

由于手机端没办法直接 console.log，所以我这里直接用 alert 打印一下这三个变量的值。
```shell
alert(isNeedMore + " " + scrollTop + " " + windowHeight + " " + scrollHeight);
```

保存代码后直接点击 build，技术派配置了热部署，所以在浏览器只要刷新就可以立即看到效果。
![1.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410032202821.png)

我们先在 PC 端看一下数值。windowHeight 为 825，scrollHeight为 1110，加上滚动条底部的这段距离（估计差不多 300），100 的间隔差不多是可以触发的（825+300+100>1110）。
![1.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410032203604.png)

再来看一下手机端微信浏览器的数值（786 < 825 < 1108），大概就能确定这个 100 的阈值是不够的。
![1.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410032203896.png)

我们加大到 1108-786=322，我们直接加大到 350 来试一下。
![1.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410032204573.png)

好，确定可以，滑动到底部了。
![1.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410032204224.png)

OK，我们直接这样来改。
```js
// 定义一个检测是否为微信浏览器的函数
const isWeixinBrowser = () => {
  const userAgent = navigator.userAgent.toLowerCase();
  console.log("userAgent", userAgent);
  return userAgent.includes('micromessenger');
}

// 滚动事件处理函数
const handleScroll = () => {
const scrollEle = document.querySelector("html") // 获取滚动元素

const scrollTop = scrollEle.scrollTop // 已滚动的距离
const windowHeight = scrollEle.clientHeight // 可视区域的高度
const scrollHeight = scrollEle.scrollHeight // 滚动条的总高度

if (!isNeedMore) return false // 如果不需要加载更多，直接返回

// 当滚动到底部时触发加载
const triggerThreshold = isWeixin
```

通过检测用户代理（User Agent）字符串来判断当前网页是否在微信浏览器中打开，微信浏览器的用户代理中通常会包含特定的标识符，比如 MicroMessenger。

确认 OK，问题轻松解决。不过随后我又发现，只能翻一页，就翻不了，看来还有问题我没发现。

再加上 alert 真的测起来很痛苦（会阻塞前端动作），于是我就在服务端加了一个 testloadmore 的测试接口。
```java
// 前端把一些数据发送到这里并打印出来
@PostMapping(path = "loadmore")
public void testLoadMore(@RequestBody String loadmore) {
    log.info("loadmore: {}", loadmore);
}
```

然后再把前端的数据传递过来。
```js
post("/test/loadmore", {
            scrollTop: scrollTop,
            windowHeight: windowHeight,
              isNeedMore: isNeedMore,
            scrollHeight: scrollHeight
        }, (res) => {
            console.log(res)
        })
```

这样不就可以在控制台实时看到手机端微信下拉的数据了吗？
![1.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410032206335.png)

最后发现问题竟然出在 scrollEle.scrollTop 上，在手机微信上，该数值一直是 0，那还有没有其他方法可以获取到滚动的距离呢？

有。

```js
const scrollTop =  window.pageYOffset || scrollEle.scrollTop  || document.body.scrollTop // 已滚动的距离
```

有时候，滚动位置可能记录在 body 元素而非 documentElement，所以可以尝试使用 document.body.scrollTop 来获取滚动位置。

window.pageYOffset 也是一个标准的方式来获取垂直滚动的像素数，虽然已经被废弃。

最终，经过测试，这种方案确实有效。
![1.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410032207142.png)

代码已经提交到 GitHub，大家可以参考一下。

当然了，问题的解决，离不开 ngrok 的帮助，不然手机微信上根本没办法测试本地服务，只能在服务器上测试，那真的痛苦死。

# 4、小结

内网穿透对开发微信小程序或者以前的微信公众号开发会非常有用，记得 2016 年做公众号商城开发的时候，包括微信支付，都需要提前配置好内网穿透，否则就没办法在移动端完成测试。

虽然 Chrome 浏览器可以模拟手机端模式，但和真实的环境还是有差别，所以这个 ngrok 希望大家也都能掌握。

再总结一下常见的内网穿透方案，也是面试中挺常见的一道面试题。

## 4.1、端口映射（Port forwarding）

端口映射是一种将公网上的 IP 地址和端口映射到局域网内一台计算机的指定端口上的技术。通常是在路由器上进行配置，将外网特定端口的流量转发到内网特定的端口上。

## 4.2、反向代理（Reverse proxy）

反向代理是一种将公网上的访问请求转发到局域网内一台计算机的指定端口上的技术。实现反向代理需要在公网服务器上部署一个代理服务器，在代理服务器上配置反向代理规则，将公网请求转发到内网服务器上的指定端口。从而实现内网穿透。

ngrok 实际上就结合了反向代理和端口映射两种技术来实现内网穿透，当我们在本地机器上启动 ngrok，它会创建一个到 ngrok 服务器的反向代理连接，ngrok 服务器为这个链接提供一个可供公网访问的 URL。

当 ngrok 服务器接收到请求后，会将请求转发到反向代理连接上，然后 ngrok 客户端接收到 ngrok 服务器转发的请求后，会将它映射到本地的端口和服务上。

## 4.3、VPN（Virtual Private Network，虚拟专用网络）

VPN 是一种通过公用网络建立安全的、点对点连接的私人网络技术。VPN 可以让远程用户或外部网络通过加密的方式连接到内部网络，实现内网穿透。

VPN 在用户的设备和 VPN 服务器之间创建一个加密的隧道。通过这个隧道，用户的所有网络流量都会被加密并通过 VPN 服务器路由。

注意 VPN 和反向代理之间的差别，一个是从外部访问到内部，一个是从内部访问到外部。

## 4.4、NAT 穿透（NAT Traversal）

NAT 穿透是一种穿过网络地址转换（NAT）设备连接内部网络的技术。主要利用特定的协议（TCP、UDP）和技术（如STUN、TURN、ICE），实现在 NAT 后的设备之间的直接通信。

主要用于P2P通信，如VoIP、在线游戏、视频会议等。

在 NAT 网络中，内网的 IP 地址和端口号经过 NAT 转换后，对外部网络是不可见的。因此，当外部网络需要连接内网中的计算机时，需要通过一定的方式绕过 NAT 转换，使得内网计算机可以直接和外网通信。

NAT本质上是一种映射机制，将内部私有网络地址映射到外部公共IP地址上。

UDP穿透相对较简单，因为它不依赖于会话状态，通过让两个位于NAT后的设备分别向外部公网服务器发送数据包，使得NAT设备建立映射条目。之后，这两个设备可以直接互发数据包，因为NAT设备会根据已建立的映射转发这些数据包。

TCP Hole Punching 是一种通过 TCP 协议建立连接的 NAT Traversal 技术，它利用 TCP 三次握手建立连接的特点，通过同时向 NAT 路由器发送数据包，使得 NAT 路由器在建立连接时将数据包转发到对应的内网计算机上，从而实现 NAT 穿透。


