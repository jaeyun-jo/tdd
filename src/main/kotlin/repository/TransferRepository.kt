package repository

import domain.Account
import java.time.LocalDate

interface TransferRepository {
    fun getWithdrawalAmount(account: Account, date: LocalDate): Long
}
