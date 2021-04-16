package com.corsosystems;

import com.corsosystems.db.HubSettingsRecord;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ValueNode;
import com.inductiveautomation.ignition.common.BasicDataset;

import com.inductiveautomation.ignition.common.model.values.QualityCode;
import com.inductiveautomation.ignition.common.sqltags.model.types.DataType;

import com.inductiveautomation.ignition.common.tags.model.TagPath;
import com.inductiveautomation.ignition.common.util.DatasetBuilder;
import com.inductiveautomation.ignition.gateway.localdb.persistence.IRecordListener;
import com.inductiveautomation.ignition.gateway.model.GatewayContext;
import com.inductiveautomation.ignition.gateway.tags.managed.ManagedTagProvider;
import com.inductiveautomation.ignition.gateway.tags.managed.ProviderConfiguration;
import com.inductiveautomation.ignition.gateway.tags.managed.WriteHandler;
import com.inductiveautomation.ignition.gateway.web.models.KeyValue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import simpleorm.dataset.SQuery;

import java.io.IOException;
import java.net.*;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.net.http.HttpClient;

public class HueProvider {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private GatewayContext gatewayContext;
    private ManagedTagProvider tagProvider;

    private final HttpClient httpClient = HttpClient.newHttpClient();;

    public HueProvider(GatewayContext gatewayContext) {
        this.gatewayContext = gatewayContext;

        HubSettingsRecord.META.addRecordListener(new IRecordListener<HubSettingsRecord>() {
            @Override
            public void recordUpdated(HubSettingsRecord hubSettingsRecord) {

            }

            @Override
            public void recordAdded(HubSettingsRecord hubSettingsRecord) {
                init();
            }

            @Override
            public void recordDeleted(KeyValue keyValue) {

            }
        });

        init();
    }

    private void init() {
        SQuery<HubSettingsRecord> query = new SQuery<>(HubSettingsRecord.META);
        List<HubSettingsRecord> hubs = gatewayContext.getPersistenceInterface().query(query);

        if (hubs.size() <= 0) {
            logger.warn("No hubs configured");
            return;
        }

        createTagProvider();
        initializeHubConnections(hubs);
    }

    private void createTagProvider() {
        ProviderConfiguration providerConfiguration = new ProviderConfiguration("Hue");
        providerConfiguration.setAllowTagCustomization(true);
        providerConfiguration.setPersistTags(true);
        providerConfiguration.setPersistValues(false);
        providerConfiguration.setStaleTimeoutMS(100000000);

        tagProvider = gatewayContext.getTagManager().getOrCreateManagedProvider(providerConfiguration);
    }

