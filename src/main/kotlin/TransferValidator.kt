import domain.Account
import exceptions.ExceedTransferAmountAtATimeException
import exceptions.ExceedWithdrawAmountPerDayException
import exceptions.LeakOfBalanceException
import repository.TransferRepository
import java.time.LocalDate

class TransferValidator(
   private val transferRepository: TransferRepository
) {

    fun validateTransfer(fromAccount: Account, amount: Long) {
        if (fromAccount.balance < amount) {
            throw LeakOfBalanceException()
        }

        if (fromAccount.withdrawAvailableAmountAtATime < amount) {
            throw ExceedTransferAmountAtATimeException()
        }

        val todayTransferAmount = transferRepository.getTransferAmount(fromAccount.id, LocalDate.now())

        if (fromAccount.transferAvailableAmountPerDay <= todayTransferAmount + amount) {
            throw ExceedWithdrawAmountPerDayException()
        }
    }
}