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

	    http
	        .securityMatcher(
	            "/adminLogin",
	            "/adminLogin/**",
	            "/admin/**"
	        )
	        .authenticationProvider(adminAuthenticationProvider)
	        .authorizeHttpRequests(auth -> auth
	            .requestMatchers(
	                "/adminLogin",
	                "/css/**",
	                "/js/**",
	                "/images/**"
	            ).permitAll()
	            .anyRequest().authenticated()
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
	                response.sendError(
	                    HttpServletResponse.SC_FORBIDDEN);
	                return;
	            }

	            response.sendRedirect(
	                request.getContextPath()
	                + "/shops/" + shopAdmin.getStoreId());
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

        http
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
	
	
// 以下元のログイン処理　問題発生時戻し用    
//    //本番用：ログイン処理
//	    @Bean
//	    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
//	
//	        http
//	            // ① どの画面に、ログインが必要か
//	            .authorizeHttpRequests(auth -> auth
//	                // ログインしなくても見られる画面
//	                .requestMatchers(
//	                    "/", "/shops/**",                  // トップ・店舗一覧・検索・店舗詳細
//	                    "/register/**",                    // 会員登録（URLは会員登録の担当と合わせる）
//	                    "/login",                          // ログイン画面
//	                    "/css/**", "/js/**", "/images/**"  // CSS・JavaScript・画像
//	                ).permitAll()
//	                // ログインが必要な画面（会員情報・変更・退会、予約履歴）
//	                .requestMatchers("/members/**", "/reservations/history").authenticated()
//	                // それ以外は、いまは誰でも使えるようにしておく
//	                .anyRequest().permitAll()
//	            )
//	            // ② ログイン：Spring Security 標準のログイン画面を使う
//	            .formLogin(form -> form
//	                .defaultSuccessUrl("/shops", true)//ログインが成功したらトップ画面へ遷移
//	                .permitAll()
//	            )
//	            // ③ ログアウト：POST /logout でログアウトし、トップ画面へ戻る
//	            .logout(logout -> logout
//	                .logoutUrl("/logout")
//	                .logoutSuccessUrl("/shops")
//	                .permitAll()
//	            );
//	
//	        return http.build();
//	    }
//
//    
//   //本番用：ログインできる人の名簿を作成する↓
//  //ログインできる人の名簿：DBの members から、会員IDで会員を探す
//    @Bean
//    UserDetailsService userDetailsService(MembersMapper membersMapper) {
//
//        return username -> {
//	    // ① ログイン画面で入力された会員ID（文字列）を、数字に変換する
//	        int memberId;
//	        try {
//	            memberId = Integer.parseInt(username);
//	        } catch (NumberFormatException e) {
//	            throw new UsernameNotFoundException("会員IDは数字で入力してください：" + username);
//	        }
//	
//	    // ② DBから会員を探す（退会済みは見つからないので、ログインできない）
//	        Members member = membersMapper.findById(memberId);
//	        if (member == null) {
//	            throw new UsernameNotFoundException("会員が見つかりません：" + username);
//	        }
//	
//	    // ③ Spring Security に渡す（パスワードの照合は自動で行われる）
//	        return User.builder()
//	                .username(String.valueOf(member.getId()))//ログインID（principal.getName() で受け取れる値）
//	                .password(member.getPassword())//DBの暗号化されたパスワード
//	                .roles("MEMBER")//役割
//	                .build();
//        };
//    }
//    

}	