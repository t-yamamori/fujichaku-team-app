package com.example.demo.config;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository; // ★追加

import com.example.demo.entity.Members;
import com.example.demo.entity.ShopAdmin;
import com.example.demo.mapper.AdminMapper;
import com.example.demo.mapper.MembersMapper;
import com.example.demo.mapper.ShopAdminMapper;


@Configuration
@EnableWebSecurity
public class SecurityConfig {

	@Bean
	DaoAuthenticationProvider adminAuthenticationProvider(
	        @Qualifier("adminUserDetailsService")
	        UserDetailsService userDetailsService,
	        PasswordEncoder passwordEncoder) {

	    DaoAuthenticationProvider provider =
	            new DaoAuthenticationProvider(userDetailsService);

	    provider.setPasswordEncoder(passwordEncoder);

	    return provider;
	}

	@Bean
	DaoAuthenticationProvider memberAuthenticationProvider(
	        @Qualifier("memberUserDetailsService")
	        UserDetailsService userDetailsService,
	        PasswordEncoder passwordEncoder) {

	    DaoAuthenticationProvider provider =
	            new DaoAuthenticationProvider(userDetailsService);

	    provider.setPasswordEncoder(passwordEncoder);

	    return provider;
	}

    /*
     * ==========================================
     * 管理者側のログイン
     * ==========================================
     */
	@Bean
	@Order(1)
	SecurityFilterChain adminSecurityFilterChain(
	        HttpSecurity http,
	        DaoAuthenticationProvider adminAuthenticationProvider,
	        AuthenticationSuccessHandler adminLoginSuccessHandler)
	        throws Exception {

	    // ★追加：管理者のログイン状態をセッション内の専用キーに保存する
	    HttpSessionSecurityContextRepository adminContextRepository =
	            new HttpSessionSecurityContextRepository();
	    adminContextRepository.setSpringSecurityContextKey("ADMIN_SECURITY_CONTEXT");

	    http
	        .securityMatcher(
	        		"/adminLogin",
	        		"/adminLogin/**",
	        		"/admin/**",
	        		"/shop-admin/**"
	        )
	        .securityContext(context -> context                         // ★追加
	            .securityContextRepository(adminContextRepository)
	        )
	        .authenticationProvider(adminAuthenticationProvider)
	        .authorizeHttpRequests(auth -> auth
	            .requestMatchers(
	                "/adminLogin",
	                "/css/**",
	                "/js/**",
	                "/images/**"
	            ).permitAll()
	            .anyRequest().hasAnyRole("ADMIN", "SHOP_ADMIN")        // ★変更：authenticated() から
	        )
	        .formLogin(form -> form
	            .loginPage("/adminLogin")
	            .loginProcessingUrl("/adminLogin")
	            .successHandler(adminLoginSuccessHandler)
	            .permitAll()
	        )
	        .logout(logout -> logout
	            .logoutUrl("/adminLogout")
	            .logoutSuccessUrl("/adminLogin")
	            .permitAll()
	        );

	    return http.build();
	}


	@Bean
	AuthenticationSuccessHandler adminLoginSuccessHandler(
	        ShopAdminMapper shopAdminMapper) {

	    return (request, response, authentication) -> {

	        // 通常の管理者：admins テーブル
	        boolean isAdmin = authentication.getAuthorities().stream()
	                .anyMatch(authority ->
	                    authority.getAuthority().equals("ROLE_ADMIN"));

	        if (isAdmin) {
	            // 管理者は従来どおり管理者TOPへ
	            response.sendRedirect(
	                request.getContextPath() + "/admin/index");
	            return;
	        }

	     // 店舗管理者：shop_admins テーブル
	        boolean isShopAdmin = authentication.getAuthorities().stream()
	                .anyMatch(authority ->
	                    authority.getAuthority().equals("ROLE_SHOP_ADMIN"));

	        if (isShopAdmin) {

	            int shopAdminId =
	                    Integer.parseInt(authentication.getName());

	            ShopAdmin shopAdmin =
	                    shopAdminMapper.findById(shopAdminId);

	            if (shopAdmin == null) {
	                response.sendError(HttpServletResponse.SC_FORBIDDEN);
	                return;
	            }

	            // store_id が未設定の場合のエラー処理
	            Integer storeId = shopAdmin.getStoreId();

	            if (storeId == null) {
	                response.sendError(
	                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
	                    "店舗管理者に store_id が設定されていません。"
	                );
	                return;
	            }

	            // 店舗管理画面へ遷移
	            response.sendRedirect(
	                request.getContextPath() + "/shop-admin/" + storeId);
	            return;
	        }

	        // 想定外の権限
	        response.sendError(HttpServletResponse.SC_FORBIDDEN);
	    };
	}

