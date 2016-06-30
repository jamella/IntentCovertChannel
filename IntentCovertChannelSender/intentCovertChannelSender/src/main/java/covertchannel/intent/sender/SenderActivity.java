package covertchannel.intent.sender;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import intent.covertchannel.intentencoderdecoder.BitstringEncoder;
import intent.covertchannel.intentencoderdecoder.EncodingScheme;
import intent.covertchannel.intentencoderdecoder.EncodingUtils;
import intent.covertchannel.intentencoderdecoder.LowerCaseAlphaEncoder;

// TODO: Look into using InputFilters with the message entry EditText to limit
// the character values which can be typed to those supported by the current
// scheme
public class SenderActivity extends Activity {
    private static final String TAG = "covertchannel.intent.sender.SenderActivity";

    // TODO: Prune any overly-large message sizes (or add more if possible)
    private static final int[] THROUGHPUT_TEST_MESSAGE_SIZES_IN_BYTES = {1, 2, 4, 8, 16, 32, 64, 128, 256, 512, 1024, 2048, 4096, 8192, 16384, 32768, 65536, 131072, 262144, 524288, 1048576, 2097152, 4194304, 8388608, 16777216};

    private static final int[] EXPANSION_CODE_COUNTS =
           {0,
            1,
            2,
            (EncodingScheme.NUM_BASE_VALUES * 1) + 1,
            (EncodingScheme.NUM_BASE_VALUES * 2) + 1,
            (EncodingScheme.NUM_BASE_VALUES * 3) + 1,
            (EncodingScheme.NUM_BASE_VALUES * 4) + 1,
            (EncodingScheme.NUM_BASE_VALUES * 5) + 1,
            (EncodingScheme.NUM_BASE_VALUES * 10) + 1};

    private static final int[] ACTION_STRING_COUNTS = {1, 2, 3, 4, 5, 10, 15, 20, 25, 30, 35, 40, 45, 50, 75, 100};

    private EditText messsageEntry;
    private Button sendAlphaButton;
    private Button sendAlphaOrigInterceptibleButton;
    private Button sendBitstringButton;
    private Button sendBitstringInterceptibleButton;
    private Button clearMessageStoreButton;
    private Button runThroughputTestButton;

    private class ThroughputCalculationReceiver extends BroadcastReceiver {
        private List<Intent> messageIntents;
        private ThroughputTestInfoTable resultsTable;
        private int numBaseValues;
        private long startTime;
        private long endTime;
        private long totalTime;
        private int msgByesToSend;

        public ThroughputCalculationReceiver(List<Intent> messageIntents, int numBaseValues, long startTime, int msgByesToSend) {
            this.messageIntents = messageIntents;
            this.numBaseValues = numBaseValues;
            this.startTime = startTime;
            this.msgByesToSend = msgByesToSend;

            resultsTable = new ThroughputTestInfoTable();
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            long endTime = intent.getLongExtra(EncodingUtils.END_TIME_KEY, 0);

            double timeElapsed = Math.abs(endTime - startTime);

            totalTime += timeElapsed;
            totalBytesSent += bytesSent;

            updateTimeElapsedReport(timeElapsed, bytesSent);
            transEndTime = new Date().getTime();


            // TODO: Wait for acknowledgement plus end time

            resultsTable.recordTestRunResults(numMessageBytes, numBaseValues, numExpansionCodes, numActions);
        }

        public void performTestRun() {

        }

