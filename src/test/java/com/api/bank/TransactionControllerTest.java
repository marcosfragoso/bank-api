package com.api.bank;

import com.api.bank.dtos.AuthenticationDTO;
import com.api.bank.dtos.LoginResponseDTO;
import com.api.bank.dtos.RegisterDTO;
import com.api.bank.dtos.TransactionPostDTO;
import com.api.bank.entities.Account;
import com.api.bank.entities.Transaction;
import com.api.bank.entities.user.User;
import com.api.bank.enums.TransactionStatus;
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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private Transaction transaction;

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

        User user2 = new User();
        user2.setEmail("userteste2@example.com");
        user2.setPassword(new BCryptPasswordEncoder().encode("userpass"));
        user2.setRole(UserRole.USER);
        userRepository.save(user2);

        Account fromAccount = Account.builder()
                .number("123456")
                .balance(BigDecimal.valueOf(2000))
                .user(user)
                .build();

        Account toAccount = Account.builder()
                .number("654321")
                .balance(BigDecimal.valueOf(500))
                .user(user2)
                .build();

        accountRepository.save(fromAccount);
        accountRepository.save(toAccount);

        transaction = Transaction.builder()
                .fromAccount(fromAccount)
                .toAccount(toAccount)
                .amount(BigDecimal.valueOf(250.5))
                .status(TransactionStatus.COMPLETED)
                .build();

        transactionRepository.save(transaction);
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
    @DisplayName("GET /transactions should return all transactions")
    void shouldAllowAdminGetAllTransactions() throws Exception {
        String adminToken = registerAndLogin("admin@example.com", "adminpass", "ADMIN");

        mockMvc.perform(get("/transactions/")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", notNullValue()))
                .andExpect(jsonPath("$[0].fromAccount.number", is(transaction.getFromAccount().getNumber())))
                .andExpect(jsonPath("$[0].toAccount.number", is(transaction.getToAccount().getNumber())))
                .andExpect(jsonPath("$[0].amount", is(transaction.getAmount().doubleValue())))
                .andExpect(jsonPath("$[0].status", is(transaction.getStatus().name())));
    }

    @Test
    @DisplayName("GET /transactions should return 403 when not admin")
    void shouldDenyUserGetAllTransactions() throws Exception {
        String adminToken = registerAndLogin("admin@example.com", "adminpass", "USER");

        mockMvc.perform(get("/transactions/")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /transactions should return 403 when not admin")
    void shouldDenyAnonymousAllTransactions() throws Exception {
        mockMvc.perform(get("/transactions/")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /transactions should create a new transaction")
    void shouldDenyAnonymousCreateTransaction() throws Exception {
        TransactionPostDTO transactionPostDTO = TransactionPostDTO.builder()
                .fromAccount("123456")
                .toAccount("654321")
                .amount(BigDecimal.valueOf(1000.0))
                .passwordUser("123456")
                .build();

        mockMvc.perform(post("/transactions/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transactionPostDTO)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /transactions should return 400 when password invalid")
    void shouldNotCreateTransactionWithPasswordInvalid() throws Exception {
        User user = (User) userRepository.findByEmail("userteste@example.com");
        String adminToken = login(user.getEmail(), "userpass");

        TransactionPostDTO transactionPostDTO = TransactionPostDTO.builder()
                .fromAccount("123456")
                .toAccount("654321")
                .passwordUser("qualquer")
                .amount(BigDecimal.valueOf(1000.0))
                .build();

        mockMvc.perform(post("/transactions/")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transactionPostDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Invalid account password.")));
    }

    @Test
    @DisplayName("POST /transactions should return 400 when not owner")
    void shouldNotCreateTransactionWithNotOwner() throws Exception {
        String adminToken = registerAndLogin("admin@example.com", "adminpass", "USER");

        TransactionPostDTO transactionPostDTO = TransactionPostDTO.builder()
                .fromAccount("123456")
                .toAccount("654321")
                .passwordUser("userpass")
                .amount(BigDecimal.valueOf(1000.0))
                .build();

        mockMvc.perform(post("/transactions/")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transactionPostDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("You do not have permission to perform this transaction.")));
    }

    @Test
    @DisplayName("POST /transactions should create a new transaction")
    void shouldAllowOwnerCreateTransaction() throws Exception {
        User user = (User) userRepository.findByEmail("userteste@example.com");
        String adminToken = login(user.getEmail(), "userpass");

        TransactionPostDTO transactionPostDTO = TransactionPostDTO.builder()
                .fromAccount("123456")
                .toAccount("654321")
                .passwordUser("userpass")
                .amount(BigDecimal.valueOf(1000.0))
                .build();

        mockMvc.perform(post("/transactions/")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transactionPostDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.fromAccount.number", is(transactionPostDTO.getFromAccount())))
                .andExpect(jsonPath("$.toAccount.number", is(transactionPostDTO.getToAccount())))
                .andExpect(jsonPath("$.amount", is(transactionPostDTO.getAmount().doubleValue())))
                .andExpect(jsonPath("$.status", is(TransactionStatus.COMPLETED.name())));

        Account updatedFromAccount = accountRepository.findByNumber("123456").orElseThrow();
        Account updatedToAccount = accountRepository.findByNumber("654321").orElseThrow();

        BigDecimal expectedFromBalance = BigDecimal.valueOf(2000.0).subtract(BigDecimal.valueOf(1000.0));
        BigDecimal expectedToBalance = BigDecimal.valueOf(500.0).add(BigDecimal.valueOf(1000.0));

        assertEquals(0, updatedFromAccount.getBalance().compareTo(expectedFromBalance));
        assertEquals(0, updatedToAccount.getBalance().compareTo(expectedToBalance));
    }

    @Test
    @DisplayName("POST /transactions should create a new transaction")
    void shouldAllowAdminCreateTransaction() throws Exception {
        String adminToken = registerAndLogin("admin@example.com", "adminpass", "ADMIN");

        TransactionPostDTO transactionPostDTO = TransactionPostDTO.builder()
                .fromAccount("123456")
                .toAccount("654321")
                .passwordUser("asdasdad")
                .amount(BigDecimal.valueOf(1000.0))
                .build();

        mockMvc.perform(post("/transactions/")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transactionPostDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.fromAccount.number", is(transactionPostDTO.getFromAccount())))
                .andExpect(jsonPath("$.toAccount.number", is(transactionPostDTO.getToAccount())))
                .andExpect(jsonPath("$.amount", is(transactionPostDTO.getAmount().doubleValue())))
                .andExpect(jsonPath("$.status", is(TransactionStatus.COMPLETED.name())));

        Account updatedFromAccount = accountRepository.findByNumber("123456").orElseThrow();
        Account updatedToAccount = accountRepository.findByNumber("654321").orElseThrow();

        BigDecimal expectedFromBalance = BigDecimal.valueOf(2000.0).subtract(BigDecimal.valueOf(1000.0));
        BigDecimal expectedToBalance = BigDecimal.valueOf(500.0).add(BigDecimal.valueOf(1000.0));

        assertEquals(0, updatedFromAccount.getBalance().compareTo(expectedFromBalance));
        assertEquals(0, updatedToAccount.getBalance().compareTo(expectedToBalance));
    }

    @Test
    @DisplayName("POST /transactions should return 404 when from account does not exist")
    void shouldReturn404WhenFromAccountDoesNotExist() throws Exception {
        String adminToken = registerAndLogin("admin@example.com", "adminpass", "ADMIN");

        TransactionPostDTO dto = TransactionPostDTO.builder()
                .fromAccount("999999")
                .toAccount("654321")
                .passwordUser("adsdsadasd")
                .amount(BigDecimal.valueOf(100))
                .build();

        mockMvc.perform(post("/transactions/")
                        .header("Authorization", "Bearer " + adminToken)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("Account not found.")));
    }

    @Test
    @DisplayName("POST /transactions should return 404 when to account does not exist")
    void shouldReturn404WhenToAccountDoesNotExist() throws Exception {
        String adminToken = registerAndLogin("admin@example.com", "adminpass", "ADMIN");

        TransactionPostDTO dto = TransactionPostDTO.builder()
                .fromAccount("654321")
                .toAccount("999999")
                .passwordUser("adminpass")
                .amount(BigDecimal.valueOf(100))
                .build();

        mockMvc.perform(post("/transactions/")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("Account not found.")));
    }

    @Test
    @DisplayName("POST /transactions should return 400 when amount exceeds balance")
    void shouldReturn400WhenAmountExceedsBalance() throws Exception {
        String adminToken = registerAndLogin("admin@example.com", "adminpass", "ADMIN");

        TransactionPostDTO dto = TransactionPostDTO.builder()
                .fromAccount("123456")
                .toAccount("654321")
                .passwordUser("adminpass")
                .amount(BigDecimal.valueOf(5000))
                .build();

        mockMvc.perform(post("/transactions/")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Insufficient balance.")));
    }

    @Test
    @DisplayName("POST /transactions should return 400 when from and to accounts are the same")
    void shouldReturn400WhenFromAndToAccountsAreTheSame() throws Exception {
        String adminToken = registerAndLogin("admin@example.com", "adminpass", "ADMIN");

        TransactionPostDTO dto = TransactionPostDTO.builder()
                .fromAccount("123456")
                .toAccount("123456")
                .passwordUser("adminpass")
                .amount(BigDecimal.valueOf(100))
                .build();

        mockMvc.perform(post("/transactions/")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Transfer to the same account is not allowed.")));
    }

    @Test
    @DisplayName("GET /transactions/{id} should return transactions for existing account")
    void shouldReturnTransactionsByAccount() throws Exception {
        String adminToken = registerAndLogin("admin@example.com", "adminpass", "ADMIN");

        Account account = accountRepository.findByNumber("123456").orElseThrow();

        mockMvc.perform(get("/transactions/{id}", account.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].fromAccount.number", anyOf(is("123456"), is("654321"))))
                .andExpect(jsonPath("$[0].toAccount.number", anyOf(is("123456"), is("654321"))));
    }

    @Test
    @DisplayName("GET /transactions/{id} should return 404 when account not found")
    void shouldReturn404WhenAccountNotFound() throws Exception {
        String adminToken = registerAndLogin("admin@example.com", "adminpass", "ADMIN");

        UUID nonExistentId = UUID.randomUUID();

        mockMvc.perform(get("/transactions/{id}", nonExistentId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("Account not found.")));
    }

    @Test
    @DisplayName("GET /transactions/{id} should return 400 when not owner account")
    void shouldDenyWhenNoOwnerAccountNotFound() throws Exception {
        String adminToken = registerAndLogin("admin@example.com", "adminpass", "USER");

        Account account = accountRepository.findByNumber("123456").orElseThrow();

        mockMvc.perform(get("/transactions/{id}", account.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("You do not have permission to access this account.")));
    }

    @Test
    @DisplayName("GET /transactions/{id} should return 403 when anonymous get account")
    void shouldDenyAnonymousTransactionsByAccount() throws Exception {

        Account account = accountRepository.findByNumber("123456").orElseThrow();

        mockMvc.perform(get("/transactions/{id}", account.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

}
