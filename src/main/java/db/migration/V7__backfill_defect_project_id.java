package db.migration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/**
 * 결함(Defect)에 project_id를 도입하면서, 기존 결함을 연결된 테스트케이스의 프로젝트로 백필한다.
 *
 * Flyway는 Hibernate ddl-auto보다 먼저 실행되므로, 기존 DB에는 이 시점에 defects.project_id 컬럼이
 * 아직 없다 — 컬럼을 먼저 추가한 뒤 test_case_defects 조인을 타고 소속 프로젝트를 채운다.
 * (신규 설치라 defects 테이블 자체가 없으면 손대지 않고, 이후 Hibernate가 컬럼째로 생성한다.)
 */
public class V7__backfill_defect_project_id extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection conn = context.getConnection();

        if (!tableExists(conn, "defects")) {
            return; // 신규 설치 — Hibernate가 project_id 포함해 생성한다.
        }
        if (!columnExists(conn, "defects", "project_id")) {
            try (Statement st = conn.createStatement()) {
                st.executeUpdate("ALTER TABLE defects ADD COLUMN project_id BIGINT");
            }
        }
        // 연결된 테스트케이스의 프로젝트로 채운다 — 여러 케이스에 연결됐으면 그중 하나를 사용.
        // 프로젝트가 지정되지 않은(전역) 결함만 대상으로 한다.
        try (Statement st = conn.createStatement()) {
            st.executeUpdate(
                    "UPDATE defects d SET d.project_id = (" +
                    "  SELECT tc.project_id FROM test_case_defects tcd " +
                    "  JOIN test_cases tc ON tc.id = tcd.test_case_id " +
                    "  WHERE tcd.defect_id = d.id AND tc.project_id IS NOT NULL LIMIT 1" +
                    ") WHERE d.project_id IS NULL");
        }
    }

    private boolean tableExists(Connection conn, String table) {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES " +
                "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?")) {
            ps.setString(1, table);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getLong(1) > 0;
            }
        } catch (Exception e) {
            return false;
        }
    }

    private boolean columnExists(Connection conn, String table, String column) {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS " +
                "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?")) {
            ps.setString(1, table);
            ps.setString(2, column);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getLong(1) > 0;
            }
        } catch (Exception e) {
            return false;
        }
    }
}
