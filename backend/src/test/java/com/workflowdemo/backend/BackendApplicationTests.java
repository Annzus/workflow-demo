package com.workflowdemo.backend;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BackendApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void contextLoads() {
	}

	@Test
	void createDraftRejectsMissingRequiredFieldsWithBadRequest() throws Exception {
		mockMvc.perform(post("/api/applications/drafts")
				.with(httpBasic("demo1@growtea.co.jp", "demo1001"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "formCode": "TRAVEL",
					  "title": "必須チェック確認",
					  "values": {
					    "destination": "東京本社"
					  }
					}
					"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message").value("開始日 is required"));
	}

	@Test
	void createDraftRequiresAuthentication() throws Exception {
		mockMvc.perform(post("/api/applications/drafts")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "formCode": "TRAVEL",
					  "title": "認証チェック",
					  "values": {
					    "destination": "東京本社",
					    "start_date": "2026-06-10",
					    "end_date": "2026-06-11",
					    "purpose": "打ち合わせ"
					  }
					}
					"""))
			.andExpect(status().isUnauthorized());
	}

}
