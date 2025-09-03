# 前言

  在开发过程中异常处理是经常用到的，相信大部分使用try、catch、finally的只知道try中出现异常catch中会捕获，finally块中代码何时都会执行。其实其中还有很多细微的知识点，下面我们来学习学习。

# try、catch、finally执行顺序

  try块和catch块中逻辑基本相同。try中出现异常跳转到catch，若catch中出现异常则跳转到finally，try或catch正常执行若存在return则先执行return的代码并保存返回值信息（基本类型保存值信息，引用类型则保存地址信息下面会说明）然后执行finally，若finally中出现异常或包含return则执行结束，若无异常且没有return则会执行try或catch中的return或结束。整体执行流程如下：
![image-20230615232856710](D:\Tyora\AssociatedPicturesInTheArticles\1_try、catch、finally 用法总结\image-20230615232856710.png)

# 说明与代码展示

  当try-catch-finally中无return时，如果try块中出现异常则进入catch中，执行完catch中代码后进入finally，如果catch中出现异常仍然会执行finally代码块，finally块执行结束后抛出异常。try块中无异常时执行完try块直接执行finally。
以catch中抛出异常为例，代码如下：

```java
public static int testNoReturn(){
        int res = 1;
        try{
            res++;
            System.out.println("try ======== res:"+res);
            int a = 1/0;
        }catch (Exception e){
            res++;
            System.out.println("catch ======== res:"+res);
            int a = 1/0;
        }finally {
            res++;
            System.out.println("finally ======== res:"+res);
        }
        return res;
    }
```


  catch中抛出异常后finally仍然会执行，执行结束后抛出catch中的异常。执行结果如下：
![image-20230615233042643](D:\Tyora\AssociatedPicturesInTheArticles\1_try、catch、finally 用法总结\image-20230615233042643.png)

  try或catch中存在return 时流程基本一致所以合到一起讲下。finally中包含return时会覆盖try或catch中的return值，而且会覆盖catch中抛出的异常信息。
try或catch包含return，返回值为基本数据类型代码如下：

```java
 public static int testTryCatchReturn(){
        int res = 1;
        try{
            res++;
            System.out.println("try ======== res:"+res);
            int a=1/0;
            return res;
        }catch (Exception e){
            res++;
            System.out.println("catch ======== res:"+res);
            return res;
        }finally {
            res++;
            System.out.println("finally ======== res:"+res);
        }
    }
```

执行结果如下：
![image-20230615233129149](D:\Tyora\AssociatedPicturesInTheArticles\1_try、catch、finally 用法总结\image-20230615233129149.png)

try或catch包含return，返回值为引用数据类型代码如下：

```java
public static List testTryCatchReturn1(){
        List res = new ArrayList();
        try{
            res.add(1);
            System.out.println("try ======== res:"+res);
            int a=1/0;
            return res;
        }catch (Exception e){
            res.add(2);
            System.out.println("catch ======== res:"+res);
            return res;
        }finally {
            res.add(3);
            System.out.println("finally ======== res:"+res);
        }
    }
```


执行结果如下：
![image-20230615233155402](D:\Tyora\AssociatedPicturesInTheArticles\1_try、catch、finally 用法总结\image-20230615233155402.png)

finally中将引用的返回值赋值为null时代码如下：

```java
public static List testTryCatchReturn1(){
        List res = new ArrayList();
        try{
            res.add(1);
            System.out.println("try ======== res:"+res);
            int a=1/0;
            return res;
        }catch (Exception e){
            res.add(2);
            System.out.println("catch ======== res:"+res);
            return res;
        }finally {
            res.add(3);
            System.out.println("finally ======== res:"+res);
            res = null;
            System.out.println("finally ======== res:"+res);
        }
    }
```


执行结果如下：
![image-20230615233254424](D:\Tyora\AssociatedPicturesInTheArticles\1_try、catch、finally 用法总结\image-20230615233254424.png)

  try或catch中存在return时会将返回值进行保存（基本数据类型直接保存，引用数据类型则保存引用地址），然后执行finally块中的代码，finally块中代码执行结束后直接返回。因为引用类型返回时保存的是地址，所以修改引用对象时返回信息会发生变化，但如果赋值为null时地址指向的信息并未发生变化所以返回值依然是地址指向的对象。
  当finally中存在return时会覆盖try或catch中返回的数据。如果catch中抛出异常时会将该异常覆盖掉。以catch中存在异常的情况为例，代码如下：

```java
public static int testFinallyReturn(){
        int res = 1;
        try{
            res++;
            System.out.println("try ======== res:"+res);
            int a=1/0;
            return res;
        }catch (Exception e){
            res++;
            System.out.println("catch ======== res:"+res);
            int a = 1/0;
            return res;
        }finally {
            res++;
            System.out.println("finally ======== res:"+res);
            return res;
        }
    }
```

执行结果如下：
![image-20230615233224010](D:\Tyora\AssociatedPicturesInTheArticles\1_try、catch、finally 用法总结\image-20230615233224010.png)

finally的return会覆盖catch或try的return和异常信息。

# 总结

  1、无return且未出现异常时try->finally，出现异常时try->catch->finally。
  2、try或catch中有return时，返回数据为基本类型则finally代码块执行完后不会更改，返回值为引用类型，return保存的是引用地址，finally块中代码执行完会改变返回值。
  3、finally中存在return时会覆盖try或catch中的返回值信息，若try或catch中抛出异常也会被finally中的return覆盖。



————————————————
版权声明：本文为CSDN博主「诸葛小哥~」的原创文章，遵循CC 4.0 BY-SA版权协议，转载请附上原文出处链接及本声明。
原文链接：https://blog.csdn.net/weixin_42168421/article/details/120744192