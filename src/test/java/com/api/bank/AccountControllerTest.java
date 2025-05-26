package com.api.bank;

import com.api.bank.dtos.*;
import com.api.bank.entities.Account;
import com.api.bank.entities.user.User;
import com.api.bank.enums.UserRole;
import com.api.bank.repositories.AccountRepository;
import com.api.bank.repositories.TransactionRepository;
import com.api.bank.repositories.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private UserRepository userRepository;

    private Account account;

    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();
        accountRepository.deleteAll();
        userRepository.deleteAll();

        User user = new User();
        user.setEmail("userteste@example.com");
        user.setPassword(new BCryptPasswordEncoder().encode("userpass"));
        user.setRole(UserRole.USER);
        userRepository.save(user);

        account = Account.builder()
                .number("123456")
                .balance(BigDecimal.valueOf(1000.0))
                .user(user)
                .build();
        accountRepository.save(account);
    }

    private String registerAndLogin(String email, String password, String role) throws Exception {

        var registerDto = new RegisterDTO(email, password, Enum.valueOf(UserRole.class, role));
        mockMvc.perform(post("/auth/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(registerDto)))
                .andExpect(status().isOk());


        var authDto = new AuthenticationDTO(email, password);
        var result = mockMvc.perform(post("/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(authDto)))
                .andExpect(status().isOk())
                .andReturn();

        var responseJson = result.getResponse().getContentAsString();
        var loginResponse = objectMapper.readValue(responseJson, LoginResponseDTO.class);
        return loginResponse.token();
    }

    private String login(String email, String password) throws Exception {
        var authDto = new AuthenticationDTO(email, password);
        var result = mockMvc.perform(post("/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(authDto)))
                .andExpect(status().isOk())
                .andReturn();

        var responseJson = result.getResponse().getContentAsString();
        var loginResponse = objectMapper.readValue(responseJson, LoginResponseDTO.class);
        return loginResponse.token();
    }


    @Test
    @DisplayName("GET /accounts/ should return all accounts when user admin")
    void shouldAllowAdminGetAccounts() throws Exception {
        String adminToken = registerAndLogin("admin@example.com", "adminpass", "ADMIN");

        mockMvc.perform(get("/accounts/")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].number", is(account.getNumber())))
                .andExpect(jsonPath("$[0].balance", is(account.getBalance().doubleValue())));
    }

    @Test
    @DisplayName("GET /accounts/ should return 403 when user not admin role")
    void shouldDenyUserGetAccounts() throws Exception {

        String userToken = registerAndLogin("user@example.com", "userpass", "USER");

        mockMvc.perform(get("/accounts/")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /accounts/ should return 403 when anonymous")
    void shouldDenyAnonymousGetAccounts() throws Exception {
        mockMvc.perform(get("/accounts/"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /accounts/{id} should return account by id")
    void shouldAllowAdminGetAccountById() throws Exception {
        String adminToken = registerAndLogin("admin@example.com", "adminpass", "ADMIN");

        mockMvc.perform(get("/accounts/{id}", account.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.number", is(account.getNumber())))
                .andExpect(jsonPath("$.balance", is(account.getBalance().doubleValue())));
    }

    @Test
    @DisplayName("GET /accounts/{id} should return account by id")
    void shouldAllowOwnerGetAccountById() throws Exception {
        User user = (User) userRepository.findByEmail("userteste@example.com");
        String adminToken = login(user.getEmail(), "userpass");

        mockMvc.perform(get("/accounts/{id}", account.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.number", is(account.getNumber())))
                .andExpect(jsonPath("$.balance", is(account.getBalance().doubleValue())));
    }

    @Test
    @DisplayName("GET /accounts/{id} should return 400 when not owner account")
    void shouldDenyUserNotOwnerGetAccountById() throws Exception {
        String adminToken = registerAndLogin("admin@example.com", "adminpass", "USER");

        mockMvc.perform(get("/accounts/{id}", account.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("You do not have permission to access this account.")));
    }

    @Test
    @DisplayName("GET /accounts/{id} should return 403 when anonymous")
    void shouldDenyAnonymousGetAccountById() throws Exception {
        mockMvc.perform(get("/accounts/{id}", account.getId()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /accounts/{id} should return 404 when account not found")
    void shouldReturn404WhenAccountNotFound() throws Exception {
        String adminToken = registerAndLogin("admin@example.com", "adminpass", "ADMIN");

        String invalidId = UUID.randomUUID().toString();

        mockMvc.perform(get("/accounts/{id}", invalidId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("Account not found.")));
    }

    @Test
    @DisplayName("POST /accounts/ should create a new account")
    void shouldCreateAccount() throws Exception {
        String adminToken = registerAndLogin("admin@example.com", "adminpass", "ADMIN");

        AccountPostDTO dto = new AccountPostDTO("654321", BigDecimal.valueOf(5000.0));

        mockMvc.perform(post("/accounts/")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.number", is(dto.getNumber())))
                .andExpect(jsonPath("$.user.email", is("admin@example.com")))
                .andExpect(jsonPath("$.balance", is(dto.getBalance().doubleValue())));
    }

    @Test
    @DisplayName("POST /accounts/ should return error when account number is null")
    void shouldReturnErrorWhenAccountNumberIsNull() throws Exception {
        String adminToken = registerAndLogin("admin@example.com", "adminpass", "ADMIN");

        AccountPostDTO dto = new AccountPostDTO(null, BigDecimal.valueOf(1000));

        mockMvc.perform(post("/accounts/")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors", hasItem("Account number is required.")));
    }

    @Test
    @DisplayName("POST /accounts/ should return error when account number has invalid length")
    void shouldReturnErrorWhenAccountNumberInvalidLength() throws Exception {
        String adminToken = registerAndLogin("admin@example.com", "adminpass", "ADMIN");

        AccountPostDTO dto = new AccountPostDTO("12345", BigDecimal.valueOf(1000));

        mockMvc.perform(post("/accounts/")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors", hasItem("Account number must have exactly 6 characters.")));
    }

    @Test
    @DisplayName("POST /accounts/ should return error when balance is null")
    void shouldReturnErrorWhenBalanceIsNull() throws Exception {
        String adminToken = registerAndLogin("admin@example.com", "adminpass", "ADMIN");

        AccountPostDTO dto = new AccountPostDTO("123456", null);

        mockMvc.perform(post("/accounts/")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors", hasItem("Account balance is required.")));
    }

    @Test
    @DisplayName("POST /accounts/ should return error when balance is negative")
    void shouldReturnErrorWhenBalanceIsNegative() throws Exception {
        String adminToken = registerAndLogin("admin@example.com", "adminpass", "ADMIN");

        AccountPostDTO dto = new AccountPostDTO("123456", BigDecimal.valueOf(-1));

        mockMvc.perform(post("/accounts/")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors", hasItem("The account balance cannot be negative.")));
    }

    @Test
    @DisplayName("PUT /accounts/{id} should admin update an account")
    void shouldAllowAdminUpdateAccount() throws Exception {
        String adminToken = registerAndLogin("admin@example.com", "adminpass", "ADMIN");

        AccountUpdateDTO dto = new AccountUpdateDTO("654321", BigDecimal.valueOf(2000.0));

        mockMvc.perform(put("/accounts/{id}", account.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.number", is(dto.getNumber())))
                .andExpect(jsonPath("$.balance", is(dto.getBalance().doubleValue())));
    }

    @Test
    @DisplayName("PUT /accounts/{id} should user owner update an account")
    void shouldOwnerUserUpdateAccount() throws Exception {
        User user = (User) userRepository.findByEmail("userteste@example.com");
        String adminToken = login(user.getEmail(), "userpass");

        AccountUpdateDTO dto = new AccountUpdateDTO("654321", BigDecimal.valueOf(2000.0));

        mockMvc.perform(put("/accounts/{id}", account.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.number", is(dto.getNumber())))
                .andExpect(jsonPath("$.balance", is(dto.getBalance().doubleValue())));
    }

    @Test
    @DisplayName("PUT /accounts/{id} should return 400 when not owner update an account")
    void shouldDenyNotOwnerUpdateAccount() throws Exception {
        String adminToken = registerAndLogin("admin@example.com", "adminpass", "USER");

        AccountUpdateDTO dto = new AccountUpdateDTO("654321", BigDecimal.valueOf(2000.0));

        mockMvc.perform(put("/accounts/{id}", account.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("You do not have permission to access this account.")));
    }

    @Test
    @DisplayName("PUT /accounts/{id} should update only account number with invalid length")
    void shouldReturnErrorWhenUpdateNumberInvalidLength() throws Exception {
        String adminToken = registerAndLogin("admin@example.com", "adminpass", "ADMIN");

        AccountUpdateDTO dto = new AccountUpdateDTO("12345", null);

        mockMvc.perform(put("/accounts/{id}", account.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors", hasItem("Account number must have exactly 6 characters.")));
    }

    @Test
    @DisplayName("PUT /accounts/{id} should update only balance with negative value")
    void shouldReturnErrorWhenUpdateBalanceNegative() throws Exception {
        String adminToken = registerAndLogin("admin@example.com", "adminpass", "ADMIN");

        AccountUpdateDTO dto = new AccountUpdateDTO(null, BigDecimal.valueOf(-10));

        mockMvc.perform(put("/accounts/{id}", account.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors", hasItem("The account balance cannot be negative.")));
    }

    @Test
    @DisplayName("PUT /accounts/{id} should return 404 when account id not found")
    void shouldReturnNotFoundWhenAccountIdNotFound() throws Exception {
        String adminToken = registerAndLogin("admin@example.com", "adminpass", "ADMIN");

        AccountUpdateDTO dto = new AccountUpdateDTO("654321", BigDecimal.valueOf(2000.0));

        UUID invalidId = UUID.randomUUID();

        mockMvc.perform(put("/accounts/{id}", invalidId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("Account not found.")));
    }

    @Test
    @DisplayName("PUT /accounts/{id} should update only the account number")
    void shouldUpdateOnlyNumber() throws Exception {
        String adminToken = registerAndLogin("admin@example.com", "adminpass", "ADMIN");

        AccountUpdateDTO dto = new AccountUpdateDTO("654321", null);

        mockMvc.perform(put("/accounts/{id}", account.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.number", is(dto.getNumber())))
                .andExpect(jsonPath("$.balance", is(account.getBalance().doubleValue())));
    }

    @Test
    @DisplayName("PUT /accounts/{id} should update only the account balance")
    void shouldUpdateOnlyBalance() throws Exception {
        String adminToken = registerAndLogin("admin@example.com", "adminpass", "ADMIN");

        AccountUpdateDTO dto = new AccountUpdateDTO(null, BigDecimal.valueOf(3000.0));

        mockMvc.perform(put("/accounts/{id}", account.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.number", is(account.getNumber())))
                .andExpect(jsonPath("$.balance", is(dto.getBalance().doubleValue())));
    }

    @Test
    @DisplayName("PUT /accounts/{id} should return 403 when anonymous update the account")
    void shouldDenyAnonymousUpdateAccount() throws Exception {
        mockMvc.perform(put("/accounts/{id}", account.getId()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("DELETE /accounts/{id} should admin delete the account")
    void shouldAllowAdminDeleteAccount() throws Exception {
        String adminToken = registerAndLogin("admin@example.com", "adminpass", "ADMIN");

        mockMvc.perform(delete("/accounts/{id}", account.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /accounts/{id} should owner user delete the account")
    void shouldOwnerUserDeleteAccount() throws Exception {
        User user = (User) userRepository.findByEmail("userteste@example.com");
        String adminToken = login(user.getEmail(), "userpass");

        mockMvc.perform(delete("/accounts/{id}", account.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /accounts/{id} should return 400 when not owner delete the account")
    void shouldDenyNotOwnerDeleteAccount() throws Exception {
        String adminToken = registerAndLogin("admin@example.com", "adminpass", "USER");

        mockMvc.perform(delete("/accounts/{id}", account.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("You do not have permission to access this account.")));
    }

    @Test
    @DisplayName("DELETE /accounts/{id} should return 403 when anonymous delete the account")
    void shouldDenyAnonymousDeleteAccount() throws Exception {
        mockMvc.perform(delete("/accounts/{id}", account.getId()))
                .andExpect(status().isForbidden());
    }
}

