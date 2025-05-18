package com.dws.challenge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

import java.math.BigDecimal;

import com.dws.challenge.domain.Account;
import com.dws.challenge.domain.TransferRequest;
import com.dws.challenge.service.AccountsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@WebAppConfiguration
class AccountsControllerTest {

  private MockMvc mockMvc;

  @Autowired
  private AccountsService accountsService;

  @Autowired
  private WebApplicationContext webApplicationContext;

  private final ObjectMapper objectMapper = new ObjectMapper();

  @BeforeEach
  void prepareMockMvc() {
    this.mockMvc = webAppContextSetup(this.webApplicationContext).build();

    // Reset the existing accounts before each test.
    accountsService.getAccountsRepository().clearAccounts();
  }

  @Test
  void createAccount() throws Exception {
    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
      .content("{\"accountId\":\"Id-123\",\"balance\":1000}")).andExpect(status().isCreated());

    Account account = accountsService.getAccount("Id-123");
    assertThat(account.getAccountId()).isEqualTo("Id-123");
    assertThat(account.getBalance()).isEqualByComparingTo("1000");
  }

  @Test
  void createDuplicateAccount() throws Exception {
    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
      .content("{\"accountId\":\"Id-123\",\"balance\":1000}")).andExpect(status().isCreated());

    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
      .content("{\"accountId\":\"Id-123\",\"balance\":1000}")).andExpect(status().isBadRequest());
  }

  @Test
  void createAccountNoAccountId() throws Exception {
    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
      .content("{\"balance\":1000}")).andExpect(status().isBadRequest());
  }

  @Test
  void createAccountNoBalance() throws Exception {
    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
      .content("{\"accountId\":\"Id-123\"}")).andExpect(status().isBadRequest());
  }

  @Test
  void createAccountNegativeBalance() throws Exception {
    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
      .content("{\"accountId\":\"Id-123\",\"balance\":-1000}")).andExpect(status().isBadRequest());
  }

  @Test
  void createAccountEmptyAccountId() throws Exception {
    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
      .content("{\"accountId\":\"\",\"balance\":1000}")).andExpect(status().isBadRequest());
  }

  @Test
  void getAccount() throws Exception {
    String uniqueAccountId = "Id-" + System.currentTimeMillis();
    Account account = new Account(uniqueAccountId, new BigDecimal("123.45"));
    this.accountsService.createAccount(account);
    this.mockMvc.perform(get("/v1/accounts/" + uniqueAccountId))
      .andExpect(status().isOk())
      .andExpect(
        content().string("{\"accountId\":\"" + uniqueAccountId + "\",\"balance\":123.45}"));
  }

  @Test
  void transferSuccess() throws Exception {
    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
            .content("{\"accountId\":\"Id-From\",\"balance\":1000}")).andExpect(status().isCreated());

    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
            .content("{\"accountId\":\"Id-To\",\"balance\":500}")).andExpect(status().isCreated());

    TransferRequest transferRequest = new TransferRequest();
    transferRequest.setAccountFromId("Id-From");
    transferRequest.setAccountToId("Id-To");
    transferRequest.setAmount(new BigDecimal("300"));

    this.mockMvc.perform(post("/v1/accounts/transfer")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(transferRequest)))
            .andExpect(status().isOk());

    this.mockMvc.perform(get("/v1/accounts/Id-From"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.balance").value(700));

    this.mockMvc.perform(get("/v1/accounts/Id-To"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.balance").value(800));
  }

  @Test
  void transferFailsOnNegativeAmount() throws Exception {
    TransferRequest transferRequest = new TransferRequest();
    transferRequest.setAccountFromId("Id-From");
    transferRequest.setAccountToId("Id-To");
    transferRequest.setAmount(new BigDecimal("-100"));

    this.mockMvc.perform(post("/v1/accounts/transfer")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(transferRequest)))
            .andExpect(status().isBadRequest());
  }

  @Test
  void transferFailsOnZeroAmount() throws Exception {
    TransferRequest transferRequest = new TransferRequest();
    transferRequest.setAccountFromId("Id-From");
    transferRequest.setAccountToId("Id-To");
    transferRequest.setAmount(BigDecimal.ZERO);

    this.mockMvc.perform(post("/v1/accounts/transfer")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(transferRequest)))
            .andExpect(status().isBadRequest());
  }

  @Test
  void transferFailsOnSameAccount() throws Exception {
    TransferRequest transferRequest = new TransferRequest();
    transferRequest.setAccountFromId("Id-From");
    transferRequest.setAccountToId("Id-From");
    transferRequest.setAmount(new BigDecimal("100"));

    this.mockMvc.perform(post("/v1/accounts/transfer")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(transferRequest)))
            .andExpect(status().isBadRequest());
  }

  @Test
  void transferFailsOnInsufficientFunds() throws Exception {
    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
            .content("{\"accountId\":\"Id-From\",\"balance\":50}")).andExpect(status().isCreated());

    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
            .content("{\"accountId\":\"Id-To\",\"balance\":100}")).andExpect(status().isCreated());

    TransferRequest transferRequest = new TransferRequest();
    transferRequest.setAccountFromId("Id-From");
    transferRequest.setAccountToId("Id-To");
    transferRequest.setAmount(new BigDecimal("100"));

    this.mockMvc.perform(post("/v1/accounts/transfer")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(transferRequest)))
            .andExpect(status().isBadRequest());
  }

  @Test
  void transferFailsOnNonExistingSourceAccount() throws Exception {
    TransferRequest transferRequest = new TransferRequest();
    transferRequest.setAccountFromId("NonExisting");
    transferRequest.setAccountToId("Id-To");
    transferRequest.setAmount(new BigDecimal("100"));

    this.mockMvc.perform(post("/v1/accounts/transfer")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(transferRequest)))
            .andExpect(status().isBadRequest());
  }

  @Test
  void transferFailsOnNonExistingDestinationAccount() throws Exception {
    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
            .content("{\"accountId\":\"Id-From\",\"balance\":1000}")).andExpect(status().isCreated());

    TransferRequest transferRequest = new TransferRequest();
    transferRequest.setAccountFromId("Id-From");
    transferRequest.setAccountToId("NonExisting");
    transferRequest.setAmount(new BigDecimal("100"));

    this.mockMvc.perform(post("/v1/accounts/transfer")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(transferRequest)))
            .andExpect(status().isBadRequest());
  }
}
