package exceptions

class LeakOfBalanceException : RuntimeException(ERROR_MESSAGE) {

    companion object {
        val ERROR_MESSAGE = "transfer amount must be greater than balance"
    }
}
