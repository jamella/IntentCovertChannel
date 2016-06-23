package intent.covertchannel.intentencoderdecoder;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Created by Tim on 5/24/2016.
 */
public class BitstringEncoder implements EncodingScheme {

    // TODO: Cleanup
    //private static final String TAG = "intent.covertchannel.intentencoderdecoder.BitstringEncoder";
    private static final String TAG = EncodingUtils.TRACE_TAG;
    private final int numUniqueValues;
    private final int maxValue;
    private final int fragmentMaxBitLength;
    private final int fragmentMinBitLength;
    private final int numValuesPerAction;
    private final int maxFragmetValue;
    private final int segmentMaxBitLength;
    private final int segmentMinBitLength;

    protected KeyGenerator keyGenerator;

    // Comparator for ordering the String keys in a Bundle.
    protected Comparator<String> keyComparator;
    private int numBaseValues;
    private int numExpansionCodes;
    private List<String> actionStrings;
    private int buildVersion;
    private Map<String, String> actionToMessageMap;

    public BitstringEncoder(int numBaseValues, int numExpansionCodes, List<String> actionStrings, int buildVersion) {
        this.numBaseValues = numBaseValues;
        this.numExpansionCodes = numExpansionCodes;
        this.actionStrings = actionStrings;
        this.buildVersion = buildVersion;

        // There are NUM_EXANSION_CODES + 1 different value sets (the first value set has no expansion code)
        this.numValuesPerAction = numBaseValues * (numExpansionCodes + 1);
        this.maxFragmetValue = numValuesPerAction - 1;
        this.fragmentMaxBitLength = calculateMaxFragmentBitLength(maxFragmetValue);
        this.fragmentMinBitLength = calculateMinFragmentBitLength(maxFragmetValue);

        this.numUniqueValues = numValuesPerAction * actionStrings.size();
        this.maxValue = numUniqueValues - 1;
        this.segmentMaxBitLength = calculateMaxFragmentBitLength(maxValue);
        this.segmentMinBitLength = calculateMinFragmentBitLength(maxValue);

        this.keyGenerator = new AlphabeticalKeySequence();
        this.keyComparator = new AlphabeticalKeySequenceComparator();
        this.actionToMessageMap = new HashMap<>();
    }

    @Override
    public Collection<Intent> encodeMessage(String message) {
        Message messageToSend = buildMessage(strToBitString(message), maxValue, fragmentMaxBitLength, fragmentMinBitLength);
        List<String> fragments = messageToSend.getFragments();

        // TODO: Make sure that the fragments are being iterated over in order
        Log.d(TAG, "Message Fragments with Keys:");
        SegmentMap segmentMap = new SegmentMap(actionStrings, numUniqueValues);
        for(String fragment: fragments) {
            String key = segmentMap.putFragment(fragment);
            Log.d(TAG, "\"" + key + "\" => " + fragment + "(" + Integer.parseInt(fragment, 2) + ")");
        }

        Log.d(TAG, "");
        Log.d(TAG, "Message Segments");

        List<Intent> carriers = new ArrayList<>();

        // TODO: Make sure that this get iterated over in order
        List<Segment> segments = segmentMap.getSegments();
        for(Segment segment: segments) {
            Log.d(TAG, "Action = \"" + segment.getAction() + "\"");
            Log.d(TAG, "Min Value = " + segment.getMinVal() + " (0b" + Integer.toBinaryString(segment.getMinVal()) + ")");
            Log.d(TAG, "Max Value = " + segment.getMaxVal() + " (0b" + Integer.toBinaryString(segment.getMaxVal()) + ")");
            carriers.add(encodeSegment(segment));
        }

        return carriers;
    }

