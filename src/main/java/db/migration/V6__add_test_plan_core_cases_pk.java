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
        if (!tableExists(conn) || hasPrimaryKey(conn)) {
            return;
        }
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
