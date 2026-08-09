package de.thm.mni.backend.security

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
class SecurityIntegrationTests(
    @Autowired private val mockMvc: MockMvc
) {
    @Test
    fun `health endpoint is publicly available`() {
        mockMvc.perform(get("/api/health"))
            .andExpect(status().isOk)
    }

    @Test
    fun `mail API rejects requests without access token`() {
        mockMvc.perform(get("/api/mails/drafts"))
            .andExpect(status().isUnauthorized)
    }
}
