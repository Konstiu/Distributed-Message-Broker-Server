package dslab.util.grading;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class GradingData {

    private static final GradingData INSTANCE = new GradingData();
    private static final int MAX_SCORE_ACHIEVABLE = 125;

    private int totalScore = 0;
    private final Map<String, Integer> methodScore = new HashMap<>();
    private final Map<String, Boolean> testMethodResults = new HashMap<>();

    private final Map<String, TestMethodInfo> methodInfoMap = new LinkedHashMap<>();

    public static GradingData getInstance() {
        return INSTANCE;
    }

    public synchronized void addTestMethod(String methodId, String className, String methodName, int score) {
        if (!methodInfoMap.containsKey(methodId)) {
            totalScore += score;
            methodInfoMap.put(methodId, new TestMethodInfo(className, methodName, score));
            testMethodResults.put(methodId, true); // Assume pass initially
        }
    }

    public synchronized void markTestMethodFailed(String methodId) {
        testMethodResults.put(methodId, false);
    }

    public void outputSummary() {

        if (totalScore != MAX_SCORE_ACHIEVABLE) return;

        final int testColumnWidth = 123;
        final int reachedScoreWidth = 7;
        final int reachableScoreWidth = 8;
        final int resultWidth = 6;

        final int totalWidth = testColumnWidth + reachedScoreWidth + reachableScoreWidth + resultWidth + 8;
        // Calculate total earned score
        int totalEarnedScore = 0;

        // Create a list to hold the formatted rows
        List<String[]> rows = new ArrayList<>();

        // Iterate over the test methods and collect data
        for (String methodId : methodInfoMap.keySet()) {
            boolean testPassed = testMethodResults.getOrDefault(methodId, false);
            TestMethodInfo info = methodInfoMap.get(methodId);
            String testName = info.className + "#" + info.methodName;
            String reachedScore = testPassed ? String.valueOf(info.score) : "0";
            String reachableScore = String.valueOf(info.score);
            String result = testPassed ? "\u001B[32m" + "PASSED" + "\u001B[0m" : "\u001B[31m" + "FAILED" + "\u001B[0m";

            if (testPassed) {
                totalEarnedScore += info.score;
            }

            rows.add(new String[]{testName, reachedScore, reachableScore, result});
        }

        // Calculate total reachable score
        int totalReachableScore = totalScore;

        // Print the centered headline
        String headline = "=== GRADING SIMULATION REPORT ===";
        System.out.println();
        System.out.println(centerString(headline, totalWidth));
        System.out.println();

        // Print the table header
        String formatHeader = "| %-" + testColumnWidth + "s | %-" + reachedScoreWidth + "s | %-" + reachableScoreWidth + "s | %-" + resultWidth + "s |%n";
        String formatRow = "| %-" + testColumnWidth + "s | %" + reachedScoreWidth + "s | %" + reachableScoreWidth + "s | %-" + resultWidth + "s |%n";
        String totalsRow = "| %-" + testColumnWidth + "s | %" + reachedScoreWidth + "s | %" + reachableScoreWidth + "s | %-" + resultWidth + "s |%n";
        String separatorLine = "+" + "-".repeat(testColumnWidth + 2) + "+" + "-".repeat(reachedScoreWidth + 2) + "+" + "-".repeat(reachableScoreWidth + 2) + "+" + "-".repeat(resultWidth + 2) + "+";

        System.out.println(separatorLine);
        System.out.format(formatHeader, "TestClassName#TestMethodName", "Reached", "Possible", "Result");
        System.out.flush();
        System.out.println(separatorLine);

        // Print each row
        for (String[] row : rows) {
            System.out.format(formatRow, row[0], row[1], row[2], row[3]);
            System.out.flush();
        }

        // Print the table footer
        System.out.println(separatorLine);

        // Print the total lines
        System.out.format(totalsRow, "TOTAL (Expected Score of the GitHub Classroom Grading Workflow)", totalEarnedScore, totalReachableScore, "SCORE");
        System.out.println(separatorLine);
        System.out.format(totalsRow, "TOTAL (Expected Score as Assignment Points)", totalEarnedScore / 10.0d, totalReachableScore / 10.0d, "POINTS");
        System.out.println(separatorLine);
        System.out.println();

        // Print disclaimer
        System.out.println("""
                IMPORTANT NOTE: The Grading Simulation Report is just an helper utility and does not submit your final points to GitHub Classroom.\
                
                Actual grading will be done by the Grading Workflow on GitHub when you push your changes to the main branch of the provided repository.
                """);
        System.out.flush();
    }

    private String centerString(String s, int width) {
        // Get the strings length
        int stringLength = s.length();

        // If the string is longer than the width, return the string
        if (stringLength >= width) {
            return s;
        }

        // Calculate the number of spaces to add to the left and right
        int spaces = width - stringLength;
        int spacesLeft = spaces / 2;

        // Build the string with the spaces
        return " ".repeat(spacesLeft) + s + " ".repeat(spaces - spacesLeft);
    }
}
