原文地址：[https://reflectoring.io/unit-...](https://link.segmentfault.com/?enc=hjTsTaHRfbuyil7dyEZGxw%3D%3D.o0DBFJkOEMqh0n4HfxgKgUOhyy2lco025uboqB5e9lxh5HzObJ5JOqYNSuT5cSJvVgFZmIEQsfWsDSDsrg1mSw%3D%3D)

编写好的单元测试可以被看成一个很难掌握的艺术。但好消息是支持单元测试的机制很容易学习。

本文给你提供在Spring Boot 应用程序中编写好的单元测试的机制，并且深入技术细节。

我们将带你学习如何以可测试的方式创建Spring Bean实例，然后讨论如何使用`Mockito`和`AssertJ`，这两个包在Spring Boot中都为了测试默认引用了。

本文只讨论单元测试。至于集成测试，测试web层和测试持久层将会在接下来的系列文章中进行讨论。

## 代码示例

本文附带的代码示例地址：[spring-boot-testing](https://github.com/OtherGods/code-examples/tree/master/spring-boot/spring-boot-testing)【该项目已经frock到github】

## 使用 Spring Boot 进行测试系列文章

这个教程是一个系列：

1. 使用 Spring Boot 进行单元测试（本文）
2. 使用 Spring Boot 和 @WebMvcTest 测试SpringMVC controller层
3. 使用 Spring Boot 和 @DataJpaTest 测试JPA持久层查询
4. 通过 @SpringBootTest 进行集成测试

如果你喜欢看视频教程，可以看看`Philip`的课程：[测试Spring Boot应用程序课程](https://link.segmentfault.com/?enc=U1fBCc%2BvYjizfWsOLdT1vg%3D%3D.rXykct7%2FjbgYeXoiykHxHHqxJPVnZA551kJhh34Kj8kquywYtRN2dsS4Z7HWGTo8O%2B25wDUeh2ztqP%2B8yol25w%3D%3D)

## 依赖项

本文中，为了进行单元测试，我们会使用`JUnit Jupiter（Junit 5）`，`Mockito`和`AssertJ`。此外，我们会引用`Lombok`来减少一些模板代码：

```
dependencies{
  compileOnly('org.projectlombok:lombok')
  testCompile('org.springframework.boot:spring-boot-starter-test')
  testCompile 'org.junit.jupiter:junit-jupiter-engine:5.2.0'
  testCompile('org.mockito:mockito-junit-jupiter:2.23.0')
}
```

`Mockito`和`AssertJ`会在`spring-boot-test`依赖中自动引用，但是我们需要自己引用`Lombok`。

## 不要在单元测试中使用Spring

如果你以前使用`Spring`或者`Spring Boot`写过单元测试，你可能会说我们不要在写单元测试的时候用`Spring`。但是为什么呢？

考虑下面的单元测试类，这个类测试了`RegisterUseCase`类的单个方法：

```java
@ExtendWith(SpringExtension.class)
@SpringBootTest
class RegisterUseCaseTest {

  @Autowired
  private RegisterUseCase registerUseCase;

  @Test
  void savedUserHasRegistrationDate() {
    User user = new User("zaphod", "zaphod@mail.com");
    User savedUser = registerUseCase.registerUser(user);
    assertThat(savedUser.getRegistrationDate()).isNotNull();
  }
}
```

这个测试类在我的电脑上需要大概4.5秒来执行一个空的Spring项目。

但是一个好的单元测试仅仅需要几毫秒。否则就会阻碍TDD（测试驱动开发）流程，这个流程倡导“测试/开发/测试”。

但是就算我们不使用TDD，等待一个单元测试太久也会破坏我们的注意力。

执行上述的测试方法事实上仅需要几毫秒。剩下的4.5秒是因为`@SpringBootTest`告诉了 `Spring Boot` 要启动整个Spring Boot 应用程序上下文。

所以我们启动整个应用程序仅仅是因为要把`RegisterUseCase`实例注入到我们的测试类中。启动整个应用程序可能耗时更久，假设应用程序更大、`Spring`需要加载更多的实例到应用程序上下文中。

所以，这就是为什么不要在单元测试中使用`Spring`。坦白说，大部分编写单元测试的教程都没有使用`Spring Boot`。

## 创建一个可测试的类实例

然后，为了让`Spring`实例有更好的测试性，有几件事是我们可以做的。

### 属性注入是不好的

让我们以一个反例开始。考虑下述类：

```java
@Service
public class RegisterUseCase {

  @Autowired
  private UserRepository userRepository;

  public User registerUser(User user) {
    return userRepository.save(user);
  }
}
```

这个类如果没有`Spring`没法进行单元测试，因为它没有提供方法传递`UserRepository`实例。因此我们只能用文章之前讨论的方式-让Spring创建`UserRepository`实例，并通过`@Autowired`注解注入进去。

这里的教训是：不要用属性注入。

### 提供一个构造函数

实际上，我们根本不需要使用`@Autowired`注解：

```java
@Service
public class RegisterUseCase {

  private final UserRepository userRepository;

  public RegisterUseCase(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  public User registerUser(User user) {
    return userRepository.save(user);
  }
}
```

这个版本通过提供一个允许传入`UserRepository`实例参数的构造函数来允许构造函数注入。在这个单元测试中，我们现在可以创建这样一个实例（或者我们之后要讨论的Mock实例）并通过构造函数注入了。

当创建生成应用上下文的时候，Spring会自动使用这个构造函数来初始化`RegisterUseCase`对象。注意，在Spring 5 之前，我们需要在构造函数上增加`@Autowired`注解，以便让Spring找到这个构造函数。

还要注意的是，现在`UserRepository`属性是`final`修饰的。这很重要，因为这样的话，应用程序生命周期时间内这个属性内容不会再变化。此外，它还可以帮我们避免变成错误，因为如果我们忘记初始化该属性的话，编译器就报错。

### 减少模板代码

通过使用`Lombok`的`@RequiredArgsConstructor`注解，我们可以让构造函数自动生成:

```java
@Service
@RequiredArgsConstructor
public class RegisterUseCase {

  private final UserRepository userRepository;

  public User registerUser(User user) {
    user.setRegistrationDate(LocalDateTime.now());
    return userRepository.save(user);
  }
}
```

现在，我们有一个非常简洁的类，没有样板代码，可以在普通的 java 测试用例中很容易被实例化：

```java
class RegisterUseCaseTest {

  private UserRepository userRepository = ...;

  private RegisterUseCase registerUseCase;

  @BeforeEach
  void initUseCase() {
    registerUseCase = new RegisterUseCase(userRepository);
  }

  @Test
  void savedUserHasRegistrationDate() {
    User user = new User("zaphod", "zaphod@mail.com");
    User savedUser = registerUseCase.registerUser(user);
    assertThat(savedUser.getRegistrationDate()).isNotNull();
  }
}
```

还有部分确实，就是如何模拟测试类所依赖的`UserReposity`实例，我们不想依赖真实的类，因为这个类需要一个数据库连接。

## 使用Mockito来模拟依赖项

现在事实上的标准模拟库是 `Mockito`。它提供至少两种方式来创建一个模拟`UserRepository`实例，来填补前述代码的空白。

### 使用普通`Mockito`来模拟依赖

第一种方式是使用Mockito编程：

```java
private UserRepository userRepository = Mockito.mock(UserRepository.class);
```

这会从外界创建一个看起来像`UserRepository`的对象。默认情况下，方法被调用时不会做任何事情，如果方法有返回值，会返回`null`。

因为`userRepository.save(user)`返回null，现在我们的测试代码`assertThat(savedUser.getRegistrationDate()).isNotNull()`会报空指针异常（NullPointerException）。

所以我们需要告诉`Mockito`，当`userRepository.save(user)`调用的时候返回一些东西。我们可以用静态的`when`方法实现：

```java
@Test
void savedUserHasRegistrationDate() {
  User user = new User("zaphod", "zaphod@mail.com");
  when(userRepository.save(any(User.class))).then(returnsFirstArg());
  User savedUser = registerUseCase.registerUser(user);
  assertThat(savedUser.getRegistrationDate()).isNotNull();
}
```

这会让`userRepository.save()`返回和传入对象相同的对象。

`Mockito`为了模拟对象、匹配参数以及验证方法调用，提供了非常多的特性。想看更多，[文档](https://link.segmentfault.com/?enc=yIgYsLjJYWhje%2BRKyS3Zfw%3D%3D.V924WZwNvuJkeMkYBir5sDSexpLVhNM%2Fuk%2FxjR9DaAmxGoJm%2BYYF8IgHL75H8zjBOv0J1bqQUdE7janmp0UZR6qlC2CGafBQN0TpMotTFr9pnnhd6mk1kQwFnL3V1KSm)

### 通过`Mockito`的`@Mock`注解模拟对象

创建一个模拟对象的第二种方式是使用`Mockito`的`@Mock`注解结合 JUnit Jupiter的`MockitoExtension`一起使用：

```java
@ExtendWith(MockitoExtension.class)
class RegisterUseCaseTest {

  @Mock
  private UserRepository userRepository;

  private RegisterUseCase registerUseCase;

  @BeforeEach
  void initUseCase() {
    registerUseCase = new RegisterUseCase(userRepository);
  }

  @Test
  void savedUserHasRegistrationDate() {
    // ...
  }
}
```

`@Mock`注解指明那些属性需要`Mockito`注入模拟对象。由于`JUnit`不会自动实现，`MockitoExtension`则告诉`Mockito`来评估这些`@Mock`注解。

这个结果和调用`Mockito.mock()`方法一样，凭个人品味选择即可。但是请注意，通过使用 `MockitoExtension`，我们的测试用例被绑定到测试框架。

我们可以在`RegisterUseCase`属性上使用`@InjectMocks`注解来注入实例，而不是手动通过构造函数构造。`Mockito`会使用特定的[算法](https://link.segmentfault.com/?enc=plqGHLcgJVfOu11ncXswlQ%3D%3D.gTZhJ1dHjDWC1GlU1k4YbbE4uWD%2BxH8YNljm7RR4P7NzsbuPsy3CfqRr5xVsuMGUknO2ejAh8hfmYtKV%2BPTHPATaBsy3G0%2BKSYCNB23nDjiqT54%2Bc2kIQl0Txnr0x%2BMt)来帮助我们创建相应实例对象：

```java
@ExtendWith(MockitoExtension.class)
class RegisterUseCaseTest {

  @Mock
  private UserRepository userRepository;

  @InjectMocks
  private RegisterUseCase registerUseCase;

  @Test
  void savedUserHasRegistrationDate() {
    // ...
  }
}
```

## 使用AssertJ创建可读断言

`Spring Boot` 测试包自动附带的另一个库是`AssertJ`。我们在上面的代码中已经用到它进行断言：

```java
assertThat(savedUser.getRegistrationDate()).isNotNull();
```

然而，有没有可能让断言可读性更强呢？像这样，例子：

```java
assertThat(savedUser).hasRegistrationDate();
```

有很多测试用例，只需要像这样进行很小的改动就能大大提高可理解性。所以，让我们在test/sources中创建我们自定义的断言吧：

```java
class UserAssert extends AbstractAssert<UserAssert, User> {

  UserAssert(User user) {
    super(user, UserAssert.class);
  }

  static UserAssert assertThat(User actual) {
    return new UserAssert(actual);
  }

  UserAssert hasRegistrationDate() {
    isNotNull();
    if (actual.getRegistrationDate() == null) {
      failWithMessage(
        "Expected user to have a registration date, but it was null"
      );
    }
    return this;
  }
}
```

现在，如果我们不是从`AssertJ`库直接导入，而是从我们自定义断言类`UserAssert`引入`assertThat`方法的话，我们就可以使用新的、更可读的断言。

创建一个这样自定义的断言类看起来很费时间，但是其实几分钟就完成了。我相信，将这些时间投入到创建可读性强的测试代码中是值得的，即使之后它的可读性只有一点点提高。我们编写测试代码就一次，但是之后，很多其他人（包括未来的我）在软件生命周期中，需要阅读、理解然后操作这些代码很多次。

如果你还是觉得很费事，可以看看[断言生成器](https://link.segmentfault.com/?enc=6Va2lCpsFbx9mT0tLHHADQ%3D%3D.Ax%2FQx1zJdcQdGDlMZgpmHenVrKnn3M1XmhF6Eu87J575%2B9mvys51A%2Fcm%2Fz4OSEYt7TWe8HDeh%2Bta7v0URcHYKG9Xpa1rc6vo9XjaDiQWo6M%3D)

## 结论

尽管在测试中启动Spring应用程序也有些理由，但是对于一般的单元测试，它不必要。有时甚至有害，因为更长的周转时间。换言之，我们应该使用更容易支持编写普通单元测试的方式构建Spring实例。

`Spring Boot Test Starter`附带`Mockito`和`AssertJ`作为测试库。让我们利用这些测试库来创建富有表现力的单元测试!


转载自：[https://segmentfault.com/a/1190000040153574](https://segmentfault.com/a/1190000040153574)