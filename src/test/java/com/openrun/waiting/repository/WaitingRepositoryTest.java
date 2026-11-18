package com.openrun.waiting.repository;

import com.openrun.store.domain.Store;
import com.openrun.user.domain.Role;
import com.openrun.user.domain.User;
import com.openrun.waiting.domain.Waiting;
import com.openrun.waiting.domain.WaitingStatus;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class WaitingRepositoryTest {

    @Autowired
    private WaitingRepository waitingRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("Waiting 저장 후 flush 및 clear를 거쳐 다시 조회하면 값이 정확히 일치한다.")
    void saveAndFindWaitingAfterFlushAndClear() {
        // given: 웨이팅 등록에 필요한 유저(User)와 가게(Store) 객체를 먼저 생성하고 영속화합니다.
        User owner = User.builder()
                .loginId("owner123")
                .password("password")
                .phone("010-1234-5678")
                .username("사장님")
                .role(Role.STORE)
                .build();
        entityManager.persist(owner);

        User customer = User.builder()
                .loginId("user123")
                .password("password")
                .phone("010-9876-5432")
                .username("손님")
                .role(Role.USER)
                .build();
        entityManager.persist(customer);

        Store store = Store.builder()
                .ownerUser(owner)
                .name("오픈런 팝업스토어")
                .address("부산 해운대구")
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(20, 0))
                .build();
        entityManager.persist(store);

        // 테스트할 웨이팅 객체 생성 (손님, 가게, 영업일, 대기번호, 인원수)
        Waiting waiting = new Waiting(customer, store, LocalDate.now(), 1, 2);

        // when: 저장 후 영속성 컨텍스트를 초기화하여 DB에서 직접 조회할 준비를 합니다.
        Waiting savedWaiting = waitingRepository.save(waiting);

        entityManager.flush(); // 쓰기 지연 저장소의 쿼리를 실제 DB에 즉시 반영
        entityManager.clear(); // 1차 캐시(영속성 컨텍스트)를 비워 캐시가 아닌 DB 조회를 유도

        // DB에서 ID로 다시 조회
        Waiting foundWaiting = waitingRepository.findById(savedWaiting.getId()).orElseThrow();

        // then: 저장했던 데이터와 다시 조회한 데이터의 값이 정확히 일치하는지 검증
        assertThat(foundWaiting.getId()).isEqualTo(savedWaiting.getId());
        assertThat(foundWaiting.getUser().getId()).isEqualTo(customer.getId());
        assertThat(foundWaiting.getStore().getId()).isEqualTo(store.getId());
        assertThat(foundWaiting.getQueueNumber()).isEqualTo(1);
        assertThat(foundWaiting.getPartySize()).isEqualTo(2);
        assertThat(foundWaiting.getStatus()).isEqualTo(WaitingStatus.WAITING);
        assertThat(foundWaiting.getBusinessDate()).isEqualTo(LocalDate.now());
        // then 부분에 추가할 검증 코드
        assertThat(foundWaiting.getIsActive()).isEqualTo(1);
    }
}