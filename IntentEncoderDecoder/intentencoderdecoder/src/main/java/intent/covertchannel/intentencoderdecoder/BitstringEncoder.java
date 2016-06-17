package intent.covertchannel.intentencoderdecoder;

import android.content.Intent;
import android.util.Log;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by Tim on 5/24/2016.
 */
public class BitstringEncoder extends EncodingScheme {

    // TODO: Print lines to log lines

    // TODO: Make these config values instead
    public static final int NUM_EXANSION_CODES = NUM_BASE_VALUES + 1;
    public static final int NUM_DATA_ACTIONS = 4;
    private static final String TAG = "intent.covertchannel.intentencoderdecoder.BitstringEncoder";

    /* TODO: Cleanup
    // There are NUM_EXANSION_CODES + 1 different value sets (the first value set has no expansion code)
    //public static final int NUM_UNIQUE_VALUES = NUM_BASE_VALUES * (NUM_EXANSION_CODES + 1) * NUM_DATA_ACTIONS;
    public static final int MAX_VALUE = NUM_UNIQUE_VALUES - 1;
    public static final int FRAGMENT_MAX_BIT_LENGTH = calculateMaxFragmentBitLength(MAX_VALUE);
    public static final int FRAGMENT_MIN_BIT_LENGTH = calculateMinFragmentBitLength(MAX_VALUE);
    */

    public BitstringEncoder() {
        // TODO: Change dictionary
        dictionary = new LowerCaseAlphaEncodingDictionary();
        keyGenerator = new AlphabeticalKeySequence();
        keyComparator = new AlphabeticalKeySequenceComparator();
    }

    @Override
    public Collection<Intent> encodeMessage(String message, int numBaseValues, int numExpansionCodes, Set<String> actionStrings) {
        // There are NUM_EXANSION_CODES + 1 different value sets (the first value set has no expansion code)
        final int numUniqueValues = numBaseValues * (numExpansionCodes + 1) * actionStrings.size();
        final int maxValue = numUniqueValues - 1;
        final int fragmentMaxBitLength = calculateMaxFragmentBitLength(maxValue);
        final int fragmentMinBitLength = calculateMinFragmentBitLength(maxValue);

        Message messageToSend = buildMessage(strToBitString(message), maxValue, fragmentMaxBitLength, fragmentMinBitLength);
        Collection<String> fragments = messageToSend.getFragments();

        Log.d(TAG, "Message Fragments with Keys:");
        SegmentMap segmentMap = new SegmentMap(actionStrings, numUniqueValues);
        for(String fragment: fragments) {
            String key = segmentMap.putFragment(fragment);
            Log.d(TAG, "\"" + key + "\" => " + fragment + "(" + Integer.parseInt(fragment, 2) + ")");
        }

        Log.d(TAG, "");
        Log.d(TAG, "Message Segments");

        Set<Segment> segments = segmentMap.getSegments();
        for(Segment segment: segments) {
            Log.d(TAG, "Action = \"" + segment.getAction() + "\"");
            Log.d(TAG, "Min Value = " + segment.getMinVal() + " (0b" + Integer.toBinaryString(segment.getMinVal()) + ")");
            Log.d(TAG, "Max Value = " + segment.getMaxVal() + " (0b" + Integer.toBinaryString(segment.getMaxVal()) + ")");

            Map<String, String> fragmentMessageKeyMap = segment.getFragmentMessageKeyMap();
            for(String key: fragmentMessageKeyMap.keySet()) {
                Log.d(TAG, "\"" + key + "\" => " + fragmentMessageKeyMap.get(key) + "(" + Integer.parseInt(fragmentMessageKeyMap.get(key), 2) + ")");
            }
        }

        Log.d(TAG, "\nMessage Fragment Expansion Codes");
        for(String fragment: fragments) {
            int fragmentIntVal = bitstringToInt(fragment);
            Log.d(TAG, String.valueOf(fragmentIntVal / NUM_BASE_VALUES));
        }

        Log.d(TAG, "\nMessage Fragment Base Values");
        for(String fragment: fragments) {
            int fragmentIntVal = bitstringToInt(fragment);
            Log.d(TAG, String.valueOf(fragmentIntVal % NUM_BASE_VALUES));
        }

        /* TODO: Incorporate
        carrier.setType("plain/text");
        carrier.setAction(actionString);
        */

        return null; // TODO: implement
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

    // Actually throws UnsupportedEncodingException
    public static String bitStringToStr(String bitstring) throws Exception {
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
