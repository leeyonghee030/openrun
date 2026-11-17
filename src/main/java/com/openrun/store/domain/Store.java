package com.openrun.store.domain;

import com.openrun.user.domain.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Table(name = "stores")
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Store {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_user_id",nullable = false,unique = true)
    private User ownerUser;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false, length = 200)
    private String address;

    private LocalTime startTime;
    private LocalTime endTime;

    @Column(nullable = false)
    private LocalTime dayChangeTime;

    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 10)
    @Enumerated(EnumType.STRING)
    private StoreStatus status;

    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 10)
    @Enumerated(EnumType.STRING)
    private AcceptStatus acceptStatus;

    @CreationTimestamp
    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Builder
    public Store(User ownerUser, String name, String address, LocalTime startTime, LocalTime endTime) {
        this.ownerUser = ownerUser;
        this.name = name;
        this.address = address;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = StoreStatus.PENDING;
        this.acceptStatus = AcceptStatus.CLOSED;
        this.dayChangeTime = LocalTime.MIDNIGHT;
    }

    //편의메서드
    //로그인한 유저가 이 가게유저인지
    public boolean isOwner(Long userId) {
        return this.ownerUser.getId().equals(userId);
    }
    // 가입 승인되었는지
    public boolean isApproved () {
        return this.status == StoreStatus.APPROVED;
    }
    // 가게 접수중인지
    public boolean isOpen() {
        return this.acceptStatus == AcceptStatus.OPEN;
    }
}
