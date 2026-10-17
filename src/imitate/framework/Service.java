package imitate.framework;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 业务层组件注解。
 *
 * <p>对应 Spring 的 {@code org.springframework.stereotype.Service}。
 * 它本身没有额外语义，本质是 @Component 的“特化”，
 * 用于表达“这个类属于业务层”，让代码层次更清晰；
 * 扫描、实例化、注入流程与普通 @Component 完全一致。</p>
 *
 * <p><b>为什么 Spring 还要区分 Controller / Service / Repository？</b>
 * 一是语义化，便于阅读和按层做统一处理；
 * 二是部分注解带有附加能力，例如 @Repository 会把持久层异常转换为
 * Spring 的统一 DataAccessException，@Controller 会被 MVC 识别为处理器。
 * 本例中 @Service 与 @Controller 的差别仅在于：@Controller
 * 会被 {@code ImitateSpringApplication#bindCalling} 选中，@Service 不会。</p>
 */
@Component
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Service {
}
