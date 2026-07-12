package db.migration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/**
 * 테스트 플랜 폼 전면 개편: 예전 "확장 필드"(description 포함)를 제거하고,
 * status 컬럼을 7단계 값으로 넓힌다.
 *
 * status는 MySQL 네이티브 ENUM으로 생성돼 있어(예: enum('ACTIVE','ARCHIVED','COMPLETED','DRAFT')),
 * ddl-auto=update가 자동으로 값 목록을 넓혀주지 않는다 — 오늘 test_suite_cases.case_order에서
 * 겪은 것과 같은 유형의 "ddl-auto가 처리 못 하는 변경"이라 직접 ALTER한다.
 */
public class V5__drop_test_plan_extension_fields extends BaseJavaMigration {

    private static final String[] COLUMNS_TO_DROP = {
            "description", "risk_items", "scope", "team_size",
            "team_members", "quality_criteria", "budget", "plan_notes"
    };

    @Override
    public void migrate(Context context) throws Exception {
        Connection conn = context.getConnection();

        for (String column : COLUMNS_TO_DROP) {
            if (!columnExists(conn, "test_plans", column)) {
                continue; // 이미 제거된 상태(또는 신규 설치)
            }
            try (Statement st = conn.createStatement()) {
                st.executeUpdate("ALTER TABLE test_plans DROP COLUMN " + column);
            }
        }

        String statusType = statusColumnType(conn);
        if (statusType == null) {
            return; // status 컬럼이 아직 없음(신규 설치) — ddl-auto가 최종 값 목록으로 생성한다.
        }
        boolean hasLegacyValues = statusType.contains("ACTIVE") || statusType.contains("ARCHIVED");
        boolean hasNewValues = statusType.contains("IN_REVIEW");
        if (hasLegacyValues || !hasNewValues) {
            try (Statement st = conn.createStatement()) {
                // 1) 구·신 값을 모두 허용하도록 임시로 넓힌다 — 기존 행을 remap하기 전에
                //    바로 최종 목록으로 좁히면 ACTIVE/ARCHIVED 행이 truncate되거나 ALTER가 실패한다.
                st.executeUpdate(
                        "ALTER TABLE test_plans MODIFY COLUMN status " +
                        "ENUM('DRAFT','ACTIVE','COMPLETED','ARCHIVED'," +
                        "'IN_REVIEW','APPROVED','IN_PROGRESS','ON_HOLD','CANCELLED') NOT NULL");
                // 2) 제거되는 구 값을 신 값으로 remap (ACTIVE→진행중, ARCHIVED→완료)
                st.executeUpdate("UPDATE test_plans SET status = 'IN_PROGRESS' WHERE status = 'ACTIVE'");
                st.executeUpdate("UPDATE test_plans SET status = 'COMPLETED' WHERE status = 'ARCHIVED'");
                // 3) 최종 값 목록으로 좁힌다
                st.executeUpdate(
                        "ALTER TABLE test_plans MODIFY COLUMN status " +
                        "ENUM('DRAFT','IN_REVIEW','APPROVED','IN_PROGRESS','COMPLETED','ON_HOLD','CANCELLED') NOT NULL");
            }
        }
    }

    private boolean columnExists(Connection conn, String table, String column) {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS " +
                "WHERE TABLE_SCHEMA = DATABASE() " +
                "  AND TABLE_NAME = ? " +
                "  AND COLUMN_NAME = ?")) {
            ps.setString(1, table);
            ps.setString(2, column);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getLong(1) > 0;
            }
        } catch (Exception e) {
            return false;
        }
    }

    /** status 컬럼의 COLUMN_TYPE 문자열(예: "enum('DRAFT','ACTIVE',...)")을 반환. 컬럼이 없으면 null. */
    private String statusColumnType(Connection conn) {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT COLUMN_TYPE FROM INFORMATION_SCHEMA.COLUMNS " +
                "WHERE TABLE_SCHEMA = DATABASE() " +
                "  AND TABLE_NAME = 'test_plans' " +
                "  AND COLUMN_NAME = 'status'")) {
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null; // 컬럼이 없으면(신규 설치, ddl-auto가 곧 생성) 손대지 않는다.
                return rs.getString(1);
            }
        } catch (Exception e) {
            return null;
        }
    }
}
