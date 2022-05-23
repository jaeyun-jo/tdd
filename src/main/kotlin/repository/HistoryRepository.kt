package repository

import domain.Account
import enums.HistoryType

interface HistoryRepository {
    fun save(account: Account, amount: Long, type: HistoryType)
}
