# mini-spring：一个控制台版的“Spring 原理演示”

用几百行代码，把 Spring 启动时最核心的一条主线摊开：

```text
扫描包 → 找组件 → 实例化 → 注册到容器 → 依赖注入 → 绑定命令 → 循环处理请求
```

它想回答一个初学 Spring 时最常见的问题：

> **为什么在类和方法上加几个注解，程序就能“自动”跑起来？**

答案是：注解本身**不执行任何代码**，它只是类、方法、字段上的元数据。真正让注解“生效”的，是框架在启动时通过**反射**读取这些元数据，再据此完成扫描、实例化、注入和调用。

本项目用一个“控制台命令框架”替代 Web 层：你在控制台输入 `add 1 2`，框架找到对应的 Controller 方法并反射调用，效果类似于访问一个 HTTP 接口。没有 Servlet、没有 Tomcat、没有第三方依赖，只保留理解原理所必需的部分。

## 这个项目演示了什么

| 注解 | 作用 | 对应 Spring |
| --- | --- | --- |
| `@Component` | 标记“交给容器管理” | `@Component` |
| `@Service` | 业务层组件（本质是 `@Component`） | `@Service` |
| `@Controller` | 控制器组件，其中的方法可以被外部调用 | `@Controller` |
| `@Autowired` | 按类型/名称自动注入依赖 | `@Autowired` |
| `@CallableMethod("add")` | 把方法暴露成控制台命令 | `@RequestMapping` / `@GetMapping` |

对应的核心机制：

- **组件扫描**：从启动类所在包开始，递归查找所有 `.class`
- **组合注解**：`@Service`、`@Controller` 本身被 `@Component` 标注，因此也能被识别
- **IoC 容器**：用 `Map<类型, Map<Bean 名称, 实例>>` 保存所有组件
- **依赖注入**：读取 `@Autowired` 字段，先按类型、多个候选再按字段名查找并赋值
- **HandlerMapping**：把“命令名 → Bean + Method”登记成一张表
- **DispatcherServlet 式的主循环**：读入命令、解析参数、反射调用、输出结果、异常兜底

## 前置知识

阅读源码前，建议先掌握下面这些内容。不要求精通，但至少看得懂对应语法。

### 1. Java 基础

- 面向对象：类、接口、继承、多态
- 包（package）与访问修饰符（`public` / `private` / 默认）
- 泛型与集合：`List`、`Map`、`HashMap`、`Enumeration`
- 异常处理：`try/catch`、受检异常

### 2. 注解（Annotation）

- 注解是**元数据**，不是可执行代码
- `@Target` 决定注解能贴在哪里：`TYPE`（类）、`FIELD`（字段）、`METHOD`（方法）、`ANNOTATION_TYPE`（注解类型）
- `@Retention` 决定注解保留到什么时候：
  - `SOURCE`：编译后丢弃
  - `CLASS`：保留在字节码中，但反射读不到（默认值）
  - `RUNTIME`：保留到运行期，**反射可以读取**
- 组合注解：注解可以标注在另一个注解上，例如 `@Controller` 被 `@Component` 标注

> 如果 `@Retention` 不是 `RUNTIME`，`isAnnotationPresent(...)` 永远返回 `false`，框架就“看不见”注解。这是初学者最常踩的坑。

### 3. 反射（Reflection）

本项目的每个核心步骤都依赖反射：

- `Class.forName(...)`：按全限定类名加载类
- `Class#getAnnotations()`：读取类上的注解
- `Class#getDeclaredFields()` / `getDeclaredMethods()`：读取字段和方法
- `Constructor#newInstance()`：创建实例
- `Field#set(...)`：给字段赋值（依赖注入的本质）
- `Method#invoke(...)`：调用方法（命令真正执行的那一步）
- `setAccessible(true)`：访问 `private` 成员

### 4. 类加载器（ClassLoader）

- 知道 `Thread.currentThread().getContextClassLoader().getResources(packagePath)` 能定位包对应的目录
- 知道 `.class` 文件如何还原成全限定类名
- 了解本示例只能扫描“展开的目录”，不能扫描 jar（见“刻意简化”）

