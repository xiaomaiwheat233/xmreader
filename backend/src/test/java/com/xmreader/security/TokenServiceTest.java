package com.xmreader.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.xmreader.user.UserEntity;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

@ExtendWith(MockitoExtension.class)
class TokenServiceTest {

    private static final Instant ISSUED_AT = Instant.parse("2026-01-02T03:04:05Z");

    @Mock
    private JwtEncoder jwtEncoder;

    @Test
    void createAccessTokenBuildsExpectedClaimsAndHeader() {
        SecurityProperties properties = properties();
        TokenService tokenService = new TokenService(
                jwtEncoder,
                properties,
                Clock.fixed(ISSUED_AT, ZoneOffset.UTC));
        UserEntity user = new UserEntity();
        user.setId(42L);
        user.setUsername("reader");
        user.setRole("USER");
        when(jwtEncoder.encode(any(JwtEncoderParameters.class))).thenReturn(
            Jwt.withTokenValue("access-token")
                .header("alg", "HS256")
                .claim("sub", "42")
                .build());

        String token = tokenService.createAccessToken(user, 99L);

        assertThat(token).isEqualTo("access-token");
        ArgumentCaptor<JwtEncoderParameters> captor = ArgumentCaptor.forClass(JwtEncoderParameters.class);
        verify(jwtEncoder).encode(captor.capture());
        JwtClaimsSet claims = captor.getValue().getClaims();
        assertThat(claims.getIssuer()).hasToString("https://xmreader.test");
        assertThat(claims.getAudience()).containsExactly("web");
        assertThat(claims.getIssuedAt()).isEqualTo(ISSUED_AT);
        assertThat(claims.getExpiresAt()).isEqualTo(ISSUED_AT.plusSeconds(900));
        assertThat(claims.getSubject()).isEqualTo("42");
        assertThat(claims.getClaimAsString("username")).isEqualTo("reader");
        assertThat(claims.getClaimAsString("role")).isEqualTo("USER");
        assertThat(claims.getClaimAsString("sid")).isEqualTo("99");
        assertThat(captor.getValue().getJwsHeader().getAlgorithm().getName()).isEqualTo("HS256");
    }

    @Test
    void accessTokenTtlSecondsReturnsConfiguredDuration() {
        TokenService tokenService = new TokenService(
                jwtEncoder,
                properties(),
                Clock.fixed(ISSUED_AT, ZoneOffset.UTC));

        assertThat(tokenService.accessTokenTtlSeconds()).isEqualTo(900);
    }

    private SecurityProperties properties() {
        return new SecurityProperties(
                "https://xmreader.test",
                "web",
                "secret",
                Duration.ofMinutes(15),
                Duration.ofDays(7),
                "refresh_token",
                false);
    }
}
