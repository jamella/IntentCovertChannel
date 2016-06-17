/**
 * 
 */
package intent.covertchannel.intentcovertchannelanalysis;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import intent.covertchannel.intentencoderdecoder.EncodingUtils;
import intent.covertchannel.intentencoderdecoder.EncodingScheme;
import intent.covertchannel.intentencoderdecoder.LowerCaseAlphaEncoder;

/**
 * Tests the throughput of the Intent/Bundle based covert channel.
 * 
 * @author Timothy Heard
 */
public class IntentThroughputTest extends Activity
{
	public static final String MESSAGE_BYTES_TO_SEND = "max_bundle_size"; 
	public static final String NUM_INTENTS = "num_intents";
	
	// TODO: Make this configurable
	private static final int BUILD_VERSION = 8;

	private TextView testOutput;
	
	private EncodingScheme schema;
	private BroadcastReceiver endTimeReceiver, bitErrorReceiver;
	
	private String testAction, messageToSend;
	private double startTime, endTime, transStartTime, transEndTime, totalTime,
		totalBytesSent;
	private int numIntents, numSent;
	private int charsToSend;
	
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.size_test);
        testOutput = (TextView) findViewById(R.id.size_test_output);
        schema = new LowerCaseAlphaEncoder();
    	
    	Intent launcherIntent = getIntent();
    	
    	testAction = launcherIntent.getAction();
    	numIntents = launcherIntent.getIntExtra(NUM_INTENTS, -1);
    	charsToSend = launcherIntent.getIntExtra(MESSAGE_BYTES_TO_SEND, -1);
    	numSent = 0;
    	totalTime = 0;
    	totalBytesSent = 0;
    	transStartTime = 0;
    	transEndTime = 0;

    	if(charsToSend > 0 && numIntents > 0 && testAction != null)
    	{
    		displayOutputHeader();
    		
    		if(testAction.equals(EncodingUtils.CALCULATE_THROUGHPUT_ACTION))
    		{
    			messageToSend = readMessage(charsToSend);
    			endTimeReceiver = new EndTimeReceiver();
    		
    			IntentFilter filter = new IntentFilter();
    			filter.addAction(EncodingUtils.SEND_TIME_ACTION);
    			registerReceiver(endTimeReceiver, filter);
    		
    			transStartTime = new Date().getTime();
    			broadcastMessageIntent(EncodingUtils.CALCULATE_THROUGHPUT_ACTION, messageToSend);
    		}
    		else if(testAction.equals(EncodingUtils.CALCULATE_BIT_ERROR_RATE))
    		{
    			messageToSend = readMessage(charsToSend);
    			bitErrorReceiver = new BitErrorRateReceiver();
    		
    			IntentFilter filter = new IntentFilter();
    			filter.addAction(EncodingUtils.SEND_BIT_ERRORS_ACTION);
    			registerReceiver(bitErrorReceiver, filter);
    			
    			transStartTime = new Date().getTime();
    			broadcastMessageIntent(EncodingUtils.CALCULATE_BIT_ERROR_RATE, messageToSend);
    		}
    		else if(testAction.equals(EncodingUtils.RECEIVER_COVERT_MESSAGE_ACTION))
    		{
    			messageToSend = readMessage(charsToSend);
    			broadcastMessageIntent(EncodingUtils.RECEIVER_COVERT_MESSAGE_ACTION, messageToSend);
    		}
    		else
    		{
    			failInit();
    		}
    	}
    	else
    	{
    		failInit();
    	}
    }

	@Override
    public void onResume()
    {
    	super.onResume();
    }
    
    @Override
    public void onDestroy()
    {
    	super.onDestroy();
    	
    	if(endTimeReceiver != null)
    	{
    		unregisterReceiver(endTimeReceiver);
    	}
    	
    	if(bitErrorReceiver != null)
    	{
    		unregisterReceiver(bitErrorReceiver);
    	}
    }
   
	private void failInit()
    {
		Toast.makeText(this, "Did not receive a valid number of characters " +
			"to send count (" + charsToSend + "), number of intents (" + 
			numIntents + "), and/or Intent action (" + testAction + ")",
			Toast.LENGTH_LONG).show();
		
		endTimeReceiver = null;
		bitErrorReceiver = null;
		finish();
    }
    
    private void broadcastMessageIntent(String action, String message)
    {
        // TODO: Allow this to use more expansion codes
        int numExpansionCodes = 1;
        Set<String> actionStrings = new HashSet<String>();
        actionStrings.add(action);
		Intent intent = schema.encodeMessage(message, EncodingScheme.NUM_BASE_VALUES, numExpansionCodes, actionStrings).iterator().next();

        // TODO: Cleanup
		//intent.setAction(action);
		
		startTime = new Date().getTime();
		startService(intent);
    }

    private void displayOutputHeader()
    {
    	testOutput.append("Number of intents to send: " + numIntents + "\n");
    	testOutput.append("Number of characters to send in each intent: " +
    		charsToSend + "\n\n");
    }
    
    private void updateTimeElapsedReport(double timeElapsed, double bytesSent)
    {
    	testOutput.append("Intent " + numSent + ": It took " + timeElapsed + 
    		" milliseconds to send an intent, containing " + bytesSent +
    		" bytes of covert message data encoded in a Bundle (start time " +
    		"= " + startTime + ", end time = " + endTime + ")\n\n");
    }

	private void updateBitErrorReport(double timeElapsed, double bytesSent,
		double bitError, String receivedMessage, String originalMessage)
    {
		double bitErrorRate = (bitError / timeElapsed) * 1000;
		testOutput.append("Intent " + numSent + ": Bit error rate = " + 
			bitErrorRate + " bits per second. There was a total of " +
	    	bitError + " bit errors.\n\n");
    }

	private void displayFinalReport()
    {
		double transDuration = Math.abs(transStartTime - transEndTime); 
		
		// This average does not account for the time between when 
		// confirmation was received for the previous Intent and when the next
		// Intent was sent
		double avgIntentTransRate = (totalBytesSent / totalTime) * 1000;
		
		// This average simply looks at when the first Intent was sent and 
		// when the last Intent was received
		double overallTransRateAvg = (totalBytesSent / transDuration) * 1000;
		
		testOutput.append("\nIt took " + transDuration + " milliseconds to " +
			"send a total of " + totalBytesSent + " bytes.\n\n");
		testOutput.append("The average transmission rate (per intent) " + 
			"was:\n" + avgIntentTransRate + " bytes per second (" + 
			(avgIntentTransRate * 8) + " bits per second)\n");
		testOutput.append("Total send time: " + totalTime + " milliseconds\n\n");
		testOutput.append("The average transmission rate for the entire " +
			"transmission was:\n" + overallTransRateAvg + " bytes per second " +
			"(" + (overallTransRateAvg * 8) + " bits per second)\n\n");
    }
	
	private void displayFinalBitErrorReport(double bitError)
	{
		double avgIntentBitError = bitError / numIntents; 
		double bitErrorRate = bitError / totalTime;
		
		testOutput.append("Average Intent bit error rate: " + 
			avgIntentBitError + " bit errors per Intent\n");
		testOutput.append("\n\nAverage bit error rate (by time): " + 
			avgIntentBitError +	" bit errors per second (" + 
			(bitErrorRate / numIntents) + " bit errors per second for each " +
			"Intent on average)");
	}
    
	private String readMessage(int bytesToSend)
	{
    	return EncodingUtils.readRawTextFile(this, R.raw.taming_of_the_shrew, bytesToSend);
	}

    private int calcCharErrors(String receivedMessage, String originalMessage)
    {
    	int numErrors = 0;
    	for(int i = 0; i < originalMessage.length(); ++i)
    	{
    		if(i >= receivedMessage.length() || 
    		   originalMessage.charAt(i) != receivedMessage.charAt(i))
    		{
    			++numErrors;
    		}
    	}
    	
    	// If the received message is longer than the received message then 
    	// every extra character is counted as an error
    	if(receivedMessage.length() > originalMessage.length())
    	{
    		numErrors += receivedMessage.length() - originalMessage.length(); 
    	}
    	
    	return numErrors;
    }
	
    /**
     * Coverts the given number of characters into the corresponding number of
     * bytes based on how many character codes are supported by the given 
     * {@link EncodingScheme}.
     */
    private static double charsToBytes(int numChars, int buildVersion, EncodingScheme schema) {
	    return ((double) (schema.getEncodableCharactersCount(buildVersion) * numChars)) / 8;
    }
    
    private class EndTimeReceiver extends BroadcastReceiver
    {
    	public EndTimeReceiver()
    	{
    		// Empty default constructor
    	}
    	
	    @Override
	    public void onReceive(Context context, Intent intent) 
	    {	    	
	    	if(numSent >= numIntents)
	    	{
	    		return;
	    	}

	    	endTime = intent.getLongExtra(EncodingUtils.END_TIME_KEY, 0);

	    	double timeElapsed = Math.abs(endTime - startTime);
	    	double bytesSent = charsToBytes(charsToSend, BUILD_VERSION, schema);
	    	
	    	totalTime += timeElapsed;
	    	totalBytesSent += bytesSent;
	    	
	    	updateTimeElapsedReport(timeElapsed, bytesSent);
	    	
	    	// numSent is incremented before updateTimeElapsedReport() is 
	    	// called so that the correct number of Intents sent will be 
	    	// used when the display is updated 
	    	numSent++;
	    	
	    	if(numSent < numIntents)
	    	{
	    		broadcastMessageIntent(EncodingUtils.CALCULATE_THROUGHPUT_ACTION, messageToSend);
	    	}
	    	else if(numSent == numIntents)
	    	{
	    		transEndTime = new Date().getTime();
	    		displayFinalReport();
	    	}
	    }
	}
    
    private class BitErrorRateReceiver extends BroadcastReceiver
    {
    	public BitErrorRateReceiver()
    	{
    		// Empty default constructor
    	}
    	
    	@Override
    	public void onReceive(Context context, Intent intent) 
    	{
	    	if(numSent >= numIntents)
	    	{
	    		return;
	    	}
	    	
    		endTime = intent.getLongExtra(EncodingUtils.END_TIME_KEY, 0);
    		String receivedMessage = intent.getStringExtra(EncodingUtils.RECEIVED_MESSAGE_KEY);

			int charErrors = calcCharErrors(receivedMessage, messageToSend);
    		double bitErrors = charsToBytes(charErrors, BUILD_VERSION, schema) * 8;    		
    		double timeElapsed = Math.abs(endTime - startTime);
    		double bytesSent = charsToBytes(charsToSend, BUILD_VERSION, schema);
    		
    		// numSent is incremented before updateTimeElapsedReport() is 
    		// called so that the correct number of Intents sent will be 
    		// used when the display is updated 
    		numSent++;
    		
    		totalTime += timeElapsed;
    		totalBytesSent += bytesSent;
    		
    		updateBitErrorReport(timeElapsed, bytesSent, bitErrors, 
    			receivedMessage, messageToSend);
    		
    		if(numSent < numIntents)
    		{
    			broadcastMessageIntent(EncodingUtils.CALCULATE_BIT_ERROR_RATE, messageToSend);
    		}
    		else if(numSent == numIntents)
    		{
    			transEndTime = new Date().getTime();
    			displayFinalReport();
    			displayFinalBitErrorReport(bitErrors);
    		}
    	}
    }
}