### 5. Spring 概念（可先了解，读源码时会不断对应上）

- IoC（控制反转）与 DI（依赖注入）
- Bean、Bean 名称、单例
- `ApplicationContext#refresh()`、BeanDefinition、BeanPostProcessor
- Spring MVC 的 `DispatcherServlet`、`HandlerMapping`、`HandlerMethod`

### 6. 运行环境

- **JDK 25 及以上**
- 项目使用了 JDK 25 的两个便利特性：
  - 实例 main 方法：`void main()`，可以省略 `public static void main(String[] args)`
  - `java.lang.IO`：`IO.println(...)` / `IO.readln()`
- 如果你使用 JDK 21 或更早版本，需要把 `Application` 的入口改成传统写法，并把 `IO.println` 换成 `System.out.println`、`IO.readln` 换成 `Scanner`
- 项目没有 Maven / Gradle，IDE（推荐 IntelliJ IDEA）会直接把 `src` 作为源码目录

## 快速开始

### 方式一：IntelliJ IDEA（推荐）

1. 用 IDEA 打开项目根目录
2. 确认 Project SDK 为 JDK 25（Project Structure → Project）
3. 打开 `src/imitate/example/Application.java`
4. 运行 `Application` 的 `main` 方法
5. 在控制台输入命令：

```text
add 1 2        → 3.0
reduce 5 3     → 2.0
rInt           → 一个随机整数
exit           → 退出
```

### 方式二：命令行（PowerShell）

```powershell
# 编译到 out 目录
javac -encoding UTF-8 -d out (Get-ChildItem -Recurse -Filter *.java src | ForEach-Object { $_.FullName })

# 运行
java -cp out imitate.example.Application
```

### 方式三：命令行（Bash / Git Bash）

```bash
mkdir -p out
javac -encoding UTF-8 -d out $(find src -name "*.java")
java -cp out imitate.example.Application
```

> 启动类必须位于业务代码的父包中。框架以 `Application.class` 所在包为扫描根包，对应 Spring Boot 中 `@SpringBootApplication` 默认扫描启动类所在包及其子包。

## 项目结构

```text
src/
└── imitate/
    ├── framework/                       迷你框架
    │   ├── ImitateSpringApplication.java  容器启动器 + 命令分发主循环
    │   ├── Component.java                 @Component
    │   ├── Service.java                   @Service（组合 @Component）
    │   ├── Controller.java                @Controller（组合 @Component）
    │   ├── Autowired.java                 @Autowired
    │   └── CallableMethod.java            @CallableMethod，类比 @RequestMapping
    └── example/                         使用示例
        ├── Application.java              启动类
        ├── controller/
        │   ├── CalculateController.java  add / reduce 命令
        │   └── RandomController.java     rInt 命令
        └── service/
            ├── CalculateService.java     计算业务
            └── RandomService.java        随机数业务
```

## 启动流程与源码阅读顺序

建议**不要**一上来就从 `ImitateSpringApplication` 第 1 行读到底。按下面的顺序读，每一步都能和 Spring 的概念对应上。

```text
启动类 Application
  ↓
scanPackageClasses 扫描包
  ↓
getComponents 筛选并实例化
  ↓
registerComponents 注册到容器
  ↓
initComponents 依赖注入
  ↓
bindCalling 建立命令映射
  ↓
while 循环: 读取命令
  ↓  ↑
反射调用方法
```

### 第 0 步：看用法

先读 `example` 包，理解“使用者视角”：

- `Application`：启动入口，只调用 `ImitateSpringApplication.start(Application.class)`
- `CalculateController` / `RandomController`：类上 @Controller，字段上 @Autowired，方法上 @CallableMethod
- `CalculateService` / `RandomService`：类上 @Service，内部没有 `new` 任何东西

注意：Controller 里**没有** `new CalculateService()`。对象由谁创建、何时注入，就是框架要回答的问题。

### 第 1 步：看注解定义

读 `framework` 包里的 5 个注解，重点看：

