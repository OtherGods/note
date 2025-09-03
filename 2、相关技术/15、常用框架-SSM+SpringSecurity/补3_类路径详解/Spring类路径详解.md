转载自CSDN：https://blog.csdn.net/hansome_hong/article/details/124267485

简单解释：以ssm项目为例，classpath指向的就是打war包之后的classes的位置。而classes文件夹下就是原项目的java文件和resources文件夹里面的内容。

## 1.web项目工程结构

![在这里插入图片描述](D:\Tyora\AssociatedPicturesInTheArticles\Spring类路径详解\aec946c6d06f450f88c928d27c4c5696.png)



2.打包
src/main/下面的java和resources文件夹都被(编译)打包到了生产包的WEB-INF/classes/目录下；而原来WEB-INF下面的views(jsp)和web.xml则仍然还是在WEB-INF下面。同时由maven引入的依赖都被放入到了WEB-INF/lib/下面。最后，编译后的class文件和资源文件都放在了classes目录下。
打包前：
![在这里插入图片描述](D:\Tyora\AssociatedPicturesInTheArticles\Spring类路径详解\2d70fb886e6c44319e4a8fb36319a0e1.png)

打包后：
![img](D:\Tyora\AssociatedPicturesInTheArticles\Spring类路径详解\6cd385afd9144daca8ed09e21eb612ec.png)

## 3.classpath 和 classpath*区别

classpath：只会到你的class路径中查找找文件; 
classpath*：不仅包含class路径，还包括jar文件中(class路径)进行查找. 

注意： 
用classpath*:需要遍历所有的classpath，所以加载速度是很慢的，因此，在规划的时候，应该尽可能规划好资源文件所在的路径，尽量避免使用 这种方式。

另外： 
"**/" 表示的是任意目录；

"** /applicationContext- *.xml"  表示任意目录下的以"applicationContext-"开头的XML文件。  
程序部署到tomcat后，src目录下的配置文件会和class文件一样，自动copy到应用的 WEB-INF/classes目录下 







