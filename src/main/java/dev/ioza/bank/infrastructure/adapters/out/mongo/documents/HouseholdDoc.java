package dev.ioza.bank.infrastructure.adapters.out.mongo.documents;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("households")
public record HouseholdDoc(
    @Id String id, String name, String currency, String timezone, boolean active) {}
