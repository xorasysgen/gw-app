package com.solum.gwapp.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiKey;
import springfox.documentation.service.AuthorizationScope;
import springfox.documentation.service.SecurityReference;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spi.service.contexts.SecurityContext;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import java.util.Arrays;
import java.util.List;

@EnableSwagger2
@Configuration
public class SwaggerConfig {

   @Bean
   public Docket apiDocket() {

      return  new Docket(DocumentationType.SWAGGER_2)
              .select()
              .apis(RequestHandlerSelectors.basePackage("com.solum.gwapp"))
              .paths(PathSelectors.any())
              .build()
              .securityContexts(Arrays.asList(securityContext())).securitySchemes(Arrays.asList(apiKey()));

   }

   private SecurityContext securityContext() { return SecurityContext.builder().securityReferences(defaultAuth()).build(); }

   private List<SecurityReference> defaultAuth() {
      AuthorizationScope authorizationScope = new AuthorizationScope("global", "accessEverything"); AuthorizationScope[] authorizationScopes = new AuthorizationScope[1]; authorizationScopes[0] = authorizationScope; return Arrays.asList(new SecurityReference("JWT", authorizationScopes));
   }

   private ApiKey apiKey() { return new ApiKey("JWT", "Authorization", "header"); }
}
