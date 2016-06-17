package intent.covertchannel.intentcovertchannelanalysis;

import java.io.IOException;
import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import intent.covertchannel.intentencoderdecoder.EncodingUtils;

public class ChannelStatsActivity extends Activity
{
	// TODO: Make this configurable
	private static final int BUILD_VERSION = 8;
	
	// Symbollic representations of the different tests which can be performed
	private enum TEST {MESSAGE, THROUGHPUT, BIT_ERROR};
	
	private String tag;
	private TextView statsDisplay;
	private Button testIntentSizeButton, sendMessageButton, 
		testThroughputButton, calcBitErrorButton;
	private StringBuilder stats;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.stats_screen);
        
        tag = getClass().getName();
        
        statsDisplay = (TextView) findViewById(R.id.stats_display);
        
        testIntentSizeButton = (Button) findViewById(R.id.test_intent_size_button);
        testIntentSizeButton.setOnClickListener(new View.OnClickListener()
        {
			@Override
			public void onClick(View view)
			{
				Intent tranistionIntent = new Intent(ChannelStatsActivity.this, IntentSizeTest.class);
				startActivity(tranistionIntent);
			}
        });
        
        sendMessageButton = (Button) findViewById(R.id.send_message_button);
        sendMessageButton.setOnClickListener(new View.OnClickListener()
        {
        	@Override
        	public void onClick(View view)
        	{
        		showIntentTestDialog(false, TEST.MESSAGE);
        	}
        });
        
        testThroughputButton = (Button) findViewById(R.id.test_throughput_button);
        testThroughputButton.setOnClickListener(new View.OnClickListener()
        {
        	@Override
        	public void onClick(View view)
        	{
        		showIntentTestDialog(false, TEST.THROUGHPUT);
        	}
        });
        
        calcBitErrorButton = (Button) findViewById(R.id.calc_bit_error_button);
        calcBitErrorButton.setOnClickListener(new View.OnClickListener()
        {
        	@Override
        	public void onClick(View view)
        	{
        		showIntentTestDialog(false, TEST.BIT_ERROR);
        	}
        });
        
        stats = new StringBuilder();
        initStats();
        statsDisplay.setText(stats.toString());
    }

	private void initStats()
	{
		int numValues = EncodingUtils.getCharacterSetSize(BUILD_VERSION);
		int totalBytes = 0;
		int size;
		
		try
		{
			size = Memory.sizeOf(true);
			totalBytes += size;
			
			stats.append("boolean: ");
			stats.append(size);
			stats.append("\n");

			size = Memory.sizeOf(new boolean[0]);
			totalBytes += size;
			
			stats.append("boolean array: ");
			stats.append(size);
			stats.append("\n");

			size = Memory.sizeOf(new Byte((byte) 1));
			totalBytes += size;
			
			stats.append("byte: ");
			stats.append(size);
			stats.append("\n");

			size = Memory.sizeOf(new byte[0]);
			totalBytes += size;
			
			stats.append("byte array: ");
			stats.append(size);
			stats.append("\n");

			size = Memory.sizeOf((char) 1);
			totalBytes += size;
			
			stats.append("char: ");
			stats.append(size);
			stats.append("\n");

			size = Memory.sizeOf(new char[0]);
			totalBytes += size;
			
			stats.append("char array: ");
			stats.append(size);
			stats.append("\n");

			size = Memory.sizeOf((CharSequence) "1");
			totalBytes += size;
			
			stats.append("CharSequence: ");
			stats.append(size);
			stats.append("\n");

			size = Memory.sizeOf(new CharSequence[0]);
			totalBytes += size;
			
			stats.append("CharSequence array: ");
			stats.append(size);
			stats.append("\n");

			size = Memory.sizeOf("1");
			totalBytes += size;
			
			stats.append("String: ");
			stats.append(size);
			stats.append("\n");

			size = Memory.sizeOf(new ArrayList<CharSequence>());
			totalBytes += size;
			
			stats.append("CharSequenceArrayList: ");
			stats.append(size);
			stats.append("\n");

			size = Memory.sizeOf(new ArrayList<Integer>());
			totalBytes += size;
			
			stats.append("IntegerArrayList: ");
			stats.append(size);
			stats.append("\n");

			size = Memory.sizeOf(new ArrayList<Parcelable>());
			totalBytes += size;
			
			stats.append("ParcelableArrayList: ");
			stats.append(size);
			stats.append("\n");

			size = Memory.sizeOf((double) 1.0);
			totalBytes += size;
			
			stats.append("double: ");
			stats.append(size);
			stats.append("\n");

			size = Memory.sizeOf(new double[0]);
			totalBytes += size;
			
			stats.append("double array: ");
			stats.append(size);
			stats.append("\n");

			size = Memory.sizeOf((float) 1.0);
			totalBytes += size;
			
			stats.append("float: ");
			stats.append(size);
			stats.append("\n");

			Memory.sizeOf(new float[0]);
			totalBytes += size;
			
			stats.append("float array: ");
			stats.append(size);
			stats.append("\n");

			size = Memory.sizeOf(1);
			totalBytes += size;
			
			stats.append("int: ");
			stats.append(size);
			stats.append("\n");

			size = Memory.sizeOf(new int[0]);
			totalBytes += size;
			
			stats.append("int array: ");
			stats.append(size);
			stats.append("\n");

			size = Memory.sizeOf(1L);
			totalBytes += size;
			
			stats.append("long: ");
			stats.append(size);
			stats.append("\n");

			size = Memory.sizeOf(new long[0]);
			totalBytes += size;
			
			stats.append("long array: ");
			stats.append(size);
			stats.append("\n");

			size = Memory.sizeOf(new Parcelable[0]);
			totalBytes += size;
			
			stats.append("Parcelable array: ");
			stats.append(size);
			stats.append("\n");

			size = Memory.sizeOf(new Short((short) 1));
			totalBytes += size;
			
			stats.append("short: ");
			stats.append(size);
			stats.append("\n");

			size = Memory.sizeOf(new short[0]);
			totalBytes += size;
			
			stats.append("short array: ");
			stats.append(size);
			stats.append("\n");

			size = Memory.sizeOf(new SparseArray<Parcelable>());
			totalBytes += size;
			
			// TODO: Figure out why this is throwing an IOException
			stats.append("Sparse Parcelable Array: ");
			stats.append(size);
		}
		catch (IOException e)
		{
			Log.w(tag, "IOException encountered in " + tag + ".initStats(): " +
				e.getMessage());
		}
		
		int averageSize = totalBytes / numValues;

		// New lines for readability
		stats.append("\n");
		stats.append("\n");
		stats.append("Average Size: ");
		stats.append(averageSize);
	}
	
	private void showIntentTestDialog(boolean error, final TEST testType)
	{
		// Code in this method is based on a snippet from
		// http://www.androidsnippets.com/prompt-user-input-with-an-alertdialog
		AlertDialog.Builder alert = new AlertDialog.Builder(this);

		alert.setTitle("Intent Test");
		
		if(error)
		{
			alert.setMessage("Invalid input. Please enter a positive, " + 
				"non-zero integer value.");
		}
		else
		{
			alert.setMessage("How many Intents do you want to send?");
		}

		// Set an EditText view to get user input 
		final EditText input = new EditText(this);
		alert.setView(input);

		alert.setPositiveButton("Ok", new DialogInterface.OnClickListener()
		{
			public void onClick(DialogInterface dialog, int whichButton)
			{
				String numIntentString = input.getText().toString();
				
				try
				{
					int numIntents = Integer.parseInt(numIntentString);
					showBytesToSendDialog(false, numIntents, testType);
				}
				catch(NumberFormatException e)
				{
					showIntentTestDialog(true, testType);
				}
			}
		});

		alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener()
		{
		  public void onClick(DialogInterface dialog, int whichButton) 
		  {
			  // Canceled.
		  }
		});

		alert.show();
	}
	
	private void showBytesToSendDialog(boolean error, final int numIntents, 
		final TEST testType)
	{
		// Code in this method is based on a snippet from
		// http://www.androidsnippets.com/prompt-user-input-with-an-alertdialog
		AlertDialog.Builder alert = new AlertDialog.Builder(this);

		alert.setTitle("Intent Test");
		
		if(error)
		{
			alert.setMessage("Invalid input. Please enter a positive, " + 
				"non-zero integer value.");
		}
		else
		{
			alert.setMessage("How many characters do you want to send in each Intent?");
		}

		// Set an EditText view to get user input 
		final EditText input = new EditText(this);
		alert.setView(input);

		alert.setPositiveButton("Ok", new DialogInterface.OnClickListener()
		{
			public void onClick(DialogInterface dialog, int whichButton)
			{
				String numBytesString = input.getText().toString();
				
				try
				{
					int numBytes = Integer.parseInt(numBytesString);
					
					Intent tranistionIntent = new Intent(ChannelStatsActivity.this, IntentThroughputTest.class);
					
					switch(testType)
					{
					case THROUGHPUT:
						tranistionIntent.setAction(EncodingUtils.CALCULATE_THROUGHPUT_ACTION);
						tranistionIntent.putExtra(IntentThroughputTest.MESSAGE_BYTES_TO_SEND, numBytes);
						tranistionIntent.putExtra(IntentThroughputTest.NUM_INTENTS, numIntents);
						break;
					case BIT_ERROR:
						tranistionIntent.setAction(EncodingUtils.CALCULATE_BIT_ERROR_RATE);
						tranistionIntent.putExtra(IntentThroughputTest.MESSAGE_BYTES_TO_SEND, numBytes);
						tranistionIntent.putExtra(IntentThroughputTest.NUM_INTENTS, numIntents);
						break;
					case MESSAGE:
						tranistionIntent.setAction(EncodingUtils.RECEIVER_COVERT_MESSAGE_ACTION);
						tranistionIntent.putExtra(IntentThroughputTest.MESSAGE_BYTES_TO_SEND, numBytes);
						tranistionIntent.putExtra(IntentThroughputTest.NUM_INTENTS, numIntents);
						break;
					default:
						Log.w(tag, "In showBytesToSendDialog(): Unknown " + 
							"test type \"" + testType + "\" encountered");
					}
					startActivity(tranistionIntent);
				}
				catch(NumberFormatException e)
				{
					showBytesToSendDialog(true, numIntents, testType);
				}
			}
		});

		alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener()
		{
		  public void onClick(DialogInterface dialog, int whichButton) 
		  {
			  // Canceled.
		  }
		});

		alert.show();
	
	}
}