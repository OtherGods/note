#序列化 #反序列化 #Serializable  #transient #wirteObject #readObject #readResolve #serialVersionUID #Java序列化原理 #Java反序列化 

# Java 序列化和反序列化的底层原理

#### 序列化和反序列化

序列化是通过某种算法<font color="red" size=5>将存储于内存中的<font color="blue" size=5>对象转换成可传输格式</font>，可以用于持久化存储或者通信的形式的过程</font>

反序列化是<font color="red" size=5>将这种被持久化存储或者通信的数据通过对应解析算法<font color="blue" size=5>还原成对象</font>的过程，它是序列化的逆向操作</font>

#### 为什么需要序列化

**==前端请求后端接口数据==的时候，后端需要返回 `JSON` 数据，这就是==后端将 `Java` 堆中的对象序列化为了 `JSON` 数据传给前端==，前端可以根据自身需求直接使用或者将其==反序列化为 JS 对象==**

RPC 远程调用过程中，**调用者**和**被调用者**必须约定好序列化和反序列化算法，比如 A 应用将 `User` 对象序列化为了 `JSON` 数据传给 B 应用，`User` 对象数据为 `{"id": 1, "name": "long"}`，到达 B 应用的时候需要将这些数据反序列化为对象，如果此时 B 应用的反序列化算法是 `XML` 的话那么肯定就解析失败了，所以必须都得约定好他们都采用 `JSON` 序列化算法，那么基于 `JSON` 标准就能成功解析出 `User` 对象

#### Java 中的序列化

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202508132358070.png)

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202508132359323.png)

##### transient

参照：[4、Java transient关键字使用](2、相关技术/1、Java基础/4、Java%20transient关键字使用.md)

如果某个字段我们不想通过 Java 默认序列化机制输出，我们就可以通过该字段来表明当前字段不需要被序列化
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202508132359210.png)

##### writeObject、readObject

序列化反序列化`transient`字段的方式：[3. transient使用细节——被transient关键字修饰的变量真的不能被序列化吗？](4、Java%20transient关键字使用#3.%20transient使用细节——被transient关键字修饰的变量真的不能被序列化吗？)

我们想通过自定义的方式将 `address` 数据序列化
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202508140000029.png)

其中的 
1. `writeObject` 作用于**写序列化数据的时候会反射调用该方法**，**==必须是私有的方法==**
2. `readObject` 会在**反序列化的时候调用**，**==必须是私有的方法==**
3. `writeObject`、`readObject`想要生效的前提是这两个方法都是私有的
4. 如果两个方法`writeObject`、`readObject`都是空的，那么序列化后反序列化得到的对象就是空对象（对象中字段为`null`）；

##### readResolve

[避免反序列化破坏单例](如何破坏单例模式？#避免反序列化破坏单例)

##### serialVersionUID 的作用

当实现`Serializable`接口的类，**==如果显示定义了 `serialVersionUID` ，就只能手动修改它的值==**；如果 **==没有显示定义==** 这个 `serialVersionUID` 的时候，Java序列化机制会**根据编译的Class==自动生成==一个 `serialVersionUID`（==类信息变化，自动生成的 `serialVersionUID` 会自动变化==）** 作序列化版本比较用，这种情况下:
1. 如果**Class文件没有发生变化**，就算再编译多次，**==不会自动修改这个UID==**
2. 如果**Class文件发生变化（比如：类字段增加或减少）**，那么这个文件对应的UID也会 **==自动发生变化==**；

**==虚拟机是否允许反序列化==，不仅取决于类路径和功能代码是否一致，==取决于两个类的序列化ID是否一致==，即serialVersionUID要求一致**：
1. 在进行反序列化时，JVM会把传来的字节流中的`serialVersionUID`与本地相应实体类的`serialVersionUID`进行比较，如果 **==相同就认为是一致的，可以进行反序列化==**，否则就会出现序列化版本不一致异常，即`InvalidCastException`，这样做是为了保证安全，因为文件存储的内容可能被篡改。

基于以上原理，如果将一个这样的类（没有显示定义`serialVersionUID`）的对象序列化到磁盘中【记作Q】，之后修改类的数据域，在程序中反序列化Q的时候会报错

###### 自定义 `serialVersionUID` 反序列化时可能存在的情况

背景：假设雇员类的最初版本（1.0）在磁盘上保存了大量的雇员记录（也就是将多个版本1.0的实例对象保存在文件中），现在我们在`Employee`类中添加了称为`department`的数据域，从而将其演化到了2.0版本：
1. 情况1、将1.0版本的序列化后的对象读入到2.0版本对象的程序中的
   
   可以看反序列化后得到的Employee对象的department域被设置成了null，因为序列化后的1.0版本中没有这个域对应的数据。![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202508141312329.png)

2. 情况2、将2.0版本的序列化后的对象读入到1.0版本对象的程序中
   
   可以看到反序列化后得到的Employee对象中没有department域对应的数据，因为1.0版本的Employee的程序中没有这个数据域。![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202508141312304.png)

#### Java 序列化的实现原理

> 最开始看代码的时候，不要一下就陷入全部细节，我们应该只看我们目前关注的点，当认识逐渐深刻之后再来看一些细节，不然的话容易看的一脸懵逼

首先调用`objectOutputStream.writeObject(user);` 然后调用 `writeObject0(obj, false);` 在这个方法里面有这样一段代码
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202508140004586.png)

