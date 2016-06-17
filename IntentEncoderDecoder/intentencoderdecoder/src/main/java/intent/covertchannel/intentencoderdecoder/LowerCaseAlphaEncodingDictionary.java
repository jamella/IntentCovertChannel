package intent.covertchannel.intentencoderdecoder;

/**
 * Represents the encoding for all of the lower case letters from 'a' to 'z' as
 * well as single spaces. 
 * 
 * @author Timothy Heard
 */
public class LowerCaseAlphaEncodingDictionary implements EncodingDictionary
{
	// The number of lower case characters plus the space character
	private static final int DICTIONARY_SIZE = 27;
	
	// The space character is the last character code supported by this 
	// dictionary, and character codes start at zero, which is why the code for
	// the space character is 26.
	private static final int SPACE_CODE = DICTIONARY_SIZE -1;
	
	@Override
	public int getCodeForValue(String str, int buildVersion) {
        char c = str.toCharArray()[0];

		if(c == ' ') {
			return SPACE_CODE;
		}
		
		return (c - 'a');
	}
	
	@Override
	public String getValue(int charCode, int buildVersion) throws IllegalArgumentException {
		if(!isValidValue(charCode, buildVersion)) {
			throw new IllegalArgumentException("In " + this.getClass().getName() + ".getChar(): character code " + charCode + " is not valid.");
		}
	
		if(charCode == SPACE_CODE) {
			return " ";
		}
		
		char c = (char) (charCode + 'a');
        return String.valueOf(c);
	}

	@Override
	public boolean isValidValue(int charCode, int buildVersion)
	{
		return (charCode < DICTIONARY_SIZE) && (charCode >= 0);
	}
	
	@Override
	public boolean isSupportedChar(char c, int buildVersion) {
		return (c == ' ') || (c <= 'z') && (c >= 'a');
	}

    @Override
	public int getDictionarySize(int buildVersion) {
	    return DICTIONARY_SIZE;
    }

    @Override
    public boolean isSupportedString(String str, int buildVersion) {
        char c = str.toCharArray()[0];
        return isSupportedChar(c, buildVersion);
    }
}
