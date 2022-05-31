package exceptions

class ExceedWithdrawalAmountPerDayException : RuntimeException(ERROR_MESSAGE) {
    companion object {
        const val ERROR_MESSAGE = "cannot exceed withdrawal amount per day"
    }

}
