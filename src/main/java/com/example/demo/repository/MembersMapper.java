package com.example.demo.repository;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MembersMapper {
   
	List<Integer> selectAllIds();

}
