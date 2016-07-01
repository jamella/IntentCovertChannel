package covertchannel.intent.sender;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import intent.covertchannel.intentencoderdecoder.BitstringEncoder;
import intent.covertchannel.intentencoderdecoder.EncodingScheme;
import intent.covertchannel.intentencoderdecoder.EncodingUtils;
import intent.covertchannel.intentencoderdecoder.LowerCaseAlphaEncoder;

// TODO: Look into using InputFilters with the message entry EditText to limit
// the character values which can be typed to those supported by the current
// scheme
public class SenderActivity extends Activity {

    // TODO: Move into a service

    // TODO: Prune any overly-large message sizes (or add more if possible)
    // TODO: Cleanup
    private static final int[] THROUGHPUT_TEST_MESSAGE_SIZES_IN_BYTES = {2048, 4096, 8192, 16384, 32768, 65536};//, 131072, 262144, 524288, 1048576, 2097152, 4194304, 8388608, 16777216};
    //{256, 512, 1024, 2048, 4096, 8192};//, 16384, 32768, 65536, 131072, 262144, 524288, 1048576, 2097152, 4194304, 8388608, 16777216};

    private static final int[] BASE_VALUE_COUNTS = {EncodingScheme.NUM_BASE_VALUES};

    private static final int[] EXPANSION_CODE_COUNTS =
            {0,
                    1,
                    2,
                    (EncodingScheme.NUM_BASE_VALUES * 1) + 1, // 21 + 1 = 22
                    //(EncodingScheme.NUM_BASE_VALUES * 5) + 1, // 105 + 1 = 106
            };

    private static final int[] ACTION_STRING_COUNTS = {1, 5, 25};//, 75, 100};

    private static final int NUM_TEST_REPETITIONS = 5;
    private static final String TEST_RESULTS_FILE_NAME = "TestResults_"; // TODO: Write to DCIM

    // TODO: Cleanup
    public class TestRunEntry {//} implements Runnable {
        private int numMessageBytes;
        private int numBaseValues;
        private int numExpansionCodes;
        private int numActions;
        private int numUniqueValues;
        private String message; // Redundant with the numMessageBytes
        //private Long elapsedTime;
        private Double startTime;
        private Double endTime;
        private long testRunId;
        private List<Intent> testMessageIntents;

        public TestRunEntry(int numMessageBytes, int numBaseValues, int numExpansionCodes, int numActions, int numUniqueValues, String message) {
            testRunId = UUID.randomUUID().getLeastSignificantBits();
            testMessageIntents = new ArrayList<>();

            this.numMessageBytes = numMessageBytes;
            this.numBaseValues = numBaseValues;
            this.numExpansionCodes = numExpansionCodes;
            this.numActions = numActions;
            this.numUniqueValues = numUniqueValues;
            this.message = message;
            //this.elapsedTime = null;
            this.startTime = null;
            this.endTime = null;
        }

        public long getTestRunId() {
            return testRunId;
        }

        public void setEndTime(double endTime) {
            this.endTime = endTime;
        }

        public boolean hasBeenCompleted() {
            return (startTime != null && endTime != null);
            //return elapsedTime != null;
        }

        public double getElapsedTimeMillis() {
            return Math.abs(endTime - startTime);
            //return elapsedTime;
        }

        public double getBitsPerSecond() {
            return (numMessageBytes * 8 * 1000) / getElapsedTimeMillis();
        }

        public String getTestConfigurationSummary() {
            StringBuilder strBldr = new StringBuilder();
            strBldr.append("\tNumber of message bytes: " + numMessageBytes + "\n");
            strBldr.append("\tNumber of base values: " + numBaseValues + "\n");
            strBldr.append("\tNumber of expansion codes: " + numExpansionCodes + "\n");
            strBldr.append("\tNumber of actions: " + numActions + "\n");
            strBldr.append("\tNumber of unique values: " + numUniqueValues + "\n");
            //strBldr.append("\tMessage: " + message + "\n");

            return strBldr.toString();
        }

