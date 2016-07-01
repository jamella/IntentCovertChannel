package covertchannel.intent.receiver;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import intent.covertchannel.intentencoderdecoder.AlphabeticalKeySequenceComparator;
import intent.covertchannel.intentencoderdecoder.BitstringEncoder;
import intent.covertchannel.intentencoderdecoder.EncodingScheme;
import intent.covertchannel.intentencoderdecoder.EncodingUtils;
import intent.covertchannel.intentencoderdecoder.LowerCaseAlphaEncoder;

public class MessageReceiver extends Service {
    protected static final String MESSAGE_STORE_KEY = "covert_message_store";
    public static final String ALPHA_ENCODED_MESSAGE_STORAGE_KEY = "alpha_encoded";
    public static final String BITSTRING_ENCODED_MESSAGE_STORAGE_KEY = "bitstring_encoded";
    private static final String ELAPSED_TIME_MESSAGE_STORAGE_KEY = "elapsed_time";

    // TODO: Cleanup
    //private static final String TAG = "covertchannel.intent.receiver.MessageReceiver";
    private static final String TAG = EncodingUtils.TRACE_TAG;

    // Used to persist received messages so that they can be accessed later
    private SharedPreferences messageStore;

	// This is the object that receives interactions from clients.
    private final IBinder mBinder = new MessageBinder();
    private Integer numBaseValues;
    private Integer numExpansionCodes;
    private Integer numActions;
    private long testId;
    private int segmentsSeen;

    @Override
    public void onCreate() {
		super.onCreate();

        Log.d(TAG, "Receiver service starting");
		messageStore = getSharedPreferences(MESSAGE_STORE_KEY, MODE_PRIVATE);
        numBaseValues = null;
        numExpansionCodes = null;
        numActions = null;
        testId = EncodingUtils.DEFAULT_TEST_ID;
        segmentsSeen = 0;
	}

