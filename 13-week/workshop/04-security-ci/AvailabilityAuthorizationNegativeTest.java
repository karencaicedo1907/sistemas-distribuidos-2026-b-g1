package co.edu.corhuila.csp.catalog.adapter.in.http;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * Negative test for story #287 (STRIDE S): GET /showtimes/{id}/availability of catalog-service must reject a
 * service token that was issued for another audience, even if its sub is booking-service.
 * Copy it to catalog-adapters/src/test/java/co/edu/corhuila/csp/catalog/adapter/in/http/.
 */
class AvailabilityAuthorizationNegativeTest {

    private static final String PATH = "/showtimes/33333333-3333-4333-8333-333333333333/availability";

    private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(
            TestTokens.publicKey(), new ObjectMapper(), new ServiceSubjects(Set.of("booking-service")));

    private int statusFor(String token) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/catalog" + PATH);
        request.setContextPath("/api/v1/catalog");
        request.setServletPath(PATH);
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response.getStatus();
    }

    @Test
    void aUserTokenWithShowtimeWriteIsForbiddenOnAvailability() throws Exception {
        assertEquals(403, statusFor(TestTokens.rs256(TestTokens.claims(
                "44444444-4444-4444-8444-444444444444", List.of("showtime:write"), TestTokens.inOneHour()).build())));
    }

    @Test
    void aBookingServiceTokenForAnotherAudienceIsUnauthorizedOnAvailability() throws Exception {
        assertEquals(401, statusFor(TestTokens.rs256(TestTokens.claims(
                "booking-service", List.of(), TestTokens.inOneHour()).audience("payments-service").build())));
    }
}
