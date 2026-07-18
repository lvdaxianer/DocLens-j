package io.github.lvdaxianer.doclens.j.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.FileSystemResource;

/**
 * 运行时 profile 配置测试。
 *
 * @author lvdaxianer@yeah.net
 * @date 2026-07-02
 */
class RuntimeProfileConfigurationTest {

    /*
     * 配置文件名常量：
     * - shared 配置只允许放跨环境安全默认值
     * - dev 配置承载本地开发可运行默认值
     * - prod 配置承载无默认值的环境变量占位符
     */
    private static final String SHARED_CONFIG = "application.yml";
    private static final String DEV_CONFIG = "application-dev.yml";
    private static final String PROD_CONFIG = "application-prod.yml";
    private static final String MAIN_RESOURCES_DIRECTORY = "src/main/resources";
    private static final String TEST_RESOURCES_DIRECTORY = "src/test/resources";
    /*
     * 运行时属性键常量：
     * - 数据源、存储、worker、OCR、文档转换属于环境相关运行参数
     * - gateway-auth 属于生产可信网关边界配置
     * - 测试通过属性键直接验证 YAML 是否被拆分到正确 profile
     */
    private static final String DATASOURCE_URL_PROPERTY = "spring.datasource.url";
    private static final String DATASOURCE_USERNAME_PROPERTY = "spring.datasource.username";
    private static final String DATASOURCE_PASSWORD_PROPERTY = "spring.datasource.password";
    private static final String DATASOURCE_DRIVER_PROPERTY = "spring.datasource.driver-class-name";
    private static final String STORAGE_ROOT_PROPERTY = "doclens.storage-root";
    private static final String WORKER_ID_PROPERTY = "doclens.worker-id";
    private static final String DEFAULT_ADAPTER_KEY_PROPERTY = "doclens.adapter.default-key";
    private static final String PADDLE_ENABLED_PROPERTY = "doclens.paddle-ocr.enabled";
    private static final String PADDLE_ENDPOINT_PROPERTY = "doclens.paddle-ocr.endpoint";
    private static final String PADDLE_TIMEOUT_PROPERTY = "doclens.paddle-ocr.timeout-seconds";
    private static final String PADDLE_VISUALIZE_PROPERTY = "doclens.paddle-ocr.visualize";
    private static final String PADDLE_BOOTSTRAP_HOST_PROPERTY = "doclens.paddle-ocr.bootstrap-nodes[0].host";
    private static final String WORD_CONVERSION_COMMAND_PROPERTY = "doclens.word-conversion.command";
    private static final String GATEWAY_AUTH_ENABLED_PROPERTY = "doclens.gateway-auth.enabled";
    private static final String GATEWAY_SECRET_PROPERTY = "doclens.gateway-auth.trusted-gateway.header-value";
    private static final String GATEWAY_PRINCIPAL_PROPERTY = "doclens.gateway-auth.principals[0].principal";
    private static final String GATEWAY_PARTITION_PROPERTY = "doclens.gateway-auth.principals[0].allowed-partitions[0]";
    /*
     * 期望值常量：
     * - LOCAL_* 只能出现在 dev profile 中
     * - PROD_* 必须是无默认值环境变量，避免生产静默回退
     * - TEST_* 必须隔离到 target 目录，数据库由 PostgreSQL 测试容器动态注入
     */
    private static final String LOCAL_POSTGRESQL_URL = "${DOCLENS_DB_URL:jdbc:postgresql://localhost:5432/doclens}";
    private static final String LOCAL_DATASOURCE_USERNAME = "${DOCLENS_DB_USERNAME:doclens}";
    private static final String LOCAL_DATASOURCE_PASSWORD = "${DOCLENS_DB_PASSWORD:doclens}";
    private static final String LOCAL_POSTGRESQL_DRIVER = "${DOCLENS_DB_DRIVER:org.postgresql.Driver}";
    private static final String LOCAL_STORAGE_ROOT = "./var/storage";
    private static final String LOCAL_WORKER_ID = "local-worker";
    private static final String LOCAL_PADDLE_ENDPOINT = "${DOCLENS_PADDLE_OCR_ENDPOINT:http://10.100.30.215:8080/ocr}";
    private static final String LOCAL_PADDLE_HOST = "10.100.30.215";
    private static final String LOCAL_WORD_CONVERSION_COMMAND = "/opt/homebrew/bin/soffice";
    private static final String PROD_DATASOURCE_URL = "${DOCLENS_DB_URL}";
    private static final String PROD_DATASOURCE_USERNAME = "${DOCLENS_DB_USERNAME}";
    private static final String PROD_DATASOURCE_PASSWORD = "${DOCLENS_DB_PASSWORD}";
    private static final String PROD_DATASOURCE_DRIVER = "${DOCLENS_DB_DRIVER}";
    private static final String PROD_STORAGE_ROOT = "${DOCLENS_STORAGE_ROOT}";
    private static final String PROD_PADDLE_ENDPOINT = "${DOCLENS_PADDLE_OCR_ENDPOINT}";
    private static final String PROD_WORD_CONVERSION_COMMAND = "${DOCLENS_WORD_CONVERSION_COMMAND}";
    private static final String PROD_GATEWAY_SECRET = "${DOCLENS_GATEWAY_SECRET}";
    private static final String PROD_GATEWAY_PRINCIPAL = "${DOCLENS_GATEWAY_PRINCIPAL}";
    private static final String PROD_GATEWAY_PARTITION = "${DOCLENS_GATEWAY_PARTITION}";
    private static final String TEST_STORAGE_ROOT = "./target/test-storage";

