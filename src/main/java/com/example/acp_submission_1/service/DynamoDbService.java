package com.example.acp_submission_1.service;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.BillingMode;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableRequest;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableResponse;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;
import software.amazon.awssdk.services.dynamodb.waiters.DynamoDbWaiter;

@Service
public class DynamoDbService {
    private final DynamoDbClient dynamoDbClient;

    public DynamoDbService(DynamoDbClient dynamoDbClient) {
        this.dynamoDbClient = dynamoDbClient;
    }

    public List<Map<String, Object>> getAllItems(String table) {
        List<Map<String, Object>> items = new ArrayList();
        ScanRequest.Builder requestBuilder = ScanRequest.builder().tableName(table);

        ScanResponse response;
        do {
            response = this.dynamoDbClient.scan((ScanRequest)requestBuilder.build());
            Iterator it = response.items().iterator();

            while(it.hasNext()) {
                Map<String, AttributeValue> item = (Map)it.next();
                items.add(this.convertItem(item));
            }

            if (response.hasLastEvaluatedKey() && !response.lastEvaluatedKey().isEmpty()) {
                requestBuilder.exclusiveStartKey(response.lastEvaluatedKey());
            }
        } while(response.hasLastEvaluatedKey() && !response.lastEvaluatedKey().isEmpty());

        return items;
    }

    public Map<String, Object> getItemByKey(String table, String keyValue) {
        String keyName = this.getPartitionKeyName(table);
        GetItemResponse response = this.dynamoDbClient.getItem((GetItemRequest)GetItemRequest.builder().tableName(table).key(Map.of(keyName, (AttributeValue)AttributeValue.builder().s(keyValue).build())).build());
        return response.hasItem() && !response.item().isEmpty() ? this.convertItem(response.item()) : null;
    }

    public void putJsonItem(String table, String keyValue, JsonObject jsonData) {
        this.ensureTableExists(table);
        Map<String, AttributeValue> item = new HashMap();
        item.put("key", (AttributeValue)AttributeValue.builder().s(keyValue).build());
        Iterator it = jsonData.entrySet().iterator();

        while(it.hasNext()) {
            Map.Entry<String, JsonElement> entry = (Map.Entry)it.next();
            item.put((String)entry.getKey(), this.jsonToAttributeValue((JsonElement)entry.getValue()));
        }

        this.dynamoDbClient.putItem((PutItemRequest)PutItemRequest.builder().tableName(table).item(item).build());
    }

    public void putMapItem(String table, String keyValue, Map<String, Object> data) {
        this.ensureTableExists(table);
        Map<String, AttributeValue> item = new HashMap();
        item.put("key", (AttributeValue)AttributeValue.builder().s(keyValue).build());
        Iterator it = data.entrySet().iterator();

        while(it.hasNext()) {
            Map.Entry<String, Object> entry = (Map.Entry)it.next();
            item.put((String)entry.getKey(), this.objectToAttributeValue(entry.getValue()));
        }

        this.dynamoDbClient.putItem((PutItemRequest)PutItemRequest.builder().tableName(table).item(item).build());
    }

    private String getPartitionKeyName(String table) {
        DescribeTableResponse response = this.dynamoDbClient.describeTable((DescribeTableRequest)DescribeTableRequest.builder().tableName(table).build());
        return (String)response.table().keySchema().stream().filter((k) -> {
            return k.keyType() == KeyType.HASH;
        }).map(KeySchemaElement::attributeName).findFirst().orElse("key");
    }

    private void ensureTableExists(String table) {
        try {
            this.dynamoDbClient.describeTable((DescribeTableRequest)DescribeTableRequest.builder().tableName(table).build());
        } catch (ResourceNotFoundException var14) {
            this.dynamoDbClient.createTable((CreateTableRequest)CreateTableRequest.builder().tableName(table).attributeDefinitions(new AttributeDefinition[]{(AttributeDefinition)AttributeDefinition.builder().attributeName("key").attributeType(ScalarAttributeType.S).build()}).keySchema(new KeySchemaElement[]{(KeySchemaElement)KeySchemaElement.builder().attributeName("key").keyType(KeyType.HASH).build()}).billingMode(BillingMode.PAY_PER_REQUEST).build());
            try (DynamoDbWaiter waiter = DynamoDbWaiter.builder().client(this.dynamoDbClient).build()) {
                waiter.waitUntilTableExists((DescribeTableRequest)DescribeTableRequest.builder().tableName(table).build());
            }
        }

    }

