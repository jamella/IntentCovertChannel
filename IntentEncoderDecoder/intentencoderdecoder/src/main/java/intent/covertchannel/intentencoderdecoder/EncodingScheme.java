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
public interface EncodingScheme {

    // TODO: Find out if null values are allowed; if so, 22 values are possible
    public static final int NUM_BASE_VALUES = 21;

    // TODO: Change to a configurable value (or just remove)
    public static final int MAX_MESSAGE_SIZE = Integer.MAX_VALUE;

    // TODO: Remove references to this; make it either a parameter or a system config value
    public static final int BUILD_VERSION = 8;

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
}
