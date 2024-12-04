package dslab.broker;

import java.util.List;

public class TopicTrie {
    private final TopicTrieNode root;

    public TopicTrie() {
        this.root = new TopicTrieNode();
    }

    public synchronized void insert(String key, String value) {
        TopicTrieNode current = root;
        String[] keys = key.split("\\.");
        for (String k : keys) {
            if (!current.getChildren().containsKey(k)) {
                current.getChildren().put(k, new TopicTrieNode());

            }
            current = current.getChildren().get(k);

        }
        current.setEndOfTopic(true);
        current.addValue(value);
    }

    public List<String> searchExact(String key) {
        List<String> result = new java.util.ArrayList<>();
        searchRecursive(root, key.split("\\."), result, 0);
        return result;
    }

    private synchronized void searchRecursive(TopicTrieNode current, String[] key, List<String> result, int idx) {
        if (idx == key.length) {
            for (int i = 0; i < current.getValue().size(); i++) {
                if (!result.contains(current.getValue().get(i))) {
                    result.add(current.getValue().get(i));
                }
            }
            if (current.getChildren().containsKey("#")) {
                searchRecursive(current.getChildren().get("#"), key, result, idx);
            }

            return;
        }

        if (current.getChildren().containsKey("#")) {
            searchRecursive(current, key, result, idx +1);
            searchRecursive(current.getChildren().get("#"), key, result, idx );
        }
        if (current.getChildren().containsKey("*")) {
            searchRecursive(current.getChildren().get("*"), key, result, idx + 1);
        }
        if (current.getChildren().containsKey(key[idx])) {
            searchRecursive(current.getChildren().get(key[idx]), key, result, idx + 1);
        }
    }


}