    private Map<String, Object> convertItem(Map<String, AttributeValue> item) {
        Map<String, Object> result = new LinkedHashMap();
        Iterator it = item.entrySet().iterator();

        while(it.hasNext()) {
            Map.Entry<String, AttributeValue> entry = (Map.Entry)it.next();
            result.put((String)entry.getKey(), this.attributeValueToObject((AttributeValue)entry.getValue()));
        }

        return result;
    }

    private Object attributeValueToObject(AttributeValue av) {
        if (av == null) {
            return null;
        } else {
            switch (av.type()) {
                case S:
                    return av.s();
                case N:
                    String num = av.n();

                    try {
                        if (num.contains(".")) {
                            return Double.parseDouble(num);
                        }

                        return Long.parseLong(num);
                    } catch (NumberFormatException var7) {
                        return num;
                    }
                case B:
                case SS:
                case NS:
                case BS:
                default:
                    return av.toString();
                case M:
                    Map<String, Object> map = new LinkedHashMap();
                    Iterator it = av.m().entrySet().iterator();

                    while(it.hasNext()) {
                        Map.Entry<String, AttributeValue> e = (Map.Entry)it.next();
                        map.put((String)e.getKey(), this.attributeValueToObject((AttributeValue)e.getValue()));
                    }

                    return map;
                case L:
                    List<Object> list = new ArrayList();
                    Iterator it1 = av.l().iterator();

                    while(it1.hasNext()) {
                        AttributeValue v = (AttributeValue)it1.next();
                        list.add(this.attributeValueToObject(v));
                    }

                    return list;
                case BOOL:
                    return av.bool();
                case NUL:
                    return null;
            }
        }
    }

    private AttributeValue jsonToAttributeValue(JsonElement element) {
        if (element != null && !element.isJsonNull()) {
            if (element.isJsonPrimitive()) {
                JsonPrimitive prim = element.getAsJsonPrimitive();
                if (prim.isBoolean()) {
                    return (AttributeValue)AttributeValue.builder().bool(prim.getAsBoolean()).build();
                } else {
                    return prim.isNumber() ? (AttributeValue)AttributeValue.builder().n(prim.getAsString()).build() : (AttributeValue)AttributeValue.builder().s(prim.getAsString()).build();
                }
            } else {
                Iterator it;
                if (element.isJsonArray()) {
                    List<AttributeValue> list = new ArrayList();
                    it = element.getAsJsonArray().iterator();

                    while(it.hasNext()) {
                        JsonElement e = (JsonElement)it.next();
                        list.add(this.jsonToAttributeValue(e));
                    }

                    return (AttributeValue)AttributeValue.builder().l(list).build();
                } else if (!element.isJsonObject()) {
                    return (AttributeValue)AttributeValue.builder().s(element.toString()).build();
                } else {
                    Map<String, AttributeValue> map = new LinkedHashMap();
                    it = element.getAsJsonObject().entrySet().iterator();

                    while(it.hasNext()) {
                        Map.Entry<String, JsonElement> e = (Map.Entry)it.next();
                        map.put((String)e.getKey(), this.jsonToAttributeValue((JsonElement)e.getValue()));
                    }

                    return (AttributeValue)AttributeValue.builder().m(map).build();
                }
            }
        } else {
            return (AttributeValue)AttributeValue.builder().nul(true).build();
        }
    }

    private AttributeValue objectToAttributeValue(Object value) {
        if (value == null) {
            return (AttributeValue)AttributeValue.builder().nul(true).build();
        } else if (value instanceof Boolean) {
            Boolean b = (Boolean)value;
            return (AttributeValue)AttributeValue.builder().bool(b).build();
        } else if (value instanceof Number) {
            Number n = (Number)value;
            return (AttributeValue)AttributeValue.builder().n(n.toString()).build();
        } else {
            return (AttributeValue)AttributeValue.builder().s(value.toString()).build();
        }
    }
}

