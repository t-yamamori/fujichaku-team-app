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
    // 【変更】void → Integer（登録した会員IDを返す）
    public Integer register(RegisterForm form) {

        Members member = new Members();

        member.setName(form.getName());

        // RegisterFormのbirthDateはLocalDate型のため、そのまま設定
        member.setBirthDate(form.getBirthDate());

        member.setMail(form.getMail());

        // パスワードをハッシュ化
        member.setPassword(
            passwordEncoder.encode(form.getPassword())
        );

        // INSERT すると、useGeneratedKeys の設定で member の id に会員IDが入る
        memberMapper.insert(member);

        // 【追加】DBが決めた会員IDを返す
        return member.getId();
    }

    // メールがすでに使われているか（今までと同じ）
    public boolean existsByMail(String mail) {
        return memberMapper.existsByMail(mail);
    }
}