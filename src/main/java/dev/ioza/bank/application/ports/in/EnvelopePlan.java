package dev.ioza.bank.application.ports.in;

import dev.ioza.bank.domain.model.Envelope;
import java.math.BigDecimal;

/** Read model; habitualAmount comes from the template, not the envelope balance. */
public record EnvelopePlan(Envelope envelope, BigDecimal habitualAmount) {}
