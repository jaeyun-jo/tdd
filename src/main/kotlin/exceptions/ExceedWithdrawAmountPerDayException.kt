package exceptions

class ExceedWithdrawAmountPerDayException : RuntimeException(ERROR_MESSAGE) {

    companion object {
        val ERROR_MESSAGE = "cannot exceed withdraw amount per day"
    }
}
