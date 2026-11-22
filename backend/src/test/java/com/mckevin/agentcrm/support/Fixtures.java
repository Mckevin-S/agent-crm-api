package com.mckevin.agentcrm.support;

import com.mckevin.agentcrm.domain.customer.Claim;
import com.mckevin.agentcrm.domain.customer.ClaimStatus;
import com.mckevin.agentcrm.domain.customer.Contract;
import com.mckevin.agentcrm.domain.customer.ContractStatus;
import com.mckevin.agentcrm.domain.customer.Customer;
import com.mckevin.agentcrm.domain.customer.CustomerId;
import com.mckevin.agentcrm.domain.customer.ProductLine;
import com.mckevin.agentcrm.domain.customer.Segment;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Fabriques lisibles pour les tests. */
public final class Fixtures {

    public static final LocalDate TODAY = LocalDate.of(2026, 11, 20);

    private Fixtures() {
    }

    public static Customer individual(String id, LocalDate birthDate) {
        return new Customer(new CustomerId(id), "Mme", "Claire", "Martin", birthDate, "claire.martin@exemple.fr",
                "06 00 00 00 00", "Lille", "59000", Segment.INDIVIDUAL, LocalDate.of(2014, 3, 1));
    }

    public static Customer professional(String id) {
        return new Customer(new CustomerId(id), "M.", "Thomas", "Fournier", LocalDate.of(1988, 11, 11), null, null,
                "Lille", "59000", Segment.PROFESSIONAL, LocalDate.of(2018, 4, 3));
    }

    public static Contract contract(String number, ProductLine product, ContractStatus status, int renewalInDays) {
        return new Contract(number, product, product.label() + " " + number, status, new BigDecimal("500.00"),
                TODAY.minusYears(3), TODAY.plusDays(renewalInDays));
    }

    public static Claim claim(String number, ClaimStatus status, int openedDaysAgo) {
        return new Claim(number, "MRH-1", "Dégât des eaux", status, TODAY.minusDays(openedDaysAgo), new BigDecimal("1800"));
    }
}
