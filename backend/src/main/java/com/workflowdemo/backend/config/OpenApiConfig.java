package com.workflowdemo.backend.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class OpenApiConfig {

    static final String BASIC_AUTH_SCHEME = "basicAuth";

    @Bean
    OpenAPI workflowDemoOpenApi() {
        return new OpenAPI()
            .info(new Info()
                .title("Workflow Demo API")
                .version("0.7.0")
                .description("""
                    Java 21 / Spring Boot workflow approval demo API.

                    Demo Basic Auth accounts:
                    - applicant: demo1@growtea.co.jp / demo1001
                    - approver: demo5@growtea.co.jp / demo1005
                    """)
                .license(new License().name("Portfolio demo")))
            .addServersItem(new Server().url("http://localhost:8080").description("Local backend"))
            .components(new Components().addSecuritySchemes(
                BASIC_AUTH_SCHEME,
                new SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("basic")
            ));
    }

    @Bean
    OpenApiCustomizer basicAuthOperationCustomizer() {
        return openApi -> openApi.getPaths().forEach((path, pathItem) ->
            pathItem.readOperationsMap().forEach((method, operation) -> {
                if (!isPublicOperation(method.name(), path)) {
                    operation.addSecurityItem(new SecurityRequirement().addList(BASIC_AUTH_SCHEME));
                }
            })
        );
    }

    private static boolean isPublicOperation(String method, String path) {
        if ("GET".equals(method) && ("/api/health".equals(path) || "/actuator/health".equals(path))) {
            return true;
        }
        if ("GET".equals(method) && path.startsWith("/api/master-data")) {
            return true;
        }
        if ("GET".equals(method) && path.startsWith("/api/form-definitions")) {
            return true;
        }
        return "GET".equals(method) && path.startsWith("/api/workflow-definitions");
    }
}
