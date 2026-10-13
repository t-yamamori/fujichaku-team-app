package com.example.demo.service;

import java.util.concurrent.ThreadLocalRandom;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.entity.Members;
import com.example.demo.form.MemberEditForm;
import com.example.demo.mapper.MembersMapper;
import com.example.demo.service.serviceInterface.MembersService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MembersServiceImpl implements MembersService{
	
	private final MembersMapper membersMapper;
	private final PasswordEncoder passwordEncorder;//Spring Security：パスワードの暗号化ができるインターフェース
	
	@Override
	//会員のIdをもらって会員情報をとる処理を行う
	public Members findById( Integer id ) {
		return membersMapper.findById(id);
	}
	
	
	@Override //会員情報画面を開いたときに、入力欄へ今の登録内容を
	          //入れておくために使う
	public MemberEditForm toForm(Members member) {
		MemberEditForm form = new MemberEditForm();
		form.setName(member.getName());
		form.setMail(member.getMail());
		form.setBirthDate(member.getBirthDate());
		//パスワードは暗号化されていて元に戻せないので入力欄は空欄のままにする
		return form;
		
		
	}
	
	@Override
	@Transactional
	//特定の会員の情報とフォームに書き込まれた変更フォームをもらい、情報を書き換える
	public void update(Integer id, MemberEditForm form) {
		//今のデータを取得して画面に変更された項目だけを書き換える
		Members member = membersMapper.findById(id);
		if(member == null) {
			throw new IllegalStateException("会員が見つかりません（退会済みの可能性あり）"+ id);
		}
		
		member.setName(form.getName());//MemberEditFormに入っている名前をMembersに格納
		member.setMail(form.getMail());//MemberEditFormに入っているメールをMembersに格納
		member.setBirthDate(form.getBirthDate());//MemberEditFormに入っている生年月日をMembersに格納
		
		//新しいパスワードが入力された時だけ、暗号化して書き換える
		if(form.getPassword()!= null && !form.getPassword().isEmpty()) {
			member.setPassword(passwordEncorder.encode(form.getPassword()));
			
		}
		
		membersMapper.update(member);

		
	}


	@Override
	@Transactional
	//会員IDをもらい、退会処理を行うMapperに渡す
	public void delete(Integer id) {
		membersMapper.delete(id);
		
	}
	
    // ===== ここからポイント機能 =====

    /**
     * ポイントを加算する
     * 「履歴に1行追加」と「合計を増やす」を1セットで行う。
     * @Transactional を付けているので、途中で失敗したら両方とも取り消される。
     */
    @Override
    @Transactional
    public void addPoint(Integer memberId, int points, String reason) {
        membersMapper.insertPointHistory(memberId, points, reason); // 履歴に記録
        membersMapper.addPoint(memberId, points);                   // 合計を増やす
    }

    /** 理由ごとのポイント合計（カードの内訳表示用） */
    @Override
    public int getPointByReason(Integer memberId, String reason) {
        return membersMapper.sumPointsByReason(memberId, reason);
    }

    /** 今日ガチャを回した回数 */
    @Override
    public int getTodayGachaCount(Integer memberId) {
        return membersMapper.countTodayGacha(memberId);
    }

    /**
     * ガチャを1回回す
     * ① 今日の回数が上限なら 0 を返して終わり
     * ② 抽選して順位を決める
     * ③ 順位に応じたポイントを加算する
     */
    @Override
    @Transactional
    public int playGacha(Integer memberId) {
        // ① 回数チェック
        if (getTodayGachaCount(memberId) >= GACHA_LIMIT_PER_DAY) {
            return 0;
        }

        // ② 抽選
        int rank = drawRank();

        // ③ ポイント加算（理由は GACHA）
        addPoint(memberId, getGachaPoint(rank), "GACHA");

        return rank;
    }

    /** 順位 → ポイント */
    @Override
    public int getGachaPoint(int rank) {
        switch (rank) {
            case 1: return 10;
            case 2: return 5;
            case 3: return 3;
            default: return 1;   // 4等
        }
    }

    /**
     * 抽選する（このクラスの中だけで使うので private）
     * 0〜99 の数字をランダムに1つ出して、どの範囲に入ったかで順位を決める
     *   0〜4   （5個）  → 1等  5%
     *   5〜19  （15個） → 2等 15%
     *   20〜49 （30個） → 3等 30%
     *   50〜99 （50個） → 4等 50%
     */
    private int drawRank() {
        int number = ThreadLocalRandom.current().nextInt(100); // 0〜99
        if (number < 5) {
            return 1;
        } else if (number < 20) {
            return 2;
        } else if (number < 50) {
            return 3;
        } else {
            return 4;
        }
    }


	/*
	 * 会員を探す際に使用するメソッド
	 */
	@Override //文字列の会員IDを数字にして、会員を探す
	public Members findByLoginId(String loginId) {
	    try {
	        return membersMapper.findById(Integer.valueOf(loginId));
	        
	    } catch (NumberFormatException e) {
	        // ログインIDが数字でなかったとき
	        return null;
	    }
	}

	
}
