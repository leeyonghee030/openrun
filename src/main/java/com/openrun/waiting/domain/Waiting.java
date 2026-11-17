package com.openrun.waiting.domain;

import com.openrun.store.domain.Store;
import com.openrun.user.domain.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
// TODO 4주차: UNIQUE(store_id, business_date, queue_number) — 유니크 없이 중복 수 측정 후 추가
@Table(
        name = "waitings",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_waiting_active",
                columnNames = {"user_id", "store_id", "is_active"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Waiting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id",nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id",nullable = false)
    private Store store;

    @Column(nullable = false)
    private LocalDate businessDate;

    @Column(nullable = false)
    private int queueNumber;

    @Column(nullable = false, columnDefinition = "TINYINT")
    private int partySize;

    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false,length = 10)
    private WaitingStatus status;

    @Column(columnDefinition = "TINYINT GENERATED ALWAYS AS (IF(status IN ('WAITING','CALLED'), 1, NULL)) STORED",
            insertable = false, updatable = false)
    private Integer isActive;

    @CreationTimestamp
    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime calledAt;
    private LocalDateTime endedAt;

    public Waiting(User user, Store store, LocalDate businessDate, int queueNumber, int partySize) {
        this.user = user;
        this.store = store;
        this.businessDate = businessDate;
        this.queueNumber = queueNumber;
        this.partySize = partySize;
        this.status = WaitingStatus.WAITING;
    }


}

