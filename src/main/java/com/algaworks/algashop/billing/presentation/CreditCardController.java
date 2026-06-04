package com.algaworks.algashop.billing.presentation;

import com.algaworks.algashop.billing.application.creditcard.management.CreditCardManagementService;
import com.algaworks.algashop.billing.application.creditcard.management.TokenizedCreditCardInput;
import com.algaworks.algashop.billing.application.creditcard.query.CreditCardOutput;
import com.algaworks.algashop.billing.application.creditcard.query.CreditCardQueryService;
import com.algaworks.algashop.billing.domain.model.creditcard.CreditCardProviderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/customers/{customerId}/credit-cards")
@RequiredArgsConstructor
public class CreditCardController {

    private final CreditCardQueryService creditCardQueryService;
    private final CreditCardManagementService creditCardManagementService;
    private final CreditCardProviderService creditCardProviderService;


    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreditCardOutput register(@PathVariable UUID customerId,
                                     @RequestBody @Valid TokenizedCreditCardInput input){

        input.setCustomerId(customerId);
        UUID creditCardId = creditCardManagementService.register(input);
        return creditCardQueryService.findOne(customerId, creditCardId);

    }

    @GetMapping
    public List<CreditCardOutput> findAllByCustomer(@PathVariable UUID customerId){
        return creditCardQueryService.findByCustomer(customerId);
    }

    @GetMapping("/{creditCardId}")
    public CreditCardOutput findOne(@PathVariable UUID customerId, @PathVariable UUID creditCardId){
        return creditCardQueryService.findOne(customerId, creditCardId);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID customerId, @PathVariable UUID creditCardId){
        creditCardManagementService.delete(customerId, creditCardId);
    }

}
