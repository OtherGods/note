# Servlet中的跳转方式



[TOC]



## 重定向 Redirect



### 使用背景

在某些些情况下，针对用户端的请求，一个Servlet类可能无法完成全部工作，这时可以通过重定向来完成，所谓的重定向是指：Web服务器接收到客户端的请求之后，可能由于某些条件限制，不能访问当前请求URL所指向的Web资源，而是指定了一个新的资源路径，让客户端重新发送请求。



### 工作原理图

![请求重定向](D:\Tyora\AssociatedPicturesInTheArticles\Servlet中的跳转方式_我补充修改之后的\请求重定向.jpg)



### 重定向的两种方式

#### 传统写法

```javascript
response.getWriter().write("<script>location.href='index.jsp'; </script>");
```

#### sendRedirect方法

```java
//HttpServletReponse类中的方法
//该方法用于生成302响应码和Location响应头，从而通知客户端重新访
//问Location响应头中指定的URL
public void sendRedirect(String location)
```

使用方式：（等价于传统写法）

```java
//可以重定向到一个静态资源，也可以重定向到一个Servlet
response.sendRedirect("index.jsp");   //参数可以是一个静态页面名，也可以是一个Servlet的路径名
```



### 重定向跳转的特点

1. 整个过程用户的浏览器一共发送了2次请求

2. 第一次请求服务器，服务器返回了一个302的重定向状态码（第一次是用户手动发送）

3. 第二次浏览器根据服务器返回的跳转地址又重新请求到新的页面（第二次是浏览器根据返回第一次请求的响应内容而自动发送）

4. 重定向时候浏览器地址发生变化，因为浏览器会重新发一次请求

   +---+-----------------+------------------+
   | 1 | > 常见的状态码:  						|
   +===+=================+==================+
   | 2 | 404             | > 资源找不到          |
   +---+-----------------+------------------+
   | 3 | 500             | > 服务器内部错误  |
   +---+-----------------+------------------+
   | 4 | 200             | > 服务器正常返回  |
   +---+-----------------+------------------+
   | 5 | 302             | > 请求重定向          |
   +---+-----------------+------------------+
   | 6 | > \...\....       | 								   |
   +---+-----------------+------------------+



### 注意

​	在重定向中两次浏览器发送的请求中的内容不一定是相同的，有可能在第一次请求发送到服务端之后，服务端在这个请求中增加了一些东西，而第二次请求时获取不到这些东西的；不同于请求转发。



## 转发 forward

### 使用背景

当一个Web资源受到客户端的请求后，如果希望服务器通知另一个资源去处理请求，这是处理使用sendRedirect()方法实现请求重定向之外，还可以通过RequestDispatcher接口的实例对象来实现；这就是请求转发：在Servlet中，如果当前Web资源不想处理请求时，可以i通过forward()方法将当前请求传递给其他Web资源进行处理，这种方式称之为请求转发。

### 工作原理图

![请求转发](D:\Tyora\AssociatedPicturesInTheArticles\Servlet中的跳转方式_我补充修改之后的\请求转发.jpg)



### 转发的方式

```Java
//HttpServletRequest类中的方法
public RequestDispatcher getRequestDispatcher(String path)

//RequestDispatcher类中的方法
public void forward(ServletRequest request, ServletResponse response)
```

使用方式：

```Java
//可以重定向到一个静态资源，也可以重定向到一个Servlet
request.getRequestDispatcher("list.jsp").forward(request,response); //第一个方法的参数可以是一个静态页面名，也可以是一个Servlet的路径名
```



### 转发特点

1. 转发是服务器内部的跳转，一共发送了一次请求！！！
2. ServletRequest中存储的数据在转发前后不会丢失
3. 转发时浏览器中的地址保留上一次浏览器发送请求时的地址（浏览器地址栏不变）



### 请求转发实际场景

> 场景：用户在登录一个购物网站之后查看自己已购买的商品；

```
过程介绍：
用户在发送了一次请求之后，服务器中相应的Web应用程序会接收到用户的请求，之后调用模型层中的方法去查询该用户的所有商品，将查询到的商品的信息存储在这个ServletRequest中(调用request.setAttribute(String name, Object o)方法)，之后使用请求转发(调用request.getRequestDispatcher(String path).forward(request,response)方法)在服务器内部进行跳转，跳转到另一个Servelt或者JSP中将用户请求商品信息的数据写入到视图层最后返回给用户；
```

