package com.crm.matrix.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class AuthController {

    @GetMapping
    public String getUsers() {
        return "Users working";
    }

    @GetMapping("/{id}")
    public String getUser(@PathVariable Long id) {
        return "User " + id;
    }
}
