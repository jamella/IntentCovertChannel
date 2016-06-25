package covertchannel.intent.receiver;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import intent.covertchannel.intentencoderdecoder.BitstringEncoder;
import intent.covertchannel.intentencoderdecoder.EncodingScheme;
import intent.covertchannel.intentencoderdecoder.EncodingUtils;
import intent.covertchannel.intentencoderdecoder.LowerCaseAlphaEncoder;

public class MessageReceiver extends Service {
    protected static final String MESSAGE_STORE_KEY = "covert_message_store";
    protected static final String MESSAGE_READY_KEY = "message_ready";

    private static final String ALPHA_ENCODED_MESSAGE_STORAGE_KEY = "alpha_encoded";
    private static final String BITSTRING_ENCODED_MESSAGE_STORAGE_KEY = "bitstring_encoded";

    // TODO: Make this configurable
    private static final int BUILD_VERSION = 8;
    private static final String TAG = "covertchannel.intent.receiver.MessageReceiver";

    // Used to persist received messages so that they can be accessed later
    private SharedPreferences messageStore;

	// This is the object that receives interactions from clients.
    private final IBinder mBinder = new MessageBinder();

    @Override
    public void onCreate() {
		super.onCreate();
        Log.d(TAG, "Receiver service starting");
		messageStore = getSharedPreferences(MESSAGE_STORE_KEY, MODE_PRIVATE);
	}

    private void handleIntent(Intent intent) {
        if(matchesFilter(intent)) {
            Log.d(TAG, "Intent filter matched for action " + intent.getAction());

            long endTime = new Date().getTime();

            String intentAction = intent.getAction();
            if(intentAction.equals(EncodingUtils.CALCULATE_THROUGHPUT_ACTION)) {
                Intent responseIntent = new Intent();
                responseIntent.setAction(EncodingUtils.SEND_TIME_ACTION);
                responseIntent.putExtra(EncodingUtils.END_TIME_KEY, endTime);
                sendBroadcast(responseIntent);
            } else if(intentAction.equals(EncodingUtils.CALCULATE_BIT_ERROR_RATE)) {
                LowerCaseAlphaEncoder alphaEncoder = new LowerCaseAlphaEncoder(EncodingScheme.NUM_BASE_VALUES, EncodingUtils.NUM_ALPHA_EXPANSION_CODES, Collections.singleton(EncodingUtils.ALPHA_ENCODING_ACTION), EncodingScheme.BUILD_VERSION);
                String receivedMessage = alphaEncoder.decodeMessage(intent);

                Intent responseIntent = new Intent();
                responseIntent.setAction(EncodingUtils.SEND_BIT_ERRORS_ACTION);
                responseIntent.putExtra(EncodingUtils.END_TIME_KEY, endTime);
                responseIntent.putExtra(EncodingUtils.RECEIVED_MESSAGE_KEY, receivedMessage);

                sendBroadcast(responseIntent);
            } else if(intentAction.equals(EncodingUtils.ALPHA_ENCODING_ACTION)) {
                decodeAndStore(intent);
            } else { // Must be a bitstring-encoded Intent
                decodeAndStoreBitstring(intent);
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Receiver service onStart(): action = " + intent.getAction());
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
	    		EncodingUtils.CALCULATE_THROUGHPUT_ACTION.equals(intent.getAction()) ||
	    		EncodingUtils.CALCULATE_BIT_ERROR_RATE.equals(intent.getAction()) ||
                EncodingUtils.ACTIONS.contains(intent.getAction()));
    }
	
	private void decodeAndStore(Intent messageIntent) {
        LowerCaseAlphaEncoder alphaEncoder = new LowerCaseAlphaEncoder(EncodingScheme.NUM_BASE_VALUES, EncodingUtils.NUM_ALPHA_EXPANSION_CODES, Collections.singleton(EncodingUtils.ALPHA_ENCODING_ACTION), EncodingScheme.BUILD_VERSION);
        String message = alphaEncoder.decodeMessage(messageIntent);

        SharedPreferences.Editor messageStoreEditor = messageStore.edit();
    	messageStoreEditor.putString(ALPHA_ENCODED_MESSAGE_STORAGE_KEY, message);
        messageStoreEditor.putBoolean(MESSAGE_READY_KEY, true);
    	messageStoreEditor.commit();
	}

    private void decodeAndStoreBitstring(Intent messageIntent) {
        BitstringEncoder bitstringEncoder = new BitstringEncoder(EncodingScheme.NUM_BASE_VALUES, EncodingUtils.NUM_EXPANSION_CODES, EncodingUtils.ACTIONS, EncodingScheme.BUILD_VERSION);
        bitstringEncoder.decodeMessageAsBitstring(messageIntent);

        // TODO: Implement and integrate the concept of segment number metadata fields; don't display message until fully arrive
        // TODO: Write that concept into paper

        // TODO: Finish implementing
        List<String> orderedMessageActionStrings = bitstringEncoder.getOrderedMessageActionStrings();
        Map<String, String> actionToMessageMap = bitstringEncoder.getActionToMessageMap();

        SharedPreferences.Editor messageStoreEditor = messageStore.edit();
        StringBuilder bitstringBldr = new StringBuilder();
        for(String action: orderedMessageActionStrings) {
            String bitstring = actionToMessageMap.get(action);
            Log.d(TAG, "Found bitstring of \"" + bitstring + "\" for action \"" + action + "\"");

            if(bitstring != null) {
                bitstringBldr.append(bitstring);
                messageStoreEditor.putString(action, bitstring);
            }
        }

        // TODO: Cleanup
        // TODO: Make sure that this works
        //String message = BitstringEncoder.bitStringToStr(bitstringBldr.toString());

        // TODO: Implement check for the number of segments received (stored by action); if all have been received
        // set to true; otherwise, reset to false (also need to clear out the action key values)
        messageStoreEditor.putBoolean(MESSAGE_READY_KEY, false);
        messageStoreEditor.commit();
    }

	public class MessageBinder extends Binder {
		public MessageReceiver getService()
		{
			return MessageReceiver.this;
		}
	}
}
