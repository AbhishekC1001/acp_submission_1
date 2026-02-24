package com.example.acp_submission_1.controller;

import com.google.gson.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.example.acp_submission_1.service.DroneProcessingService;
import com.example.acp_submission_1.service.DynamoDbService;
import com.example.acp_submission_1.service.PostgresService;
import com.example.acp_submission_1.service.S3Service;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping(
        value = {"/api/v1/acp"},
        produces = {"application/json"}
)
public class AcpController {
    private final S3Service s3Service;
    private final DynamoDbService dynamoDbService;
    private final PostgresService postgresService;
    private final DroneProcessingService droneProcessingService;
    private final Gson gson = new Gson();
    @Value("${acp.sid}")
    private String sid;

    public AcpController(S3Service s3Service, DynamoDbService dynamoDbService, PostgresService postgresService, DroneProcessingService droneProcessingService) {
        this.s3Service = s3Service;
        this.dynamoDbService = dynamoDbService;
        this.postgresService = postgresService;
        this.droneProcessingService = droneProcessingService;
    }

    @GetMapping({"/all/s3/{bucket}"})
    public ResponseEntity<String> getAllS3Objects(@PathVariable String bucket) {
        try {
            List<String> contents = this.s3Service.getAllObjectContents(bucket);
            JsonArray array = new JsonArray();
            Iterator it = contents.iterator();

            while(it.hasNext()) {
                String content = (String)it.next();

                try {
                    array.add(JsonParser.parseString(content));
                } catch (JsonSyntaxException e) {
                    array.add(content);
                }
            }

            return ResponseEntity.ok(this.gson.toJson(array));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping({"/single/s3/{bucket}/{key}"})
    public ResponseEntity<String> getSingleS3Object(@PathVariable String bucket, @PathVariable String key) {
        try {
            String content = this.s3Service.getObjectContent(bucket, key);
            return ResponseEntity.ok(content);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping({"/all/dynamo/{table}"})
    public ResponseEntity<String> getAllDynamoItems(@PathVariable String table) {
        try {
            List<Map<String, Object>> items = this.dynamoDbService.getAllItems(table);
            return ResponseEntity.ok(this.gson.toJson(items));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping({"/single/dynamo/{table}/{key}"})
    public ResponseEntity<String> getSingleDynamoItem(@PathVariable String table, @PathVariable String key) {
        try {
            Map<String, Object> item = this.dynamoDbService.getItemByKey(table, key);
            return item == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(this.gson.toJson(item));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping({"/all/postgres/{table}"})
    public ResponseEntity<String> getAllPostgresRows(@PathVariable String table) {
        try {
            List<Map<String, Object>> rows = this.postgresService.getAllRows(table);
            return ResponseEntity.ok(this.gson.toJson(rows));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping({"/process/dump"})
    public ResponseEntity<String> processDump(@RequestBody Map<String, String> body) {
        try {
            String urlPath = (String)body.get("urlPath");
            List<JsonObject> processed = this.droneProcessingService.fetchAndProcessDrones(urlPath);
            return ResponseEntity.ok(this.droneProcessingService.toJsonArray(processed));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping({"/process/dynamo"})
    public ResponseEntity<Void> processDynamo(@RequestBody Map<String, String> body) {
        try {
            String urlPath = (String)body.get("urlPath");
            List<JsonObject> processed = this.droneProcessingService.fetchAndProcessDrones(urlPath);
            Iterator it = processed.iterator();

            while(it.hasNext()) {
                JsonObject drone = (JsonObject)it.next();
                String droneName = drone.get("name").getAsString();
                this.dynamoDbService.putJsonItem(this.sid, droneName, drone);
            }

            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping({"/process/s3"})
    public ResponseEntity<Void> processS3(@RequestBody Map<String, String> body) {
        try {
            String urlPath = (String)body.get("urlPath");
            List<JsonObject> processed = this.droneProcessingService.fetchAndProcessDrones(urlPath);
            Iterator it = processed.iterator();

            while(it.hasNext()) {
                JsonObject drone = (JsonObject)it.next();
                String droneName = drone.get("name").getAsString();
                this.s3Service.putObject(this.sid, droneName, this.gson.toJson(drone));
            }

            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping({"/process/postgres/{table}"})
    public ResponseEntity<Void> processPostgres(@PathVariable String table, @RequestBody Map<String, String> body) {
        try {
            String urlPath = (String)body.get("urlPath");
            List<JsonObject> processed = this.droneProcessingService.fetchAndProcessDrones(urlPath);
            Iterator it = processed.iterator();

            while(it.hasNext()) {
                JsonObject drone = (JsonObject)it.next();
                this.postgresService.insertJsonRow(table, drone);
            }

            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping({"/copy-content/dynamo/{table}"})
    public ResponseEntity<Void> copyContentToDynamo(@PathVariable String table) {
        try {
            List<Map<String, Object>> rows = this.postgresService.getAllRows(table);
            Iterator it = rows.iterator();

            while(it.hasNext()) {
                Map<String, Object> row = (Map)it.next();
                String uuid = UUID.randomUUID().toString();
                this.dynamoDbService.putMapItem(this.sid, uuid, row);
            }

            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping({"/copy-content/S3/{table}"})
    public ResponseEntity<Void> copyContentToS3(@PathVariable String table) {
        try {
            List<Map<String, Object>> rows = this.postgresService.getAllRows(table);
            Iterator it = rows.iterator();

            while(it.hasNext()) {
                Map<String, Object> row = (Map)it.next();
                String uuid = UUID.randomUUID().toString();
                this.s3Service.putObject(this.sid, uuid, this.gson.toJson(row));
            }

            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}
