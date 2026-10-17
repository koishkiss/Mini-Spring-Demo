package imitate.example.controller;

import imitate.example.service.CalculateService;
import imitate.framework.Autowired;
import imitate.framework.CallableMethod;
import imitate.framework.Controller;

/**
 * 计算器控制器：负责把控制台命令转发给业务层。
 *
 * <p>三个注解分别对应三件由框架完成的事：</p>
 * <ul>
 *   <li>@Controller：被组件扫描发现，创建为 Bean（它本身是 @Component 的组合注解）；</li>
 *   <li>@Autowired：由容器把 CalculateService 实例注入到字段中；</li>
 *   <li>@CallableMethod：把方法登记为“命令名 → 方法”的映射，供外部调用。</li>
 * </ul>
 *
 * <p>与 Spring MVC 的对照：</p>
 * <pre>
 * &#64;RestController
 * public class CalculateController {
 *     &#64;Autowired CalculateService calculateService;
 *
 *     &#64;GetMapping("/add")
 *     public double add(&#64;RequestParam double a, &#64;RequestParam double b) {
 *         return calculateService.add(a, b);
 *     }
 * }
 * </pre>
 *
 * <p>这里没有 {@code new CalculateService()}，控制器只声明“我需要它”。
 * 对象的创建和装配由容器负责，这就是控制反转（IoC）；
 * 面向类型编程让替换实现时无需修改调用方，
 * 也为 Spring 后续生成 AOP 代理（事务、日志等）留下了空间。</p>
 */
@Controller
public class CalculateController {

    // 字段名 calculateService 同时也是按名称查找时的候选 Bean 名称。
    // 本示例中 CalculateService 只有一个实例，因此按类型即可唯一确定。
    @Autowired
    CalculateService calculateService;

    /**
     * 控制台输入 {@code add 1 2} 时，框架会把两个参数转成 double，
     * 再通过反射调用本方法；返回值直接打印出来。
     */
    @CallableMethod("add")
    public double add(double a, double b) {
        return calculateService.add(a, b);
    }

    /**
     * 控制台输入 {@code reduce 5 3} 时调用，结果为 2.0。
     */
    @CallableMethod("reduce")
    public double reduce(double a, double b) {
        return calculateService.reduce(a, b);
    }

}
