package com.example.bankapp.service;

import com.example.bankapp.model.Account;
import com.example.bankapp.model.AccountType;
import com.example.bankapp.model.Transaction;
import com.example.bankapp.repository.AccountRepository;
import com.example.bankapp.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

@Service
public class AccountService implements UserDetailsService {

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    public Account findAccountByUsername(String username) {
        Account account = accountRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Account not found"));
        initializeBalances(account);
        return account;
    }

    public Account registerAccount(String username, String password) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username is required");
        }
        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters");
        }
        if (accountRepository.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("Username already exists");
        }

        Account account = new Account();
        account.setUsername(username);
        account.setPassword(passwordEncoder.encode(password));
        account.setBalance(BigDecimal.ZERO);
        account.setCheckingBalance(BigDecimal.ZERO);
        account.setSavingsBalance(BigDecimal.ZERO);
        account.setAccountType(AccountType.CHECKING);
        return accountRepository.save(account);
    }

    public void changePassword(String username, String currentPassword, String password,
                               String confirmPassword) {
        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters");
        }
        if (!password.equals(confirmPassword)) {
            throw new IllegalArgumentException("Passwords do not match");
        }

        Account account = accountRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));
        if (currentPassword == null || !passwordEncoder.matches(currentPassword, account.getPassword())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }
        account.setPassword(passwordEncoder.encode(password));
        accountRepository.save(account);
    }

    @Transactional
    public void deposit(Account account, BigDecimal amount) {
        deposit(account, AccountType.CHECKING, amount);
    }

    @Transactional
    public void deposit(Account account, AccountType accountType, BigDecimal amount) {
        validateAmount(amount);
        initializeBalances(account);
        setBalanceForType(account, accountType, getBalanceForType(account, accountType).add(amount));
        saveWithTotal(account);
        transactionRepository.save(new Transaction(amount,
                "Deposit to " + accountType.getDisplayName(), LocalDateTime.now(), account));
    }

    @Transactional
    public void withdraw(Account account, BigDecimal amount) {
        withdraw(account, AccountType.CHECKING, amount);
    }

    @Transactional
    public void withdraw(Account account, AccountType accountType, BigDecimal amount) {
        validateAmount(amount);
        initializeBalances(account);
        BigDecimal currentBalance = getBalanceForType(account, accountType);
        if (currentBalance.compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient funds in " + accountType.getDisplayName());
        }
        setBalanceForType(account, accountType, currentBalance.subtract(amount));
        saveWithTotal(account);
        transactionRepository.save(new Transaction(amount,
                "Withdrawal from " + accountType.getDisplayName(), LocalDateTime.now(), account));
    }

    public List<Transaction> getTransactionHistory(Account account) {
        return transactionRepository.findByAccountId(account.getId());
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Account account = findAccountByUsername(username);
        return new Account(account.getUsername(), account.getPassword(), account.getBalance(),
                account.getTransactions(), authorities());
    }

    public Collection<? extends GrantedAuthority> authorities() {
        return Arrays.asList(new SimpleGrantedAuthority("USER"));
    }

    @Transactional
    public void transferAmount(Account fromAccount, String toUsername, BigDecimal amount) {
        transferAmount(fromAccount, AccountType.CHECKING, toUsername, AccountType.CHECKING, amount);
    }

    @Transactional
    public void transferAmount(Account fromAccount, AccountType fromType, String toUsername,
                               AccountType toType, BigDecimal amount) {
        validateAmount(amount);
        initializeBalances(fromAccount);
        if (getBalanceForType(fromAccount, fromType).compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient funds in " + fromType.getDisplayName());
        }

        Account toAccount = accountRepository.findByUsername(toUsername)
                .orElseThrow(() -> new RuntimeException("Recipient account not found"));
        initializeBalances(toAccount);

        setBalanceForType(fromAccount, fromType,
                getBalanceForType(fromAccount, fromType).subtract(amount));
        setBalanceForType(toAccount, toType,
                getBalanceForType(toAccount, toType).add(amount));
        saveWithTotal(fromAccount);
        saveWithTotal(toAccount);

        transactionRepository.save(new Transaction(amount,
                "Transfer Out (" + fromType.getDisplayName() + ") to " + toAccount.getUsername(),
                LocalDateTime.now(), fromAccount));
        transactionRepository.save(new Transaction(amount,
                "Transfer In (" + toType.getDisplayName() + ") from " + fromAccount.getUsername(),
                LocalDateTime.now(), toAccount));
    }

    @Transactional
    public void transferBetween(Account account, AccountType fromType, AccountType toType,
                                BigDecimal amount) {
        validateAmount(amount);
        if (fromType == toType) {
            throw new RuntimeException("Choose different account types");
        }
        initializeBalances(account);
        BigDecimal sourceBalance = getBalanceForType(account, fromType);
        if (sourceBalance.compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient funds in " + fromType.getDisplayName());
        }
        setBalanceForType(account, fromType, sourceBalance.subtract(amount));
        setBalanceForType(account, toType, getBalanceForType(account, toType).add(amount));
        saveWithTotal(account);
        transactionRepository.save(new Transaction(amount,
                "Transfer " + fromType.getDisplayName() + " to " + toType.getDisplayName(),
                LocalDateTime.now(), account));
    }

    public BigDecimal getBalanceForType(Account account, AccountType accountType) {
        initializeBalances(account);
        return accountType == AccountType.SAVINGS
                ? account.getSavingsBalance() : account.getCheckingBalance();
    }

    private void initializeBalances(Account account) {
        boolean needsSave = false;

        if (account.getCheckingBalance() == null && account.getSavingsBalance() == null) {
            BigDecimal legacyBalance = account.getBalance() == null ? BigDecimal.ZERO : account.getBalance();
            account.setCheckingBalance(legacyBalance);
            account.setSavingsBalance(BigDecimal.ZERO);
            needsSave = true;
        } else {
            if (account.getCheckingBalance() == null) {
                account.setCheckingBalance(BigDecimal.ZERO);
                needsSave = true;
            }
            if (account.getSavingsBalance() == null) {
                account.setSavingsBalance(BigDecimal.ZERO);
                needsSave = true;
            }
        }

        BigDecimal total = account.getCheckingBalance().add(account.getSavingsBalance());
        if (account.getBalance() == null || account.getBalance().compareTo(total) != 0) {
            account.setBalance(total);
            needsSave = true;
        }

        if (needsSave) {
            accountRepository.save(account);
        }
    }

    private void setBalanceForType(Account account, AccountType accountType, BigDecimal amount) {
        if (accountType == AccountType.SAVINGS) {
            account.setSavingsBalance(amount);
        } else {
            account.setCheckingBalance(amount);
        }
    }

    private void saveWithTotal(Account account) {
        account.setBalance(account.getCheckingBalance().add(account.getSavingsBalance()));
        accountRepository.save(account);
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Amount must be greater than zero");
        }
    }
}
