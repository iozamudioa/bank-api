package dev.ioza.bank.application.usecase;

import dev.ioza.bank.application.commands.CorrectMovement;
import dev.ioza.bank.application.ports.in.CorrectionPreview;
import dev.ioza.bank.domain.model.Movement;

public interface CorrectFinancialMovement {
  Movement correct(String householdId, String movementId, CorrectMovement command);

  CorrectionPreview previewCorrection(
      String householdId, String movementId, CorrectMovement command);
}