        public String toString() {
            StringBuilder strBldr = new StringBuilder();
            strBldr.append("Test Run Information:\n");
            strBldr.append(this.getTestConfigurationSummary());
            strBldr.append("\tStart time: " + startTime + "\n");
            strBldr.append("\tEnd time: " + endTime + "\n");

            if(this.hasBeenCompleted()) {
                strBldr.append("\tElapsed time: " + this.getElapsedTimeMillis() + " milliseconds\n");
                strBldr.append("\tBits per Second: " + this.getBitsPerSecond() + "\n");
            }

            return strBldr.toString();
        }


        // TODO: Cleanup
        //@Override
        //public void run() {
        public void runTest() {
            //Log.d(TAG, "Running test case with " + this.toString());

            // TODO: Make this variable
            final EncodingScheme bitstringEncoder = new BitstringEncoder(this.numBaseValues, this.numExpansionCodes, EncodingUtils.ACTIONS.subList(0, this.numActions), EncodingScheme.BUILD_VERSION);

            configureReceiver(numBaseValues, numExpansionCodes, numActions, testRunId);

            Log.d(TAG, "Encoding message \"" + message + "\" as bitstring");
            Collection<Intent> encodedIntents = bitstringEncoder.encodeMessage(message);

            testMessageIntents.clear();
            for(Intent encodedIntent: encodedIntents) {
                if(encodedIntent.getExtras().isEmpty()) {
                    // Don't send empty Intents
                    Log.d(TAG, "Skipping Intent with action \"" + encodedIntent.getAction() + "\"; no data");
                    continue;
                }

                // TODO: Make these constants
                ComponentName cn = new ComponentName("covertchannel.intent.receiver", "covertchannel.intent.receiver.MessageReceiver");
                encodedIntent.setComponent(cn);
                encodedIntent.setAction(EncodingUtils.CALCULATE_THROUGHPUT_ACTION_BITSTRING_ENCODING);
                testMessageIntents.add(encodedIntent);

                // TODO: Cleanup
                /*
                this.startTime = EncodingUtils.getTimeMillisAccurate();
                Log.d(TAG, "Starting service with Intent with action of " + encodedIntent.getAction() + " and test ID of " + testRunId + "; start time = " + startTime);
                startService(encodedIntent);
                */
            }

            IntentFilter filter = new IntentFilter();
            filter.addAction(EncodingUtils.ACKNOWLEDGE_MESSAGE_SEGMENT_ACTION);
            registerReceiver(new BroadcastReceiver() {
                                 @Override
                                 public void onReceive(Context context, Intent intent) {
                                     if(!testMessageIntents.isEmpty()) {
                                         Log.d(TAG, "Sending next message segment for test " + testRunId);
                                         Intent encodedIntent = testMessageIntents.remove(0);
                                         Log.d(TAG, "Starting service with Intent with action of " + encodedIntent.getAction());
                                         startService(encodedIntent);
                                     } else {
                                         Log.d(TAG, "Test " + testRunId + " is complete");
                                         unregisterReceiver(this);
                                     }
                                 }
                             },
                    filter);

            this.startTime = EncodingUtils.getTimeMillisAccurate();
            Intent encodedIntent = testMessageIntents.remove(0);
            /*
            ComponentName cn = new ComponentName("covertchannel.intent.receiver", "covertchannel.intent.receiver.MessageReceiver");
            encodedIntent.setComponent(cn);
            encodedIntent.setAction(EncodingUtils.CALCULATE_THROUGHPUT_ACTION_BITSTRING_ENCODING);
            */
            Log.d(TAG, "Starting service with Intent with action of " + encodedIntent.getAction());
            startService(encodedIntent);
        }

        /* TODO: Remove
        public void setElapsedTime(long elapsedTime) {
            this.elapsedTime = elapsedTime;
        }
        */
    }


