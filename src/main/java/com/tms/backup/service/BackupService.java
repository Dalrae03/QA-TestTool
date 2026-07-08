package com.tms.backup.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tms.global.exception.InvalidRequestException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 전체 데이터 백업/복구.
 *
 * <p>백업: 모든 사용자 테이블을 테이블 단위로 JSON 스냅샷(data.json)으로 덤프하고,
 * 첨부파일 저장 폴더(uploads)를 함께 zip 으로 묶는다.
 * 복구: 외래키 검증을 끄고 스냅샷에 있는 테이블을 전부 비운 뒤 그대로 다시 채우고, uploads 도 교체한다.
 *
 * <p>JPA 엔티티 그래프 대신 JDBC 로 raw 행을 다뤄, ID·관계(조인 테이블 포함)를 있는 그대로 보존한다.
 */
@Service
public class BackupService {

    /** 백업 포맷 버전 — 추후 호환성 판단용. */
    private static final int FORMAT_VERSION = 1;
    private static final String DATA_ENTRY = "data.json";
    private static final String UPLOADS_PREFIX = "uploads/";

    /** 스키마 관리 테이블은 백업/복구 대상에서 제외한다. */
    private static final List<String> EXCLUDED_TABLES = List.of("flyway_schema_history");

    private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    private final DataSource dataSource;
    private final ObjectMapper objectMapper;
    private final com.tms.audit.service.AuditLogService auditLogService;
    private final Path uploadRoot;

