package com.dws.challenge;

import com.dws.challenge.domain.Account;
import com.dws.challenge.exception.InsufficientBalanceException;
import com.dws.challenge.exception.InvalidTransferRequestException;
import com.dws.challenge.repository.AccountsRepository;
import com.dws.challenge.service.AccountsService;
import com.dws.challenge.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import java.math.BigDecimal;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class AccountsServiceTest {

  @Mock
  private AccountsRepository accountsRepository;

  @Mock
  private NotificationService notificationService;

  @InjectMocks
  private AccountsService accountsService;

  private Account fromAccount;
  private Account toAccount;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);

    fromAccount = new Account("Id-From", new BigDecimal("1000"));
    toAccount = new Account("Id-To", new BigDecimal("500"));

    when(accountsRepository.getAccount("Id-From")).thenReturn(fromAccount);
    when(accountsRepository.getAccount("Id-To")).thenReturn(toAccount);
  }

  @Test
  void createAccount_delegatesToRepository() {
    Account account = new Account("Id-123", BigDecimal.TEN);
    accountsService.createAccount(account);
    verify(accountsRepository).createAccount(account);
  }

  @Test
  void getAccount_delegatesToRepository() {
    Account result = accountsService.getAccount("Id-From");
    assertThat(result).isSameAs(fromAccount);
  }

  @Test
  void transfer_successfulTransfer_updatesBalancesAndSendsNotifications() {
    BigDecimal transferAmount = new BigDecimal("300");

    accountsService.transfer("Id-From", "Id-To", transferAmount);

    assertThat(fromAccount.getBalance()).isEqualByComparingTo("700");
    assertThat(toAccount.getBalance()).isEqualByComparingTo("800");

    verify(notificationService).notifyAboutTransfer(fromAccount, "Transferred 300 to account Id-To");
    verify(notificationService).notifyAboutTransfer(toAccount, "Received 300 from account Id-From");

    verifyNoMoreInteractions(notificationService);
  }

  @Test
  void transfer_sameAccount_throwsInvalidTransferRequestException() {
    assertThatThrownBy(() -> accountsService.transfer("Id-From", "Id-From", BigDecimal.ONE))
            .isInstanceOf(InvalidTransferRequestException.class)
            .hasMessage("Cannot transfer to the same account.");
  }

  @Test
  void transfer_nullAmount_throwsInvalidTransferRequestException() {
    assertThatThrownBy(() -> accountsService.transfer("Id-From", "Id-To", null))
            .isInstanceOf(InvalidTransferRequestException.class)
            .hasMessage("Transfer amount must be greater than zero.");
  }

  @Test
  void transfer_zeroOrNegativeAmount_throwsInvalidTransferRequestException() {
    assertThatThrownBy(() -> accountsService.transfer("Id-From", "Id-To", BigDecimal.ZERO))
            .isInstanceOf(InvalidTransferRequestException.class)
            .hasMessage("Transfer amount must be greater than zero.");

    assertThatThrownBy(() -> accountsService.transfer("Id-From", "Id-To", new BigDecimal("-1")))
            .isInstanceOf(InvalidTransferRequestException.class)
            .hasMessage("Transfer amount must be greater than zero.");
  }

  @Test
  void transfer_insufficientBalance_throwsInsufficientBalanceException() {
    BigDecimal amount = new BigDecimal("1500"); // more than fromAccount balance

    assertThatThrownBy(() -> accountsService.transfer("Id-From", "Id-To", amount))
            .isInstanceOf(InsufficientBalanceException.class)
            .hasMessageContaining("Account Id-From has insufficient balance.");
  }

  @Test
  void transfer_givenNonExistentSourceAccount_throwsInvalidTransferRequestException() {
    when(accountsRepository.getAccount("NonExistent")).thenReturn(null);

    assertThatThrownBy(() -> accountsService.transfer("NonExistent", "Id-To", BigDecimal.TEN))
            .isInstanceOf(InvalidTransferRequestException.class)
            .hasMessage("Source or destination account does not exist.");
  }

  @Test
  void transfer_givenNonExistentDestinationAccount_throwsInvalidTransferRequestException() {
    when(accountsRepository.getAccount("NonExistent")).thenReturn(null);

    assertThatThrownBy(() -> accountsService.transfer("Id-From", "NonExistent", BigDecimal.TEN))
            .isInstanceOf(InvalidTransferRequestException.class)
            .hasMessage("Source or destination account does not exist.");
  }


  @Test
  void transfer_locksInConsistentOrderToAvoidDeadlock() throws InterruptedException {

    fromAccount.setBalance(new BigDecimal("10000"));
    toAccount.setBalance(new BigDecimal("10000"));

    Runnable transfer1 = () -> accountsService.transfer("Id-From", "Id-To", new BigDecimal("100"));
    Runnable transfer2 = () -> accountsService.transfer("Id-To", "Id-From", new BigDecimal("200"));

    Thread t1 = new Thread(transfer1);
    Thread t2 = new Thread(transfer2);

    t1.start();
    t2.start();

    t1.join();
    t2.join();

    assertThat(fromAccount.getBalance()).isEqualByComparingTo("10100");
    assertThat(toAccount.getBalance()).isEqualByComparingTo("9900");
  }
}
