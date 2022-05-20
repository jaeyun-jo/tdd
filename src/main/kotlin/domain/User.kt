package domain

data class User (
    val id: Long,
    var balance: Long,
    var transferAvailableAmountPerDay: Long,
    var withdrawAvailableAmountAtATime: Long
)
