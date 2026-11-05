package ma.fstg.security.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    /**
     * Utilisateurs en memoire (herites du TP1).
     * {noop} = mot de passe en clair : acceptable en TP, JAMAIS en production.
     */
    @Bean
    public UserDetailsService userDetailsService() {
        UserDetails admin = User.withUsername("admin")
                .password("{noop}1234")
                .roles("ADMIN")
                .build();

        UserDetails user = User.withUsername("user")
                .password("{noop}1111")
                .roles("USER")
                .build();

        return new InMemoryUserDetailsManager(admin, user);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // L'ordre compte : la premiere regle qui correspond s'applique
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/login", "/access-denied", "/css/**").permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/user/**").hasAnyRole("USER", "ADMIN")
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")                  // GET  : page personnalisee (AuthController)
                .loginProcessingUrl("/authenticate")  // POST : traite par Spring Security
                .defaultSuccessUrl("/home", true)     // succes -> /home (toujours)
                .failureUrl("/login?error=true")      // echec  -> /login?error=true
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")                 // POST uniquement (CSRF)
                .logoutSuccessUrl("/login?logout=true")
                .permitAll()
            )
            // Page 403 personnalisee (utilisateur connecte mais role insuffisant)
            .exceptionHandling(ex -> ex.accessDeniedPage("/access-denied"));

        return http.build();
    }

    /*
     * ------------------------------------------------------------------
     * VARIANTE (bonus) : redirection selon le role apres connexion
     * Remplacer .defaultSuccessUrl("/home", true) par
     *     .successHandler(roleBasedSuccessHandler())
     * ------------------------------------------------------------------
     *
     * @Bean
     * public AuthenticationSuccessHandler roleBasedSuccessHandler() {
     *     return (request, response, authentication) -> {
     *         boolean isAdmin = authentication.getAuthorities().stream()
     *                 .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
     *         response.sendRedirect(request.getContextPath()
     *                 + (isAdmin ? "/admin/dashboard" : "/user/dashboard"));
     *     };
     * }
     */
}
