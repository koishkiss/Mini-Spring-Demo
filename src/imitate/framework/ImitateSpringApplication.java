package imitate.framework;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.net.URL;
import java.util.*;

/**
 * 迷你版 Spring 容器 / MVC 启动器。
 *
 * <p>本类把 Spring 启动时最核心的几件事压缩在了一条主线里：</p>
 * <pre>
 *   扫描包 → 找出组件 → 实例化 → 注册到容器 → 依赖注入 → 绑定调用 → 循环处理请求
 * </pre>
 *
 * <p>与 Spring 的对应关系：</p>
 * <ul>
 *   <li>{@link #scanPackageClasses} ≈ {@code @ComponentScan} +
 *       {@code ClassPathScanningCandidateComponentProvider}</li>
 *   <li>{@link #getComponents} ≈ 解析 BeanDefinition 并实例化 Bean</li>
 *   <li>{@link #registerComponents} ≈ {@code DefaultListableBeanFactory}
 *       中的 Bean 定义表 / 单例表</li>
 *   <li>{@link #initComponents} ≈ {@code AutowiredAnnotationBeanPostProcessor}
 *       完成依赖注入</li>
 *   <li>{@link #bindCalling} ≈ {@code RequestMappingHandlerMapping}
 *       建立“请求路径 → HandlerMethod”的映射</li>
 *   <li>{@link #start} 中的 while 循环 ≈ {@code DispatcherServlet#doDispatch}
 *       接收请求、找到处理器、反射调用、输出结果</li>
 * </ul>
 *
 * <p><b>核心结论：注解为什么能“跑起来”？</b>
 * 注解本身不执行任何代码，它只是类、方法、字段上的元数据。
 * 必须有框架在启动时读取这些元数据，据此完成扫描、实例化、注入和方法调用。
 * Spring 看起来“加个注解就行”，是因为这些“脏活累活”被框架启动流程
 * 和大量反射调用封装起来了；本类就是把这层封装摊开给读者看。</p>
 *
 * <p><b>本实现为了教学做的简化：</b>
 * 只支持普通目录下的 .class 扫描（不支持 jar）、只支持单例、只支持字段注入、
 * 只支持无参构造、没有 AOP 代理和 Bean 生命周期回调、参数只支持 double。
 * 每处简化在对应方法中都会说明 Spring 的真实做法。</p>
 */
public class ImitateSpringApplication {

