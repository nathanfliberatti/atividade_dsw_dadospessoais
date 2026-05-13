package br.senac.tads.dsw.dadospessoais.seguranca;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiracao-segundos:3600}")
    private long expiracaoSegundos;

    /**
     * Gera um JWT a partir dos dados do usuário autenticado.
     * Monta as claims e delega a assinatura para JwtEncode().
     */
    public String gerarToken(UsuarioSistema usuario) {
        // Converte as authorities para uma lista de Strings (ex: "ROLE_ADMIN", "ROLE_USER")
        List<String> roles = new ArrayList<>();
        for (GrantedAuthority auth : usuario.getAuthorities()) {
            roles.add(auth.getAuthority());
        }

        Instant agora = Instant.now();
        Instant expiracao = agora.plusSeconds(expiracaoSegundos);

        // JWTClaimsSet representa o payload do token → o conjunto de "claims" (afirmações)
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(usuario.getUsername())      // "sub" identificador do usuário
                .issuer("dados-pessoais-api")        // "iss" quem emitiu o token
                .issueTime(Date.from(agora))         // "iat" timestamp de emissão (issued at)
                .expirationTime(Date.from(expiracao)) // "exp" timestamp de expiração
                .claim("roles", roles)               // claim customizada com as roles do usuário
                .build();

        return jwtEncode(claims);
    }

    /**
     * Assina um JWTClaimsSet e serializa o resultado no formato 'header.payload.signature'.
     *
     * Separar esta lógica de gerarToken() permite reutilizá-la para outros tipos de token
     * (ex: refresh token) sem duplicar a lógica de assinatura.
     */
    private String jwtEncode(JWTClaimsSet claims) {
        try {
            // 1. Deriva uma chave de 256 bits a partir da string secreta usando SHA-256.
            // Isso permite usar qualquer string como segredo, independente do comprimento.
            byte[] keyBytes = MessageDigest.getInstance("SHA-256")
                    .digest(jwtSecret.getBytes(StandardCharsets.UTF_8));

            MACSigner signer = new MACSigner(keyBytes);

            // 2. Cria o JWT combinado o header (algoritmo) com o payload (claims)
            SignedJWT jwt = new SignedJWT(
                    new JWSHeader(JWSAlgorithm.HS256), // HS256 = HMAC-SHA256
                    claims
            );

            // 3. Assina com a chave secreta → gera a terceira parte do token
            jwt.sign(signer);

            // 4. Serializa para a String final: "header.payload.signature"
            return jwt.serialize();

        } catch (JOSEException | NoSuchAlgorithmException e) {
            throw new RuntimeException("Erro ao assinar token JWT", e);
        }
    }
}