```
过程分析：（参考下面示例代码中的请求转发的代码）

在这个过程中，跳转前和跳转后的请求是同一个ServletRequest实例对象，在服务端存储在ServletRequest中的数据不会丢失，而请求重定向不能保证第一次请求中得到的商品数据信息在第二次重定向的请求中还存在；

之所以需要使用跳转是因为用户自己想要看到的页面（例如下面示例代码中的index.jsp页面）中包含着一些存储在服务端数据库中的数据，所以用户不能直接在浏览器中输入可以获取到index.jsp页面的地址从而得到正确的index.jsp页面，因为用户没有这些数据，需要服务端的Web应用做一些处理，所以需要用户在浏览器中请求另一个Servlet（例如下面示例代码中的xxx Servlet），这个Servlet接收到请求之后会自动获取对应的数据，把数据存储到ServletRequest中，然后请求转发到index.jsp，把用户需要的数据渲染到这个jsp页面中，最后返回给用户,这样就可以得到正确的index.jsp的页面了。


```





## 请求包含（类似于转发）



### 使用背景

请求包含是使用include()方法将Servlet请求转发给其他Web资源进行处理，与请求转发不同的时，在请求包含返回的响应消息中，即包含了当前Servlet的响应消息，也包含了请求包含中指向的其他Web资源所做出的响应消息。

### 工作原理图

![请求包含](D:\Tyora\AssociatedPicturesInTheArticles\Servlet中的跳转方式_我补充修改之后的\请求包含.jpg)

### 请求包含的方式

```Java
//HttpServletRequest类中的方法
public RequestDispatcher getRequestDispatcher(String path)

//RequestDispatcher类中的方法
public void include(ServletRequest request,ServletResponse response)
```

使用方式

```java
//可以重定向到一个静态资源，也可以重定向到一个Servlet
RequestDispatcher rd = request.getRequestDispatcher("index"); //第一个方法的参数可以是一个静态页面名，也可以是一个Servlet的路径名

//这是请求包含前给客户端的输出，会和请求包含后的资源以同响应给客户端
out.println("before includeing + </br>");

//执行请求包含
rd.include(request,response);
```

### 包含特点

​	和请求转发的特点类似，只是在包含前后（include方法执行前后）的Servlet给客户端的响应都会显示在客户端的浏览器中，而请求转发不会将转发前Servlet给客户端的响应展示给客户端。



### 关于请求包含中的乱码

​	在下面的示例代码中关于包含的代码中在include方法执行前后的Servlet（include前是xxx  Servlet，include后是index  Servlet）中，如果仅仅在index  Servlet中解决乱码问题那么，不会成功解决，这是因为浏览器在请求xxx  Servlet时，用于封装响应消息的HttpServletResponse对象已经创建，该对象在编码时采用的时默认的ISO-8859-1，所以，当用户端对接收到的数据进行解码的时候，Web服务器会保持调用HttpServletResponse对象中的信息，从而使index  Servlet中的输出内容发生乱码。所以想要在请求转发中解决乱码问题，应该在调用include()方法的Servlet中解决乱码（也就是xxx  Servlet）。

​	



## 总结



在理解请求转发的时候，我发现我对在浏览器中访问服务端中的资源的理解有些问题，我重新梳理了一下知识点之后的感悟如下：

1. 在浏览器中给服务端发送请求的时候，使用的url中资源的路径可以直接指向一个静态的页面（html、jsp），也可以指向在服务端中的一个Servlet（需要给它家一个注解，或者在web.xml中配置路径）
2. 在服务端中的Web应用程序中接收到用户的一次请求之后，可以转发用户的请求到另一个Servlet或者静态资源（例如：html页面），也可以重定向用户的请求到另一个Servlet或者静态资源（例如：html页面）
3. 在html、jsp等视图层中，使用的url中资源的路径可以直接指向一个静态的页面（html、jsp），也可以指向在服务端中的一个Servlet（需要给它家一个注解，或者在web.xml中配置路径）



### 示例代码

shitou1项目中的代码如下（有关转发和重定向）：

xxx  Servlet的代码（主要时这段代码，在测试的时候打开注释一一对比）

