package imitate.framework;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 控制器组件注解：既是“组件”，又标记这个类负责接收外部调用。
 *
 * <p>对应 Spring MVC 的 {@code org.springframework.stereotype.Controller}。
 * 关键点：它被 {@link Component} 标注，所以会被组件扫描当普通 Bean 创建；
 * 同时框架在绑定调用时会额外判断 @Controller，
 * 只从这类 Bean 中查找可调用的方法（见
 * {@code ImitateSpringApplication#bindCalling}）。</p>
 *
 * <p>这与 Spring 的分层是同一思路：所有层（Controller/Service/Repository）
 * 首先都是 Bean；在此基础上，@Controller 又给 MVC 的 HandlerMapping
 * 提供了识别依据。Spring 启动时由 {@code RequestMappingHandlerMapping}
 * 扫描所有 @Controller，把 @RequestMapping 方法注册成
 * “请求 → 处理器方法”的映射。</p>
 *
 * <p><b>为什么 Spring 不直接用 @Component 处理请求？</b>
 * 单一职责：@Component 只表达“交给容器管理”，而“接收 HTTP 请求”
 * 是 Web 层的能力，需要额外的映射、参数解析、返回值处理。
 * 用 @Controller 单独标记，可以让 MVC 只扫描它关心的 Bean，
 * 避免把 Service 的内部方法误暴露成接口。</p>
 */
@Component
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Controller {
}
