package imitate.framework;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * “组件”标记注解：被它（直接或间接）标注的类，会被框架扫描并交给容器管理。
 *
 * <p>对应 Spring 的 {@code org.springframework.stereotype.Component}。
 * 在 Spring 中 @Controller、@Service、@Repository、@Configuration 都以 @Component
 * 作为元注解，形成“组合注解”（composed annotation）。
 * 因此扫描时不能只判断 {@code annotation instanceof Component}，
 * 还要判断“这个注解本身是否被 @Component 标注”，
 * 见 {@code ImitateSpringApplication#getComponents}。</p>
 *
 * <p><b>为什么需要 @Retention(RUNTIME)？</b>
 * 扫描发生在应用启动时，必须通过反射读取类上的注解，
 * 因此注解信息要保留到运行期。Spring 对组件注解也有同样的要求。</p>
 *
 * <p><b>Spring 的真实实现：</b>
 * Spring 并不使用 {@code Class#getAnnotations()} 手工遍历，而是用
 * {@code MergedAnnotations} 支持多层组合注解与属性覆盖，用
 * {@code ClassPathScanningCandidateComponentProvider} 完成扫描，
 * 并结合 {@code @ComponentScan} 的 include/exclude 过滤器。
 * Spring 还会在编译期通过 {@code @Indexed} 生成候选组件索引，
 * 启动时直接读取索引文件，避免全量扫描 classpath，提升启动速度。</p>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Component {
}
