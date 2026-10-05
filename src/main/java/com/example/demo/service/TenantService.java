package com.example.demo.service;

import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.entity.Stores;
import com.example.demo.mapper.TenantMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TenantService {

    private final TenantMapper tenantMapper;
    private final PasswordEncoder passwordEncoder;

    // 一覧取得
    public List<Stores> findAll() {
        return tenantMapper.findAll();
    }

    // 更新
    @Transactional
    public void update(Stores store) {
        tenantMapper.update(store);
    }

    // 論理削除
    @Transactional
    public void deleteByIds(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        tenantMapper.deleteByIds(ids);
    }

    // 新規登録
    @Transactional
    public void create(Stores store) {
        // パスワードをBCryptでハッシュ化
        String hashedPassword =
                passwordEncoder.encode(store.getPassword());
        store.setPassword(hashedPassword);
        // DBへ登録
        tenantMapper.create(store);
    }
}