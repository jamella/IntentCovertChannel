package intent.covertchannel.intentencoderdecoder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import android.content.Intent;
import android.os.Bundle;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

// TODO: Make the encoding scheme more intelligent by encoding the more
// frequently used characters in the smaller type fields (i.e. byte, short,
// etc.)

// TODO: Retrofit to work with API 7/all APIs
public class LowerCaseAlphaEncoder extends EncodingScheme
{
    // TODO: Change to a configurable value
    private static final int MAX_MESSAGE_SIZE = Integer.MAX_VALUE;

    // TODO: Change to a configurable value
    private static final int BUILD_VERSION = 8;
    private static final String TAG = "intent.covertchannel.intentencoderdecoder.LowerCaseAlphaEncoder";

    private EncodingDictionary dictionary;

    // TODO: Use a more intelligent, less detectable key generation scheme?
    private AlphabeticalKeySequence keySequence;

    // Comparator for ordering the String keys in a Bundle.
    private Comparator<String> keyComparator;

    public LowerCaseAlphaEncoder()
    {
        dictionary = new LowerCaseAlphaEncodingDictionary();
        keySequence = new AlphabeticalKeySequence();
        keyComparator = new AlphabeticalKeySequenceComparator();
    }

    @Override
    public Collection<Intent> encodeMessage(String message, int numBaseValues, int numExpansionCodes, Set<String> actionStrings) {
        // TODO: Actually use all actions strings (for enhanced version)
        String actionString = actionStrings.iterator().next();
        char[] chars = message.toCharArray();
        int numSegments = chars.length / MAX_MESSAGE_SIZE;

        // If MAX_MESSAGE_SIZE does not divide evenly into the number of
        // characters contained in the message to encode then an additional
        // segment is going to be required for encoding the remainder
        if((chars.length % MAX_MESSAGE_SIZE) != 0) {
            ++numSegments;
        }

        Log.d(TAG, "numSegments = " + numSegments);

        List<Intent> messageCarriers = new ArrayList<Intent>();
        for(int i = 0; i < numSegments; i++) {
            // Calculate the bounding indices for the current segment. Note
            // that the start index is inclusive and the end index is
            // exclusive. If this is the last segment then chars.length will be
            // used as the end index; otherwise the start index of the next
            // segment ((i + 1) * MAX_MESSAGE_SIZE) will be used.
            int startIndex = i * MAX_MESSAGE_SIZE;
            int endIndex = i == (numSegments - 1) ? chars.length : (i + 1) * MAX_MESSAGE_SIZE;

            messageCarriers.add(encodeSegment(chars, startIndex, endIndex, actionString));
        }

        return messageCarriers;
    }

    /**
     * Attempts to decode the covert message contained in the {@code carrier}
     * {@link android.content.Intent}'s {@link android.os.Bundle}. Will either return the decoded message
     * as a {@link String} or {@code null} if the provided {@code carrier} or
     * the {@link android.os.Bundle} of extras for that {@link android.content.Intent} are {@code null}.
     */
    public String decode(Intent carrier, int buildVersion) {
        if (carrier == null) {
            return ""; // TODO: Make sure that this return value is handled properly
        }

        Bundle dataBundle = carrier.getExtras();
        if (dataBundle == null) {
            return ""; // TODO: Make sure that this return value is handled properly
        }

        Set<String> bundleKeys;
        bundleKeys = new TreeSet<String>(keyComparator);
        bundleKeys.addAll(dataBundle.keySet());

        // Create a StringBuilder with an initial capacity equal to the number
        // of Bundle keys (which is equivalent to the possible number of
        // encoded characters) for accumulating the covertly-encoded characters
        StringBuilder messageBuilder = new StringBuilder(bundleKeys.size());

        try {
            int currentExpansionCode = 0;
            for(String key: bundleKeys) {
                if(EncodingUtils.containsExpansionCode(dataBundle, key)) {
                    currentExpansionCode = decodeExpansionCode(dataBundle.getBundle(key), buildVersion);
                    Log.d(EncodingUtils.TRACE_TAG, "Expansion code for key \"" + key + "\" is " + currentExpansionCode);
                    continue;
                }

                char c = decodeChar(dataBundle, key, currentExpansionCode, buildVersion);

                Log.d(EncodingUtils.TRACE_TAG, "Decoded char '" + c + "' for key \"" + key + "\"");

                messageBuilder.append(c);

                if(currentExpansionCode != 0) {
                    currentExpansionCode = 0;
                }
            }
        } catch(IllegalArgumentException e) {
            // TODO: Log the fact that some message could not be fully encoded in a more application visible way

            // TODO: DO NOT USE LOGCAT IN THE FINAL IMPLEMENTATION => too visible
            Log.w(this.getClass().getName(), "Could not fully decode message: " + e.getMessage() + "\n" + e.getStackTrace().toString());
        }

        String message = messageBuilder.toString();

        Log.d(EncodingUtils.TRACE_TAG, "Decoded message \"" + message + "\"");

        return message;
    }

