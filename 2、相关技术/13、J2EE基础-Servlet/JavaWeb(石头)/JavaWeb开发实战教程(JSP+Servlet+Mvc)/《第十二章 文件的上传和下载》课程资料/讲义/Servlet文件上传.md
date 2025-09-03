# Servlet文件上传

## 1.1 注解式实现文件**上传**

-   文件上传注意事项：

1.  表单一定是POST请求，否则不能文件上传

2.  表单设置属性：enctype=\"multipart/form-data\" 表示表单中存在文件上传框！

    multipart/form-data是指表单数据有多部分构成，既有文本数据，又有文件等二进制数据的意思

    ![image-20220731220538038](D:\Tyora\AssociatedPicturesInTheArticles\Servlet文件上传\image-20220731220538038.png)

```java
/**
 * Servlet3.0之后可以使用@MultipartConfig注解实现文件的上传
 * 同步文件上传，不是异步
 */
@WebServlet("/NewFileUpServlet")
@MultipartConfig
public class NewFileUpServlet extends HttpServlet {

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		//获取用户提交的表单中所有的键值对
		Collection<Part> list = request.getParts();
        //创建要文件的上传到服务器中的地址（项目发布到Tomcat中的地址）
		File fileDir = createDir(getServletContext());
        //因为是同步上传文件，只有用户在点击form表单的提交按钮的时候才会同时上传文件
		for (Part p : list) 
        {
			String name = p.getName();
			if("要上传的文件所在的标签的name值".equals(name)){
        	    //得到的header字符串为：
            	//form-data; name="providerCard"; filename="Bean的生命周期.jpg"
	        	String header = p.getHeader("content-disposition");
    	        //这里是获得header中的上传的文件的名字，可以调用
        	    //createName方法给文件名加一个前缀防止文件名重复
				String fileName = getNameByHeader(header);
    	        //创建要上传的文件的File对象
				File file = new File(fileDir, fileName);
				//保存文件
				p.write(file.getAbsolutePath());
            }
		}
        //接下来应该处理form表单中的其他数据，例如：将表单中的所有数据添加到数据库中（数据库中保
        //存的是文件上传的路径）
        
	}

	// 从消息头中获取上传的文件名称
	private String getNameByHeader(String msg) {
		// 获取正文的描述信息
		// String msg=request.getHeader("content-disposition");
		System.out.println(msg);
		String[] arr1 = msg.split(";");
		String fn = "";
		if (arr1.length == 3) {
			fn = arr1[2].split("=")[1].replaceAll("\"", "");
		}
        //这样文件名可能会冲突，可以调用createName方法给文件名加上一个前缀
		return fn;
	}

	// 创建目录---以日期，一天一个文件夹
	private File createDir(ServletContext context) {
        //要上传的服务器的绝对地址（也就是项目发布到Tomcat中的
        //地址），而不是硬盘中的绝对地址（如下图①），因为最后要把
        //项目发布到Tomcat服务器中。
		String p1 = context.getRealPath("/files");
		File file = new File(p1, new SimpleDateFormat("yyyy-MM-dd").format(Calendar.getInstance().getTime()));
		if (!file.exists()) {
			file.mkdirs();
		}
		return file;
	}

	// 创建文件名--区分同名文件,在文件名前加上当前的时间
	private String createName(String name) {
		return new SimpleDateFormat("yyyyMMddHHmmssSSS").format(Calendar.getInstance().getTime()) + "_" + name;
	}
}


```

![图①](D:\Tyora\AssociatedPicturesInTheArticles\Servlet文件上传\1.png)



# Servlet文件下载

```java
			String id = request.getParameter("id");//要下载的编号
			String downPath = "xxx/xxx"; //要下载的文件在服务器中的真实路径
			//文件名，可以通过方法获得
			String fileName="01.wmv";
			
			//设置响应头，设置文件名，防止用户下载的时候出现文件名乱码
			response.setHeader("Content-Disposition","attachment;filename="+URLEncoder.encode(fileName,"utf-8"));
			
			//5.用文件流的方式输出到用户浏览器中
			FileInputStream fis=new FileInputStream(downPath);//输入流，把服务器硬盘中的文件封读入到内存中
			ServletOutputStream os = response.getOutputStream();//字节输出流，将内存中文件输出到这里
			
			byte[] bytes=new byte[1024]; //缓冲区  1024byte=1kb 
			int data = fis.read(bytes);
			while(data!=-1){
                //将内存中文件数据写出到字节流中
				os.write(bytes, 0, bytes.length);
				data = fis.read(bytes);
			}
			//6.关闭流
			os.close();
			fis.close();

```


