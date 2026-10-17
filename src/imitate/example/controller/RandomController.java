package imitate.example.controller;

import imitate.example.service.RandomService;
import imitate.framework.Autowired;
import imitate.framework.CallableMethod;
import imitate.framework.Controller;

/**
 * 随机数控制器：演示无参数命令的绑定与调用。
 *
 * <p>与 {@link CalculateController} 的结构相同：@Controller
 * 负责被扫描成 Bean，@Autowired 负责注入依赖，@CallableMethod 负责把方法暴露成命令。
 * 区别只是方法没有参数，控制台输入 {@code rInt} 即可调用。</p>
 *
 * <p>在 Spring MVC 中，无参接口同样可以写成
 * {@code @GetMapping("/random")}，参数解析器会直接调用无参方法；
 * 返回值会被 {@code HttpMessageConverter} 序列化成响应体。</p>
 */
@Controller
public class RandomController {

    @Autowired
    RandomService randomService;

    /**
     * 输入 {@code rInt} 时调用，返回一个随机整数。
     */
    @CallableMethod("rInt")
    public int randomInt() {
        return randomService.randomInt();
    }

}
