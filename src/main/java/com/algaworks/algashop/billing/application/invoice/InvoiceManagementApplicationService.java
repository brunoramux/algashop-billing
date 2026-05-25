package com.algaworks.algashop.billing.application.invoice;

import com.algaworks.algashop.billing.domain.model.DomainException;
import com.algaworks.algashop.billing.domain.model.creditcard.CreditCardRepository;
import com.algaworks.algashop.billing.domain.model.invoice.*;
import com.algaworks.algashop.billing.domain.model.invoice.payment.Payment;
import com.algaworks.algashop.billing.domain.model.invoice.payment.PaymentGatewayService;
import com.algaworks.algashop.billing.domain.model.invoice.payment.PaymentRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvoiceManagementApplicationService {

    private final PaymentGatewayService paymentGatewayService;
    private final InvoicingService invoicingService;
    private final InvoiceRepository invoiceRepository;
    private final CreditCardRepository creditCardRepository;

    @Transactional
    public UUID generate(GenerateInvoiceInput input) {

        verifyCreditCardId(input.getPaymentSettings().getCreditCardId());

        Payer payer = convertToPayer(input.getPayer());
        Set<LineItem> items = convertToLineItems(input.getItems());

        Invoice invoice = invoicingService.issue(input.getOrderId(), input.getCustomerId(), payer, items);
        invoice.changePaymentSettings(input.getPaymentSettings().getMethod(),
                input.getPaymentSettings().getCreditCardId());

        invoiceRepository.saveAndFlush(invoice);

        return invoice.getId();
    }

    @Transactional
    public void processPayment(UUID invoiceId){
        Invoice invoice = invoiceRepository.findById(invoiceId).orElseThrow(
                () -> new DomainException("Invoice with ID " + invoiceId + " does not exist.")
        );

        PaymentRequest paymentRequest = toPaymentRequest(invoice);

        Payment payment;

        try {
            payment = paymentGatewayService.capture(paymentRequest);
        } catch (Exception e){
            String paymentFailedMessage = String.format("Payment failed: %s", e.getMessage());
            log.error(paymentFailedMessage);
            invoice.cancel(paymentFailedMessage);
            invoiceRepository.saveAndFlush(invoice);
            return;
        }

        invoicingService.assignPayment(invoice, payment);
        invoiceRepository.saveAndFlush(invoice);
    }

    private PaymentRequest toPaymentRequest(Invoice invoice) {
        return PaymentRequest.builder()
                .invoiceId(invoice.getId())
                .method(invoice.getPaymentSettings().getMethod())
                .creditCardId(invoice.getPaymentSettings().getCreditCardId())
                .amount(invoice.getTotalAmount())
                .build();
    }

    private Set<LineItem> convertToLineItems(Set<LineItemInput> items) {
        Set<LineItem> lineItems = new LinkedHashSet<>();
        int itemNumber = 1;
        for(LineItemInput item : items) {
            lineItems.add(LineItem.builder()
                            .name(item.getName())
                            .number(itemNumber)
                            .amount(item.getAmount())
                    .build());
            itemNumber++;
        }

        return lineItems;
    }


    private Payer convertToPayer(PayerData payer) {
        return Payer.builder()
                .fullName(payer.getFullName())
                .email(payer.getEmail())
                .phone(payer.getPhone())
                .document(payer.getDocument())
                .address(Address.builder()
                        .street(payer.getAddress().getStreet())
                        .number(payer.getAddress().getNumber())
                        .complement(payer.getAddress().getComplement())
                        .neighborhood(payer.getAddress().getNeighborhood())
                        .city(payer.getAddress().getCity())
                        .state(payer.getAddress().getState())
                        .zipCode(payer.getAddress().getZipCode())
                        .build())
                .build();
    }

    private void verifyCreditCardId(UUID creditCardId) {
        if(creditCardId != null && !creditCardRepository.existsById(creditCardId)) {
            throw new IllegalArgumentException("Credit card with ID " + creditCardId + " does not exist.");
        }
    }

}
