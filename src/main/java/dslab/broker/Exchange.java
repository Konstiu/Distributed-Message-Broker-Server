package dslab.broker;

import lombok.Getter;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class Exchange {
    @Getter
    private final ExchangeType type;
    private final List<Queue> queues;
    private volatile String[][] bindings;
    private volatile TopicTrie trie;

    public Exchange(ExchangeType type, List<Queue> queues, ConcurrentHashMap<String, String> bindings) {
        this.type = type;
        this.queues = queues;
        this.bindings = new String[1][2];
        trie = new TopicTrie();
    }

    public synchronized void addQueue(Queue queue) {
        for (Queue value : queues) {
            if (value.getName().equals(queue.getName())) {
                if (value.getBrokerCommandHandler().equals(queue.getBrokerCommandHandler())) {
                    return;
                }
                value.addBrokerCommandHandler(queue.getBrokerCommandHandler().getFirst());
                return;
            }
        }
        queues.add(queue);
    }

    public void addBinding(String queue, String key) {
        if (bindings[bindings.length - 1][0] != null) {
            String[][] temp = new String[bindings.length * 2][2];
            System.arraycopy(bindings, 0, temp, 0, bindings.length);
            bindings = temp;
        }
        trie.insert(key, queue);
        for (int i = 0; i < bindings.length; i++) {
            if (bindings[i][0] == null) {
                bindings[i][0] = key;
                bindings[i][1] = queue;
                return;
            }
        }
    }

    public void publish(String key, String message) {
        for (Queue queue : queues) {
            switch (type) {
                case FANOUT:
                    String helperString = "";
                    for (String[] binding : bindings) {
                        if (binding[0] != null) {
                            helperString += binding[1] + " ";
                        }
                    }
                    String[] keysFanout = helperString.split(" ");
                    for (String s : keysFanout) {
                        if (queue.getName().equals(s)) {
                            queue.addMessage(message);
                        }
                    }
                    break;
                case DIRECT, DEFAULT:
                    if (getBindingByKey(key) != null) {
                        String[] keys = Objects.requireNonNull(getBindingByKey(key)).split(" ");
                        for (String s : keys) {
                            if (queue.getName().equals(s)) {
                                queue.addMessage(message);
                            }
                        }
                    }
                    break;
                case TOPIC:
                    List<String> foundQueues = trie.searchExact(key);
                    for (String s : foundQueues) {
                        if (queue.getName().equals(s)) {
                            queue.addMessage(message);
                        }
                    }
                    break;
            }
        }
    }

    private String getBindingByKey(String key) {
        String value = "";
        for (String[] binding : bindings) {
            if (binding[0] != null && binding[0].equals(key)) {
                value += binding[1] + " ";
            }
        }
        return !value.isEmpty() ? value : null;
    }

    public void subscribed(BrokerCommandHandler brokerCommandHandler, String queueName) {
        for (Queue queue : queues) {
            if (queue.getName().equals(queueName)) {
                queue.printmsg(brokerCommandHandler);
            }
        }
    }

    public void unsubscribe(BrokerCommandHandler brokerCommandHandler) {
        for (Queue queue : queues) {
            queue.unsubscribe(brokerCommandHandler);
        }
    }
}
