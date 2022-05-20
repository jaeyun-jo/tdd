package repository

import java.time.LocalDate

interface TransferRepository {
    fun getTransferAmount(userId: Long, date: LocalDate): Long
}