**==`String`、`Enum` 都实现了 `Serializable` 接口==**；这里可以看到如果我们要序列化的是一个对象并且它没有实现 `Serializable` 接口的话就会直接抛出 `NotSerializableException`，由于我们目前传入的是 `User` 对象它实现了 `Serializable` 所以会进入到 `writeOrdinaryObject` 中
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202508140006314.png)

首先将 `TC_OBJECT` 这一个<font color="red" size=5>对象标志位(字节0x73)</font>写入到流中，<font color="red" size=5>标识着当前开始写一个的数据是一个对象</Font>，然后调用 `writeSerialData` 开始写入具体数据
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202508140019056.png)
1. 首先会获得 `ClassDataSlot` 我们可以把它看做是提供了序列化对象的辅助手段
2. 在此通过 `ClassDataSlot` 去检查序列化对象中是否实现了 `writeObject` 这个方法，那么这个 `writeObjectMethod` 是在什么时候初始化的呢？马上会讲到
   ![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202508140024958.png)
3. 如果实现了 `writeObject` 我们就去反射调用该方法
4. 如果当前类没有实现 `writeObject` 方法就调用默认的 `defaultWriteFields` 去写数据

##### 如何知道对象是否实现了 writeObject(ObjectOutputStream out) 和 readObject

在上文我们知道是通过 `writeObjectMethod` 这个来判断的，那么这个字段是在哪里初始化的呢，我们回到 `ObjectOutputStream` 的 `writeObject0` 方法，在调用后续的 `writeOrderinaryObject` 方法之前有这样一段代码
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202508140026950.png)

然后会调用到这段代码
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202508140026968.png)

在创建 `ObjectStreamClass` 对象的过程中会通过反射去拿到当前类的方法，然后根据方法名 `writeObject` 和参数 `ObjectOutputStream` 去判断有没有这个方法，有的话就返回没有就返回为 null
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202508140028009.png)

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202508140028217.png)

然后我们回到 `defaultWriteFields(Object obj, ObjectStreamClass desc)` 继续来看
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202508140028809.png)

1. 拿到当前需要写入数据的具体长度
2. 通过反射去获取当前数据的值，`ObjectStreamClass desc` 这个对象可能是个 `Object` 可能是基本类型等，此时拿到的是 `User` 对象，所以取到的值默认为空，因为这里只是写入它的具体字段的数据
3. 通过反射拿到当前对象的所有的值
   ![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202508140029717.png)
