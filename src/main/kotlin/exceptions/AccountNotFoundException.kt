package exceptions

class AccountNotFoundException : RuntimeException(ERROR_MESSAGE) {
    companion object {
        const val ERROR_MESSAGE = "cannot found account"
    }
}
