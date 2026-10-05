package com.example.demo.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.entity.Members;
import com.example.demo.form.MemberEditForm;
import com.example.demo.mapper.MembersMapper;
import com.example.demo.service.serviceInterface.MemberService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MemberServiceImpl implements MemberService{
	
	private final MembersMapper membersMapper;
	private final PasswordEncoder passwordEncorder;//Spring Security：パスワードの暗号化ができるインターフェース
	
	@Override
	//会員のIdをもらって会員情報をとる処理を行う
	public Members selectById( Integer id ) {
		return membersMapper.selectById(id);
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
		Members member = membersMapper.selectById(id);
		if(member == null) {
			throw new IllegalStateException("会員が見つかりません（退会済みの可能性あり）"+ id);
		}
		
		member.setName(form.getName());
		member.setMail(form.getMail());
		member.setBirthDate(form.getBirthDate());
		
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
	
}
