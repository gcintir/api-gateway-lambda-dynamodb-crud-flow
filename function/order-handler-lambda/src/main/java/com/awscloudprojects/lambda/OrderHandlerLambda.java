package com.awscloudprojects.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.awscloudprojects.model.Order;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemResponse;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;
import software.amazon.awssdk.services.dynamodb.model.Select;
import software.amazon.lambda.powertools.logging.Logging;

public class OrderHandlerLambda implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    private static final Logger logger = LoggerFactory.getLogger(OrderHandlerLambda.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final String DDB_TABLE_NAME = "orders";
    private static final DynamoDbClient dynamoDbClient = DynamoDbClient.builder().region(Region.US_EAST_1).build();
    @Override
    @Logging(clearState = true)
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {
        APIGatewayProxyResponseEvent apiGatewayProxyResponseEvent = new APIGatewayProxyResponseEvent();
        try {
            logger.info("received http method:{}", event.getHttpMethod());
            if (event.getHttpMethod().equals("POST")) {
                Order order = objectMapper.readValue(event.getBody(), Order.class);
                saveOrder(order);
                apiGatewayProxyResponseEvent.setBody("Order saved with id:" + order.getOrderId());
                apiGatewayProxyResponseEvent.setStatusCode(200);
            } else if (event.getHttpMethod().equals("GET")) {

                if (event.getPath().contains("order/prices")) {
                    Map<String, String> pathParametersMap = event.getPathParameters();
                    String userId = Objects.nonNull(pathParametersMap.get("userId")) ? pathParametersMap.get("userId") : null;
                    List<Float> prices = getOrderPricesByUserId(userId);
                    apiGatewayProxyResponseEvent.setBody(objectMapper.writeValueAsString(prices));
                    apiGatewayProxyResponseEvent.setStatusCode(200);
                } else {
                    Map<String, String> queryParametersMap = event.getQueryStringParameters();
                    String userId = null;
                    String minPrice = null;
                    String maxPrice = null;
                    if (queryParametersMap != null) {
                        userId = Objects.nonNull(queryParametersMap.get("userId")) ? queryParametersMap.get("userId") : null;
                        minPrice = Objects.nonNull(queryParametersMap.get("minPrice")) ? queryParametersMap.get("minPrice") : null;
                        maxPrice = Objects.nonNull(queryParametersMap.get("maxPrice")) ? queryParametersMap.get("maxPrice") : null;
                    }
                    List<Order> orders;
                    if (minPrice == null && userId == null) {
                        orders = getOrders();
                    } else if (minPrice == null) {
                        orders = getOrdersByUserId(userId);
                    } else {
                        orders = getOrdersByUserIdAndPriceRange(userId, Float.parseFloat(minPrice), Float.parseFloat(maxPrice));
                    }
                    apiGatewayProxyResponseEvent.setBody(objectMapper.writeValueAsString(orders));
                    apiGatewayProxyResponseEvent.setStatusCode(200);
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
            apiGatewayProxyResponseEvent.setBody(e.getMessage());
            apiGatewayProxyResponseEvent.setStatusCode(500);
        }
        return apiGatewayProxyResponseEvent;
    }

    private Order saveOrder (Order order) {
        try {
            HashMap<String, AttributeValue> itemMap = new HashMap<>();
            itemMap.put("user_id", AttributeValue.builder().s(order.getUserId()).build());
            itemMap.put("order_id", AttributeValue.builder().s(order.getOrderId()).build());
            itemMap.put("price", AttributeValue.builder().n(String.valueOf(order.getPrice())).build());
            itemMap.put("created_at", AttributeValue.builder().n(String.valueOf(new Date().getTime())).build());

            PutItemRequest putItemRequest = PutItemRequest.builder()
                    .tableName(DDB_TABLE_NAME)
                    .item(itemMap)
                    .conditionExpression("attribute_not_exists(#ui) AND attribute_not_exists(#oi)")
                    .expressionAttributeNames(Map.of("#ui", "user_id",
                                                     "#oi", "order_id"))
                    .build();

            PutItemResponse putItemResponse = dynamoDbClient.putItem(putItemRequest);
            logger.info("Order saved with id:{} statusCode:{} requestId:{}", order.getOrderId(), putItemResponse.sdkHttpResponse().statusCode(), putItemResponse.responseMetadata().requestId());

            return order;
        } catch (DynamoDbException e) {
            logger.error("DynamoDB exception", e);
            throw new RuntimeException(e);
        }
    }

    public List<Order> getOrdersByUserId (String userId) {
        List<Order> orders = new ArrayList<>();
        try {
            QueryRequest queryRequest = QueryRequest.builder()
                    .tableName(DDB_TABLE_NAME)
                    .keyConditionExpression("#ui = :uiValue")
                    .expressionAttributeNames(Map.of("#ui", "user_id"))
                    .expressionAttributeValues(Map.of(":uiValue", AttributeValue.builder().s(userId).build()))
                    .build();

            QueryResponse queryResponse = dynamoDbClient.query(queryRequest);
            if (queryResponse.hasItems()) {
                queryResponse.items().forEach(itemData -> {
                    Order order = new Order(itemData.get("user_id").s(), itemData.get("order_id").s(), Float.parseFloat(itemData.get("price").n()), Long.parseLong(itemData.get("created_at").n()));
                    orders.add(order);
                });
            }

        } catch (DynamoDbException e) {
            logger.error("DynamoDB exception", e);
            throw new RuntimeException(e);
        }
        return orders;
    }

    public List<Float> getOrderPricesByUserId (String userId) {
        List<Float> orderPrices = new ArrayList<>();
        try {
            QueryRequest queryRequest = QueryRequest.builder()
                    .tableName(DDB_TABLE_NAME)
                    .keyConditionExpression("#ui = :uiValue")
                    .expressionAttributeNames(Map.of("#ui", "user_id"))
                    .expressionAttributeValues(Map.of(":uiValue", AttributeValue.builder().s(userId).build()))
                    .projectionExpression("price")
                    .build();

            QueryResponse queryResponse = dynamoDbClient.query(queryRequest);
            if (queryResponse.hasItems()) {
                queryResponse.items().forEach(itemData -> {
                    orderPrices.add(Float.parseFloat(itemData.get("price").n()));
                });
            }

        } catch (DynamoDbException e) {
            logger.error("DynamoDB exception", e);
            throw new RuntimeException(e);
        }
        return orderPrices;
    }

    public List<Order> getOrdersByUserIdAndPriceRange (String userId, float minPrice, float maxPrice) {
        List<Order> orders = new ArrayList<>();
        try {
            QueryRequest queryRequest = QueryRequest.builder()
                    .tableName(DDB_TABLE_NAME)
                    .keyConditionExpression("#ui = :uiValue")
                    .filterExpression("#p BETWEEN :min_p AND :max_p")
                    .expressionAttributeNames(Map.of("#ui", "user_id",
                                                     "#p", "price"))
                    .expressionAttributeValues(Map.of(":uiValue", AttributeValue.builder().s(userId).build(),
                                                      ":min_p", AttributeValue.builder().n(String.valueOf(minPrice)).build(),
                                                      ":max_p", AttributeValue.builder().n(String.valueOf(maxPrice)).build()))
                    .build();

            QueryResponse queryResponse = dynamoDbClient.query(queryRequest);
            if (queryResponse.hasItems()) {
                queryResponse.items().forEach(itemData -> {
                    Order order = new Order(itemData.get("user_id").s(), itemData.get("order_id").s(), Float.parseFloat(itemData.get("price").n()), Long.parseLong(itemData.get("created_at").n()));
                    orders.add(order);
                });
            }

        } catch (DynamoDbException e) {
            logger.error("DynamoDB exception", e);
            throw new RuntimeException(e);
        }
        return orders;
    }

    public List<Order> getOrders () {
        List<Order> orders = new ArrayList<>();
        try {
            ScanRequest scanRequest = ScanRequest.builder()
                    .tableName(DDB_TABLE_NAME)
                    .build();

            ScanResponse scanResponse = dynamoDbClient.scan(scanRequest);
            if (scanResponse.hasItems()) {
                scanResponse.items().forEach(itemData -> {
                    Order order = new Order(itemData.get("user_id").s(), itemData.get("order_id").s(), Float.parseFloat(itemData.get("price").n()), Long.parseLong(itemData.get("created_at").n()));
                    orders.add(order);
                });
            }
        } catch (DynamoDbException e) {
            logger.error("DynamoDB exception", e);
            throw new RuntimeException(e);
        }
        return orders;
    }

}
