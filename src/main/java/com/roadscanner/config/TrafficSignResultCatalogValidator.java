package com.roadscanner.config;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/** Fails startup if the ML result IDs would resolve to the wrong DB descriptions. */
@Component
@Profile({"!local", "local-ml"})
public class TrafficSignResultCatalogValidator implements InitializingBean {

    private static final String CLASS_MAP_RESOURCE = "ml_service/class_map.json";

    private final JdbcTemplate jdbcTemplate;
    private final String expectedCatalogSha256;

    public TrafficSignResultCatalogValidator(
            JdbcTemplate jdbcTemplate,
            @Value("${roadscanner.ml.catalog-sha256}") String expectedCatalogSha256) {
        this.jdbcTemplate = jdbcTemplate;
        this.expectedCatalogSha256 = expectedCatalogSha256;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        List<CatalogRow> expected = loadExpectedCatalog();
        List<CatalogRow> actual = jdbcTemplate.query(
                "SELECT no, name, content, url FROM result_image "
                        + "WHERE no BETWEEN 1 AND 44 ORDER BY no",
                (resultSet, rowNumber) -> new CatalogRow(
                        resultSet.getInt("no"),
                        resultSet.getString("name"),
                        resultSet.getString("content"),
                        resultSet.getString("url")));
        if (!expected.equals(actual)) {
            throw new IllegalStateException(
                    "RESULT_IMAGE does not match the 44-row ML class catalog; "
                            + "apply the versioned Oracle catalog migration before enabling ML");
        }
    }

    private List<CatalogRow> loadExpectedCatalog() throws IOException {
        ClassPathResource resource = new ClassPathResource(CLASS_MAP_RESOURCE);
        verifyResourceHash(resource);
        try (InputStream input = resource.getInputStream()) {
            JsonNode root = new ObjectMapper().readTree(input);
            List<CatalogRow> rows = new ArrayList<>();
            for (JsonNode item : root.path("classes")) {
                rows.add(new CatalogRow(
                        item.path("result_id").asInt(),
                        item.path("name_en").asText(),
                        item.path("name_ko").asText(),
                        "none"));
            }
            JsonNode unknown = root.path("unknown");
            rows.add(new CatalogRow(
                    unknown.path("result_id").asInt(),
                    unknown.path("name_en").asText(),
                    unknown.path("name_ko").asText(),
                    "none"));
            if (rows.size() != 44) {
                throw new IllegalStateException("ML class catalog must contain exactly 44 results");
            }
            return rows;
        }
    }

    private void verifyResourceHash(ClassPathResource resource) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream input = resource.getInputStream()) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = input.read(buffer)) != -1) {
                    digest.update(buffer, 0, read);
                }
            }
            StringBuilder actual = new StringBuilder(64);
            for (byte value : digest.digest()) {
                actual.append(String.format("%02X", value & 0xff));
            }
            if (expectedCatalogSha256 == null
                    || !actual.toString().equals(expectedCatalogSha256.trim().toUpperCase())) {
                throw new IllegalStateException(
                        "Packaged ML class catalog does not match the configured SHA-256");
            }
        } catch (java.security.NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is unavailable", impossible);
        }
    }

    private static final class CatalogRow {
        private final int no;
        private final String name;
        private final String content;
        private final String url;

        private CatalogRow(int no, String name, String content, String url) {
            this.no = no;
            this.name = name;
            this.content = content;
            this.url = url;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof CatalogRow)) {
                return false;
            }
            CatalogRow row = (CatalogRow) other;
            return no == row.no
                    && name.equals(row.name)
                    && content.equals(row.content)
                    && url.equals(row.url);
        }

        @Override
        public int hashCode() {
            int result = Integer.hashCode(no);
            result = 31 * result + name.hashCode();
            result = 31 * result + content.hashCode();
            return 31 * result + url.hashCode();
        }
    }
}
