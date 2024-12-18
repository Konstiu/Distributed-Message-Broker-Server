package dslab.assignment2.broker;

import dslab.util.Constants;
import dslab.util.Global;
import dslab.util.helper.TelnetClientHelper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static dslab.util.CommandBuilder.SUBSCRIBE;
import static dslab.util.CommandBuilder.bind;
import static dslab.util.CommandBuilder.exchange;
import static dslab.util.CommandBuilder.queue;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class MultipleExchangeScenarioTest extends BaseSingleBrokerTest {

    private static final String queue1Name = String.format("queue-%s", Global.SECURE_STRING_GENERATOR.getSecureString());
    private static final String queue2Name = String.format("queue-%s", Global.SECURE_STRING_GENERATOR.getSecureString());
    private TelnetClientHelper pub1;
    private TelnetClientHelper pub2;
    private TelnetClientHelper sub1;
    private TelnetClientHelper sub2;

    @Override
    protected void initTelnetClientHelpers() throws IOException {
        pub1 = new TelnetClientHelper(Constants.LOCALHOST, config.port());
        pub2 = new TelnetClientHelper(Constants.LOCALHOST, config.port());
        sub1 = new TelnetClientHelper(Constants.LOCALHOST, config.port());
        sub2 = new TelnetClientHelper(Constants.LOCALHOST, config.port());

        sub1.connectAndReadResponse();
        sub2.connectAndReadResponse();
        pub1.connectAndReadResponse();
        pub2.connectAndReadResponse();
    }

    @Override
    protected void closeTelnetClientHelpers() throws IOException {
        pub1.disconnect();
        pub2.disconnect();
        sub1.disconnect();
        sub2.disconnect();
    }

    @Test
    @Timeout(value = 1500, unit = TimeUnit.MILLISECONDS, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    void publish_to_fanout_exchange_and_direct_exchange_successfully() throws IOException {
        final String key = "key-%s".formatted(Global.SECURE_STRING_GENERATOR.getSecureString());

        sub1.subscribe("fanout-1", "fanout", queue1Name, "none");
        sub2.subscribe("direct-1", "direct", queue2Name, key);

        pub1.sendCommandAndReadResponse(exchange("fanout", "fanout-1"));
        pub2.sendCommandAndReadResponse(exchange("direct", "direct-1"));

        pub1.publish("none", "VERIFICATION-MESSAGE-FANOUT");
        assertEquals("VERIFICATION-MESSAGE-FANOUT", sub1.readResponse());

        pub2.publish(key, "VERIFICATION-MESSAGE-DIRECT");
        assertEquals("VERIFICATION-MESSAGE-DIRECT", sub2.readResponse());
    }

    @Test
    @Timeout(value = 1500, unit = TimeUnit.MILLISECONDS, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    void three_exchanges_two_queues_successfully() throws IOException {
        final String key1 = "key-%s".formatted(Global.SECURE_STRING_GENERATOR.getSecureString());
        final String key2 = "key-%s".formatted(Global.SECURE_STRING_GENERATOR.getSecureString());
        final String topicBase = "topic-%s".formatted(Global.SECURE_STRING_GENERATOR.getSecureString());

        sub1.subscribe("topic-1", "topic", queue1Name, String.format("%s.*", topicBase));

        sub2.sendCommandAndReadResponse(exchange("direct", "direct-1"));
        sub2.sendCommandAndReadResponse(queue(queue1Name));
        sub2.sendCommandAndReadResponse(bind(key1));
        sub2.sendCommandAndReadResponse(queue(queue2Name));
        sub2.sendCommandAndReadResponse(bind(key2));
        sub2.sendCommandAndReadResponse(SUBSCRIBE);

        pub1.sendCommandAndReadResponse(exchange("direct", "direct-1"));
        pub2.sendCommandAndReadResponse(exchange("topic", "topic-1"));

        pub1.publish(key1, "VERIFICATION-MESSAGE-DIRECT-QUEUE-1");
        assertEquals("VERIFICATION-MESSAGE-DIRECT-QUEUE-1", sub1.readResponse());
        pub2.publish(String.format("%s.at", topicBase), "VERIFICATION-MESSAGE-TOPIC-QUEUE-1");
        assertEquals("VERIFICATION-MESSAGE-TOPIC-QUEUE-1", sub1.readResponse());
        pub1.publish(key2, "VERIFICATION-MESSAGE-DIRECT-QUEUE-2");
        assertEquals("VERIFICATION-MESSAGE-DIRECT-QUEUE-2", sub2.readResponse());
        
        pub1.sendCommandAndReadResponse(exchange("default", "default"));
        pub1.publish(queue1Name, "VERIFICATION-MESSAGE-DEFAULT-QUEUE-1");
        assertEquals("VERIFICATION-MESSAGE-DEFAULT-QUEUE-1", sub1.readResponse());
        pub1.publish(queue2Name, "VERIFICATION-MESSAGE-DEFAULT-QUEUE-2");
        assertEquals("VERIFICATION-MESSAGE-DEFAULT-QUEUE-2", sub2.readResponse());
    }
}