    public static void start(Class<?> clazz) {
        // ===== 第 0 步：准备容器 =====
        // 组件表：类型 → (Bean 名称 → 实例)。
        // 相当于 Spring 的“单例池 + 类型索引”：DefaultSingletonBeanRegistry /
        // DefaultListableBeanFactory 同时维护 beanName → 实例、类型 → beanName 等索引，
        // 这样才能支持按类型注入和按名称获取。
        // 这里用嵌套 Map 是简化写法：先按类型缩小范围，再按名称精确查找。
        Map<Class<?>, Map<String,Object>> componentsMap = new HashMap<>();

        // 控制台调用对象映射：命令名 → 处理该命令的 Bean。
        // 类比 Spring MVC 的 HandlerMapping：URL → Controller 实例。
        Map<String, Object> componentsCallingMap = new HashMap<>();

        // 控制台调用方法映射：命令名 → 处理方法。
        // 与上面成对出现，合起来相当于 Spring 的 HandlerMethod
        // （HandlerMethod = Bean + Method 的封装）。
        Map<String, Method> methodsCallingMap = new HashMap<>();

        // 初始化应用。
        // Spring 中对应 AbstractApplicationContext#refresh() 的核心阶段：
        // 扫描 BeanDefinition、实例化单例、完成依赖注入。
        // refresh() 里还包含环境准备、后置处理器注册、事件发布等大量步骤，
        // 这里只保留理解“注解如何生效”所必需的部分。
        try {
            // 以启动类所在包为扫描根包。
            // 对应 Spring Boot 中 @SpringBootApplication 隐含的
            // @ComponentScan(basePackages = 启动类所在包)。
            // 这也是为什么启动类通常要放在所有业务代码的父包下。
            String packageName = clazz.getPackage().getName();

            // 1. 扫描：把包下所有类加载进来。
            List<Class<?>> classes = scanPackageClasses(packageName);

            // 2. 筛选并实例化：只保留被 @Component 体系标注的类。
            List<Object> components = getComponents(classes);

            // 3. 注册：给每个实例生成 Bean 名称并放进容器。
            registerComponents(components, componentsMap);

            // 4. 注入：处理 @Autowired 字段。
            // 注意必须先完成“全部注册”再统一注入，否则可能找不到
            // 声明顺序在后面的组件。Spring 也是先注册 BeanDefinition，
            // 再在创建 Bean 时填充属性，只是它支持按需创建和循环依赖处理。
            initComponents(components, componentsMap);

            // 5. 绑定调用：把 @CallableMethod 方法登记成命令。
            bindCalling(components, componentsCallingMap, methodsCallingMap);
        } catch (Exception e) {
            // 启动期错误直接终止。
            // Spring 在 refresh() 失败时会销毁已创建的单例并抛出
            // BeanCreationException / ApplicationContextException，
            // 保证“要么完整启动，要么不启动”，避免容器处于半初始化状态。
            IO.println("【启动失败】" + e.getMessage());
            return;
        }

        // ===== 第 6 步：运行期，循环接收并处理命令 =====
        // 对应 Spring MVC 的 DispatcherServlet#doDispatch：
        // 等待请求 → 解析请求 → 查找 Handler → 参数转换 → 反射调用 → 返回响应。
        // 控制台在这里就是“Servlet 容器 + HTTP 协议”的替代品。
        while (true) {
            try {
                String line = IO.readln();
                if (line == null || line.isBlank()) continue;

                // 简单的请求解析：命令名 + 空格分隔的参数。
                // 类比 HTTP 请求行/请求参数解析，Spring 由 Servlet 容器和
                // DispatcherServlet 完成，并支持路径变量、查询参数、请求体等。
                String[] cmd = line.split(" ");

                String callingName = cmd[0];
                // 特殊命令，类比优雅停机 / 关闭 ApplicationContext。
                if (callingName.equals("exit")) break;
                if (!methodsCallingMap.containsKey(callingName)) throw new RuntimeException("函数 " + callingName + " 未定义！");

                // 根据命令名找到对应的 Bean 和方法。
                // 这就是 HandlerMapping 的作用：把外部请求定位到具体处理器。
                Object component = componentsCallingMap.get(callingName);
                Method method = methodsCallingMap.get(callingName);

                // 参数转换：把控制台字符串转成方法需要的 double。
                // 类比 Spring MVC 的 HandlerMethodArgumentResolver + ConversionService：
                // 它们负责把字符串/JSON 转换成方法参数类型；类型不匹配会返回 400。
                // 本示例为了简单，假定所有参数都是 double。
                Object[] callingParam = new Object[cmd.length - 1];
                for (int i = 1; i < cmd.length; i++) callingParam[i - 1] = Double.valueOf(cmd[i]);

                // 反射调用目标方法——注解真正“生效”的最后一环。
                // 前面所有扫描、注册、注入都是在准备“谁来处理、依赖是谁”，
                // 到这里才通过 Method#invoke 执行真正的业务代码。
                // Spring MVC 最终也是通过反射（或 CGLIB 等方法句柄）调用 Controller 方法。
                Object result = method.invoke(component, callingParam);

                // 输出返回值，类比把方法返回值序列化为 HTTP 响应体（@ResponseBody）。
                IO.println(result);
            } catch (Exception e) {
                // 单个请求失败不影响整个应用，类比 MVC 的异常处理器
                // （HandlerExceptionResolver）把异常转换成错误响应。
                IO.println("【执行失败】" + e.getMessage());
            }
        }
    }


    /**
     * 扫描指定包及其子包下的所有类。
     *
     * <p>对应 Spring 的组件扫描（@ComponentScan）。Spring 的扫描器
     * {@code ClassPathScanningCandidateComponentProvider} 会遍历 classpath，
     * 读取类的元数据并应用 include/exclude 过滤器，只保留候选组件，
     * 而不是把所有类都加载成 Class 对象。</p>
     *
     * <p><b>本实现的简化与原因：</b>
     * 用 ClassLoader 的 {@code getResources(packagePath)} 找到包对应的目录，
     * 再递归遍历 .class 文件。它只能处理“展开的目录”（IDE 运行、target/classes），
     * 无法处理打包后的 jar。Spring 使用 {@code PathMatchingResourcePatternResolver}
     * 同时支持文件系统和 jar（jar:file: 协议），所以打成 jar 也能扫描。</p>
     */
    private static List<Class<?>> scanPackageClasses(String packageName) throws IOException, ClassNotFoundException {
        // 把包名 imitate.example 转成路径 imitate/example，供类加载器查找资源。
        String packagePath = packageName.replace('.', '/');

        List<Class<?>> classList = new ArrayList<>();
        // getResources 可能返回多个目录（同一包名出现在多个 classpath 位置），
        // 因此需要用 Enumeration 逐个处理。
        Enumeration<URL> resources = Thread.currentThread().getContextClassLoader().getResources(packagePath);
        while (resources.hasMoreElements()) {
            File directory = new File(resources.nextElement().getFile());
            classList.addAll(recursiveClassSearch(directory, packageName));
        }

        return classList;
    }

