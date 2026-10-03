package com.sdws.wallet.api;

import com.sdws.wallet.model.AppUser;
import com.sdws.wallet.service.WalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.concurrent.atomic.AtomicInteger;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:api-wallet-test;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "app.jwt.secret=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="
})
class WalletApiTest {

    private static final AtomicInteger NEXT_PHONE = new AtomicInteger(1);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WalletService walletService;

    private String ownerPhone;

    @BeforeEach
    void registerOwner() {
        ownerPhone = "078000" + String.format("%04d", NEXT_PHONE.getAndIncrement());
        walletService.register("Test Owner", ownerPhone, null, "UserPass123");
    }

    @Test
    void walletResponseContainsOwnerAccountAndBalance() throws Exception {
        mockMvc.perform(get("/api/wallet").with(user(ownerPhone).roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ownerName").value("Test Owner"))
                .andExpect(jsonPath("$.phone").value(ownerPhone))
                .andExpect(jsonPath("$.balance").value(0.0));
    }

    @Test
    void depositUpdatesBalanceAndTransactionHistory() throws Exception {
        mockMvc.perform(post("/api/wallet/deposits")
                        .with(user(ownerPhone).roles("USER"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":75.25}"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/wallet").with(user(ownerPhone).roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(75.25));

        mockMvc.perform(get("/api/transactions?type=ADD").with(user(ownerPhone).roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("ADD"))
                .andExpect(jsonPath("$[0].amount").value(75.25));
    }

    @Test
    void transferDebitsSenderAndRecordsTransfer() throws Exception {
        walletService.add(walletService.current(ownerPhone), new java.math.BigDecimal("100.00"));
        String recipientPhone = "079000" + String.format("%04d", NEXT_PHONE.getAndIncrement());
        walletService.register("Test Recipient", recipientPhone, null, "UserPass123");

        mockMvc.perform(post("/api/transfers")
                        .with(user(ownerPhone).roles("USER"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"" + recipientPhone + "\",\"amount\":30.00,\"note\":\"test\"}"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/wallet").with(user(ownerPhone).roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(70.0));
    }
}