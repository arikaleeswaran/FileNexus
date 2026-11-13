
package com.capstone.filenexus.service;

import com.capstone.filenexus.dto.request.RegisterRequest;
import com.capstone.filenexus.dto.response.UserResponse;

public interface UserService {

    UserResponse registerUser(RegisterRequest request);
}
