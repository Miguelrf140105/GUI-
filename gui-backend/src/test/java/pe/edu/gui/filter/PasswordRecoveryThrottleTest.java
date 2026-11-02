package pe.edu.gui.filter;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.*;

class PasswordRecoveryThrottleTest {
  @Test
  void limitaPeticionesSinConfiarEnXForwardedFor() throws Exception {
    var filter = new PasswordRecoveryThrottle();
    for (int i = 0; i < 11; i++) {
      var req = new MockHttpServletRequest("POST", "/api/auth/recovery/request");
      req.setServletPath("/api/auth/recovery/request");
      req.setRemoteAddr("127.0.0.1");
      req.addHeader("X-Forwarded-For", "10.0.0." + i);
      var response = new MockHttpServletResponse();
      filter.doFilter(req, response, new MockFilterChain());
      assertEquals(i < 10 ? 200 : 429, response.getStatus());
    }
  }
}
