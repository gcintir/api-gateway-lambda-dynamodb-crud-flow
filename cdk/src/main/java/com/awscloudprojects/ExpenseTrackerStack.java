package com.awscloudprojects;

import java.util.List;
import software.amazon.awscdk.Duration;
import software.amazon.awscdk.RemovalPolicy;
import software.amazon.awscdk.aws_apigatewayv2_integrations.HttpLambdaIntegration;
import software.amazon.awscdk.services.apigateway.LambdaIntegration;
import software.amazon.awscdk.services.apigatewayv2.AddRoutesOptions;
import software.amazon.awscdk.services.apigatewayv2.HttpApi;
import software.amazon.awscdk.services.apigatewayv2.HttpMethod;
import software.amazon.awscdk.services.apigatewayv2.HttpRouteIntegration;
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

public class ExpenseTrackerStack extends Stack {
    public ExpenseTrackerStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        Role executionRole = Role.Builder.create(this, id + "-CreateOrderLambdaRole")
                .assumedBy(new ServicePrincipal("lambda.amazonaws.com"))
                .roleName(id + "-CreateOrderLambdaRole")
                .build();
        executionRole.addManagedPolicy(ManagedPolicy.fromAwsManagedPolicyName("service-role/AWSLambdaBasicExecutionRole"));

        String functionName = id + "-ExpenseTrackerLambda";
        Function expenseTrackerLambda = Function.Builder.create(this, functionName)
                .runtime(Runtime.JAVA_21)
                .code(Code.fromAsset("../function/expense-tracker-lambda/target/expense-tracker-lambda.jar"))
                .handler("com.awscloudprojects.lambda.ExpenseTrackerLambda::handleRequest")
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

        HttpApi httpApi = HttpApi.Builder.create(this, "http-api-gateway")
                .apiName(id + "-http-api-gateway")
                .description("HTTP API Gateway")
                .build();

        httpApi.addRoutes(AddRoutesOptions.builder()
                        .path("/expense")
                        .integration(HttpLambdaIntegration.Builder.create("expense-tracker-lambda-integration", expenseTrackerLambda).build())
                .methods(List.of(HttpMethod.POST))
                .build());

    }
}