    @Override
    public String decodeMessage(Intent carrier) {
        // TODO: Implement concept of message and/or app ID's (later)

        if (carrier == null) {
            throw new IllegalArgumentException("Cannot decode message; carrier Intent is null");
        }

        if (carrier.getAction() == null) {
            throw new IllegalArgumentException("Cannot decode message; carrier Intent action is null");
        }
        if(actionToMessageMap.containsKey(carrier.getAction())) {
            throw new IllegalArgumentException("Message action " + carrier.getAction() + " has already been seen");
        }

        Bundle dataBundle = carrier.getExtras();
        if (dataBundle == null) {
            throw new IllegalArgumentException("Cannot decode message; extras Bundle is null");
        }

        Set<String> bundleKeys;
        bundleKeys = new TreeSet<>(keyComparator);
        bundleKeys.addAll(dataBundle.keySet());

        int actionOffset = calculateActionOffset(carrier.getAction());

        StringBuilder messageBuilder = new StringBuilder(bundleKeys.size());
        try {
            for(String key: bundleKeys) {
                int value;
                if(EncodingUtils.containsExpansionCode(dataBundle, key)) {
                    Log.d(TAG, "Decoding value and expansion code for key \"" + key + "\"");
                    value = decodeExpansionCodeAndValue(dataBundle, key);
                } else {
                    Log.d(TAG, "Decoding value without expansion code for key \"" + key + "\"");
                    value = EncodingUtils.decodeValueForEntry(dataBundle, key, 0, buildVersion);
                }

                value += actionOffset;
                String msgBitstring = Integer.toBinaryString(value);

                // TODO: Pad bitstring with zeroes (from the left/most significant bits) up to min length
                if(msgBitstring.length() < this.fragmentMinBitLength) {
                    msgBitstring = leftPadWithZeroes(msgBitstring, this.fragmentMinBitLength - msgBitstring.length());
                }
                Log.d(TAG, "Decoded bitstring " + msgBitstring + " for key \"" + key + "\" with an actionOffset of " + actionOffset);

                String msg = bitStringToStr(msgBitstring);
                messageBuilder.append(msg);
            }
        } catch(IllegalArgumentException e) {
            Log.w(TAG, "Could not fully decode message: " + e.getMessage() + "\n" + e.getStackTrace().toString());
        }

        String msg = messageBuilder.toString();

        // TODO: Change back to actual text
        actionToMessageMap.put(carrier.getAction(), msg);
        //actionToMessageMap.put(carrier.getAction(), strToBitString(msg));

        return msg;
    }

    private static String leftPadWithZeroes(String msgBitstring, int numZeroes) {
        String padding = "";
        for(int i = 0; i < numZeroes; i++) {
            padding += "0";
        }

        return padding + msgBitstring;
    }

    private int decodeExpansionCodeAndValue(Bundle dataBundle, String baseKey) {
        Log.d(TAG, "Decoding expansion code and value for base key \"" + baseKey + "\"");

        Bundle nestedBundle = dataBundle.getBundle(baseKey);

        Set<String> bundleKeys = new TreeSet<>(keyComparator);
        bundleKeys.addAll(dataBundle.keySet());

        int expansionCode = 1; // The implicit one (the fact that there is a nested Bundle is treated as an expansion code of one)

        // The last key-value pairing holds the base value that the expansion code will be applied too
        // and therefore the last key should be skipped in this loop
        Iterator<String> keyIter = bundleKeys.iterator();
        String key = keyIter.next();
        while(keyIter.hasNext()) {
            if(EncodingUtils.containsExpansionCode(nestedBundle, key)) {
                Log.d(TAG, "Found deeper nesting; decoding recursively");
                expansionCode += decodeExpansionCodeAndValue(nestedBundle, key);
            } else {
                expansionCode += EncodingUtils.decodeValueForEntry(nestedBundle, key, 0, buildVersion);
            }

            key = keyIter.next();
        }

        // TODO: Figure out why the value for the expansion code is not being read/evaluated
        int baseValue = EncodingUtils.decodeValueForEntry(nestedBundle, key, expansionCode, buildVersion);

        Log.d(TAG, "Expansion code for root key \"" + baseKey + "\" => " + expansionCode);
        Log.d(TAG, "Base value for root key \"" + baseKey + "\" => " + baseValue);

        return baseValue;
    }

    /**
     * Returns the current message which has been decoded so far.
     */
    @Override
    public String getMessage() {
        StringBuilder msgBuilder = new StringBuilder();

        // TODO: Implement message metadata fields (number of significant bits in last fragment, number of segments in message, segment number, and app ID)
        // The EncodingUtils.ACTIONS list is used to determine the ordering of the different segments
        for(String action: EncodingUtils.ACTIONS) {
            if(actionToMessageMap.containsKey(action)) {
                msgBuilder.append(actionToMessageMap.get(action));
            }
        }

        actionToMessageMap.clear();
        return msgBuilder.toString();
    }

    // Taken from http://stackoverflow.com/questions/3305059/how-do-you-calculate-log-base-2-in-java-for-integers;
    // as noted in the answer, this approach might not be completely accurate, but it should be sufficient for current
    // purposes
    public static int calculateMaxFragmentBitLength(int maxValue) {
        double maxBitLength = Math.log(maxValue) / Math.log(2);
        if(maxBitLength % 1 == 0) {
            return (int) maxBitLength;
        }

        return ((int) maxBitLength) + 1;
    }

    public static int calculateMinFragmentBitLength(int maxValue) {
        double minBitLength = Math.log(maxValue) / Math.log(2);
        return (int) minBitLength;
    }

    private int calculateActionOffset(String action) {
        return EncodingUtils.ACTIONS.indexOf(action) * numValuesPerAction;
    }

