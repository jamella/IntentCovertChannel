package intent.covertchannel.intentencoderdecoder;

import android.content.Intent;
import android.os.Bundle;

import java.util.HashMap;
import java.util.Map;

/**
 * An Action-based segment of a message.
 */
public class Segment {
    private String action;
    private int minVal;
    private int maxVal;
    private Map<String, String> bitstringsByMessageKey;

    public Segment(String action, int minVal, int maxVal) {
        this.action = action;
        this.minVal = minVal;
        this.maxVal = maxVal;

        this.bitstringsByMessageKey = new HashMap<String, String>();
    }

    public String getAction() {
        return this.action;
    }

    public int getMinVal() {
        return this.minVal;
    }

    public int getMaxVal() {
        return this.maxVal;
    }

    public boolean valueWithinLimits(int fragmentVal) {
        return (fragmentVal >= minVal) && (fragmentVal <= maxVal);
    }

    public void addFragment(String key, String fragmentBits) {
        bitstringsByMessageKey.put(key, fragmentBits);
    }

    public Map<String, String> getFragmentMessageKeyMap() {
        // TODO: Return copy for better safety
        return bitstringsByMessageKey;
    }

    public Intent asEncodedIntent() {
        Intent encodedIntent = new Intent();
        encodedIntent.setType("plain/text");
        encodedIntent.setAction(action);

        Bundle msgBundle = new Bundle();
        for(String msgKey: bitstringsByMessageKey.keySet()) {
            // TODO: Encode each value string (confirm that these are bitstrings) into the bundle in an expansion-code and action-band value set offset aware way
            // (probably a static function in BistringEncoder)
        }

        encodedIntent.putExtras(msgBundle);
        return encodedIntent;
    }
}
