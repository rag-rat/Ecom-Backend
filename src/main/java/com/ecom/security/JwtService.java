package com.ecom.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;


@Service
@Slf4j
public class JwtService {

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.access-token-expiration}")
    private long accessTokenExpirationMs;

    @Value("${app.jwt.refresh-token-expiration}")
    private long refreshTokenExpirationMs;



    //////////////////// Access Token Generation


    public String generateAccessToken(UserDetails userDetailis, String userId, String role){
        Map<String, Object> extraClaims = new HashMap<>();

        extraClaims.put("role", role);
        extraClaims.put("type", "ACCESS");

        return buildToken(extraClaims, userId, accessTokenExpirationMs);
    }




    ////////////////// Generate Refresh Token

    public String generateRefreshToken(String userId){
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("type", "REFRESH");

        return buildToken(extraClaims, userId, refreshTokenExpirationMs);
    }




    ///////////////////// Build Token

    private String buildToken(
            Map<String, Object> extraClaims,
            String subject,
            long expirationMs){

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .claims(extraClaims)
                .subject(subject)
                .issuedAt(now)
                .expiration(expiryDate)
                .id(UUID.randomUUID().toString())
                .signWith(getSigningKey())
                .compact();

    }




    //////////////////////// Token Validation

    public boolean validateToken(String token, UserDetails userDetails){
        try{
            final String userId = extractUserId(token);
            return userId.equals(userDetails.getUsername()) && !isTokenExpired(token);
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("JWT validation failed: {}", e.getMessage());
            return false;
        }
    }



    /////////     Checking Token Expiration
    public boolean isTokenExpired(String token){
        return extractExpiration(token).before(new Date());
    }



    //////////////// Claim Extraction Methods
    public String extractUserId(String token){
        return extractClaim(token, Claims::getSubject);
    }


    public String extractJti(String token){
        return extractClaim(token, Claims::getId);
    }


    public String extractRole(String token){
        return extractClaim(token, claims -> claims.get("role", String.class));
    }


    public Date extractExpiration(String token){
        return extractClaim(token, Claims::getExpiration);
    }


    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver){
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }


    private Claims extractAllClaims(String token){
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }


    private SecretKey getSigningKey(){
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }


    public long getRemainingValidityMs(String token){
        Date expiration = extractExpiration(token);
        long remaining = expiration.getTime() - System.currentTimeMillis();
        return Math.max(0, remaining);
    }
}
