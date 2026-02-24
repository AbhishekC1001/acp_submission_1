package com.example.acp_submission_1.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class DroneProcessingService {
    private final RestTemplate restTemplate;
    private final Gson gson = (new GsonBuilder()).create();

    public DroneProcessingService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public List<JsonObject> fetchAndProcessDrones(String urlPath) {
        String json = (String)this.restTemplate.getForObject(urlPath, String.class, new Object[0]);
        JsonArray drones = JsonParser.parseString(json).getAsJsonArray();
        List<JsonObject> processed = new ArrayList();
        Iterator it = drones.iterator();

        while(it.hasNext()) {
            JsonElement element = (JsonElement)it.next();
            processed.add(this.processDrone(element.getAsJsonObject()));
        }

        return processed;
    }

    private JsonObject processDrone(JsonObject source) {
        JsonObject result = new JsonObject();
        if (source.has("name")) {
            result.add("name", source.get("name"));
        }

        if (source.has("id")) {
            result.add("id", source.get("id"));
        }

        JsonObject capability = new JsonObject();
        String[] capFields = new String[]{"cooling", "heating", "capacity", "maxMoves", "costPerMove", "costInitial", "costFinal"};
        int len = capFields.length;
        if(source.has("capability")) {
            JsonObject cap=source.get("capability").getAsJsonObject();
            for (int i = 0; i < len; i++) {
                String field = capFields[i];
                if (cap.has(field)) {
                    capability.add(field, cap.get(field));
                }
            }
            result.add("capability", capability);
            double costInitial = this.safeDouble(cap, "costInitial");
            double costFinal = this.safeDouble(cap, "costFinal");
            double costPerMove = this.safeDouble(cap, "costPerMove");
            double costPer100Moves = costInitial + costFinal + costPerMove * 100.0;
            result.addProperty("costPer100Moves", costPer100Moves);
        }
        return result;
    }

    private double safeDouble(JsonObject obj, String field) {
        if (obj.has(field) && !obj.get(field).isJsonNull()) {
            try {
                double val = obj.get(field).getAsDouble();
                return Double.isNaN(val) ? 0.0 : val;
            } catch (Exception var5) {
                return 0.0;
            }
        } else {
            return 0.0;
        }
    }

    public String toJsonArray(List<JsonObject> list) {
        JsonArray array = new JsonArray();
        list.forEach(array::add);
        return this.gson.toJson(array);
    }

    public String toJson(Object obj) {
        return this.gson.toJson(obj);
    }
}
