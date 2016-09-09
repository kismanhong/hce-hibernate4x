package softtech.hong.hce.constant;

import java.util.HashMap;
import java.util.Map;

/**
 * @author kismanhong
 * constant that store entity information to Map
 */
public class HCEConstant {
	
	/**
	 * static variable that store table name, can be accessed by using canonical class name as key
	 */
	public static Map<String, String> ENTITY_TO_TABLE = new HashMap<String, String>();
	
	/**
	 * static variable that store identifier of entity, can be accessed by using canonical class name as key
	 */
	public static Map<String, String> ENTITY_IDENTIFIER_PROPERTY = new HashMap<String, String>();
		
	/**
	 * primary column name
	 */
	public static Map<String, String[]> ENTITY_IDENTIFIER_PRIMARY_COLUMN = new HashMap<String, String[]>();
	
	/**
	 * hibernate dialect
	 */
	public static DialectType DIALECT_TYPE;
	
	/**
	 * type to be by pass when restrictions
	 */
	public static Class<?>[] BY_PASS_TYPE;
}
