package service

import domain.Account
import enums.HistoryType
import exceptions.AccountNotFoundException
import repository.HistoryRepository
import repository.UserRepository

class TransferService(
    private val userRepository: UserRepository,
    private val transferValidator: TransferValidator,
    private val historyRepository: HistoryRepository
) {
    fun transfer(fromUserId: Long, toUserId: Long, amount: Long) {
        val fromAccount: Account = userRepository.findById(fromUserId).orElseThrow { AccountNotFoundException() }
        val toAccount: Account = userRepository.findById(toUserId).orElseThrow { AccountNotFoundException() }
        transferValidator.validateTransfer(fromAccount, amount)
        fromAccount.withdraw(amount, toAccount)
        historyRepository.save(fromAccount, amount, HistoryType.WITHDRAW)
        historyRepository.save(toAccount, amount, HistoryType.DEPOSIT)
    }
}