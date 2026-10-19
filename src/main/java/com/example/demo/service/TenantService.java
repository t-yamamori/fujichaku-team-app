
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

    // 全テナント取得
    public List<Stores> findAll() {
        return tenantMapper.findAll();
    }

    // 複数店舗の一括更新
    @Transactional
    public void updateAll(List<Stores> stores) {
        if (stores == null || stores.isEmpty()) {
            return;
        }

        for (Stores store : stores) {
            tenantMapper.update(store);
        }
    }

    // 論理削除
    @Transactional
    public void deleteByIds(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }

        tenantMapper.deleteByIds(ids);
    }

    // 名前・ふりがな・メールアドレスがすべて一致する店舗の確認
    public boolean existsSameStore(Stores store) {
        return tenantMapper.existsSameStore(
                store.getName(),
                store.getNameKana(),
                store.getMail());
    }

    // メールアドレスの重複確認
    public boolean existsMail(String mail) {
        return tenantMapper.existsMail(mail);
    }

    // 一括更新対象以外に同じ店舗が存在するか確認
    public boolean existsSameStoreExcludingIds(
            Stores store,
            List<Integer> excludedIds) {

        return tenantMapper.existsSameStoreExcludingIds(
                store.getName(),
                store.getNameKana(),
                store.getMail(),
                excludedIds);
    }

    // 一括更新対象以外に同じメールアドレスが存在するか確認
    public boolean existsMailExcludingIds(
            String mail,
            List<Integer> excludedIds) {

        return tenantMapper.existsMailExcludingIds(
                mail,
                excludedIds);
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
