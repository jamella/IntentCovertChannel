package intent.covertchannel.intentencoderdecoder;

import android.content.Intent;
import android.os.Bundle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * An Action-based segment of a message.
 */
public class Segment {

    public static class MetadataEntry {
        private String key;
        private String valueAsBitstring;

        public MetadataEntry(String key, String valueAsBitstring) {
            this.key = key;
            this.valueAsBitstring = valueAsBitstring;
        }

        public MetadataEntry() {
            this.key = null;
            this.valueAsBitstring = null;
        }

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public String getValueAsBitstring() {
            return valueAsBitstring;
        }

        public void setValueAsBitstring(String valueAsBitstring) {
            this.valueAsBitstring = valueAsBitstring;
        }
    }

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

    private List<MetadataEntry> metadataEntries;

    public Segment(String action, int minVal, int maxVal) {
        this.action = action;
        this.minVal = minVal;
        this.maxVal = maxVal;

        bitstringsByMessageKey = new HashMap<String, String>();
        sigBitsInLastFragmentKey = null;
        sigBitsInLastFragment = 0;

        metadataEntries = new ArrayList<MetadataEntry>();
        metadataEntries.add(new MetadataEntry());
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
        Map<String, String> fragmentMessageKeyMap  = new HashMap(bitstringsByMessageKey);

        for(MetadataEntry metadataEntry: metadataEntries) {
            fragmentMessageKeyMap.put(metadataEntry.getKey(), metadataEntry.getValueAsBitstring());
        }

        return fragmentMessageKeyMap;
    }

    public boolean hasMetadataKey(int keyIndex) {
        return metadataEntries.get(keyIndex).getKey() != null;
    }

    public void setMetadataKey(String key, int keyIndex) {
        metadataEntries.get(keyIndex).setKey(key);
    }

    public String getMetadataKey(int keyIndex) {
        return metadataEntries.get(keyIndex).getKey();
    }

    public Set<String> getMetadataKeys() {
        Set<String> metadataKeys = new HashSet();
        for(MetadataEntry entry: metadataEntries) {
            metadataKeys.add(entry.getKey());
        }

        return metadataKeys;
    }

    public void setMetadataValue(String value, int keyIndex) {
        metadataEntries.get(keyIndex).setValueAsBitstring(value);
    }

    /**
     * @return The number of data entries in this Segment.
     */
    public int length() {
        int length = 0;
        for(Map.Entry<String, String> entry: bitstringsByMessageKey.entrySet()) {
            if(entry.getKey() != null && entry.getValue() != null) {
                length++;
            }
        }

        return length;
    }

    public boolean isEmpty() {
        return length() == 0;
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Segment[action = " + action + "]\n");

        Map<String, String> fragmentMessageKeyMap = this.getFragmentMessageKeyMap();
        for(Map.Entry<String, String> entry: fragmentMessageKeyMap.entrySet()) {
            stringBuilder.append(entry.getKey() + " => " + entry.getValue() + "\n");
        }

        return stringBuilder.toString();
    }
}
