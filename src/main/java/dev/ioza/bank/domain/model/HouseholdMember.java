package dev.ioza.bank.domain.model;

import dev.ioza.bank.domain.enums.MemberRole;

public record HouseholdMember(
    String id,
    String householdId,
    String username,
    String displayName,
    MemberRole role,
    boolean active) {}
