package dslab.broker;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Getter
public class TopicTrieNode {

    private final ConcurrentHashMap<String, TopicTrieNode> children;
    private boolean isEndOfTopic;
    private final List<String> value;

    public TopicTrieNode() {
        this.children = new ConcurrentHashMap<>();
        this.isEndOfTopic = false;
        value = new ArrayList<>();
    }

    public boolean isEndOfTopic() {
        return isEndOfTopic;
    }

    public void setEndOfTopic(boolean endOfTopic) {
        isEndOfTopic = endOfTopic;
    }
    public void addValue(String value) {
        this.value.add(value);
    }

}
