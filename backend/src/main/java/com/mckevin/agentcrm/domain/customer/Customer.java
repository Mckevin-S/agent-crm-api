package com.mckevin.agentcrm.domain.customer;

import java.time.LocalDate;
import java.time.Period;

/** Identité d'un client, issue du CRM. */
public record Customer(CustomerId id, String title, String firstName, String lastName, LocalDate birthDate, String email,
                       String phone, String city, String postalCode, Segment segment, LocalDate customerSince) {

    public String fullName() {
        return firstName + " " + lastName;
    }

    public int ageOn(LocalDate day) {
        return birthDate == null ? 0 : Period.between(birthDate, day).getYears();
    }

    public int seniorityYearsOn(LocalDate day) {
        return customerSince == null ? 0 : Period.between(customerSince, day).getYears();
    }
}
