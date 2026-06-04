package com.algaworks.algashop.billing;

import com.algaworks.algashop.billing.infrastructure.AlgaShopPaymentProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
public class FastpayCreditCardTokenizationAPIClientConfig {

    @Bean
    public FastpayCreditCardTokenizationAPIClient fastpayCreditCardTokenizationAPIClient(
            RestClient.Builder builder,
            AlgaShopPaymentProperties properties,
            @Value("${algashop.integrations.payment.fastpay.public-token}") String publicToken
    ) {
        var fastpayProperties = properties.getFastpay();

        RestClient restClient = builder.baseUrl(fastpayProperties.getHostname())
                .requestInterceptor(((request, body, execution) -> {
                    request.getHeaders().add("Token", publicToken);
                    return execution.execute(request, body);
                })).build();

        RestClientAdapter adapter = RestClientAdapter.create(restClient);
        HttpServiceProxyFactory proxyFactory = HttpServiceProxyFactory.builderFor(adapter).build();

        return proxyFactory.createClient(FastpayCreditCardTokenizationAPIClient.class);
    }

}