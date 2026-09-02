package com.crm.matrix.security;

import com.crm.matrix.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.List;
import java.util.Set;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    @Value("${jwt.refresh-expiration}")
    private long jwtRefreshExpiration;



    public String generateToken(User user, String role, Set<String> permissions) {

        return Jwts.builder()

                .subject(user.getEmployeeCode())

                .claim("tokenType", "ACCESS")

                .claim("userId", user.getId())

                .claim("employeeCode", user.getEmployeeCode())

                .claim("role", role)

                .claim("departmentId", user.getDepartment() != null ? user.getDepartment().getId() : null)

                .claim("teamId", user.getTeam() != null ? user.getTeam().getId() : null)

                .claim("permissions", permissions)

                .issuedAt(new Date())

                .expiration(new Date(System.currentTimeMillis() + jwtExpiration))

                .signWith(getSigningKey())

                .compact();
    }



    public String generateRefreshToken(User user) {

        return Jwts.builder()

                .subject(user.getEmployeeCode())

                .claim("tokenType", "REFRESH")

                .claim("userId", user.getId())

                .claim("employeeCode", user.getEmployeeCode())

                .issuedAt(new Date())

                .expiration(new Date(System.currentTimeMillis() + jwtRefreshExpiration))

                .signWith(getSigningKey())

                .compact();
    }



    public String extractUsername(String token) {

        return extractAllClaims(token).getSubject();
    }


    public Long extractUserId(String token) {

        return extractAllClaims(token).get("userId", Long.class);
    }



    public String extractRole(String token) {

        return extractAllClaims(token).get("role", String.class);
    }



    public Long extractDepartmentId(String token) {

        return extractAllClaims(token).get("departmentId", Long.class);
    }

    public Long extractTeamId(String token) {

        return extractAllClaims(token).get("teamId", Long.class);
    }



    public List<String> extractPermissions(String token) {

        return extractAllClaims(token).get("permissions", List.class);
    }



    public String extractTokenType(String token) {

        return extractAllClaims(token).get("tokenType", String.class);
    }


    public boolean isAccessToken(String token) {

        return "ACCESS".equals(extractTokenType(token));
    }


    public boolean isRefreshToken(String token) {

        return "REFRESH".equals(extractTokenType(token));
    }


    public boolean isTokenValid(String token, UserDetails userDetails) {

        String employeeCode = extractUsername(token);

        return employeeCode != null && employeeCode.equals(userDetails.getUsername()) && isAccessToken(token) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {

        return extractAllClaims(token).getExpiration().before(new Date());
    }

    private Claims extractAllClaims(String token) {

        return Jwts.parser()

                .verifyWith(getSigningKey())

                .build()

                .parseSignedClaims(token)

                .getPayload();
    }


    private SecretKey getSigningKey() {

        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);

        return Keys.hmacShaKeyFor(keyBytes);
    }
}