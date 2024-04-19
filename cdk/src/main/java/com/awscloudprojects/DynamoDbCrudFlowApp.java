package com.awscloudprojects;

import software.amazon.awscdk.App;
import software.amazon.awscdk.StackProps;

public class DynamoDbCrudFlowApp {
    public static void main(final String[] args) {
        App app = new App();

        new DynamoDbCrudFlowStack(app, "DynamoDbCrudFlowStack", StackProps.builder().build());
        app.synth();
    }
}

