package db.migration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/**
 * @OrderColumn 도입 시 PK가 (test_suite_id, case_order)로 바뀐 환경을
 * (test_suite_id, test_case_id) PK + case_order 컬럼 제거 형태로 재구성한다.
 */
public class V2__fix_test_suite_cases_pk extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection conn = context.getConnection();
        if (!caseOrderColumnExists(conn)) {
            return; // 이미 수정된 상태(또는 신규 설치)
        }

        try (Statement st = conn.createStatement()) {
            // 중복 (suite_id, tc_id) 행 제거 (case_order가 큰 쪽 삭제)
            st.executeUpdate(
                    "DELETE t1 FROM test_suite_cases t1 " +
                    "INNER JOIN test_suite_cases t2 " +
                    "  ON t1.test_suite_id = t2.test_suite_id " +
                    "  AND t1.test_case_id  = t2.test_case_id " +
                    "  AND t1.case_order    > t2.case_order");
        }

        try (Statement st = conn.createStatement()) {
            // MySQL은 FK가 있는 인덱스를 단독으로 DROP할 수 없으므로 한 문장으로 처리
            st.executeUpdate(
                    "ALTER TABLE test_suite_cases " +
                    "DROP PRIMARY KEY, " +
                    "ADD PRIMARY KEY (test_suite_id, test_case_id), " +
                    "DROP COLUMN case_order");
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
