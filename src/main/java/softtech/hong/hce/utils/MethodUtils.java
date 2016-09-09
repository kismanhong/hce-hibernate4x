package softtech.hong.hce.utils;

import java.lang.reflect.Method;

import org.apache.commons.lang3.StringUtils;

/**
 * @author Kisman Hong
 *
 */
public class MethodUtils {
	private static final String GET = "get";
	private static final String SET = "set";
	private static final String CLASS = "Class";

	public static boolean isGetter(Method method) {
		if (method.getName().startsWith(GET) && !method.getName().endsWith(CLASS))
			return true;
		return false;
	}
	
	/**
	 * @param name
	 * @return String
	 * get field name from getter method
	 */
	public static String getFieldName(String name) {
		return StringUtils.uncapitalize(StringUtils.removeStart(name, GET));
	}
	
	/**
	 * @param name
	 * @return String
	 * get setter method name
	 */
	public static String getSetMethodName(String name) {
		return SET + StringUtils.capitalize(name);
	}
	
	/**
	 * @param name
	 * @return String
	 * get getter method name
	 */
	public static String getGetMethodName(String name) {
		return GET + StringUtils.capitalize(name);
	}
}
