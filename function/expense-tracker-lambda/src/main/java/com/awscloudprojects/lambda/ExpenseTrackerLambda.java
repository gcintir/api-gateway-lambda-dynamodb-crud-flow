package com.awscloudprojects.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.awscloudprojects.model.Expense;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemResponse;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemResponse;
import software.amazon.lambda.powertools.logging.Logging;

public class ExpenseTrackerLambda implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    private static final Logger logger = LoggerFactory.getLogger(ExpenseTrackerLambda.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final String DDB_TABLE_NAME = "expenses";
    private static final DynamoDbClient dynamoDbClient = DynamoDbClient.builder().region(Region.US_EAST_1).build();
    @Override
    @Logging(clearState = true)
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {
        APIGatewayProxyResponseEvent apiGatewayProxyResponseEvent = new APIGatewayProxyResponseEvent();
        apiGatewayProxyResponseEvent.setHeaders(
                Map.of(
                        "Access-Control-Allow-Origin", "*",
                        "Access-Control-Allow-Methods", "*",
                        "Access-Control-Allow-Headers", "*"));
        try {
            logger.info("received http method:{}", event.getHttpMethod());
            if (event.getHttpMethod().equals("POST")) {
                Expense expense = objectMapper.readValue(event.getBody(), Expense.class);
                createExpense(expense);
                apiGatewayProxyResponseEvent.setBody("Expense created");
                apiGatewayProxyResponseEvent.setStatusCode(200);
            } else if (event.getHttpMethod().equals("PUT")) {

            } else if (event.getHttpMethod().equals("GET")) {
                Map<String, String> queryParametersMap = event.getQueryStringParameters();
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
            } else if (event.getHttpMethod().equals("DELETE")) {
                Map<String, String> pathParameters = event.getPathParameters();
                String userId = Objects.nonNull(pathParameters.get("userId")) ? pathParameters.get("userId") : null;
                String creationTime = Objects.nonNull(pathParameters.get("creationTime")) ? pathParameters.get("creationTime") : null;
                logger.info("received userId:{} creationTime:{}", userId, creationTime);
                deleteExpenseByUserIdAndCreationTime(userId, creationTime);
                apiGatewayProxyResponseEvent.setBody("expense deleted for userId:" + userId + " creationTime:" + creationTime);
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

    private Expense createExpense (Expense expense) {
        try {
            HashMap<String, AttributeValue> itemMap = new HashMap<>();
            itemMap.put("user_id", AttributeValue.builder().s(expense.getUserId()).build());
            itemMap.put("creation_time", AttributeValue.builder().n(expense.getCreationTime() + "").build());
            itemMap.put("category", AttributeValue.builder().s(expense.getCategory()).build());
            itemMap.put("cost", AttributeValue.builder().n(expense.getCost() + "").build());
            itemMap.put("description", AttributeValue.builder().s(expense.getDescription()).build());
            itemMap.put("status", AttributeValue.builder().s(expense.getStatus()).build());

            PutItemRequest putItemRequest = PutItemRequest.builder()
                    .tableName(DDB_TABLE_NAME)
                    .item(itemMap)
                    .build();

            PutItemResponse putItemResponse = dynamoDbClient.putItem(putItemRequest);
            logger.info("Expense creation completed with statusCode:{} requestId:{}", putItemResponse.sdkHttpResponse().statusCode(), putItemResponse.responseMetadata().requestId());

            return expense;
        } catch (DynamoDbException e) {
            logger.error("DynamoDB exception", e);
            throw new RuntimeException(e);
        }
    }

    private void deleteExpenseByUserIdAndCreationTime(String userId, String creationTime) {
        try {
            HashMap<String, AttributeValue> keyMap = new HashMap<>();
            keyMap.put("user_id", AttributeValue.builder().s(userId).build());
            keyMap.put("creation_time", AttributeValue.builder().n(creationTime).build());

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



}