- `@Target` 和 `@Retention(RUNTIME)` 分别写了什么、为什么
- `@Service` / `@Controller` 上为什么有 `@Component`
- 本示例把 `@Component` 的 `@Target` 限制为 `ANNOTATION_TYPE`，所以它只能作为元注解使用

### 第 2 步：看启动主线

打开 `ImitateSpringApplication#start`，对照 `// ===== 第 N 步 =====` 注释顺序阅读。它做了 7 件事：

| 步骤 | 代码 | 对应 Spring |
| --- | --- | --- |
| 0 | 准备 `componentsMap` 等三张表 | 单例池 + Bean 定义表 + HandlerMapping |
| 1 | `scanPackageClasses` | `@ComponentScan` + 类路径扫描 |
| 2 | `getComponents` | 筛选候选组件并实例化 Bean |
| 3 | `registerComponents` | Bean 名称生成 + 注册单例 |
| 4 | `initComponents` | `@Autowired` 依赖注入 |
| 5 | `bindCalling` | `RequestMappingHandlerMapping` 注册 HandlerMethod |
| 6 | `while` 循环 | `DispatcherServlet#doDispatch` 接收并分发请求 |

### 第 3 步：带着问题精读方法

- **`scanPackageClasses` / `recursiveClassSearch`**
  包名如何变成目录？`.class` 文件名如何还原成类名？为什么 `Class.forName` 会触发静态初始化？
- **`getComponents`**
  为什么判断两次 `@Component`？为什么 `@Controller` 也能被识别？为什么必须有无参构造？
- **`registerComponents`**
  Bean 名称怎么生成？为什么需要名称？同名冲突怎么处理？
- **`initComponents`**
  为什么先注册全部组件再统一注入？按类型找不到/找到多个分别怎么办？`setAccessible(true)` 在做什么？
- **`bindCalling`**
  为什么只扫描 `@Controller`？命令名重复为什么要在启动期报错？为什么参数只允许 `double`？
- **`start` 的 `while` 循环**
  字符串如何变成方法参数？`Method#invoke` 前后分别发生了什么？单个命令失败为什么不会退出程序？

### 第 4 步：动手验证（强烈建议）

在关键位置打断点或加日志：

1. 在 `getComponents` 打断点，观察扫描到了哪些类、哪些被实例化
2. 在 `initComponents` 观察 `field.set(...)` 前后字段是否为 `null`
3. 在 `method.invoke(...)` 观察 `component` 是哪个对象、`callingParam` 是什么类型
4. 把 `@Service` 改成 `@Controller`，看看 `rInt` 是否被意外暴露
5. 再新建一个 `CalculateService` 子类并标上 `@Service`，观察注入时如何按字段名兜底

## “注解为什么能跑起来”一句话总结

1. 注解提供**声明**：这个类是组件、这个字段要注入、这个方法可以被调用
2. 框架启动时用反射**读取声明**
3. 框架根据声明**创建对象、建立映射、完成赋值**
4. 请求到来时，框架查表找到方法，再用 `Method#invoke` **执行真正的业务代码**

Spring 看起来“加个注解就行”，是因为上述“脏活累活”被启动流程和大量反射调用封装起来了。本项目只是把这层封装拆开给你看。

## 与真实 Spring 的对照

| 本项目 | Spring / Spring Boot | 说明 |
| --- | --- | --- |
| `ImitateSpringApplication.start` | `SpringApplication.run` + `refresh()` | 真实流程包含环境准备、后置处理器、事件、生命周期等大量阶段 |
| `scanPackageClasses` | `ClassPathScanningCandidateComponentProvider` | Spring 支持文件系统与 jar，并支持过滤器、索引 |
| 直接 `newInstance()` | `BeanDefinition` + `InstantiationStrategy` | Spring 先登记定义、再按需创建，支持构造器推断、作用域、懒加载 |
| `componentsMap` | `DefaultListableBeanFactory` | 真实容器维护 Bean 定义表、单例池、类型索引、三级缓存等 |
| `initComponents` | `AutowiredAnnotationBeanPostProcessor` | 支持字段/方法/构造器注入、`@Qualifier`、`@Primary`、循环依赖处理 |
| `bindCalling` | `RequestMappingHandlerMapping` | 支持路径变量、HTTP 方法、参数解析、返回值处理 |
| `while` + 字符串解析 | `DispatcherServlet#doDispatch` | 真实请求来自 Servlet 容器，参数解析由 `HandlerMethodArgumentResolver` 完成 |
| `@CallableMethod` | `@RequestMapping` / `@GetMapping` | 本示例只支持固定命令名和无参/`double` 参数 |
| 控制台输出 | `HttpMessageConverter` | 真实框架把返回值序列化成 JSON/HTML 等响应体 |

