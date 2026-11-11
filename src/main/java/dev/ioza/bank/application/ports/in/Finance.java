package dev.ioza.bank.application.ports.in;

import dev.ioza.bank.application.queries.MovementQuery;
import dev.ioza.bank.application.usecase.AllocateMoney;
import dev.ioza.bank.application.usecase.CorrectFinancialMovement;
import dev.ioza.bank.application.usecase.PreviewExpense;
import dev.ioza.bank.application.usecase.ReallocateEnvelopes;
import dev.ioza.bank.application.usecase.RegisterAccountExpense;
import dev.ioza.bank.application.usecase.RegisterExpense;
import dev.ioza.bank.application.usecase.RegisterIncome;
import dev.ioza.bank.application.usecase.RegisterOpeningBalance;
import dev.ioza.bank.application.usecase.ReleaseReserve;
import dev.ioza.bank.application.usecase.ReverseMovement;
import dev.ioza.bank.application.usecase.TransferAccounts;
import dev.ioza.bank.application.usecase.TransferEnvelopes;
import dev.ioza.bank.domain.model.Movement;
import java.util.List;

/** Input facade composing specific financial use cases and movement queries. */
public interface Finance
    extends RegisterOpeningBalance,
        RegisterIncome,
        AllocateMoney,
        ReleaseReserve,
        ReallocateEnvelopes,
        TransferAccounts,
        TransferEnvelopes,
        PreviewExpense,
        RegisterExpense,
        ReverseMovement,
        RegisterAccountExpense,
        CorrectFinancialMovement {
  List<Movement> movements(String householdId, MovementQuery query);

  List<Movement> movements(String householdId);

  Movement movement(String householdId, String movementId);
}
