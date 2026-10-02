package com.example.demo.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.example.demo.entity.Member;


@Mapper
public interface MemberMapper {
	 // 会員一覧
    List<Member> selectAll();

    // 新規登録
	void insert(Member member);
	
	// 会員情報更新
    void update(Member member);

    // 会員削除
    void delete(@Param("id") Long id);

}
