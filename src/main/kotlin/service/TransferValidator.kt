package service

import domain.Account
import exceptions.ExceedWithdrawalAmountAtOnceException
import exceptions.ExceedWithdrawalAmountPerDayException
import exceptions.LeakOfBalanceException
import repository.TransferRepository
import java.time.LocalDate

class TransferValidator(
    private val transferRepository: TransferRepository
) {
    fun validateTransfertaion(fromAccount: Account, amount: Long) {
        if(fromAccount.balance < amount) {
            throw LeakOfBalanceException()
        }

        val todayWithdrawalAmount = transferRepository.getWithdrawalAmount(fromAccount, LocalDate.now())
        if(todayWithdrawalAmount + amount > fromAccount.withdrawalAvailableAmountPerDay) {
            throw ExceedWithdrawalAmountPerDayException()
        }

        if(fromAccount.withdrawalAvailableAmountAtOnce < amount) {
            throw ExceedWithdrawalAmountAtOnceException()
        }
    }
}