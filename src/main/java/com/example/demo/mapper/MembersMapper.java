package com.example.demo.mapper;

import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MembersMapper {

	boolean existsById(int id);

}