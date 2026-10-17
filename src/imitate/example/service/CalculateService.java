package imitate.example.service;

import imitate.framework.Service;

/**
 * 计算业务组件。
 *
 * <p>@Service 是 @Component 的组合注解，因此会被扫描、实例化并注册到容器，
 * 再注入到需要它的 Controller 中。它本身不感知“自己被谁使用”，
 * 也不负责创建自己——这两件事都交给了容器。</p>
 *
 * <p>在真实 Spring 中，业务层通常还会叠加 @Transactional。
 * 此时容器注入到 Controller 的往往不是这个类的原始实例，而是 AOP 生成的代理：
 * 代理在方法调用前后开启/提交事务。本示例没有 AOP，
 * 所以注入的就是普通实例，方便读者先看清 IoC 的主干。</p>
 */
@Service
public class CalculateService {

    public double add(double a, double b) {
        return a + b;
    }

    public double reduce(double a, double b) {
        return a - b;
    }

}
