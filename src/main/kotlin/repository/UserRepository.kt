package repository

import domain.Account
import java.util.*

interface UserRepository {
    fun findById(id: Long): Optional<Account>
}
