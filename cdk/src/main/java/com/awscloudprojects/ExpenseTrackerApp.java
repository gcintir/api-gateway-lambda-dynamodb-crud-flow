package com.awscloudprojects;

import software.amazon.awscdk.App;
import software.amazon.awscdk.StackProps;

public class ExpenseTrackerApp {
    public static void main(final String[] args) {
        App app = new App();

        new ExpenseTrackerStack(app, "ExpenseTrackerStack", StackProps.builder().build());
        app.synth();
    }
}

