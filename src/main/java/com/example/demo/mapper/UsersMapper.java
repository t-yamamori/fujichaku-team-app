package com.example.demo.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.example.demo.entity.Users;

@Mapper
public interface UsersMapper {

    /**
     * IDで非会員を取得
     */
    Users findById(@Param("id") int id);

    /**
     * メールアドレスで非会員を取得
     */
    Users findByMail(@Param("mail") String mail);

    /**
     * 非会員を新規登録
     * DBで自動採番されたIDをUsers.idにセットする
     */
    void insert(Users user);
}