    public int decodeExpansionCode(Bundle nestedBundle, int buildVersion) {
        if(nestedBundle == null) {
            return 0;
        }

        Set<String> bundleKeys;
        bundleKeys = new TreeSet<String>(keyComparator);
        bundleKeys.addAll(nestedBundle.keySet());

        int exCodeValue = 0;

        try {
            int currentExpansionCode = 0;
            for(String bundleKey: bundleKeys) {
                if(EncodingUtils.containsExpansionCode(nestedBundle, bundleKey)) {
                    currentExpansionCode += decodeExpansionCode(nestedBundle, buildVersion);
                    Log.d(EncodingUtils.TRACE_TAG, "Expansion code for key \"" + bundleKey + "\" is " + currentExpansionCode);
                    continue;
                }

                exCodeValue += EncodingUtils.decodeCharCode(nestedBundle, bundleKey, currentExpansionCode, buildVersion);

                if(currentExpansionCode != 0) {
                    currentExpansionCode = 0;
                }
            }
        } catch(IllegalArgumentException e) {
            // TODO: Log the fact that some message could not be fully encoded in a more application visible way

            // TODO: DO NOT USE LOGCAT IN THE FINAL IMPLEMENTATION => too visible
            Log.w(this.getClass().getName(), "Could not fully decode message: " + e.getMessage() + "\n" + e.getStackTrace().toString());
        }

        Log.d(EncodingUtils.TRACE_TAG, "Decoded expansion code value " + exCodeValue);

        /* TODO: Remove
        int expansionCode = 0;
        while(nestedBundle != null) {
            Log.d(TRACE_TAG, "Examining nested bundle (expansionCode = " + expansionCode + ")");

            expansionCode++;

            // The inner-most Bundle (i.e. the one of the end of the nesting
            // chain) is the once which contains the actual character encoding
            charCodeBundle = nestedBundle;

            // TODO: Use a constant and/or allow for variability
            try {
                nestedBundle = nestedBundle.getBundle("a");
            } catch(ClassCastException e) {
                nestedBundle = null;
            }
        }

        return expansionCode;
        */

        return exCodeValue;
    }

    /**
     * Returns {@code true} if the given {@code character} is supported by the
     * encoding schema and {@code false} otherwise.
     */
    public boolean isCharSupported(char character)
    {
        return dictionary.isSupportedChar(character, BUILD_VERSION);
    }

    /**
     * Encodes the characters in {@code charArray} from {@code startIndex}
     * (inclusively) to {@code endIndex} (exclusively) into a {@link android.os.Bundle}
     * encapsulated in an {@link android.content.Intent}.
     *
     * @throws IllegalArgumentException	If the {@code startIndex} is not a
     * valid {@code charArray} index, if {@code endIndex} is less than 0 or
     * greater than {@code charArray}.{@code length} (not that it can be equal
     * to the length of the array as it is exclusive), or if {@code startIndex}
     * is greater than or equal to {@code endIndex}.
     */
    private Intent encodeSegment(char[] charArray, int startIndex, int endIndex, String actionString) throws IllegalArgumentException {
        if(startIndex < 0 || startIndex >= charArray.length || endIndex < 0 || endIndex > charArray.length || startIndex >= endIndex) {
            throw new IllegalArgumentException("In " + this.getClass().getCanonicalName() + ": Invalid index value");
        }

        Intent carrier = new Intent();
        Bundle dataPacket = new Bundle();

        for(int i = startIndex; i < endIndex; i++) {
            // TODO: Make sure that passing in the key-generator works as expected
            dataPacket = encodeChar(dataPacket, keySequence, charArray[i], BUILD_VERSION);
        }

        carrier.putExtras(dataPacket);
        carrier.setType("plain/text");
        carrier.setAction(actionString);

        Log.d(TAG, "Encoded message onto Intent with action of " + actionString);

        return carrier;
    }

