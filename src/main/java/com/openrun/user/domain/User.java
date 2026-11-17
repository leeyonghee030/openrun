package com.openrun.user.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;


import java.time.LocalDate;
import java.time.LocalDateTime;

@Table(name = "users")
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {
    private static final int BAN_NO_SHOW_COUNT = 5;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, length = 30)
    private String loginId;

    @Column(length = 100)
    private String password;

    @Column(unique = true, length = 20)
    private String phone;

    @Column(length = 30)
    private String username;

    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Provider provider;

    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Role role;

    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private UserStatus status;

    @Column(nullable = false)
    private int consecutiveNoShowCount;

    private LocalDate bannedUntil;

    @Column(nullable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    private LocalDateTime withdrawnAt;

    @Builder
    public User(String loginId, String password, String phone, String username, Role role) {
        this.loginId = loginId;
        this.password = password;
        this.phone = phone;
        this.username = username;
        this.provider = Provider.LOCAL;
        this.role = role;
        this.status = UserStatus.ACTIVE;
        this.consecutiveNoShowCount = 0;
    }

    //편의메서드

    //로그인한 유저가 본인 아이디맞나
    public boolean isOwner(Long userId) {
        return this.id.equals(userId);
    }

    // 탈퇴안한 유저인지
    public boolean isActive() {
        return this.status == UserStatus.WITHDRAWN;
    }

    //가게 유저인지
    public boolean isStore() {
        return this.role == Role.STORE;
    }

    //관리자인지
    public boolean isAdmin() {
        return this.role == Role.ADMIN;
    }


    //연속 노쇼 체크

    //연속 노쇼 체크: 이번 노쇼로 제재 받을 횟수가 됐나
    public boolean reachedBanCount() {
        return this.consecutiveNoShowCount >= BAN_NO_SHOW_COUNT;
    }

    //제제 종료일 체크
    public boolean isBanned(LocalDate today) {
        return this.bannedUntil != null && !today.isAfter(this.bannedUntil);
    }


}
