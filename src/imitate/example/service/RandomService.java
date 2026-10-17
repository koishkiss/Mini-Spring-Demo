package imitate.example.service;

import imitate.framework.Service;

import java.util.Random;

/**
 * 随机数业务组件。
 *
 * <p>@Service 让这个类被组件扫描发现并交给容器管理，
 * 之后通过 @Autowired 注入到 {@code RandomController}。
 * Controller 只依赖类型，不关心实例是何时、由谁创建的。</p>
 *
 * <p>如果在 Spring 中给这个类加上缓存、事务或监控注解，
 * 容器可以向 Controller 注入一个代理对象，在调用前后插入额外逻辑，
 * 而 Controller 的代码完全不用改——这就是 AOP 与 IoC 配合的价值。</p>
 */
@Service
public class RandomService {

    public int randomInt() {
        return new Random().nextInt();
    }

}
