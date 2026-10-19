
package com.example.demo.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.example.demo.entity.Stores;

@Mapper
public interface TenantMapper {

    // 全テナント取得
    List<Stores> findAll();

    // テナント更新
    void update(Stores store);

    // テナント論理削除
    void deleteByIds(List<Integer> ids);

    // テナント新規登録
    void create(Stores store);

    // 名前・ふりがな・メールアドレスがすべて一致する店舗を確認
    boolean existsSameStore(
            @Param("name") String name,
            @Param("nameKana") String nameKana,
            @Param("mail") String mail);

    // メールアドレスが一致する店舗を確認
    boolean existsMail(@Param("mail") String mail);

    // 指定ID群を除外して、同じ店舗が存在するか確認
    boolean existsSameStoreExcludingIds(
            @Param("name") String name,
            @Param("nameKana") String nameKana,
            @Param("mail") String mail,
            @Param("excludedIds") List<Integer> excludedIds);

    // 指定ID群を除外して、メールアドレスが存在するか確認
    boolean existsMailExcludingIds(
            @Param("mail") String mail,
            @Param("excludedIds") List<Integer> excludedIds);
}
