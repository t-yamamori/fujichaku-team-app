package com.example.demo.service;

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

	}

