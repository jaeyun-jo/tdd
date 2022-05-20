import domain.User
import enums.HistoryType
import exceptions.ExceedTransferAmountAtATimeException
import exceptions.ExceedWithdrawAmountPerDayException
import exceptions.LeakOfBalanceException
import exceptions.NotFoundUserException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import repository.HistoryRepository
import repository.TransferRepository
import repository.UserRepository
import java.time.LocalDate
import java.util.*

/**
 *    Test 작성 순서
 *    실패테스트 작성 → 테스트 성공시키기 → 코드 청소하기
 *    쉬운것 예외적인 것 → 어려운것 정상적인 것
 */
/**
 *     요구사항
 *     1. 내 계좌나 상대방 계좌를 조회한다. 존재하지 않으면 exception
 *     2. 내 계좌에 보낼 돈은 보내려는 금액 이상이어야 한다.
 *     3. 하루 출금 가능 금액을 넘었으면 exception
 *     4. 이체한 후의 잔액이 입,출금계좌에 반영되어야 한다.
 *     5. 이체 기록이 남아야 한다.
 */
class TransferTest {

    private val userRepository: UserRepository = mockk()
    private val transferRepository: TransferRepository = mockk()
    private val historyRepository: HistoryRepository = mockk(relaxed = true)

    @InjectMockKs
    private val transferService: TransferService = TransferService(userRepository, transferRepository, historyRepository)

    @Test
    @DisplayName("이체 후에는 이체 기록이 남아야 한다")
    fun givenTransferCompleted_thenHistorySaved() {
        val fromUser = User(
            id = 1L,
            balance = 1000L,
            transferAvailableAmountPerDay = 10000,
            withdrawAvailableAmountAtATime = 1000
        )
        val toUser = User(
            id = 2L,
            balance = 1000L,
            transferAvailableAmountPerDay = 10000,
            withdrawAvailableAmountAtATime = 1000
        )

        every { userRepository.findById(fromUser.id) } returns Optional.of(fromUser)
        every { userRepository.findById(toUser.id) } returns Optional.of(toUser)
        every {
            transferRepository.getTransferAmount(
                fromUser.id,
                LocalDate.now()
            )
        } returns 0
        val amount = 1000L
        transferService.transfer(fromUser.id, toUser.id, amount)
        verify(exactly = 1) { historyRepository.save(fromUser, amount, HistoryType.WITHDRAW) }
    }

    @Test
    @DisplayName("이체 후에는 송금계좌에서 이체금액만큼 빠져나가고 이체계좌에서는 이체금액만큼 더해여쟈 한다")
    fun whenTransferCompleted_thenTransferAmountBalanceShouldBeReducedTransferAmount() {
        val beforeBalance = 1000L
        val fromUser = User(
            id = 1L,
            balance = beforeBalance,
            transferAvailableAmountPerDay = 10000,
            withdrawAvailableAmountAtATime = 1000
        )
        val toUser = User(
            id = 2L,
            balance = 1000L,
            transferAvailableAmountPerDay = 10000,
            withdrawAvailableAmountAtATime = 1000
        )

        every { userRepository.findById(fromUser.id) } returns Optional.of(fromUser)
        every { userRepository.findById(toUser.id) } returns Optional.of(toUser)
        every {
            transferRepository.getTransferAmount(
                fromUser.id,
                LocalDate.now()
            )
        } returns 0

        val transferAmount = 500L
        transferService.transfer(fromUser.id, toUser.id, transferAmount)
        fromUser.balance shouldBe (beforeBalance - transferAmount)
        toUser.balance shouldBe (beforeBalance + transferAmount)
    }

    @Test
    @DisplayName("하루 출금 가능 금액을 초과한 경우 exception이 발생한다")
    fun whenTransferAmountPerDayWasFulled_givenTransferAmount_thenExceedTransferAmountPerDayException() {
        val fromUser = User(
            id = 1L,
            balance = 1000L,
            transferAvailableAmountPerDay = 10000,
            withdrawAvailableAmountAtATime = 1000
        )
        val toUser = User(
            id = 1L,
            balance = 1000L,
            transferAvailableAmountPerDay = 10000,
            withdrawAvailableAmountAtATime = 1000
        )

        every { userRepository.findById(fromUser.id) } returns Optional.of(fromUser)
        every { userRepository.findById(toUser.id) } returns Optional.of(toUser)
        every {
            transferRepository.getTransferAmount(
                fromUser.id,
                LocalDate.now()
            )
        } returns fromUser.transferAvailableAmountPerDay

        val exception = shouldThrow<ExceedWithdrawAmountPerDayException> {
            transferService.transfer(fromUser.id, toUser.id, 1000)
        }
        exception.message shouldBe (ExceedWithdrawAmountPerDayException.ERROR_MESSAGE)
    }

    @Test
    @DisplayName("한번에 출금 가능 금액을 초과한 경우 exception이 발생한다")
    fun whenTransferAmountPerDayWasFulled_givenTransferAmount_thenExceedTransferAmountAtATimeException() {
        val fromUser = User(
            id = 1,
            balance = 10000,
            transferAvailableAmountPerDay = 10000,
            withdrawAvailableAmountAtATime = 500
        )

        val toUser = User(
            id = 2,
            balance = 1000,
            transferAvailableAmountPerDay = 10000,
            withdrawAvailableAmountAtATime = 500
        )
        every { userRepository.findById(fromUser.id) } returns Optional.of(fromUser)
        every { userRepository.findById(toUser.id) } returns Optional.of(toUser)
        val exception = shouldThrow<ExceedTransferAmountAtATimeException> {
            transferService.transfer(fromUser.id, toUser.id, 6000)
        }

        exception.message shouldBe (ExceedTransferAmountAtATimeException.ERROR_MESSAGE)
    }

    @Test
    @DisplayName("내 계좌의 잔액이 보내려는 돈보다 적으면 exception이 발생한다")
    fun whenMyBalanceLessThanTransferAmount_throwLeakOfBalanceException() {
        val fromUser = User(
            id = 1,
            balance = 1000,
            transferAvailableAmountPerDay = 10000,
            withdrawAvailableAmountAtATime = 1000
        )
        val toUser = User(
            id = 2,
            balance = 1000L,
            transferAvailableAmountPerDay = 10000,
            withdrawAvailableAmountAtATime = 1000
        )
        every { userRepository.findById(fromUser.id) } returns Optional.of(fromUser)
        every { userRepository.findById(toUser.id) } returns Optional.of(toUser)

        val exception = shouldThrow<LeakOfBalanceException> {
            transferService.transfer(fromUser.id, toUser.id, fromUser.balance + 1000)
        }
        exception.message shouldBe (LeakOfBalanceException.ERROR_MESSAGE)
    }

    @Test
    @DisplayName("보내거나 받는유저가 없을 경우 exception이 발생한다")
    fun givenInvalidUserId_throwNotFoundUserException() {
        assertNotFoundUserException(0L, 1L)
        assertNotFoundUserException(1L, 0L)

    }

    private fun assertNotFoundUserException(fromUserId: Long, toUserId: Long) {
        every { userRepository.findById(0L) } returns Optional.empty()
        every { userRepository.findById(1L) } returns Optional.of(
            User(
                id = 1,
                balance = 0,
                transferAvailableAmountPerDay = 10000,
                withdrawAvailableAmountAtATime = 1000
            )
        )
        val exception = shouldThrow<NotFoundUserException> {
            transferService.transfer(fromUserId, toUserId, 10000L)
        }
        exception.message shouldBe (NotFoundUserException.ERROR_MESSAGE)
    }
}