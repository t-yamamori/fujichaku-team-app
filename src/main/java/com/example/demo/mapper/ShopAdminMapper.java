package com.example.demo.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.example.demo.entity.ShopAdmin;

@Mapper
public interface ShopAdminMapper {

    ShopAdmin findById(int id);

    List<ShopAdmin> findAll();

    void update(ShopAdmin shopAdmin);

    void deleteByIds(List<Integer> ids);

    void create(ShopAdmin shopAdmin);
    
    boolean existsStoreId(Integer storeId);
}