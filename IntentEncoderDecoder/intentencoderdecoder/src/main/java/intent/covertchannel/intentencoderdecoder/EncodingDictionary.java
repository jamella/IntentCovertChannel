package intent.covertchannel.intentencoderdecoder;

/**
 * Represents a set of encodable characters.
 *
 * @author Timothy Heard
 */
public interface EncodingDictionary
{
	public int getCodeForValue(String str, int buildVersion);
	
	public String getValue(int value, int buildVersion);

	boolean isSupportedChar(char c, int buildVersion);
	
	boolean isValidValue(int charCode, int buildVersion);
	
	int getDictionarySize(int buildVersion);

    boolean isSupportedString(String str, int buildVersion);
}
