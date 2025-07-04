package com.yash.usermanagement.service;

import com.yash.usermanagement.dto.LoginRequestDTO;
import com.yash.usermanagement.dto.LoginResponseDTO;
import reactor.core.publisher.Mono;

public interface AuthenticationService {
    Mono<LoginResponseDTO> login(LoginRequestDTO loginRequest);

    Mono<Void> logout(String token);
}