package com.algaworks.algashop.billing;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class FastpayTokenizedCreditCardModel {
    private String tokenizedCard;
    private OffsetDateTime expiresAt;
}