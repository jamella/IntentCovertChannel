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

    // TODO: Implement concept of message and/or app ID's (later)

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

    private int maxBaseValue;

    public BitstringEncoder(int numBaseValues, int numExpansionCodes, List<String> actionStrings, int buildVersion) {
        this.numBaseValues = numBaseValues;
        this.maxBaseValue = numBaseValues - 1;
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
    }

    @Override
    public Collection<Intent> encodeMessage(String message) {
        Message messageToSend = buildMessage(strToBitString(message), maxValue, segmentMaxBitLength, segmentMinBitLength);
        List<String> fragments = messageToSend.getFragments();

        //Log.d(TAG, "Message Fragments with Keys:");
        SegmentMap segmentMap = new SegmentMap(actionStrings, numUniqueValues);
        for(String fragment: fragments) {
            String key = segmentMap.putFragment(fragment);
            //Log.d(TAG, "\"" + key + "\" => " + fragment + "(" + Integer.parseInt(fragment, 2) + ")");
        }

        //Log.d(TAG, "Message Segments");

        int segmentIndex = 0;
        int segmentCount = 0;
        List<Intent> carriers = new ArrayList<>();
        List<Segment> segments = segmentMap.getSegments();
        List<Segment> segmentsToEncode = new ArrayList();
        for(Segment segment: segments) {
            /*
            Log.d(TAG, "Action = \"" + segment.getAction() + "\"");
            Log.d(TAG, "Min Value = " + segment.getMinVal() + " (0b" + Integer.toBinaryString(segment.getMinVal()) + ")");
            Log.d(TAG, "Max Value = " + segment.getMaxVal() + " (0b" + Integer.toBinaryString(segment.getMaxVal()) + ")");
            Log.d(TAG, "segment entries: " + segment.getFragmentMessageKeyMap().size());
            Log.d(TAG, segment.toString());
            */

            if(!segment.isEmpty()) {
                //Log.d(TAG, "Encoding segment \"" + segment.getAction() + "\"");
                segment.setMetadataValue(Segment.SEGMENT_NUMBER_KEY, Integer.toBinaryString(segmentIndex));
                segmentsToEncode.add(segment);
                segmentIndex++;
                segmentCount++;
            }
        }

        for(Segment segment: segmentsToEncode) {
            segment.setMetadataValue(Segment.MESSAGE_SEGMENT_COUNT_KEY, Integer.toBinaryString(segmentCount));
            carriers.add(encodeSegment(segment));
        }

        return carriers;
    }

    public DecodedMessage decodeMessageAsBitstring(Intent carrier) {
        //Log.d(TAG, "Starting to decode bitstring for \"" + carrier.getAction() + "\"");

        if (carrier == null) {
            throw new IllegalArgumentException("Cannot decode message; carrier Intent is null");
        }

        if (carrier.getAction() == null) {
            throw new IllegalArgumentException("Cannot decode message; carrier Intent action is null");
        }

        Bundle dataBundle = carrier.getExtras();
        if (dataBundle == null) {
            throw new IllegalArgumentException("Cannot decode message; extras Bundle is null");
        }

        if (dataBundle.isEmpty()) {
            Log.d(TAG, "No information found for carrier with action \"" + carrier.getAction() + "\"");
            return null;
        }

        // Must have all of the metadata entries and at least one data entry
        if (dataBundle.size() < (Segment.NUM_METADATA_FIELDS +  1)) {
            throw new IllegalArgumentException("Data bundle must contain data fields in addition to metadata fields");
        }

        Set<String> bundleKeys = new TreeSet<>(keyComparator);
        bundleKeys.addAll(dataBundle.keySet());

        int actionOffset = calculateActionOffset(carrier.getAction());

        List<String> messageFragments = new ArrayList<>();
        Iterator<String> bundleKeyIter = bundleKeys.iterator();
        String sigBitsInLastFragmentKey = bundleKeyIter.next();
        String segmentNumberKey = bundleKeyIter.next();
        String segmentCountKey = bundleKeyIter.next();

        // Metadata keys do not use the action offset
        String sigBitsMetadataBitstring = decodeFragmentAsBitstring(dataBundle, sigBitsInLastFragmentKey, 0, fragmentMinBitLength);
        String segmentNumberBitstring = decodeFragmentAsBitstring(dataBundle, segmentNumberKey, 0, fragmentMinBitLength);
        String segmentCountBitstring = decodeFragmentAsBitstring(dataBundle, segmentCountKey, 0, fragmentMinBitLength);

        int numSigBitsInLastFragment = bitstringToInt(sigBitsMetadataBitstring);
        int segmentNumber = bitstringToInt(segmentNumberBitstring);
        int segmentCount = bitstringToInt(segmentCountBitstring);

        //Log.d(TAG, "Decoded metadata field values: numSigBitsInLastFragment = " + numSigBitsInLastFragment + ", segmentNumber = " + segmentNumber + ", segmentCount = " + segmentCount);

        DecodedMessage decodedMessage = new DecodedMessage(segmentNumber, segmentCount);

        try {
            String key = bundleKeyIter.next();
            while (bundleKeyIter.hasNext()) {
                String fragment = decodeFragmentAsBitstring(dataBundle, key, actionOffset, segmentMinBitLength);
                messageFragments.add(fragment);
                decodedMessage.putBitstring(key, fragment);
                key = bundleKeyIter.next();
            }

            // Join the message fragments together, accounting for the fact that the last fragment might have
            // been padded (and therefore need to have the padding removed from the end [least significant
            // bits])
            /* TODO: Move or remove
            for(int i = 0; i < messageFragments.size() - 1; i++) {
                msgBuilder.append(messageFragments.get(i));
            }
            */

            String lastFragmentBitstring = decodeFragmentAsBitstring(dataBundle, key, actionOffset, segmentMinBitLength);
            //Log.d(TAG, "Last fragment: \"" + lastFragmentBitstring + "\"; num bits: " + lastFragmentBitstring.length() + ", significant bits in fragment: " + numSigBitsInLastFragment);
            if(numSigBitsInLastFragment < fragmentMinBitLength) {
                int numPaddingBits = lastFragmentBitstring.length() - numSigBitsInLastFragment;
                lastFragmentBitstring = lastFragmentBitstring.substring(0, lastFragmentBitstring.length() - numPaddingBits);
                //Log.d(TAG, "Last fragment with padding removed: \"" + lastFragmentBitstring + "\"");
            }

            decodedMessage.putBitstring(key, lastFragmentBitstring);
        } catch(IllegalArgumentException e) {
            Log.w(TAG, "Could not fully decode message: " + e.getMessage() + "\n" + e.getStackTrace().toString());
        }

        return decodedMessage;
    }

    @Override
    public String decodeMessage(Intent carrier) {
        //Log.d(TAG, "Starting to decode message: " + carrier.getAction());
        DecodedMessage message = decodeMessageAsBitstring(carrier);
        return message.getMessageString();
    }

    public String decodeFragmentAsBitstring(Bundle dataBundle, String key, int actionOffset, int minBitLength) {
        int value;
        if(EncodingUtils.containsExpansionCode(dataBundle, key)) {
            //Log.d(TAG, "Decoding value and expansion code for key \"" + key + "\"");
            value = decodeExpansionCodeAndValue(dataBundle, key);
        } else {
            //Log.d(TAG, "Decoding value without expansion code for key \"" + key + "\"");
            value = EncodingUtils.decodeValueForEntry(dataBundle, key, 0, buildVersion);
        }

        value += actionOffset;
        String msgBitstring = Integer.toBinaryString(value);

        //Log.d(TAG, "Decoded fragment bitstring: \"" + msgBitstring + "\" (" + msgBitstring.length() + " bits; min bits => " + minBitLength + ") for key \"" + key + "\" with action offset of " + actionOffset);
        if(msgBitstring.length() < minBitLength) {
            msgBitstring = leftPadWithZeroes(msgBitstring, minBitLength - msgBitstring.length());
            //Log.d(TAG, "Bitstring with zero padding: \"" + msgBitstring + "\" (" + msgBitstring.length() + " bits)");
        }

        //Log.d(TAG, "Decoded bitstring " + msgBitstring + " for key \"" + key + "\" with an actionOffset of " + actionOffset);
        return msgBitstring;
    }

    private static String leftPadWithZeroes(String msgBitstring, int numZeroes) {
        String padding = "";
        for(int i = 0; i < numZeroes; i++) {
            padding += "0";
        }

        return padding + msgBitstring;
    }

    private int decodeExpansionCodeAndValue(Bundle dataBundle, String baseKey) {
        //Log.d(TAG, "Decoding expansion code and value for base key \"" + baseKey + "\"");

        Bundle nestedBundle = dataBundle.getBundle(baseKey);

        Set<String> bundleKeys = new TreeSet<>(keyComparator);
        bundleKeys.addAll(nestedBundle.keySet());

        int expansionCode = 1; // The implicit one (the fact that there is a nested Bundle is treated as an expansion code of one)

        // The last key-value pairing holds the base value that the expansion code will be applied too
        // and therefore the last key should be skipped in this loop
        Iterator<String> keyIter = bundleKeys.iterator();
        String key = keyIter.next();
        while(keyIter.hasNext()) {
            //Log.d(TAG, "Decoding nested key \"" + key + "\"");

            if(EncodingUtils.containsExpansionCode(nestedBundle, key)) {
                //Log.d(TAG, "Found deeper nesting; decoding recursively");
                expansionCode += decodeExpansionCodeAndValue(nestedBundle, key);
            } else {
                expansionCode += EncodingUtils.decodeValueForEntry(nestedBundle, key, 0, buildVersion);
            }

            key = keyIter.next();
        }

        int baseValue = EncodingUtils.decodeValueForEntry(nestedBundle, key, expansionCode, buildVersion);

        //Log.d(TAG, "Expansion code for root key \"" + baseKey + "\" => " + expansionCode);
        //Log.d(TAG, "Base value for root key \"" + baseKey + "\" => " + baseValue);

        return baseValue;
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

            //Log.d(TAG, "String \"" + text + "\" as bytes:");

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
                //Log.d(TAG, byteString);
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
        //Log.d(TAG, "Converting bitstring\"" + bitstring + "\" to a text string");

        String[] byteStrings = bitstringToByteStrings(bitstring);
        String msgStr = "";
        for(String byteStr: byteStrings) {
            //Log.d(TAG, "byteStr = " + byteStr);

            char c = (char) Integer.parseInt(byteStr, 2);

            //Log.d(TAG, "Character: " + c);

            msgStr += "" + c;
        }

        //Log.d(TAG, "\"" + bitstring + "\" as text string: " + msgStr);
        return msgStr;
    }

    public static int bitstringToInt(String inputString) {
        // TODO: Remove 
        //Log.d(TAG, "Converting bitstring \"" + inputString + "\" to int");

        /* TODO: Remove
        // Makes sure that the values are interpretted as absolute values (i.e. no negative values allowed)
        String bitstring;
        if(inputString.charAt(0) == '1') {
            bitstring = "0" + inputString;
        } else {
            bitstring = inputString;
        }

        return new BigInteger(bitstring, 2).intValue();
        */

        // Note: if this BigInteger is too big to fit in an int, only the low-order 32 bits are returned.
        // This conversion can lose information about the overall magnitude of the BigInteger value as well
        // as return a result with the opposite sign.
        return new BigInteger(inputString, 2).intValue();
    }

    private Message buildMessage(String bitstring, int maxValue, int fragmentMaxBitLength, int fragmentMinBitLength) {
        /*
        Log.d(TAG, "Building message for bitstring: \"" + bitstring + "\"\nwith max value = " +
                maxValue + ", fragmentMaxBitLength = " + fragmentMaxBitLength + ", fragmentMinBitLength = " + fragmentMinBitLength);
                */

        List<String> fragments = new ArrayList<String>();

        String currentFragment = "";
        int lengthOfLastFragment = 0;
        int fragmentLength = 0;
        for(char bit: bitstring.toCharArray()) {
            String newFragment = currentFragment + bit;
            if(bitstringToInt(newFragment) > maxValue) {
                //Log.d(TAG, "Adding fragment \"" + currentFragment + "\"");
                fragments.add(currentFragment);
                currentFragment = "" + bit;
                fragmentLength = 1;

            // TODO: Test this case (suspect it's not 100% efficient at bit packing
            } else if((newFragment.startsWith("0") && newFragment.length() == fragmentMinBitLength) ||
                    (newFragment.startsWith("1") && newFragment.length() == fragmentMaxBitLength)) {
                //Log.d(TAG, "Adding fragment \"" + newFragment + "\"");
                fragments.add(newFragment);
                currentFragment = "";
                fragmentLength = 0;
            } else {
                currentFragment += bit;
                fragmentLength++;
            }
        }

        lengthOfLastFragment = fragmentLength;

        if(currentFragment.length() != 0) {
            //Log.d(TAG, "Padding the last fragment \"" + currentFragment + "\"");
            while(currentFragment.length() < fragmentMinBitLength) {
                currentFragment += "0";
            }

            //Log.d(TAG, "Adding padded fragment \"" + currentFragment + "\" with a significant bit length of " + lengthOfLastFragment);
            fragments.add(currentFragment);
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

        Bundle msgBundle = new Bundle();

        Set<String> metadataKeys = segment.getMetadataKeys();
        Map<String, String> bitstringsByMessageKey = segment.getFragmentMessageKeyMap();
        for(String msgKey: bitstringsByMessageKey.keySet()) {
            String fragmentBits = bitstringsByMessageKey.get(msgKey);
            int value = bitstringToInt(fragmentBits);

            // Don't use action off set for metadata keys
            if(!metadataKeys.contains(msgKey)) {
                // Subtract minVal to account for the action-defined value band that this segment belongs to
                value -= segment.getMinVal();
            }

            //Log.d(TAG, "Encoding bitstring " + fragmentBits + " as value " + value + " with action " + segment.getAction());
            msgBundle = encodeValue(msgBundle, segment.getAction(), value, msgKey);
        }

        encodedIntent.putExtras(msgBundle);
        return encodedIntent;
    }

    private Bundle encodeValue(Bundle msgBundle, String action, int value, String msgKey) {
        int expansionCode = value / numBaseValues;
        int baseVal = value % numBaseValues;

        //Log.d(TAG, "For value " + value + " with action " + action + " expansionCode = " + expansionCode + ", baseVal = " + baseVal);

        if(expansionCode > 0) {
            return encodeExcodeAndValue(msgBundle, msgKey, baseVal, expansionCode);
        }

        return EncodingUtils.encodeValue(msgBundle, msgKey, baseVal, buildVersion);
    }

    private Bundle encodeExcodeAndValue(Bundle msgBundle, String key, int baseValue, int expansionCode) {
        if(expansionCode <= 0) {
            throw new IllegalArgumentException("Expansion code must be greater than zero");
        }

        //Log.d(TAG, "Encoding expansion code " + expansionCode + " with key \"" + key + "\"");

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
            if(expansionCode >= maxBaseValue) {
                nestedBundle = EncodingUtils.encodeValue(nestedBundle, nestedKeyGenerator.next(), maxBaseValue, buildVersion);
            } else {
                nestedBundle = EncodingUtils.encodeValue(nestedBundle, nestedKeyGenerator.next(), expansionCode, buildVersion);
            }

            expansionCode -= maxBaseValue;
        }

        nestedBundle = EncodingUtils.encodeValue(nestedBundle, nestedKeyGenerator.next(), baseValue, buildVersion);

        msgBundle.putBundle(key, nestedBundle);
        return msgBundle;
    }

    public List<String> getOrderedMessageActionStrings() {
        return new ArrayList<String>(actionStrings);
    }

    public int getFragmentMinBitLength() {
        return fragmentMinBitLength;
    }

    public static class DecodedMessage {
        private Map<String, String> fragmentBitstringsByMessageKey;
        private int segmentNumber;
        private int segmentCount;
        private Comparator<String> keyComparator;

        public DecodedMessage(int segmentNumber, int segmentCount) {
            this.segmentNumber = segmentNumber;
            this.segmentCount = segmentCount;
            fragmentBitstringsByMessageKey = new HashMap<>();
            keyComparator = new AlphabeticalKeySequenceComparator();
        }

        public int getSegmentNumber() {
            return segmentNumber;
        }

        public int getSegmentCount() {
            return segmentCount;
        }

        public Map<String, String> getFragmentBitstringsByMessageKey() {
            return fragmentBitstringsByMessageKey;
        }

        private void putBitstring(String key, String bitstring) {
            //Log.d(TAG, "Key \"" + key + "\" => bitstring: \"" + bitstring +"\"");
            fragmentBitstringsByMessageKey.put(key, bitstring);
        }

        public String getMessageString() {
            StringBuilder msgBuilder = new StringBuilder();
            Set<String> msgKeys = new TreeSet<>(keyComparator);
            msgKeys.addAll(fragmentBitstringsByMessageKey.keySet());

            for(String msgKey: msgKeys) {
                msgBuilder.append(fragmentBitstringsByMessageKey.get(msgKey));
            }

            String consolidatedBitstring = msgBuilder.toString();
            return bitStringToStr(consolidatedBitstring);
        }
    }
}
