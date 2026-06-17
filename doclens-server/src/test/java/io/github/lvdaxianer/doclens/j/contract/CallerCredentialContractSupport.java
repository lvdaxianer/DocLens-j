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
 * caller 凭证契约测试共享支持。
 *
 * @author lvdaxianerplus
 * @date 2026-06-17
 */
interface CallerCredentialContractSupport {

    /** 测试 API Key 请求头。 */
    String API_KEY_HEADER = "X-DocLens-Api-Key";
    /** 测试 caller API Key。 */
    String TEST_API_KEY = "test-api-key";
    /** 测试 caller client id。 */
    String TEST_CLIENT_ID = "rag-flow";
    /** 测试 caller source app。 */
    String TEST_SOURCE_APP = "knowledge-base";
    /** 测试 caller tenant key。 */
    String TEST_TENANT_KEY = "tenant-east";

    /**
     * 创建带 caller 凭证的 multipart 请求。
     *
     * @param path 请求路径
     * @return multipart 请求构建器
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    default MockMultipartHttpServletRequestBuilder authenticatedMultipart(String path) {
        MockMultipartHttpServletRequestBuilder builder = multipart(path);
        builder.header(API_KEY_HEADER, TEST_API_KEY);
        return builder;
    }

    /**
     * 创建带 caller 凭证的 GET 请求。
     *
     * @param uriTemplate URI 模板
     * @param uriVars URI 参数
     * @return GET 请求构建器
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    default MockHttpServletRequestBuilder authenticatedGet(String uriTemplate, Object... uriVars) {
        return get(uriTemplate, uriVars).header(API_KEY_HEADER, TEST_API_KEY);
    }

    /**
     * 创建带 caller 凭证的 POST 请求。
     *
     * @param uriTemplate URI 模板
     * @param uriVars URI 参数
     * @return POST 请求构建器
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    default MockHttpServletRequestBuilder authenticatedPost(String uriTemplate, Object... uriVars) {
        return post(uriTemplate, uriVars).header(API_KEY_HEADER, TEST_API_KEY);
    }

    /**
     * 创建带 caller 凭证的 PUT 请求。
     *
     * @param uriTemplate URI 模板
     * @param uriVars URI 参数
     * @return PUT 请求构建器
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    default MockHttpServletRequestBuilder authenticatedPut(String uriTemplate, Object... uriVars) {
        return put(uriTemplate, uriVars).header(API_KEY_HEADER, TEST_API_KEY);
    }

    /**
     * 创建带 caller 凭证的 DELETE 请求。
     *
     * @param uriTemplate URI 模板
     * @param uriVars URI 参数
     * @return DELETE 请求构建器
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    default MockHttpServletRequestBuilder authenticatedDelete(String uriTemplate, Object... uriVars) {
        return delete(uriTemplate, uriVars).header(API_KEY_HEADER, TEST_API_KEY);
    }

    /**
     * 创建带 caller 凭证的 PATCH 请求。
     *
     * @param uriTemplate URI 模板
     * @param uriVars URI 参数
     * @return PATCH 请求构建器
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    default MockHttpServletRequestBuilder authenticatedPatch(String uriTemplate, Object... uriVars) {
        return patch(uriTemplate, uriVars).header(API_KEY_HEADER, TEST_API_KEY);
    }
}
