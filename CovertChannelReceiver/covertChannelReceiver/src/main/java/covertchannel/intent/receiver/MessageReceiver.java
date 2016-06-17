package covertchannel.intent.receiver;

import java.util.Date;

import android.app.IntentService;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import intent.covertchannel.intentencoderdecoder.AlphabeticalKeySequence;
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
    private LowerCaseAlphaEncoder schema;
    
    // Generates an alphabetical sequence of keys for storing messages
    private AlphabeticalKeySequence keySequence;
    
	// This is the object that receives interactions from clients.
    private final IBinder mBinder = new MessageBinder();

    /* TODO: Remove
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "Receiver service onReceive(): action = " + intent.getAction());
            handleIntent(intent);
        }
    };
    */

    @Override
    public void onCreate()
	{
		super.onCreate();

        Log.d(TAG, "Receiver service starting");

		messageStore = getSharedPreferences(MESSAGE_STORE_KEY, MODE_PRIVATE);
		schema = new LowerCaseAlphaEncoder();
		keySequence = new AlphabeticalKeySequence();

        /* TODO: Remove
        IntentFilter filter = new IntentFilter();
        filter.addAction(EncodingUtils.RECEIVER_COVERT_MESSAGE_ACTION);
        filter.addAction(EncodingUtils.CALCULATE_THROUGHPUT_ACTION);
        filter.addAction(EncodingUtils.CALCULATE_BIT_ERROR_RATE);

        // TODO: Add the remaining actions
        //filter.addAction(EncodingUtils.);

        registerReceiver(receiver, filter);
        */
	}

    private void handleIntent(Intent intent) {
        if(matchesFilter(intent)) {
            Log.d(TAG, "Intent filter matched");

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
                String receivedMessage = schema.decode(intent, BUILD_VERSION);

                Intent responseIntent = new Intent();
                responseIntent.setAction(EncodingUtils.SEND_BIT_ERRORS_ACTION);
                responseIntent.putExtra(EncodingUtils.END_TIME_KEY, endTime);
                responseIntent.putExtra(EncodingUtils.RECEIVED_MESSAGE_KEY, receivedMessage);

                sendBroadcast(responseIntent);
            }
            else
            {
                // Default action
                decodeAndStore(intent);
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
    		decodeAndStore(intent);
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
	    return (EncodingUtils.RECEIVER_COVERT_MESSAGE_ACTION.equals(intent.getAction()) ||
	    		EncodingUtils.CALCULATE_THROUGHPUT_ACTION.equals(intent.getAction()) ||
	    		EncodingUtils.CALCULATE_BIT_ERROR_RATE.equals(intent.getAction()));
    }
	
	private void decodeAndStore(Intent messageIntent) {
		String message = schema.decode(messageIntent, BUILD_VERSION);
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
