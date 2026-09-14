package com.example.scheduling.auth;

import com.example.scheduling.user.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.time.*;

@RestController @RequestMapping("/api/auth")
public class AuthController {
    private final UserRepository users; private final PasswordEncoder passwords; private final JwtEncoder jwt;
    public AuthController(UserRepository users,PasswordEncoder passwords,JwtEncoder jwt){this.users=users;this.passwords=passwords;this.jwt=jwt;}
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request){
        var user=users.findByEmailIgnoreCase(request.email()).filter(AppUser::isActive)
            .filter(u->passwords.matches(request.password(),u.getPasswordHash()))
            .orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED,"帳號或密碼錯誤"));
        var now=Instant.now();
        var claims=JwtClaimsSet.builder().issuer("scheduling").issuedAt(now).expiresAt(now.plus(Duration.ofHours(8)))
            .subject(user.getId().toString()).claim("email",user.getEmail()).claim("name",user.getDisplayName()).claim("role",user.getRole().name()).build();
        var token=jwt.encode(JwtEncoderParameters.from(JwsHeader.with(() -> "HS256").build(),claims)).getTokenValue();
        return new LoginResponse(token,new CurrentUser(user.getId(),user.getEmail(),user.getDisplayName(),user.getRole().name()));
    }
    public record LoginRequest(@Email @NotBlank String email,@NotBlank String password){}
    public record CurrentUser(Long id,String email,String displayName,String role){}
    public record LoginResponse(String token,CurrentUser user){}
}

