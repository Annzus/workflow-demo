package com.workflowdemo.backend;

import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.workflowdemo.backend.approval.ApprovalTaskRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

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

	private static final String APPLICANT_USERNAME = "demo1@growtea.co.jp";
	private static final String APPLICANT_PASSWORD = "demo1001";
	private static final String APPROVER_USERNAME = "demo5@growtea.co.jp";
	private static final String APPROVER_PASSWORD = "demo1005";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private ApprovalTaskRepository approvalTaskRepository;

	@Test
	void contextLoads() {
	}

	@Test
	void createDraftRejectsMissingRequiredFieldsWithBadRequest() throws Exception {
		mockMvc.perform(post("/api/applications/drafts")
				.with(applicantAuth())
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
				.with(applicantAuth()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.username").value(APPLICANT_USERNAME))
			.andExpect(jsonPath("$.employeeCode").value("1001"))
			.andExpect(jsonPath("$.name").value("山田 太郎"))
			.andExpect(jsonPath("$.organizationName").value("第１グループ"))
			.andExpect(jsonPath("$.positionName").value("課長"));
	}

	@Test
	void currentUserReturnsAuthenticatedDemoApprover() throws Exception {
		mockMvc.perform(get("/api/me")
				.with(approverAuth()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.username").value(APPROVER_USERNAME))
			.andExpect(jsonPath("$.employeeCode").value("1005"))
			.andExpect(jsonPath("$.name").value("岩瀬 大樹"))
			.andExpect(jsonPath("$.organizationName").value("第１ソリューション部"))
			.andExpect(jsonPath("$.positionName").value("部長"));
	}

	@Test
	void applicationsReturnsCreatedDrafts() throws Exception {
		createTravelDraft("一覧表示確認");

		mockMvc.perform(get("/api/applications")
				.with(applicantAuth()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].title").value("一覧表示確認"))
			.andExpect(jsonPath("$[0].status").value("DRAFT"))
			.andExpect(jsonPath("$[0].applicantName").value("山田 太郎"))
			.andExpect(jsonPath("$[0].formName").value("出張申請"));
	}

	@Test
	void applicationsAreScopedToAuthenticatedApplicant() throws Exception {
		createTravelDraft("申請者別一覧確認");

		mockMvc.perform(get("/api/applications")
				.with(approverAuth()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.length()").value(0));
	}

	@Test
	void applicationDetailReturnsFieldValueSnapshot() throws Exception {
		String id = createTravelDraft("詳細表示確認");

		mockMvc.perform(get("/api/applications/{id}", id)
				.with(applicantAuth()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.title").value("詳細表示確認"))
			.andExpect(jsonPath("$.values[0].fieldKey").value("destination"))
			.andExpect(jsonPath("$.values[0].label").value("出張先"))
			.andExpect(jsonPath("$.values[0].value").value("東京本社"))
			.andExpect(jsonPath("$.approvalRoute[0].stepName").value("申請者"))
			.andExpect(jsonPath("$.approvalRoute[0].status").value("CURRENT"))
			.andExpect(jsonPath("$.approvalRoute[1].stepName").value("部長承認"))
			.andExpect(jsonPath("$.approvalRoute[1].status").value("WAITING"))
			.andExpect(jsonPath("$.approvalRoute[2].stepName").value("完了"))
			.andExpect(jsonPath("$.approvalRoute[2].status").value("WAITING"));
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
				.with(applicantAuth()))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.originalFilename").value("travel-plan.txt"))
			.andExpect(jsonPath("$.contentType").value("text/plain"))
			.andExpect(jsonPath("$.sizeBytes").value(12))
			.andExpect(jsonPath("$.uploadedByName").value("山田 太郎"));

		mockMvc.perform(get("/api/applications/{id}/attachments", id)
				.with(applicantAuth()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].originalFilename").value("travel-plan.txt"))
			.andExpect(jsonPath("$[0].uploadedByName").value("山田 太郎"));
	}

	@Test
	void assignedApproverCanReadAttachmentListButCannotUpload() throws Exception {
		String id = submitTravelDraft("添付参照確認");
		MockMultipartFile file = new MockMultipartFile(
			"file",
			"approver-view.txt",
			"text/plain",
			"content".getBytes()
		);

		mockMvc.perform(get("/api/applications/{id}/attachments", id)
				.with(approverAuth()))
			.andExpect(status().isOk());

		mockMvc.perform(multipart("/api/applications/{id}/attachments", id)
				.file(file)
				.with(approverAuth()))
			.andExpect(status().isNotFound());
	}

	@Test
	void assignedApproverCanReadApplicationDetailAndHistory() throws Exception {
		String id = submitTravelDraft("承認者詳細参照確認");

		mockMvc.perform(get("/api/applications/{id}", id)
				.with(approverAuth()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.title").value("承認者詳細参照確認"))
			.andExpect(jsonPath("$.applicantName").value("山田 太郎"))
			.andExpect(jsonPath("$.approvalRoute[1].status").value("CURRENT"));

		mockMvc.perform(get("/api/applications/{id}/history", id)
				.with(approverAuth()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].action").value("SUBMIT"));
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
				.with(applicantAuth()))
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
				.with(applicantAuth()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("SUBMITTED"))
			.andExpect(jsonPath("$.submittedAt").isNotEmpty())
			.andExpect(jsonPath("$.values[0].value").value("東京本社"))
			.andExpect(jsonPath("$.approvalRoute[0].status").value("COMPLETED"))
			.andExpect(jsonPath("$.approvalRoute[1].actorName").value("岩瀬 大樹"))
			.andExpect(jsonPath("$.approvalRoute[1].status").value("CURRENT"))
			.andExpect(jsonPath("$.approvalRoute[2].status").value("WAITING"));
	}

	@Test
	void submitApplicationRejectsAlreadySubmittedApplication() throws Exception {
		String id = createTravelDraft("重複提出確認");
		mockMvc.perform(post("/api/applications/{id}/submit", id)
				.with(applicantAuth()))
			.andExpect(status().isOk());

		mockMvc.perform(post("/api/applications/{id}/submit", id)
				.with(applicantAuth()))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message").value("Only draft applications can be submitted"));
	}

	@Test
	void submitApplicationCreatesPendingApprovalTaskAndHistory() throws Exception {
		String id = submitTravelDraft("承認タスク確認");

		mockMvc.perform(get("/api/approval-tasks/pending")
				.with(applicantAuth()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.length()").value(0));

		mockMvc.perform(get("/api/approval-tasks/pending")
				.with(approverAuth()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].applicationId").value(id))
			.andExpect(jsonPath("$[0].title").value("承認タスク確認"))
			.andExpect(jsonPath("$[0].status").value("PENDING"))
			.andExpect(jsonPath("$[0].approverName").value("岩瀬 大樹"));

		mockMvc.perform(get("/api/applications/{id}/history", id)
				.with(applicantAuth()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].action").value("SUBMIT"))
			.andExpect(jsonPath("$[0].actorName").value("山田 太郎"));
	}

	@Test
	void applicationDetailDoesNotShowCurrentApprovalWhenTaskIsMissing() throws Exception {
		String id = submitTravelDraft("承認タスク欠落確認");
		approvalTaskRepository.deleteAll(approvalTaskRepository.findByApplicationIdOrderByCreatedAtAsc(
			UUID.fromString(id)
		));

		mockMvc.perform(get("/api/applications/{id}", id)
				.with(applicantAuth()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("SUBMITTED"))
			.andExpect(jsonPath("$.approvalRoute[1].status").value("WAITING"))
			.andExpect(jsonPath("$.approvalRoute[2].status").value("WAITING"));
	}

	@Test
	void approvePendingTaskChangesApplicationToApproved() throws Exception {
		String applicationId = submitTravelDraft("承認確認");
		String taskId = pendingTaskIdForApplication(applicationId);

		mockMvc.perform(post("/api/approval-tasks/{id}/approve", taskId)
				.with(approverAuth())
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
				.with(approverAuth()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("APPROVED"))
			.andExpect(jsonPath("$.approvalRoute[0].status").value("COMPLETED"))
			.andExpect(jsonPath("$.approvalRoute[1].status").value("COMPLETED"))
			.andExpect(jsonPath("$.approvalRoute[2].status").value("COMPLETED"));
	}

	@Test
	void applicantCannotCompleteApproverTask() throws Exception {
		String applicationId = submitTravelDraft("申請者承認不可確認");
		String taskId = pendingTaskIdForApplication(applicationId);

		mockMvc.perform(post("/api/approval-tasks/{id}/approve", taskId)
				.with(applicantAuth())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "comment": "申請者は承認できません"
					}
					"""))
			.andExpect(status().isNotFound());
	}

	@Test
	void rejectPendingTaskChangesApplicationToRejected() throws Exception {
		String applicationId = submitTravelDraft("否認確認");
		String taskId = pendingTaskIdForApplication(applicationId);

		mockMvc.perform(post("/api/approval-tasks/{id}/reject", taskId)
				.with(approverAuth())
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

		mockMvc.perform(get("/api/applications/{id}", applicationId)
				.with(approverAuth()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("REJECTED"))
			.andExpect(jsonPath("$.approvalRoute[1].status").value("REJECTED"))
			.andExpect(jsonPath("$.approvalRoute[2].status").value("REJECTED"));
	}

	private String createTravelDraft(String title) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/applications/drafts")
				.with(applicantAuth())
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
				.with(applicantAuth()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("SUBMITTED"));
		return id;
	}

	private String pendingTaskIdForApplication(String applicationId) throws Exception {
		MvcResult result = mockMvc.perform(get("/api/approval-tasks/pending")
				.with(approverAuth()))
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

	private static RequestPostProcessor applicantAuth() {
		return httpBasic(APPLICANT_USERNAME, APPLICANT_PASSWORD);
	}

	private static RequestPostProcessor approverAuth() {
		return httpBasic(APPROVER_USERNAME, APPROVER_PASSWORD);
	}

}
