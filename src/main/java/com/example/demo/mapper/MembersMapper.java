package com.example.demo.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.example.demo.entity.Members;

@Mapper
public interface MembersMapper {

	boolean existsById(int id);
	
	 // 会員一覧
    List<Members> selectAll();

    // 新規登録
	void insert(Members member);
	
	// 会員情報更新
    void update(Members member);

    // 会員削除
    void delete(@Param("id") Long id);

}