    /**
     * 递归遍历目录，把 .class 文件名还原成全限定类名后加载。
     *
     * <p>文件名去掉结尾的 ".class"（6 个字符）后拼上包名，
     * 就得到 {@code Class.forName} 需要的形式，例如
     * {@code imitate.example.service.CalculateService}。</p>
     *
     * <p><b>与 Spring 的差异：</b>
     * 这里的 {@code Class.forName} 会触发类的静态初始化（执行 static 块），
     * 扫描阶段本不该产生业务副作用。Spring 的扫描通常只读取字节码元数据
     * （ASM）或使用不初始化类的加载方式，真正的实例化推迟到创建 Bean 时；
     * 这样既避免副作用，也更快。本例为了直观使用了 Class.forName。</p>
     */
    private static List<Class<?>> recursiveClassSearch(File directory, String packageName) throws ClassNotFoundException {
        List<Class<?>> classes = new ArrayList<>();

        if (!directory.exists()) return classes;

        File[] files = directory.listFiles();
        if (files == null) return classes;

        for (File file : files) {
            if (file.isDirectory()) {
                // 子目录就是子包，包名追加目录名后继续递归，
                // 对应 @ComponentScan 默认递归扫描子包的行为。
                String subPackage = packageName + '.' + file.getName();
                classes.addAll(recursiveClassSearch(file, subPackage));
            } else if (file.getName().endsWith(".class")) {
                // 文件名 → 全限定类名，例如 CalculateService.class
                // → imitate.example.service.CalculateService。
                String className = packageName + '.' + file.getName().substring(0, file.getName().length() - 6);
                classes.add(Class.forName(className));
            }
        }

        return classes;
    }