    // TODO: Need a bitstring and original encoding version
    private class TestRun {
        private int numRepitions;
        private List<TestRunEntry> testsToRun;
        private List<TestRunEntry> completedTests;
        private TestRunEntry currentTest;
        private int numMessageBytes;
        private int numBaseValues;
        private int numExpansionCodes;
        private int numActions;
        private int numUniqueValues;
        private String message;

        public TestRun(int numRepitions, int numMessageBytes, int numBaseValues, int numExpansionCodes, int numActions, String message) {
            this.numRepitions = numRepitions;
            this.testsToRun = new ArrayList<>();
            this.completedTests = new ArrayList<>();
            this.numMessageBytes = numMessageBytes;
            this.numBaseValues = numBaseValues;
            this.numExpansionCodes = numExpansionCodes;
            this.numActions = numActions;
            this.message = message;

            this.numUniqueValues = numBaseValues * (numExpansionCodes + 1) * numActions;

            currentTest = null;

            for(int i = 0; i < numRepitions; i++) {
                testsToRun.add(new TestRunEntry(numMessageBytes, numBaseValues, numExpansionCodes, numActions, numUniqueValues, message));
            }
        }

        public void startTestRun() {
            if(currentTest != null) {
                Log.w(TAG, "Attempt to start a test while another test was already in progress");
                return;
            }

            if(!testsToRun.isEmpty()) {
                currentTest = testsToRun.remove(0);

                Log.d(TAG, "Starting test: " + currentTest.getTestConfigurationSummary());
                Log.d(REPORT_TAG, "Starting test: " + currentTest.getTestConfigurationSummary());

                // TODO: Cleanup
                currentTest.runTest();
                //new Thread(currentTest).start();
            } else {
                throw new RuntimeException("Out of tests to run");
            }
        }

        public void endTestRun(double endTime) {
            //public void endTestRun(long elapsedTime) {
            // TODO: Cleanup
            currentTest.setEndTime(endTime);
            //currentTest.setElapsedTime(elapsedTime);

            String testSummary = currentTest.toString();
            Log.d(TAG, "Finished test: " + testSummary);
            Log.d(REPORT_TAG, "Finished test: " + testSummary);
            //outputTestResults(testSummary);

            completedTests.add(currentTest);
            currentTest = null;
        }

        public boolean isComplete() {
            return testsToRun.isEmpty() && currentTest == null;
        }

        private double getAverageThroughput() {
            if(!isComplete()) {
                throw new RuntimeException("Tried to get average throughput before test was complete");
            }

            double combinedBitsPerSecond = 0.0;
            for(TestRunEntry testRunEntry: completedTests) {
                combinedBitsPerSecond += testRunEntry.getBitsPerSecond();
            }

            return combinedBitsPerSecond / numRepitions;
        }

        public String generateTestReport() {
            if(!isComplete()) {
                throw new RuntimeException("Tried to generate summary before test was complete");
            }

            StringBuilder reportBuilder = new StringBuilder();
            reportBuilder.append("Test Report:\n");
            reportBuilder.append(completedTests.get(0).getTestConfigurationSummary());
            reportBuilder.append("\n");
            reportBuilder.append("Average Throughput: " + getAverageThroughput() + " bits per second\n");
            reportBuilder.append("\n\n");

            return reportBuilder.toString();
        }

        public int getNumMessageBytes() {
            return numMessageBytes;
        }

        public int getNumBaseValues() {
            return numBaseValues;
        }

        public int getNumExpansionCodes() {
            return numExpansionCodes;
        }

        public int getNumActions() {
            return numActions;
        }

        public int getNumUniqueValues() {
            return numUniqueValues;
        }

        public long getCurrentTestId() {
            return currentTest == null ? EncodingUtils.DEFAULT_TEST_ID : currentTest.getTestRunId();
        }
    }

    // Note: This class assumes it has been registered as a receiver before it is constructed
    private class ThroughputCalculationReceiver extends BroadcastReceiver {
        // TODO: Cleanup
        private int numRepitions;
        private List<TestRun> pendingTestRuns;
        private List<TestRun> completedTests;
        //private Map<Long, TestRun> completedTestsByTestId;
        private TestRun currentTestRun;

