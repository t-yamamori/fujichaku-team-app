package com.example.demo.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.example.demo.entity.Stores;

@Mapper
public interface StoresMapper {

    // 店舗一覧
    List<Stores> selectList();

    // 店舗名検索（既存：他の画面で使っている可能性があるので残しておく）
    List<Stores> selectByName(String name);

    // 【追加】店舗名・読みかなで検索（ひらがな・カタカナ・漢字・英字に対応）
    //   name    : 入力そのまま（LIKEエスケープ済み）→ stores.name と比較
    //   kana    : 入力をひらがなに揃えたもの        → stores.name_kana と比較
    //   reading : 入力の漢字を読みに変換したもの    → stores.name_kana と比較
    List<Stores> selectByKeyword(@Param("name") String name,
                                 @Param("kana") String kana,
                                 @Param("reading") String reading);

    // 店舗詳細
    Stores selectDetailByName(String name);

    // 店舗IDから取得

    Stores selectById(int id);

}