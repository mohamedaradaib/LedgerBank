package com.example.bankapp.controller;

import com.example.bankapp.model.Account;
import com.example.bankapp.model.AccountType;
import com.example.bankapp.service.AccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;

@Controller
public class BankController {

    @Autowired
    private AccountService accountService;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Account account = accountService.findAccountByUsername(username);
        model.addAttribute("account", account);
        model.addAttribute("checkingBalance", accountService.getBalanceForType(account, AccountType.CHECKING));
        model.addAttribute("savingsBalance", accountService.getBalanceForType(account, AccountType.SAVINGS));
        return "dashboard";
    }

    @GetMapping("/register")
    public String showRegistrationForm() {
        return "register";
    }

    @PostMapping("/register")
    public String registerAccount(@RequestParam String username, @RequestParam String password,
                                  Model model) {
        try {
            accountService.registerAccount(username, password);
            return "redirect:/login";
        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage());
            return "register";
        }
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @PostMapping("/deposit")
    public String deposit(@RequestParam BigDecimal amount,
                          @RequestParam(defaultValue = "CHECKING") AccountType accountType) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Account account = accountService.findAccountByUsername(username);
        accountService.deposit(account, accountType, amount);
        return "redirect:/dashboard";
    }

    @PostMapping("/withdraw")
    public String withdraw(@RequestParam BigDecimal amount,
                           @RequestParam(defaultValue = "CHECKING") AccountType accountType, Model model) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Account account = accountService.findAccountByUsername(username);

        try {
            accountService.withdraw(account, accountType, amount);
        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("account", account);
            return "dashboard";
        }

        return "redirect:/dashboard";
    }

    @GetMapping("/transactions")
    public String transactionHistory(Model model) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Account account = accountService.findAccountByUsername(username);
        model.addAttribute("transactions", accountService.getTransactionHistory(account));
        return "transactions";
    }

    @PostMapping("/transfer")
    public String transferAmount(@RequestParam String toUsername, @RequestParam BigDecimal amount,
                                 @RequestParam(defaultValue = "CHECKING") AccountType fromType,
                                 @RequestParam(defaultValue = "CHECKING") AccountType toType, Model model) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Account fromAccount = accountService.findAccountByUsername(username);

        try {
            accountService.transferAmount(fromAccount, fromType, toUsername, toType, amount);
        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("account", fromAccount);
            return "dashboard";
        }

        return "redirect:/dashboard";
    }

    @PostMapping("/transfer-between")
    public String transferBetween(@RequestParam AccountType fromType, @RequestParam AccountType toType,
                                  @RequestParam BigDecimal amount, Model model) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Account account = accountService.findAccountByUsername(username);
        try {
            accountService.transferBetween(account, fromType, toType, amount);
        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("account", account);
            model.addAttribute("checkingBalance", accountService.getBalanceForType(account, AccountType.CHECKING));
            model.addAttribute("savingsBalance", accountService.getBalanceForType(account, AccountType.SAVINGS));
            return "dashboard";
        }
        return "redirect:/dashboard";
    }

    @GetMapping("/credentials")
    public String credentials() {
        return "credentials";
    }

    @PostMapping("/credentials")
    public String resetPassword(@RequestParam String username, @RequestParam String password,
                                @RequestParam String confirmPassword, Model model) {
        try {
            accountService.resetPassword(username, password, confirmPassword);
            return "redirect:/login?reset";
        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("username", username);
            return "credentials";
        }
    }

}
