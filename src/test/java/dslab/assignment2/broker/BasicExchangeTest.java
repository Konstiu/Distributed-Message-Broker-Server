package dslab.assignment2.broker;

import dslab.util.Constants;
import dslab.util.Global;
import dslab.util.helper.TelnetClientHelper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static dslab.util.CommandBuilder.EXIT;
import static dslab.util.CommandBuilder.SUBSCRIBE;
import static dslab.util.CommandBuilder.bind;
import static dslab.util.CommandBuilder.exchange;
import static dslab.util.CommandBuilder.queue;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class BasicExchangeTest extends BaseSingleBrokerTest {

    private TelnetClientHelper publisher;
    private TelnetClientHelper subscriber;

    private static final String exchangeName = "exchange-%s".formatted(Global.SECURE_STRING_GENERATOR.getSecureString());
    private static final String queueName = "queue-%s".formatted(Global.SECURE_STRING_GENERATOR.getSecureString());
    private static final String key = "key-%s".formatted(Global.SECURE_STRING_GENERATOR.getSecureString());

    @Override
    protected void initTelnetClientHelpers() throws IOException {
        publisher = new TelnetClientHelper(Constants.LOCALHOST, config.port());
        subscriber = new TelnetClientHelper(Constants.LOCALHOST, config.port());

        subscriber.connectAndReadResponse();
        publisher.connectAndReadResponse();
    }

    @Override
    protected void closeTelnetClientHelpers() throws IOException {
        publisher.disconnect();
        subscriber.disconnect();
    }

    @Test
    @Timeout(value = 1500, unit = TimeUnit.MILLISECONDS, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    void exchange_accepts_multiple_connections_successfully() throws IOException {
        final TelnetClientHelper helper1 = new TelnetClientHelper(Constants.LOCALHOST, config.port());
        final TelnetClientHelper helper2 = new TelnetClientHelper(Constants.LOCALHOST, config.port());
        final TelnetClientHelper helper3 = new TelnetClientHelper(Constants.LOCALHOST, config.port());
        assertEquals("ok SMQP", helper1.connectAndReadResponse());
        assertEquals("ok SMQP", helper2.connectAndReadResponse());
        assertEquals("ok SMQP", helper3.connectAndReadResponse());
    }

    @Test
    @Timeout(value = 1500, unit = TimeUnit.MILLISECONDS, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    void publish_to_fanout_exchange_successfully() throws IOException {
        subscriber.connectAndReadResponse();
        publisher.connectAndReadResponse();

        subscriber.subscribe(exchangeName, "fanout", queueName, "none");

        publisher.sendCommandAndReadResponse(exchange("fanout", exchangeName));

        publisher.publish("none", "VERIFICATION-MESSAGE-1");
        assertEquals("VERIFICATION-MESSAGE-1", subscriber.readResponse());
        publisher.publish("none", "VERIFICATION-MESSAGE-2");
        assertEquals("VERIFICATION-MESSAGE-2", subscriber.readResponse());
    }

    @Test
    @Timeout(value = 1500, unit = TimeUnit.MILLISECONDS, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    void publish_to_direct_exchange_successfully() throws IOException {
        subscriber.connectAndReadResponse();
        publisher.connectAndReadResponse();

        subscriber.subscribe(exchangeName, "direct", queueName, key);
        publisher.sendCommandAndReadResponse(exchange("direct", exchangeName));
        publisher.publish(key, "VERIFICATION-MESSAGE-1");
        assertEquals("VERIFICATION-MESSAGE-1", subscriber.readResponse());
        publisher.publish(key, "VERIFICATION-MESSAGE-2");
        assertEquals("VERIFICATION-MESSAGE-2", subscriber.readResponse());
    }

    @Test
    @Timeout(value = 1500, unit = TimeUnit.MILLISECONDS, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    void publish_to_direct_exchange_discards_invalid_message() throws IOException {
        subscriber.connectAndReadResponse();
        publisher.connectAndReadResponse();

        subscriber.subscribe(exchangeName, "direct", queueName, key);
        publisher.sendCommandAndReadResponse(exchange("direct", exchangeName));
        publisher.publish("invalid.key", "THIS-SHOULD-BE-DISCARDED");
        publisher.publish(key, "VERIFICATION-MESSAGE");

        assertEquals("VERIFICATION-MESSAGE", subscriber.readResponse());
    }

    @Test
    @Timeout(value = 1500, unit = TimeUnit.MILLISECONDS, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    void publish_to_default_exchange_successfully() throws IOException {
        subscriber.connectAndReadResponse();
        publisher.connectAndReadResponse();

        subscriber.sendCommandAndReadResponse(exchange("default", "default"));
        subscriber.sendCommandAndReadResponse(queue(queueName));
        subscriber.sendCommandAndReadResponse(SUBSCRIBE);

        publisher.sendCommandAndReadResponse(exchange("default", "default"));
        publisher.publish(queueName, "VERIFICATION-MESSAGE-1");
        assertEquals("VERIFICATION-MESSAGE-1", subscriber.readResponse());
        publisher.publish(queueName, "VERIFICATION-MESSAGE-2");
        assertEquals("VERIFICATION-MESSAGE-2", subscriber.readResponse());
    }

    @Test
    @Timeout(value = 1500, unit = TimeUnit.MILLISECONDS, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    void publish_to_default_exchange_discards_invalid_message() throws IOException {
        subscriber.connectAndReadResponse();
        publisher.connectAndReadResponse();

        subscriber.sendCommandAndReadResponse(exchange("default", "default"));
        subscriber.sendCommandAndReadResponse(queue(queueName));
        subscriber.sendCommandAndReadResponse(SUBSCRIBE);

        publisher.sendCommandAndReadResponse(exchange("default", "default"));
        publisher.publish("invalid.key", "THIS-SHOULD-BE-DISCARDED");
        publisher.publish(queueName, "VERIFICATION-MESSAGE");

        assertEquals("VERIFICATION-MESSAGE", subscriber.readResponse());
    }

    @Test
    @Timeout(value = 1500, unit = TimeUnit.MILLISECONDS, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    void publish_to_direct_exchange_discards_invalid_message_publisher_binds_queue() throws IOException {
        subscriber.connectAndReadResponse();
        publisher.connectAndReadResponse();

        publisher.sendCommandAndReadResponse(exchange("direct", exchangeName));
        publisher.sendCommandAndReadResponse(queue(queueName));
        publisher.sendCommandAndReadResponse(bind(key));

        subscriber.subscribe(queueName);

        publisher.publish("invalid.key", "THIS-SHOULD-BE-DISCARDED");
        publisher.publish(key, "VERIFICATION-MESSAGE");

        assertEquals("VERIFICATION-MESSAGE", subscriber.readResponse());
    }

    @Test
    @Timeout(value = 1500, unit = TimeUnit.MILLISECONDS, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    void publish_to_fanout_exchange_subscriber_reconnects_before_activating_subscription() throws IOException {
        subscriber.connectAndReadResponse();

        subscriber.sendCommandAndReadResponse(exchange("fanout", exchangeName));
        subscriber.sendCommandAndReadResponse(queue(queueName));
        subscriber.sendCommandAndReadResponse(bind("none"));
        subscriber.sendCommandAndReadResponse(EXIT);
        subscriber.disconnect();

        subscriber.connectAndReadResponse();
        subscriber.subscribe(queueName);

        publisher.connectAndReadResponse();
        publisher.sendCommandAndReadResponse(exchange("fanout", exchangeName));
        publisher.publish("whatever.key", "VERIFICATION-MESSAGE");

        assertEquals("VERIFICATION-MESSAGE", subscriber.readResponse());
    }

    @Test
    @Timeout(value = 1500, unit = TimeUnit.MILLISECONDS, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    void publish_to_direct_exchange_bound_queue_before_subscription_successfully() throws IOException {
        subscriber.connectAndReadResponse();
        publisher.connectAndReadResponse();

        publisher.sendCommandAndReadResponse(exchange("direct", exchangeName));
        publisher.sendCommandAndReadResponse(queue(queueName));
        publisher.sendCommandAndReadResponse(bind(key));
        publisher.publish(key, "VERIFICATION-MESSAGE");
        subscriber.subscribe(queueName);

        assertEquals("VERIFICATION-MESSAGE", subscriber.readResponse());
    }

    @Test
    @Timeout(value = 1500, unit = TimeUnit.MILLISECONDS, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    void publish_to_direct_exchange_bind_queue_after_publish_unsuccessfully() throws IOException {
        subscriber.connectAndReadResponse();
        publisher.connectAndReadResponse();

        publisher.sendCommandAndReadResponse(exchange("direct", exchangeName));
        publisher.sendCommandAndReadResponse(queue(queueName));
        publisher.publish(key, "THIS-SHOULD-BE-DISCARDED");
        subscriber.subscribe(exchangeName, "direct", queueName, key);
        publisher.publish(key, "VERIFICATION-MESSAGE");

        assertEquals("VERIFICATION-MESSAGE", subscriber.readResponse());
    }

}
