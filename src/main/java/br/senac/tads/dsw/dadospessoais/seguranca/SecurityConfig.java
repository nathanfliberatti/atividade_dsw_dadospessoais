package br.senac.tads.dsw.dadospessoais.seguranca;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtFilter jwtFilter;

    public SecurityConfig(JwtFilter jwtFilter) {
        this.jwtFilter = jwtFilter;
    }

    // Define a cadeia de filtros HTTP que o Spring Security aplica em cada
    // requisição.
    // A ordem das regras em authorizeHttpRequests importa: a primeira regra que
    // casar é aplicada.
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
                // Desabilita CSRF — não é necessário em APIs REST stateless
                // (CSRF protege formulários HTML com sessão; aqui usamos JWT por requisição)
                .csrf(csrf -> csrf.disable())

                // Permite renderização em frames — necessário para o console H2 funcionar no
                // navegador
                .headers(headers -> headers
                        .frameOptions(frame -> frame.disable()))

                // Define a política de sessão como STATELESS: o servidor não cria nem usa
                // sessão HTTP.
                // Cada requisição deve ser autenticada de forma independente (via JWT no
                // header).
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Regras de autorização por URL
                .authorizeHttpRequests(auth -> auth

                        // Endpoint de login: sempre público (é aqui que o usuário obtém o token)
                        .requestMatchers(HttpMethod.POST, "/login").permitAll()

                        // Console H2: público durante o desenvolvimento (REMOVER em produção!)
                        .requestMatchers("/h2-console/**").permitAll()

                        // Swagger UI: público para facilitar testes
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()

                        .requestMatchers("/me").authenticated()

                        .requestMatchers("/*.html", "/*.css", "/*.js").permitAll()

                        // Leitura de pessoas: público
                        .requestMatchers(HttpMethod.GET, "/pessoas", "/pessoas/**").permitAll()
                        // Escrita exige autenticação (o @PreAuthorize no controller exigirá ROLE_ADMIN)
                        .requestMatchers(HttpMethod.POST, "/pessoas", "/pessoas/**").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/pessoas/**").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/pessoas/**").authenticated()

                        // Todo o resto exige autenticação
                        .anyRequest().authenticated())

                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // AuthenticationManager: componente responsável por executar a autenticação.
    // DaoAuthenticationProvider é a implementação que usa um UserDetailsService
    // (nosso service)
    // UserDetailsService para buscar o usuário e um PasswordEncoder para comparar a
    // senha.
    @Bean
    public AuthenticationManager authenticationManager(
            UserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder) {

        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);

        return new ProviderManager(provider);
    }

    // PasswordEncoder: define como as senhas são codificadas e comparadas.
    // ATENÇÃO: NoOpPasswordEncoder armazena senhas em TEXTO PURO.
    // Uso APENAS para fins didáticos — será substituído por BCrypt na Etapa 7.
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}