package com.example.demo.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;

import com.example.demo.entity.Members;
import com.example.demo.mapper.MembersMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminMemberService {
	private final MembersMapper membersMapper;

	// 会員一覧取得
	public List<Members> findAll() {
		return membersMapper.selectAll();
	}	
		
    // 会員情報を1件取得
	public Members findById(int id) {
		 return membersMapper.findById(id);
		}
	
	
	public void update(Integer id, String name, LocalDate birthDate, String mail) {

	    // 存在しない(または削除済みの)会員は更新しない
	    if (!membersMapper.existsById(id)) {
	        throw new IllegalArgumentException("会員が見つかりません: " + id);
	    }

	    Members member = new Members();
	    member.setId(id);
	    member.setName(name);
	    member.setBirthDate(birthDate);
	    member.setMail(mail);

	    membersMapper.update(member);
	}
	
	public void delete(Integer id) {

	    // 存在しない(または削除済みの)会員は削除しない
	    if (!membersMapper.existsById(id)) {
	        throw new IllegalArgumentException("会員が見つかりません: " + id);
	    }

	    membersMapper.delete(id);
	}

	}

