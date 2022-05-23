package exceptions

class AccountNotFoundException : RuntimeException(ERROR_MESSAGE) {

    companion object {
        val ERROR_MESSAGE: String = "cannot found account"
    }
}

