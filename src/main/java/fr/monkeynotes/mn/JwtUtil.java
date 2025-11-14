package fr.monkeynotes.mn;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.*;
import java.security.Key;
import java.util.stream.Collectors;

public class JwtUtil {

    //TODO different for each env ?
    private static final String SECRET_KEY = "SD4vhsqkP2WhBrrHm6QWqxEG2W1pagG6"; // minimum 256-bit for HS256
    private static final long EXPIRATION_TIME = 86400000; // 1 day in ms

    private static final Key key = Keys.hmacShaKeyFor(SECRET_KEY.getBytes());
    public static final String AUTHORITIES = "authorities";
    public static final String AUTHORITY = "authority";

    public static String generateToken(UserDetails ud) {
        return Jwts.builder()
                .setSubject(ud.getUsername())
                .claim(AUTHORITIES, ud.getAuthorities())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public static String extractUsername(String token) {
        return Jwts.parserBuilder().setSigningKey(key).build()
                .parseClaimsJws(token).getBody().getSubject();
    }

    public static List<SimpleGrantedAuthority> extractAuthorities(String token) {
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

    public static boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (JwtException e) {
            return false;
        }
    }
}