        // TODO: Run tests for both encoding strategies
        public ThroughputCalculationReceiver(int numRepitions, int[] throughputTestMessageSizesInBytes, int[] baseValueCounts, int[] expansionCodeCounts, int[] actionStringCounts) {
            this.numRepitions = numRepitions;
            pendingTestRuns = new ArrayList<>();
            // TODO: Cleanup
            completedTests = new ArrayList<>();
            //completedTestsByTestId = new HashMap<>();
            currentTestRun = null;

            for (int numMessageBytes: throughputTestMessageSizesInBytes) {
                final String message = readMessage(numMessageBytes);

                for(int numBaseValues: baseValueCounts) {
                    for (int numExpansionCodes: expansionCodeCounts) {
                        for (int numActions: actionStringCounts) {
                            pendingTestRuns.add(new TestRun(numRepitions, numMessageBytes, numBaseValues, numExpansionCodes, numActions, message));
                        }
                    }
                }
            }
        }

        private void runNextTest() {
            if(currentTestRun == null) {
                currentTestRun = pendingTestRuns.remove(0);
            }

            // TODO: Cleanup

            // Assumes that the fresh test-run entry has at least one repetition to be performed;
            // will call back to this receiver once the message transmission is complete
            currentTestRun.startTestRun();
            //currentTestRun.startTestRun(System.currentTimeMillis());
            //currentTestRun.startTestRun(new Date().getTime());
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            //Log.d(TAG, "Received end time acknowledgement from the receiver");
            //Log.d(REPORT_TAG, "Received end time acknowledgement from the receiver");

            // TODO: Cleanup
            double endTime = intent.getDoubleExtra(EncodingUtils.END_TIME_KEY, 0.0);
            long testId = intent.getLongExtra(EncodingUtils.TEST_ID_KEY, EncodingUtils.DEFAULT_TEST_ID);
            Log.d(TAG, "Received end time of " + endTime + " for test ID: " + testId);
            //long elapsedTime = intent.getLongExtra(EncodingUtils.ELAPSED_TIME_KEY, 0);
            //currentTestRun.endTestRun(elapsedTime);

            //

            if(currentTestRun != null && testId == currentTestRun.getCurrentTestId()) {
                currentTestRun.endTestRun(endTime);

                if (currentTestRun.isComplete()) {
                    String completedTestReport = currentTestRun.generateTestReport();
                    Log.d(REPORT_TAG, "Completed test: " + completedTestReport);
                    // TODO: Cleanup
                    //outputTestResults(completedTestReport);
                    completedTests.add(currentTestRun);
                    //completedTestsByTestId.put(currentTestRun.get)
                    updateTestingReport(completedTests);

                    if (pendingTestRuns.isEmpty()) {
                        currentTestRun = null;
                        // TODO: Cleanup
                        //outputReport(completedTests);
                        return;
                    }
                    currentTestRun = pendingTestRuns.remove(0);
                }

                // Sleep to lower continuous system load
                // This is a bad thing to do on a UI thread; forgive me
                Log.d(TAG, "Sleeping for five seconds");
                SystemClock.sleep(5000);

                // TODO: Cleanup
                //currentTestRun.startTestRun(new Date().getTime());
                //currentTestRun.startTestRun(System.currentTimeMillis());
                currentTestRun.startTestRun();
            } else {
                if(currentTestRun == null) {
                    Log.d(TAG, "Received end time response with a test ID of " + testId + " when no test is ongoing");
                } else {
                    Log.d(TAG, "Received end time for a test ID of " + testId + ", which does not match the current test ID: " + currentTestRun.getCurrentTestId());
                }
            }
        }
    }


