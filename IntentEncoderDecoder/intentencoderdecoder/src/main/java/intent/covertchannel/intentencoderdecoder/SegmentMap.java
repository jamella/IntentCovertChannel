package intent.covertchannel.intentencoderdecoder;

import android.util.Log;

import java.util.Iterator;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;

public class SegmentMap {

    // TODO: Remove
    //private static final String BASE_ACTION_NAME = "data-";

    private static final String TAG = "intent.covertchannel.intentencoderdecoder.SegmentMap";

    // TODO: Cleanup
    //private int numActions;
    private Set<String> actionStrings;
    private int numUniqueValues;
    private int maxVal;
    private int minVal;
    private int maxFragmentLength;
    private int minFragmentLength;
    private Set<Segment> segments;
    private KeyGenerator keyGenerator;
    private Map<String, String> fragmentKeyMap;

    public SegmentMap(Set<String> actionStrings, int numUniqueValues) {
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

    private static Set<Segment> initializeSegments(Set<String> actionStrings, int numUniqueValues) {
        Set<Segment> segments = new HashSet<Segment>();
        int valsPerSegment = numUniqueValues / actionStrings.size();

        int val = 0;
        int i = 0;

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

            i++;
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
                String key = keyGenerator.next();
                segment.addFragment(key, fragment);
                fragmentKeyMap.put(key, fragment);
                return key;
            }
        }

        return null; // This should never happen
    }

    public Set<Segment> getSegments() {
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
