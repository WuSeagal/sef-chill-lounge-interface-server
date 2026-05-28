package com.sef.cli.tag.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class TagPropertiesTest {

    @Autowired
    TagProperties props;

    @Test
    void defaults() {
        assertThat(props.getMaxPerUser()).isEqualTo(20);
        assertThat(props.getCustomHoldersThreshold()).isEqualTo(5);
    }
}
