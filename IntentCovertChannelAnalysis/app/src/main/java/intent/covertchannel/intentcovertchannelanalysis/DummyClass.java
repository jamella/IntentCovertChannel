/**
 * 
 */
package intent.covertchannel.intentcovertchannelanalysis;

import android.app.Activity;

/**
 * Activity which immediately returns control when started for a result.
 * 
 * @author Timothy Heard
 */
public class DummyClass extends Activity
{
	@Override
	public void onResume()
	{
		super.onResume();
		setResult(RESULT_OK,null);     
		finish();
	}
}
