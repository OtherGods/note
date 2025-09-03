参考：
- [4、对JDK进程执行kill -9有什么影响？](2、相关技术/3、JVM/Hollis/4、对JDK进程执行kill%20-9有什么影响？.md)
- [57、什么情况会导致JVM退出？](2、相关技术/3、JVM/Hollis/57、什么情况会导致JVM退出？.md)

# 典型回答
[54、final、finally、finalize有什么区别](2、相关技术/1、Java基础/Hollis/54、final、finally、finalize有什么区别.md)

通常情况下，`finally`的代码一定会被执行，但是这是有一个前提的：
1. **对应 try 语句块被执行**
2. **程序正常运行**

如果没有符合这两个条件的话，`finally`中的代码就无法被执行，如发生以下情况，都会导致`finally`不会执行：
1. `System.exit()`方法被执行
2. `Runtime.getRuntime().halt()`方法被执行
3. 操作系统强制杀掉了JVM进程，如执行了`kill -9`
4. try或者catch中有死循环
5. 其他原因导致的虚拟机崩溃了
6. 虚拟机所运行的环境挂了，如计算机电源断了
7. 如果一个finally是由**守护线程执行**的，那么是不保证一定能执行的；**JVM退出时，JVM会检查其他非守护线程，如果都执行完了，那么就直接退出了**。这时候finally可能就没办法执行完。
[4、什么是守护线程，和普通线程有什么区别？](2、相关技术/2、JUC/Hollis/Java并发/4、什么是守护线程，和普通线程有什么区别？.md)

# 扩展知识

## finally执行顺序

[53、try中return A，catch中return B，finally中return C，最终返回值是什么？](2、相关技术/1、Java基础/Hollis/53、try中return%20A，catch中return%20B，finally中return%20C，最终返回值是什么？.md)
