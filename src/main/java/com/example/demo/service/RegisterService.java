package com.example.demo.service;

import java.time.LocalDate;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.demo.entity.Member;
import com.example.demo.form.RegisterForm;
import com.example.demo.mapper.MemberMapper;

@Service
public class RegisterService {

    private final MemberMapper memberMapper;
    private final PasswordEncoder passwordEncoder;

    public RegisterService(
            MemberMapper memberMapper,
            PasswordEncoder passwordEncoder) {

        this.memberMapper = memberMapper;
        this.passwordEncoder = passwordEncoder;
    }

    // 会員新規登録
    public void register(RegisterForm form) {

        Member member = new Member();

        member.setName(form.getName());

        member.setBirthDate(
        		LocalDate.parse(form.getBirthDate())
        );

        member.setMail(form.getMail());

        // パスワードをハッシュ化
        member.setPassword(
                passwordEncoder.encode(form.getPassword())
        );

        memberMapper.insert(member);
    }
}