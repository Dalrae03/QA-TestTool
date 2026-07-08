package db.migration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/**
 * Hibernate ddl-auto=update가 자동 생성한 test_plan_core_cases 조인 테이블은
 * FK 컬럼만 있고 PK가 없다(test_suite_cases가 V2 이전에 그랬던 것과 동일한 상태).
 * (test_plan_id, test_case_id) 복합 PK를 추가해 중복 연결 행을 DB 레벨에서 막는다.
 */
public class V6__add_test_plan_core_cases_pk extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection conn = context.getConnection();
        if (!tableExists(conn)) {
            // 신규 설치: Flyway는 Hibernate ddl-auto보다 먼저 돌기 때문에 아직 조인 테이블이 없다.
            // 여기서 PK를 갖춰 미리 만들어 두면, 뒤이어 도는 ddl-auto=update가 이 테이블을 그대로 두고
            // FK 제약만 추가한다(FK 대상 test_plans/test_cases는 이 시점에 아직 없으므로 여기서 만들지 않는다).
            try (Statement st = conn.createStatement()) {
                st.executeUpdate(
                        "CREATE TABLE test_plan_core_cases (" +
                        "  test_plan_id BIGINT NOT NULL," +
                        "  test_case_id BIGINT NOT NULL," +
                        "  PRIMARY KEY (test_plan_id, test_case_id))");
            }
            return;
        }
        if (hasPrimaryKey(conn)) {
            return;
        }
        // 기존 설치: Hibernate가 이미 만든 PK 없는 테이블에 복합 PK를 추가한다.
        try (Statement st = conn.createStatement()) {
            st.executeUpdate("ALTER TABLE test_plan_core_cases ADD PRIMARY KEY (test_plan_id, test_case_id)");
        }
    }

    private boolean tableExists(Connection conn) {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES " +
                "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'test_plan_core_cases'")) {
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getLong(1) > 0;
            }
        } catch (Exception e) {
            return false;
        }
    }

    private boolean hasPrimaryKey(Connection conn) {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS " +
                "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'test_plan_core_cases' " +
                "  AND CONSTRAINT_TYPE = 'PRIMARY KEY'")) {
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getLong(1) > 0;
            }
        } catch (Exception e) {
            return true;
        }
    }
}
