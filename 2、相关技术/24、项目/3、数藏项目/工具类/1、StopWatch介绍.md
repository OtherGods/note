`org.apache.commons.lang3.time.StopWatch` 是 Apache Commons Lang 库中用于高精度时间测量的工具类。它提供了简单的 API 来启动、停止、暂停和恢复计时，并支持分段计时。以下是详细用法及示例：

---

### **1. 添加依赖**
在项目中引入 Apache Commons Lang3 依赖：

#### **Maven**
```xml
<dependency>
    <groupId>org.apache.commons</groupId>
    <artifactId>commons-lang3</artifactId>
    <version>3.13.0</version>
</dependency>
```

#### **Gradle**
```groovy
implementation 'org.apache.commons:commons-lang3:3.13.0'
```

---

### **2. 基本用法**

#### **创建实例**
```java
StopWatch stopWatch = new StopWatch();
```

#### **核心方法**
- `start()`: 开始计时（若已启动会抛出异常）。
- `stop()`: 停止计时。
- `reset()`: 重置计时器到初始状态。
- `suspend()`: 暂停计时（`resume()` 恢复）。
- `split()`: 记录“分段时间点”（但自 3.12 版本后已废弃，建议改用其他方式）。

---

### **3. 使用示例**

#### **示例 1：基本计时**
```java
import org.apache.commons.lang3.time.StopWatch;

public class BasicExample {
    public static void main(String[] args) throws InterruptedException {
        StopWatch stopWatch = new StopWatch();
        
        stopWatch.start();
        Thread.sleep(1000); // 模拟耗时操作
        stopWatch.stop();
        
        System.out.println("耗时: " + stopWatch.getTime() + " 毫秒");
        stopWatch.reset(); // 重置以备下次使用
    }
}
```

#### **示例 2：暂停与恢复**
```java
stopWatch.start();
Thread.sleep(500); // 第一阶段
stopWatch.suspend(); // 暂停

Thread.sleep(1000); // 此段时间不计入

stopWatch.resume(); // 恢复
Thread.sleep(500); // 第二阶段
stopWatch.stop();

System.out.println("实际耗时: " + stopWatch.getTime() + " 毫秒"); // 输出约 1000ms
```

#### **示例 3：多阶段计时（无 split）**
```java
stopWatch.start();
// 阶段1
Thread.sleep(800);
stopWatch.stop();
System.out.println("阶段1: " + stopWatch.getTime() + "ms");

stopWatch.reset();
stopWatch.start();
// 阶段2
Thread.sleep(1200);
stopWatch.stop();
System.out.println("阶段2: " + stopWatch.getTime() + "ms");
```

---

### **4. 时间格式转换**
使用 `DurationFormatUtils` 将毫秒转换为可读格式：
```java
import org.apache.commons.lang3.time.DurationFormatUtils;

stopWatch.start();
Thread.sleep(123456);
stopWatch.stop();

String formatted = DurationFormatUtils.formatDurationHMS(stopWatch.getTime());
System.out.println("格式化时间: " + formatted); // 输出 "0:02:03.456"
```

---

### **5. 注意事项**
1. **状态管理**：`StopWatch` 有 `UNSTARTED`、`RUNNING`、`STOPPED`、`SUSPENDED` 四种状态。重复调用 `start()` 或 `stop()` 会抛出 `IllegalStateException`。
2. **线程安全**：非线程安全，需在单线程中使用。
3. **精度**：依赖 `System.nanoTime()`，通常为纳秒级，但返回值为毫秒。
4. **版本差异**：`split()` 方法在 3.12 后废弃，建议通过多次 `start-stop` 或自定义逻辑实现分段计时。

---

### **6. 完整示例代码**
```java
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.lang3.time.DurationFormatUtils;

public class StopWatchDemo {
    public static void main(String[] args) throws InterruptedException {
        StopWatch sw = new StopWatch();

        // 简单计时
        sw.start();
        Thread.sleep(1500);
        sw.stop();
        System.out.println("简单计时: " + sw.getTime() + "ms");

        // 重置并测试多阶段
        sw.reset();
        sw.start();
        Thread.sleep(800);
        sw.stop();
        System.out.println("阶段A: " + sw.getTime() + "ms");

        sw.reset();
        sw.start();
        Thread.sleep(1200);
        sw.stop();
        System.out.println("阶段B: " + sw.getTime() + "ms");

        // 格式化输出
        String formatted = DurationFormatUtils.formatDurationHMS(sw.getTime());
        System.out.println("阶段B 格式化: " + formatted);
    }
}
```

---

通过 `StopWatch` 可以高效管理复杂的时间测量需求，适用于性能分析、任务监控等场景。注意根据版本调整方法使用，避免废弃 API。