```java
@WebServlet("/xxx")
public class xxx extends HttpServlet{

	@Override
	public void service(HttpServletRequest request,HttpServletResponse response) throws IOException, ServletException{
         //Web应用程序保存在ServletRequest中的键值对
		request.setAttribute("我是一个标志", "——我是该标志的值——");
		
		//请求重定向
//		response.sendRedirect("index.jsp");		//①
//		response.sendRedirect("index");		    //②
		
		//请求转发
//		request.getRequestDispatcher("index.jsp").forward(request,response);	//③
//		request.getRequestDispatcher("/index").forward(request,response);		//④
	}
}

@WebServlet("/xxx")
public class xxx extends HttpServlet{

	@Override
	public void service(HttpServletRequest request,HttpServletResponse response) throws IOException, ServletException{
		request.setAttribute("我是一个标志", "——我是该标志的值——");
		
		//请求重定向
//		response.sendRedirect("index.jsp");		//①
//		response.sendRedirect("index");		    //②
		
		//请求转发
//		request.getRequestDispatcher("index.jsp").forward(request,response);	//③
//		request.getRequestDispatcher("/index").forward(request,response);		//④

		
		//请求包含，需要在这个xxx  Servlet中解决乱码
//		//解决请求参数的中文乱码问题//——————————————————————————————————————————|
//		request.setCharacterEncoding("utf-8");//						    ⑤	
//		//解决中文输出（响应）乱码问题//										   | 	
//		response.setContentType("text/html;charset=utf-8");//———————————————|
		
//		RequestDispatcher rd = request.getRequestDispatcher("index.jsp");//————|
//		response.getWriter().write("include方法执行前的Servlet响应");//			  ⑥
//		rd.include(request,response);//————————————————————————————————————————|
//		RequestDispatcher rd = request.getRequestDispatcher("/index");//———————|
//		response.getWriter().write("include方法执行前的Servlet响应</br>");//	  ⑦
//		rd.include(request,response);//————————————————————————————————————————|
	}
}
```



index  Servlet的代码

```java
@WebServlet("/index")
public class index extends HttpServlet {
	private static final long serialVersionUID = 1L;
	protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		//在这个示例代码中用户请求重定向和转发的解决乱发代码，在请求包含解决乱码应在在xxx  Servlet中
		//解决请求参数的中文乱码问题
		request.setCharacterEncoding("utf-8");
		//解决中文输出（响应）乱码问题
		response.setContentType("text/html;charset=utf-8");
		
		response.getWriter().write("index  Servlet接收到的request中的内容为："+request.getAttribute("我是一个标志")+"</br>");
		response.getWriter().write("[重定向|转发|包含]到index  Servlet路径，在该Servlet中向页面输出内容");
	}
}
```



index.jsp  的代码

```html
<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<%@ page import="java.lang.String"%>
<html>
<head>
<meta charset="UTF-8">
<title>Insert title here</title>
</head>
<body>
	<div>直接重定向到一个index.html页面</div>
	<%
		String s = (String)request.getAttribute("我是一个标志");
		out.print(s);
	%>
</body>
</html>
```

在浏览器的url栏中输入localhost:8080/shitou1/xxx进行测试

测试的结果:

1. 打开xxx  Servlet代码中的①处的注释，因为在重定向之后用户的浏览器第二次发送的请求中没有index.jsp页面中需要的键  “我是一个标志”，所以返回给用户的页面中打印出来的该键对应的值为null。
2. 打开xxx  Servlet代码中的②处的注释，因为在重定向之后用户的浏览器第二次发送的请求中没有index.jsp页面中需要的键  “我是一个标志”，所以返回给用户的页面中打印出来的该键对应的值为null。
3. 打开xxx  Servlet代码中的③处的注释，因为转发之前和转发之后的ServletRequest是同一个，所以在request中保存的键  “我是一个标志”  有对应的值，在index.jsp页面中可以打印出来
4. 打开xxx  Servlet代码中的④处的注释，因为转发之前和转发之后的ServletRequest是同一个，所以在request中保存的键  “我是一个标志”  有对应的值，在index.jsp页面中可以打印出来
5. 打开xxx  Servlet代码中的⑤、⑥处的注释，在客户端会展示出xxx、index  Servlet中展示给客户端的响应内容，同时在request中存储的键  “我是一个标志”  有对应的值，在index.jsp页面中可以打印出来
6. 打开xxx  Servlet代码中的⑤、⑦处的注释，在客户端会展示出xxx、index  Servlet中展示给客户端的响应内容，同时在request中存储的键  “我是一个标志”  有对应的值，在index.jsp页面中可以打印出来

