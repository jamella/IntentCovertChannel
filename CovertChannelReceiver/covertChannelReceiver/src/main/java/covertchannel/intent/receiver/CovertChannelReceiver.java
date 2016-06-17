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
	
	private String message;
	
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        receivedMessageLabel = (TextView) findViewById(R.id.received_message_label);
        receivedMessageField = (TextView) findViewById(R.id.received_message);
        
        messageStore = getSharedPreferences(MessageReceiver.MESSAGE_STORE_KEY, MODE_PRIVATE);
        message = "";
    }
    
    @Override
    public void onStart()
    {
    	super.onStart();
    	
    	// TODO: Implement intent filtering (ignore LAUNCHER/MAIN, etc.)
    	// inside the MessageReceiver service
    	
    	SharedPreferences.Editor messageStoreEditor = messageStore.edit();
    	
    	Map<String, ?> messages = messageStore.getAll();
    	for(String key: messages.keySet())
    	{
    		message += messages.get(key) + " ";
    		
    		// Removes the message from the store
    		messageStoreEditor.remove(key);
    	}
    	
    	// Commit changes (i.e. remove all the stored messages)
    	messageStoreEditor.commit();
    	
    	if(message != null && message.length() != 0)
    	{
    		updateDisplay(message);
    	}
    	else
    	{
    		updateDisplay(NO_MESSAGE_ERROR);
    	}
    }
    
    @Override
    public void onResume()
    {
    	super.onResume();
    }

    /**
     * Updates the user display with the current message which has
     * been received so far.
     */
	private void updateDisplay(String messageString)
	{
		if(messageString.equals(NO_MESSAGE_ERROR))
		{
			receivedMessageLabel.setText("");
		}
		else
		{
			receivedMessageLabel.setText("Received " + message.getBytes().length + " bytes:\n");
		}
		
		receivedMessageField.setText(messageString);
	}
}