    private void handleIntent(Intent intent) {
        if(matchesFilter(intent)) {
            Log.d(TAG, "Intent filter matched for action " + intent.getAction());

            // TODO: Cleanup
            //long endTime = new Date().getTime();
            double endTime = EncodingUtils.getTimeMillisAccurate();

            // TODO: Throughput calculations to work for bitstring and alpha-encoding methods
            String intentAction = intent.getAction();
            if(intentAction.equals(EncodingUtils.CALCULATE_THROUGHPUT_ACTION_ALPHA_ENCODING)) {
                Intent responseIntent = new Intent();
                responseIntent.setAction(EncodingUtils.SEND_TIME_ACTION);

                // TODO: Cleanup
                //responseIntent.putExtra(EncodingUtils.END_TIME_KEY, System.currentTimeMillis());
                responseIntent.putExtra(EncodingUtils.END_TIME_KEY, endTime);
                sendBroadcast(responseIntent);
            } else if(intentAction.equals(EncodingUtils.CALCULATE_THROUGHPUT_ACTION_BITSTRING_ENCODING)) {
                Log.d(TAG, "Received CALCULATE_THROUGHPUT_ACTION_BITSTRING_ENCODING action");
                // TODO: Cleanup

                segmentsSeen++;

                BitstringEncoder bitstringEncoder;
                if(numBaseValues != null && numExpansionCodes != null && numActions != null) {
                    Log.d(TAG, "Initializing bitstring encoder with values: base val count: " + numBaseValues + ", expansion code count: " + numExpansionCodes + ", num actions: " + numActions);
                    bitstringEncoder = new BitstringEncoder(numBaseValues, numExpansionCodes, EncodingUtils.ACTIONS.subList(0, numActions), EncodingScheme.BUILD_VERSION);
                } else {
                    Log.d(TAG, "Initializing bitstring encoder with values: base val count: " + EncodingScheme.NUM_BASE_VALUES + ", expansion code count: " + EncodingUtils.NUM_EXPANSION_CODES + ", num actions: " + EncodingUtils.ACTIONS.size());
                    bitstringEncoder = new BitstringEncoder(EncodingScheme.NUM_BASE_VALUES, EncodingUtils.NUM_EXPANSION_CODES, EncodingUtils.ACTIONS, EncodingScheme.BUILD_VERSION);
                }

                Bundle dataBundle = intent.getExtras();
                Set<String> bundleKeys = new TreeSet<>(new AlphabeticalKeySequenceComparator());
                bundleKeys.addAll(dataBundle.keySet());
                Iterator<String> bundleKeyIter = bundleKeys.iterator();
                String sigBitsInLastFragmentKey = bundleKeyIter.next();
                String segmentNumberKey = bundleKeyIter.next();
                String segmentCountKey = bundleKeyIter.next();

                // Metadata keys do not use the action offset
                //String sigBitsMetadataBitstring = bitstringEncoder.decodeFragmentAsBitstring(dataBundle, sigBitsInLastFragmentKey, 0, bitstringEncoder.getFragmentMinBitLength());
                //String segmentNumberBitstring = bitstringEncoder.decodeFragmentAsBitstring(dataBundle, segmentNumberKey, 0, bitstringEncoder.getFragmentMinBitLength());
                String segmentCountBitstring = bitstringEncoder.decodeFragmentAsBitstring(dataBundle, segmentCountKey, 0, bitstringEncoder.getFragmentMinBitLength());

                //int numSigBitsInLastFragment = bitstringEncoder.bitstringToInt(sigBitsMetadataBitstring);
                //int segmentNumber = bitstringEncoder.bitstringToInt(segmentNumberBitstring);
                int segmentCount = bitstringEncoder.bitstringToInt(segmentCountBitstring);

                if(segmentsSeen < segmentCount) {
                    Log.d(TAG, "Sending acknowledgement for test ID: " + testId);
                    Intent responseIntent = new Intent();
                    responseIntent.setAction(EncodingUtils.ACKNOWLEDGE_MESSAGE_SEGMENT_ACTION);
                    responseIntent.putExtra(EncodingUtils.TEST_ID_KEY, testId);
                    sendBroadcast(responseIntent);
                } else {
                    Log.d(TAG, "Sending response with end time = " + endTime + " and test ID of " + testId);

                    // Note: this does not account for decoding time
                    Intent responseIntent = new Intent();
                    responseIntent.setAction(EncodingUtils.SEND_TIME_ACTION);
                    //responseIntent.putExtra(EncodingUtils.END_TIME_KEY, System.currentTimeMillis());
                    responseIntent.putExtra(EncodingUtils.END_TIME_KEY, endTime);
                    responseIntent.putExtra(EncodingUtils.TEST_ID_KEY, testId);
                    sendBroadcast(responseIntent);
                }

                //decodeAndStoreBitstring(intent);
            } else if(intentAction.equals(EncodingUtils.CALCULATE_BIT_ERROR_RATE)) {
                LowerCaseAlphaEncoder alphaEncoder = new LowerCaseAlphaEncoder(EncodingScheme.NUM_BASE_VALUES, EncodingUtils.NUM_ALPHA_EXPANSION_CODES, Collections.singleton(EncodingUtils.ALPHA_ENCODING_ACTION), EncodingScheme.BUILD_VERSION);
                String receivedMessage = alphaEncoder.decodeMessage(intent);

                Intent responseIntent = new Intent();
                responseIntent.setAction(EncodingUtils.SEND_BIT_ERRORS_ACTION);

                // TODO: Cleanup
                //responseIntent.putExtra(EncodingUtils.END_TIME_KEY, System.currentTimeMillis());
                responseIntent.putExtra(EncodingUtils.END_TIME_KEY, endTime);
                responseIntent.putExtra(EncodingUtils.RECEIVED_MESSAGE_KEY, receivedMessage);

                sendBroadcast(responseIntent);
            } else if(intentAction.equals(EncodingUtils.CLEAR_MESSAGE_STORE_ACTION)) {
                Log.d(TAG, "Clearing the message store");
                messageStore.edit().clear().commit();
            } else if(intentAction.equals(EncodingUtils.ALPHA_ENCODING_ACTION)) {
                decodeAndStore(intent);
            } else if(intentAction.equals(EncodingUtils.SET_CHANNEL_CONFIGURATION_ACTION)) {
                Bundle extras = intent.getExtras();
                this.numBaseValues = extras.getInt(EncodingUtils.NUM_BASE_VALUES_KEY);
                this.numExpansionCodes = extras.getInt(EncodingUtils.NUM_EXPANSION_CODES_KEY);
                this.numActions = extras.getInt(EncodingUtils.NUM_ACTIONS_KEY);
                this.testId = extras.getLong(EncodingUtils.TEST_ID_KEY, EncodingUtils.DEFAULT_TEST_ID);

                Log.d(TAG, "Configuring channel config values to: base val count: " + numBaseValues + ", expansion code count: " + numExpansionCodes + ", num actions: " + numActions);
            } else {
                // Must be a bitstring-encoded Intent
                decodeAndStoreBitstring(intent);
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //Log.d(TAG, "Receiver service onStart(): action = " + intent.getAction());
        handleIntent(intent);

    	// TODO: Explore the possibility of stopping the service using 
    	// stopSelf() after the message Intent has been received and 
    	// persisted
        return START_NOT_STICKY;
    }

	@Override
    public void onDestroy() {
    	super.onDestroy();

        // TODO: Remove
        //unregisterReceiver(receiver);
    }

    @Override
    public IBinder onBind(Intent intent) {
        handleIntent(intent);
        return mBinder;
    }

    private boolean matchesFilter(Intent intent) {
	    // TODO: Implement Intent filtering so that only Intents which are
		// covertly marked as containing a covert message (such as through
		// the ClipData channel) will attempted to be decoded
		
		// TODO: Represent the Intent filters as a custom class to allow the
		// signature/pattern/microprotocol being used and looked for to
		// vary independently
	    return (EncodingUtils.ALPHA_ENCODING_ACTION.equals(intent.getAction()) ||
	    		EncodingUtils.CALCULATE_THROUGHPUT_ACTION_ALPHA_ENCODING.equals(intent.getAction()) ||
                EncodingUtils.CALCULATE_THROUGHPUT_ACTION_BITSTRING_ENCODING.equals(intent.getAction()) ||
	    		EncodingUtils.CALCULATE_BIT_ERROR_RATE.equals(intent.getAction()) ||
                EncodingUtils.CLEAR_MESSAGE_STORE_ACTION.equals(intent.getAction()) ||
                EncodingUtils.SET_CHANNEL_CONFIGURATION_ACTION.equals(intent.getAction()) ||
                EncodingUtils.ACTIONS.contains(intent.getAction()));
    }
	
	private void decodeAndStore(Intent messageIntent) {
        LowerCaseAlphaEncoder alphaEncoder = new LowerCaseAlphaEncoder(EncodingScheme.NUM_BASE_VALUES, EncodingUtils.NUM_ALPHA_EXPANSION_CODES, Collections.singleton(EncodingUtils.ALPHA_ENCODING_ACTION), EncodingScheme.BUILD_VERSION);
        String message = alphaEncoder.decodeMessage(messageIntent);

        SharedPreferences.Editor messageStoreEditor = messageStore.edit();
    	messageStoreEditor.putString(ALPHA_ENCODED_MESSAGE_STORAGE_KEY, message);
    	messageStoreEditor.commit();
	}

    private void decodeAndStoreBitstring(Intent messageIntent) {
        // TODO: Cleanup
        //long startTime = System.currentTimeMillis();
        double startTime = EncodingUtils.getTimeMillisAccurate();
        Log.d(TAG, "Starting to decode message: current action \"" + messageIntent.getAction() + "\"; start time = " + startTime);
        // TODO: Cleanup
        //printMessageStore();

        BitstringEncoder bitstringEncoder;
        if(numBaseValues != null && numExpansionCodes != null && numActions != null) {
            Log.d(TAG, "Initializing bitstring encoder with values: base val count: " + numBaseValues + ", expansion code count: " + numExpansionCodes + ", num actions: " + numActions);
            bitstringEncoder = new BitstringEncoder(numBaseValues, numExpansionCodes, EncodingUtils.ACTIONS.subList(0, numActions), EncodingScheme.BUILD_VERSION);
        } else {
            Log.d(TAG, "Initializing bitstring encoder with values: base val count: " + EncodingScheme.NUM_BASE_VALUES + ", expansion code count: " + EncodingUtils.NUM_EXPANSION_CODES + ", num actions: " + EncodingUtils.ACTIONS.size());
            bitstringEncoder = new BitstringEncoder(EncodingScheme.NUM_BASE_VALUES, EncodingUtils.NUM_EXPANSION_CODES, EncodingUtils.ACTIONS, EncodingScheme.BUILD_VERSION);
        }

        BitstringEncoder.DecodedMessage messageSegment = bitstringEncoder.decodeMessageAsBitstring(messageIntent);
        Map<String, String> fragmentBitstringsByMessageKey = messageSegment.getFragmentBitstringsByMessageKey();

        //Log.d(TAG, mapToString(fragmentBitstringsByMessageKey, "Fragment Bitstrings by Message Key:"));

        int segmentNumber = messageSegment.getSegmentNumber();
        int segmentCount = messageSegment.getSegmentCount();

        //Log.d(TAG, "Current segment number: " + segmentNumber + ", segment count = " + segmentCount);

        SharedPreferences.Editor messageStoreEditor = messageStore.edit();
        Set<String> remainingSegmentKeys = new HashSet<>();
        Map<String, String> masterBitstringMap = new HashMap<>();
        boolean messageIsComplete = true;
        for(int i = 0; i < segmentCount; i++) {
            String key = String.valueOf(i);
            if(segmentNumber == i) {
                /*
                if(!messageStore.contains(key)) {
                    Log.d(TAG, "Storing bitstring map for message key \"" + key + "\" (i.e. storing the action key set and then storing the value for each key)");
                } else {
                    // TODO: Cleanup
                    //throw new RuntimeException("Encountered existing entry for message key \"" + key + "\"");
                    Log.d(TAG, "Encountered existing entry for message key \"" + key + "\": \"" + messageStore.getString(key, "") + "\"");
                }
                */

                masterBitstringMap.putAll(fragmentBitstringsByMessageKey);
                storeStringSet(messageStoreEditor, key, fragmentBitstringsByMessageKey.keySet());
                storeMessageFragments(messageStoreEditor, fragmentBitstringsByMessageKey);
                //Log.d(TAG, "Commiting changes to the message store");
                messageStoreEditor.commit();
                messageStoreEditor = messageStore.edit();
            } else if(!messageStore.contains(key)) {
                Log.d(TAG, "No bitstring found for segment key \"" + key + "\"; message is incomplete. Will wait for more segments to arrive.");
                messageIsComplete = false;
                break;
            } else {
                remainingSegmentKeys.add(key);
            }
        }

        // TODO: Cleanup
        //long endTime = System.currentTimeMillis();
        double endTime = EncodingUtils.getTimeMillisAccurate();

        /* TODO: Cleanup
        long elapsedTime = endTime - startTime;

        if(messageStore.contains(ELAPSED_TIME_MESSAGE_STORAGE_KEY)) {
            elapsedTime += messageStore.getLong(ELAPSED_TIME_MESSAGE_STORAGE_KEY, 0);
        }
        */

        if(messageIsComplete) {
            Log.d(TAG, "Message is complete");

            // TODO: Cleanup
            Intent responseIntent = new Intent();
            responseIntent.setAction(EncodingUtils.SEND_TIME_ACTION);
            responseIntent.putExtra(EncodingUtils.END_TIME_KEY, endTime);
            responseIntent.putExtra(EncodingUtils.TEST_ID_KEY, testId);

            // TODO: Cleanup
            //responseIntent.putExtra(EncodingUtils.ELAPSED_TIME_KEY, elapsedTime);

            Log.d(TAG, "Sending response with end time = " + endTime + " and test ID of " + testId);
            Log.d(TAG, "Sending end time response of " + endTime);
            //Log.d(TAG, "Sending elapsed time response of " + elapsedTime);
            sendBroadcast(responseIntent);

            for(String segmentKey: remainingSegmentKeys) {
                //Log.d(TAG, "Retrieving fragment map for segment key \"" + segmentKey + "\"");
                printMessageStore();

                Map<String, String> segmentBitstringMap = retrieveMessageFragmentMap(messageStore, segmentKey);
                masterBitstringMap.putAll(segmentBitstringMap);
            }

            String message = fragmentMapToBitstring(masterBitstringMap);
            messageStoreEditor.clear();
            messageStoreEditor.putString(BITSTRING_ENCODED_MESSAGE_STORAGE_KEY, message);
        } else {
            Log.d(TAG, "Message is not complete; sending segment acknowledgement");

            Intent responseIntent = new Intent();
            responseIntent.setAction(EncodingUtils.ACKNOWLEDGE_MESSAGE_SEGMENT_ACTION);
            sendBroadcast(responseIntent);

            /* TODO: Cleanup
            Log.d(TAG, "Message is not complete; storing elapsed time");
            messageStoreEditor.putLong(ELAPSED_TIME_MESSAGE_STORAGE_KEY, elapsedTime);
            */
        }

        messageStoreEditor.commit();
    }

    private void printMessageStore() {
        /*
        Map<String, ?> messageStoreEntries = messageStore.getAll();
        Map<String, String> messsageStoreStringMap = new HashMap<>();
        for(Map.Entry<String, ?> entry: messageStoreEntries.entrySet()) {
            String key = entry.getKey();
            String value = (String) entry.getValue();
            messsageStoreStringMap.put(key, value);
        }

        Log.d(TAG, mapToString(messsageStoreStringMap, "Message Store Data:"));
        */
    }

    private static String mapToString(Map<String, String> stringMap, String header) {
        StringBuilder messageStoreStrBldr = new StringBuilder();
        messageStoreStrBldr.append(header);
        messageStoreStrBldr.append("\n");

        for(Map.Entry<String, ?> entry: stringMap.entrySet()) {
            String key = entry.getKey();
            String value = (String) entry.getValue();
            messageStoreStrBldr.append("\t");
            messageStoreStrBldr.append(key);
            messageStoreStrBldr.append(" => ");
            messageStoreStrBldr.append(value);
            messageStoreStrBldr.append("\n");
        }

        return messageStoreStrBldr.toString();
    }

    private static void storeStringSet(SharedPreferences.Editor messageStoreEditor, String key, Set<String> valueSet) {
        StringBuilder joinedStringBldr = new StringBuilder();

        Iterator<String> valueIter = valueSet.iterator();
        String value = valueIter.next();
        while(valueIter.hasNext()) {
            joinedStringBldr.append(value);
            joinedStringBldr.append(", ");
            value = valueIter.next();
        }
        joinedStringBldr.append(value);

        //Log.d(TAG, "Storing: key -> \"" + key + "\"; value -> \"" + joinedStringBldr.toString() + "\"");
        messageStoreEditor.putString(key, joinedStringBldr.toString());
    }

    private static Set<String> retrieveStringSet(SharedPreferences messageStore, String key) {
        String joinedString = messageStore.getString(key, "");
        //Log.d(TAG, "Retrieved: key -> \"" + key + "\"; value -> \"" + joinedString + "\"");

        String[] values = joinedString.split(",");

        Set<String> valueSet = new HashSet<>();
        for(String value: values) {
            valueSet.add(value.trim());
        }

        return valueSet;
    }

    private static void storeMessageFragments(SharedPreferences.Editor messageStoreEditor, Map<String, String> fragmentBitstringsByMessageKey) {
        for(Map.Entry<String, String> fragmentAndMessageKey: fragmentBitstringsByMessageKey.entrySet()) {
            String key = fragmentAndMessageKey.getKey();
            String value = fragmentAndMessageKey.getValue();
            //Log.d(TAG, "Storing: key -> \"" + key + "\"; value -> \"" + value + "\"");
            messageStoreEditor.putString(key, value);
        }
    }

    private static Map<String, String> retrieveMessageFragmentMap(SharedPreferences messageStore, String segmentKey) {
        Map<String, String> fragmentBitstringsByMessageKey = new HashMap<>();
        Set<String> segmentKeys = retrieveStringSet(messageStore, segmentKey);
        for(String key: segmentKeys) {
            //Log.d(TAG, "Retrieving stored fragment bitstring for key \"" + key + "\"");
            String fragmentBitstring = messageStore.getString(key, null);
            if(fragmentBitstring != null) {
                //Log.d(TAG, "Retrieved bitstring \"" + fragmentBitstring + "\" for key \"" + key + "\"");
                fragmentBitstringsByMessageKey.put(key, fragmentBitstring);
            } else {
                throw new RuntimeException("Null fragment bitstring retrieved from the message store for key \"" + key + "\" and segment key \"" + segmentKey + "\"");
            }
        }

        return fragmentBitstringsByMessageKey;
    }

    private static String fragmentMapToBitstring(Map<String, String> fragmentBitstringsByMessageKey) {
        StringBuilder bitstringBldr = new StringBuilder();
        Set<String> msgKeys = new TreeSet<>(new AlphabeticalKeySequenceComparator());
        msgKeys.addAll(fragmentBitstringsByMessageKey.keySet());
        for(String key: msgKeys) {
            bitstringBldr.append(fragmentBitstringsByMessageKey.get(key));
        }

        String joinedBitstring = bitstringBldr.toString();
        //Log.d(TAG, "Retrieved joined bitstring of \"" + joinedBitstring + "\"");

        return BitstringEncoder.bitStringToStr(joinedBitstring);
    }

    // TODO: Remove?
    private void clearMessageSegmentEntries(SharedPreferences.Editor messageStoreEditor) {
        Map<String, ?> messageStoreEntries = messageStore.getAll();
        for(Map.Entry<String, ?> entry: messageStoreEntries.entrySet()) {
            String key = entry.getKey();
            if(!(key.equals(ALPHA_ENCODED_MESSAGE_STORAGE_KEY) || key.equals(BITSTRING_ENCODED_MESSAGE_STORAGE_KEY))) {
                //Log.d(TAG, "Removing message store key \"" + key + "\"");
                messageStoreEditor.remove(key);
            } /*else {
                Log.d(TAG, "Skipping message store key \"" + key + "\"");
            }*/
        }
    }

    public class MessageBinder extends Binder {
		public MessageReceiver getService()
		{
			return MessageReceiver.this;
		}
	}
}
