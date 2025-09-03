Spring Boot 早在 2.1.x 版本后就在 starter 中内置了 Lombok 依赖，Intellij IDEA 也早在 IDEA 2020.3 版本的时候内置了 Lombok 插件。为什么它们都要支持 Lombok 呢？Lombok 到底有啥牛皮的？今天我们就来补上这一课。

# 1、Lombok的自我介绍

Lombok 在官网是这样作自我介绍的：
> Project Lombok makes java a spicier language by adding 'handlers' that know how to build and compile simple, boilerplate-free, not-quite-java code.

大致的意思就是：Lombok 是个好类库，可以为 Java 代码添加一些“处理程序”，让其变得更简洁、更优雅。在我看来，Lombok 最大的好处就在于通过注解的形式来简化 Java 代码，简化到什么程度呢？

作为一名 Java 程序员，我相信你一定写过不少的 getter / setter，尽管可以借助 IDE 来自动生成，可一旦 Javabean 的属性很多，就免不了要产生大量的 getter / setter，这会让代码看起来不够简练，就像老太婆的裹脚布一样，又臭又长。

Lombok 可以通过注解的方式，在编译的时候自动为 Javabean 的属性生成 getter / setter，不仅如此，还可以生成构造方法、equals方法、hashCode方法，以及 toString方法。注意是在编译的时候哦，源码当中是没有 getter / setter 等等的。

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202307221050062.png)

瞧一瞧，源码看起来苗条多了，对不对？

# 2、集成Lombok

如果项目使用 Maven 构建的话，添加Lombok 的依赖就变得轻而易举了。

```yml
<dependency>
	<groupId>org.projectlombok</groupId>
	<artifactId>lombok</artifactId>
	<version>1.18.6</version>
	<scope>provided</scope>
</dependency>
```

其中 scope=provided，就说明 Lombok 只在编译阶段生效。也就是说，Lombok 会在编译期静悄悄地将带 Lombok 注解的源码文件正确编译为完整的 class 文件。

其中 scope=provided，就说明 Lombok 只在编译阶段生效。也就是说，Lombok 会在编译期静悄悄地将带 Lombok 注解的源码文件正确编译为完整的 class 文件。

SpringBoot 2.1.x 版本后不需要再显式地添加 Lombok 依赖了。之后，还需要为 Intellij IDEA 安装 Lombok 插件，否则 Javabean 的 getter / setter 就无法自动编译，也就不能被调用。不过，新版的 Intellij IDEA 也已经内置好了，不需要再安装。

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202307221102864.png)


# 3、常用Lombok注解
具体内容可以去看[补2_十分钟搞懂Java效率工具Lombok使用与原理](2、相关技术/16、常用框架-SpringBoot/补2_十分钟搞懂Java效率工具Lombok使用与原理.md)
1. @Getter  / @Setter  
   
   @Getter / @Setter 用起来很灵活，比如说像下面这样：
   ![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202307221104859.png)
   
   字节码文件反编译后的内容是：
   ![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202307221104577.png)

2. @ToString：打印日志的好帮手哦。
3. val：用在局部变量前面，相当于将变量声明为final
4. @Data：注解可以生成 getter / setter、equals、hashCode，以及 toString，是个总和的选项。
5. @Slf4j：可以用来生成注解对象，你可以根据自己的日志实现方式来选用不同的注解，比如说：@Log、@Log4j、@Log4j2、@Slf4j等。
   ![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202307221144432.png)
   
   字节码文件反编译后的内容是：
   ![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202307221146592.png)
6. @Builder：注解可以用来通过建造者模式来创建对象，这样就可以通过链式调用的方式进行对象赋值，非常的方便。
   ![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202307221623681.png)
   
   字节码文件反编译后的内容是：
   ![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202307221629678.png)
除了我上面提到的这些，Lombok 还提供了同步注解 @Synchronized、自动抛出异常注解 @SneakyThrows、不可变对象 @Value、自动生成 hashCode 和 equals 方法的注解 @EqualsAndHashCode  等等，大家可以去尝试一下，顺带看一下反编译后的字节码，体验一下 Lombok 的工作原理。

# 4、Lombok的处理流程

一图胜千言，直接上图。

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202307221647512.png)

1. javac 对源代码进行分析，生成一棵抽象语法树（AST）
2. javac 编译过程中调用实现了JSR 269 的 Lombok 程序
3. Lombok 对 AST 进行处理，找到 Lombok 注解所在类对应的语法树（AST），然后修改该语法树，增加 Lombok 注解定义的相应树节点（所谓代码）
4. javac 使用修改后的抽象语法树生成字节码文件

Lombok 用起来虽然爽，但需要团队内部达成一致，就是要用大家都用，否则有些用了有些没用就会乱成一锅粥，很影响代码的整体风格。另外，假如有团队成员还在用 Eclipse，那么也得要求他安装 Lombok 插件，否则打开一个使用 Lombok 注解的项目就会无法通过编译。

如果一类使用了 Lombok 注解，通过类结构是可以查看到对应的方法的，比如说下图中的 toString 和 builder 方法。

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202307221648238.png)

打开 target 目录下的 .class 文件，就可以看到 Lombok 生成的反编译后的字节码文件，也可以验证 Lombok 是在编译阶段实现 Java 代码增强功能的。

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202307221649012.png)


