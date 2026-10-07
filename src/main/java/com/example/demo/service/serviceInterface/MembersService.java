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

}
