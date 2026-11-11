package dev.ioza.bank.application.commands;

public record CorrectMovement(String periodId, String reason, MovementReplacement replacement) {}
