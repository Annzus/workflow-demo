package com.workflowdemo.backend;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BackendApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

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

	@Test
	void applicationsRequireAuthentication() throws Exception {
		mockMvc.perform(get("/api/applications"))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void applicationsReturnsCreatedDrafts() throws Exception {
		createTravelDraft("一覧表示確認");

		mockMvc.perform(get("/api/applications")
				.with(httpBasic("demo1@growtea.co.jp", "demo1001")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].title").value("一覧表示確認"))
			.andExpect(jsonPath("$[0].status").value("DRAFT"))
			.andExpect(jsonPath("$[0].applicantName").value("山田 太郎"))
			.andExpect(jsonPath("$[0].formName").value("出張申請"));
	}

	@Test
	void applicationDetailReturnsFieldValueSnapshot() throws Exception {
		String id = createTravelDraft("詳細表示確認");

		mockMvc.perform(get("/api/applications/{id}", id)
				.with(httpBasic("demo1@growtea.co.jp", "demo1001")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.title").value("詳細表示確認"))
			.andExpect(jsonPath("$.values[0].fieldKey").value("destination"))
			.andExpect(jsonPath("$.values[0].label").value("出張先"))
			.andExpect(jsonPath("$.values[0].value").value("東京本社"));
	}

	@Test
	void submitApplicationRequiresAuthentication() throws Exception {
		String id = createTravelDraft("提出認証確認");

		mockMvc.perform(post("/api/applications/{id}/submit", id))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void submitApplicationChangesDraftToSubmitted() throws Exception {
		String id = createTravelDraft("提出確認");

		mockMvc.perform(post("/api/applications/{id}/submit", id)
				.with(httpBasic("demo1@growtea.co.jp", "demo1001")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("SUBMITTED"))
			.andExpect(jsonPath("$.submittedAt").isNotEmpty())
			.andExpect(jsonPath("$.values[0].value").value("東京本社"));
	}

	@Test
	void submitApplicationRejectsAlreadySubmittedApplication() throws Exception {
		String id = createTravelDraft("重複提出確認");
		mockMvc.perform(post("/api/applications/{id}/submit", id)
				.with(httpBasic("demo1@growtea.co.jp", "demo1001")))
			.andExpect(status().isOk());

		mockMvc.perform(post("/api/applications/{id}/submit", id)
				.with(httpBasic("demo1@growtea.co.jp", "demo1001")))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message").value("Only draft applications can be submitted"));
	}

	private String createTravelDraft(String title) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/applications/drafts")
				.with(httpBasic("demo1@growtea.co.jp", "demo1001"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "formCode": "TRAVEL",
					  "title": "%s",
					  "values": {
					    "destination": "東京本社",
					    "start_date": "2026-06-10",
					    "end_date": "2026-06-11",
					    "purpose": "打ち合わせ"
					  }
					}
					""".formatted(title)))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.id").isNotEmpty())
			.andExpect(jsonPath("$.applicationNumber").isNotEmpty())
			.andReturn();

		JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
		return response.get("id").asText();
	}

}
