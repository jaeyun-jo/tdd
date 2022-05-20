package repository

import domain.User
import java.util.*

interface UserRepository {
    fun findById(id: Long): Optional<User>
}
