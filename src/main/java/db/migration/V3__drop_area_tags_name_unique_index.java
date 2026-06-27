package db.migration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/**
 * area_tags.name의 전역 UNIQUE 인덱스를 제거한다 — 프로젝트별로 같은 이름의 태그를 허용하기 위함.
 */
public class V3__drop_area_tags_name_unique_index extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection conn = context.getConnection();
        List<String> indexNames = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT INDEX_NAME FROM INFORMATION_SCHEMA.STATISTICS " +
                "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'area_tags' AND COLUMN_NAME = 'name' " +
                "  AND NON_UNIQUE = 0 AND INDEX_NAME <> 'PRIMARY'")) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    indexNames.add(rs.getString(1));
                }
            }
        } catch (Exception ignored) {
            return; // 테이블이 없는 환경(신규 설치)
        }

        for (String name : indexNames) {
            try (Statement st = conn.createStatement()) {
                st.executeUpdate("ALTER TABLE area_tags DROP INDEX " + name);
            }
        }
    }
}
