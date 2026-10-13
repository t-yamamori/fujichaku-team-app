package com.example.demo.service;

import java.time.LocalDate;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.demo.entity.Members;
import com.example.demo.mapper.MembersMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final MembersMapper memberMapper;
    private final PasswordEncoder passwordEncoder;

    // 名前と生年月日で本人確認
    public Members verifyMember(String name, LocalDate birthDate) {
        return memberMapper.findByNameAndBirthDate(name, birthDate);
    }
    
    // パスワードを再設定
    public void resetPassword(int memberId, String password) {

        // パスワードを暗号化する
        String encodedPassword = passwordEncoder.encode(password);

        // DBのパスワードを更新する
        memberMapper.updatePassword(memberId, encodedPassword);
    }
}