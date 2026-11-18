package com.openrun.user.repository;

import com.openrun.user.domain.Role;
import com.openrun.user.domain.User;
import com.openrun.user.domain.UserStatus;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("User 저장 후 flush 및 clear를 거쳐 다시 조회하면 값이 정확히 일치한다.")
    void saveAndFindUserAfterFlushAndClear() {
        // given: 테스트용 유저 객체 생성
        User user = User.builder()
                .loginId("testuser123")
                .password("encodedPassword")
                .phone("010-1111-2222")
                .username("테스트유저")
                .role(Role.USER)
                .build();

        // when: 저장 후 영속성 컨텍스트 초기화 준비
        User savedUser = userRepository.save(user);

        entityManager.flush(); // 쓰기 지연 저장소의 쿼리를 실제 DB에 즉시 반영
        entityManager.clear(); // 1차 캐시를 비워 DB에서 직접 조회하도록 유도

        // DB에서 ID로 다시 조회
        User foundUser = userRepository.findById(savedUser.getId()).orElseThrow();

        // then: 저장했던 데이터와 다시 조회한 데이터의 값이 정확히 일치하는지 검증
        assertThat(foundUser.getId()).isEqualTo(savedUser.getId());
        assertThat(foundUser.getLoginId()).isEqualTo("testuser123");
        assertThat(foundUser.getPhone()).isEqualTo("010-1111-2222");
        assertThat(foundUser.getUsername()).isEqualTo("테스트유저");
        assertThat(foundUser.getRole()).isEqualTo(Role.USER);
        assertThat(foundUser.getStatus()).isEqualTo(UserStatus.ACTIVE);
    }
}