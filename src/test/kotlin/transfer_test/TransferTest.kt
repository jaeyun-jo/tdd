package transfer_test

import domain.Account
import enums.HistoryType
import exceptions.AccountNotFoundException
import exceptions.ExceedWithdrawalAmountAtOnceException
import exceptions.ExceedWithdrawalAmountPerDayException
import exceptions.LeakOfBalanceException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.mockk
import io.mockk.verify
import repository.AccountRepository
import repository.HistoryRepository
import repository.TransferRepository
import service.TransferService
import service.TransferValidator
import java.time.LocalDate
import java.util.*

/**
 *    [ Test 작성 순서 ]
 *    실패테스트 작성 → 테스트 성공시키기 → 코드 청소하기
 *    쉬운것 예외적인 것 → 어려운것 정상적인 것
 *
 *    [ 요구 사항 ]
 *    1. 보내거나 받는계좌가 존재해야 한다.
 *    2. 송금액은 송금계좌의 잔액이하여야 한다.
 *    3. 송금액은 하루 출금 가능 금액을 초과할 수 없다.
 *    4. 송금액은 한번에 출금 가능한 금액을 초과할 수 없다.
 *    5. 이체금액이 입,출금계좌에 잔액에 반영되어야 한다.
 *    6. 입,출금 계좌에 이체 기록이 남아야 한다.
 */

val historyRepository: HistoryRepository = mockk(relaxed = true)
val accountRepository: AccountRepository = mockk()
val transferRepository: TransferRepository = mockk()

@InjectMockKs
val transferValidator: TransferValidator = TransferValidator(transferRepository)

@InjectMockKs
val transferService: TransferService = TransferService(accountRepository, transferValidator, historyRepository)

class TransferTest : BehaviorSpec({

    Given("송금이 가능한 상황에서") {
        val transferAmount = 10L
        val beforeAmount = 1000L
        val fromAccount = Account(
            id = 1L,
            balance = beforeAmount,
            withdrawalAvailableAmountPerDay = 10000L,
            withdrawalAvailableAmountAtOnce = 100L
        )
        val toAccount = Account(
            id = 2L,
            balance = beforeAmount
        )
        every { accountRepository.findById(fromAccount.id) } returns Optional.of(fromAccount)
        every { accountRepository.findById(toAccount.id) } returns Optional.of(toAccount)
        every { transferRepository.getWithdrawalAmount(fromAccount, LocalDate.now()) } returns 0L

        When("송금하기를 요청하면") {
            transferService.transfer(fromAccount.id, toAccount.id, transferAmount)
            Then("송신계좌의 잔액은 송금액만큼 빠져냐가야 한다") {
                fromAccount.balance shouldBe (beforeAmount - transferAmount)
            }
            Then("수신계좌의 잔액은 송금액만큼 늘어나야 한다") {
                toAccount.balance shouldBe (beforeAmount + transferAmount)
            }
            Then("이체기록이 남아야 한다") {
                verify(exactly = 1) { historyRepository.save(fromAccount, transferAmount, HistoryType.WITHDRAW) }
                verify(exactly = 1) { historyRepository.save(toAccount, transferAmount, HistoryType.DEBIT) }
            }
        }
    }

    Given("송금액이 한번에 출금 가능한 금액을 초과하는 상황에서") {
        val fromAccount = Account(
            id = 1L,
            balance = 10000L,
            withdrawalAvailableAmountPerDay = 10000L,
            withdrawalAvailableAmountAtOnce = 100L
        )
        val toAccount = Account(
            id = 2L
        )
        every { accountRepository.findById(fromAccount.id) } returns Optional.of(fromAccount)
        every { accountRepository.findById(toAccount.id) } returns Optional.of(toAccount)
        every { transferRepository.getWithdrawalAmount(fromAccount, LocalDate.now()) } returns 0L

        When("송금하기를 요청하면") {
            val exception = shouldThrow<ExceedWithdrawalAmountAtOnceException> {
                transferService.transfer(fromAccount.id, toAccount.id, fromAccount.withdrawalAvailableAmountAtOnce + 1)
            }
            Then("에외가 발생해야한다.") {
                exception.message shouldBe ExceedWithdrawalAmountAtOnceException.ERROR_MESSAGE
            }
        }
    }

    Given("송금액이 하루 출금 가능 금액을 초과한 상황에서") {
        val fromAccount = Account(
            id = 1L,
            balance = 1000L,
            withdrawalAvailableAmountPerDay = 1000L
        )
        val toAccount = Account(
            id = 2L
        )
        every { accountRepository.findById(fromAccount.id) } returns Optional.of(fromAccount)
        every { accountRepository.findById(toAccount.id) } returns Optional.of(toAccount)
        every {
            transferRepository.getWithdrawalAmount(
                fromAccount,
                LocalDate.now()
            )
        } returns fromAccount.withdrawalAvailableAmountPerDay
        When("송금하기를 요청했을 때") {
            val exception = shouldThrow<ExceedWithdrawalAmountPerDayException> {
                transferService.transfer(fromAccount.id, toAccount.id, 1000L)
            }
            Then("예외가 발생해야 한다") {
                exception.message shouldBe ExceedWithdrawalAmountPerDayException.ERROR_MESSAGE
            }
        }
    }

    Given("송금액이 송신계좌의 잔액 이상인 상황에서") {
        val fromAccount = Account(
            id = 1L,
            balance = 1000L
        )
        val toAccount = Account(
            id = 2L
        )
        every { accountRepository.findById(fromAccount.id) } returns Optional.of(fromAccount)
        every { accountRepository.findById(toAccount.id) } returns Optional.of(toAccount)
        When("송금하기를 요청하면") {
            val exception = shouldThrow<LeakOfBalanceException> {
                transferService.transfer(fromAccount.id, toAccount.id, fromAccount.balance + 1)
            }
            Then("예외가 발생해야 한다.") {
                exception.message shouldBe LeakOfBalanceException.ERROR_MESSAGE
            }
        }
    }


    Given("보내거나 받는계좌가 존재하지 않는 상황일 때") {
        every { accountRepository.findById(-1L) } returns Optional.empty()
        every { accountRepository.findById(1L) } returns Optional.of(Account(id = 1L))
        When("송금하기를 요청하면") {
            val exception = shouldThrow<AccountNotFoundException> {
                transferService.transfer(-1L, 1L, 1000L)
            }
            Then("예외가 발생해야한다. (송신 계좌가 없을 때)") {
                exception.message shouldBe AccountNotFoundException.ERROR_MESSAGE
            }
        }

        When("송금하기를 요청하면") {
            val exception = shouldThrow<AccountNotFoundException> {
                transferService.transfer(1L, -1L, 1000L)
            }
            Then("예외가 발생해야한다. (수신 계좌가 없을 때)") {
                exception.message shouldBe AccountNotFoundException.ERROR_MESSAGE
            }
        }
    }
})