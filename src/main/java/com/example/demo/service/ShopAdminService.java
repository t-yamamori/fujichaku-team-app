package com.example.demo.service;

import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.entity.ShopAdmin;
import com.example.demo.mapper.ShopAdminMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ShopAdminService {

    private final ShopAdminMapper shopAdminMapper;
    private final PasswordEncoder passwordEncoder;

    // 一覧取得
    public List<ShopAdmin> findAll() {
        return shopAdminMapper.findAll();
    }

    // 更新
    @Transactional
    public void update(ShopAdmin shopAdmin) {
        shopAdminMapper.update(shopAdmin);
    }

    // 論理削除
    @Transactional
    public void deleteByIds(List<Integer> ids) {

        if (ids == null || ids.isEmpty()) {
            return;
        }

        shopAdminMapper.deleteByIds(ids);
    }

    // 新規登録
    @Transactional
    public void create(ShopAdmin shopAdmin) {

        // パスワードをBCryptでハッシュ化
        String hashedPassword =
                passwordEncoder.encode(shopAdmin.getPassword());

        shopAdmin.setPassword(hashedPassword);

        // DBへ登録
        shopAdminMapper.create(shopAdmin);
    }
}