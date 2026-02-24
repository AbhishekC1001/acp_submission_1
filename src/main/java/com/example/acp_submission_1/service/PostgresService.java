package com.example.acp_submission_1.service;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class PostgresService {
    private final JdbcTemplate jdbcTemplate;
    private final Gson gson = new Gson();

    public PostgresService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Map<String, Object>> getAllRows(String table) {
        JdbcTemplate jdbcTemplate1 = this.jdbcTemplate;
        String sanitized = this.sanitize(table);
        return jdbcTemplate1.queryForList("SELECT * FROM " + sanitized);
    }

    public void insertJsonRow(String table, JsonObject json) {
        List<String> columns = new ArrayList();
        List<String> placeholders = new ArrayList();
        List<Object> values = new ArrayList();
        Iterator it = json.entrySet().iterator();

        while(it.hasNext()) {
            Map.Entry<String, JsonElement> entry = (Map.Entry)it.next();
            columns.add((String)entry.getKey());
            placeholders.add("?");
            values.add(this.jsonElementToSqlValue((JsonElement)entry.getValue()));
        }

        String sanitized = this.sanitize(table);
        String sql = "INSERT INTO " + sanitized + " (" + String.join(", ", columns) + ") VALUES (" + String.join(", ", placeholders) + ")";
        this.jdbcTemplate.update(sql, values.toArray());
    }

    private Object jsonElementToSqlValue(JsonElement element) {
        if (element != null && !element.isJsonNull()) {
            if (element.isJsonPrimitive()) {
                JsonPrimitive prim = element.getAsJsonPrimitive();
                if (prim.isBoolean()) {
                    return prim.getAsBoolean();
                } else {
                    return prim.isNumber() ? prim.getAsDouble() : prim.getAsString();
                }
            } else {
                return this.gson.toJson(element);
            }
        } else {
            return null;
        }
    }

    private String sanitize(String tableName) {
        return tableName.replaceAll("[^a-zA-Z0-9_.]", "");
    }
}

