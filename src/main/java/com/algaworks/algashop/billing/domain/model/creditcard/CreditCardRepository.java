package com.algaworks.algashop.billing.domain.model.creditcard;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface CreditCardRepository extends JpaRepository<CreditCard, UUID> {
    Optional<CreditCard> findByCustomerIdAndId(UUID customerId, UUID creditCardId);

    Collection<Object> findAllByCustomerId(UUID customerId);
}