    /**
     * 从扫描到的类中筛选出“组件”，并为每个组件创建实例。
     *
     * <p>判断规则有两条：</p>
     * <ol>
     *   <li>类上直接标了 @Component；</li>
     *   <li>类上的注解本身被 @Component 标注（如 @Controller、@Service）。</li>
     * </ol>
     *
     * <p>第二条正是“组合注解”的原理：@Controller 并不是 @Component 的子类，
     * Java 注解之间也没有继承关系，但注解可以标注在另一个注解上。
     * 框架读取 @Controller 后，再检查 @Controller 这个“注解类型”上有没有 @Component，
     * 从而把 @Controller 当作组件标记。Spring 的 {@code MergedAnnotations}
     * 把这套查找做得更完整，支持任意层级的组合、元注解属性覆盖和 @AliasFor。</p>
     *
     * <p><b>为什么这里直接 new 出实例？</b>
     * Spring 会把“Bean 定义（BeanDefinition）”和“Bean 实例”分开：
     * 扫描阶段只登记定义（类型、作用域、构造参数等），实例化推迟到需要时，
     * 并由 {@code InstantiationStrategy} 选择构造器、由 BeanPostProcessor
     * 决定是否生成代理。本例省略了定义阶段，扫描到就立即实例化，
     * 所以只能支持无参构造和单例。</p>
     */
    private static List<Object> getComponents(List<Class<?>> classes) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        List<Object> components = new ArrayList<>();
        for (Class<?> clazz : classes) {
            // getAnnotations() 只返回直接标注在类上的注解（以及 @Inherited 的父类注解），
            // 不会自动展开“注解上的注解”，所以下面需要手工再判断一层。
            Annotation[] annotations = clazz.getAnnotations();
            for (Annotation annotation : annotations) {
                // 情况一：直接标注 @Component。
                // 情况二：注解类型本身带 @Component，例如 @Controller、@Service。
                if (annotation instanceof Component || annotation.annotationType().isAnnotationPresent(Component.class)) {
                    // 创建组件。
                    // Spring 默认也要求无参构造（或唯一构造器），再通过反射实例化；
                    // 有参构造/工厂方法需要 @Autowired 构造器或 @Bean 方法配合。
                    Constructor<?> componentConstructor = clazz.getConstructor();
                    Object component = componentConstructor.newInstance();
                    components.add(component);
                }
            }
        }
        return components;
    }

    /**
     * 把实例注册到容器，并生成 Bean 名称。
     *
     * <p>Bean 名称规则：简单类名首字母小写，例如
     * {@code CalculateService → calculateService}。
     * 这与 Spring 默认的 {@code AnnotationBeanNameGenerator} 一致
     * （内部使用 {@code Introspector.decapitalize}），
     * Spring 也允许通过 {@code @Component("customName")} 显式指定名称。</p>
     *
     * <p><b>为什么需要 Bean 名称？</b>
     * 名称是容器中的唯一标识：按名称获取（getBean("xxx")）、
     * 按名称注入（@Qualifier）、同类型多实例时区分彼此都依赖它。
     * 本实现用“类型 → 名称 → 实例”的嵌套表同时支持按类型和按名称查找。</p>
     *
     * <p><b>Spring 的真实结构：</b>
     * {@code DefaultListableBeanFactory} 用 {@code beanDefinitionMap}
     * 保存名称 → BeanDefinition，用 {@code singletonObjects} 保存名称 → 单例实例，
     * 另外维护类型到名称的缓存来加速按类型查找。出现同名 Bean 时，
     * Spring Boot 2.1+ 默认禁止覆盖并抛出异常，与本处行为一致；
     * 可通过配置允许覆盖。</p>
     *
     * <p><b>本实现的简化：</b>
     * 这里按“类型”分组后再判重，不同包下的同名类不会冲突；
     * Spring 的 Bean 名称在整个容器范围内全局唯一，冲突判断更严格。</p>
     */
    private static void registerComponents(List<Object> components, Map<Class<?>,Map<String,Object>> componentsMap) {
        for (Object component : components) {
            // 组件类型。
            Class<?> componentType = component.getClass();

            // 组件名称：简单类名，首字母小写。
            String componentName = componentType.getName();
            componentName = componentName.substring(componentName.lastIndexOf('.') + 1);
            componentName = Character.toLowerCase(componentName.charAt(0)) + componentName.substring(1);

            // 加入到组件表。
            Map<String,Object> map = componentsMap.get(componentType);
            if (map == null) map = new HashMap<>();
            if (map.containsKey(componentName)) throw new RuntimeException("存在多个同名组件：" + componentName);
            map.put(componentName, component);
            componentsMap.put(componentType, map);
        }
    }

    /**
     * 处理 @Autowired 字段注入。
     *
     * <p>Spring 的注入由 {@code AutowiredAnnotationBeanPostProcessor}
     * 在 Bean 实例化后、初始化方法（@PostConstruct）前完成，
     * 本质也是反射读取字段上的注解，再用 {@code Field#set} 赋值。
     * 依赖查找逻辑位于 {@code DefaultListableBeanFactory#doResolveDependency}。</p>
     *
     * <p>查找顺序：</p>
     * <ol>
     *   <li>先按字段类型找所有候选 Bean；</li>
     *   <li>只有一个候选 → 直接注入（等价 Spring 的 byType）；</li>
     *   <li>有多个候选 → 用字段名作为 Bean 名称再找（等价 Spring 的 byName 兜底）。</li>
     * </ol>
     *
     * <p>Spring 的真实规则更细致：按类型匹配到多个时，会继续考虑
     * {@code @Primary}、{@code @Priority}、{@code @Qualifier}，
     * 仍无法唯一确定才抛 {@code NoUniqueBeanDefinitionException}。
     * 本示例用“字段名 = Bean 名称”简化了 @Qualifier 的作用。</p>
     *
     * <p><b>为什么先创建全部实例、再统一注入？</b>
     * 因为 A 依赖 B 时，B 可能还没被扫描/创建。两阶段处理可以避免顺序问题。
     * Spring 更进一步：它允许按需创建依赖，并用三级缓存解决单例循环依赖；
     * 但构造器循环依赖仍然无法解决，会直接报错。</p>
     */
    private static void initComponents(List<Object> components, Map<Class<?>,Map<String,Object>> componentsMap) throws IllegalAccessException {
        for (Object component : components) {
            Class<?> componentType = component.getClass();
            // getDeclaredFields 能拿到本类声明的所有字段，包括 private；
            // 父类字段不在其中（Spring 会沿继承链向上查找）。
            Field[] fields = componentType.getDeclaredFields();
            for (Field field : fields) {
                // 判断是否自动注入组件。没有 @Autowired 的字段保持原样。
                if (!field.isAnnotationPresent(Autowired.class)) continue;

                // 获取对应组件：先按字段类型查找。
                Class<?> fieldType = field.getType();
                Map<String, Object> map = componentsMap.get(fieldType);
                if (map == null || map.isEmpty()) throw new RuntimeException("未找到类型为 " + fieldType + " 的组件！");

                // 同类型只有一个实例时直接用它；有多个时再用字段名匹配 Bean 名称。
                // 这对应 Spring 的“先 byType，多个候选再 byName/@Qualifier”。
                String fieldName = field.getName();
                Object autowiredComponent = map.size() == 1 ? map.values().iterator().next() : map.get(fieldName);
                if (autowiredComponent == null) throw new RuntimeException("未找到名称为 " + fieldName + " 的组件！");

                // 自动注入。
                // private 字段默认不可写，setAccessible(true) 临时关闭访问检查；
                // Spring 内部通过 ReflectionUtils.makeAccessible 做同样的事。
                // 注意：本示例未排除 static/final 字段，Spring 会忽略 static 字段的 @Autowired。
                field.setAccessible(true);
                field.set(component, autowiredComponent);
            }
        }
    }

    /**
     * 建立“命令名 → Bean + 方法”的调用映射。
     *
     * <p>这一步对应 Spring MVC 启动时的 {@code RequestMappingHandlerMapping}：
     * 它扫描所有 @Controller Bean，找出带 @RequestMapping（及其组合注解）的方法，
     * 解析路径、HTTP 方法、参数、返回值等信息，注册到 HandlerMapping 中。
     * 运行期 DispatcherServlet 拿到请求后，就是在这里查表找到 HandlerMethod。</p>
     *
     * <p>为什么只扫描 @Controller？如果所有 Bean 的 public 方法都能被外部调用，
     * 会把 Service、内部工具方法也暴露出去，破坏分层与安全边界。
     * Spring MVC 同样只把 @Controller 中显式映射的方法暴露为 HTTP 端点。</p>
     *
     * <p><b>简化点：</b>
     * {@code getDeclaredMethods()} 不包含父类方法；Spring 使用
     * {@code ReflectionUtils.getAllDeclaredMethods} 连同父类一起查找。
     * 本示例只支持固定参数类型，Spring 则由参数解析器支持路径变量、
     * 查询参数、请求体、文件等，并通过 ConversionService 做类型转换。</p>
     */
    private static void bindCalling(List<Object> components, Map<String, Object> componentsCallingMap, Map<String, Method> methodsCallingMap) {
        for (Object component : components) {
            // 只处理控制器：对应 MVC 只扫描 @Controller/@RestController。
            if (!component.getClass().isAnnotationPresent(Controller.class)) continue;
            Method[] methods = component.getClass().getDeclaredMethods();
            for (Method method : methods) {
                // 判断方法是否为控制台调用：对应查找 @RequestMapping 等映射注解。
                CallableMethod callableMethod = method.getAnnotation(CallableMethod.class);
                if (callableMethod == null) continue;

                // 判断是否存在重复的控制台调用。
                // Spring MVC 中两个方法映射到同一路径会抛
                // IllegalStateException: Ambiguous mapping，思路完全一致。
                String callingName = callableMethod.value();
                if (methodsCallingMap.containsKey(callingName)) throw new RuntimeException("函数 " + callingName + " 重复定义！");

                // 检查参数列表（仅允许浮点数）。
                // 本示例在启动期就校验，属于“快速失败”；
                // Spring MVC 的处理器方法参数类型更自由，由参数解析器在运行期处理，
                // 无法解析时会返回 400/415 等错误。
                for (Parameter parameter : method.getParameters())
                    if (parameter.getType() != double.class && parameter.getType() != Double.class)
                        throw new RuntimeException("函数 " + callingName + " 存在非法参数类型！");

                // 绑定控制台调用。
                // 同时保存 Bean 和 Method，相当于 Spring 的 HandlerMethod；
                // 调用前需要 setAccessible，否则非 public 方法无法反射执行。
                method.setAccessible(true);
                componentsCallingMap.put(callingName, component);
                methodsCallingMap.put(callingName, method);
            }
        }
    }

}
