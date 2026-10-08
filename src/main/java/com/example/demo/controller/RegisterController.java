package com.example.demo.controller;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import com.example.demo.form.RegisterForm;
import com.example.demo.service.RegisterService;

@Controller
public class RegisterController {

    private static final String SESSION_KEY = "registerForm";

    private final RegisterService service;

    public RegisterController(RegisterService service) {
        this.service = service;
    }

    // ホーム("/")は入力画面へ
    @GetMapping("/")
    public String home() {
        return "redirect:/register";
    }

    // 1. 入力画面

    @GetMapping("/register/register")
    public String showRegister(HttpSession session, Model model) {

        RegisterForm form =
                (RegisterForm) session.getAttribute(SESSION_KEY);

        model.addAttribute(
                "form",
                form != null ? form : new RegisterForm()
        );

        return "register/register";
    }

    // 2. 確認画面
    @PostMapping("/register/confirm")
    public String confirm(
            @Valid @ModelAttribute("form") RegisterForm form,
            BindingResult result,
            HttpSession session) {

        if (result.hasErrors()) {
            return "register/register";
        }

        session.setAttribute(SESSION_KEY, form);

        return "register/confirm";
    }

    // 3. DB登録
    @PostMapping("/register/complete")
    public String complete(HttpSession session) {

        RegisterForm form =
                (RegisterForm) session.getAttribute(SESSION_KEY);

        if (form == null) {
            return "redirect:/register";
        }

        service.register(form);

        session.removeAttribute(SESSION_KEY);

        return "register/complete";
    }
}