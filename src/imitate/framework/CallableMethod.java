package imitate.framework;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 把某个方法暴露为控制台命令。
 *
 * <p>可以把它类比成 Spring MVC 的 {@code @RequestMapping} / {@code @GetMapping}：
 * value 就是“请求路径”，命令名就是“URL”。例如
 * {@code @CallableMethod("add")} 相当于 {@code @GetMapping("/add")}，
 * 用户在控制台输入 {@code add 1 2}，就像请求了 /add 并携带两个参数。</p>
 *
 * <p>为什么需要 value？因为方法名不一定要暴露给外部，
 * 外部的“调用名”和内部的“方法名”应当解耦；Spring 的路径同样可以与方法名不同，
 * 并且还能通过组合注解、路径变量等方式扩展。</p>
 *
 * <p>本示例要求方法参数都是 double/Double，并在启动时校验
 * （见 {@code ImitateSpringApplication#bindCalling}）。
 * Spring MVC 则通过 {@code HandlerMethodArgumentResolver}、
 * {@code ConversionService}、{@code HttpMessageConverter} 等组件，
 * 把 HTTP 请求中的字符串、JSON 按需转换成方法参数，能力更强也更复杂。</p>
 *
 * <p>注解只是“声明”，真正的命令表登记和反射调用分别发生在
 * {@code ImitateSpringApplication#bindCalling} 与
 * {@code ImitateSpringApplication#start} 的主循环中。</p>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CallableMethod {

    /**
     * 命令名，相当于 Spring MVC 中的请求路径。
     */
    String value();

}
