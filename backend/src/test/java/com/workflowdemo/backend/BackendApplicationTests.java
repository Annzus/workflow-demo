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
import org.springframework.mock.web.MockMultipartFile;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
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
	void currentUserRequiresAuthentication() throws Exception {
		mockMvc.perform(get("/api/me"))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void currentUserReturnsAuthenticatedDemoEmployee() throws Exception {
		mockMvc.perform(get("/api/me")
				.with(httpBasic("demo1@growtea.co.jp", "demo1001")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.username").value("demo1@growtea.co.jp"))
			.andExpect(jsonPath("$.employeeCode").value("1001"))
			.andExpect(jsonPath("$.name").value("山田 太郎"))
			.andExpect(jsonPath("$.organizationName").value("第１グループ"))
			.andExpect(jsonPath("$.positionName").value("課長"));
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
	void uploadAttachmentRequiresAuthentication() throws Exception {
		String id = createTravelDraft("添付認証確認");
		MockMultipartFile file = new MockMultipartFile(
			"file",
			"estimate.pdf",
			"application/pdf",
			"見積書".getBytes()
		);

		mockMvc.perform(multipart("/api/applications/{id}/attachments", id)
				.file(file))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void uploadAttachmentStoresMetadataAndReturnsList() throws Exception {
		String id = createTravelDraft("添付確認");
		MockMultipartFile file = new MockMultipartFile(
			"file",
			"travel-plan.txt",
			"text/plain",
			"出張計画".getBytes()
		);

		mockMvc.perform(multipart("/api/applications/{id}/attachments", id)
				.file(file)
				.with(httpBasic("demo1@growtea.co.jp", "demo1001")))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.originalFilename").value("travel-plan.txt"))
			.andExpect(jsonPath("$.contentType").value("text/plain"))
			.andExpect(jsonPath("$.sizeBytes").value(12))
			.andExpect(jsonPath("$.uploadedByName").value("山田 太郎"));

		mockMvc.perform(get("/api/applications/{id}/attachments", id)
				.with(httpBasic("demo1@growtea.co.jp", "demo1001")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].originalFilename").value("travel-plan.txt"))
			.andExpect(jsonPath("$[0].uploadedByName").value("山田 太郎"));
	}

	@Test
	void uploadAttachmentRejectsTooLongFilename() throws Exception {
		String id = createTravelDraft("添付ファイル名確認");
		String tooLongFilename = "a".repeat(252) + ".txt";
		MockMultipartFile file = new MockMultipartFile(
			"file",
			tooLongFilename,
			"text/plain",
			"content".getBytes()
		);

		mockMvc.perform(multipart("/api/applications/{id}/attachments", id)
				.file(file)
				.with(httpBasic("demo1@growtea.co.jp", "demo1001")))
			.andExpect(status().isBadRequest());
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

	@Test
	void submitApplicationCreatesPendingApprovalTaskAndHistory() throws Exception {
		String id = submitTravelDraft("承認タスク確認");

		mockMvc.perform(get("/api/approval-tasks/pending")
				.with(httpBasic("demo1@growtea.co.jp", "demo1001")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].applicationId").value(id))
			.andExpect(jsonPath("$[0].title").value("承認タスク確認"))
			.andExpect(jsonPath("$[0].status").value("PENDING"))
			.andExpect(jsonPath("$[0].approverName").value("岩瀬 大樹"));

		mockMvc.perform(get("/api/applications/{id}/history", id)
				.with(httpBasic("demo1@growtea.co.jp", "demo1001")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].action").value("SUBMIT"))
			.andExpect(jsonPath("$[0].actorName").value("山田 太郎"));
	}

	@Test
	void approvePendingTaskChangesApplicationToApproved() throws Exception {
		String applicationId = submitTravelDraft("承認確認");
		String taskId = pendingTaskIdForApplication(applicationId);

		mockMvc.perform(post("/api/approval-tasks/{id}/approve", taskId)
				.with(httpBasic("demo1@growtea.co.jp", "demo1001"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "comment": "内容を確認しました"
					}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.applicationId").value(applicationId))
			.andExpect(jsonPath("$.applicationStatus").value("APPROVED"))
			.andExpect(jsonPath("$.taskStatus").value("APPROVED"))
			.andExpect(jsonPath("$.history.action").value("APPROVE"));

		mockMvc.perform(get("/api/applications/{id}", applicationId)
				.with(httpBasic("demo1@growtea.co.jp", "demo1001")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("APPROVED"));
	}

	@Test
	void rejectPendingTaskChangesApplicationToRejected() throws Exception {
		String applicationId = submitTravelDraft("否認確認");
		String taskId = pendingTaskIdForApplication(applicationId);

		mockMvc.perform(post("/api/approval-tasks/{id}/reject", taskId)
				.with(httpBasic("demo1@growtea.co.jp", "demo1001"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "comment": "差戻し確認"
					}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.applicationId").value(applicationId))
			.andExpect(jsonPath("$.applicationStatus").value("REJECTED"))
			.andExpect(jsonPath("$.taskStatus").value("REJECTED"))
			.andExpect(jsonPath("$.history.action").value("REJECT"));
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

	private String submitTravelDraft(String title) throws Exception {
		String id = createTravelDraft(title);
		mockMvc.perform(post("/api/applications/{id}/submit", id)
				.with(httpBasic("demo1@growtea.co.jp", "demo1001")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("SUBMITTED"));
		return id;
	}

	private String pendingTaskIdForApplication(String applicationId) throws Exception {
		MvcResult result = mockMvc.perform(get("/api/approval-tasks/pending")
				.with(httpBasic("demo1@growtea.co.jp", "demo1001")))
			.andExpect(status().isOk())
			.andReturn();

		JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
		for (JsonNode task : response) {
			if (applicationId.equals(task.get("applicationId").asText())) {
				return task.get("id").asText();
			}
		}
		throw new AssertionError("Pending task not found for application " + applicationId);
	}

}
