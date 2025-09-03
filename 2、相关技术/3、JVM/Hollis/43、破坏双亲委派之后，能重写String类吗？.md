# 典型回答

> 我整理的：想要重新String类需要两方面操作
> 1. 破坏双亲委托（重写`ClassLoader`z中的`localClass`方法和`findClass`方法）
> 2. 自己实现将字节码的二进制流转换为`Class`对象的逻辑（`ClassLoader`类中的`defineClass`方法（在`findClass`方法中调用）就是干这个事情的，但是个方法里限定了不能将类全限定名以`java.`开头的字节码转换为`Class`对象）

Java通过双亲委派模型保证了java核心包中的类不会被破坏，但破坏双亲委派能够脱离加载范围的限制，增强第三方组件的能力。
[24、什么是双亲委派？如何破坏？](2、相关技术/3、JVM/Hollis/24、什么是双亲委派？如何破坏？.md)

但是我们虽然可以通过破坏双亲委派屏蔽`Bootstrap ClassLoader`，但无法重写java.包下的类，如java.lang.String。

我们知道，要破坏双亲委派模型是需要`extends ClassLoader`并重写其中的`loadClass()`和`findClass()`方法。

之所以无法替换`java.`包的类，**主要原因是即使我们破坏双亲委派模型，==依然需要调用父类中（java.lang.ClassLoader.java）的`defineClass()`方法来把字节流转换为一个JVM识别的`Class`对象==。而`defineClass()`方法中通过`preDefineClass()`方法==限制了类全限定名不能以`java.`开头==。**

如下代码所示：
```java
//将字节流转换成jvm可识别的java类
  protected final Class<?> defineClass(String name, byte[] b, int off, int len,
                                         ProtectionDomain protectionDomain)
        throws ClassFormatError
    {
        protectionDomain = preDefineClass(name, protectionDomain);//检查类全限定名是否有效
        String source = defineClassSourceLocation(protectionDomain);
        Class<?> c = defineClass1(name, b, off, len, protectionDomain, source);//调用本地方法，执行字节流转JVM类的逻辑。
        postDefineClass(c, protectionDomain);
        return c;
    }

//检查类名的有效性
 private ProtectionDomain preDefineClass(String name,
                                            ProtectionDomain pd)
    {
        if (!checkName(name))
            throw new NoClassDefFoundError("IllegalName: " + name);
        if ((name != null) && name.startsWith("java.")) { //禁止替换以java.开头的类文件
            throw new SecurityException
                ("Prohibited package name: " +
                 name.substring(0, name.lastIndexOf('.')));
        }
        if (pd == null) {
            pd = defaultDomain;
        }

        if (name != null) checkCerts(name, pd.getCodeSource());

        return pd;
    }
```

注意，`defineClassX`三兄弟是三个本地方法，用于不同参数长度的方法调用。
```java
private native Class<?> defineClass0(String name, byte[] b, int off, int len, ProtectionDomain pd);

private native Class<?> defineClass1(String name, byte[] b, int off, int len, ProtectionDomain pd, String source);

private native Class<?> defineClass2(String name, java.nio.ByteBuffer b, int off, int len, ProtectionDomain pd, String source);
```

对应到JDK源码中分别为：
```java
JNIEXPORT jclass JNICALL
Java_java_lang_ClassLoader_defineClass0(JNIEnv *env,
                                        jobject loader,
                                        jstring name,
                                        jbyteArray data,
                                        jint offset,
                                        jint length,
                                        jobject pd)
                                        
JNIEXPORT jclass JNICALL
Java_java_lang_ClassLoader_defineClass1(JNIEnv *env,
                                        jobject loader,
                                        jstring name,
                                        jbyteArray data,
                                        jint offset,
                                        jint length,
                                        jobject pd,
                                        jstring source)
                                    
JNIEXPORT jclass JNICALL
Java_java_lang_ClassLoader_defineClass2(JNIEnv *env,
                                        jobject loader,
                                        jstring name,
                                        jobject data,
                                        jint offset,
                                        jint length,
                                        jobject pd,
                                        jstring source)
```

这三个`C++`方法会调用到`SystemDictionary::resolve_from_stream`检查全限定名是否包含`java.`
```java
klassOop SystemDictionary::resolve_from_stream(Symbol* class_name, Handle class_loader, Handle protection_domain, ClassFileStream* st, bool verify, TRAPS) {
 ...//省略无关代码，以下是并检查全限定名，若包含java.，则抛出异常。
 const char* pkg = "java/";
  if (!HAS_PENDING_EXCEPTION &&
      !class_loader.is_null() &&
      parsed_name != NULL &&
      !strncmp((const char*)parsed_name->bytes(), pkg, strlen(pkg))) {
    ResourceMark rm(THREAD);
    char* name = parsed_name->as_C_string();
    char* index = strrchr(name, '/');
    *index = '\0';
    while ((index = strchr(name, '/')) != NULL) {
      *index = '.';
    }
    const char* fmt = "Prohibited package name: %s";
    size_t len = strlen(fmt) + strlen(name);
    char* message = NEW_RESOURCE_ARRAY(char, len);
    jio_snprintf(message, len, fmt, name);
    Exceptions::_throw_msg(THREAD_AND_LOCATION,
      vmSymbols::java_lang_SecurityException(), message);
  }
}
```

