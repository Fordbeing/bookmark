package com.bookmark.dto.response;

import lombok.Data;

@Data
public class LoginResponse {
    private Long id;
    private String username;
    private String email;
    private String nickname;
    private String phone;
    private String avatar;
    private String token;
}
