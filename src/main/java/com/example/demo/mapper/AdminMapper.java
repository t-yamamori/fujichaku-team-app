
package com.example.demo.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.example.demo.entity.Admin;

@Mapper
public interface AdminMapper {

    // IDから管理者を取得
    Admin findById(int id);
}