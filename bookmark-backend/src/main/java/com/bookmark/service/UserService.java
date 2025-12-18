package com.bookmark.service;

import com.bookmark.dto.request.LoginRequest;
import com.bookmark.dto.request.RegisterRequest;
import com.bookmark.dto.response.LoginResponse;
import com.bookmark.entity.User;

public interface UserService {
    LoginResponse register(RegisterRequest request);

    LoginResponse login(LoginRequest request);

    User getCurrentUser();
}
