package com.tms.backup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tms.testcase.entity.TestCase;
import com.tms.testcase.entity.TestCaseBrowser;
import com.tms.testcase.entity.TestCaseDevice;
import com.tms.testcase.entity.TestCaseOs;
import com.tms.testcase.entity.TestCasePriority;
import com.tms.testcase.entity.TestCaseStatus;
import com.tms.testcase.entity.TestCaseType;
import com.tms.testcase.entity.TestFolder;
import com.tms.testcase.repository.TestCaseRepository;
import com.tms.testcase.repository.TestFolderRepository;
import com.tms.testsuite.entity.TestSuite;
import com.tms.testsuite.repository.TestSuiteRepository;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 전체 백업 → 데이터 소실 → 복구 시나리오를 종단 검증한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@org.springframework.test.context.TestPropertySource(
        properties = "tms.upload.dir=${java.io.tmpdir}/tms-backup-test-uploads")
class BackupRestoreIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired TestCaseRepository testCaseRepository;
    @Autowired TestFolderRepository testFolderRepository;
    @Autowired TestSuiteRepository testSuiteRepository;
    @Autowired org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @Value("${tms.upload.dir}")
    String uploadDir;

    @AfterEach
    void tearDown() throws Exception {
        testSuiteRepository.deleteAll();
        testCaseRepository.deleteAll();
        testFolderRepository.deleteAll();
        Path root = Paths.get(uploadDir);
        if (Files.isDirectory(root)) {
            try (var s = Files.list(root)) {
                for (Path p : s.filter(Files::isRegularFile).toList()) Files.deleteIfExists(p);
            }
        }
    }

    @Test
    void backsUpThenRestoresAllDataAndAttachments() throws Exception {
        // given — 폴더/케이스/스위트 + 첨부파일 1개를 만든다.
        TestFolder folder = testFolderRepository.save(new TestFolder("로그인", null, 7L));
        TestCase a = newCase("로그인 성공", TestCasePriority.HIGH, folder);
        TestCase b = newCase("로그인 실패", TestCasePriority.LOW, folder);
        TestSuite suite = new TestSuite("회귀 스위트", "릴리스 전", List.of(a, b));
        suite.setProjectId(7L);
        suite = testSuiteRepository.save(suite);

        Path root = Paths.get(uploadDir);
        Files.createDirectories(root);
        // 전용 업로드 폴더를 깨끗이 비워 첨부파일 개수를 결정적으로 만든다.
        try (var s = Files.list(root)) {
            for (Path p : s.filter(Files::isRegularFile).toList()) Files.deleteIfExists(p);
        }
        Path attachment = root.resolve("evidence-001.png");
        Files.write(attachment, "FAKE-PNG-BYTES".getBytes());

        long casesBefore = testCaseRepository.count();
        long foldersBefore = testFolderRepository.count();
        long suitesBefore = testSuiteRepository.count();
        assertThat(casesBefore).isEqualTo(2);

        // when 1) 백업 zip 다운로드
        byte[] zip = mockMvc.perform(get("/api/backup/export"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
        assertThat(zip).isNotEmpty();
        System.out.println("[backup] zip 크기: " + zip.length + " bytes");

        // when 2) 데이터가 다 날아간 상황을 재현 — 모든 데이터/첨부 삭제
        testSuiteRepository.deleteAll();
        testCaseRepository.deleteAll();
        testFolderRepository.deleteAll();
        Files.deleteIfExists(attachment);
        assertThat(testCaseRepository.count()).isZero();
        assertThat(Files.exists(attachment)).isFalse();

        // when 3) 백업으로 복구
        MockMultipartFile upload = new MockMultipartFile(
                "file", "tms-backup.zip", "application/zip", zip);
        mockMvc.perform(multipart("/api/backup/import").file(upload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.files").value(1))
                .andExpect(jsonPath("$.rows").value(org.hamcrest.Matchers.greaterThanOrEqualTo(5)))
                .andReturn();

        // then — 데이터와 첨부파일이 그대로 복원됐다.
        assertThat(testCaseRepository.count()).isEqualTo(casesBefore);
        assertThat(testFolderRepository.count()).isEqualTo(foldersBefore);
        assertThat(testSuiteRepository.count()).isEqualTo(suitesBefore);

        TestCase restored = testCaseRepository.findAll().stream()
                .filter(tc -> "로그인 성공".equals(tc.getTitle()))
                .findFirst().orElseThrow();
        assertThat(restored.getPriority()).isEqualTo(TestCasePriority.HIGH);
        assertThat(restored.getStatus()).isEqualTo(TestCaseStatus.READY);
        assertThat(restored.getProjectId()).isEqualTo(7L);

        // 스위트-케이스 다대다 관계도 복원됐는지(조인 테이블 백업/복구 확인)
        Long joinRows = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM test_suite_cases", Long.class);
        assertThat(joinRows).isEqualTo(2L);

        // 첨부파일 실파일 복원
        assertThat(Files.exists(attachment)).isTrue();
        assertThat(Files.readString(attachment)).isEqualTo("FAKE-PNG-BYTES");

        System.out.println("[restore] 케이스 " + testCaseRepository.count()
                + "건 / 스위트-케이스 링크 " + joinRows
                + "건 / 첨부 복원=" + Files.exists(attachment));
    }

    private TestCase newCase(String title, TestCasePriority priority, TestFolder folder) {
        TestCase tc = new TestCase(TestCaseType.FUNCTIONAL, priority, TestCaseStatus.READY, title,
                "설명 " + title, "사전조건", "1) 진입\n2) 확인", "비고",
                TestCaseOs.MAC, TestCaseBrowser.CHROME, TestCaseDevice.MOBILE,
                new ArrayList<>(), null, null, "tester@example.com", "v1.0");
        tc.moveToFolder(folder);
        tc.setProjectId(7L);
        return testCaseRepository.save(tc);
    }
}
