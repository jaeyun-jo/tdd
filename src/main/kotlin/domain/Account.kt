package domain

data class Account(
    val id: Long,
    var balance: Long,
    var transferAvailableAmountPerDay: Long,
    var withdrawAvailableAmountAtATime: Long
) {
    fun withdraw(amount: Long, toAccount: Account) {
        balance -= amount
        toAccount.balance += amount
    }
}

