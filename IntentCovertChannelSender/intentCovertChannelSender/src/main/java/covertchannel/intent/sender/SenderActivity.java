package covertchannel.intent.sender;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import intent.covertchannel.intentencoderdecoder.BitstringEncoder;
import intent.covertchannel.intentencoderdecoder.EncodingScheme;
import intent.covertchannel.intentencoderdecoder.EncodingUtils;
import intent.covertchannel.intentencoderdecoder.LowerCaseAlphaEncoder;

// TODO: Look into using InputFilters with the message entry EditText to limit
// the character values which can be typed to those supported by the current
// scheme
public class SenderActivity extends Activity {
    private static final String TAG = "covertchannel.intent.sender.SenderActivity";

    private EditText messsageEntry;
    private Button sendAlphaButton;
    private Button sendAlphaOrigInterceptibleButton;
    private Button sendBitstringButton;
    private Button sendBitstringInterceptibleButton;
    private Button clearMessageStoreButton;

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
                        // TODO: Check the input validity
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
                        // TODO: Figure out this works with the enhanced method (I think it only kind-of does)
                        // TODO: Check the input validity
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
