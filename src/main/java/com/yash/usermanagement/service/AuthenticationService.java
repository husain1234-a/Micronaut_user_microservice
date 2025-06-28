package com.yash.usermanagement.service;

import com.yash.usermanagement.dto.LoginRequestDTO;
import com.yash.usermanagement.dto.LoginResponseDTO;

public interface AuthenticationService {
    LoginResponseDTO login(LoginRequestDTO loginRequest);

    void logout(String token);
}