        public void start() {
            for(int numMessageBytes: THROUGHPUT_TEST_MESSAGE_SIZES_IN_BYTES) {
                final String message = readMessage(numMessageBytes);
                for(int numExpansionCodes: EXPANSION_CODE_COUNTS) {
                    for(int numActions: ACTION_STRING_COUNTS) {
                        final EncodingScheme bitstringEncoder = new BitstringEncoder(numBaseValues, numExpansionCodes, EncodingUtils.ACTIONS.subList(0, numActions), EncodingScheme.BUILD_VERSION);
                        // TODO: Start running tests and aggregating results

                        Log.d(TAG, "Encoding message \"" + message + "\" as bitstring");
                        Collection<Intent> encodedIntents = bitstringEncoder.encodeMessage(message);

                        for(Intent encodedIntent: encodedIntents) {
                            if(encodedIntent.getExtras().isEmpty()) {
                                // Don't send empty Intents
                                Log.d(TAG, "Skipping Intent with action \"" + encodedIntent.getAction() + "\"; no data");
                                continue;
                            }

                            // TODO: Make these constants
                            ComponentName cn = new ComponentName("covertchannel.intent.receiver", "covertchannel.intent.receiver.MessageReceiver");
                            encodedIntent.setComponent(cn);

                            Log.d(TAG, "Starting service with Intent with action of " + encodedIntent.getAction());
                            startService(encodedIntent);
                        }
                    }
                }
            }
        }
    }

    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

        // TODO: Remove concept of enhanced alpha encoder
        messsageEntry = (EditText) findViewById(R.id.message_entry);
		sendAlphaButton = (Button) findViewById(R.id.send_alpha_orig_button);
        sendAlphaOrigInterceptibleButton = (Button) findViewById(R.id.send_alpha_orig_interceptible_button);
        sendBitstringButton = (Button) findViewById(R.id.send_bitstring_button);
        sendBitstringInterceptibleButton = (Button) findViewById(R.id.send_bitstring_interceptible_button);
        clearMessageStoreButton = (Button) findViewById(R.id.send_clear_message_store_intent);
        runThroughputTestButton = (Button) findViewById(R.id.run_throughput_test_button);

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

                        Log.d(TAG, "Encoding message \"" + message + "\"");
                        Intent encodedIntent = alphaEncoder.encodeMessage(message).iterator().next();
                        messsageEntry.setText("");

                        // TODO: Move into encoding schemes/ use constants
                        ComponentName cn = new ComponentName("covertchannel.intent.receiver", "covertchannel.intent.receiver.MessageReceiver");
                        encodedIntent.setComponent(cn);

                        Log.d(TAG, "Starting activity with Intent with action of " + encodedIntent.getAction());
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

                        Log.d(TAG, "Starting activity with Intent with action of " + encodedIntent.getAction());

