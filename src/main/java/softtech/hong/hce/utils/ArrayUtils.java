package softtech.hong.hce.utils;

import org.apache.commons.lang3.StringUtils;

/**
 * @author Kisman Hong
 * helping array manipulation
 */
public class ArrayUtils {
	/**
	 * sum value of array (int based)
	 * @param values
	 * @return int
	 */
	public static int sumArray(int[] values)
	{
		int sum =0;
		for (int i=0; i < values.length; i++)
			sum+=values[i]; 
		return sum;
	}
	
	/**
	 * sum value of two arrays (int based)
	 * @param values
	 * @param untill
	 * @return int
	 */
	public static int sumArray(int[] values, int untill)
	{
		int sum =0;
		for (int i=0; i < values.length; i++)
		{
			sum+=values[i]; 
			if(i == untill)
				break;
		}
		return sum;
	}
	
	/**
	 * sum value of array (double based)
	 * @param arg0
	 * @return double
	 */
	public static double sum(double[] arg0){
		double result=0;
		for (double d : arg0) {
			result += d;
		}
		return result;
	}
	
	/**
	 * convert from array of string to array of double
	 * @param arg0
	 * @return
	 */
	public static double[] convertToDouble(String[] arg0){
		double[] result = new double[arg0.length];
		for(int i=0; i < arg0.length; i++){
			result[i] = Double.valueOf(arg0[i]);
		}
		return result;
	}
	
	/**
	 * find a value string by index of
	 * @param array
	 * @param valueToFind
	 * @return
	 */
	public static boolean indexOfString(String[] array, String valueToFind){
		for (String content : array) {
			if(StringUtils.indexOf(content, valueToFind) > -1){
				return true;
			}
		}
		return false;
	}
}