    /*
     * ==========================================
     * 会員側のログイン
     * ==========================================
     */
	@Bean
	@Order(2)
	SecurityFilterChain memberSecurityFilterChain(
	        HttpSecurity http,
	        DaoAuthenticationProvider memberAuthenticationProvider)
	        throws Exception {

        // ★追加：会員のログイン状態をセッション内の専用キーに保存する
        HttpSessionSecurityContextRepository memberContextRepository =
                new HttpSessionSecurityContextRepository();
        memberContextRepository.setSpringSecurityContextKey("MEMBER_SECURITY_CONTEXT");

        http
            .securityContext(context -> context                         // ★追加
                .securityContextRepository(memberContextRepository)
            )
            .authenticationProvider(memberAuthenticationProvider)
            .authorizeHttpRequests(auth -> auth

                .requestMatchers(
                    "/",
                    "/shops/**",
                    "/register/**",
                    "/login",
                    "/css/**",
                    "/js/**",
                    "/images/**"
                ).permitAll()

                .requestMatchers(
                    "/members/**",
                    "/reservations/history"
                ).authenticated()

                .anyRequest().permitAll()
            )

            .formLogin(form -> form
//                .loginPage("/login")
                .defaultSuccessUrl("/shops", true)
                .permitAll()
            )

            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/shops")
                .permitAll()
            );

        return http.build();
    }


    /*
     * ==========================================
     * 会員の認証
     * ==========================================
     */
    @Bean
    UserDetailsService memberUserDetailsService(
            MembersMapper membersMapper) {

        return username -> {

            int memberId;

            try {
                memberId = Integer.parseInt(username);

            } catch (NumberFormatException e) {

                throw new UsernameNotFoundException(
                    "会員IDは数字で入力してください：" + username
                );
            }

            Members member =
                    membersMapper.findById(memberId);

            if (member == null) {

                throw new UsernameNotFoundException(
                    "会員が見つかりません：" + username
                );
            }

            return User.builder()
                    .username(String.valueOf(member.getId()))
                    .password(member.getPassword())
                    .roles("MEMBER")
                    .build();
        };
    }


    /*
     * ==========================================
     * 管理者の認証
     *
     * admins と shop_admins の両方から探す
     * ==========================================
     */
    @Bean
    UserDetailsService adminUserDetailsService(
            AdminMapper adminMapper,
            ShopAdminMapper shopAdminMapper) {

        return username -> {

            int id;

            try {

                id = Integer.parseInt(username);

            } catch (NumberFormatException e) {

                throw new UsernameNotFoundException(
                    "管理者IDは数字で入力してください：" + username
                );
            }


            /*
             * まず admins を検索
             */
            var admin = adminMapper.findById(id);

            if (admin != null) {

                return User.builder()
                        .username(String.valueOf(admin.getId()))
                        .password(admin.getPassword())
                        .roles("ADMIN")
                        .build();
            }


            /*
             * admins にいなければ
             * shop_admins を検索
             */
            ShopAdmin shopAdmin =
                    shopAdminMapper.findById(id);

            if (shopAdmin != null) {

                return User.builder()
                        .username(String.valueOf(shopAdmin.getId()))
                        .password(shopAdmin.getPassword())
                        .roles("SHOP_ADMIN")
                        .build();
            }


            /*
             * どちらにも存在しない
             */
            throw new UsernameNotFoundException(
                "管理者が見つかりません：" + username
            );
        };
    }
}
