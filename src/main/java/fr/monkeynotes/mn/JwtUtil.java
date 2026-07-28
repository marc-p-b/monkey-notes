package fr.monkeynotes.mn;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.*;
import java.security.Key;
import java.util.stream.Collectors;

@Component
public class JwtUtil {

    //TODO config
    private static final long EXPIRATION_TIME = 86400000; // 1 day in ms

    public static final String AUTHORITIES = "authorities";
    public static final String AUTHORITY = "authority";

    private final Key key;

    public JwtUtil(@Value("${app.security.jwt.secret}") String jwtSecretKey) {
        this.key = Keys.hmacShaKeyFor(jwtSecretKey.getBytes());
    }

    public String generateToken(UserDetails ud) {
        return Jwts.builder()
                .setSubject(ud.getUsername())
                .claim(AUTHORITIES, ud.getAuthorities())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public String extractUsername(String token) {
        return Jwts.parserBuilder().setSigningKey(key).build()
                .parseClaimsJws(token).getBody().getSubject();
    }

    public List<SimpleGrantedAuthority> extractAuthorities(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();

        List<SimpleGrantedAuthority> listAuth = claims.get(AUTHORITIES, List.class).stream()
            .map(o -> {
                //todo seems that we got 2 times the same authority for each one
                    LinkedHashMap<String, String> lhMap = (LinkedHashMap) o;
                    return new SimpleGrantedAuthority(lhMap.get(AUTHORITY));
                })
            .toList();

        return listAuth;
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (JwtException e) {
            return false;
        }
    }
}