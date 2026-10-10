package com.example.demo.service.serviceInterface;

import com.example.demo.entity.Members;
import com.example.demo.form.MemberEditForm;


/**
 * 会員情報に関する処理の「窓口」となるインターフェース
 * 何ができるか（メソッドの名前と引数・戻り値）だけを決める。
 * 実際の処理は MemberServiceImpl に書く。
 */
   public interface MembersService {
 
    /**
     * IDで会員を1件取得する
     * @param id 会員ID
     * @return 会員（退会済み、または存在しない場合は null）
     */
    Members findById(Integer id);
 
    /**
     * 会員情報画面の入力欄に入れるために、Member を MemberForm に変換する
     * @param member 会員
     * @return 入力欄用のフォーム（パスワードは空欄）
     */
    MemberEditForm toForm(Members member);
 
    /**
     * 会員情報を更新する（「変更」ボタン）
     * パスワードが空欄なら、パスワードは変更しない。
     * メールアドレスが他の会員と重複した場合は、DuplicateKeyException が発生する。
     * @param id 会員ID
     * @param form 画面で入力された内容
     */
    void update(Integer id, MemberEditForm form);
 
    /**
     * 退会する（退会確認の「はい」ボタン）
     * データは消さず、is_deleted を true にする。
     * @param id 会員ID
     */
    void delete(Integer id);
    
    
    //文字列の会員IDを数字にして、会員を探す
	public Members findByLoginId(String loginId);
	
	
    // ===== ここからポイント機能 =====

    /** ガチャを1日に回せる回数 */
    int GACHA_LIMIT_PER_DAY = 5;

    /** ★追加：口コミ投稿でもらえるポイント */
    int REVIEW_POINT = 5;

    /** ★追加：店舗予約でもらえるポイント */
    int RESERVATION_POINT = 10;

    /**
     * ポイントを加算する（履歴に1行追加 ＋ members.point を増やす）
     * 口コミ・予約の担当者にも、この1行を呼んでもらう
     * @param memberId 会員ID
     * @param points   加算するポイント
     * @param reason   理由（"GACHA" / "REVIEW" / "RESERVATION"）
     */
    void addPoint(Integer memberId, int points, String reason);

    /**
     * 理由ごとのポイント合計を取得する（カードの内訳表示用）
     * @return 合計ポイント（履歴がなければ 0）
     */
    int getPointByReason(Integer memberId, String reason);

    /**
     * 今日ガチャを回した回数を取得する
     * @return 今日の回数（0〜5）
     */
    int getTodayGachaCount(Integer memberId);

    /**
     * ガチャを1回回す（回数チェック → 抽選 → ポイント加算）
     * @return 当たった順位（1〜4）。今日の上限に達していたら 0
     */
    int playGacha(Integer memberId);

    /**
     * 順位からもらえるポイントを返す（1等→10P など）
     * @param rank 順位（1〜4）
     * @return ポイント
     */
    int getGachaPoint(int rank);

}
