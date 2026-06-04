package com.algaworks.algashop.billing;

import com.algaworks.algashop.billing.domain.model.creditcard.CreditCard;
import com.algaworks.algashop.billing.domain.model.creditcard.CreditCardRepository;
import com.algaworks.algashop.billing.domain.model.creditcard.LimitedCreditCard;
import com.algaworks.algashop.billing.domain.model.invoice.PaymentMethod;
import com.algaworks.algashop.billing.domain.model.invoice.payment.Payment;
import com.algaworks.algashop.billing.domain.model.invoice.payment.PaymentRequest;
import com.algaworks.algashop.billing.infrastructure.payment.fastpay.PaymentGatewayServiceFastpayImpl;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@SpringBootTest
@Transactional
class PaymentGatewayServiceFastpayImplIT extends AbstractFastpayIT {

    @Autowired
    private PaymentGatewayServiceFastpayImpl paymentGatewayServiceFastpay;

    @Autowired
    private CreditCardRepository creditCardRepository;

    @Test
    public void shouldProcessPaymentWithCreditCard() {
        // CHAMA GATEWAY DE PAGAMENTO PARA TOKENIZAR CARTÃO VÁLIDO COMO TESTE - PARTE REALIZADA PELO FRONTEND EM FLUXO REAL
        LimitedCreditCard limitedCreditCard = registerCard();

        // CRIAR NOVA ENTIDADE DE CARTÃO DE CRÉDITO
        CreditCard creditCard = CreditCard.brandNew(
                validCustomerId,
                limitedCreditCard.getLastNumbers(),
                limitedCreditCard.getBrand(),
                limitedCreditCard.getExpMonth(),
                limitedCreditCard.getExpYear(),
                limitedCreditCard.getGatewayCode()
        );

        // SALVA NO BANCO DE DADOS
        creditCardRepository.save(creditCard);

        // SIMULA REQUISIÇÃO DE PAGAMENTO
        UUID invoiceId = UUID.randomUUID();

        // CRIA UM REQUEST PARA O GATEWAY DE PAGAMENTOS
        PaymentRequest request = PaymentRequest.builder()
                .method(PaymentMethod.CREDIT_CARD)
                .amount(new BigDecimal("1000.00"))
                .invoiceId(invoiceId)
                .creditCardId(creditCard.getId())
                .payer(InvoiceTestDataBuilder.aPayer())
                .build();

        // REALIZA O REQUEST
        Payment payment = paymentGatewayServiceFastpay.capture(request);

        Assertions.assertThat(payment.getInvoiceId()).isEqualTo(invoiceId);
        System.out.println(payment.getGatewayCode());
    }

}