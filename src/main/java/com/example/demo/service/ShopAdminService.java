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

    public List<ShopAdmin> findAll() {
        return shopAdminMapper.findAll();
    }

    @Transactional
    public void update(ShopAdmin shopAdmin) {
        shopAdminMapper.update(shopAdmin);
    }

    @Transactional
    public void updateAll(List<ShopAdmin> shopAdmins) {
        if (shopAdmins == null || shopAdmins.isEmpty()) {
            return;
        }

        for (ShopAdmin shopAdmin : shopAdmins) {
            shopAdminMapper.update(shopAdmin);
        }
    }

    @Transactional
    public void deleteByIds(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }

        shopAdminMapper.deleteByIds(ids);
    }

    @Transactional
    public void create(ShopAdmin shopAdmin) {
        String hashedPassword = passwordEncoder.encode(shopAdmin.getPassword());
        shopAdmin.setPassword(hashedPassword);
        shopAdminMapper.create(shopAdmin);
    }
    
    public boolean existsStoreId(Integer storeId) {
        if (storeId == null) {
            return false;
        }
        return shopAdminMapper.existsStoreId(storeId);
    }
}