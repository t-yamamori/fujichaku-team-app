package com.example.demo.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.example.demo.entity.Reservations;

@Mapper
public interface ReservationMapper {
	//予約登録
	void insertReservation(Reservations reservation);
	
	//予約番号取得
	Reservations findById(@Param("reservationId") int reservationId);
	
	//予約履歴を取得
	List<Reservations> findByMemberId(@Param("memberId") int memberId);

}
