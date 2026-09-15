package ru.itmo.secureapi;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import ru.itmo.secureapi.data.DataItemRepository;
import ru.itmo.secureapi.security.PasswordService;
import ru.itmo.secureapi.user.UserAccount;
import ru.itmo.secureapi.user.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecureApiIntegrationTest {

    private static final String USERNAME = "student";
    private static final String PASSWORD = "correct horse battery staple";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository users;

    @Autowired
    private DataItemRepository items;

    @Autowired
    private PasswordService passwords;

    @BeforeEach
    void setUp() {
        items.deleteAll();
        users.deleteAll();
        users.save(new UserAccount(USERNAME, passwords.hash(PASSWORD)));
    }

    @Test
    void successfulLoginReturnsBearerTokenAndNeverReturnsPassword() throws Exception {
        mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(USERNAME, PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", startsWith("eyJ")))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(content().string(not(containsString(PASSWORD))))
                .andExpect(content().string(not(containsString("passwordHash"))));
    }

    @Test
    void wrongPasswordAndSqliPayloadAreRejectedWithSameGenericMessage() throws Exception {
        mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(USERNAME, "wrong-password")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid username or password"));

        mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson("student' OR '1'='1", "anything")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid username or password"));
    }

    @Test
    void protectedEndpointRejectsMissingAndInvalidTokens() throws Exception {
        mvc.perform(get("/api/data"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Bearer token is required"));

        mvc.perform(get("/api/data")
                        .header("Authorization", "Bearer definitely-not-a-jwt"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid or expired bearer token"));
    }

    @Test
    void authenticatedUserCanCreateAndReadData() throws Exception {
        String token = loginAndGetToken();

        mvc.perform(post("/api/data")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"First item","content":"Visible only with a token"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("First item"))
                .andExpect(jsonPath("$.owner").value(USERNAME));

        mvc.perform(get("/api/data")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].content").value("Visible only with a token"));
    }

    @Test
    void htmlAndScriptMarkupIsRemovedBeforeStorageAndResponse() throws Exception {
        String token = loginAndGetToken();

        mvc.perform(post("/api/data")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title":"<b>Safe title</b>",
                                  "content":"Hello <img src=x onerror=alert(1)><script>alert(2)</script>world"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Safe title"))
                .andExpect(content().string(not(containsString("<script"))))
                .andExpect(content().string(not(containsString("onerror"))))
                .andExpect(content().string(not(containsString("<img"))));
    }

    @Test
    void invalidInputIsRejectedBeforeDatabaseAccess() throws Exception {
        String token = loginAndGetToken();

        mvc.perform(post("/api/data")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"\",\"content\":\"text\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Request validation failed"));
    }

    @Test
    void storedPasswordIsABcryptHash() {
        String stored = users.findByUsername(USERNAME).orElseThrow().getPasswordHash();
        org.assertj.core.api.Assertions.assertThat(stored)
                .startsWith("$2")
                .isNotEqualTo(PASSWORD);
        org.assertj.core.api.Assertions.assertThat(passwords.matches(PASSWORD, stored)).isTrue();
    }

    private String loginAndGetToken() throws Exception {
        String response = mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(USERNAME, PASSWORD)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode json = objectMapper.readTree(response);
        return json.get("token").asText();
    }

    private String loginJson(String username, String password) throws Exception {
        return objectMapper.writeValueAsString(new LoginInput(username, password));
    }

    private record LoginInput(String username, String password) {
    }
}
