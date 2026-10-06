package com.example.bankapp.service;

import com.example.bankapp.model.Account;
import com.example.bankapp.repository.AccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class AccountServiceBalanceSyncTest {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private AccountService accountService;

    @Test
    void findAccountByUsernamePreservesTotalWhenOneSubBalanceIsMissing() {
        Account account = new Account();
        account.setUsername("balance-sync-test");
        account.setBalance(new BigDecimal("100.00"));
        account.setCheckingBalance(new BigDecimal("35.00"));
        account.setSavingsBalance(null);
        accountRepository.save(account);

        Account loaded = accountService.findAccountByUsername(account.getUsername());

        assertThat(loaded.getCheckingBalance()).isEqualByComparingTo("35.00");
        assertThat(loaded.getSavingsBalance()).isEqualByComparingTo("65.00");
        assertThat(loaded.getBalance()).isEqualByComparingTo("100.00");
    }
}