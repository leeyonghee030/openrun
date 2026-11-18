package com.openrun.store.repository;

import com.openrun.store.domain.AcceptStatus;
import com.openrun.store.domain.Store;
import com.openrun.store.domain.StoreStatus;
import com.openrun.user.domain.Role;
import com.openrun.user.domain.User;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class StoreRepositoryTest {

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("Store 저장 후 flush 및 clear를 거쳐 다시 조회하면 값이 정확히 일치한다.")
    void saveAndFindStoreAfterFlushAndClear() {
        // given: Store는 ownerUser(사장님) 외래키가 있으므로 유저를 먼저 생성하고 영속화합니다.
        User owner = User.builder()
                .loginId("ownerUser1")
                .password("password")
                .phone("010-5555-6666")
                .username("가게사장님")
                .role(Role.STORE)
                .build();
        entityManager.persist(owner);

        Store store = Store.builder()
                .ownerUser(owner)
                .name("오픈런 테스트 팝업")
                .address("부산 서면")
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(21, 0))
                .build();

        // when: 저장 후 영속성 컨텍스트 초기화 준비
        Store savedStore = storeRepository.save(store);

        entityManager.flush(); // 쓰기 지연 저장소의 쿼리를 실제 DB에 즉시 반영
        entityManager.clear(); // 1차 캐시를 비워 DB에서 직접 조회하도록 유도

        // DB에서 ID로 다시 조회
        Store foundStore = storeRepository.findById(savedStore.getId()).orElseThrow();

        // then: 저장했던 데이터와 다시 조회한 데이터의 값이 정확히 일치하는지 검증
        assertThat(foundStore.getId()).isEqualTo(savedStore.getId());
        assertThat(foundStore.getName()).isEqualTo("오픈런 테스트 팝업");
        assertThat(foundStore.getAddress()).isEqualTo("부산 서면");
        assertThat(foundStore.getOwnerUser().getId()).isEqualTo(owner.getId());
        assertThat(foundStore.getStatus()).isEqualTo(StoreStatus.PENDING); // 생성자 기본값 검증
        assertThat(foundStore.getAcceptStatus()).isEqualTo(AcceptStatus.CLOSED); // 생성자 기본값 검증
    }
}