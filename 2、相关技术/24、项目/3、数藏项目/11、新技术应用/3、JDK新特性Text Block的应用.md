Java 13中提供了一个Text Blocks的预览特性，并且在Java 14中提供了第二个版本的预览。

text block，文本块，是一个多行字符串文字，它避免了对大多数转义序列的需要，以可预测的方式自动格式化字符串，并在需要时让开发人员控制格式。

我们以前从外部copy一段文本串到Java中，会被自动转义，如有一段以下字符串：
```java
<html>  
  <body>  
      <p>Hello, world</p>  
  </body>  
</html>
```

将其复制到Java的字符串中，会展示成以下内容：
```java
"<html>\n" +  
"    <body>\n" +  
"        <p>Hello, world</p>\n" +  
"    </body>\n" +  
"</html>\n";
```

即被自动进行了转义，这样的字符串看起来不是很直观，在JDK 13中，就可以使用以下语法了：
```java
"""  
<html>  
  <body>
	  <p>Hello, world</p>
  </body>
</html>  
""";
```

使用`"""`作为文本块的开始符合结束符，在其中就可以放置多行的字符串，不需要进行任何转义。看起来就十分清爽了。

如常见的SQL语句：
```java
String query = """  
    SELECT `EMP_ID`, `LAST_NAME` FROM `EMPLOYEE_TB`
	WHERE `CITY` = 'INDIANAPOLIS'
	ORDER BY `EMP_ID`, `LAST_NAME`;
""";
```

看起来就比较直观，清爽了。

我们的项目中，也用到了 text block，如：
```java
String luaScript = """  
        local value = redis.call('GET', KEYS[1])
        redis.call('DEL', KEYS[1])
        return value""";
```

我们在定义 lua 脚本的时候，用到了text block
