package covertchannel.intent.sender;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import intent.covertchannel.intentencoderdecoder.BitstringEncoder;
import intent.covertchannel.intentencoderdecoder.EncodingScheme;
import intent.covertchannel.intentencoderdecoder.EncodingUtils;

public class ThroughputEvaluationService extends Service {
    private final IBinder mBinder = new MessageBinder();

    private static final String TAG = EncodingUtils.TRACE_TAG;




    public void initiateTest(Intent intent) {
        // TODO: implement and use
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Figure out what else to do here
        //handleIntent(intent);
        return mBinder;
    }

    public class MessageBinder extends Binder {
        public ThroughputEvaluationService getService()
        {
            return ThroughputEvaluationService.this;
        }
    }


}
