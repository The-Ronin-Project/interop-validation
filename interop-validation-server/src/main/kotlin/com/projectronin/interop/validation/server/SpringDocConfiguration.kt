package com.projectronin.interop.validation.server

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import javax.annotation.Generated

// This was originally generated, but copied here to prevent some issues with the generator requiring Spring Boot 3
@Generated(value = ["org.openapitools.codegen.languages.KotlinSpringServerCodegen"])
@Configuration
class SpringDocConfiguration {
    @Bean
    fun apiInfo(): OpenAPI {
        return OpenAPI()
            .info(
                Info()
                    .title("Data Validation Service")
                    .description("The Data Validation Service provides APIs for managing data validation issues within the Ronin data ecosystem.")
                    .version("1.0.0")
            )
    }
}
