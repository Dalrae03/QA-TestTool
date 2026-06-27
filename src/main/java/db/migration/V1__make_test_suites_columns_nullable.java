package db.migration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/**
 * test_suites.test_plan_id / project_id를 NOT NULL -> NULL로 변경한다.
 * ddl-auto=update는 컬럼을 nullable로 되돌리지 못하므로 직접 처리해야 한다.
 */
public class V1__make_test_suites_columns_nullable extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection conn = context.getConnection();
        makeNullable(conn, "test_suites", "test_plan_id", "BIGINT");
        makeNullable(conn, "test_suites", "project_id", "BIGINT");
    }

    private void makeNullable(Connection conn, String table, String column, String type) {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT IS_NULLABLE FROM INFORMATION_SCHEMA.COLUMNS " +
                "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?")) {
            ps.setString(1, table);
            ps.setString(2, column);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next() && "NO".equals(rs.getString(1))) {
                    try (Statement st = conn.createStatement()) {
                        st.executeUpdate("ALTER TABLE " + table + " MODIFY " + column + " " + type + " NULL");
                    }
                }
            }
        } catch (Exception ignored) {
            // 테이블/컬럼이 없는 환경(신규 설치)에서는 아무 것도 하지 않는다.
        }
    }
}
