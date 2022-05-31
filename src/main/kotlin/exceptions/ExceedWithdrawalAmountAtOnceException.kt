package exceptions

class ExceedWithdrawalAmountAtOnceException : RuntimeException(ERROR_MESSAGE) {
    companion object {
        const val ERROR_MESSAGE = "cannot exceed withdrawal amount at once"
    }

}
