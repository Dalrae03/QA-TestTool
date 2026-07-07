package db.migration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/**
 * V2에서 제거됐어야 할 test_suite_cases.case_order가 이후 ddl-auto=update로
 * (엔티티에 @OrderColumn이 있던 시점 기준) 재생성된 환경을 다시 정리한다.
 * 엔티티는 이 컬럼을 매핑하지 않으므로 NOT NULL로 남아 있으면 스위트 저장 시 INSERT가 실패한다.
 */
public class V4__drop_test_suite_cases_case_order extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection conn = context.getConnection();
        if (!caseOrderColumnExists(conn)) {
            return; // 이미 제거된 상태(또는 신규 설치)
        }
        try (Statement st = conn.createStatement()) {
            st.executeUpdate("ALTER TABLE test_suite_cases DROP COLUMN case_order");
        }
    }

    private boolean caseOrderColumnExists(Connection conn) {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS " +
                "WHERE TABLE_SCHEMA = DATABASE() " +
                "  AND TABLE_NAME = 'test_suite_cases' " +
                "  AND COLUMN_NAME = 'case_order'")) {
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getLong(1) > 0;
            }
        } catch (Exception e) {
            return false;
        }
    }
}
