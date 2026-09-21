package com.juridico.sistema_juridico.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import java.security.Key;
import java.util.Date;
import org.springframework.beans.factory.annotation.Autowired;
import com.juridico.sistema_juridico.repository.Usuarios.UsuarioRepository;

@Component
public class JwtTokenProvider {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.expiration}")
    private long jwtExpirationInMs;

    // 1. Generar el Token
    public String generateToken(Authentication authentication) {
        String username = authentication.getName();
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationInMs);

        Key key = Keys.hmacShaKeyFor(jwtSecret.getBytes());

        int securityVersion = usuarioRepository.findByEmail(username)
                .map(usuario -> usuario.getSecurityVersion() == null ? 0 : usuario.getSecurityVersion())
                .orElse(0);

        return Jwts.builder()
                .setSubject(username)
                .claim("sv", securityVersion)
                .setIssuedAt(new Date())
                .setExpiration(expiryDate)
                .signWith(key, SignatureAlgorithm.HS512)
                .compact();
    }

    // 2. Obtener el usuario del Token
    public String getUsernameFromJWT(String token) {
        Key key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();

        return claims.getSubject();
    }

    public Date getIssuedAtFromJWT(String token) {
        Key key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getIssuedAt();
    }

    public int getSecurityVersionFromJWT(String token) {
        Key key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
        Object value = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .get("sv");
        return value instanceof Number number ? number.intValue() : 0;
    }

    // 3. Validar el Token
    public boolean validateToken(String authToken) {
        try {
            Key key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(authToken);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            // Aquí podrías usar tu GlobalExceptionHandler lanzando una UnauthorizedException
            return false;
        }
    }

    @Value("${app.jwt.refreshExpiration}")
    private long refreshExpirationInMs;

    public String generateRefreshToken(Authentication authentication) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + refreshExpirationInMs);
        Key key = Keys.hmacShaKeyFor(jwtSecret.getBytes());

        int securityVersion = usuarioRepository.findByEmail(authentication.getName())
                .map(usuario -> usuario.getSecurityVersion() == null ? 0 : usuario.getSecurityVersion())
                .orElse(0);
        return Jwts.builder()
                .setSubject(authentication.getName())
                .claim("sv", securityVersion)
                .setIssuedAt(new Date())
                .setExpiration(expiryDate)
                .signWith(key, SignatureAlgorithm.HS512)
                .compact();
    }
}
