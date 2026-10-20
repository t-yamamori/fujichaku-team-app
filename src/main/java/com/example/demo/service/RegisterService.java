
package com.example.demo.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.demo.entity.Members;
import com.example.demo.form.RegisterForm;
import com.example.demo.mapper.MembersMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RegisterService {

    private final MembersMapper memberMapper;
    private final PasswordEncoder passwordEncoder;

    // 会員新規登録
    public void register(RegisterForm form) {

        Members member = new Members();

        member.setName(form.getName());

        // RegisterFormのbirthDateはLocalDate型のため、そのまま設定
        member.setBirthDate(form.getBirthDate());

        member.setMail(form.getMail());

        // パスワードをハッシュ化
        member.setPassword(
            passwordEncoder.encode(form.getPassword())
        );

        memberMapper.insert(member);
    }

    // メールがすでに使われているか
    public boolean existsByMail(String mail) {
        return memberMapper.existsByMail(mail);
    }
}
