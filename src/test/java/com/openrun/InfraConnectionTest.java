package com.openrun;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
class InfraConnectionTest {

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    StringRedisTemplate redisTemplate;

    @Test
    void mysql에_연결된다() {
        Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);

        assertThat(result).isEqualTo(1);
    }

    @Test
    void redis에_연결된다() {
        redisTemplate.opsForValue().set("openrun:infra-test", "ok");

        assertThat(redisTemplate.opsForValue().get("openrun:infra-test")).isEqualTo("ok");

        redisTemplate.delete("openrun:infra-test");
    }
}
