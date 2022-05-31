package service

import enums.HistoryType
import exceptions.AccountNotFoundException
import exceptions.ExceedWithdrawalAmountAtOnceException
import exceptions.ExceedWithdrawalAmountPerDayException
import exceptions.LeakOfBalanceException
import repository.AccountRepository
import repository.HistoryRepository
import repository.TransferRepository
import java.time.LocalDate

class TransferService(
    private val accountRepository: AccountRepository,
    private val transferValidator: TransferValidator,
    private val historyRepository: HistoryRepository
) {

    fun transfer(fromAccountId: Long, toAccountId: Long, amount: Long) {
        val fromAccount = accountRepository.findById(fromAccountId).orElseThrow { AccountNotFoundException() }
        val toAccount = accountRepository.findById(toAccountId).orElseThrow { AccountNotFoundException() }
        transferValidator.validateTransfertaion(fromAccount, amount)
        fromAccount.withdraw(toAccount, amount)
        historyRepository.save(fromAccount, amount, HistoryType.WITHDRAW)
        historyRepository.save(toAccount, amount, HistoryType.DEBIT)
    }

}
