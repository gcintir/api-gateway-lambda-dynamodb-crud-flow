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

                /*
                if (accessType.equals("expenseByUserIdAndCreationTime")) {
                    String userId = Objects.nonNull(queryParametersMap.get("userId")) ? queryParametersMap.get("userId") : null;
                    String creationTime = Objects.nonNull(queryParametersMap.get("creationTime")) ? queryParametersMap.get("creationTime") : null;

                    Optional<Expense> expenseOptional = getExpenseByUserIdAndCreationTime(userId, creationTime);
                    if (expenseOptional.isPresent()) {
                        apiGatewayProxyResponseEvent.setBody(objectMapper.writeValueAsString(expenseOptional.get()));
                        apiGatewayProxyResponseEvent.setStatusCode(200);
                    } else {
                        apiGatewayProxyResponseEvent.setBody("expense not found for userId:" + userId + " creationTime:" + creationTime);
                        apiGatewayProxyResponseEvent.setStatusCode(404);
                    }
                } else if (accessType.equals("expensesByUserId")) {
                    String userId = Objects.nonNull(queryParametersMap.get("userId")) ? queryParametersMap.get("userId") : null;
                    List<Expense> expenseList = getExpenseListByUserId(userId);
                    apiGatewayProxyResponseEvent.setBody(objectMapper.writeValueAsString(expenseList));
                    apiGatewayProxyResponseEvent.setStatusCode(200);
                } else if (accessType.equals("expensesByUserIdAndCreationTimeRange")) {
                    String userId = Objects.nonNull(queryParametersMap.get("userId")) ? queryParametersMap.get("userId") : null;
                    String creationTimeMin = Objects.nonNull(queryParametersMap.get("creationTimeMin")) ? queryParametersMap.get("creationTimeMin") : null;
                    String creationTimeMax = Objects.nonNull(queryParametersMap.get("creationTimeMax")) ? queryParametersMap.get("creationTimeMax") : null;

                    List<Expense> expenseList = getExpenseListByUserIdAndCreationTimeRange(userId, creationTimeMin, creationTimeMax);
                    apiGatewayProxyResponseEvent.setBody(objectMapper.writeValueAsString(expenseList));
                    apiGatewayProxyResponseEvent.setStatusCode(200);
                }

                 */

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

    private Expense createExpense (Expense expense) {
        try {
            HashMap<String, AttributeValue> itemMap = new HashMap<>();
            itemMap.put("user_id", AttributeValue.builder().s(expense.getUserId()).build());
            itemMap.put("id", AttributeValue.builder().s(UUID.randomUUID().toString()).build());
            itemMap.put("creation_time", AttributeValue.builder().n(new Date().getTime() + "").build());
            itemMap.put("category", AttributeValue.builder().s(expense.getCategory()).build());
            itemMap.put("cost", AttributeValue.builder().n(expense.getCost() + "").build());
            itemMap.put("description", AttributeValue.builder().s(expense.getDescription()).build());
            itemMap.put("status", AttributeValue.builder().s(expense.getStatus()).build());
            itemMap.put("tags", AttributeValue.builder().ss(expense.getTags()).build());

            PutItemRequest putItemRequest = PutItemRequest.builder()
                    .tableName(DDB_TABLE_NAME)
                    .item(itemMap)
                    .build();

            PutItemResponse putItemResponse = dynamoDbClient.putItem(putItemRequest);
            logger.info("Expense creation completed with id:{} statusCode:{} requestId:{}", expense.getId(), putItemResponse.sdkHttpResponse().statusCode(), putItemResponse.responseMetadata().requestId());

            return expense;
        } catch (DynamoDbException e) {
            logger.error("DynamoDB exception", e);
            throw new RuntimeException(e);
        }
    }

    private Expense updateExpense (Expense expense) {
        try {
            HashMap<String, AttributeValue> keyMap = new HashMap<>();
            keyMap.put("user_id", AttributeValue.builder().s(expense.getUserId()).build());
            keyMap.put("creation_time", AttributeValue.builder().n(expense.getCreationTime() + "").build());

            HashMap<String, AttributeValueUpdate> updatedValues = new HashMap<>();
            updatedValues.put("category", AttributeValueUpdate.builder()
                            .value(AttributeValue.builder()
                                    .s(expense.getCategory())
                                    .build())
                            .action(AttributeAction.PUT)
                    .build());

            updatedValues.put("cost", AttributeValueUpdate.builder()
                    .value(AttributeValue.builder()
                            .n(expense.getCost() + "")
                            .build())
                    .action(AttributeAction.PUT)
                    .build());

            updatedValues.put("description", AttributeValueUpdate.builder()
                    .value(AttributeValue.builder()
                            .s(expense.getDescription())
                            .build())
                    .action(AttributeAction.PUT)
                    .build());

            updatedValues.put("status", AttributeValueUpdate.builder()
                    .value(AttributeValue.builder()
                            .s(expense.getStatus())
                            .build())
                    .action(AttributeAction.PUT)
                    .build());

            UpdateItemRequest updateItemRequest = UpdateItemRequest.builder()
                    .tableName(DDB_TABLE_NAME)
                    .key(keyMap)
                    .attributeUpdates(updatedValues)
                    .build();

            UpdateItemResponse updateItemResponse = dynamoDbClient.updateItem(updateItemRequest);
            logger.info("Expense updated with statusCode:{} requestId:{}", updateItemResponse.sdkHttpResponse().statusCode(), updateItemResponse.responseMetadata().requestId());

            return expense;
        } catch (Exception e) {
            logger.error("DynamoDB exception", e);
            throw new RuntimeException(e);
        }
    }

    private void deleteExpenseByUserIdAndId(String userId, String id) {
        try {
            HashMap<String, AttributeValue> keyMap = new HashMap<>();
            keyMap.put("user_id", AttributeValue.builder().s(userId).build());
            keyMap.put("id", AttributeValue.builder().s(id).build());

            DeleteItemRequest deleteItemRequest = DeleteItemRequest.builder()
                    .tableName(DDB_TABLE_NAME)
                    .key(keyMap)
                    .build();

            DeleteItemResponse deleteItemResponse = dynamoDbClient.deleteItem(deleteItemRequest);
            logger.info("Expense deleted with statusCode:{} requestId:{}", deleteItemResponse.sdkHttpResponse().statusCode(), deleteItemResponse.responseMetadata().requestId());

        } catch (DynamoDbException e) {
            logger.error("DynamoDB exception", e);
            throw new RuntimeException(e);
        }

    }

    /*

    private Optional<Expense> getExpenseByUserIdAndCreationTime (String userId, String creationTime) {
        try {
            HashMap<String, AttributeValue> keyMap = new HashMap<>();
            keyMap.put("user_id", AttributeValue.builder().s(userId).build());
            keyMap.put("creation_time", AttributeValue.builder().n(creationTime).build());

            GetItemRequest getItemRequest = GetItemRequest.builder()
                    .tableName(DDB_TABLE_NAME)
                    .key(keyMap)
                    .build();

            GetItemResponse getItemResponse = dynamoDbClient.getItem(getItemRequest);
            logger.info("Expense retrieve operation completed with statusCode:{} requestId:{}", getItemResponse.sdkHttpResponse().statusCode(), getItemResponse.responseMetadata().requestId());

            if (!getItemResponse.hasItem()) {
                logger.info("Expense record not found with userId:{} creationTime:{}", userId, creationTime);
                return Optional.empty();
            }

            Map<String, AttributeValue> item = getItemResponse.item();
            Expense expense = new Expense();
            expense.setUserId(item.get("user_id").s());
            expense.setCreationTime(Long.valueOf(item.get("creation_time").n()));
            expense.setCategory(item.get("category").s());
            expense.setCost(Integer.valueOf(item.get("cost").n()));
            expense.setDescription(item.get("description").s());
            expense.setStatus(item.get("status").s());

            return Optional.of(expense);

        } catch (DynamoDbException e) {
            logger.error("DynamoDB exception", e);
            throw new RuntimeException(e);
        }
    }

     */

    /*
    private List<Expense> getExpenseListByUserId (String userId) {
        List<Expense> expenseList = new ArrayList<>();
        try {
            Condition userIdValue = Condition.builder()
                    .comparisonOperator(ComparisonOperator.EQ)
                    .attributeValueList(AttributeValue.builder()
                            .s(userId)
                            .build())
                    .build();

            QueryRequest queryRequest = QueryRequest.builder()
                    .tableName(DDB_TABLE_NAME)
                    .keyConditions(Map.of("user_id", userIdValue))
                    .build();

            QueryIterable queryIterableResponse = dynamoDbClient.queryPaginator(queryRequest);
            Iterator<QueryResponse> iteratorResponse = queryIterableResponse.iterator();
            if (iteratorResponse.hasNext()) {
                QueryResponse queryResponse = iteratorResponse.next();
                List<Map<String, AttributeValue>> items = queryResponse.items();
                if (items.isEmpty()) {
                    return expenseList;
                } else {
                    for (Map<String, AttributeValue> item : items) {
                        Expense expense = new Expense();
                        expense.setUserId(item.get("user_id").s());
                        expense.setCreationTime(Long.valueOf(item.get("creation_time").n()));
                        expense.setCategory(item.get("category").s());
                        expense.setCost(Integer.valueOf(item.get("cost").n()));
                        expense.setDescription(item.get("description").s());
                        expense.setStatus(item.get("status").s());
                        expenseList.add(expense);
                    }
                }
            } else {
                logger.info("No expense found for userId:{}", userId);
            }
            return expenseList;
        } catch (Exception e) {
            logger.error("DynamoDB exception", e);
            throw new RuntimeException(e);
        }
    }

     */

    /*
    private List<Expense> getExpenseListByUserIdAndCreationTimeRange (String userId, String creationTimeMin, String creationTimeMax) {
        List<Expense> expenseList = new ArrayList<>();
        try {

            QueryRequest queryRequest = QueryRequest.builder()
                    .tableName(DDB_TABLE_NAME)
                    .keyConditionExpression("#pk = :pk and #sk between :ctmin and :ctmax")
                    .expressionAttributeNames(Map.of(
                            "#pk", "user_id",
                            "#sk", "creation_time"))
                    .expressionAttributeValues(Map.of(
                            ":pk", AttributeValue.builder().s(userId).build(),
                            ":ctmin", AttributeValue.builder().n(creationTimeMin).build(),
                            ":ctmax", AttributeValue.builder().n(creationTimeMax).build()
                    ))
                    .build();

            QueryIterable queryIterableResponse = dynamoDbClient.queryPaginator(queryRequest);
            Iterator<QueryResponse> iteratorResponse = queryIterableResponse.iterator();
            if (iteratorResponse.hasNext()) {
                QueryResponse queryResponse = iteratorResponse.next();
                List<Map<String, AttributeValue>> items = queryResponse.items();
                if (items.isEmpty()) {
                    return expenseList;
                } else {
                    for (Map<String, AttributeValue> item : items) {
                        Expense expense = new Expense();
                        expense.setUserId(item.get("user_id").s());
                        expense.setCreationTime(Long.valueOf(item.get("creation_time").n()));
                        expense.setCategory(item.get("category").s());
                        expense.setCost(Integer.valueOf(item.get("cost").n()));
                        expense.setDescription(item.get("description").s());
                        expense.setStatus(item.get("status").s());
                        expenseList.add(expense);
                    }
                }
            } else {
                logger.info("No expense found for userId:{}", userId);
            }
            return expenseList;
        } catch (Exception e) {
            logger.error("DynamoDB exception", e);
            throw new RuntimeException(e);
        }
    }

     */



}