    public static String strToBitString(String text) {
        try {
            String[] byteStrings = new String[text.length()];

            // TODO: Remove
            Log.d(TAG, "String \"" + text + "\" as bytes:");
            //

            int i = 0;
            for (char c: text.toCharArray()) {
                String byteString = Integer.toBinaryString((int) c);

                // Pad to a full byte
                while(byteString.length() < 8) {
                    byteString = "0" + byteString;
                }

                byteStrings[i] = byteString;
                i++;

                // TODO: Remove
                Log.d(TAG, byteString);
                //
            }

            String result = "";
            for(String byteStr: byteStrings) {
                result += byteStr;
            }

            return result;
        } catch(Exception e) {
            return "Exception!";
        }
    }

    public static String[] bitstringToByteStrings(String bitstring) {
        int bitsInByte = 0;
        int byteIndex = 0;

        // Assumes that the provided bitstring is evenly divisible
        // by 8 (i.e. consists of full-formed bytes)
        String[] bytes = new String[bitstring.length() / 8];
        //String b = "0"; // Prime a leftmost 0 which will be the most significant digit
        String b = "";
        for (char bit: bitstring.toCharArray()) {
            b += bit;
            bitsInByte++;

            if(bitsInByte == 8) {
                bytes[byteIndex] = b;
                bitsInByte = 0;
                b = "";
                byteIndex++;
            }
        }

        return bytes;
    }

    public static String bitStringToStr(String bitstring) throws NumberFormatException {
        String[] byteStrings = bitstringToByteStrings(bitstring);
        String msgStr = "";
        for(String byteStr: byteStrings) {
            Log.d(TAG, "byteStr = " + byteStr);

            char c = (char) Integer.parseInt(byteStr, 2);

            // TODO: Remove
            Log.d(TAG, "Character: " + c);

            msgStr += "" + c;
        }

        return msgStr;
    }

    public static int bitstringToInt(String bitstring) {
        // Note: if this BigInteger is too big to fit in an int, only the low-order 32 bits are returned.
        // This conversion can lose information about the overall magnitude of the BigInteger value as well
        // as return a result with the opposite sign.
        return new BigInteger(bitstring, 2).intValue();
    }

    private Message buildMessage(String bitstring, int maxValue, int fragmentMaxBitLength, int fragmentMinBitLength) {
        List<String> fragments = new ArrayList<String>();

        String currentFragment = "";
        int lengthOfLastFragment = 0;
        int fragmentLength = 0;
        for(char bit: bitstring.toCharArray()) {
            String newFragment = currentFragment + bit;
            if(bitstringToInt(newFragment) > maxValue) {
                fragments.add(currentFragment);
                currentFragment = "" + bit;
            } else if((newFragment.startsWith("0") && newFragment.length() == fragmentMinBitLength) ||
                    (newFragment.startsWith("1") && newFragment.length() == fragmentMaxBitLength)) {
                fragments.add(newFragment);
                fragmentLength = 0;
                currentFragment = "";
            } else {
                currentFragment += bit;
            }

            fragmentLength++;
        }

        lengthOfLastFragment = fragmentLength;

        if(currentFragment.length() != 0) {
            // Pad the last fragment from right with zeros
            while(currentFragment.length() < fragmentMaxBitLength) {
                currentFragment += "0";
            }

            fragments.add(currentFragment);
            currentFragment = "";
        }

        return new Message(fragments, lengthOfLastFragment);
    }

    public static String fragmentsToBitString(Collection<String> fragments, int lengthOfLastFragment) {
        String bitstring = "";
        Iterator<String> iter = fragments.iterator();
        while(iter.hasNext()) {
            String fragment = iter.next();
            if(!iter.hasNext()) { // if last fragment
                bitstring += fragment.substring(0, lengthOfLastFragment);
            } else {
                bitstring += fragment;
            }
        }

        return bitstring;
    }

    private Intent encodeSegment(Segment segment) {
        Intent encodedIntent = new Intent();

        encodedIntent.setType("plain/text");
        encodedIntent.setAction(segment.getAction());

        // TODO: Incorporate concept of metadata fields (first entry needs to be the number of significant bits in the last fragment for each segment [i.e. Intent])
        Bundle msgBundle = new Bundle();

        Map<String, String> bitstringsByMessageKey = segment.getFragmentMessageKeyMap();
        for(String msgKey: bitstringsByMessageKey.keySet()) {
            String fragmentBits = bitstringsByMessageKey.get(msgKey);

            // Subtract minVal to account for the action-defined value band that this segment belongs to
            int value = bitstringToInt(fragmentBits) - segment.getMinVal();

            Log.d(TAG, "Encoding bitstring " + fragmentBits + " as value " + value + " with action " + segment.getAction());
            msgBundle = encodeValue(msgBundle, segment.getAction(), value, msgKey);
        }

        encodedIntent.putExtras(msgBundle);
        return encodedIntent;
    }

