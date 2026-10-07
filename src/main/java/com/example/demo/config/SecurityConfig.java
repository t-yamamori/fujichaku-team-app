package com.example.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

import com.example.demo.mapper.MembersMapper;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /*
     * =========================================================
     * ログイン方式の切り替え
     * =========================================================
     *
     * true
     *  → テスト用ログイン
     *     ID       : test
     *     password : test123
     *
     * false
     *  → 本番用ログイン
     *     membersテーブルから会員IDを検索
     *
     * 今はデザイン確認・動作確認が終わったばかりなので
     * true にしておく。
     *
     * 本番ログインを確認するときは false に変更する。
     *
     */
    public static final boolean TEST_MODE = true;


    /*
     * =========================================================
     * Spring Security設定
     * =========================================================
     */
    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http) throws Exception {

        http
            .authorizeHttpRequests(auth -> auth

                // ログイン画面
                .requestMatchers("/login").permitAll()

                // その他はログイン必須
                .anyRequest().authenticated()
            )

            // ログイン
            .formLogin(form -> form
                .defaultSuccessUrl("/shops", true)
                .permitAll()
            )

            // ログアウト
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/shops")
                .permitAll()
            );

        return http.build();
    }


    /*
     * =========================================================
     * UserDetailsService
     * =========================================================
     *
     * TEST_MODE = true
     * → testユーザーを使用
     *
     * TEST_MODE = false
     * → DBのMembersMapperを使用
     *
     */
    @Bean
    UserDetailsService userDetailsService(
            PasswordEncoder passwordEncoder,
            MembersMapper membersMapper) {

        /*
         * -----------------------------------------------------
         * テスト用
         * -----------------------------------------------------
         */
        if (TEST_MODE) {

            UserDetails user = User.builder()
                    .username("test")
                    .password(
                        passwordEncoder.encode("test123")
                    )
                    .roles("USER")
                    .build();

            return new InMemoryUserDetailsManager(user);
        }


        /*
         * -----------------------------------------------------
         * 本番用
         * -----------------------------------------------------
         *
         * ログイン画面で入力された会員IDを
         * membersテーブルから検索する。
         *
         */
        return username -> {

            int memberId;

            try {

                memberId = Integer.parseInt(username);

            } catch (NumberFormatException e) {

                throw new org.springframework.security.core.userdetails.UsernameNotFoundException(
                    "会員IDは数字で入力してください：" + username
                );
            }


            // DBから会員を取得
            com.example.demo.entity.Members member =
                    membersMapper.findById(memberId);


            // 会員が存在しない
            if (member == null) {

                throw new org.springframework.security.core.userdetails.UsernameNotFoundException(
                    "会員が見つかりません：" + username
                );
            }


            // Spring Securityへ会員情報を渡す
            return User.builder()
                    .username(
                        String.valueOf(member.getId())
                    )
                    .password(
                        member.getPassword()
                    )
                    .roles("MEMBER")
                    .build();
        };
    }
}
