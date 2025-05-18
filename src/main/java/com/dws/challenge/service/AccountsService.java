package com.dws.challenge.service;

import com.dws.challenge.domain.Account;
import com.dws.challenge.exception.InsufficientBalanceException;
import com.dws.challenge.exception.InvalidTransferRequestException;
import com.dws.challenge.repository.AccountsRepository;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.Objects;

@Service
public class AccountsService {

  @Getter
  private final AccountsRepository accountsRepository;
  private final NotificationService notificationService;

  @Autowired
  public AccountsService(AccountsRepository accountsRepository, NotificationService notificationService) {
    this.accountsRepository = accountsRepository;
    this.notificationService = notificationService;
  }

  public void createAccount(Account account) {
    this.accountsRepository.createAccount(account);
  }

  public Account getAccount(String accountId) {
    return this.accountsRepository.getAccount(accountId);
  }

  public void transfer(String fromId, String toId, BigDecimal amount) {
    if (Objects.equals(fromId, toId)) {
      throw new InvalidTransferRequestException("Cannot transfer to the same account.");
    }

    if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
      throw new InvalidTransferRequestException("Transfer amount must be greater than zero.");
    }

    Account fromAccount = getAccount(fromId);
    Account toAccount = getAccount(toId);

    if (fromAccount == null || toAccount == null) {
      throw new InvalidTransferRequestException("Source or destination account does not exist.");
    }

    Account firstLock = fromId.compareTo(toId) < 0 ? fromAccount : toAccount;
    Account secondLock = fromId.compareTo(toId) < 0 ? toAccount : fromAccount;

    synchronized (firstLock) {
      synchronized (secondLock) {
        if (fromAccount.getBalance().compareTo(amount) < 0) {
          throw new InsufficientBalanceException("Account " + fromId + " has insufficient balance.");
        }

        fromAccount.setBalance(fromAccount.getBalance().subtract(amount));
        toAccount.setBalance(toAccount.getBalance().add(amount));
      }
    }

    notify(fromId, toId, amount, fromAccount, toAccount);
  }

  private void notify(String fromId, String toId, BigDecimal amount, Account fromAccount, Account toAccount) {
    String fromMessage = String.format("Transferred %s to account %s", amount, toId);
    String toMessage = String.format("Received %s from account %s", amount, fromId);

    notificationService.notifyAboutTransfer(fromAccount, fromMessage);
    notificationService.notifyAboutTransfer(toAccount, toMessage);
  }
}

