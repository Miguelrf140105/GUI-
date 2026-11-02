package pe.edu.gui.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import pe.edu.gui.filter.LoginThrottle;
import pe.edu.gui.security.GuiPrincipal;
import pe.edu.gui.service.SeguridadService;

/** Configura el acceso HTTP, la sesión y la protección de las peticiones de escritura. */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

  /** Usa BCrypt con coste 12 para generar y comprobar los hashes de contraseña. */
  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(12);
  }

  /**
   * Carga el ID y el hash de la cuenta; Spring Security comprueba la contraseña durante el login.
   */
  @Bean
  UserDetailsService userDetails(SeguridadService seguridad) {
    return username -> {
      var user = seguridad.credenciales(username);
      return new GuiPrincipal(
          user.id(), user.username(), user.passwordHash(), user.passwordVersion());
    };
  }

  /**
   * Protege la API independiente del frontend. Mantiene CSRF activo y cambia el ID de sesión al
   * autenticar. El servidor Angular redirige /api al backend mediante su proxy de desarrollo.
   */
  @Bean
  SecurityFilterChain security(HttpSecurity http, SeguridadService seguridad) throws Exception {
    return http.authorizeHttpRequests(
            a ->
                a.requestMatchers(
                        "/api/auth/csrf",
                        "/api/auth/login",
                        "/api/auth/recovery/status",
                        "/api/auth/recovery/request",
                        "/api/auth/recovery/complete",
                        "/actuator/health")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .formLogin(
            f ->
                f.loginProcessingUrl("/api/auth/login")
                    .successHandler((req, res, auth) -> res.setStatus(204))
                    .failureHandler((req, res, e) -> res.sendError(401, "Credenciales inválidas")))
        .logout(
            l ->
                l.logoutUrl("/api/auth/logout")
                    .logoutSuccessHandler((req, res, auth) -> res.setStatus(204)))
        .exceptionHandling(
            e ->
                e.authenticationEntryPoint(
                    (req, res, ex) -> res.sendError(401, "Sesión requerida")))
        .sessionManagement(s -> s.sessionFixation(f -> f.changeSessionId()))
        .headers(
            h ->
                h.contentSecurityPolicy(
                    c ->
                        c.policyDirectives(
                            "default-src 'self'; script-src 'self'; "
                                + "style-src 'self' 'unsafe-inline'; "
                                + "font-src 'self' data:; "
                                + "img-src 'self' data: blob:; connect-src 'self'; "
                                + "object-src 'none'; base-uri 'self'; "
                                + "form-action 'self'; frame-ancestors 'none'")))
        .addFilterAfter(
            new pe.edu.gui.filter.PasswordVersionFilter(seguridad),
            org.springframework.security.web.context.SecurityContextHolderFilter.class)
        .addFilterBefore(
            new pe.edu.gui.filter.PasswordRecoveryThrottle(),
            UsernamePasswordAuthenticationFilter.class)
        .addFilterBefore(new LoginThrottle(), UsernamePasswordAuthenticationFilter.class)
        .build();
  }
}
