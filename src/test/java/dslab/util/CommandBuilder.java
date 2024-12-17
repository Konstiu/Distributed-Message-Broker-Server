package dslab.util;

public class CommandBuilder {
    public static String exchange(String type, String name) {
        return String.format("exchange %s %s", type, name);
    }

    public static String queue(String name) {
        return String.format("queue %s", name);
    }

    public static String bind(String bindingKey) {
        return String.format("bind %s", bindingKey);
    }

    public static String publish(String routingKey, String message) {
        return String.format("publish %s %s", routingKey, message);
    }

    public static String subscribe() {
        return "subscribe";
    }

    public static String exit() {
        return "exit";
    }

    public static String resolve(String name) {
        return "resolve %s".formatted(name);
    }

    public static String unregister(String name) {
        return "unregister %s".formatted(name);
    }

    public static String register(String name, String ip) {
        return "register %s %s".formatted(name, ip);
    }

    public static String elect(int id) {
        return "elect %d".formatted(id);
    }

    public static String declare(int id) {
        return "declare %d".formatted(id);
    }

    public static String ok() {
        return "ok";
    }

    public static String ack(int id) {
        return "ack %d".formatted(id);
    }

    public static String vote(int senderId, int candidateId) {
        return "vote %d %d".formatted(senderId, candidateId);
    }

    public static String ping() {
        return "ping";
    }

    public static String pong() {
        return "pong";
    }
}
