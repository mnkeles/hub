package etiya.omniAutomation.controller;

import etiya.omniAutomation.business.dto.AuthResponse;
import etiya.omniAutomation.request.AuthRequest;
import etiya.omniAutomation.request.RefreshTokenRequest;
import etiya.omniAutomation.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@CrossOrigin("*")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final KeyPair rsaKeyPair;

    @GetMapping("/public-key")
    public ResponseEntity<Map<String, String>> getPublicKey() {
        String base64Key = Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.UTF_8))
                .encodeToString(rsaKeyPair.getPublic().getEncoded());
        String pemKey = "-----BEGIN PUBLIC KEY-----\n" + base64Key + "\n-----END PUBLIC KEY-----";
        Map<String, String> response = new HashMap<>();
        response.put("publicKey", pemKey);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest authRequest) {
        AuthResponse login = this.authService.login(authRequest);
        return ResponseEntity.ok(login);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(@RequestBody RefreshTokenRequest refreshTokenRequest) {
        AuthResponse response = this.authService.refreshToken(refreshTokenRequest.getRefreshToken());
        return ResponseEntity.ok(response);
    }
}