    public Bundle encodeExcode(Bundle dataPacket, int expansionCode, String key, int buildVersion) {
        // TODO: Refactor based on new ex-code encoding scheme (but preserve old version)
        if(expansionCode < 0) {
            throw new IllegalArgumentException("Expansion code must be greater than or equal to zero");
        }

        Log.d(EncodingUtils.TRACE_TAG, "Encoding expansion code " + expansionCode + " with key \"" + key + "\"");

        Bundle nestedBundle = null;
        if(expansionCode >= 1) {
            nestedBundle = new Bundle();

            KeyGenerator nestedKeyGenerator = new AlphabeticalKeySequence();
            while (expansionCode > 0) {
                if(expansionCode >= NUM_BASE_VALUES) {
                    nestedBundle = EncodingUtils.encodeValue(nestedBundle, nestedKeyGenerator.next(), NUM_BASE_VALUES, buildVersion);
                } else {
                    nestedBundle = EncodingUtils.encodeValue(nestedBundle, nestedKeyGenerator.next(), expansionCode, buildVersion);
                }

                expansionCode -= NUM_BASE_VALUES;
            }
        }

        dataPacket.putBundle(key, nestedBundle);
        return dataPacket;
    }

    /**
     * Encodes a single character in the given {@link android.os.Bundle} using the given
     * {@code key}.
     *
     * @throws IllegalArgumentException	If an unsupported character (anything
     * that is not a lower case character from a-z) is provided.
     */
    private Bundle encodeChar(Bundle dataPacket, KeyGenerator keyGenerator, char c, int buildVersion) throws IllegalArgumentException {
        // TODO: Create object to encapsulate the datapacket and key-generator to make this more functional and less hacky

        if(!isCharSupported(c)) {
            throw new IllegalArgumentException("In " + this.getClass().getName() + ".encodeChar(): Unsupported " + "character " + c + " encountered");
        }

        int charCode = dictionary.getCodeForValue(String.valueOf(c), buildVersion);
        int expansionCode = charCode / NUM_BASE_VALUES;
        int baseVal = charCode % NUM_BASE_VALUES;

        Log.d(TAG, "Encoding character '" + c + "' as int value " + charCode + "; expansionCode = " + expansionCode + ", baseVal = " + baseVal);

        if(expansionCode > 0) {
            dataPacket = encodeExcode(dataPacket, expansionCode, keyGenerator.next(), buildVersion);
        }

        return EncodingUtils.encodeValue(dataPacket, keyGenerator.next(), baseVal, buildVersion);
    }


    /**
     * Decodes the character encoded in the given {@link android.os.Bundle} for the given
     * {@code key}.
     *
     * @throws IllegalArgumentException	If the provided {@code key} does not
     * exist in the given {@link android.os.Bundle} or if no character encoding was found
     * for that {@code key}.
     */
    private char decodeChar(Bundle dataPacket, String key, int expansionCode, int buildVersion) throws IllegalArgumentException {
        int charCode = EncodingUtils.decodeCharCode(dataPacket, key, expansionCode, buildVersion);

        Log.d(EncodingUtils.TRACE_TAG, "Decoded char code of " + charCode + "for the key \"" + key + "\"");

        String value = dictionary.getValue(charCode, buildVersion);

        Log.d(EncodingUtils.TRACE_TAG, "Decoded value of " + value + " for the key \"" + key + "\"");

        return value.toCharArray()[0];
    }
}

//
// TODO: Cleanup
//

