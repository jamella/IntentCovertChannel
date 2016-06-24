package covertchannel.intent.receiver;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import intent.covertchannel.intentencoderdecoder.AlphabeticalKeySequence;
import intent.covertchannel.intentencoderdecoder.BitstringEncoder;
import intent.covertchannel.intentencoderdecoder.EncodingScheme;
import intent.covertchannel.intentencoderdecoder.EncodingUtils;
import intent.covertchannel.intentencoderdecoder.LowerCaseAlphaEncoder;

public class MessageReceiver extends Service {
    protected static final String MESSAGE_STORE_KEY = "covert_message_store";
	
    // TODO: Make this configurable
    private static final int BUILD_VERSION = 8;
    private static final String TAG = "covertchannel.intent.receiver.MessageReceiver";

    // Used to persist received messages so that they can be accessed later
    private SharedPreferences messageStore;

    // The schema to use for decoding received messages
    private LowerCaseAlphaEncoder alphaEncoder;

    private EncodingScheme bitstringEncoder;

    // TODO: Incorporate BitString encoder (needs separate action set)

    // Generates an alphabetical sequence of keys for storing messages
    private AlphabeticalKeySequence keySequence;
    
	// This is the object that receives interactions from clients.
    private final IBinder mBinder = new MessageBinder();


    @Override
    public void onCreate()
	{
		super.onCreate();

        Log.d(TAG, "Receiver service starting");

		messageStore = getSharedPreferences(MESSAGE_STORE_KEY, MODE_PRIVATE);
        alphaEncoder = new LowerCaseAlphaEncoder(EncodingScheme.NUM_BASE_VALUES, EncodingUtils.NUM_ALPHA_EXPANSION_CODES, Collections.singleton(EncodingUtils.ALPHA_ENCODING_ACTION), EncodingScheme.BUILD_VERSION);
        bitstringEncoder = new BitstringEncoder(EncodingScheme.NUM_BASE_VALUES, EncodingUtils.NUM_EXPANSION_CODES, EncodingUtils.ACTIONS, EncodingScheme.BUILD_VERSION);
		keySequence = new AlphabeticalKeySequence();
	}

    private void handleIntent(Intent intent) {
        if(matchesFilter(intent)) {
            Log.d(TAG, "Intent filter matched for action " + intent.getAction());

            long endTime = new Date().getTime();

            String intentAction = intent.getAction();
            if(intentAction.equals(EncodingUtils.CALCULATE_THROUGHPUT_ACTION))
            {
                Intent responseIntent = new Intent();
                responseIntent.setAction(EncodingUtils.SEND_TIME_ACTION);
                responseIntent.putExtra(EncodingUtils.END_TIME_KEY, endTime);
                sendBroadcast(responseIntent);
            }
            else if(intentAction.equals(EncodingUtils.CALCULATE_BIT_ERROR_RATE))
            {
                String receivedMessage = alphaEncoder.decodeMessage(intent);

                Intent responseIntent = new Intent();
                responseIntent.setAction(EncodingUtils.SEND_BIT_ERRORS_ACTION);
                responseIntent.putExtra(EncodingUtils.END_TIME_KEY, endTime);
                responseIntent.putExtra(EncodingUtils.RECEIVED_MESSAGE_KEY, receivedMessage);

                sendBroadcast(responseIntent);
            }
            else if(intentAction.equals(EncodingUtils.ALPHA_ENCODING_ACTION)) {
                decodeAndStore(intent, alphaEncoder);
            } else { // Must be a bitstring-encoded Intent
                decodeAndStore(intent, bitstringEncoder);
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
    	if(matchesFilter(intent)) {
    		decodeAndStore(intent, bitstringEncoder);
    	}
    	
        return mBinder;
    }

    private boolean matchesFilter(Intent intent)
    {
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
	
	private void decodeAndStore(Intent messageIntent, EncodingScheme encoder) {
		encoder.decodeMessage(messageIntent);

        // TODO: Finish implementing
        List<String> orderedMessageKeys = encoder.getOrderedMessageKeys();
        Map<String, String> actionToMessageMap = encoder.getActionToMessageMap();

        //String message = encoder.getMessage(); // TODO: Incorporate logic for allowing all bitstring message segments to arrive first (i.e. wait for the message to be complete)

        // TODO: iterate over the actionToMessageMap in key order and append the bitstring message segments; then reconstruct the original message string
        // from byte[]

        SharedPreferences.Editor messageStoreEditor = messageStore.edit();
    	messageStoreEditor.putString(keySequence.next(), message);
    	messageStoreEditor.commit();
	}

	public class MessageBinder extends Binder {
		public MessageReceiver getService()
		{
			return MessageReceiver.this;
		}
	}
}
