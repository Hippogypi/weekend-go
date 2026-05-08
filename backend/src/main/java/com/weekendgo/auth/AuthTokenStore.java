package com.weekendgo.auth;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class AuthTokenStore {

    private final SecureRandom secureRandom = new SecureRandom();
    private final Map<String, Long> userIdsByToken = new ConcurrentHashMap<>();

    public String issueToken(UserAccount account) {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        String token = "wg_" + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        userIdsByToken.put(token, account.id());
        return token;
    }

    public Optional<Long> findUserId(String token) {
        return Optional.ofNullable(userIdsByToken.get(token));
    }

    public void revoke(String token) {
        userIdsByToken.remove(token);
    }
}
