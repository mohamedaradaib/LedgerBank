package com.example.bankapp;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import com.example.bankapp.service.AccountService;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
	void registrationFormIncludesCsrfToken() throws Exception {
		mockMvc.perform(get("/register"))
				.andExpect(status().isOk())
				.andExpect(result -> assertTrue(result.getResponse().getContentAsString().contains("name=\"_csrf\"")));
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

}