    /**
     * 共享配置不应携带环境相关运行时默认值。
     *
     * @author lvdaxianer@yeah.net
     * @date 2026-07-02
     */
    @Test
    void sharedConfigDoesNotContainEnvironmentSpecificRuntimeDefaults() throws IOException {
        PropertySource<?> sharedConfig = load(SHARED_CONFIG);

        assertThat(sharedConfig.getProperty(DATASOURCE_URL_PROPERTY)).isNull();
        assertThat(sharedConfig.getProperty(STORAGE_ROOT_PROPERTY)).isNull();
        assertThat(sharedConfig.getProperty(WORKER_ID_PROPERTY)).isNull();
        assertThat(sharedConfig.getProperty(DEFAULT_ADAPTER_KEY_PROPERTY)).isNull();
        assertThat(sharedConfig.getProperty(PADDLE_ENABLED_PROPERTY)).isNull();
        assertThat(sharedConfig.getProperty(PADDLE_ENDPOINT_PROPERTY)).isNull();
        assertThat(sharedConfig.getProperty(PADDLE_TIMEOUT_PROPERTY)).isNull();
        assertThat(sharedConfig.getProperty(PADDLE_VISUALIZE_PROPERTY)).isNull();
        assertThat(sharedConfig.getProperty(PADDLE_BOOTSTRAP_HOST_PROPERTY)).isNull();
        assertThat(sharedConfig.getProperty(WORD_CONVERSION_COMMAND_PROPERTY)).isNull();
    }

    /**
     * 开发环境保留本地运行默认值。
     *
     * @author lvdaxianer@yeah.net
     * @date 2026-07-02
     */
    @Test
    void devProfileKeepsLocalRuntimeDefaults() throws IOException {
        PropertySource<?> devConfig = load(DEV_CONFIG);

        assertThat(devConfig.getProperty(DATASOURCE_URL_PROPERTY)).isEqualTo(LOCAL_POSTGRESQL_URL);
        assertThat(devConfig.getProperty(DATASOURCE_USERNAME_PROPERTY)).isEqualTo(LOCAL_DATASOURCE_USERNAME);
        assertThat(devConfig.getProperty(DATASOURCE_PASSWORD_PROPERTY)).isEqualTo(LOCAL_DATASOURCE_PASSWORD);
        assertThat(devConfig.getProperty(DATASOURCE_DRIVER_PROPERTY)).isEqualTo(LOCAL_POSTGRESQL_DRIVER);
        assertThat(devConfig.getProperty(STORAGE_ROOT_PROPERTY)).isEqualTo(LOCAL_STORAGE_ROOT);
        assertThat(devConfig.getProperty(WORKER_ID_PROPERTY)).isEqualTo(LOCAL_WORKER_ID);
        assertThat(devConfig.getProperty(PADDLE_ENDPOINT_PROPERTY)).isEqualTo(LOCAL_PADDLE_ENDPOINT);
        assertThat(devConfig.getProperty(PADDLE_BOOTSTRAP_HOST_PROPERTY)).isEqualTo(LOCAL_PADDLE_HOST);
        assertThat(devConfig.getProperty(WORD_CONVERSION_COMMAND_PROPERTY)).isEqualTo(LOCAL_WORD_CONVERSION_COMMAND);
    }

