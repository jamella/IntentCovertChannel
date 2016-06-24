package intent.covertchannel.intentencoderdecoder;

import android.util.Log;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;

public class SegmentMap {

    // TODO: Cleanup
    //private static final String TAG = "intent.covertchannel.intentencoderdecoder.SegmentMap";
    private static final String TAG = EncodingUtils.TRACE_TAG;

    private Collection<String> actionStrings;
    private int numUniqueValues;
    private int maxVal;
    private int minVal;
    private int maxFragmentLength;
    private int minFragmentLength;
    private List<Segment> segments;
    private KeyGenerator keyGenerator;
    private Map<String, String> fragmentKeyMap;

    public SegmentMap(Collection<String> actionStrings, int numUniqueValues) {
        this.actionStrings = actionStrings;
        this.numUniqueValues = numUniqueValues;

        maxVal = numUniqueValues - 1;
        minVal = 0;
        maxFragmentLength = BitstringEncoder.calculateMaxFragmentBitLength(maxVal);
        minFragmentLength = BitstringEncoder.calculateMinFragmentBitLength(maxVal);

        segments = initializeSegments(actionStrings, numUniqueValues);
        keyGenerator = new AlphabeticalKeySequence();
        fragmentKeyMap = new HashMap<String, String>();
    }

    public static List<Segment> initializeSegments(Collection<String> actionStrings, int numUniqueValues) {
        List<Segment> segments = new ArrayList<>();
        int valsPerSegment = numUniqueValues / actionStrings.size();

        int val = 0;

        Log.d(TAG, "Initializing segments: numActions = " + actionStrings.size() + ", numUniqueValues = " + numUniqueValues);

        Iterator<String> actionIter = actionStrings.iterator();
        while(val < numUniqueValues && actionIter.hasNext()) {
            int nextVal = val + valsPerSegment - 1;

            Log.d(TAG, "val = " + val + ", nextVal = " + nextVal);

            if(nextVal > numUniqueValues) {
                segments.add(new Segment(actionIter.next(), val, numUniqueValues));
            } else {
                segments.add(new Segment(actionIter.next(), val, nextVal));
            }

            val = nextVal + 1;
        }

        return segments;
    }

    private int validateAndConvertFragment(String fragment) {
        int fragmentVal = BitstringEncoder.bitstringToInt(fragment);
        if(fragmentVal > maxVal || fragmentVal < minVal) {
            throw new RuntimeException("Fragment \"" + fragment + "\" must be between the decimal values " + minVal + " and " + maxVal); // TODO: Make illegal-arg-exception
        }

        return fragmentVal;
    }

    public String putFragment(String fragment) {
        int fragmentVal = validateAndConvertFragment(fragment);

        for(Segment segment: this.segments) {
            if(segment.valueWithinLimits(fragmentVal)) {
                if(!segment.hasMetadataKey(Segment.SIGNIFICANT_BITS_IN_LAST_FRAGMENT_KEY)) {
                    String sigBitsKey = keyGenerator.next();
                    Log.d(TAG, "Setting sig-bit metadata key for \"" + segment.getAction() + "\" to \"" + sigBitsKey + "\"");
                    segment.setMetadataKey(sigBitsKey, Segment.SIGNIFICANT_BITS_IN_LAST_FRAGMENT_KEY);
                }

                String key = keyGenerator.next();

                // TODO: Remove the duplication between these
                segment.addFragment(key, fragment);
                fragmentKeyMap.put(key, fragment);

                // TODO: Test this logic
                // Update the number of significant bits in the last fragment (i.e. the one that was just added)
                int sigBitsInFragment = fragment.length();

                Log.d(TAG, "Setting significant bits for \"" + segment.getAction() + "\" to " + sigBitsInFragment);

                //String sigBitsMetadataKey = segment.getMetadataKey(Segment.SIGNIFICANT_BITS_IN_LAST_FRAGMENT_KEY);
                String sigBitsMetadataFragmentBitstring = Integer.toBinaryString(sigBitsInFragment);
                segment.setMetadataValue(sigBitsMetadataFragmentBitstring, Segment.SIGNIFICANT_BITS_IN_LAST_FRAGMENT_KEY);
                //fragmentKeyMap.put(sigBitsMetadataKey, sigBitsMetadataFragmentBitstring);

                return key;
            }
        }

        return null; // This should never happen
    }

    public List<Segment> getSegments() {
        return segments;
    }

    public Map<String, String> getFragmentKeyMap() {
        // TODO: return a copy for better safety
        return fragmentKeyMap;
    }

    public String getActionForFragment(String fragment) {
        int fragmentVal = validateAndConvertFragment(fragment);
        for(Segment segment: segments) {
            if(segment.valueWithinLimits(fragmentVal)) {
                return segment.getAction();
            }
        }

        throw new RuntimeException("Failed to find action string for valid fragment value \"" + fragment + "\"; this should never happen");
    }
}
