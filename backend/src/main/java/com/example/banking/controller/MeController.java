package com.example.banking.controller;

import com.example.banking.dto.UserDto;
import com.example.banking.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me")
@RequiredArgsConstructor
public class MeController {

    @GetMapping
    public UserDto me(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return UserDto.from(user);
    }
}
