package com.tms.jira.repository;

import com.tms.jira.entity.JiraSetting;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JiraSettingRepository extends JpaRepository<JiraSetting, Long> {

    /** 싱글턴 설정 행 조회 — 가장 먼저 저장된 한 행만 사용한다. */
    Optional<JiraSetting> findFirstByOrderByIdAsc();
}
