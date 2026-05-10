package com.weekendgo.auth;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthTokenStore authTokenStore;

    public AuthService(
            UserAccountRepository userAccountRepository,
            PasswordEncoder passwordEncoder,
            AuthTokenStore authTokenStore
    ) {
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
        this.authTokenStore = authTokenStore;
    }

    public UserProfileResponse register(RegisterRequest request) {
        if (userAccountRepository.existsByUsername(request.username())) {
            throw new DuplicateUsernameException();
        }
        UserAccount account = userAccountRepository.save(
                request.username(),
                passwordEncoder.encode(request.password()),
                UserRole.USER,
                request.nickname()
        );
        return UserProfileResponse.from(account);
    }

    public AuthTokenResponse login(LoginRequest request) {
        UserAccount account = userAccountRepository.findByUsername(request.username())
                .filter(candidate -> passwordEncoder.matches(request.password(), candidate.passwordHash()))
                .orElseThrow(InvalidCredentialsException::new);

        String token = authTokenStore.issueToken(account);
        return new AuthTokenResponse(token, UserProfileResponse.from(account));
    }

    public void logout(String token) {
        authTokenStore.revoke(token);
    }

    public UserProfileResponse updateNickname(long userId, String nickname) {
        userAccountRepository.updateNickname(userId, nickname);
        return UserProfileResponse.from(
                userAccountRepository.findById(userId).orElseThrow()
        );
    }
}
