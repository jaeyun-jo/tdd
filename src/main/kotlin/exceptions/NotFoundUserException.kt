package exceptions

class NotFoundUserException : RuntimeException(ERROR_MESSAGE) {

    companion object {
        val ERROR_MESSAGE: String = "cannot found user"
    }
}

