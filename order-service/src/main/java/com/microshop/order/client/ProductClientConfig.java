package com.microshop.order.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.client.OAuth2ClientHttpRequestInterceptor;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import java.time.Duration;

@Configuration
public class ProductClientConfig {

    /** Tu lay token client_credentials tu auth-service, cache lai, het han thi tu xin moi. */
    @Bean
    OAuth2AuthorizedClientManager authorizedClientManager(ClientRegistrationRepository registrations,
                                                          OAuth2AuthorizedClientService clientService) {
        return new AuthorizedClientServiceOAuth2AuthorizedClientManager(registrations, clientService);
    }

    @Bean
    ProductClient productClient(RestClient.Builder builder,
                                OAuth2AuthorizedClientManager authorizedClientManager,
                                @Value("${microshop.product-service-url}") String productServiceUrl) {
        var oauth2 = new OAuth2ClientHttpRequestInterceptor(authorizedClientManager);
        oauth2.setClientRegistrationIdResolver(request -> "order-service");

        // Luon dat timeout khi goi qua mang: product-service cham thi order-service khong bi treo theo.
        var requestFactory = new JdkClientHttpRequestFactory();
        requestFactory.setReadTimeout(Duration.ofSeconds(3));

        RestClient restClient = builder
                .baseUrl(productServiceUrl)              // Docker DNS: http://product-service:8081
                .requestInterceptor(oauth2)
                .requestFactory(requestFactory)
                .build();
        return HttpServiceProxyFactory.builderFor(RestClientAdapter.create(restClient)).build()
                .createClient(ProductClient.class);
    }
}
