package dev.ioza.bank.infrastructure.adapters.out.mongo.documents;

import dev.ioza.bank.domain.enums.MemberRole;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("household_members")
@CompoundIndex(
    name = "member_household_username",
    def = "{'householdId':1,'username':1}",
    unique = true)
public record HouseholdMemberDoc(
    @Id String id,
    String householdId,
    String username,
    String displayName,
    MemberRole role,
    boolean active) {}
