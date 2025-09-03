## 1.C3P0配置

### 1.1.导入依赖

```xml
<!-- https://mvnrepository.com/artifact/com.mchange/c3p0 -->
<dependency>
    <groupId>com.mchange</groupId>
    <artifactId>c3p0</artifactId>
    <version>0.9.5.2</version>
</dependency>

<dependency>
    <groupId>mysql</groupId>
    <artifactId>mysql-connector-java</artifactId>
    <version>5.1.38</version>
</dependency>
```

### 1.2.整合C3P0配置文件

```xml
<?xml version ="1.0" encoding= "UTF-8" ?>
<c3p0-config>
    <!-- 默认配置，c3p0框架默认加载这段默认配置 -->
    <default-config>
        <!-- 配置JDBC 四个基本属性 -->
        <property name="driverClass">com.mysql.jdbc.Driver</property>
        <property name="jdbcUrl">jdbc:mysql://127.0.0.1:3306/oadb</property>
        <property name="user">root</property>
        <property name="password">123456</property>
    </default-config>
</c3p0-config>
```

### 1.3.获取DataSources

```java
package com.bruce.utils;

import com.mchange.v2.c3p0.ComboPooledDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * @BelongsProject: TomDog
 * @BelongsPackage: com.bruce.utils
 * @CreateTime: 2021-04-13 14:45
 * @Description: TODO
 */
public class JDBCUtils {

    // 获得c3p0连接池对象
    private static ComboPooledDataSource ds = new ComboPooledDataSource();

    /**
     * 获得数据库连接对象
     * @return
     * @throws SQLException
     */
    public static Connection getConnection() throws SQLException {
        return ds.getConnection();
    }

    /**
     * 获得c3p0连接池对象
     *
     * @return
     */
    public static DataSource getDataSource() {
        return ds;
    }
}

```

### 1.4.测试C3P0

```java
public class TestDs {

    public static void main(String[] args) {
        try {
            Connection connection = JDBCUtils.getConnection();
            System.out.println(connection);
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
        }
    }
}
```

## 2.DBUtils配置

### 2.1.导入依赖

```xml
<!-- https://mvnrepository.com/artifact/commons-dbutils/commons-dbutils -->
<dependency>
    <groupId>commons-dbutils</groupId>
    <artifactId>commons-dbutils</artifactId>
    <version>1.6</version>
</dependency>
```

### 2.2.使用DButils

```java
public class UserDao {
    
    QueryRunner queryRunner = new QueryRunner(JDBCUtils.getDataSource());

    public int insertuUser(User user) {
        try {
            return queryRunner.update("insert into t_user values(?,?,?,?)", user.getUsername(), user.getPassword(), user.getSex(), user.getHobby());
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
        }
        return 0;
    }

}
```

