package intent.covertchannel.intentencoderdecoder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.util.SparseArray;

/**
 * Interface which defines a schema for encoding and decoding information
 * in an {@link Intent}.
 * 
 * @author Timothy Heard
 */
public abstract class EncodingScheme {

    // TODO: Find out if null values are allowed; if so, 22 values are possible
    public static final int NUM_BASE_VALUES = 21;

    // TODO: Change to a configurable value (or just remove)
    protected static final int MAX_MESSAGE_SIZE = Integer.MAX_VALUE;

    // TODO: Remove references to this; make it either a parameter or a system config value
    protected static final int BUILD_VERSION = 8;
    private static final String TAG = "intent.covertchannel.intentencoderdecoder.EncodingScheme";

    protected EncodingDictionary dictionary;

    // TODO: Use a more intelligent, less detectable key generation scheme?
    protected KeyGenerator keyGenerator;

    // Comparator for ordering the String keys in a Bundle.
    protected Comparator<String> keyComparator;

    public abstract Collection<Intent> encodeMessage(String message, int numBaseValues, int numExpansionCodes, Set<String> actionStrings);

    // TODO: Cleanup

    // TODO: Things to add:?
	//
	// 1. Max message size
	// 2. Max Intent size
	// 3. (Serializable) representation of a character set
	// 4. (Serializable) collection of character sets into a full encoding scheme, including expansion 
	//    code -> character set mappings
	// 5. Bundle key access ordering? => Pass in a String Comparator
	// 6. (Serializable) Bundle getter call ordering?
//    public List<Intent> encode(String messageString) {
//
//        Message message = buildMessage(messageString);
//        Collection<String> fragments = message.getFragments();
//        int numSegments = fragments.size() / MAX_MESSAGE_SIZE; // TODO: incorporate action-string bands here
//
//        // If MAX_MESSAGE_SIZE does not divide evenly into the number of
//        // characters contained in the message to encode then an additional
//        // segment is going to be required for encoding the remainder
//        if((fragments.size() % MAX_MESSAGE_SIZE) != 0) {
//            ++numSegments;
//        }
//
//        List<Intent> messageCarriers = new ArrayList<Intent>();
//        for(int i = 0; i < numSegments; i++) {
//            // Calculate the bounding indices for the current segment. Note
//            // that the start index is inclusive and the end index is
//            // exclusive. If this is the last segment then chars.length will be
//            // used as the end index; otherwise the start index of the next
//            // segment ((i + 1) * MAX_MESSAGE_SIZE) will be used.
//            int startIndex = i * MAX_MESSAGE_SIZE;
//            int endIndex = i == (numSegments - 1) ? fragments.size() : (i + 1) * MAX_MESSAGE_SIZE;
//
//            messageCarriers.add(encodeSegment(fragments, startIndex, endIndex));
//        }
//
//        return messageCarriers;
//    }

     // TODO: Cleanup

    /**
     * Attempts to decode the covert message contained in the {@code carrier}
     * {@link Intent}'s {@link android.os.Bundle}. Will either return the decoded message
     * as a {@link String} or {@code null} if the provided {@code carrier} or
     * the {@link android.os.Bundle} of extras for that {@link Intent} are {@code null}.
     */
//    public String decode(Intent carrier, int buildVersion)
//    {
//        Bundle dataBundle;
//        if(carrier == null || (dataBundle = carrier.getExtras()) == null) {
//            return null; // TODO: Change to empty string
//        }
//
//        Set<String> bundleKeys;
//        bundleKeys = new TreeSet<String>(keyComparator);
//        bundleKeys.addAll(dataBundle.keySet());
//
//        // Create a StringBuilder with an initial capacity equal to the number
//        // of Bundle keys (which is equivalent to the possible number of
//        // encoded characters) for accumulating the covertly-encoded characters
//        StringBuilder messageBuilder = new StringBuilder(bundleKeys.size());
//
//        try
//        {
//            for(String key: bundleKeys)
//            {
//                messageBuilder.append(decodeChar(dataBundle, key, buildVersion));
//            }
//        }
//        catch(IllegalArgumentException e)
//        {
//            // TODO: Log the fact that some message could not be fully encoded
//            // in a more application visible way
//
//            // TODO: DO NOT USE LOGCAT IN THE FINAL IMPLEMENTATION => too visible
//            Log.w(this.getClass().getName(), "Could not fully decode message");
//        }
//
//        return messageBuilder.toString();
//    }

    // TODO: Remove this function

    // TODO: may want to change this to accept a String parameter to allow full
    // words to be encoded using a single code (Huffman-type coding for a
    // dicionary or control language)
    /**
     * Returns {@code true} if the given {@code character} is supported by the
     * encoding schema and {@code false} otherwise.
     */
    public boolean isValueSupported(String str) {
        return dictionary.isSupportedString(str, BUILD_VERSION);
    }

    /**
     * Encodes a single fragment in the given {@link Bundle} using the given
     * {@code key}.
     *
     * @throws IllegalArgumentException	If an unsupported value is provided.
     */
    private Bundle encodeFragment(Bundle dataPacket, String key, String fragment, int buildVersion)
            throws IllegalArgumentException {
        // TODO: implement
        //int charCode = dictionary.getCodeForValue(fragment, buildVersion);
        return dataPacket;
    }
}
