import domain.Account
import enums.HistoryType
import exceptions.ExceedTransferAmountAtATimeException
import exceptions.ExceedWithdrawAmountPerDayException
import exceptions.LeakOfBalanceException
import exceptions.AccountNotFoundException
import repository.HistoryRepository
import repository.TransferRepository
import repository.UserRepository
import java.time.LocalDate

class TransferService(
    private val userRepository: UserRepository,
    private val transferRepository: TransferRepository,
    private val historyRepository: HistoryRepository
) {
    fun transfer(fromUserId: Long, toUserId: Long, amount: Long) {
        val fromAccount: Account = userRepository.findById(fromUserId).orElseThrow { AccountNotFoundException() }
        val toAccount: Account = userRepository.findById(toUserId).orElseThrow { AccountNotFoundException() }

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

        fromAccount.balance -= amount
        toAccount.balance += amount

        historyRepository.save(fromAccount, amount, HistoryType.WITHDRAW)
    }
}
