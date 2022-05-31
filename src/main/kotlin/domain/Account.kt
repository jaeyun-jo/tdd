package domain

data class Account(
    val id: Long,
    var balance: Long = 0L,
    val withdrawalAvailableAmountPerDay: Long = 0L,
    val withdrawalAvailableAmountAtOnce: Long = 0L
) {

    fun withdraw(toAccount: Account, amount: Long) {
        balance -= amount
        toAccount.balance += amount
    }
}