                        startActivity(encodedIntent);
                    }
                });

        sendBitstringButton.setOnClickListener(
                new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String message = messsageEntry.getText().toString();

                        Log.d(TAG, "Encoding message \"" + message + "\" as bitstring");
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

                            Log.d(TAG, "Starting service with Intent with action of " + encodedIntent.getAction());
                            startService(encodedIntent);
                        }
                    }
                });

        sendBitstringInterceptibleButton.setOnClickListener(
                new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String message = messsageEntry.getText().toString();
                        Intent encodedIntent = bitstringEncoderInterceptible.encodeMessage(message).iterator().next();
                        messsageEntry.setText("");

                        Log.d(TAG, "Starting activity with Intent with action of " + encodedIntent.getAction());

                        startActivity(encodedIntent);
                    }
                });

        clearMessageStoreButton.setOnClickListener(
                new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Log.d(TAG, "Telling the receiver to clear its message store");
                        Intent resetMessageStoreIntent = new Intent();
                        resetMessageStoreIntent.setAction(EncodingUtils.CLEAR_MESSAGE_STORE_ACTION);
                        ComponentName cn = new ComponentName("covertchannel.intent.receiver", "covertchannel.intent.receiver.MessageReceiver");
                        resetMessageStoreIntent.setComponent(cn);

                        Log.d(TAG, "Starting activity with Intent with action of " + resetMessageStoreIntent.getAction());
                        startService(resetMessageStoreIntent);
                    }
                });

        runThroughputTestButton.setOnClickListener(
                new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Log.d(TAG, "Initiating throughput test");
                        Log.d(TAG, "Telling the receiver to clear its message store");
                        Intent resetMessageStoreIntent = new Intent();
                        resetMessageStoreIntent.setAction(EncodingUtils.CLEAR_MESSAGE_STORE_ACTION);
                        ComponentName cn = new ComponentName("covertchannel.intent.receiver", "covertchannel.intent.receiver.MessageReceiver");
                        resetMessageStoreIntent.setComponent(cn);

                        Log.d(TAG, "Starting activity with Intent with action of " + resetMessageStoreIntent.getAction());
                        startService(resetMessageStoreIntent);

                        performThroughputTest();
                    }
                });
	}

    // TODO: Organize this better
    private class ThroughputTestInfoTable {
        private static final String NUM_MESSAGE_BYTES_KEY = "number of message bytes";
        private static final String NUM_BASE_VALUES_KEY = "number of base values";
        private static final String NUM_EXPANSION_CODES_KEY = "number of expansion codes";
        private static final String NUM_ACTIONS_KEY = "number of action strings";
        private static final String START_TIME_KEY = "start time";
        private static final String END_TIME_KEY = "end time";

        private Map<Integer, Map<String, Long>> resultsTable;

        public ThroughputTestInfoTable() {
            resultsTable = new HashMap<>();

            for (int numMessageBytes : THROUGHPUT_TEST_MESSAGE_SIZES_IN_BYTES) {
                final String message = readMessage(numMessageBytes);
                for (int numExpansionCodes : EXPANSION_CODE_COUNTS) {
                    for (int numActions : ACTION_STRING_COUNTS) {
                        initializeTestRunResultsEntry(numMessageBytes, numBaseValues, numExpansionCodes, numActions, startTime);
                    }
                }
            }
        }

        public void initializeTestRunResultsEntry(int numMessageBytes, int numBaseValues, int numExpansionCodes, int numActions, long startTime) {
            Map<String, Long> testRunResults = new HashMap<>();
            testRunResults.put(NUM_BASE_VALUES_KEY, (long) numBaseValues);
            testRunResults.put(NUM_EXPANSION_CODES_KEY, (long) numExpansionCodes);
            testRunResults.put(NUM_ACTIONS_KEY, (long) numActions);
            testRunResults.put(START_TIME_KEY, startTime);
            testRunResults.put(END_TIME_KEY, null);

            resultsTable.put(numMessageBytes, testRunResults);
        }

        public void recordTestRunResults(int numMessageBytes, int numBaseValues, int numExpansionCodes, int numActions, long endTime) {
            // TODO: Lookup existing entry if it exists
            Map<String, Long> testRunResults = new HashMap<>();
            testRunResults.put(NUM_MESSAGE_BYTES_KEY, (long) numMessageBytes);
            testRunResults.put(NUM_BASE_VALUES_KEY, (long) numBaseValues);
            testRunResults.put(NUM_EXPANSION_CODES_KEY, (long) numExpansionCodes);
            testRunResults.put(NUM_ACTIONS_KEY, (long) numActions);
            testRunResults.put(END_TIME_KEY, endTime);

            resultsTable.put(numMessageBytes, testRunResults);
        }

        public List<Map<String, Integer>> getResultsTable() {
            return resultsTable;
        }
    }

    private void performThroughputTest() {
        // TODO: Add evaluating the original implementation

        // TODO: Find a good way to make this variable (?)
        final int numBaseValues = EncodingScheme.NUM_BASE_VALUES;

        ThroughputCalculationReceiver endTimeReceiver = new ThroughputCalculationReceiver();

        IntentFilter filter = new IntentFilter();
        filter.addAction(EncodingUtils.SEND_TIME_ACTION);
        registerReceiver(endTimeReceiver, filter);

        transStartTime = new Date().getTime();


        // TODO: Output results
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

    private String readMessage(int bytesToSend) {
        return EncodingUtils.readRawTextFile(this, R.raw.taming_of_the_shrew, bytesToSend);
    }
}
