package com.algaworks.algashop.billing.infrastructure.payment.fastpay;

import com.algaworks.algashop.billing.infrastructure.AlgaShopPaymentProperties;
import com.algaworks.algashop.billing.infrastructure.creditcard.fastpay.FastpayCreditCardAPIClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
public class FastpayPaymentAPIClientConfig {

    @Bean
    public FastpayPaymentAPIClient fastpayPaymentAPIClient(
            RestClient.Builder builder,
            AlgaShopPaymentProperties paymentProperties
    ) {
        AlgaShopPaymentProperties.FastpayProperties fastPayProperties = paymentProperties.getFastpay();

        RestClient restClient = builder.baseUrl(fastPayProperties.getHostname())
                .requestInterceptor(((request, body, execution) -> {
                    request.getHeaders().add("Token", fastPayProperties.getPrivateToken());
                    return execution.execute(request, body);
                })).build();

        RestClientAdapter restClientAdapter = RestClientAdapter.create(restClient);

        HttpServiceProxyFactory proxyFactory = HttpServiceProxyFactory.builderFor(restClientAdapter).build();

        return proxyFactory.createClient(FastpayPaymentAPIClient.class);

    }
}