## 刻意简化（以及为什么）

这是一个教学项目，以下简化都是为了突出主干：

- **只支持展开目录，不支持 jar**：`getResources` 拿到的是 `file:` 路径，打包成 jar 后无法这样遍历
- **只支持单例**：扫描到就立即创建，没有作用域概念
- **只支持无参构造**：没有构造器注入和 `@Bean` 工厂方法
- **只支持字段注入**：没有构造器注入、Setter 注入，也不忽略 `static` 字段
- **没有 AOP**：注入的是原始对象，没有事务、日志、缓存代理
- **没有 Bean 生命周期回调**：没有 `@PostConstruct`、`InitializingBean`、`BeanPostProcessor`
- **没有循环依赖处理**：A 依赖 B、B 依赖 A 时会注入失败
- **参数只支持 `double` / `Double`**：没有通用的类型转换
- **扫描会触发静态初始化**：`Class.forName` 会执行 `static` 块，真实框架通常会避免
- **异常处理很粗糙**：统一打印错误信息，没有分层异常体系

> 这些简化**不适合生产环境**。它们的目的只有一个：让你在不被细节淹没的前提下，看清 IoC / DI / MVC 的主干。

## 常见问题

**Q：为什么启动类必须放在父包？**
框架以启动类所在包为扫描根包。如果启动类在 `imitate.example`，而业务代码在 `imitate.other`，就扫描不到，对应 Spring Boot 的默认行为。

**Q：为什么组件没有被扫描到？**
检查三点：类是否在启动类所在包或子包下；注解是否是 `@Component` 或其组合注解；注解的 `@Retention` 是否为 `RUNTIME`。

**Q：为什么注入失败？**
检查字段类型是否在容器中有对应 Bean、字段名是否与 Bean 名称一致（同类型多个候选时）、类是否被成功扫描和注册。

**Q：为什么输入命令提示“未定义”？**
只有 `@Controller` 类中带 `@CallableMethod` 的方法才会被登记；命令名以注解的 `value` 为准，不是方法名。

**Q：为什么参数只能传数字？**
本示例为了简单，假定所有参数都是 `double`，并在启动时校验。真实 Spring MVC 通过参数解析器和 `ConversionService` 支持各种类型。

**Q：`IO.println` / `void main()` 报错？**
需要 JDK 25+。旧版本请改成 `System.out.println` 和 `public static void main(String[] args)`。

## 建议的练习

按顺序完成，能覆盖大部分核心机制：

1. 新增一个 `@Service` 和对应的 `@Controller`，实现一个 `multiply` 命令
2. 增加 `@CallableMethod` 的参数校验，让某条命令在参数个数不对时给出明确错误
3. 支持 `@Component("customName")` 自定义 Bean 名称
4. 增加一个 `@Autowired` 的构造器注入版本，对比字段注入
5. 实现一个极简的 `@PostConstruct`：Bean 创建并注入后自动调用
6. 给命令增加“帮助”命令，列出所有已注册的命令
7. 尝试用动态代理实现一个简单的 AOP：在 `@CallableMethod` 调用前后打印耗时
8. （进阶）把扫描器改为读取 jar 中的 `.class`，理解 Spring 为什么需要 `PathMatchingResourcePatternResolver`

## 最后

Spring 并不神秘，它只是把“读注解、建对象、填依赖、找方法、做调用”这些步骤做得非常完整、健壮且可扩展。希望这个项目能帮你把“加注解就能跑”从一句口号，变成一条可以逐行追踪的调用链。