4. 挨个调用 `writeObject0` 写入具体的值，首先调用的是 `Integer` 由于它是一个包装类，`Integer` 继承了 `Number`，`Number` 类实现了 `Serializable` 所以会和 `User` 对象走一样的流程到达次数（可以自己 DEBUG 一下）然后拆包取出值调用
5. 随后就进入到了 `String` 的写入，再次调用 `writeObject0`，又到达 `ObjectOutputStream` 的 `writeObject0`，后续就有所区别了因为这里写入的具体类型是 `String`
   ![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202508140031354.png)

因为 `Integer` 也实现了 `Serializable` 并且这里没有针对他坐特殊处理，所以它会走 `writeOrdinaryObject`，而 `String` 这里判断了，需要去调用 `writeString`
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202508140032307.png)

这个方法也比较简单首先写入 String 标志位然后写入具体的数据和长度，其它的类型也是一样会走这里不同的分支，最终将数据写入到流中

##### 最后来看一下序列化后占用了多少个字节
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202508140043453.png)

#### Java 反序列化的原理

对照：[反序列化步骤总结](如何破坏单例模式？#反序列化步骤总结)

反序列化其实就是序列化的逆向过程，如果你看懂了序列化的关键代码，那么看这个过程就不会很难，下面贴出关键代码做出分析
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202508140055392.png)

这里能够看到会根据反序列对象的具体类型分别做不同的处理，我们当前的对象是 `User` 对象所以会进入箭头指向的方法`readOrdinaryObject`
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202508140058664.png)
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202508140058311.png)

在`readOrdinaryObject`方法中会去创建一个实例对象，其中 `isInstantiable` 这个方法是去判断构造器是否初始化了，调用`desc.newInstance()`通过 **反射 + Unsafe** 创建具体对象，同时这里还会将 `writeObject` 和 `readObject` 方法设置好，然后会在`readSerialData`方法中通过 `hasReadObjectMethod` 方法来确定是否实现了 `readObject` 方法如果实现了就反射调用 `readObject` 方法
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202508141648857.png)

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202508140104810.png)
在调用了 `readSerialData` 方法之后会调用 `defaultReadFields` 方法来设置字段的值，当前的 Obj 是 User 对象
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202508140104273.png)

1. 获取当前传入对象数据长度由于传入的是 User 空对象，所以此时长度为空
2. 反射获取到需要被反序列化的所有字段，并且创建对应的数组来保存对应的值，此时获取到 User 对象有 2 个字段 id 和 name
3. 然后开始递归调用 `readObject0` 处理完所有需要被反序列化的字段，就一当前的 id 和 name 举例
   - 和上文序列化一样 id 是 Integer 包装类所以会被识别为 Object 当再次到达这个方法的时候，在第一步 `primDataSize` 数据长度为 4 因为是 int 类型 4 个字节
   - 到第二步，因为是 Integer 没有其它需要被反序列化的字段它只有本身的拆包后的值，所以会到达第四步设置当前 id 的值
4. 设置当前基本类型字段的值

对于 String 类型来说，在反序列化第一张中会调用读取对应的值
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202508142027762.png)

#### 其它的序列化方式

一个新的技术的诞生都是有一定的原因和背景的，比如说 `Java 原生序列化`后**数据比较大**，**传输效率低**，同时又又**无法跨语言**通信，所以很多人选择使用 `XML` 的来序列化数据，`XML` 序列化后倒是**解决了跨语言通信**的问题，但是它**序列化后的数据比原生数据还要大**，所以就诞生了 `JSON` 序列化，他支持跨语言，并且**序列化后的数据远远小于前 2 者**，最后有人想进一步的优化大小就引入了 `Protobuf` 它具备 **压缩**的功能，被压缩的数据小于 `JSON` 序列化后的数据。

其它的序列化方式
- **Java原生序列化**：序列化后数据大，传输效率低、无法跨语言
- `XML`：解决了跨语言，但是序列化后的数据比原生序列化后的还大
- `JSON`：解决了跨语言，序列化后数据远小于`XML`和`Java原生序列化`
  - Jackson
  - FastJson
- Hessian
- thrift
- `protobuf`：具备压缩功能，进一步优化了序列化后的大小
- ...

转载自：[https://juejin.cn/post/6854573214077550600](https://juejin.cn/post/6854573214077550600)