    private void initializeHubConnections(List<HubSettingsRecord> hubs) {

        for (HubSettingsRecord hub : hubs) {
            String hubName = hub.getHubName();
            String ipAddress = hub.getIpAddress();
            String apiKey = hub.getApiKey();

            String baseUrl = "http://" + ipAddress + "/api/" + apiKey;

            final JsonNode node;
            try {
                node = new ObjectMapper().readTree(new URL(baseUrl));
                logger.info(node.toPrettyString());

                Map<String, String> map = new HashMap<>();
                addTags(baseUrl, hubName + "-test", node, map);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public JsonNode getRootNode(String baseUrl, String hubName) {
        //String baseUrl = "http://" + ipAddress + "/api/" + apiKey;

        final JsonNode node;
        try {
            node = new ObjectMapper().readTree(new URL(baseUrl));
            logger.info(node.toPrettyString());

            //Map<String, String> map = new HashMap<>();
            //addTags(baseUrl, hubName + "2", node, map);
            return node;

        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void addTags(String baseUrl, String currentPath, JsonNode jsonNode, Map<String, String> map) {
        ObjectMapper mapper = new ObjectMapper();
        //logger.info("addKeys: " + currentPath);
        if (jsonNode.isObject()) {
            ObjectNode objectNode = (ObjectNode) jsonNode;
            Iterator<Map.Entry<String, JsonNode>> iter = objectNode.fields();
            String pathPrefix = currentPath.isEmpty() ? "" : currentPath + "/";

            while (iter.hasNext()) {
                Map.Entry<String, JsonNode> entry = iter.next();
                addTags(baseUrl, pathPrefix + entry.getKey(), entry.getValue(), map);
            }
        } else if (jsonNode.isArray()) {
            try {
                ArrayNode arrayNode = (ArrayNode) jsonNode;
                Iterator<JsonNode> elements = arrayNode.elements();

                if (elements.hasNext()) {
                    JsonNode element = elements.next();
                    //System.out.println("      Element: " +element.asText());
                    //System.out.println("        "+element.getNodeType());
                    if (element.isValueNode()) {
                        //System.out.println("          is value node");
                        if (element.isFloatingPointNumber()) {
                            buildFloatArray(currentPath, arrayNode);
                        } else if (element.isInt()){
                            buildIntArray(currentPath, arrayNode);
                        } else {
                            System.out.println(currentPath);
                            System.out.println("  token: "+element.asToken());
                            System.out.println("  nodeType: "+element.getNodeType());
                        }
                    } else if (element.isArray()){
                        buildDataSet(currentPath, arrayNode);
                    }
                }
            } catch (Exception e) {
                logger.error("nested arraynode: " + jsonNode.asText(), e);
            }
        } else if (jsonNode.isValueNode()) {
            ValueNode valueNode = (ValueNode) jsonNode;
            //logger.info("addKeys: valuenode: " + valueNode.asText());

            map.put(currentPath, valueNode.asText());

            Object valueObject =  mapper.convertValue(valueNode, Object.class);
            tagProvider.updateValue(currentPath, valueObject, QualityCode.Good);

//            WriteHandler handler = new ApiWriteHandler(baseUrl, jsonNode, mapper);
//
//            tagProvider.registerWriteHandler(currentPath, handler);
        }
    }

    private int[] buildIntArray(String currentPath, ArrayNode arrayNode) {
        logger.info("Building int array at " + currentPath);
        //System.out.println("  array size: "+arrayNode.size());
        int[] intObjs = new int[arrayNode.size()];
        Iterator<JsonNode> elements = arrayNode.elements();
        int i = 0;
        while (elements.hasNext()) {
            JsonNode element = elements.next();
            intObjs[i] = element.asInt();
            i += 1;
            //System.out.println(element.asText());
        }
        tagProvider.configureTag(currentPath, DataType.Int8Array);
        tagProvider.updateValue(currentPath, intObjs, QualityCode.Good);
        return intObjs;
    }

    private void buildFloatArray(String currentPath, ArrayNode arrayNode) {
        logger.info("Building float array at "+ currentPath);

        float[] floatObjs = new float[arrayNode.size()];
        Iterator<JsonNode> elements = arrayNode.elements();
        int i = 0;
        while (elements.hasNext()) {
            JsonNode element = elements.next();
            floatObjs[i] = element.floatValue();
            i += 1;
            //System.out.println(element.asText());
        }
        tagProvider.configureTag(currentPath, DataType.Float4Array);
        tagProvider.updateValue(currentPath, floatObjs, QualityCode.Good);
    }

    private void buildDataSet(String currentPath, ArrayNode arrayNode) {
        Object[][] data = new Object[2][2];

        Iterator<JsonNode> elements = arrayNode.elements();
        while (elements.hasNext()) {
            JsonNode element = elements.next();
            //System.out.println(element.asText());
            ArrayNode col = (ArrayNode) element;

        }

        logger.info("Building dataset at "+ currentPath);
        String[] columnNames = { "col1", "col2" };

        Class[] columnTypes = { Integer.class, Integer.class };

        BasicDataset ds = new BasicDataset(columnNames, columnTypes, data);

        tagProvider.configureTag(currentPath, DataType.DataSet);
        tagProvider.updateValue(currentPath, ds, QualityCode.Good);
    }

    private class ApiWriteHandler implements WriteHandler {

        String baseUrl;
        JsonNode jsonNode;
        ObjectMapper mapper;

        private ApiWriteHandler(String baseUrl, JsonNode jsonNode, ObjectMapper mapper) {
            this.baseUrl = baseUrl;
            this.jsonNode = jsonNode;
            this.mapper = mapper;
        }

        @Override
        public QualityCode write(TagPath tagPath, Object o) {
            logger.info("write handler valueNode: " + jsonNode.asText());
            logger.info(baseUrl);
            logger.info("tagPath" + tagPath.toString());

            TagPath parentPath = tagPath.getParentPath();
            logger.info("parentPath: " + parentPath);

            String hubName = parentPath.getPathComponent(0);
            logger.info("hubName: " + hubName);

            String bodyItem = tagPath.getParentPath().getLastPathComponent();
            logger.info("body item: " + bodyItem);

            String urlPath = parentPath.toStringFull().replace("[Hue]", "");
            logger.info("urlPath: " + urlPath);
            urlPath = urlPath.replace(hubName+"/", "");
            logger.info("urlPath: " + urlPath);

            String itemName = tagPath.getItemName();
            logger.info("itemName: " + itemName);
            String last = tagPath.getLastPathComponent();
            logger.info("lastPath: " + last);

            Map<String, Object> map = new HashMap<>();
            map.put(bodyItem, o);

            try {

                String reqBody2 = "{ \"" + itemName + "\": " + o.toString() + "}";
                logger.info(reqBody2);
                HttpRequest writeRequest = HttpRequest.newBuilder()
                        .uri(URI.create(baseUrl + "/" + urlPath))
                        .PUT(HttpRequest.BodyPublishers.ofString(reqBody2))
                        .build();

                HttpResponse<String> writeResponse = httpClient.send(writeRequest, HttpResponse.BodyHandlers.ofString());
                String responseBody = writeResponse.body();
                logger.info("responsebody: " + responseBody);

                int responseStatusCode = writeResponse.statusCode();
                logger.info("statusCode: " + String.valueOf(responseStatusCode));
                tagProvider.updateValue(tagPath.toStringFull(), o, QualityCode.Good);
                return QualityCode.Good;
            } catch (JsonProcessingException e) {
                e.printStackTrace();
                logger.error("error:", e);
            } catch (InterruptedException e) {
                e.printStackTrace();
                logger.error("error:", e);
            } catch (IOException e) {
                e.printStackTrace();
                logger.error("error:", e);
            }

            return QualityCode.Bad;
        }
    }

    public void shutDown() {
        tagProvider.shutdown(true);
    }
}