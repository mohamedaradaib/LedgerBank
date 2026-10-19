package com.example.bankapp.controller;

import com.example.bankapp.model.Account;
import com.example.bankapp.model.AccountType;
import com.example.bankapp.model.Transaction;
import com.example.bankapp.service.AccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;

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
    public String login(Model model) {
        return "login";
    }

    @PostMapping("/deposit")
    public String deposit(@RequestParam BigDecimal amount,
                          @RequestParam(defaultValue = "CHECKING") AccountType accountType, Model model) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Account account = accountService.findAccountByUsername(username);
        try {
            accountService.deposit(account, accountType, amount);
        } catch (RuntimeException e) {
            addDashboardAttributes(model, account);
            model.addAttribute("error", e.getMessage());
            return "dashboard";
        }
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
            addDashboardAttributes(model, account);
            model.addAttribute("error", e.getMessage());
            return "dashboard";
        }

        return "redirect:/dashboard";
    }

    @GetMapping("/transactions")
    public String transactionHistory(@RequestParam(required = false) String q,
                                     @RequestParam(defaultValue = "ALL") String type,
                                     @RequestParam(required = false)
                                     @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
                                     @RequestParam(required = false)
                                     @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
                                     Model model) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Account account = accountService.findAccountByUsername(username);
        String transactionType = type == null ? "ALL" : type.toUpperCase(Locale.ROOT);
        if (!Set.of("ALL", "DEPOSIT", "WITHDRAWAL", "TRANSFER").contains(transactionType)) {
            transactionType = "ALL";
        }
        String searchTerm = q == null || q.isBlank() ? null : q.trim();
        boolean invalidDateRange = fromDate != null && toDate != null && fromDate.isAfter(toDate);
        LocalDateTime fromDateTime = fromDate == null ? null : fromDate.atStartOfDay();
        LocalDateTime toDateExclusive = toDate == null ? null : toDate.plusDays(1).atStartOfDay();

        List<Transaction> transactions = invalidDateRange
                ? List.of()
                : accountService.searchTransactionHistory(account, searchTerm, transactionType,
                        fromDateTime, toDateExclusive);
        model.addAttribute("transactions", transactions);
        model.addAttribute("searchTerm", q == null ? "" : q);
        model.addAttribute("selectedType", transactionType);
        model.addAttribute("fromDate", fromDate == null ? "" : fromDate.toString());
        model.addAttribute("toDate", toDate == null ? "" : toDate.toString());
        if (invalidDateRange) {
            model.addAttribute("error", "Start date must be on or before end date.");
        }
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
            addDashboardAttributes(model, fromAccount);
            model.addAttribute("error", e.getMessage());
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
            addDashboardAttributes(model, account);
            model.addAttribute("error", e.getMessage());
            return "dashboard";
        }
        return "redirect:/dashboard";
    }

    @GetMapping("/credentials")
    public String credentials() {
        return "credentials";
    }

    @PostMapping("/credentials")
    public String changePassword(@RequestParam String currentPassword, @RequestParam String password,
                                 @RequestParam String confirmPassword, Model model) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        try {
            accountService.changePassword(username, currentPassword, password, confirmPassword);
            return "redirect:/dashboard";
        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage());
            return "credentials";
        }
    }

    private void addDashboardAttributes(Model model, Account account) {
        model.addAttribute("account", account);
        model.addAttribute("checkingBalance", accountService.getBalanceForType(account, AccountType.CHECKING));
        model.addAttribute("savingsBalance", accountService.getBalanceForType(account, AccountType.SAVINGS));
    }

}
