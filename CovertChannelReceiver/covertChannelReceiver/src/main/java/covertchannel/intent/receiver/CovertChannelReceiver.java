package covertchannel.intent.receiver;

import java.util.Map;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.TextView;

public class CovertChannelReceiver extends Activity 
{
	// Message displayed when an Intent was received but no message could be decoded
	// from it
	private static final String NO_MESSAGE_ERROR = "No message found";

	private TextView receivedMessageLabel, receivedMessageField;

	// Used to retrieve messages received and persisted by the MessageReceiver 
	// service
    private SharedPreferences messageStore;
	
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        receivedMessageLabel = (TextView) findViewById(R.id.received_message_label);
        receivedMessageField = (TextView) findViewById(R.id.received_message);
        
        messageStore = getSharedPreferences(MessageReceiver.MESSAGE_STORE_KEY, MODE_PRIVATE);
    }
    
    @Override
    public void onStart()
    {
    	super.onStart();
    	SharedPreferences.Editor messageStoreEditor = messageStore.edit();

        String alphaEncodedMessage = messageStore.getString(MessageReceiver.ALPHA_ENCODED_MESSAGE_STORAGE_KEY, "");
        messageStoreEditor.remove(MessageReceiver.ALPHA_ENCODED_MESSAGE_STORAGE_KEY);

        String bitstringEncodedMessage = messageStore.getString(MessageReceiver.BITSTRING_ENCODED_MESSAGE_STORAGE_KEY, "");
        messageStoreEditor.remove(MessageReceiver.BITSTRING_ENCODED_MESSAGE_STORAGE_KEY);

    	// Commit changes (i.e. remove all the stored messages)
    	messageStoreEditor.commit();

    	updateDisplay(alphaEncodedMessage, bitstringEncodedMessage);
    }
    
    @Override
    public void onResume() {
    	super.onResume();
    }

    /**
     * Updates the user display with the current message which has
     * been received so far.
     */
	private void updateDisplay(String alphaEncodedMessage, String bitstringEncodedMessage) {
        String labelText = "";
        String messageString = NO_MESSAGE_ERROR;
		if(alphaEncodedMessage.length() > 0) {
            labelText = "Alpha-encoding: Received " + alphaEncodedMessage.getBytes().length + " bytes\n";
            messageString = "Alpha-encoded message:\n\n" + alphaEncodedMessage;
		}

        if(bitstringEncodedMessage.length() > 0) {
            labelText += "Bitstring-encoding: Received " + bitstringEncodedMessage.getBytes().length + " bytes\n";
            messageString = "Bitstring-encoded message:\n\n" + bitstringEncodedMessage;
		}

        receivedMessageLabel.setText(labelText);
		receivedMessageField.setText(messageString);
	}
}