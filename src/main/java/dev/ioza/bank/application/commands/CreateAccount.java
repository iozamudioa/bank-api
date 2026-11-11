package dev.ioza.bank.application.commands;

import dev.ioza.bank.domain.enums.AccountType;

public record CreateAccount(String name, AccountType type) {}
