package imitate.example;

import imitate.framework.ImitateSpringApplication;

/**
 * 示例启动类。
 *
 * <p>把它类比成 Spring Boot 的启动类：</p>
 * <pre>
 * &#64;SpringBootApplication
 * public class Application {
 *     public static void main(String[] args) {
 *         SpringApplication.run(Application.class, args);
 *     }
 * }
 * </pre>
 *
 * <p>传入 {@code Application.class} 不只是为了“调用一下”：
 * 框架通过它拿到所在包，从该包开始扫描组件，
 * 对应 Spring Boot 中 {@code @SpringBootApplication} 默认扫描启动类所在包
 * 及其子包的行为。所以启动类要放在业务代码的父包下。</p>
 *
 * <p>启动后可在控制台输入：</p>
 * <pre>
 * add 1 2      → 3.0
 * reduce 5 3   → 2.0
 * rInt         → 随机整数
 * exit         → 退出
 * </pre>
 */
public class Application {

    void main() {
        // 启动迷你容器：扫描 → 实例化 → 注册 → 注入 → 绑定命令。
        // 对应 SpringApplication.run(...)，后者内部会创建 ApplicationContext、
        // 调用 refresh()，完成 Bean 的创建与 MVC 基础设施的初始化。
        ImitateSpringApplication.start(Application.class);
    }

}
