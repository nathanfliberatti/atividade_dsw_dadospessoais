package br.senac.tads.dsw.dadospessoais.seguranca;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

// OncePerRequestFilter garante que doFilterInternal() seja executado exatamente uma vez
// por requisição, mesmo que o Spring passe a requisição por vários filtros encadeados.
@Component
public class JwtFilter extends OncePerRequestFilter {
    @Value("${jwt.secret}")
    private String jwtSecret;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        // Extrai o Cabeçalho "Authorization" da mensagem HTTP recebida
        String authHeader = request.getHeader("Authorization");
        // Se não há header ou não começa com "Bearer ", passa para o próximo filtro
        // sem autenticar.
        // O SecurityConfig decidirá se a rota exige autenticação ou não.
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }
        // Remove o prefixo "Bearer " (7 caracteres, notar o espaço em branco)
        // para obter apenas o token
        String token = authHeader.substring(7);
        try {
            // 1. Deriva a chave de 256 bits a partir do segredo (mesmo processo do
            // JwtService)
            byte[] keyBytes = MessageDigest.getInstance("SHA-256")
                    .digest(jwtSecret.getBytes(StandardCharsets.UTF_8));
            // 2. Faz o parse do token no formato "header.payload.signature"
            SignedJWT jwt = SignedJWT.parse(token);
            // 3. Verifica a assinatura — garante que o token não foi adulterado
            MACVerifier verifier = new MACVerifier(keyBytes);
            if (!jwt.verify(verifier)) {
                // Assinatura inválida: segue sem autenticar.
                // O SecurityConfig bloqueará o acesso se a rota exigir autenticação.
                filterChain.doFilter(request, response);
                return;
            }
            // 4. Verifica a data de expiração
            JWTClaimsSet claims = jwt.getJWTClaimsSet();
            if (claims.getExpirationTime() == null
                    || claims.getExpirationTime().toInstant().isBefore(Instant.now())) {
                // Token expirado: segue sem autenticar
                filterChain.doFilter(request, response);
                return;
            }
            // 5. Extrai o username ("sub") e as roles do payload
            String username = claims.getSubject();
            List<String> roles = claims.getStringListClaim("roles");
            List<SimpleGrantedAuthority> authorities = new ArrayList<>();
            if (roles != null) {
                for (String role : roles) {
                    authorities.add(new SimpleGrantedAuthority(role));
                }
            }
            // 6. Cria o objeto de autenticação e registra no SecurityContextHolder.
            // A partir daqui, o Spring Security reconhece o usuário como autenticado
            // para toda a cadeia de processamento desta requisição.
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(username, null,
                    authorities);
            authentication.setDetails(
                    new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (Exception e) {
            // Token malformado ou erro inesperado: ignora e segue sem autenticar.
            // Não lançamos exceção aqui para não interromper a cadeia de filtros.
        }
        // Independente do resultado da validação, a requisição continua a cadeia de
        // filtros.
        // Se o usuário não foi autenticado e a rota exige autenticação, o
        // SecurityConfig
        // retornará 401 Unauthorized.
        filterChain.doFilter(request, response);
    }
}