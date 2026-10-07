package com.example.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.SecurityFilterChain;

import com.example.demo.entity.Members;
import com.example.demo.mapper.MembersMapper;

@Configuration
@EnableWebSecurity
public class SecurityConfig {//設定用クラス

	//テスト用：ログイン処理
//    @Bean
//    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
//
//        http
//            .authorizeHttpRequests(auth -> auth
//                .requestMatchers("/login").permitAll()
//                .anyRequest().authenticated()
//            )
//            .formLogin(form -> form
//                .defaultSuccessUrl("/shops", true)//ログインが成功したらトップ画面へ遷移
//            );
//
//        return http.build();
//    }
    
    //会員機能（山守専用）テスト用：ログイン後の会員情報を表示するためのテスト設定
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/login").permitAll()
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
            	    .defaultSuccessUrl("/test/top", true)//【テスト用】確認が終わったら "/shops" に戻す
            	    .permitAll()
            );

        return http.build();
    }
  
    
    //本番用：ログイン処理
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
    
    //テスト用：ログインできる人の名簿を作成する↓
//    @Bean
//    UserDetailsService userDetailsService(PasswordEncoder passwordEncoder) {
//
//        UserDetails user = User.builder()
//                .username("test")//ログインID
//                .password(passwordEncoder.encode("test123"))//ログインパスワード
//                .roles("USER")//役割
//                .build();
//
//        return new InMemoryUserDetailsManager(user);
//    }
    
    
   //本番用：ログインできる人の名簿を作成する↓
  //ログインできる人の名簿：DBの members から、会員IDで会員を探す
    @Bean
    UserDetailsService userDetailsService(MembersMapper membersMapper) {

        return username -> {
	    // ① ログイン画面で入力された会員ID（文字列）を、数字に変換する
	        int memberId;
	        try {
	            memberId = Integer.parseInt(username);
	        } catch (NumberFormatException e) {
	            throw new UsernameNotFoundException("会員IDは数字で入力してください：" + username);
	        }
	
	    // ② DBから会員を探す（退会済みは見つからないので、ログインできない）
	        Members member = membersMapper.findById(memberId);
	        if (member == null) {
	            throw new UsernameNotFoundException("会員が見つかりません：" + username);
	        }
	
	    // ③ Spring Security に渡す（パスワードの照合は自動で行われる）
	        return User.builder()
	                .username(String.valueOf(member.getId()))//ログインID（principal.getName() で受け取れる値）
	                .password(member.getPassword())//DBの暗号化されたパスワード
	                .roles("MEMBER")//役割
	                .build();
        };
    }
    
}
