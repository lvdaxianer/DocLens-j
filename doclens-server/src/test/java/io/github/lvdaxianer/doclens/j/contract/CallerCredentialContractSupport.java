package io.github.lvdaxianer.doclens.j.contract;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

/**
 * caller 分区键契约测试共享支持。
 *
 * @author lvdaxianerplus
 * @date 2026-06-17
 */
interface CallerCredentialContractSupport {

    /** 测试 caller 分区键请求头。 */
    String CALLER_PARTITION_HEADER = "X-Doclens-Key";
    /** 测试 caller 分区键。 */
    String TEST_CALLER_PARTITION_KEY = "tenant-east";
    /** 测试 caller client id。 */
    String TEST_CLIENT_ID = TEST_CALLER_PARTITION_KEY;
    /** 测试 caller source app。 */
    String TEST_SOURCE_APP = "dashboard";
    /** 测试 caller tenant key。 */
    String TEST_TENANT_KEY = TEST_CALLER_PARTITION_KEY;
    /** 兼容历史测试配置名称的 caller 分区键。 */
    String TEST_API_KEY = TEST_CALLER_PARTITION_KEY;
    /** 外部测试 caller 分区键。 */
    String FOREIGN_CALLER_PARTITION_KEY = "tenant-west";
    /** 外部测试 caller client id。 */
    String FOREIGN_CLIENT_ID = FOREIGN_CALLER_PARTITION_KEY;
    /** 外部测试 caller source app。 */
    String FOREIGN_SOURCE_APP = "dashboard";
    /** 外部测试 caller tenant key。 */
    String FOREIGN_TENANT_KEY = FOREIGN_CALLER_PARTITION_KEY;
    /** 兼容历史测试配置名称的外部 caller 分区键。 */
    String FOREIGN_API_KEY = FOREIGN_CALLER_PARTITION_KEY;

    /**
     * 创建带 caller 分区键的 multipart 请求。
     *
     * @param path 请求路径
     * @return multipart 请求构建器
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    default MockMultipartHttpServletRequestBuilder authenticatedMultipart(String path) {
        MockMultipartHttpServletRequestBuilder builder = multipart(path);
        builder.header(CALLER_PARTITION_HEADER, TEST_CALLER_PARTITION_KEY);
        return builder;
    }

    /**
     * 创建带 caller 分区键的 GET 请求。
     *
     * @param uriTemplate URI 模板
     * @param uriVars URI 参数
     * @return GET 请求构建器
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    default MockHttpServletRequestBuilder authenticatedGet(String uriTemplate, Object... uriVars) {
        return get(uriTemplate, uriVars).header(CALLER_PARTITION_HEADER, TEST_CALLER_PARTITION_KEY);
    }

    /**
     * 创建带 caller 分区键的 POST 请求。
     *
     * @param uriTemplate URI 模板
     * @param uriVars URI 参数
     * @return POST 请求构建器
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    default MockHttpServletRequestBuilder authenticatedPost(String uriTemplate, Object... uriVars) {
        return post(uriTemplate, uriVars).header(CALLER_PARTITION_HEADER, TEST_CALLER_PARTITION_KEY);
    }

    /**
     * 创建带 caller 分区键的 PUT 请求。
     *
     * @param uriTemplate URI 模板
     * @param uriVars URI 参数
     * @return PUT 请求构建器
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    default MockHttpServletRequestBuilder authenticatedPut(String uriTemplate, Object... uriVars) {
        return put(uriTemplate, uriVars).header(CALLER_PARTITION_HEADER, TEST_CALLER_PARTITION_KEY);
    }

    /**
     * 创建带 caller 分区键的 DELETE 请求。
     *
     * @param uriTemplate URI 模板
     * @param uriVars URI 参数
     * @return DELETE 请求构建器
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    default MockHttpServletRequestBuilder authenticatedDelete(String uriTemplate, Object... uriVars) {
        return delete(uriTemplate, uriVars).header(CALLER_PARTITION_HEADER, TEST_CALLER_PARTITION_KEY);
    }

    /**
     * 创建带 caller 分区键的 PATCH 请求。
     *
     * @param uriTemplate URI 模板
     * @param uriVars URI 参数
     * @return PATCH 请求构建器
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    default MockHttpServletRequestBuilder authenticatedPatch(String uriTemplate, Object... uriVars) {
        return patch(uriTemplate, uriVars).header(CALLER_PARTITION_HEADER, TEST_CALLER_PARTITION_KEY);
    }
}