    private void writeTestResultsToFile(String s) {
        return;
        /*
        Log.d(TAG, "Writing results to file: " + testResultsFileName + ":\n\n" + s);
        File file = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOCUMENTS), testResultsFileName);
*/
        /* TODO: Cleanup
        if (!file.mkdirs()) {
            Log.e(TAG, "Directory not created");
            return;
        }
        */
/*
        try {
            if (!file.exists()) {
                file.createNewFile();
            }

            // TODO:CLeanup
            FileOutputStream writer = openFileOutput(file.getName(), MODE_APPEND);
            //FileOutputStream writer = openFileOutput(file.getName(), Context.MODE_PRIVATE);
            writer.write(s.getBytes());
            writer.flush();
            writer.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Log.d(TAG, "Write complete!");
        */
    }

    private void outputReport(List<TestRun> completedTests) {
        StringBuilder reportBuilder = new StringBuilder();
        reportBuilder.append("Throughput test results:\n\n");

        for(TestRun testRun: completedTests) {
            reportBuilder.append(testRun.generateTestReport());
        }

        testResultsDisplay.setText(reportBuilder.toString());
    }

    private void performThroughputTest() {
        new Thread(new Runnable() {
            public void run() {

                // TODO: Add evaluating the original implementation
                // TODO: Cleanup
                ThroughputCalculationReceiver endTimeReceiver = new ThroughputCalculationReceiver(NUM_TEST_REPETITIONS, THROUGHPUT_TEST_MESSAGE_SIZES_IN_BYTES, BASE_VALUE_COUNTS, EXPANSION_CODE_COUNTS, ACTION_STRING_COUNTS);

                /*
                // Current throughput: 71.77 bits per second
                int[] messageSizes = {2048}; //{65536};
                int[] baseValueCounts = BASE_VALUE_COUNTS;
                int[] expansionCodeCounts = {22};
                int[] actionStringCounts = {5};

                ThroughputCalculationReceiver endTimeReceiver = new ThroughputCalculationReceiver(5, messageSizes, baseValueCounts, expansionCodeCounts, actionStringCounts);
                */

                IntentFilter filter = new IntentFilter();
                filter.addAction(EncodingUtils.SEND_TIME_ACTION);
                registerReceiver(endTimeReceiver, filter);

                // Assumes that there is at least one test to run
                endTimeReceiver.runNextTest();
            }
        }).start();
    }

    private String readMessage(int bytesToSend) {
        return EncodingUtils.readRawTextFile(this, R.raw.taming_of_the_shrew, bytesToSend);
    }

    // END TODO

    // TODO: Cleanup
    //private static final String TAG = "covertchannel.intent.sender.SenderActivity";
    private static final String TAG = EncodingUtils.TRACE_TAG;

    private static final String REPORT_TAG = "throughput.evaluation.report";

    private EditText messsageEntry;
    private Button sendAlphaButton;
    private Button sendAlphaOrigInterceptibleButton;
    private Button sendBitstringButton;
    private Button sendBitstringInterceptibleButton;
    private Button clearMessageStoreButton;
    private Button runThroughputTestButton;
    private TextView testResultsDisplay;

    private StringBuilder testResults;
    private String testResultsFileName;

    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

        testResults = new StringBuilder();
        testResultsFileName = TEST_RESULTS_FILE_NAME + System.currentTimeMillis() + ".txt";

        // TODO: Remove concept of enhanced alpha encoder
        messsageEntry = (EditText) findViewById(R.id.message_entry);
		sendAlphaButton = (Button) findViewById(R.id.send_alpha_orig_button);
        sendAlphaOrigInterceptibleButton = (Button) findViewById(R.id.send_alpha_orig_interceptible_button);
        sendBitstringButton = (Button) findViewById(R.id.send_bitstring_button);
        sendBitstringInterceptibleButton = (Button) findViewById(R.id.send_bitstring_interceptible_button);
        clearMessageStoreButton = (Button) findViewById(R.id.send_clear_message_store_intent);
        runThroughputTestButton = (Button) findViewById(R.id.run_throughput_test_button);
        testResultsDisplay = (TextView) findViewById(R.id.test_results_display);

