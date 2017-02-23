package io.symphonia;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Timer;
import io.symphonia.lambda.annotations.CloudwatchMetric;
import io.symphonia.lambda.metrics.LambdaMetricSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class FibonacciLambda {

    private static final int THREADS = 1;
    private static final Long FIBONACCI_PARAMETER = 30L;
    private static final Long FIBONACCI_ITERATIONS = 500L;
    private static Logger LOG = LoggerFactory.getLogger(FibonacciLambda.class);

    private ExecutorService executorService = Executors.newFixedThreadPool(THREADS);

    class Metrics extends LambdaMetricSet {
        @CloudwatchMetric
        Gauge jvmUptime = JVM_UPTIME;

        @CloudwatchMetric
        Timer fibonacciTimer = new Timer();
    }

    public void handler(CloudwatchEvent event) throws ExecutionException, InterruptedException {
        LOG.info("Received event = [{}]", event.getId());
        final Metrics metrics = new Metrics();

        // Pass Mapped Diagnostic Context (MDC) into child thread, for logging Request ID
        final Map<String, String> contextMap = MDC.getCopyOfContextMap();
        Runnable task = () -> {
            if (contextMap != null) {
                MDC.setContextMap(contextMap);
            }
            LOG.info("Starting fibonacci task");
            timedFibonacci(metrics);
        };

        // Start one task in the background, and one in this thread
        Future<?> future = executorService.submit(task);
        task.run();

        // Wait for background task, log out metrics
        future.get();
        metrics.report(LOG);
    }

    private void timedFibonacci(Metrics metrics) {
        for (int i = 0; i < FIBONACCI_ITERATIONS; i++) {
            try {
                metrics.fibonacciTimer.time(this::fibonacci);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private Long fibonacci() {
        return fibonacci(FIBONACCI_PARAMETER);
    }

    private Long fibonacci(Long n) {
        if (n == 0 || n == 1) {
            return n;
        } else {
            return fibonacci(n - 1) + fibonacci(n - 2);
        }
    }
}
