package com.example.acp_submission_1.service;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class PostgresService {
    private final JdbcTemplate jdbcTemplate;
    private final Gson gson = new Gson();
    @Value("${acp.sid}")
    private String sid;

    public PostgresService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Map<String, Object>> getAllRows(String table) {
        JdbcTemplate jdbcTemplate1 = this.jdbcTemplate;
        String sanitized = this.sanitize(table);
        return jdbcTemplate1.queryForList("SELECT * FROM " + sid + "." +sanitized);
    }

    public void createTableIfNotExists(String tableName) {
        String sql = String.format(
                "CREATE SCHEMA IF NOT EXISTS %s; " +
                        "CREATE TABLE IF NOT EXISTS %s.%s (" +
                        "id VARCHAR(255) PRIMARY KEY, " +
                        "name VARCHAR(255), " +
                        "costPer100Moves DOUBLE PRECISION, " +
                        "cooling BOOLEAN, " +
                        "heating BOOLEAN, " +
                        "capacity DOUBLE PRECISION, " +
                        "maxMoves INT, " +
                        "costPerMove DOUBLE PRECISION, " +
                        "costInitial DOUBLE PRECISION, " +
                        "costFinal DOUBLE PRECISION" +
                        ");",
                sid, sid, sanitize(tableName)
        );

        this.jdbcTemplate.execute(sql);
    }

    public void insertJsonRow(String table, JsonObject json) {
        List<String> columns = new ArrayList<>();
        List<Object> values = new ArrayList<>();

        for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
            if (!entry.getKey().equals("capability")) {
                columns.add(entry.getKey());
                values.add(jsonElementToSqlValue(entry.getValue()));
            }
        }

        if (json.has("capability") && json.get("capability").isJsonObject()) {
            JsonObject capability = json.getAsJsonObject("capability");
            for (Map.Entry<String, JsonElement> entry : capability.entrySet()) {
                columns.add(entry.getKey());
                values.add(jsonElementToSqlValue(entry.getValue()));
            }
        }

        String columnNames = String.join(", ", columns);
        String placeholders = columns.stream().map(c -> "?").collect(Collectors.joining(", "));

        String sql = String.format("INSERT INTO %s.%s (%s) VALUES (%s)",
                sid, sanitize(table), columnNames, placeholders);

        this.jdbcTemplate.update(sql, values.toArray());
    }


    private void extractFields(JsonObject obj, List<String> columns, List<Object> values) {
        for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
            if (entry.getKey().equals("capability")) continue;

            columns.add(entry.getKey());
            values.add(this.jsonElementToSqlValue(entry.getValue()));
        }
    }

    private Object jsonElementToSqlValue(JsonElement element) {
        if (element.isJsonNull()) return null;
        if (element.isJsonPrimitive()) {
            var primitive = element.getAsJsonPrimitive();
            if (primitive.isBoolean()) return primitive.getAsBoolean();
            if (primitive.isNumber()) return primitive.getAsDouble();
            return primitive.getAsString();
        }
        return element.toString();
    }

    private String sanitize(String tableName) {
        return tableName.replaceAll("[^a-zA-Z0-9_.]", "");
    }
}