        final EncodingScheme alphaEncoder = new LowerCaseAlphaEncoder(EncodingScheme.NUM_BASE_VALUES, EncodingUtils.NUM_ALPHA_EXPANSION_CODES, Collections.singleton(EncodingUtils.ALPHA_ENCODING_ACTION), EncodingScheme.BUILD_VERSION);
        final EncodingScheme alphaEncoderInterceptible = new LowerCaseAlphaEncoder(EncodingScheme.NUM_BASE_VALUES, EncodingUtils.NUM_ALPHA_EXPANSION_CODES, Collections.singleton(EncodingUtils.INTERCEPTIBLE_ACTION), EncodingScheme.BUILD_VERSION);

        final EncodingScheme bitstringEncoder = new BitstringEncoder(EncodingScheme.NUM_BASE_VALUES, EncodingUtils.NUM_EXPANSION_CODES, EncodingUtils.ACTIONS, EncodingScheme.BUILD_VERSION);
        final EncodingScheme bitstringEncoderInterceptible = new BitstringEncoder(EncodingScheme.NUM_BASE_VALUES, EncodingUtils.NUM_ALPHA_EXPANSION_CODES, Collections.singletonList(EncodingUtils.INTERCEPTIBLE_ACTION), EncodingScheme.BUILD_VERSION);

