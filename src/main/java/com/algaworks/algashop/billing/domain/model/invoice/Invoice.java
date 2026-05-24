package com.algaworks.algashop.billing.domain.model.invoice;

import com.algaworks.algashop.billing.domain.model.DomainException;
import com.algaworks.algashop.billing.domain.model.IdGenerator;
import io.micrometer.common.util.StringUtils;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;

@Setter(AccessLevel.PRIVATE)
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class Invoice {

    @EqualsAndHashCode.Include
    private UUID id;

    private String orderId;
    private UUID customerId;

    private OffsetDateTime issuedAt;
    private OffsetDateTime paidAt;
    private OffsetDateTime canceledAt;
    private OffsetDateTime expiresAt;

    private BigDecimal totalAmount;

    private InvoiceStatus status;

    private PaymentSettings paymentSettings;

    private Set<LineItem> items = new HashSet<>();

    private Payer payer;

    private String cancelReason;

    public static Invoice issue(String orderId, UUID customerId, Payer payer, Set<LineItem> items) {
        Objects.requireNonNull(customerId);
        Objects.requireNonNull(payer);
        Objects.requireNonNull(items);

        if(StringUtils.isBlank(orderId)){
            throw new IllegalArgumentException("Order ID cannot be blank.");
        }

        if(items.isEmpty()){
            throw new IllegalArgumentException("Items cannot be empty.");
        }

        BigDecimal totalAmount = items.stream().map(LineItem::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new Invoice(
                IdGenerator.generateTimeBasedUUID(),
                orderId,
                customerId,
                OffsetDateTime.now(),
                null,
                null,
                OffsetDateTime.now().plusDays(3),
                totalAmount,
                InvoiceStatus.UNPAID,
                null,
                items,
                payer,
                null
        );
    }

    // Garantindo que o itens não possa ser alterado diretamente via getItems().setItem()
    public Set<LineItem> getItems() {
        return Collections.unmodifiableSet(this.items);
    }

    public boolean isCanceled() {
        return InvoiceStatus.CANCELED.equals(this.getStatus());
    }

    public boolean isUnpaid() {
        return InvoiceStatus.UNPAID.equals(this.getStatus());
    }

    public boolean isPaid() {
        return InvoiceStatus.PAID.equals(this.getStatus());
    }

    public void markAsPaid() {
        if(!this.isUnpaid()) {
            throw new DomainException(String.format("Invoice %s with status %s cannot be marked as paid", this.getId(), this.getStatus()));
        }
        this.setPaidAt(OffsetDateTime.now());
        this.setStatus(InvoiceStatus.PAID);
    }

    public void cancel(String reason) {
        if(isCanceled()){
            throw new DomainException(String.format("Invoice %s is already canceled.", this.getId()));
        }
        this.setCancelReason(reason);
        this.setCanceledAt(OffsetDateTime.now());
        this.setStatus(InvoiceStatus.CANCELED);
    }

    public void assignPaymentGatewayCode(String code){
        if(!this.isUnpaid()) {
            throw new DomainException(String.format("Invoice %s with status %s cannot be edited.", this.getId(), this.getStatus()));
        }
        this.getPaymentSettings().assignGatewayCode(code);
    }

    public void changePaymentSettings(PaymentMethod paymentMethod, UUID creditCardId) {
        if(!this.isUnpaid()) {
            throw new DomainException(String.format("Invoice %s with status %s cannot be edited.", this.getId(), this.getStatus()));
        }

        PaymentSettings paymentSettings = PaymentSettings.brandNew(paymentMethod, creditCardId);
        this.setPaymentSettings(paymentSettings);
    }
}
