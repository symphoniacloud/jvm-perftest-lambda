package io.symphonia;

import org.junit.Test;

public class FibonacciLambdaTest {

    @Test
    public void handler() throws Exception {
        CloudwatchEvent event = new CloudwatchEvent();
        event.setId("ABC123");
        FibonacciLambda lambda = new FibonacciLambda();
        lambda.handler(event);
    }

}