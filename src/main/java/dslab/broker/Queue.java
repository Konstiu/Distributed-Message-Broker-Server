package dslab.broker;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Getter
public class Queue {
    private String message;
    private final String name;
    private final List<BrokerCommandHandler> brokerCommandHandler;
    private final ConcurrentHashMap<BrokerCommandHandler, Boolean> subscribed;

    public Queue(String name, BrokerCommandHandler brokerCommandHandler, String message) {
        this.name = name;
        this.subscribed = new ConcurrentHashMap<>();
        this.brokerCommandHandler = new ArrayList<>();
        this.brokerCommandHandler.add(brokerCommandHandler);
        this.subscribed.put(brokerCommandHandler, false);
        this.message = message;
    }

    public synchronized void addMessage(String message) {
        this.message += message + "\n";
        for (BrokerCommandHandler commandHandler : brokerCommandHandler) {
            if (subscribed.get(commandHandler) != null && subscribed.get(commandHandler)) {
                commandHandler.printmsg(message + "\n");
                this.message = "";
                return;
            }
        }
    }

    public synchronized void addBrokerCommandHandler(BrokerCommandHandler brokerCommandHandler) {
        if (this.brokerCommandHandler.contains(brokerCommandHandler)) {
            return;
        }
        this.brokerCommandHandler.add(brokerCommandHandler);
    }

    public synchronized void printmsg(BrokerCommandHandler brokerCommandHandler) {
        this.subscribed.put(brokerCommandHandler, true);
        brokerCommandHandler.printmsg(this.message);
        this.message = "";
    }

    public void unsubscribe(BrokerCommandHandler brokerCommandHandler) {
        this.subscribed.put(brokerCommandHandler, false);
    }
}
