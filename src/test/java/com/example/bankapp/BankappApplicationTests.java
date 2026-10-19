package com.example.bankapp;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.example.bankapp.model.AccountType;
import com.example.bankapp.service.AccountService;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class BankappApplicationTests {
	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private AccountService accountService;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Test
	void contextLoads() {
	}

	@Test
	void localDemoAccountsHaveBalancesAndTransferHistory() {
		var demo = accountService.findAccountByUsername("demo");
		var recipient = accountService.findAccountByUsername("alex");

		assertTrue(passwordEncoder.matches("DemoBank2026!", demo.getPassword()));
		assertEquals(0, new BigDecimal("5925.00").compareTo(demo.getBalance()));
		assertEquals(0, new BigDecimal("1725.00").compareTo(demo.getCheckingBalance()));
		assertEquals(0, new BigDecimal("4200.00").compareTo(demo.getSavingsBalance()));
		assertTrue(accountService.getTransactionHistory(demo).stream()
				.anyMatch(transaction -> transaction.getType().contains("Transfer Out")));
		assertTrue(accountService.getTransactionHistory(recipient).stream()
				.anyMatch(transaction -> transaction.getType().contains("Transfer In")));
	}

	@Test
	void registrationFormIncludesCsrfToken() throws Exception {
		mockMvc.perform(get("/register"))
				.andExpect(status().isOk())
				.andExpect(result -> assertTrue(result.getResponse().getContentAsString().contains("name=\"_csrf\"")));
	}

	@Test
	void loginPageShowsCenteredFormAndOriginalFooter() throws Exception {
		mockMvc.perform(get("/login"))
				.andExpect(status().isOk())
				.andExpect(result -> assertTrue(result.getResponse().getContentAsString().contains("class=\"login-card\"")))
				.andExpect(result -> assertTrue(result.getResponse().getContentAsString().contains("Access your accounts securely.")))
				.andExpect(result -> assertTrue(result.getResponse().getContentAsString().contains("Terms of Service")))
				.andExpect(result -> assertTrue(result.getResponse().getContentAsString().contains("LedgerBank, Inc.")))
				.andExpect(result -> assertTrue(!result.getResponse().getContentAsString().contains("Everyday banking")));
	}

	@Test
	@SuppressWarnings("null")
	void seededDemoAccountCanLogIn() throws Exception {
		mockMvc.perform(post("/login")
					.with(csrf())
					.param("username", "demo")
					.param("password", "DemoBank2026!"))
				.andExpect(status().is3xxRedirection())
				.andExpect(result -> assertEquals("/dashboard", result.getResponse().getRedirectedUrl()));
	}

	@Test
	void sharedStylesheetIsAvailableBeforeAuthentication() throws Exception {
		mockMvc.perform(get("/css/ledgerbank.css"))
				.andExpect(status().isOk())
				.andExpect(result -> assertTrue(result.getResponse().getContentAsString().contains("--ledger-blue")))
				.andExpect(result -> assertTrue(result.getResponse().getContentAsString().contains("/images/wallpaper.jpg")));

		mockMvc.perform(get("/css/dashboard.css"))
				.andExpect(status().isOk())
				.andExpect(result -> assertTrue(result.getResponse().getContentAsString().contains(".dashboard-container")));
	}

	@Test
	void passwordChangeRequiresAuthentication() throws Exception {
		mockMvc.perform(get("/credentials"))
				.andExpect(status().is3xxRedirection());

		mockMvc.perform(post("/credentials"))
				.andExpect(status().isForbidden());
	}

	@Test
	void passwordChangeRequiresCurrentPassword() {
		String username = "password-test-" + UUID.randomUUID();
		accountService.registerAccount(username, "old-password");

		assertThrows(IllegalArgumentException.class, () -> accountService.changePassword(
				username, "incorrect-password", "new-password", "new-password"));
		accountService.changePassword(username, "old-password", "new-password", "new-password");

		assertTrue(passwordEncoder.matches("new-password",
				accountService.findAccountByUsername(username).getPassword()));
	}

	@Test
	@SuppressWarnings("null")
	void failedWithdrawalRendersCurrentAccountBalances() throws Exception {
		String username = "withdrawal-test-" + UUID.randomUUID();
		accountService.registerAccount(username, "test-password");

		mockMvc.perform(post("/withdraw")
					.with(user(username).authorities(() -> "USER"))
					.with(csrf())
					.param("amount", "1.00")
					.param("accountType", "CHECKING"))
				.andExpect(status().isOk())
				.andExpect(result -> assertTrue(result.getResponse().getContentAsString().contains(">0.00</span>")))
				.andExpect(result -> assertTrue(result.getResponse().getContentAsString().contains("Insufficient funds")));
	}

	@Test
	@SuppressWarnings("null")
	void transactionHistoryDisplaysDepositsAsCredits() throws Exception {
		String username = "history-test-" + UUID.randomUUID();
		var account = accountService.registerAccount(username, "test-password");
		accountService.deposit(account, AccountType.CHECKING, new BigDecimal("10.00"));

		mockMvc.perform(get("/transactions").with(user(username).authorities(() -> "USER")))
				.andExpect(status().isOk())
				.andExpect(result -> assertTrue(result.getResponse().getContentAsString().contains("+$10.00")))
				.andExpect(result -> assertTrue(result.getResponse().getContentAsString().contains("amount-credit")));
	}

	@Test
	@SuppressWarnings("null")
	void transactionHistoryFiltersBySearchTypeAndInclusiveDateRange() throws Exception {
		String username = "filter-test-" + UUID.randomUUID();
		accountService.registerAccount(username, "test-password");
		accountService.deposit(accountService.findAccountByUsername(username), AccountType.CHECKING,
				new BigDecimal("20.00"));
		accountService.deposit(accountService.findAccountByUsername(username), AccountType.SAVINGS,
				new BigDecimal("30.00"));
		accountService.withdraw(accountService.findAccountByUsername(username), AccountType.CHECKING,
				new BigDecimal("5.00"));
		String today = LocalDate.now().toString();

		mockMvc.perform(get("/transactions")
					.with(user(username).authorities(() -> "USER"))
					.param("q", "Checking")
					.param("type", "DEPOSIT")
					.param("fromDate", today)
					.param("toDate", today))
				.andExpect(status().isOk())
				.andExpect(result -> assertTrue(result.getResponse().getContentAsString().contains("Deposit to Checking")))
				.andExpect(result -> assertTrue(result.getResponse().getContentAsString().contains("1 transaction")))
				.andExpect(result -> assertTrue(!result.getResponse().getContentAsString().contains("1 transactions")))
				.andExpect(result -> assertTrue(result.getResponse().getContentAsString().contains("name=\"q\"")))
				.andExpect(result -> assertTrue(result.getResponse().getContentAsString().contains("value=\"Checking\"")))
				.andExpect(result -> assertTrue(result.getResponse().getContentAsString().contains("name=\"fromDate\"")))
				.andExpect(result -> assertTrue(result.getResponse().getContentAsString().contains("value=\"" + today + "\"")))
				.andExpect(result -> assertTrue(!result.getResponse().getContentAsString().contains("Deposit to Savings")))
				.andExpect(result -> assertTrue(!result.getResponse().getContentAsString().contains("Withdrawal from Checking")));
	}

	@Test
	@SuppressWarnings("null")
	void transactionHistoryExplainsInvalidDateRange() throws Exception {
		String username = "date-filter-test-" + UUID.randomUUID();
		accountService.registerAccount(username, "test-password");

		mockMvc.perform(get("/transactions")
					.with(user(username).authorities(() -> "USER"))
					.param("fromDate", "2026-10-20")
					.param("toDate", "2026-10-19"))
				.andExpect(status().isOk())
				.andExpect(result -> assertTrue(result.getResponse().getContentAsString()
						.contains("Start date must be on or before end date.")));
	}

}
