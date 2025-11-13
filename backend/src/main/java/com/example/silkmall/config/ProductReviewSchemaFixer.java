package com.example.silkmall.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Drops the legacy unique index on {@code product_reviews.order_item_id} so that multiple roles
 * can persist reviews for the same order item. Older deployments created this constraint via the
 * JPA mapping and Hibernate's schema update does not remove it automatically, so we perform the
 * migration programmatically at startup.
 */
@Component
public class ProductReviewSchemaFixer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ProductReviewSchemaFixer.class);

    private final DataSource dataSource;

    public ProductReviewSchemaFixer(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) {
        ensureFlexibleReviewSchema();
    }

    /**
     * Exposed for tests so they can verify the migration behaviour.
     */
    public void ensureFlexibleReviewSchema() {
        Connection connection = DataSourceUtils.getConnection(dataSource);
        try {
            DatabaseMetaData metaData = connection.getMetaData();
            String productName = metaData.getDatabaseProductName();
            String quote = metaData.getIdentifierQuoteString();

            Map<String, SortedMap<Short, String>> indexColumns = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            collectUniqueIndexes(metaData, connection, "product_reviews", indexColumns);
            if (indexColumns.isEmpty()) {
                collectUniqueIndexes(metaData, connection, "PRODUCT_REVIEWS", indexColumns);
            }

            indexColumns.forEach((indexName, columns) -> {
                boolean legacyUniqueIndex = columns.values().stream()
                        .filter(Objects::nonNull)
                        .map(name -> name.toLowerCase(Locale.ROOT))
                        .allMatch("order_item_id"::equals);
                if (legacyUniqueIndex && columns.size() == 1) {
                    try {
                        dropIndex(connection, productName, quote, "product_reviews", indexName);
                    } catch (SQLException ex) {
                        log.warn("Unable to drop legacy unique index {} on product_reviews", indexName, ex);
                    }
                }
            });
        } catch (SQLException ex) {
            log.warn("Failed to inspect product_reviews indexes", ex);
        } finally {
            DataSourceUtils.releaseConnection(connection, dataSource);
        }
    }

    private void dropIndex(Connection connection, String productName, String quote,
                           String tableName, String indexName) throws SQLException {
        String identifierQuote = (quote == null || quote.isBlank()) ? "" : quote.trim();
        String quotedIndex = identifierQuote + indexName + identifierQuote;
        String quotedTable = identifierQuote + tableName + identifierQuote;

        String sql;
        if (productName != null && productName.toLowerCase(Locale.ROOT).contains("mysql")) {
            sql = "DROP INDEX " + quotedIndex + " ON " + quotedTable;
        } else {
            sql = "DROP INDEX " + quotedIndex;
        }

        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
            log.info("Dropped legacy unique index {} on {}", indexName, tableName);
        }
    }

    private void collectUniqueIndexes(DatabaseMetaData metaData, Connection connection, String tableName,
                                      Map<String, SortedMap<Short, String>> indexColumns) throws SQLException {
        if (tableName == null) {
            return;
        }
        try (ResultSet rs = metaData.getIndexInfo(connection.getCatalog(), connection.getSchema(), tableName, true, false)) {
            while (rs.next()) {
                String indexName = rs.getString("INDEX_NAME");
                String columnName = rs.getString("COLUMN_NAME");
                if (indexName == null || columnName == null || "PRIMARY".equalsIgnoreCase(indexName)) {
                    continue;
                }
                short position = rs.getShort("ORDINAL_POSITION");
                indexColumns.computeIfAbsent(indexName, k -> new TreeMap<>())
                        .put(position, columnName);
            }
        }
    }
}

