package me.kzv.okvue.infra.security.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import me.kzv.okvue.modules.account.dto.TokenDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.stream.Collectors;

@Slf4j
@Component
public class JwtTokenProvider {

    private static final String AUTHORITIES_KEY = "auth";
    private static final String BEARER_TYPE = "bearer ";
    private static final String ISSUER_NAME = "kzv";
    private static final long ACCESS_TOKEN_EXPIRE_MINUTES = 30;
    private static final long REFRESH_TOKEN_EXPIRE_DAYS = 7;
    private static final Date accessTokenExpiresIn = Date.from(Instant.now().plus(ACCESS_TOKEN_EXPIRE_MINUTES, ChronoUnit.MINUTES));
    private static final Date refreshTokenExpiresIn = Date.from(Instant.now().plus(REFRESH_TOKEN_EXPIRE_DAYS, ChronoUnit.DAYS));

    private final Key SECRET_KEY;

    public JwtTokenProvider(@Value("${jwt.secret}") String key) {
        this.SECRET_KEY = Keys.hmacShaKeyFor(Decoders.BASE64.decode(key));
    }

    public TokenDto create(Authentication authentication) {
        String authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        String accessToken = Jwts.builder()
                .setSubject(authentication.getName())
                .claim(AUTHORITIES_KEY, authorities)
                .setExpiration(accessTokenExpiresIn)
                .signWith(SECRET_KEY, SignatureAlgorithm.HS512)
                .setIssuer(ISSUER_NAME)
                .compact();

        String refreshToken = Jwts.builder()
                .setExpiration(refreshTokenExpiresIn)
                .signWith(SECRET_KEY, SignatureAlgorithm.HS512)
                .compact();

        return TokenDto.builder()
                .grantType(BEARER_TYPE)
                .accessToken(accessToken)
                .accessTokenExpiresIn(accessTokenExpiresIn.getTime())
                .refreshToken(refreshToken)
                .build();
    }

    public String validateAndGetAccountId(String token) {
        Claims claims = parseClaims(token);

        if (claims.get(AUTHORITIES_KEY) == null) {
            throw new RuntimeException("?????? ????????? ?????? ???????????????.");
        }

        return claims.getSubject();
    }


    public Authentication getAuthentication(String accessToken) {
        Claims claims = parseClaims(accessToken);

        if (claims.get(AUTHORITIES_KEY) == null) {
            throw new RuntimeException("?????? ????????? ?????? ???????????????.");
        }

        // ??????????????? ?????? ?????? ????????????
        Collection<? extends GrantedAuthority> authorities =
                Arrays.stream(claims.get(AUTHORITIES_KEY).toString().split(","))
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());

        // UserDetails ????????? ???????????? Authentication ??????
        UserDetails principal = new User(claims.getSubject(), "", authorities);
        return new UsernamePasswordAuthenticationToken(principal, "", authorities);
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(SECRET_KEY).build().parseClaimsJws(token);
            return true;
        } catch (io.jsonwebtoken.security.SecurityException | MalformedJwtException e) {
            log.info("????????? JWT ???????????????.");
        } catch (ExpiredJwtException e) {
            log.info("????????? JWT ???????????????.");
        } catch (UnsupportedJwtException e) {
            log.info("???????????? ?????? JWT ???????????????.");
        } catch (IllegalArgumentException e) {
            log.info("JWT ????????? ?????????????????????.");
        }
        return false;
    }

    private Claims parseClaims(String accessToken) {
        try {
            return Jwts.parserBuilder().setSigningKey(SECRET_KEY).build().parseClaimsJws(accessToken).getBody(); // Jwts????????? exception??????
        } catch (ExpiredJwtException e) {
            return e.getClaims();
        }
    }
}
