package com.tms.dashboard.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tms.defect.entity.Defect;
import com.tms.defect.entity.DefectSeverity;
import com.tms.defect.entity.DefectStatus;
import com.tms.defect.repository.DefectRepository;
import com.tms.testcase.entity.AreaTag;
import com.tms.testcase.entity.TestCase;
import com.tms.testcase.entity.TestCasePriority;
import com.tms.testcase.entity.TestCaseStatus;
import com.tms.testcase.entity.TestCaseType;
import com.tms.testcase.repository.AreaTagRepository;
import com.tms.testcase.repository.TestCaseRepository;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 결함 히트맵: 영역태그별 결함(실패) 빈도가 올바르게 집계되는지 종단 검증한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class DashboardHeatmapIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired AreaTagRepository areaTagRepository;
    @Autowired DefectRepository defectRepository;
    @Autowired TestCaseRepository testCaseRepository;

    @AfterEach
    void tearDown() {
        testCaseRepository.deleteAll();
        defectRepository.deleteAll();
        areaTagRepository.deleteAll();
    }

    @Test
    void aggregatesDistinctDefectsPerAreaTag() throws Exception {
        Long projectId = 1L;
        AreaTag login = areaTagRepository.save(new AreaTag("로그인", projectId));
        AreaTag payment = areaTagRepository.save(new AreaTag("결제", projectId));

        Defect d1 = defectRepository.save(new Defect("결함1", null, DefectSeverity.MAJOR, DefectStatus.OPEN, null));
        Defect d2 = defectRepository.save(new Defect("결함2", null, DefectSeverity.MINOR, DefectStatus.OPEN, null));

        // 로그인 영역: case1(d1,d2) + case3(d1) → distinct {d1, d2} = 2
        saveCase("로그인 성공", projectId, List.of(login), List.of(d1, d2));
        saveCase("로그인 재시도", projectId, List.of(login), List.of(d1));
        // 결제 영역: case2(d1) → distinct {d1} = 1
        saveCase("결제 승인", projectId, List.of(payment), List.of(d1));

        mockMvc.perform(get("/api/dashboard/stats").param("projectId", projectId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.defectHeatmap", org.hamcrest.Matchers.hasSize(2)))
                .andExpect(jsonPath("$.defectHeatmap[0].areaTag").value("로그인"))   // total 내림차순
                .andExpect(jsonPath("$.defectHeatmap[0].total").value(2))
                .andExpect(jsonPath("$.defectHeatmap[0].bySeverity.MAJOR").value(1)) // d1
                .andExpect(jsonPath("$.defectHeatmap[0].bySeverity.MINOR").value(1)) // d2
                .andExpect(jsonPath("$.defectHeatmap[1].areaTag").value("결제"))
                .andExpect(jsonPath("$.defectHeatmap[1].total").value(1))
                .andExpect(jsonPath("$.defectHeatmap[1].bySeverity.MAJOR").value(1));
    }

    @Test
    void emptyHeatmapWhenNoDefectsLinked() throws Exception {
        saveCase("태그만 있는 케이스", 1L, List.of(areaTagRepository.save(new AreaTag("로그인", 1L))), List.of());

        mockMvc.perform(get("/api/dashboard/stats").param("projectId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.defectHeatmap", org.hamcrest.Matchers.hasSize(0)));
    }

    private TestCase saveCase(String title, Long projectId, List<AreaTag> tags, List<Defect> defects) {
        TestCase testCase = new TestCase(
                TestCaseType.FUNCTIONAL, TestCasePriority.HIGH, TestCaseStatus.READY,
                title, "설명", "선행조건", "절차", null,
                null, null, null, tags
        );
        testCase.setProjectId(projectId);
        defects.forEach(testCase::linkDefect);
        return testCaseRepository.save(testCase);
    }
}
