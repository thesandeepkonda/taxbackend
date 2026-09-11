package com.crm.matrix.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
public class RestTemplateConfig {


    @Bean(name = "defaultRestTemplate")
    public RestTemplate defaultRestTemplate() {
        return new RestTemplate();
    }


    @Bean(name = "patchRestTemplate")
    public RestTemplate patchRestTemplate() {

        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(30))
                .build();

        JdkClientHttpRequestFactory factory =
                new JdkClientHttpRequestFactory(httpClient);

        factory.setReadTimeout(Duration.ofSeconds(30));

        return new RestTemplate(factory);
    }
}