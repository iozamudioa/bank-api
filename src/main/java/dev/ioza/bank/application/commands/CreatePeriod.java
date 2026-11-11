package dev.ioza.bank.application.commands;

import java.time.LocalDate;

public record CreatePeriod(LocalDate startDate, LocalDate endDate, boolean open) {}
