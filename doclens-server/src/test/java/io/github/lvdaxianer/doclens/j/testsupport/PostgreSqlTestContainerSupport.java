package io.github.lvdaxianer.doclens.j.testsupport;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * PostgreSQL 测试容器支持。
 *
 * @author lvdaxianer@yeah.net
 * @date 2026-07-14
 */
public final class PostgreSqlTestContainerSupport {

    /** PostgreSQL 测试镜像名称。 */
    private static final DockerImageName POSTGRESQL_IMAGE = DockerImageName.parse("postgres:16-alpine");
    /** PostgreSQL 测试用户名。 */
    private static final String USERNAME = "doclens";
    /** PostgreSQL 测试密码。 */
    private static final String PASSWORD = "doclens";
    /** PostgreSQL JDBC 驱动类名。 */
    private static final String DRIVER_CLASS_NAME = "org.postgresql.Driver";

    /**
     * 私有构造方法，禁止实例化工具类。
     *
     * @author lvdaxianer@yeah.net
     * @date 2026-07-14
     */
    private PostgreSqlTestContainerSupport() {
    }

    /**
     * 创建 PostgreSQL 测试容器。
     *
     * @param databaseName 数据库名称
     * @return PostgreSQL 测试容器
     * @author lvdaxianer@yeah.net
     * @date 2026-07-14
     */
    public static PostgreSQLContainer<?> createContainer(String databaseName) {
        return new PostgreSQLContainer<>(POSTGRESQL_IMAGE)
                .withDatabaseName(databaseName)
                .withUsername(USERNAME)
                .withPassword(PASSWORD);
    }

    /**
     * 注册 Spring 数据源属性。
     *
     * @param registry 动态属性注册器
     * @param container PostgreSQL 测试容器
     * @author lvdaxianer@yeah.net
     * @date 2026-07-14
     */
    public static void registerDatasource(DynamicPropertyRegistry registry, PostgreSQLContainer<?> container) {
        registry.add("spring.datasource.url", container::getJdbcUrl);
        registry.add("spring.datasource.username", container::getUsername);
        registry.add("spring.datasource.password", container::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> DRIVER_CLASS_NAME);
    }
}
