package com.microshop.auth.api;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Trang dang nhap tuy bien (templates/login.html): nut GitHub + form tai khoan demo. */
@Controller
public class LoginController {

    @GetMapping("/login")
    String login() {
        return "login";
    }
}
