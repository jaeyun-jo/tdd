import domain.User
import enums.HistoryType
import exceptions.ExceedTransferAmountAtATimeException
import exceptions.ExceedWithdrawAmountPerDayException
import exceptions.LeakOfBalanceException
import exceptions.NotFoundUserException
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
        val fromUser: User = userRepository.findById(fromUserId).orElseThrow { NotFoundUserException() }
        val toUser: User = userRepository.findById(toUserId).orElseThrow { NotFoundUserException() }

        if (fromUser.balance < amount) {
            throw LeakOfBalanceException()
        }

        if (fromUser.withdrawAvailableAmountAtATime < amount) {
            throw ExceedTransferAmountAtATimeException()
        }

        val todayTransferAmount = transferRepository.getTransferAmount(fromUser.id, LocalDate.now())

        if (fromUser.transferAvailableAmountPerDay <= todayTransferAmount + amount) {
            throw ExceedWithdrawAmountPerDayException()
        }

        fromUser.balance -= amount
        toUser.balance += amount

        historyRepository.save(fromUser, amount, HistoryType.WITHDRAW)
    }
}
