
package com.capstone.filenexus.service;

import com.capstone.filenexus.dto.request.LoginRequest;
import com.capstone.filenexus.dto.response.LoginResponse;

public interface AuthService {

    LoginResponse login(LoginRequest request);
}
