package com.awscloudprojects;

import java.util.List;
import software.amazon.awscdk.Duration;
import software.amazon.awscdk.RemovalPolicy;
import software.amazon.awscdk.aws_apigatewayv2_integrations.HttpLambdaIntegration;
import software.amazon.awscdk.services.apigatewayv2.AddRoutesOptions;
import software.amazon.awscdk.services.apigatewayv2.HttpApi;
import software.amazon.awscdk.services.apigatewayv2.HttpMethod;
import software.amazon.awscdk.services.apigatewayv2.PayloadFormatVersion;
import software.amazon.awscdk.services.dynamodb.Attribute;
import software.amazon.awscdk.services.dynamodb.AttributeType;
import software.amazon.awscdk.services.dynamodb.BillingMode;
import software.amazon.awscdk.services.dynamodb.Table;
import software.amazon.awscdk.services.iam.ManagedPolicy;
import software.amazon.awscdk.services.iam.Role;
import software.amazon.awscdk.services.iam.ServicePrincipal;
import software.amazon.awscdk.services.lambda.Code;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.Runtime;
import software.amazon.awscdk.services.logs.LogGroup;
import software.amazon.awscdk.services.logs.RetentionDays;
import software.constructs.Construct;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;

public class DynamoDbCrudFlowStack extends Stack {
    public DynamoDbCrudFlowStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        Role executionRole = Role.Builder.create(this, id + "-ProductHandlerLambdaRole")
                .assumedBy(new ServicePrincipal("lambda.amazonaws.com"))
                .roleName(id + "-ProductHandlerLambdaRole")
                .build();
        executionRole.addManagedPolicy(ManagedPolicy.fromAwsManagedPolicyName("service-role/AWSLambdaBasicExecutionRole"));

        String functionName = id + "-ProductHandlerLambda";
        Function productHandlerLambda = Function.Builder.create(this, functionName)
                .runtime(Runtime.JAVA_21)
                .code(Code.fromAsset("../function/product-handler-lambda/target/product-handler-lambda.jar"))
                .handler("com.awscloudprojects.lambda.ProductHandlerLambda::handleRequest")
                .functionName(functionName)
                .role(executionRole)
                .timeout(Duration.seconds(30L))
                .memorySize(512)
                .logGroup(LogGroup.Builder.create(this, functionName + "-logGroup")
                        .logGroupName("/aws/lambda/" + functionName)
                        .retention(RetentionDays.ONE_DAY)
                        .removalPolicy(RemovalPolicy.DESTROY)
                        .build())
                .build();

        Table dynamoDbTable = Table.Builder.create(this, "dynamodb-table")
                .tableName("products")
                .partitionKey(Attribute.builder().name("product_id").type(AttributeType.STRING).build())
                .billingMode(BillingMode.PAY_PER_REQUEST)
                .removalPolicy(RemovalPolicy.DESTROY)
                .build();

        /*

        dynamoDbTable.addGlobalSecondaryIndex(GlobalSecondaryIndexProps.builder()
                .indexName("gsi-creationTimeUserId")
                .partitionKey(Attribute.builder().name("creation_time").type(AttributeType.NUMBER).build())
                .sortKey(Attribute.builder().name("user_id").type(AttributeType.STRING).build())
                .projectionType(ProjectionType.ALL)
                .build());

        dynamoDbTable.addLocalSecondaryIndex(LocalSecondaryIndexProps.builder()
                        .indexName("lsi-cost")
                        .sortKey(Attribute.builder().name("cost").type(AttributeType.NUMBER).build())
                .build());

        dynamoDbTable.addLocalSecondaryIndex(LocalSecondaryIndexProps.builder()
                .indexName("lsi-status")
                .sortKey(Attribute.builder().name("status").type(AttributeType.STRING).build())
                .build());

         */

        dynamoDbTable.grantFullAccess(productHandlerLambda);

        HttpApi httpApi = HttpApi.Builder.create(this, "http-api-gateway")
                .apiName(id + "-http-api-gateway")
                .description("HTTP API Gateway")
                .build();

        httpApi.addRoutes(AddRoutesOptions.builder()
                        .path("/product")
                        .integration(HttpLambdaIntegration.Builder.create("product-handler-lambda-integration", productHandlerLambda)
                                .payloadFormatVersion(PayloadFormatVersion.VERSION_1_0)
                                .build())
                .methods(List.of(HttpMethod.POST))
                .build());

        httpApi.addRoutes(AddRoutesOptions.builder()
                .path("/product")
                .integration(HttpLambdaIntegration.Builder.create("product-handler-lambda-integration", productHandlerLambda)
                        .payloadFormatVersion(PayloadFormatVersion.VERSION_1_0)
                        .build())
                .methods(List.of(HttpMethod.PUT))
                .build());

        httpApi.addRoutes(AddRoutesOptions.builder()
                .path("/product")
                .integration(HttpLambdaIntegration.Builder.create("product-handler-lambda-integration", productHandlerLambda)
                        .payloadFormatVersion(PayloadFormatVersion.VERSION_1_0)
                        .build())
                .methods(List.of(HttpMethod.GET))
                .build());

        httpApi.addRoutes(AddRoutesOptions.builder()
                .path("/product/{productId}")
                .integration(HttpLambdaIntegration.Builder.create("product-handler-lambda-integration", productHandlerLambda)
                        .payloadFormatVersion(PayloadFormatVersion.VERSION_1_0)
                        .build())
                .methods(List.of(HttpMethod.DELETE))
                .build());

    }
}
