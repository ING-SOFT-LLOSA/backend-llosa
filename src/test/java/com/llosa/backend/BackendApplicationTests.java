package com.llosa.backend;

import com.llosa.backend.config.FirebaseConfig;
import com.llosa.backend.config.PostgresTestContainerConfig;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

@Disabled
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@Import(PostgresTestContainerConfig.class)
class BackendApplicationTests {

    @MockitoBean
    FirebaseConfig firebaseConfig;

    @Test
    void contextLoads() {
        // No test, just to make sure the app starts
    }
}
