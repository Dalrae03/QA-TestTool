package com.tms.excel.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/**
 * 테스트 스위트 → 엑셀(.xlsx) 내보내기를 HTTP 다운로드부터 워크북 파싱까지 종단 검증한다.
 * 생성된 파일을 target/exported-suite.xlsx 로도 저장해 실제 결과물을 확인할 수 있게 한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ExcelExportControllerIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired TestCaseRepository testCaseRepository;
    @Autowired TestFolderRepository testFolderRepository;
    @Autowired TestSuiteRepository testSuiteRepository;

    @AfterEach
    void tearDown() {
        testSuiteRepository.deleteAll();
        testCaseRepository.deleteAll();
        testFolderRepository.deleteAll();
    }

    @Test
    void exportsSuiteAsExcelGroupedByFolder() throws Exception {
        // given — 폴더 2개, 케이스 3개, 그 케이스를 담은 스위트 1개
        TestFolder loginFolder = testFolderRepository.save(new TestFolder("로그인", null, 7L));
        TestFolder payFolder = testFolderRepository.save(new TestFolder("결제", null, 7L));

        TestCase loginOk = newCase("로그인 성공", "정상 계정 로그인", TestCasePriority.HIGH,
                TestCaseStatus.READY, loginFolder);
        TestCase loginFail = newCase("로그인 실패", "잘못된 비밀번호 입력", TestCasePriority.LOW,
                TestCaseStatus.DRAFT, loginFolder);
        TestCase pay = newCase("카드 결제", "신용카드 결제 흐름", TestCasePriority.MEDIUM,
                TestCaseStatus.READY, payFolder);

        TestSuite suite = new TestSuite("회귀 스위트", "릴리스 전 회귀", List.of(loginOk, loginFail, pay));
        suite.setProjectId(7L);
        suite = testSuiteRepository.save(suite);

        // when — 다운로드 엔드포인트 호출
        MvcResult result = mockMvc.perform(
                        MockMvcRequestBuilders.get("/api/suites/{id}/export/excel", suite.getId()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type",
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andExpect(header().string("Content-Disposition",
                        org.hamcrest.Matchers.containsString("attachment")))
                .andReturn();

        byte[] xlsx = result.getResponse().getContentAsByteArray();
        assertThat(xlsx).isNotEmpty();

        // 실제 결과물을 확인할 수 있도록 파일로도 떨군다.
        Path saved = Path.of("target", "exported-suite.xlsx");
        Files.write(saved, xlsx);
        System.out.println("[export-test] 저장 위치: " + saved.toAbsolutePath() + " (" + xlsx.length + " bytes)");

        // then — 워크북 파싱: 폴더별 시트 + 헤더 + 행
        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(xlsx))) {
            assertThat(wb.getNumberOfSheets()).isEqualTo(2);
            assertThat(sheetNames(wb)).containsExactlyInAnyOrder("로그인", "결제");

            Sheet login = wb.getSheet("로그인");
            assertThat(cell(login, 0, 0)).isEqualTo("제목");
            assertThat(cell(login, 0, 6)).isEqualTo("우선순위");
            assertThat(cell(login, 1, 0)).isEqualTo("로그인 성공");
            assertThat(cell(login, 1, 6)).isEqualTo("HIGH");
            assertThat(cell(login, 1, 7)).isEqualTo("READY");
            assertThat(cell(login, 2, 0)).isEqualTo("로그인 실패");
            // 헤더(1) + 케이스(2) = 3행
            assertThat(login.getLastRowNum()).isEqualTo(2);

            Sheet paySheet = wb.getSheet("결제");
            assertThat(cell(paySheet, 1, 0)).isEqualTo("카드 결제");
            assertThat(cell(paySheet, 1, 11)).isEqualTo("MOBILE"); // device

            // 콘솔로 내용 전체를 덤프해 어떻게 다운로드되는지 보여준다.
            dump(wb);
        }
    }

    @Test
    @org.springframework.transaction.annotation.Transactional // 검증 단계에서 lazy 폴더 트리 탐색을 위해 세션 유지
    void exportedFileCanBeImportedBack() throws Exception {
        // given — 스위트를 만들어 엑셀로 내보낸다(앞 테스트와 동일한 경로).
        TestFolder loginFolder = testFolderRepository.save(new TestFolder("로그인", null, 7L));
        TestFolder payFolder = testFolderRepository.save(new TestFolder("결제", null, 7L));
        TestCase loginOk = newCase("로그인 성공", "정상 계정 로그인", TestCasePriority.HIGH,
                TestCaseStatus.READY, loginFolder);
        TestCase pay = newCase("카드 결제", "신용카드 결제 흐름", TestCasePriority.MEDIUM,
                TestCaseStatus.READY, payFolder);
        TestSuite suite = new TestSuite("회귀 스위트", "릴리스 전 회귀", List.of(loginOk, pay));
        suite.setProjectId(7L);
        suite = testSuiteRepository.save(suite);

        byte[] xlsx = mockMvc.perform(
                        MockMvcRequestBuilders.get("/api/suites/{id}/export/excel", suite.getId()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();

        long foldersBefore = testFolderRepository.count();
        long casesBefore = testCaseRepository.count();

        // when — 내려받은 그 바이트를 그대로 import 엔드포인트로 업로드한다.
        MockMultipartFile upload = new MockMultipartFile(
                "file", "회귀 스위트.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", xlsx);

        mockMvc.perform(multipart("/api/import/excel").file(upload).param("projectId", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.createdCases").value(2))     // 케이스 2건 복원
                .andExpect(jsonPath("$.createdFolders").value(3))   // 루트(파일명) + 시트 2개
                .andExpect(jsonPath("$.errors").isEmpty());

        // then — 임포트로 폴더 3개·케이스 2건이 추가됐고, 트리 구조가 그대로 복원됐다.
        assertThat(testFolderRepository.count()).isEqualTo(foldersBefore + 3);
        assertThat(testCaseRepository.count()).isEqualTo(casesBefore + 2);

        TestFolder root = testFolderRepository.findAll().stream()
                .filter(f -> "회귀 스위트".equals(f.getName()) && f.getParent() == null)
                .findFirst().orElseThrow();
        System.out.println("[roundtrip] 임포트로 생성된 루트 폴더: " + root.getName());

        // 임포트로 새로 생긴 '로그인 성공' 케이스(루트 '회귀 스위트' 아래 '로그인' 폴더 소속)를 찾아 값 보존을 확인한다.
        TestCase reimported = testCaseRepository.findAll().stream()
                .filter(tc -> "로그인 성공".equals(tc.getTitle()))
                .filter(tc -> tc.getFolder() != null
                        && tc.getFolder().getParent() != null
                        && root.getId().equals(tc.getFolder().getParent().getId()))
                .findFirst().orElseThrow();

        assertThat(reimported.getFolder().getName()).isEqualTo("로그인");
        assertThat(reimported.getPriority()).isEqualTo(TestCasePriority.HIGH);
        assertThat(reimported.getStatus()).isEqualTo(TestCaseStatus.READY);
        assertThat(reimported.getType()).isEqualTo(TestCaseType.FUNCTIONAL);
        assertThat(reimported.getOs()).isEqualTo(TestCaseOs.MAC);
        assertThat(reimported.getBrowser()).isEqualTo(TestCaseBrowser.CHROME);
        assertThat(reimported.getDevice()).isEqualTo(TestCaseDevice.MOBILE);
        assertThat(reimported.getAssignee()).isEqualTo("tester@example.com");
        assertThat(reimported.getVersion()).isEqualTo("v1.0");
        assertThat(reimported.getDescription()).isEqualTo("정상 계정 로그인");
        System.out.println("[roundtrip] 복원된 케이스: " + reimported.getTitle()
                + " | " + reimported.getPriority() + "/" + reimported.getStatus()
                + " | " + reimported.getOs() + "/" + reimported.getBrowser() + "/" + reimported.getDevice()
                + " | 폴더=" + root.getName() + "/" + reimported.getFolder().getName());
    }

    private TestCase newCase(String title, String desc, TestCasePriority priority,
                             TestCaseStatus status, TestFolder folder) {
        TestCase tc = new TestCase(TestCaseType.FUNCTIONAL, priority, status, title, desc,
                "사전조건 - " + title, "1) 진입\n2) 동작\n3) 확인", "비고 - " + title,
                TestCaseOs.MAC, TestCaseBrowser.CHROME, TestCaseDevice.MOBILE,
                new ArrayList<>(), null, null, "tester@example.com", "v1.0");
        tc.moveToFolder(folder);
        tc.setProjectId(7L);
        return testCaseRepository.save(tc);
    }

    private List<String> sheetNames(XSSFWorkbook wb) {
        List<String> names = new ArrayList<>();
        for (int i = 0; i < wb.getNumberOfSheets(); i++) names.add(wb.getSheetName(i));
        return names;
    }

    private String cell(Sheet sheet, int r, int c) {
        Row row = sheet.getRow(r);
        if (row == null || row.getCell(c) == null) return "";
        return row.getCell(c).getStringCellValue();
    }

    private void dump(XSSFWorkbook wb) {
        System.out.println("\n================ exported-suite.xlsx 내용 ================");
        for (int s = 0; s < wb.getNumberOfSheets(); s++) {
            Sheet sheet = wb.getSheetAt(s);
            System.out.println("● 시트: " + sheet.getSheetName());
            for (int r = 0; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;
                StringBuilder sb = new StringBuilder("  ");
                for (int c = 0; c < 13; c++) {
                    String v = cell(sheet, r, c).replace("\n", "↵");
                    sb.append(v).append(" | ");
                }
                System.out.println(sb);
            }
        }
        System.out.println("=========================================================\n");
    }
}