    /**
     * 生产环境必须使用无默认值的环境变量占位符。
     *
     * @author lvdaxianer@yeah.net
     * @date 2026-07-02
     */
    @Test
    void prodProfileRequiresEnvironmentVariablesWithoutDefaults() throws IOException {
        PropertySource<?> prodConfig = load(PROD_CONFIG);

        assertThat(prodConfig.getProperty(DATASOURCE_URL_PROPERTY)).isEqualTo(PROD_DATASOURCE_URL);
        assertThat(prodConfig.getProperty(DATASOURCE_USERNAME_PROPERTY)).isEqualTo(PROD_DATASOURCE_USERNAME);
        assertThat(prodConfig.getProperty(DATASOURCE_PASSWORD_PROPERTY)).isEqualTo(PROD_DATASOURCE_PASSWORD);
        assertThat(prodConfig.getProperty(DATASOURCE_DRIVER_PROPERTY)).isEqualTo(PROD_DATASOURCE_DRIVER);
        assertThat(prodConfig.getProperty(STORAGE_ROOT_PROPERTY)).isEqualTo(PROD_STORAGE_ROOT);
        assertThat(prodConfig.getProperty(PADDLE_ENDPOINT_PROPERTY)).isEqualTo(PROD_PADDLE_ENDPOINT);
        assertThat(prodConfig.getProperty(WORD_CONVERSION_COMMAND_PROPERTY)).isEqualTo(PROD_WORD_CONVERSION_COMMAND);
        assertThat(prodConfig.getProperty(GATEWAY_AUTH_ENABLED_PROPERTY)).isEqualTo(Boolean.TRUE);
        assertThat(prodConfig.getProperty(GATEWAY_SECRET_PROPERTY)).isEqualTo(PROD_GATEWAY_SECRET);
        assertThat(prodConfig.getProperty(GATEWAY_PRINCIPAL_PROPERTY)).isEqualTo(PROD_GATEWAY_PRINCIPAL);
        assertThat(prodConfig.getProperty(GATEWAY_PARTITION_PROPERTY)).isEqualTo(PROD_GATEWAY_PARTITION);
    }

    /**
     * 测试环境不声明静态数据源，只保留测试存储目录。
     *
     * @author lvdaxianer@yeah.net
     * @date 2026-07-02
     */
    @Test
    void testProfileUsesIsolatedRuntimeDefaults() throws IOException {
        PropertySource<?> testConfig = loadTest(SHARED_CONFIG);

        assertThat(testConfig.getProperty(DATASOURCE_URL_PROPERTY)).isNull();
        assertThat(testConfig.getProperty(DATASOURCE_USERNAME_PROPERTY)).isNull();
        assertThat(testConfig.getProperty(DATASOURCE_PASSWORD_PROPERTY)).isNull();
        assertThat(testConfig.getProperty(DATASOURCE_DRIVER_PROPERTY)).isNull();
        assertThat(testConfig.getProperty(STORAGE_ROOT_PROPERTY)).isEqualTo(TEST_STORAGE_ROOT);
    }

    /**
     * 读取 YAML 配置文件。
     *
     * @param fileName 配置文件名
     * @return 配置属性源
     * @throws IOException 配置文件读取失败时抛出
     * @author lvdaxianer@yeah.net
     * @date 2026-07-02
     */
    private PropertySource<?> load(String fileName) throws IOException {
        return new YamlPropertySourceLoader().load(fileName, new FileSystemResource(configPath(fileName))).getFirst();
    }

    /**
     * 读取 test resources YAML 配置文件。
     *
     * @param fileName 配置文件名
     * @return 配置属性源
     * @throws IOException 配置文件读取失败时抛出
     * @author lvdaxianer@yeah.net
     * @date 2026-07-02
     */
    private PropertySource<?> loadTest(String fileName) throws IOException {
        return new YamlPropertySourceLoader().load(fileName, new FileSystemResource(testConfigPath(fileName)))
                .getFirst();
    }

    /**
     * 创建 main resources 配置文件路径。
     *
     * @param fileName 配置文件名
     * @return 配置文件路径
     * @author lvdaxianer@yeah.net
     * @date 2026-07-02
     */
    private Path configPath(String fileName) {
        return Path.of(MAIN_RESOURCES_DIRECTORY, fileName);
    }

    /**
     * 创建 test resources 配置文件路径。
     *
     * @param fileName 配置文件名
     * @return 配置文件路径
     * @author lvdaxianer@yeah.net
     * @date 2026-07-02
     */
    private Path testConfigPath(String fileName) {
        return Path.of(TEST_RESOURCES_DIRECTORY, fileName);
    }
}