    // TODO: Create decode version
    private Bundle encodeValue(Bundle msgBundle, String action, int value, String msgKey) {
        int expansionCode = value / numBaseValues;
        int baseVal = value % numBaseValues;

        Log.d(TAG, "For value " + value + " with action " + action + " expansionCode = " + expansionCode + ", baseVal = " + baseVal);

        if(expansionCode > 0) {
            return encodeExcodeAndValue(msgBundle, msgKey, baseVal, expansionCode);
        }

        return EncodingUtils.encodeValue(msgBundle, msgKey, baseVal, buildVersion);
    }

    // TODO: Create decode version
    private Bundle encodeExcodeAndValue(Bundle msgBundle, String key, int baseValue, int expansionCode) {
        if(expansionCode <= 0) {
            throw new IllegalArgumentException("Expansion code must be greater than zero");
        }

        Log.d(TAG, "Encoding expansion code " + expansionCode + " with key \"" + key + "\"");

        // Uses the implicit-one expansion code encoding strategy where the contents of the nested bundle
        // are recursively decoded using the standard decoding process to get the expansion code which is
        // applied to the value associated with the last key in order within that Bundle (this value is excluded
        // from the expansion code decoding process). However, the presence of a nested Bundle is counted as an
        // implicit expansion code of one (therefore, if the nested Bundle contains only one key-value pair,
        // that value is interpretted using an expansion code of one; otherwise, one is added to whatever expansion code
        // value is decoded).

        Bundle nestedBundle = new Bundle();
        KeyGenerator nestedKeyGenerator = new AlphabeticalKeySequence();
        expansionCode--; // Decrement by one to signify the implicit one

        while (expansionCode > 0) {
            if(expansionCode >= numBaseValues) {
                nestedBundle = EncodingUtils.encodeValue(nestedBundle, nestedKeyGenerator.next(), numBaseValues, buildVersion);
            } else {
                nestedBundle = EncodingUtils.encodeValue(nestedBundle, nestedKeyGenerator.next(), expansionCode, buildVersion);
            }

            expansionCode -= numBaseValues;
        }

        nestedBundle = EncodingUtils.encodeValue(nestedBundle, nestedKeyGenerator.next(), baseValue, buildVersion);

        msgBundle.putBundle(key, nestedBundle);
        return msgBundle;
    }

    // TODO: Remove
    /*
    public static void main(String[] args) {
        try {
            String msg = "´The quick brown fox´";

            System.out.println("Converting \"" + msg + "\" to a message fragments..\n");
            System.out.println("As bitstring: " + strToBitString(msg) + "\n");

            Message messageToSend = buildMessage(strToBitString(msg));
            Collection<String> fragments = messageToSend.getFragments();

            System.out.println("Message Fragments with Keys:");
            SegmentMap segmentMap = new SegmentMap(NUM_DATA_ACTIONS, NUM_UNIQUE_VALUES);
            for(String fragment: fragments) {
                String key = segmentMap.putFragment(fragment);
                System.out.println("\"" + key + "\" => " + fragment + "(" + Integer.parseInt(fragment, 2) + ")");
            }

            System.out.println("");
            System.out.println("Message Segments");

            Set<Segment> segments = segmentMap.getSegments();
            for(Segment segment: segments) {
                System.out.println("");
                System.out.println("Action = \"" + segment.getAction() + "\"");
                System.out.println("Min Value = " + segment.getMinVal() + " (0b" + Integer.toBinaryString(segment.getMinVal()) + ")");
                System.out.println("Max Value = " + segment.getMaxVal() + " (0b" + Integer.toBinaryString(segment.getMaxVal()) + ")");
                System.out.println("");

                Map<String, String> fragmentMessageKeyMap = segment.getFragmentMessageKeyMap();
                for(String key: fragmentMessageKeyMap.keySet()) {
                    System.out.println("\"" + key + "\" => " + fragmentMessageKeyMap.get(key) + "(" + Integer.parseInt(fragmentMessageKeyMap.get(key), 2) + ")");
                }
            }

            System.out.println("\nMessage Fragment Expansion Codes");
            for(String fragment: fragments) {
                int fragmentIntVal = bitstringToInt(fragment);
                System.out.println(fragmentIntVal / NUM_BASE_VALUES);
            }

            System.out.println("\nMessage Fragment Base Values");
            for(String fragment: fragments) {
                int fragmentIntVal = bitstringToInt(fragment);
                System.out.println(fragmentIntVal % NUM_BASE_VALUES);
            }

            String receivedBitstring = fragmentsToBitString(fragments, messageToSend.getLengthOfLastFragment());

            System.out.println("Received bitstring: " + receivedBitstring);

            String receivedMessage = bitStringToStr(receivedBitstring);

            System.out.println("Received \"" + receivedMessage + "\"");
        } catch(Exception e) {
            System.out.println("Exception occurred!");
            e.printStackTrace();
        }
    }
    */
}
