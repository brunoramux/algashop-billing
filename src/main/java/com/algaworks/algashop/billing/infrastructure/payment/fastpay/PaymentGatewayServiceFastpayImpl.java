package com.algaworks.algashop.billing.infrastructure.payment.fastpay;

import com.algaworks.algashop.billing.domain.model.creditcard.CreditCard;
import com.algaworks.algashop.billing.domain.model.creditcard.CreditCardRepository;
import com.algaworks.algashop.billing.domain.model.invoice.Address;
import com.algaworks.algashop.billing.domain.model.invoice.Payer;
import com.algaworks.algashop.billing.domain.model.invoice.payment.Payment;
import com.algaworks.algashop.billing.domain.model.invoice.payment.PaymentGatewayService;
import com.algaworks.algashop.billing.domain.model.invoice.payment.PaymentRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@ConditionalOnProperty(name = "algashop.integrations.payment.provider", havingValue = "FASTPAY")
@RequiredArgsConstructor
public class PaymentGatewayServiceFastpayImpl implements PaymentGatewayService  {

    private final FastpayPaymentAPIClient fastpayPaymentAPIClient;
    private final CreditCardRepository creditCardRepository;

    @Override
    public Payment capture(PaymentRequest request) {
        FastpayPaymentInput input = convertToInput(request);
        FastpayPaymentModel response = fastpayPaymentAPIClient.capture(input);
        return convertToPayment(response);
    }

    @Override
    public Payment findByCode(String gatewayCode) {
        FastpayPaymentModel response = fastpayPaymentAPIClient.findById(gatewayCode);
        return convertToPayment(response);
    }

    private FastpayPaymentInput convertToInput(PaymentRequest request) {
        Payer payer = request.getPayer();
        Address address = payer.getAddress();

        FastpayPaymentInput.FastpayPaymentInputBuilder builder = FastpayPaymentInput.builder()
                .totalAmount(request.getAmount())
                .referenceCode(request.getInvoiceId().toString())
                .fullName(payer.getFullName())
                .document(payer.getDocument())
                .phone(payer.getPhone())
                .zipCode(address.getZipCode())
                .addressLine1(address.getStreet() + ", " + address.getNumber())
                .addressLine2(address.getComplement())
                .replyToUrl("http://host.docker.internal:8082/api/v1/webhooks/fastpay");

        switch (request.getMethod()){
            case CREDIT_CARD -> {
                builder.method(FastpayPaymentMethod.CREDIT.name());
                CreditCard creditCard = creditCardRepository.findById(request.getCreditCardId()).orElseThrow();
                builder.creditCardId(creditCard.getGatewayCode());
            }
            case GATEWAY_BALANCE -> {
                builder.method(FastpayPaymentMethod.GATEWAY_BALANCE.name());
            }
        }

        return builder.build();
    }

    private Payment convertToPayment(FastpayPaymentModel response) {
        Payment.PaymentBuilder builder = Payment.builder();

        builder.gatewayCode(response.getId())
                .invoiceId(UUID.fromString(response.getReferenceCode()));

        try {
            FastpayPaymentMethod fastpayPaymentMethod = FastpayPaymentMethod.valueOf(response.getMethod());
            builder.method(FastpayEnumConverter.convert(fastpayPaymentMethod));
        } catch (Exception e){
                throw new IllegalArgumentException("Unknown payment method: " + response.getMethod());
        }

        try {
            FastpayPaymentStatus fastpayPaymentStatus = FastpayPaymentStatus.valueOf(response.getStatus());
            builder.status(FastpayEnumConverter.convert(fastpayPaymentStatus));
        } catch (Exception e){
            throw new IllegalArgumentException("Unknown payment status: " + response.getStatus());
        }

        return builder.build();
    }

}
