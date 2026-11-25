package com.ruralmarketplace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com.ruralmarketplace.entity.Category;
import com.ruralmarketplace.repository.CategoryRepository;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.datasource.url=jdbc:h2:mem:velnora-auth-flow;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class AuthenticationFlowIntegrationTest {

    private static final String ADMIN_EMAIL = "auth-test-" + UUID.randomUUID() + "@example.invalid";
    private static final String ADMIN_PASSWORD = "A" + UUID.randomUUID() + "!z9";
    private static final String TEST_JWT_SECRET = createJwtSecret();

    @DynamicPropertySource
    static void configureTestProperties(DynamicPropertyRegistry registry) {
        registry.add("app.jwt.secret", () -> TEST_JWT_SECRET);
        registry.add("app.admin.setup.name", () -> "Auth Test Admin");
        registry.add("app.admin.setup.email", () -> ADMIN_EMAIL);
        registry.add("app.admin.setup.password", () -> ADMIN_PASSWORD);
    }

    @LocalServerPort private int port;
    @Autowired private CategoryRepository categoryRepository;
    private final HttpClient http = HttpClient.newHttpClient();

    @Test
    void customerShopkeeperAdminAndShopApplicationUseProtectedBackendFlows() throws Exception {
        String suffix = UUID.randomUUID().toString();
        String customerEmail = "customer-" + suffix + "@example.invalid";
        String shopkeeperEmail = "seller-" + suffix + "@example.invalid";
        String customerPassword = "Customer-" + suffix + "!";
        String shopkeeperPassword = "Shopkeeper-" + suffix + "!";

        HttpResponse<String> customerRegistration = json("POST", "/api/auth/register",
                registration("Auth Flow Customer", customerEmail, customerPassword, "CUSTOMER"), null);
        assertEquals(201, customerRegistration.statusCode());
        assertTrue(customerRegistration.body().contains("\"role\":\"CUSTOMER\""));
        String customerToken = login(customerEmail, customerPassword, "CUSTOMER");
        assertEquals(200, request("GET", "/api/profile", "", customerToken, "application/json").statusCode());
        assertEquals(403, request("GET", "/api/admin/dashboard", "", customerToken, "application/json").statusCode());

        HttpResponse<String> adminRegistration = json("POST", "/api/auth/register",
                registration("Not an Admin", "blocked-" + suffix + "@example.invalid", customerPassword, "ADMIN"), null);
        assertEquals(400, adminRegistration.statusCode());

        HttpResponse<String> shopkeeperRegistration = json("POST", "/api/auth/register",
                registration("Auth Flow Shopkeeper", shopkeeperEmail, shopkeeperPassword, "SHOPKEEPER"), null);
        assertEquals(201, shopkeeperRegistration.statusCode());
        assertTrue(shopkeeperRegistration.body().contains("\"role\":\"SHOPKEEPER\""));
        String shopkeeperToken = login(shopkeeperEmail, shopkeeperPassword, "SHOPKEEPER");
        assertEquals(403, request("GET", "/api/admin/dashboard", "", shopkeeperToken, "application/json").statusCode());

        Category category = new Category();
        category.setName("Auth test " + suffix.substring(0, 8));
        category = categoryRepository.save(category);
        String boundary = "VelnoraAuth" + UUID.randomUUID().toString().replace("-", "");
        String shopPart = "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"shop\"; filename=\"shop.json\"\r\n"
                + "Content-Type: application/json\r\n\r\n"
                + "{\"name\":\"Authentication Test Shop\",\"description\":\"Flow test\","
                + "\"address\":\"Test Street\",\"categoryId\":" + category.getId() + ","
                + "\"contactPhone\":\"9876543210\",\"contactEmail\":\"" + shopkeeperEmail + "\"}\r\n"
                + "--" + boundary + "--\r\n";
        HttpResponse<String> application = request("POST", "/api/shops", shopPart, shopkeeperToken,
                "multipart/form-data; boundary=" + boundary);
        assertEquals(200, application.statusCode(), application.body());
        assertTrue(application.body().contains("\"approvalStatus\":\"PENDING\""));
        Matcher shopId = Pattern.compile("\"shop\":\\{\\\"id\\\":(\\d+)").matcher(application.body());
        assertTrue(shopId.find(), "Shop application response must include its shop id");

        String adminToken = login(ADMIN_EMAIL, ADMIN_PASSWORD, "ADMIN");
        assertEquals(200, request("GET", "/api/admin/dashboard", "", adminToken, "application/json").statusCode());
        HttpResponse<String> approval = request("PATCH", "/api/admin/shops/" + shopId.group(1)
                + "/approval?status=APPROVED", "", adminToken, "application/json");
        assertEquals(200, approval.statusCode(), approval.body());
        HttpResponse<String> myShop = request("GET", "/api/shops/my-shop", "", shopkeeperToken, "application/json");
        assertEquals(200, myShop.statusCode(), myShop.body());
        assertTrue(myShop.body().contains("\"approvalStatus\":\"APPROVED\""));

        assertEquals(204, request("POST", "/api/account/logout", "", customerToken, "application/json").statusCode());
        assertEquals(401, request("GET", "/api/profile", "", customerToken, "application/json").statusCode());
    }

    private String login(String email, String password, String role) throws Exception {
        HttpResponse<String> response = json("POST", "/api/auth/login",
                "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}", null);
        assertEquals(200, response.statusCode(), response.body());
        assertTrue(response.body().contains("\"role\":\"" + role + "\""));
        Matcher token = Pattern.compile("\"accessToken\":\"([^\"]+)\"").matcher(response.body());
        assertTrue(token.find(), "Login response must contain an access token");
        return token.group(1);
    }

    private HttpResponse<String> json(String method, String path, String body, String token) throws Exception {
        return request(method, path, body, token, "application/json");
    }

    private HttpResponse<String> request(String method, String path, String body, String token, String contentType)
            throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Content-Type", contentType);
        if (token != null) builder.header("Authorization", "Bearer " + token);
        builder.method(method, body.isEmpty() ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(body));
        return http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private static String registration(String name, String email, String password, String role) {
        return "{\"fullName\":\"" + name + "\",\"email\":\"" + email
                + "\",\"phone\":\"\",\"password\":\"" + password + "\",\"role\":\"" + role + "\"}";
    }

    private static String createJwtSecret() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }
}
