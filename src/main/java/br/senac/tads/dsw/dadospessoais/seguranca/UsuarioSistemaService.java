package br.senac.tads.dsw.dadospessoais.seguranca;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

// Implementa UserDetailsService — interface que o Spring Security usa para carregar um usuário
// pelo username durante o processo de autenticação.
// O método loadUserByUsername() é chamado automaticamente pelo AuthenticationManager.
@Service
public class UsuarioSistemaService implements UserDetailsService {

    private final Map<String, UserDetails> usuarios;

    // PasswordEncoder é injetado pelo Spring para codificar as senhas na inicialização.
    // Neste exemplo usamos o NoOpPasswordEncoder (texto puro). Será trocado por BCrypt no Etapa 7.
    public UsuarioSistemaService(PasswordEncoder passwordEncoder) {
        this.usuarios = new HashMap<>();

        // admin — acesso total: ROLE_ADMIN, ROLE_GERENTE e ROLE_USER
        this.usuarios.put("admin", new UsuarioSistema(
            "admin",
            passwordEncoder.encode("Abcd%12345"),
            List.of(
                new SimpleGrantedAuthority("ROLE_ADMIN"),
                new SimpleGrantedAuthority("ROLE_GERENTE"),
                new SimpleGrantedAuthority("ROLE_USER")
            )
        ));

        // user1 — acesso intermediário: ROLE_GERENTE e ROLE_USER
        this.usuarios.put("user1", new UsuarioSistema(
            "user1",
            passwordEncoder.encode("Abcd%12345"),
            List.of(
                new SimpleGrantedAuthority("ROLE_GERENTE"),
                new SimpleGrantedAuthority("ROLE_USER")
            )
        ));

        // user2 — acesso básico: somente ROLE_USER
        this.usuarios.put("user2", new UsuarioSistema(
            "user2",
            passwordEncoder.encode("Abcd%12345"),
            List.of(
                new SimpleGrantedAuthority("ROLE_USER")
            )
        ));
    }

    // Chamado pelo Spring Security quando precisa verificar as credenciais de um usuário.
    // Ele usa username real, este método consultaria o banco de dados.
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserDetails usuario = usuarios.get(username);

        if (usuario == null) {
            throw new UsernameNotFoundException("Usuário não encontrado: " + username);
        }

        return usuario;
    }
}