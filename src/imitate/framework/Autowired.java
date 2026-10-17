package imitate.framework;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 自动注入注解，用来告诉框架：“这个字段需要的对象，请帮我从容器里找出来并赋值”。
 *
 * <p>对应 Spring 的 {@code org.springframework.beans.factory.annotation.Autowired}。
 * 在 Spring 中它由 {@code AutowiredAnnotationBeanPostProcessor} 解析，
 * 在 Bean 实例化之后、初始化之前完成字段/方法/构造器注入。</p>
 *
 * <p><b>为什么只加注解不会“自动注入”？</b>
 * 注解只是贴在代码上的“元数据”，JVM 不会因为看到 {@code @Autowired}
 * 就执行任何逻辑。真正完成注入的是框架启动时通过反射读取该注解、
 * 查找对应 Bean、再调用 {@link java.lang.reflect.Field#set(Object, Object)}
 * 赋值的那段代码（见 {@code ImitateSpringApplication#initComponents}）。
 * Spring 也是如此：注解负责“声明意图”，框架负责“解释并执行”。</p>
 *
 * <p><b>为什么 @Retention 必须是 RUNTIME？</b>
 * 只有保留到运行期的注解才能被反射读取。若写成 CLASS 或 SOURCE，
 * {@code field.isAnnotationPresent(Autowired.class)} 永远为 false，
 * 框架就“看不见”这个注解，注入自然也不会发生。这是初学者最常见的坑之一。</p>
 *
 * <p><b>为什么 @Target 是 FIELD？</b>
 * 本示例只演示字段注入，所以把可标注位置限制为字段，让错误用法在编译期就报错。
 * Spring 的 @Autowired 还支持 CONSTRUCTOR、METHOD、PARAMETER、ANNOTATION_TYPE，
 * 分别用于构造器注入、Setter/普通方法注入和自定义组合注解。
 * Spring 官方更推荐构造器注入：依赖关系在对象创建时就完整，且便于单元测试。</p>
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Autowired {
}