    public BackupService(DataSource dataSource, ObjectMapper objectMapper,
                         com.tms.audit.service.AuditLogService auditLogService,
                         @Value("${tms.upload.dir}") String uploadDir) {
        this.dataSource = dataSource;
        this.objectMapper = objectMapper;
        this.auditLogService = auditLogService;
        this.uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    public record RestoreResult(int tables, int rows, int files) {}

    // ── 백업(export) ──────────────────────────────────────────────────

    public byte[] exportBackup() {
        try (Connection conn = dataSource.getConnection()) {
            Map<String, Object> snapshot = new LinkedHashMap<>();
            snapshot.put("formatVersion", FORMAT_VERSION);
            snapshot.put("createdAt", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

            List<Map<String, Object>> tables = new ArrayList<>();
            for (String table : listTables(conn)) {
                tables.add(dumpTable(conn, table));
            }
            snapshot.put("tables", tables);

            byte[] json = objectMapper.writeValueAsBytes(snapshot);
            byte[] zip = zip(json);
            auditLogService.log(com.tms.audit.service.AuditLogService.BACKUP, 0L,
                    com.tms.audit.entity.AuditAction.BACKUP_CREATED,
                    "전체 데이터 백업이 생성되었습니다. (테이블 " + tables.size() + "개)");
            return zip;
        } catch (SQLException | IOException e) {
            throw new InvalidRequestException("백업 생성에 실패했습니다: " + e.getMessage());
        }
    }

    private List<String> listTables(Connection conn) throws SQLException {
        List<String> names = new ArrayList<>();
        DatabaseMetaData meta = conn.getMetaData();
        try (ResultSet rs = meta.getTables(conn.getCatalog(), conn.getSchema(), "%", new String[]{"TABLE"})) {
            while (rs.next()) {
                String name = rs.getString("TABLE_NAME");
                if (name == null) continue;
                if (EXCLUDED_TABLES.contains(name.toLowerCase())) continue;
                names.add(name);
            }
        }
        return names;
    }

    private Map<String, Object> dumpTable(Connection conn, String table) throws SQLException {
        List<String> columns = new ArrayList<>();
        List<Integer> types = new ArrayList<>();
        List<List<Object>> rows = new ArrayList<>();

        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM " + quote(table))) {
            ResultSetMetaData md = rs.getMetaData();
            int cols = md.getColumnCount();
            for (int i = 1; i <= cols; i++) {
                columns.add(md.getColumnName(i));
                types.add(md.getColumnType(i));
            }
            while (rs.next()) {
                List<Object> row = new ArrayList<>(cols);
                for (int i = 1; i <= cols; i++) {
                    row.add(serialize(rs.getObject(i)));
                }
                rows.add(row);
            }
        }

        Map<String, Object> dump = new LinkedHashMap<>();
        dump.put("name", table);
        dump.put("columns", columns);
        dump.put("types", types);
        dump.put("rows", rows);
        return dump;
    }

    /** JDBC 값을 JSON 친화 형태로 변환한다. 날짜/시간은 ISO 문자열, 불리언/숫자는 그대로. */
    private Object serialize(Object value) {
        if (value == null) return null;
        if (value instanceof Boolean || value instanceof Number || value instanceof String) return value;
        if (value instanceof Timestamp ts) return ts.toLocalDateTime().format(TS_FMT);
        if (value instanceof LocalDateTime dt) return dt.format(TS_FMT);
        if (value instanceof java.sql.Date d) return d.toString();
        if (value instanceof java.sql.Time t) return t.toString();
        if (value instanceof byte[] bytes) return java.util.Base64.getEncoder().encodeToString(bytes);
        return value.toString();
    }

    private byte[] zip(byte[] dataJson) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(out)) {
            zip.putNextEntry(new ZipEntry(DATA_ENTRY));
            zip.write(dataJson);
            zip.closeEntry();

            if (Files.isDirectory(uploadRoot)) {
                try (Stream<Path> files = Files.list(uploadRoot)) {
                    List<Path> regularFiles = files.filter(Files::isRegularFile).toList();
                    for (Path file : regularFiles) {
                        zip.putNextEntry(new ZipEntry(UPLOADS_PREFIX + file.getFileName()));
                        Files.copy(file, zip);
                        zip.closeEntry();
                    }
                }
            }
        }
        return out.toByteArray();
    }

    // ── 복구(import) ──────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    public RestoreResult restoreBackup(InputStream zipStream) {
        byte[] dataJson = null;
        Map<String, byte[]> uploadFiles = new LinkedHashMap<>();

        try (ZipInputStream zip = new ZipInputStream(zipStream)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (entry.isDirectory()) continue;
                String name = entry.getName();
                if (DATA_ENTRY.equals(name)) {
                    dataJson = zip.readAllBytes();
                } else if (name.startsWith(UPLOADS_PREFIX)) {
                    String fileName = Paths.get(name.substring(UPLOADS_PREFIX.length())).getFileName().toString();
                    if (!fileName.isBlank()) uploadFiles.put(fileName, zip.readAllBytes());
                }
            }
        } catch (IOException e) {
            throw new InvalidRequestException("백업 파일을 읽지 못했습니다: " + e.getMessage());
        }

        if (dataJson == null) {
            throw new InvalidRequestException("올바른 백업 파일이 아닙니다(data.json 누락).");
        }

        Map<String, Object> snapshot;
        try {
            snapshot = objectMapper.readValue(dataJson, Map.class);
        } catch (IOException e) {
            throw new InvalidRequestException("백업 데이터를 해석하지 못했습니다: " + e.getMessage());
        }
        List<Map<String, Object>> tables = (List<Map<String, Object>>) snapshot.get("tables");
        if (tables == null) {
            throw new InvalidRequestException("올바른 백업 파일이 아닙니다(tables 누락).");
        }

        int restoredRows = restoreDatabase(tables);
        int restoredFiles = restoreUploads(uploadFiles);
        // 복구는 audit_logs 까지 백업 시점으로 덮어쓰므로, 복구 '완료' 사실은 복구 이후에 기록한다.
        auditLogService.log(com.tms.audit.service.AuditLogService.BACKUP, 0L,
                com.tms.audit.entity.AuditAction.BACKUP_RESTORED,
                "백업에서 전체 데이터가 복구되었습니다. (행 " + restoredRows + "건, 첨부 " + restoredFiles + "개)");
        return new RestoreResult(tables.size(), restoredRows, restoredFiles);
    }

    @SuppressWarnings("unchecked")
    private int restoreDatabase(List<Map<String, Object>> tables) {
        try (Connection conn = dataSource.getConnection()) {
            boolean prevAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            boolean mysql = conn.getMetaData().getDatabaseProductName().toLowerCase().contains("mysql");
            setForeignKeyChecks(conn, mysql, false);
            int totalRows = 0;
            try {
                // 1) 스냅샷에 있는 테이블을 모두 비운다(FK 꺼져 있어 순서 무관).
                for (Map<String, Object> table : tables) {
                    String name = (String) table.get("name");
                    if (tableExists(conn, name)) {
                        try (Statement st = conn.createStatement()) {
                            st.executeUpdate("DELETE FROM " + quote(name));
                        }
                    }
                }
                // 2) 행을 그대로 다시 채운다.
                for (Map<String, Object> table : tables) {
                    String name = (String) table.get("name");
                    if (!tableExists(conn, name)) continue;
                    List<String> columns = (List<String>) table.get("columns");
                    List<Integer> types = ((List<Number>) table.get("types")).stream().map(Number::intValue).toList();
                    List<List<Object>> rows = (List<List<Object>>) table.get("rows");

                    // 백업 이후 스키마가 바뀌어(예: 마이그레이션으로 컬럼 삭제) 대상 테이블에 더 이상
                    // 존재하지 않는 컬럼은 건너뛴다 — 그대로 INSERT하면 'Unknown column'으로 전체 복구가 실패한다.
                    Set<String> existing = tableColumns(conn, name);
                    List<Integer> keep = new ArrayList<>();
                    for (int i = 0; i < columns.size(); i++) {
                        if (existing.contains(columns.get(i).toLowerCase(Locale.ROOT))) keep.add(i);
                    }
                    if (keep.isEmpty()) continue;
                    if (keep.size() < columns.size()) {
                        List<Integer> idx = keep;
                        List<String> keptCols = idx.stream().map(columns::get).toList();
                        List<Integer> keptTypes = idx.stream().map(types::get).toList();
                        List<List<Object>> keptRows = rows.stream()
                                .map(r -> idx.stream().map(r::get).collect(Collectors.toCollection(ArrayList::new)))
                                .collect(Collectors.toCollection(ArrayList::new));
                        columns = keptCols;
                        types = keptTypes;
                        rows = keptRows;
                    }
                    totalRows += insertRows(conn, name, columns, types, rows);
                }
                setForeignKeyChecks(conn, mysql, true);
                conn.commit();
            } catch (RuntimeException | SQLException e) {
                conn.rollback();
                throw e instanceof RuntimeException re ? re
                        : new InvalidRequestException("복구 중 오류가 발생했습니다: " + e.getMessage());
            } finally {
                conn.setAutoCommit(prevAutoCommit);
            }
            return totalRows;
        } catch (SQLException e) {
            throw new InvalidRequestException("데이터베이스 복구에 실패했습니다: " + e.getMessage());
        }
    }

    private int insertRows(Connection conn, String table, List<String> columns,
                           List<Integer> types, List<List<Object>> rows) throws SQLException {
        if (rows.isEmpty()) return 0;
        String cols = columns.stream().map(this::quote).reduce((a, b) -> a + ", " + b).orElse("");
        String placeholders = String.join(", ", java.util.Collections.nCopies(columns.size(), "?"));
        String sql = "INSERT INTO " + quote(table) + " (" + cols + ") VALUES (" + placeholders + ")";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (List<Object> row : rows) {
                for (int i = 0; i < columns.size(); i++) {
                    bind(ps, i + 1, types.get(i), row.get(i));
                }
                ps.addBatch();
            }
            int[] result = ps.executeBatch();
            return result.length;
        }
    }

    /** 저장된 컬럼 타입에 맞춰 값을 바인딩한다. 날짜/시간 문자열은 Timestamp 로 복원한다. */
    private void bind(PreparedStatement ps, int idx, int sqlType, Object value) throws SQLException {
        if (value == null) {
            ps.setNull(idx, sqlType);
            return;
        }
        switch (sqlType) {
            case Types.TIMESTAMP, Types.TIMESTAMP_WITH_TIMEZONE -> {
                if (value instanceof String s) {
                    ps.setTimestamp(idx, Timestamp.valueOf(LocalDateTime.parse(s, TS_FMT)));
                } else {
                    ps.setObject(idx, value);
                }
            }
            case Types.DATE -> ps.setObject(idx, value instanceof String s ? java.sql.Date.valueOf(s) : value);
            case Types.TIME -> ps.setObject(idx, value instanceof String s ? java.sql.Time.valueOf(s) : value);
            case Types.BINARY, Types.VARBINARY, Types.LONGVARBINARY, Types.BLOB -> {
                if (value instanceof String s) ps.setBytes(idx, java.util.Base64.getDecoder().decode(s));
                else ps.setObject(idx, value);
            }
            default -> ps.setObject(idx, value);
        }
    }

    private int restoreUploads(Map<String, byte[]> uploadFiles) {
        try {
            Files.createDirectories(uploadRoot);
            // 기존 파일을 비우고(백업 시점 상태로 맞춤) 새로 채운다.
            try (Stream<Path> existing = Files.list(uploadRoot)) {
                for (Path file : existing.filter(Files::isRegularFile).toList()) {
                    Files.deleteIfExists(file);
                }
            }
            int count = 0;
            for (Map.Entry<String, byte[]> e : uploadFiles.entrySet()) {
                Path target = uploadRoot.resolve(e.getKey()).normalize();
                if (!target.startsWith(uploadRoot)) continue; // 경로 조작 방지
                Files.write(target, e.getValue());
                count++;
            }
            return count;
        } catch (IOException e) {
            throw new InvalidRequestException("첨부파일 복구에 실패했습니다: " + e.getMessage());
        }
    }

    private void setForeignKeyChecks(Connection conn, boolean mysql, boolean on) throws SQLException {
        try (Statement st = conn.createStatement()) {
            if (mysql) {
                st.execute("SET FOREIGN_KEY_CHECKS = " + (on ? "1" : "0"));
            } else {
                // H2 등
                st.execute("SET REFERENTIAL_INTEGRITY " + (on ? "TRUE" : "FALSE"));
            }
        }
    }

    private boolean tableExists(Connection conn, String table) throws SQLException {
        try (ResultSet rs = conn.getMetaData().getTables(conn.getCatalog(), conn.getSchema(), table, new String[]{"TABLE"})) {
            if (rs.next()) return true;
        }
        // 대소문자 차이를 고려해 한 번 더 시도(H2 는 기본 대문자).
        try (ResultSet rs = conn.getMetaData().getTables(conn.getCatalog(), conn.getSchema(),
                table.toUpperCase(), new String[]{"TABLE"})) {
            return rs.next();
        }
    }

    /** 대상 테이블에 실제로 존재하는 컬럼명 집합(소문자)을 반환한다. */
    private Set<String> tableColumns(Connection conn, String table) throws SQLException {
        Set<String> cols = new java.util.HashSet<>();
        DatabaseMetaData md = conn.getMetaData();
        for (String candidate : new String[]{table, table.toUpperCase(Locale.ROOT)}) {
            try (ResultSet rs = md.getColumns(conn.getCatalog(), conn.getSchema(), candidate, null)) {
                while (rs.next()) {
                    cols.add(rs.getString("COLUMN_NAME").toLowerCase(Locale.ROOT));
                }
            }
            if (!cols.isEmpty()) break; // 대소문자 차이(H2 는 기본 대문자)를 고려해 한 번 더 시도.
        }
        return cols;
    }

    private String quote(String identifier) {
        return "`" + identifier.replace("`", "``") + "`";
    }
}
