package dev.ioza.bank.application.commands;

import dev.ioza.bank.domain.enums.MemberRole;

public record AddMember(String username, String displayName, MemberRole role) {}
