package com.coding.exercise.bankapp.config;

import java.util.Set;

import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.filter.UrlHandlerFilter;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem.HttpMethod;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

@Configuration
public class ApplicationConfig {

    private static final String CSRF_SCHEME = "csrf";
    private static final Set<HttpMethod> SAFE_METHODS = Set.of(HttpMethod.GET, HttpMethod.HEAD, HttpMethod.OPTIONS);

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("BANKING APPLICATION REST API")
                        .description("API for Banking Application.")
                        .version("1.0.0"))
                .components(new Components().addSecuritySchemes(CSRF_SCHEME, new SecurityScheme()
                        .type(SecurityScheme.Type.APIKEY)
                        .in(SecurityScheme.In.HEADER)
                        .name("X-XSRF-TOKEN")
                        .description("Value of the XSRF-TOKEN cookie; the cookie must be sent too.")));
    }

    /** Documents the CSRF header requirement on every state-changing operation. */
    @Bean
    public OpenApiCustomizer csrfOpenApiCustomizer() {
        return openApi -> openApi.getPaths().values().forEach(path -> path.readOperationsMap()
                .forEach((method, operation) -> {
                    if (!SAFE_METHODS.contains(method)) {
                        operation.addSecurityItem(new SecurityRequirement().addList(CSRF_SCHEME));
                    }
                }));
    }

    /** Keeps trailing-slash URLs (e.g. /customers/all/) working, as they did before Spring Framework 6. */
    @Bean
    public FilterRegistrationBean<UrlHandlerFilter> trailingSlashFilter() {
        FilterRegistrationBean<UrlHandlerFilter> registration = new FilterRegistrationBean<>(
                UrlHandlerFilter.trailingSlashHandler("/**").wrapRequest().build());
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }
}
