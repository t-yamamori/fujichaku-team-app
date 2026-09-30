package com.example.demo.controller;

@Controller
@RequestMapping("/shops/{shopId}/reservations")
public class ReservationController {
	@GetMapping("/newreserve")
    public String showReservation(@PathVariable Long shopId) {

        return "reservations/newreserve";
    }
}
