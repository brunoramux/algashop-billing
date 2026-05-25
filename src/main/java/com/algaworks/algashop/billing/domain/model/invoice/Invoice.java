package com.algaworks.algashop.billing.domain.model.invoice;

import com.algaworks.algashop.billing.domain.model.AbstractAuditableAggregateRoot;
import com.algaworks.algashop.billing.domain.model.DomainException;
import com.algaworks.algashop.billing.domain.model.IdGenerator;
import io.micrometer.common.util.StringUtils;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;

@Setter(AccessLevel.PRIVATE)
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true,  callSuper = false)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
public class Invoice extends AbstractAuditableAggregateRoot<Invoice> {

    @Id
    @EqualsAndHashCode.Include
    private UUID id;

    private String orderId;
    private UUID customerId;

    private OffsetDateTime issuedAt;
    private OffsetDateTime paidAt;
    private OffsetDateTime canceledAt;
    private OffsetDateTime expiresAt;

    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    private InvoiceStatus status;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private PaymentSettings paymentSettings;

    @ElementCollection
    @CollectionTable(name = "invoice_line_item", joinColumns = @JoinColumn(name = "invoice_id"))
    private Set<LineItem> items = new HashSet<>();

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "fullName", column = @Column(name = "payer_fullname")),
            @AttributeOverride(name = "document", column = @Column(name = "payer_document")),
            @AttributeOverride(name = "phone", column = @Column(name = "payer_phone")),
            @AttributeOverride(name = "email", column = @Column(name = "payer_email")),
            @AttributeOverride(name = "address.street", column = @Column(name = "payer_address_street")),
            @AttributeOverride(name = "address.number", column = @Column(name = "payer_address_number")),
            @AttributeOverride(name = "address.complement", column = @Column(name = "payer_address_complement")),
            @AttributeOverride(name = "address.neighborhood", column = @Column(name = "payer_address_neighborhood")),
            @AttributeOverride(name = "address.city", column = @Column(name = "payer_address_city")),
            @AttributeOverride(name = "address.state", column = @Column(name = "payer_address_state")),
            @AttributeOverride(name = "address.zipCode", column = @Column(name = "payer_address_zipCode"))
    })
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
        Invoice invoice = new Invoice(
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

        invoice.registerEvent(new InvoiceIssueEvent(invoice.getId(), invoice.getCustomerId(),
                invoice.getOrderId(), invoice.getIssuedAt()));

        return invoice;
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

        registerEvent(new InvoicePaidEvent(this.getId(), this.getCustomerId(),
                this.getOrderId(), this.getPaidAt()));
    }

    public void cancel(String reason) {
        if(isCanceled()){
            throw new DomainException(String.format("Invoice %s is already canceled.", this.getId()));
        }
        this.setCancelReason(reason);
        this.setCanceledAt(OffsetDateTime.now());
        this.setStatus(InvoiceStatus.CANCELED);

        registerEvent(new InvoiceCanceledEvent(this.getId(), this.getCustomerId(),
                this.getOrderId(), this.getCanceledAt()));
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
        paymentSettings.setInvoice(this);
        this.setPaymentSettings(paymentSettings);
    }


}
