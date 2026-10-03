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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:api-admin-test;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "app.jwt.secret=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
        "app.admin.phone=0700000000",
        "app.admin.password=Admin@12345"
})
class AdminApiTest {

    private static final AtomicInteger NEXT_PHONE = new AtomicInteger(1);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WalletService walletService;

    private AppUser user;

    @BeforeEach
    void createUser() {
        String phone = "077000" + String.format("%04d", NEXT_PHONE.getAndIncrement());
        walletService.register("Listed User", phone, "listed@example.test", "UserPass123");
        user = walletService.current(phone);
    }

    @Test
    void adminOverviewReturnsSummary() throws Exception {
        mockMvc.perform(get("/api/admin/overview").with(user("0700000000").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userCount").value(2))
                .andExpect(jsonPath("$.activeCount").value(2))
                .andExpect(jsonPath("$.latestTransactions").isArray());
    }

    @Test
    void adminUserListNeverIncludesPasswordHash() throws Exception {
        mockMvc.perform(get("/api/admin/users").with(user("0700000000").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.phone == '" + user.getPhone() + "')].passwordHash").doesNotExist())
                .andExpect(jsonPath("$[?(@.phone == '" + user.getPhone() + "')].fullName").value("Listed User"));
    }

    @Test
    void adminCanDeactivateUser() throws Exception {
        mockMvc.perform(patch("/api/admin/users/" + user.getId())
                        .with(user("0700000000").roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\":false}"))
                .andExpect(status().isNoContent());

        org.junit.jupiter.api.Assertions.assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> walletService.current(user.getPhone()));
    }
}