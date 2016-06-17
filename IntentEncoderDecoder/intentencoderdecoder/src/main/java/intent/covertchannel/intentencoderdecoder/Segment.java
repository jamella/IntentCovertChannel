package intent.covertchannel.intentencoderdecoder;

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
}
