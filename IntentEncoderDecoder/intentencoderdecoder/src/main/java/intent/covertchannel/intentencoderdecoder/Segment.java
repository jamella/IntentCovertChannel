package intent.covertchannel.intentencoderdecoder;

import android.content.Intent;
import android.os.Bundle;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * An Action-based segment of a message.
 */
public class Segment {
    private static final int KEY_INDEX = 0;
    private static final int VALUE_INDEX = 1;

    // Metadata field indices
    public static final int SIGNIFICANT_BITS_IN_LAST_FRAGMENT_KEY = 0;

    private String action;
    private int minVal;
    private int maxVal;
    private String sigBitsInLastFragmentKey;
    private int sigBitsInLastFragment;
    private Map<String, String> bitstringsByMessageKey;

    private static final String[][] metadataKeys = {null, null, null, null};

    public Segment(String action, int minVal, int maxVal) {
        this.action = action;
        this.minVal = minVal;
        this.maxVal = maxVal;

        bitstringsByMessageKey = new HashMap<String, String>();
        sigBitsInLastFragmentKey = null;
        sigBitsInLastFragment = 0;
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

    public boolean hasMetadataKey(int keyIndex) {
        return metadataKeys[keyIndex] != null;
    }

    public void setMetadataKey(String key, int keyIndex) {
        if(keyIndex < 0 || keyIndex >= metadataKeys.length) {
            throw new IndexOutOfBoundsException();
        }

        String[] keyAndValueArray = {key, null};
        metadataKeys[keyIndex] = keyAndValueArray;
    }

    public String getMetadataKey(int keyIndex) {
        return metadataKeys[keyIndex][KEY_INDEX];
    }

    public void setMetadataValue(String value, int keyIndex) {
        // TODO: Implement this in a less klunky way
        metadataKeys[keyIndex][VALUE_INDEX] = value;
        bitstringsByMessageKey.put(metadataKeys[keyIndex][VALUE_INDEX], value);
    }
}
