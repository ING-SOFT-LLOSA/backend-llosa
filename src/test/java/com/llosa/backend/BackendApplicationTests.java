package com.llosa.backend;

import com.llosa.backend.config.FirebaseConfig;
import com.llosa.backend.config.PostgresTestContainerConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@Import(PostgresTestContainerConfig.class)
class BackendApplicationTests {

    @MockBean
    FirebaseConfig firebaseConfig;

    @Test
    void contextLoads() {
    }
}
