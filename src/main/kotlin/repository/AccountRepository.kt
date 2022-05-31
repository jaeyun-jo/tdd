package repository

import domain.Account
import java.util.*

interface AccountRepository {
    fun findById(accountId: Long): Optional<Account>
}
