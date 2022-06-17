package com.tkppp.tripleclubmileage.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.EnableWebMvc
import springfox.documentation.builders.ApiInfoBuilder
import springfox.documentation.builders.RequestHandlerSelectors
import springfox.documentation.service.ApiInfo
import springfox.documentation.spi.DocumentationType
import springfox.documentation.spring.web.plugins.Docket

@Configuration
@EnableWebMvc
class SwaggerConfig {

    fun swaggerInfo(): ApiInfo =
        ApiInfoBuilder()
            .title("Triple Mileage Service")
            .description("API Docs")
            .build()

    @Bean
    fun swaggerApi(): Docket =
        Docket(DocumentationType.SWAGGER_2)
            .select()
            .apis(RequestHandlerSelectors.basePackage("com.tkppp.tripleclubmileage.mileage.controller"))
            .build()
            .apiInfo(swaggerInfo())
}