但是，**如果破坏双亲委派的时候==自己实现一个将字节流转换为一个jvm可识别的class，那确实绕过`defineClass()`中的校验全限定名的逻辑==，也就可以改写java.lang.String，并加载到JVM中。**

## 示例代码

环境：
- JDK8：JDK8 及之前能编译类路径以`java.*`开头的类，JDK9中在编译的时候做了校验

自定义String类
```java
package java.lang;

public final class String {    
    public boolean equals(Object anObject) {
        System.out.println("modify String equals method called");
        return false;
    }
}
```

在jdk8环境下编译这个类：
```shell
D:\Java\jdk\bin>java -version
java version "1.8.0_181"
Java(TM) SE Runtime Environment (build 1.8.0_181-b13)
Java HotSpot(TM) 64-Bit Server VM (build 25.181-b13, mixed mode)
D:\Java\jdk\bin>javac D:\lib\java\lang\String.java

D:\Java\jdk\bin>
```

自定义类加载器`StringClassLoader`
```java
package com.mimaxueyuan.jvm.classloader;  
  
import java.io.ByteArrayOutputStream;  
import java.io.File;  
import java.io.FileInputStream;  
import java.io.IOException;  
import java.lang.reflect.Constructor;  
import java.lang.reflect.Method;  
  
/**  
 * @ClassName: StringClassLoader 
 * @author: li 
 *  VM -verbose:class //输出类加载日志  
 */  
public class StringClassLoader extends ClassLoader {  
      
    private String customPath;  
      
    public StringClassLoader(String path) {  
       customPath = path;  
    }  
      
    /**  
     * 重写loadClass方法，破坏双亲委托机制。  
     */  
    @Override  
    public Class<?> loadClass(String name) throws ClassNotFoundException {  
       return loadClass(name, false);  
    }  
      
    /**  
     * 重写loadClass方法，有限加载自定义类路径中的字节码，找不到再去父加载器中查找。  
     */  
    protected Class<?> loadClass(String name, boolean resolve)  
          throws ClassNotFoundException  
    {  
       Class<?> c = findLoadedClass(name);  
       if (c == null) {  
          c = findClass(name);  
       }  
       // 自定义类加载器找不到，调用父类加载器  
       c = super.loadClass(name);  
       return c;  
    }  
      
    /**  
     * 从自定义路径（customPath）加载类文件，并转换成Class对象返回。  
     */  
    @Override  
    protected Class<?> findClass(String name) throws ClassNotFoundException {  
       String path = name.replace('.', '/').concat(".class");  
       File file = new File(customPath, path);  
       System.out.println("加载:"+file.getAbsolutePath());  
       try {  
          // 读取二进制流  
          FileInputStream is = new FileInputStream(file);  
          ByteArrayOutputStream bos = new ByteArrayOutputStream();  
          int len = 0;  
          try {  
             while ((len = is.read()) != -1) {  
                bos.write(len);  
             }  
          } catch (IOException e) {  
             e.printStackTrace();  
          }  
          byte[] data = bos.toByteArray();  
          is.close();  
          bos.close();  
          // 重要：将class字节码的二进制流，转换为Class对象  
          return defineClass(name, data, 0, data.length);  
       } catch (IOException e) {  
          e.printStackTrace();  
       }  
       //调用父类的findClass, 父类内部直接抛出ClassNotFoundException异常  
       return super.findClass(name);  
    }  
      
    public static void main(String[] args) {  
       StringClassLoader stringLoader = new StringClassLoader("D:\\lib");  
       try {  
          //查看父加载器  
          ClassLoader parentClassLoader = stringLoader.getParent();  
          System.out.println("parentClassLoader:"+parentClassLoader);  
            
          // StringClassLoader中重新写了loadClass方法，破坏了双亲委托，  
          // 可以先加载自定义类路径下的字节码文件，找不到才会去父加载器中查找。  
          Class clazz = stringLoader.loadClass("java.lang.String");  
          if (clazz != null) {  
             try {  
                // 初始化对象  
                Constructor constructor = clazz.getConstructor(String.class);  
                Object obj = constructor.newInstance("abc");  
                // 获取 equals 方法  
                Method method = clazz.getDeclaredMethod("equals", Object.class);  
                // 通过反射调用equals方法  
                Object abc = method.invoke(obj, "abc");
                System.out.println(abc);
             } catch (Exception e) {  
                e.printStackTrace();  
             }  
          }  
       } catch (ClassNotFoundException e) {  
          e.printStackTrace();  
       }  
    }  
}
```

运行结果：
```java
parentClassLoader:sun.misc.Launcher$AppClassLoader@18b4aac2
加载:D:\lib\java\lang\String.class
Exception in thread "main" java.lang.SecurityException: Prohibited package name: java.lang
	at java.lang.ClassLoader.preDefineClass(ClassLoader.java:662)
	at java.lang.ClassLoader.defineClass(ClassLoader.java:761)
	at java.lang.ClassLoader.defineClass(ClassLoader.java:642)
	at com.mimaxueyuan.jvm.classloader.StringClassLoader.findClass(StringClassLoader.java:71)
	at com.mimaxueyuan.jvm.classloader.StringClassLoader.loadClass(StringClassLoader.java:40)
	at com.mimaxueyuan.jvm.classloader.StringClassLoader.loadClass(StringClassLoader.java:29)
	at com.mimaxueyuan.jvm.classloader.StringClassLoader.main(StringClassLoader.java:88)

```