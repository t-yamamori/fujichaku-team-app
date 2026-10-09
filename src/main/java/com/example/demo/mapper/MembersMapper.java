package com.example.demo.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.example.demo.entity.Members;

@Mapper
public interface MembersMapper {
	// 指定IDの会員が存在するか（退会済みは除く）
	boolean existsById(int id);
	
	// 会員を1件取得する（退会済みは除く）
	Members findById(@Param("id") int id);
	
	// 会員一覧　更新画面と同じ
    List<Members> selectAll();
    
    // メールアドレスが登録済みか確認
    boolean existsByMail(@Param("mail") String mail);

    // 新規登録　RegisterFormから
	void insert(Members member);
	
	// 会員情報更新
    void update(Members member);

    // 会員削除
    void delete(@Param("id") int id);
    
    
    
    // ===== ここからポイント機能 =====

    /** ポイント履歴を1行追加する */
    void insertPointHistory(@Param("memberId") Integer memberId,
                            @Param("points") int points,
                            @Param("reason") String reason);

    /** 会員の合計ポイントを増やす */
    void addPoint(@Param("id") Integer id,
                  @Param("points") int points);

    /** 理由（GACHA / REVIEW / RESERVATION）ごとのポイント合計を取得する */
    int sumPointsByReason(@Param("memberId") Integer memberId,
                          @Param("reason") String reason);

    /** 今日ガチャを回した回数を取得する */
    int countTodayGacha(@Param("memberId") Integer memberId);

}
