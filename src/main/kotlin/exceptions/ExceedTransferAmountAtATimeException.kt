package exceptions

class ExceedTransferAmountAtATimeException : RuntimeException(ERROR_MESSAGE) {
    companion object {
        val ERROR_MESSAGE = "cannot exceed transfer amount at a time"
    }

}