//public class LowerCaseAlphaEncoder extends EncodingScheme {
//
//	public LowerCaseAlphaEncoder() {
//        dictionary = new LowerCaseAlphaEncodingDictionary();
//        keyGenerator = new AlphabeticalKeySequence();
//        keyComparator = new AlphabeticalKeySequenceComparator();
//	}
//
//    protected String[] splitMessageIntoFragments(String message) {
//        // TODO: Make sure to validate character set values (a-z plus space character)
//
//        char[] chars = message.toCharArray();
//        String[] strs = new String[chars.length];
//        for(int i = 0; i < chars.length; i++) {
//            strs[i] = Character.toString(chars[i]).toLowerCase();
//        }
//
//        return strs;
//    }
//
//    public Bundle encodeExcodeAndValue(int expansionCode, int baseVal, String key, int buildVersion) {
//        // TODO: Refactor based on new ex-code encoding scheme (but preserve old version)
//        Bundle bundle = new Bundle();
//
//        if(expansionCode <= 0) {
//            throw new IllegalArgumentException("Expansion code must be greater than zero");
//        }
//
//        if(expansionCode > 1) {
//            while (expansionCode > 0) {
//                if(expansionCode >= NUM_BASE_VALUES) {
//                    bundle = encodeValue(bundle, key, NUM_BASE_VALUES, buildVersion);
//                } else {
//                    bundle = encodeValue(bundle, key, expansionCode, buildVersion);
//                }
//
//                expansionCode -= NUM_BASE_VALUES;
//            }
//        }
//
//        return encodeValue(bundle, key, baseVal, buildVersion);
//    }
//
//    @Override
//    public Collection<Intent> encodeMessage(String message, int numBaseValues, int numExpansionCodes, Set<String> actionStrings) {
//        String[] fragments = splitMessageIntoFragments(message);
//        int numSegments = fragments.length / MAX_MESSAGE_SIZE; // TODO: incorporate action-string bands here
//
//        // If MAX_MESSAGE_SIZE does not divide evenly into the number of
//        // characters contained in the message to encode then an additional
//        // segment is going to be required for encoding the remainder
//        if((fragments.length % MAX_MESSAGE_SIZE) != 0) {
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
//            int endIndex = i == (numSegments - 1) ? fragments.length : (i + 1) * MAX_MESSAGE_SIZE;
//
//            messageCarriers.add(encodeSegment(fragments, startIndex, endIndex));
//        }
//
//        return messageCarriers;
//    }
//
//
//
//    /**
//     * Encodes the message into a {@link android.os.Bundle} encapsulated in an {@link android.content.Intent}.
//     *
//     * @throws IllegalArgumentException	If the {@code startIndex} is not a
//     * valid {@code charArray} index, if {@code endIndex} is less than 0 or
//     * greater than {@code charArray}.{@code length} (not that it can be equal
//     * to the length of the array as it is exclusive), or if {@code startIndex}
//     * is greater than or equal to {@code endIndex}.
//     */
//    private Intent encodeMessage(Message message, int startIndex, int endIndex, int buildVersion) // TODO: Remove idices
//            throws IllegalArgumentException
//    {
//        Collection<String> fragments = message.getFragments();
//        if(startIndex < 0 || startIndex >= fragments.size() ||
//                endIndex < 0 || endIndex > fragments.size() ||
//                startIndex >= endIndex)
//        {
//            throw new IllegalArgumentException("In " +
//                    this.getClass().getCanonicalName() + ": Invalid index value");
//        }
//
//        Intent carrier = new Intent();
//        Bundle dataPacket = new Bundle();
//
//        Iterator<String> iterator = message.iterator();
//        while(iterator.hasNext()) {
//            String fragment = iterator.next();
//
//            // TODO: Update/refactor
//            if(!isValueSupported(fragment))
//            {
//                throw new IllegalArgumentException("In " +
//                        this.getClass().getName() + ".encodeChar(): Unsupported " +
//                        "character " + fragment + " encountered");
//            }
//
//            dataPacket = encodeFragment(dataPacket, keyGenerator.next(), fragment, BUILD_VERSION);
//        }
//
//        carrier.putExtras(dataPacket);
//        return carrier;
//    }
//
//    /**
//     * Decodes the character encoded in the given {@link Bundle} for the given
//     * {@code key}.
//     *
//     * @throws IllegalArgumentException	If the provided {@code key} does not
//     * exist in the given {@link Bundle} or if no character encoding was found
//     * for that {@code key}.
//     */
//    private String decodeChar(Bundle dataPacket, String key, int buildVersion)
//            throws IllegalArgumentException
//    {
//        int charCode = EncodingUtils.decodeCharCode(dataPacket, key, buildVersion);
//        return dictionary.getValue(charCode, buildVersion);
//    }
//}
