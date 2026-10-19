package com.example.bankapp.config;

import com.example.bankapp.model.Account;
import com.example.bankapp.model.AccountType;
import com.example.bankapp.repository.AccountRepository;
import com.example.bankapp.service.AccountService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Component
@ConditionalOnProperty(name = "app.demo-data.enabled", havingValue = "true")
public class DemoDataInitializer implements CommandLineRunner {
    public static final String DEMO_USERNAME = "demo";
    public static final String DEMO_PASSWORD = "DemoBank2026!";
    public static final String RECIPIENT_USERNAME = "alex";

    private final AccountRepository accountRepository;
    private final AccountService accountService;

    public DemoDataInitializer(AccountRepository accountRepository, AccountService accountService) {
        this.accountRepository = accountRepository;
        this.accountService = accountService;
    }

    @Override
    @Transactional
    public void run(String... args) {
        boolean demoExists = accountRepository.findByUsername(DEMO_USERNAME).isPresent();
        boolean recipientExists = accountRepository.findByUsername(RECIPIENT_USERNAME).isPresent();

        Account demo = demoExists
                ? accountRepository.findByUsername(DEMO_USERNAME).orElseThrow()
                : accountService.registerAccount(DEMO_USERNAME, DEMO_PASSWORD);
        Account recipient = recipientExists
                ? accountRepository.findByUsername(RECIPIENT_USERNAME).orElseThrow()
                : accountService.registerAccount(RECIPIENT_USERNAME, DEMO_PASSWORD);

        if (!demoExists) {
            accountService.deposit(demo, AccountType.CHECKING, new BigDecimal("1850.00"));
            accountService.deposit(demo, AccountType.SAVINGS, new BigDecimal("4200.00"));
        }
        if (!recipientExists) {
            accountService.deposit(recipient, AccountType.CHECKING, new BigDecimal("725.00"));
            accountService.deposit(recipient, AccountType.SAVINGS, new BigDecimal("1200.00"));
        }
        if (!demoExists && !recipientExists) {
            accountService.transferAmount(demo, AccountType.CHECKING, RECIPIENT_USERNAME,
                    AccountType.CHECKING, new BigDecimal("125.00"));
        }
    }
}