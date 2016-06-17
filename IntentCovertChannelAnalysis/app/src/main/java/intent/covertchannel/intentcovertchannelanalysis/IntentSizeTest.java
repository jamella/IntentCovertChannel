package intent.covertchannel.intentcovertchannelanalysis;

import java.io.IOException;
import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import intent.covertchannel.intentencoderdecoder.AlphabeticalKeySequence;

/**
 * Used to find the maximum supported {@link Intent} size for a device.
 * 
 * @author Timothy Heard
 */
public class IntentSizeTest extends Activity
{
	private AlphabeticalKeySequence keySequence;
	private TextView testOutput;
	
	private int loopCounter;
	
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.size_test);
        
        ViewDrawListener drawListener = new ViewDrawListener(this);
        LinearLayout parentLayout = (LinearLayout) findViewById(R.id.parent_layout);
        parentLayout.addView(drawListener);
        
        testOutput = (TextView) findViewById(R.id.size_test_output);
        keySequence = new AlphabeticalKeySequence();
        loopCounter = 1;
    }
    
    @Override
    public void onResume()
    {
    	super.onResume();
    }
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
    	intentLoop();
    }
    
    /**
     * Loops until a crash occurs in order to find what the largest supported 
     * Intent and Bundle size is
     */
	private void intentLoop()
	{
    	Intent testIntent = new Intent(this, DummyClass.class);
    	
    	ArrayList<Character> fillerData = new ArrayList<Character>();
    	
    	// Add 5 KB more data to the Intent every iteration through the 
    	// while loop 
    	for(int j = 0; j < 1000 * loopCounter; j++)
    	{
    		fillerData.add('a');
    	}

    	testIntent.putExtra(keySequence.next(), fillerData);
    	
    	int dataSize = -1, intentSize = -1;
    	try
		{
    		dataSize = Memory.sizeOf(fillerData);
    		intentSize = getParcelableSize(testIntent);
		}
    	catch (IOException e1)
		{
			Log.e(this.getClass().getName(), "Unable to determine size " + 
				"of the data: " + e1.getMessage());
		}
    	
    	testOutput.append("Attempting to send an Intent of size " +
			intentSize + " bytes containing " + loopCounter + 
			" extra(s) of size " + dataSize + " bytes\n");
    	startActivityForResult(testIntent, 1);
    	loopCounter++;
	}
	
	public static int getParcelableSize(Parcelable p)
	{
		Parcel parcel = Parcel.obtain();
		p.writeToParcel(parcel, 0);
		return parcel.dataSize();
	}
	
	private class ViewDrawListener extends View
	{
		public ViewDrawListener(Context context)
		{
			super(context);
		}
		
		@Override
		protected void onDraw(Canvas c)
		{
			intentLoop();
		}
	}
}
