
# 1、Properties

Properties类是Java中的一个工具类，用于处理属性文件（.properties）的读取和写入。属性文件通常用于存储配置信息，比如数据库连接参数、应用程序的配置等。

下面是对Properties类的简要介绍和使用示例：

1. Properties类的主要方法：
    
    - `setProperty(String key, String value)`：设置属性的值。
    - `getProperty(String key)`：根据属性的键获取属性的值。
    - `load(InputStream inStream)`：从输入流中加载属性文件。
    - `store(OutputStream out, String comments)`：将属性存储到输出流中,store方法的第二个参数是一个可选的注释，用于在文件的第一行添加注释。
2. 使用示例：
    ```java
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

public class PropertiesExample {
	public static void main(String[] args) {
		Properties properties = new Properties();
	
		// 从属性文件加载配置
		try (InputStream input = new FileInputStream("config.properties")) {
			properties.load(input);
		} catch (IOException e) {
			e.printStackTrace();
		}
	
		// 读取属性值
		String username = properties.getProperty("username");
		String password = properties.getProperty("password");
	
		System.out.println("Username: " + username);
		System.out.println("Password: " + password);
	
		// 修改属性值
		properties.setProperty("password", "newPassword");
	
		// 将属性保存到文件
		try (OutputStream output = new FileOutputStream("config.properties")) {
			properties.store(output, "Updated properties");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
    ```

上述示例展示了如何使用Properties类读取和写入属性文件。首先，通过load方法从属性文件中加载配置。然后，可以使用getProperty方法获取属性的值。接着，可以使用setProperty方法修改属性的值。最后，使用store方法将修改后的属性保存到属性文件中。


希望这个示例可以帮助你理解Properties类的用法。如果你还有其他问题，请随时提问。