        // TODO: Cleanup repeated code
		sendAlphaButton.setOnClickListener(
                new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // TODO: Check the input validity
                        String message = messsageEntry.getText().toString();

                        //Log.d(TAG, "Encoding message \"" + message + "\"");
                        Intent encodedIntent = alphaEncoder.encodeMessage(message).iterator().next();
                        messsageEntry.setText("");

                        // TODO: Move into encoding schemes/ use constants
                        ComponentName cn = new ComponentName("covertchannel.intent.receiver", "covertchannel.intent.receiver.MessageReceiver");
                        encodedIntent.setComponent(cn);

                        //Log.d(TAG, "Starting activity with Intent with action of " + encodedIntent.getAction());
                        startService(encodedIntent);
                    }
                });

        sendAlphaOrigInterceptibleButton.setOnClickListener(
                new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // TODO: Check the input validity
                        String message = messsageEntry.getText().toString();
                        Intent encodedIntent = alphaEncoderInterceptible.encodeMessage(message).iterator().next();
                        messsageEntry.setText("");

                        //Log.d(TAG, "Starting activity with Intent with action of " + encodedIntent.getAction());

                        startActivity(encodedIntent);
                    }
                });

        sendBitstringButton.setOnClickListener(
                new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String message = messsageEntry.getText().toString();

                        configureReceiver(EncodingScheme.NUM_BASE_VALUES, EncodingUtils.NUM_EXPANSION_CODES, EncodingUtils.ACTIONS.size(), EncodingUtils.DEFAULT_TEST_ID);

                        //Log.d(TAG, "Encoding message \"" + message + "\" as bitstring");
                        Collection<Intent> encodedIntents = bitstringEncoder.encodeMessage(message);
                        messsageEntry.setText("");

                        for(Intent encodedIntent: encodedIntents) {
                            if(encodedIntent.getExtras().isEmpty()) {
                                // Don't send empty Intents
                                Log.d(TAG, "Skipping Intent with action \"" + encodedIntent.getAction() + "\"; no data");
                                continue;
                            }

                            // TODO: Make these constants
                            ComponentName cn = new ComponentName("covertchannel.intent.receiver", "covertchannel.intent.receiver.MessageReceiver");
                            encodedIntent.setComponent(cn);

                            //Log.d(TAG, "Starting service with Intent with action of " + encodedIntent.getAction());
                            startService(encodedIntent);
                        }
                    }
                });

        sendBitstringInterceptibleButton.setOnClickListener(
                new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String message = messsageEntry.getText().toString();

                        // Note this does not reconfigure the receiver first
                        // TODO: configureReceiver(EncodingScheme.NUM_BASE_VALUES, EncodingUtils.NUM_EXPANSION_CODES, EncodingUtils.ACTIONS.size());
                        Intent encodedIntent = bitstringEncoderInterceptible.encodeMessage(message).iterator().next();
                        messsageEntry.setText("");

                        //Log.d(TAG, "Starting activity with Intent with action of " + encodedIntent.getAction());

                        startActivity(encodedIntent);
                    }
                });

        clearMessageStoreButton.setOnClickListener(
                new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        //Log.d(TAG, "Telling the receiver to clear its message store");
                        Intent resetMessageStoreIntent = new Intent();
                        resetMessageStoreIntent.setAction(EncodingUtils.CLEAR_MESSAGE_STORE_ACTION);
                        ComponentName cn = new ComponentName("covertchannel.intent.receiver", "covertchannel.intent.receiver.MessageReceiver");
                        resetMessageStoreIntent.setComponent(cn);

                        //Log.d(TAG, "Starting activity with Intent with action of " + resetMessageStoreIntent.getAction());
                        startService(resetMessageStoreIntent);
                    }
                });

        runThroughputTestButton.setOnClickListener(
                new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        //Log.d(TAG, "Initiating throughput test");
                        //Log.d(TAG, "Telling the receiver to clear its message store");
                        Intent resetMessageStoreIntent = new Intent();
                        resetMessageStoreIntent.setAction(EncodingUtils.CLEAR_MESSAGE_STORE_ACTION);
                        ComponentName cn = new ComponentName("covertchannel.intent.receiver", "covertchannel.intent.receiver.MessageReceiver");
                        resetMessageStoreIntent.setComponent(cn);

                        //Log.d(TAG, "Starting activity with Intent with action of " + resetMessageStoreIntent.getAction());
                        startService(resetMessageStoreIntent);

                        performThroughputTest();
                    }
                });
	}

    private void configureReceiver(int numBaseValues, int numExpansionCodes, int numActions, long testId) {
        Intent channelConfigIntent = new Intent();
        channelConfigIntent.setAction(EncodingUtils.SET_CHANNEL_CONFIGURATION_ACTION);
        channelConfigIntent.putExtra(EncodingUtils.NUM_BASE_VALUES_KEY, numBaseValues);
        channelConfigIntent.putExtra(EncodingUtils.NUM_EXPANSION_CODES_KEY, numExpansionCodes);
        channelConfigIntent.putExtra(EncodingUtils.NUM_ACTIONS_KEY, numActions);
        channelConfigIntent.putExtra(EncodingUtils.TEST_ID_KEY, testId);
        ComponentName cn = new ComponentName("covertchannel.intent.receiver", "covertchannel.intent.receiver.MessageReceiver");
        channelConfigIntent.setComponent(cn);

        Log.d(TAG, "Sending Intent to configure channel config values to: base val count: " + numBaseValues + ", expansion code count: " + numExpansionCodes + ", num actions: " + numActions + ", and test ID: " + testId);
        startService(channelConfigIntent);
        // TODO: Wait for acknowledgement?
    }

    private void updateTestingReport(List<TestRun> completedTests) {
        StringBuilder reportBldr = new StringBuilder();
        reportBldr.append("Message Bytes | Num Base Values | Num Ex Codes | Num Actions | Num Unique Vals | Avg Throughput\n");
        for (TestRun testRunInfo : completedTests) {
            reportBldr.append("" + testRunInfo.getNumMessageBytes() + " | " + testRunInfo.getNumBaseValues() + " | " +
                    testRunInfo.getNumExpansionCodes() + " | " + testRunInfo.getNumActions() + " | " +
                    testRunInfo.getNumUniqueValues() + " | " + testRunInfo.getAverageThroughput() + "\n");
        }

        String report = reportBldr.toString();
        Log.d(TAG, "Current test report:\n\n" + report);
        testResultsDisplay.setText(report);
    }

    // TODO: Remove?
    private void outputTestResults(String results) {
        testResults.append(results);
        testResultsDisplay.setText(testResults.toString());

        // TODO: Cleanup
        //writeTestResultsToFile(testResults.toString());
    }

    @Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public void onDestroy()
	{
		super.onDestroy();
		
		// TODO
		//stopService(new Intent("experiment"));
	}
}
