package com.algaworks.algashop.billing.infrastructure.creditcard.fastpay;

import com.algaworks.algashop.billing.domain.model.creditcard.CreditCardProviderService;
import com.algaworks.algashop.billing.domain.model.creditcard.LimitedCreditCard;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@ConditionalOnProperty(name = "algashop.integrations.payment.provider", havingValue = "FASTPAY")
@RequiredArgsConstructor
public class CreditCardProviderServiceFastpayImpl implements CreditCardProviderService {

    private final FastpayCreditCardAPIClient fastpayCreditCardAPIClient;

    @Override
    public LimitedCreditCard register(UUID customerId, String tokenizedCard) {

        FastpayCreditCardResponse fastpayCreditCardResponse = fastpayCreditCardAPIClient.create(
                FastpayCreditCardInput.builder()
                        .customerCode(customerId.toString())
                        .tokenizedCard(tokenizedCard)
                        .build()
        );

        return LimitedCreditCard.builder()
                .gatewayCode(fastpayCreditCardResponse.getId())
                .lastNumbers(fastpayCreditCardResponse.getLastNumbers())
                .brand(fastpayCreditCardResponse.getBrand())
                .expMonth(fastpayCreditCardResponse.getExpMonth())
                .expYear(fastpayCreditCardResponse.getExpYear())
                .build();

    }

    @Override
    public Optional<LimitedCreditCard> findById(String gatewayCode) {

        FastpayCreditCardResponse response = fastpayCreditCardAPIClient.findById(gatewayCode);

        return Optional.of(
                LimitedCreditCard.builder()
                        .gatewayCode(response.getId())
                        .lastNumbers(response.getLastNumbers())
                        .brand(response.getBrand())
                        .expMonth(response.getExpMonth())
                        .expYear(response.getExpYear())
                        .build()
        );

    }

    @Override
    public void delete(String gatewayCode) {
        fastpayCreditCardAPIClient.delete(gatewayCode);
    }
}
