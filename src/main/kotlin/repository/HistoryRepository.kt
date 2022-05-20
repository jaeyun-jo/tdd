package repository

import domain.User
import enums.HistoryType

interface HistoryRepository {
    fun save(user: User, amount: Long, type: HistoryType)
}
