package transfer_test

import TransferService
import TransferValidator
import domain.Account
import enums.HistoryType
import exceptions.AccountNotFoundException
import exceptions.ExceedWithdrawAmountPerDayException
import exceptions.LeakOfBalanceException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.mockk
import io.mockk.verify
import repository.HistoryRepository
import repository.TransferRepository
import repository.UserRepository
import java.time.LocalDate
import java.util.*

/**
 *    [ Test 작성 순서 ]
 *    실패테스트 작성 → 테스트 성공시키기 → 코드 청소하기
 *    쉬운것 예외적인 것 → 어려운것 정상적인 것
 */

class TransferTest : BehaviorSpec({
    val userRepository: UserRepository = mockk()
    val transferRepository: TransferRepository = mockk()
    val historyRepository: HistoryRepository = mockk(relaxed = true)

    @InjectMockKs
    val transferValidator: TransferValidator = TransferValidator(transferRepository)

    @InjectMockKs
    val transferService: TransferService = TransferService(userRepository, transferValidator, historyRepository)


    Given("보내는 사람의 계좌의 잔액이 송금액보다 작은 상황에서") {
        val fromAccount = Account(
            id = 1,
            balance = 1000,
            transferAvailableAmountPerDay = 10000,
            withdrawAvailableAmountAtATime = 1000
        )
        val toAccount = Account(
            id = 2,
            balance = 1000L,
            transferAvailableAmountPerDay = 10000,
            withdrawAvailableAmountAtATime = 1000
        )
        every { userRepository.findById(fromAccount.id) } returns Optional.of(fromAccount)
        every { userRepository.findById(toAccount.id) } returns Optional.of(toAccount)
        When("송금하기를 요청하면") {
            Then("LeakOfBalanceException 예외가 발생한다") {
                val exception = shouldThrow<LeakOfBalanceException> {
                    transferService.transfer(fromAccount.id, toAccount.id, fromAccount.balance + 1000)
                }
                exception.message shouldBe (LeakOfBalanceException.ERROR_MESSAGE)
            }
        }
    }

    Given("송금액을 포함했을 때 하루 출금 가능 금액이 초과한 상태에서") {
        val fromAccount = Account(
            id = 1L,
            balance = 1000L,
            transferAvailableAmountPerDay = 10000,
            withdrawAvailableAmountAtATime = 1000
        )
        val toAccount = Account(
            id = 1L,
            balance = 1000L,
            transferAvailableAmountPerDay = 10000,
            withdrawAvailableAmountAtATime = 1000
        )

        every { userRepository.findById(fromAccount.id) } returns Optional.of(fromAccount)
        every { userRepository.findById(toAccount.id) } returns Optional.of(toAccount)
        every {
            transferRepository.getTransferAmount(
                fromAccount.id,
                LocalDate.now()
            )
        } returns fromAccount.transferAvailableAmountPerDay
        When("송금하기를 요청하면") {
            Then("ExceedWithdrawAmountPerDayException 예외가 발생한다") {
                val exception = shouldThrow<ExceedWithdrawAmountPerDayException> {
                    transferService.transfer(fromAccount.id, toAccount.id, 1000)
                }
                exception.message shouldBe (ExceedWithdrawAmountPerDayException.ERROR_MESSAGE)
            }
        }
    }

    Given("보내는 계좌의 잔액이 송금액보다 많고 하루 출금 금액이 초과하지 않는 상황에서") {
        val beforeBalance = 1000L
        val transferAmount = 500L
        val fromAccount = Account(
            id = 1L,
            balance = beforeBalance,
            transferAvailableAmountPerDay = 10000,
            withdrawAvailableAmountAtATime = 1000
        )
        val toAccount = Account(
            id = 2L,
            balance = 1000L,
            transferAvailableAmountPerDay = 10000,
            withdrawAvailableAmountAtATime = 1000
        )

        every { userRepository.findById(fromAccount.id) } returns Optional.of(fromAccount)
        every { userRepository.findById(toAccount.id) } returns Optional.of(toAccount)
        every {
            transferRepository.getTransferAmount(
                fromAccount.id,
                LocalDate.now()
            )
        } returns 0
        When("송금하기를 요청하면") {
            transferService.transfer(fromAccount.id, toAccount.id, transferAmount)
            Then("송금계좌에서 이체금액만큼 줄어든다") {
                fromAccount.balance shouldBe (beforeBalance - transferAmount)
            }
            Then("이체계좌에서는 이체금액만큼 늘어난다") {
                toAccount.balance shouldBe (beforeBalance + transferAmount)
            }
            Then("이체 기록이 남아야 한다") {
                verify(exactly = 1) { historyRepository.save(fromAccount, transferAmount, HistoryType.WITHDRAW) }
            }
        }
    }

    Given("보내거나 받는계좌가 존재하지 않는 상황일때") {
        val emptyAccountId = 0L
        val existAccountId = 1L
        every { userRepository.findById(emptyAccountId) } returns Optional.empty()
        every { userRepository.findById(existAccountId) } returns Optional.of(
            Account(
                id = existAccountId,
                balance = 0,
                transferAvailableAmountPerDay = 10000,
                withdrawAvailableAmountAtATime = 1000
            )
        )
        When("송금하기를 요청하면") {
            Then("AccountNotFoundException 예외가 발생한다(송금계좌가 없을 때)") {
                val exception = shouldThrow<AccountNotFoundException> {
                    transferService.transfer(emptyAccountId, existAccountId, 10000L)
                }
                exception.message shouldBe (AccountNotFoundException.ERROR_MESSAGE)
            }

            Then("AccountNotFoundException 예외가 발생한다(수신계좌가 없을 때)") {
                val exception = shouldThrow<AccountNotFoundException> {
                    transferService.transfer(existAccountId, emptyAccountId, 10000L)
                }
                exception.message shouldBe (AccountNotFoundException.ERROR_MESSAGE)
            }
        }
    }
})