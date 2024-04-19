package com.awscloudprojects.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.awscloudprojects.model.Expense;
import com.awscloudprojects.model.Product;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeAction;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.AttributeValueUpdate;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemResponse;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemResponse;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemResponse;
import software.amazon.lambda.powertools.logging.Logging;

public class ProductHandlerLambda implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    private static final Logger logger = LoggerFactory.getLogger(ProductHandlerLambda.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final String DDB_TABLE_NAME = "products";
    private static final DynamoDbClient dynamoDbClient = DynamoDbClient.builder().region(Region.US_EAST_1).build();
    @Override
    @Logging(clearState = true)
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {
        APIGatewayProxyResponseEvent apiGatewayProxyResponseEvent = new APIGatewayProxyResponseEvent();
        try {
            logger.info("received http method:{}", event.getHttpMethod());
            if (event.getHttpMethod().equals("POST")) {
                Product product = objectMapper.readValue(event.getBody(), Product.class);
                saveProduct(product);
                apiGatewayProxyResponseEvent.setBody("Product saved with id:" + product.getProductId());
                apiGatewayProxyResponseEvent.setStatusCode(200);
            } else if (event.getHttpMethod().equals("PUT")) {
                Product product = objectMapper.readValue(event.getBody(), Product.class);
                updateProduct(product);
                apiGatewayProxyResponseEvent.setBody("Product updated with id:" + product.getProductId());
                apiGatewayProxyResponseEvent.setStatusCode(200);
            } else if (event.getHttpMethod().equals("GET")) {
                Map<String, String> queryParametersMap = event.getQueryStringParameters();
                String productId = Objects.nonNull(queryParametersMap.get("productId")) ? queryParametersMap.get("productId") : null;
                logger.info("productId:{}", productId);
                Optional<Product> productOpt = getProductById(productId);
                if (productOpt.isPresent()) {
                    apiGatewayProxyResponseEvent.setBody(objectMapper.writeValueAsString(productOpt.get()));
                    apiGatewayProxyResponseEvent.setStatusCode(200);
                } else {
                    apiGatewayProxyResponseEvent.setBody("Product not found with id:" + productId);
                    apiGatewayProxyResponseEvent.setStatusCode(404);
                }

            } else if (event.getHttpMethod().equals("DELETE")) {
                Map<String, String> pathParameters = event.getPathParameters();
                String productId = Objects.nonNull(pathParameters.get("productId")) ? pathParameters.get("productId") : null;
                deleteProductById(productId);
                apiGatewayProxyResponseEvent.setBody("Product removed with id:" + productId);
                apiGatewayProxyResponseEvent.setStatusCode(200);
            } else {

            }
        } catch (Exception e) {
            logger.error(e.getMessage());
            apiGatewayProxyResponseEvent.setBody(e.getMessage());
            apiGatewayProxyResponseEvent.setStatusCode(500);
        }
        return apiGatewayProxyResponseEvent;
    }

    private Product saveProduct (Product product) {
        try {
            HashMap<String, AttributeValue> itemMap = new HashMap<>();
            itemMap.put("product_id", AttributeValue.builder().s(product.getProductId()).build());
            itemMap.put("title", AttributeValue.builder().s(product.getTitle()).build());
            itemMap.put("category", AttributeValue.builder().s(product.getCategory()).build());
            itemMap.put("price", AttributeValue.builder().n(String.valueOf(product.getPrice())).build());
            itemMap.put("allowed_credit_cards", AttributeValue.builder().ss(product.getAllowedCreditCards()).build());
            itemMap.put("existing_sizes", AttributeValue.builder().ns(product.getExistingSizes().stream().map(Object::toString).collect(Collectors.toList())).build());
            itemMap.put("stock_amount", AttributeValue.builder().n(String.valueOf(product.getStockAmount())).build());
            itemMap.put("active", AttributeValue.builder().bool(product.isActive()).build());
            itemMap.put("features", AttributeValue.builder().m(product.getFeatures().entrySet().stream().collect(
                    Collectors.toMap(Map.Entry::getKey, entry -> AttributeValue.builder().s(entry.getValue()).build())))
                    .build());
            itemMap.put("created_at", AttributeValue.builder().n(String.valueOf(new Date().getTime())).build());

            PutItemRequest putItemRequest = PutItemRequest.builder()
                    .tableName(DDB_TABLE_NAME)
                    .item(itemMap)
                    .conditionExpression("attribute_not_exists(#pi)")
                    .expressionAttributeNames(Map.of("#pi", "product_id"))
                    .build();

            PutItemResponse putItemResponse = dynamoDbClient.putItem(putItemRequest);
            logger.info("Product saved with id:{} statusCode:{} requestId:{}", product.getProductId(), putItemResponse.sdkHttpResponse().statusCode(), putItemResponse.responseMetadata().requestId());

            return product;
        } catch (DynamoDbException e) {
            logger.error("DynamoDB exception", e);
            throw new RuntimeException(e);
        }
    }

    private Product updateProduct (Product product) {
        try {
            HashMap<String, AttributeValue> itemMap = new HashMap<>();
            itemMap.put("product_id", AttributeValue.builder().s(product.getProductId()).build());
            itemMap.put("title", AttributeValue.builder().s(product.getTitle()).build());
            itemMap.put("category", AttributeValue.builder().s(product.getCategory()).build());
            itemMap.put("price", AttributeValue.builder().n(String.valueOf(product.getPrice())).build());
            itemMap.put("allowed_credit_cards", AttributeValue.builder().ss(product.getAllowedCreditCards()).build());
            itemMap.put("existing_sizes", AttributeValue.builder().ns(product.getExistingSizes().stream().map(Object::toString).collect(Collectors.toList())).build());
            itemMap.put("stock_amount", AttributeValue.builder().n(String.valueOf(product.getStockAmount())).build());
            itemMap.put("active", AttributeValue.builder().bool(product.isActive()).build());
            itemMap.put("features", AttributeValue.builder().m(product.getFeatures().entrySet().stream().collect(
                            Collectors.toMap(Map.Entry::getKey, entry -> AttributeValue.builder().s(entry.getValue()).build())))
                    .build());
            itemMap.put("created_at", AttributeValue.builder().n(String.valueOf(new Date().getTime())).build());

            PutItemRequest putItemRequest = PutItemRequest.builder()
                    .tableName(DDB_TABLE_NAME)
                    .item(itemMap)
                    .build();

            PutItemResponse putItemResponse = dynamoDbClient.putItem(putItemRequest);
            logger.info("Product updated with id:{} statusCode:{} requestId:{}", product.getProductId(), putItemResponse.sdkHttpResponse().statusCode(), putItemResponse.responseMetadata().requestId());

            return product;
        } catch (DynamoDbException e) {
            logger.error("DynamoDB exception", e);
            throw new RuntimeException(e);
        }
    }

    private Optional<Product> getProductById (String productId) {
        try {
            HashMap<String, AttributeValue> keyMap = new HashMap<>();
            keyMap.put("product_id", AttributeValue.builder().s(productId).build());

            GetItemRequest getItemRequest = GetItemRequest.builder()
                    .tableName(DDB_TABLE_NAME)
                    .key(keyMap)
                    .build();

            GetItemResponse getItemResponse = dynamoDbClient.getItem(getItemRequest);
            logger.info("Product retrieve operation completed with statusCode:{} requestId:{}", getItemResponse.sdkHttpResponse().statusCode(), getItemResponse.responseMetadata().requestId());

            if (!getItemResponse.hasItem()) {
                logger.info("Product record not found with productId:{}", productId);
                return Optional.empty();
            }

            Map<String, AttributeValue> item = getItemResponse.item();
            Product product = new Product();
            product.setProductId(item.get("product_id").s());
            product.setTitle(item.get("title").s());
            product.setCategory(item.get("category").s());
            product.setPrice(Float.parseFloat(item.get("price").n()));
            product.setAllowedCreditCards(new HashSet<>(item.get("allowed_credit_cards").ss()));
            product.setExistingSizes(new HashSet<>(item.get("existing_sizes").ns().stream().map(Integer::valueOf).collect(Collectors.toList())));
            product.setStockAmount(Integer.parseInt(item.get("stock_amount").n()));
            product.setActive(item.get("active").bool());
            product.setFeatures(item.get("features").m().entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().s())));
            product.setCreatedAt(Long.parseLong(item.get("created_at").n()));

            return Optional.of(product);

        } catch (DynamoDbException e) {
            logger.error("DynamoDB exception", e);
            throw new RuntimeException(e);
        }
    }

    private void deleteProductById (String productId) {
        try {
            HashMap<String, AttributeValue> keyMap = new HashMap<>();
            keyMap.put("product_id", AttributeValue.builder().s(productId).build());

            DeleteItemRequest deleteItemRequest = DeleteItemRequest.builder()
                    .tableName(DDB_TABLE_NAME)
                    .key(keyMap)
                    .build();

            DeleteItemResponse deleteItemResponse = dynamoDbClient.deleteItem(deleteItemRequest);
            logger.info("Product deleted with statusCode:{} requestId:{}", deleteItemResponse.sdkHttpResponse().statusCode(), deleteItemResponse.responseMetadata().requestId());

        } catch (DynamoDbException e) {
            logger.error("DynamoDB exception", e);
            throw new RuntimeException(e);
        }
    }
}
