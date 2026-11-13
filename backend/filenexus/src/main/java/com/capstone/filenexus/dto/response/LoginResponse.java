
package com.capstone.filenexus.dto.response;

public class LoginResponse {

    private String accessToken;
    private String tokenType;
    private long expiresIn;
    private UserResponse user;

    public LoginResponse(
            String accessToken,
            String tokenType,
            long expiresIn,
            UserResponse user
    ) {
        this.accessToken = accessToken;
        this.tokenType = tokenType;
        this.expiresIn = expiresIn;
        this.user = user;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public long getExpiresIn() {
        return expiresIn;
    }

    public UserResponse getUser() {
        return user;
    }
}
