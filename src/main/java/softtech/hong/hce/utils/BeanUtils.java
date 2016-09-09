package softtech.hong.hce.utils;

import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import softtech.hong.hce.constant.HCEConstant;

/**
 * @author kismanhong
 *
 */
public class BeanUtils {
	private static String[] byPassFields = { "id", "createdBy", "createdDate",
			"updatedBy", "updatedDate", "class" };
	
//	private static Class<?>[] javaClass = {Integer.class, Double.class, String.class, Float.class, Date.class };
	
	/**
	 * field / property to by pass
	 */
	protected static final String[] byPass = {"active", "number", "viewable", "editable", "removed", "serialVersionUID", "createdBy", "createdDate"};
	
	/**
	 * array of java type variable
	 */
	protected static final String[] javaTypes = {"double","int","long", "float","class java.lang.String", "class java.lang.Integer", 
										"class java.lang.Float", "class java.lang.Double", "class java.lang.Long", "class java.util.Date",
										"class java.sql.Time", "boolean", "class java.lang.Boolean"};

	/**
	 * @param source -> object source to be transfered
	 * @param target -> object target to get transfered
	 * @param byPassFieldNames -> name of field to be passed
	 * @return String
	 * @throws Exception
	 */
	public static String transfer(Object source, Object target, String[] byPassFieldNames) throws Exception{
		
		PropertyDescriptor[] targetProperties = null;
		targetProperties = Introspector.getBeanInfo(target.getClass()).getPropertyDescriptors();

		StringBuffer results = new StringBuffer();
		Object transferValue ;
		Method read, write;
		if(byPassFieldNames == null)
			byPassFieldNames = new String[]{};

		PropertyDescriptor propertyDescriptor; 
		for (int t = 0; t < targetProperties.length; ++t) {
			String name = targetProperties[t].getName();
			
				if (!ArrayUtils.contains(byPassFields, name) && !ArrayUtils.contains(byPassFieldNames, name)) {
					//System.out.println("field name :" + name);
					propertyDescriptor = new PropertyDescriptor(name, source.getClass());
							read = propertyDescriptor.getReadMethod();
							write = targetProperties[t].getWriteMethod();
							if (read != null && write != null){
								transferValue = read.invoke(source);
								write.invoke(target, transferValue);
	
								results.append("Transferred property ").append(name).append(" = ").append(transferValue).append(".").append("\r\n");
							}
				}
			
		}

		return results.toString();

	}
	
	
	/**
	 * @param source -> object source to be transfered
	 * @param target -> object target to get transfered
	 * @param byPassFieldNames -> name of field to be passed
	 * @return String
	 * @throws Exception
	 */
	public static String transferDifferentObject(Object source, Object target,
			String[] byPassFieldNames) throws Exception{
		
		PropertyDescriptor[] sourceProperties = null;
		PropertyDescriptor[] targetProperties = null;
		sourceProperties = Introspector.getBeanInfo(source.getClass()).getPropertyDescriptors();
		targetProperties = Introspector.getBeanInfo(target.getClass()).getPropertyDescriptors();

		StringBuffer results = new StringBuffer();
		Object transferValue ;
		if(byPassFieldNames == null)
			byPassFieldNames = new String[]{};

		for (int s = 0; s < sourceProperties.length; ++s) {
			
			String name = sourceProperties[s].getName();
			System.out.println("Source Name: " + name);
				if (!ArrayUtils.contains(byPassFields, name) && !ArrayUtils.contains(byPassFieldNames, name)) {
					Target: for (int t = 0; t < targetProperties.length;){
						System.out.println("Target Name: " + targetProperties[t].getName());
						if (targetProperties[t].getName().equals(name)) {
							Method read = sourceProperties[s].getReadMethod();
							Method write = targetProperties[t].getWriteMethod();
							if (read == null || write == null){
								break Target;
							}
								transferValue = read.invoke(source, new Object());
								write.invoke(target, transferValue);
	
								results.append("Transferred property ").append(name).append(" = ")
										.append(transferValue).append(".").append("\r\n");
							}
							break Target;
					}
				}
			
		}
		return results.toString();

	}
	
	/**
	 * @param object -> object to be printed (it's value)
	 * @return String
	 * @throws Exception
	 */
	public static String reflectionToString(Object object) throws Exception{
		StringBuffer results = new StringBuffer();
		Field[] innerFields = object.getClass().getDeclaredFields();
			for (Field field : innerFields) {
				field.setAccessible(true);
				String fieldName = field.getName();
				Class<?> fieldType = field.getType();
				Object fieldValue = field.get(object);
					if(isJavaType(fieldType)) {					
						results.append(fieldName).append("=").append(fieldValue==null?"":fieldValue.toString()).append("; ");
					}else{
						listRecursively(fieldValue, results);
					}
			}
		return results.toString();
	}
	
	private static StringBuffer concatField = new StringBuffer();
	
	/**
	 * @param object
	 * @param results
	 * @throws Exception
	 */
	private static void listRecursively(Object object, StringBuffer results) throws Exception  {
		Field[] innerFields = object.getClass().getDeclaredFields();
		for (Field field : innerFields) {
			field.setAccessible(true);
			String fieldName = field.getName();
			Class<?> fieldType = field.getType();
			Object fieldValue = field.get(object);
			
			if(!ArrayUtils.contains(byPass, fieldName)){
				concatField.append(fieldName);
				if(isJavaType(fieldType)) {		
					if(fieldValue != null){
						results.append(concatField).append("=").append(fieldValue==null?"":fieldValue.toString()).append("; ");
					}
					concatField = new StringBuffer();
				}else{ 
					if(fieldValue != null){
						Field idField;
						Field codeField;
						
						try {
							idField = fieldValue.getClass().getDeclaredField("id");
							codeField = fieldValue.getClass().getDeclaredField("code");
							
							if( idField.get(fieldValue) != null){			
								results.append(concatField.append(".id")).append("=").append(idField.get(fieldValue).toString()).append("; ");
								concatField = new StringBuffer();
							}else if(codeField.get(fieldValue) != null){
								results.append(concatField.append(".code")).append("=").append(codeField.get(fieldValue).toString()).append("; ");
								concatField = new StringBuffer();
							}else{
								listRecursively(fieldValue, results);
							}
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}
			}
		}
	}
	
	/**
	 * @param type
	 * @return
	 */
	public static boolean isJavaType(Class<?> type)
	{
		if(ArrayUtils.contains(javaTypes, type.toString()) || type.isEnum())
			return true;
		return false;
	}
	

	/**
	 * @param type
	 * @return
	 */
	public static boolean isJavaType(String type)
	{
		if(ArrayUtils.contains(javaTypes, type))
			return true;
		return false;
	}
	
	/**
	 * @param object
	 * @param results
	 * @param depth
	 * @param prefix
	 * @throws Exception
	 */
	private static void listRecursively(Object object, StringBuffer results, int depth, String prefix) throws Exception  {
		if(depth < 4){
			Field[] innerFields = object.getClass().getDeclaredFields();
			for (Field field : innerFields) {
				field.setAccessible(true);
				String fieldName = field.getName();
				Class<?> fieldType = field.getType();
				Object fieldValue = field.get(object);
				
				if(!ArrayUtils.contains(byPass, fieldName)){			
					if(isJavaType(fieldType)) {						
						if(fieldValue != null){
							concatField.append(prefix).append(".").append(fieldName);
							results.append(concatField).append("=").append(fieldValue.toString()).append("; ");
						}
						concatField = new StringBuffer();
					}else{ 
						if(fieldValue != null){
							Field idField = null;
							Field codeField = null;
							
							try {
								idField = fieldValue.getClass().getDeclaredField("id");
								codeField = fieldValue.getClass().getDeclaredField("code");
							} catch (Exception e) {
								//not suitable object found for log audittrail, just bypass
							}
							if(idField.get(fieldValue) != null && codeField.get(fieldValue) != null){
								concatField.append(prefix).append(".").append(fieldName);
							}else{
								if( idField.get(fieldValue) != null){
									idField.setAccessible(true);
									if(idField.get(fieldValue) != null){
										results.append(concatField.append(".id")).append("=").append(idField.get(fieldValue)).append("; ");
										concatField = new StringBuffer();
									}							
								}else if(codeField.get(fieldValue) != null){
									codeField.setAccessible(true);
									if(idField.get(fieldValue) != null){
										results.append(concatField.append(".code")).append("=").append(codeField.get(fieldValue)).append("; ");
										concatField = new StringBuffer();
									}
								}else{
									depth++;
									listRecursively(fieldValue, results, depth, prefix);
								}
							}
						}
					}
				}
			}
		}
	}
	
	/**
	 * @param object -> object to be printed (it's value)
	 * @return String
	 * @throws Exception
	 */
	public static String reflectionToStringBuilder(Object object) throws Exception{
		StringBuffer results = new StringBuffer();
				Field[] innerFields = object.getClass().getDeclaredFields();
				for (Field field : innerFields) {
					field.setAccessible(true);
					String fieldName = field.getName();
					Class<?> fieldType = field.getType();
					Object fieldValue = field.get(object);
					if(isJavaType(fieldType) && !fieldName.equals("serialVersionUID")) {					
						results.append(fieldName).append("=").append(fieldValue==null?"":fieldValue.toString()).append("; ");
					}else{
						Field idField = null, codeField = null;
						try {
							idField = fieldValue.getClass().getDeclaredField("id");
							codeField = fieldValue.getClass().getDeclaredField("code");
						} catch (Exception e) {
							//we just want to find class that have id or code
						}
						if(idField != null && codeField != null){
							Object idValue, codeValue;
							idField.setAccessible(true);
							codeField.setAccessible(true);
							idValue = idField.get(fieldValue);
							codeValue = codeField.get(fieldValue);
							
							if(idValue == null && codeValue == null){
								listRecursively(fieldValue, results, 0, fieldName);
							}else{
								if(idField != null){
									if(idField.get(fieldValue) != null && !fieldName.equals("serialVersionUID")){
										results.append(fieldName).append(".id=").append(idField.get(fieldValue)).append("; ");
									}
								}
								if(codeField != null){
									if(codeField.get(fieldValue) != null && !fieldName.equals("serialVersionUID")){
										results.append(fieldName).append(".code=").append(codeField.get(fieldValue)).append("; ");
									}
								}
							}
						}else{					
							if(idField != null){
								idField.setAccessible(true);
								if(idField.get(fieldValue) != null && !fieldName.equals("serialVersionUID")){
									results.append(fieldName).append(".id=").append(idField.get(fieldValue)).append("; ");
								}
							}
							else if(codeField != null){ // actually, this is not needed to check again...
								codeField.setAccessible(true);
								if(codeField.get(fieldValue) != null){
									results.append(fieldName).append(".code=").append(codeField.get(fieldValue)).append("; ");
								}
							}
						}
					}
				}
				
		return results.toString();

	}
	
	/**
	 * @param type
	 * @return
	 */
	public static boolean isTypeByPass(Class<?> type){
		if(HCEConstant.BY_PASS_TYPE != null){
			if(ArrayUtils.contains(HCEConstant.BY_PASS_TYPE, type)){
				return true;
			}
		}
		return false;
	}


	public static void copyFieldByField(Object src, Object dest) {
	    copyFields(src, dest, src.getClass());
	}
	 
	public static void copyFields(Object src, Object dest, Class<?> klass) {
	    Field[] fields = klass.getDeclaredFields();
	    for (Field f : fields) {
	        f.setAccessible(true);
	        copyFieldValue(src, dest, f);
	    }
	 
	    klass = klass.getSuperclass();
	    if (klass != null) {
	        copyFields(src, dest, klass);
	    }
	}
	 
	public static void copyFieldValue(Object src, Object dest, Field f) {
	    try {
	        Object value = f.get(src);
	        f.set(dest, value);
	    } catch (Exception e) {
	        throw new RuntimeException(e);
	    }
	}
	
	public static void copyDifferentObjectField(Object src, Object dest, String[] fieldName) {
		try {
			
		} catch (Exception e) {
			
		}
	}
	
	/**
	 * @param object
	 * @return
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 */
	public static boolean isNotEmpty(Object object) throws IllegalArgumentException, IllegalAccessException{
		Field[] innerFields = object.getClass().getDeclaredFields();
		for (Field field : innerFields) {
			field.setAccessible(true);
			String fieldType = field.getType().toString();
			if (BeanUtils.isJavaType(fieldType)) {
				Object fieldValue = field.get(object);
				if(fieldValue != null && !StringUtils.isBlank(fieldValue.toString()))
					return true;
			}
		}
		return false;
	}
	
	/**
	 * @param object
	 * @return
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 */
	public static boolean isEmpty(Object object) throws IllegalArgumentException, IllegalAccessException{
		Field[] innerFields = object.getClass().getDeclaredFields();
		for (Field field : innerFields) {
			field.setAccessible(true);
			String fieldType = field.getType().toString();
			if (BeanUtils.isJavaType(fieldType)) {
				Object fieldValue = field.get(object);
				if(fieldValue != null && !StringUtils.isBlank(fieldValue.toString()))
					return false;
			}
		}
		return true;
	}
	
	/**
	 * @param object
	 * @param excludeProperties
	 * @return
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 */
	public static boolean isNotEmpty(Object object, String[] excludeProperties) throws IllegalArgumentException, IllegalAccessException{
		Field[] innerFields = object.getClass().getDeclaredFields();
		for (Field field : innerFields) {
			field.setAccessible(true);
			String fieldName = field.getName();
			String fieldType = field.getType().toString();
			if (BeanUtils.isJavaType(fieldType) && !ArrayUtils.contains(excludeProperties, fieldName) && !fieldName.equals("id")) {
				Object fieldValue = field.get(object);
				if(fieldValue != null && !StringUtils.isBlank(fieldValue.toString()))
					return true;
			}
		}
		return false;
	}
	
	/**
	 * @param object
	 * @param excludeProperties
	 * @return
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 */
	public static boolean isEmpty(Object object, String[] excludeProperties) throws IllegalArgumentException, IllegalAccessException{
		Field[] innerFields = object.getClass().getDeclaredFields();
		for (Field field : innerFields) {
			field.setAccessible(true);
			String fieldName = field.getName();
			String fieldType = field.getType().toString();
			if (BeanUtils.isJavaType(fieldType) && !ArrayUtils.contains(excludeProperties, fieldName)) {
				Object fieldValue = field.get(object);
				if(fieldValue != null && !StringUtils.isBlank(fieldValue.toString()))
					return false;
			}
		}
		return true;
	}
	
	/**
	 * @param arg0
	 * @param arg1
	 * @return
	 * @throws Exception
	 */
	public static String[] getDifferent(Object arg0, Object arg1) throws Exception {
		String[] results;
		Field[] innerFields = arg0.getClass().getDeclaredFields();
		StringBuffer sb0 = new StringBuffer();
		StringBuffer sb1 = new StringBuffer();
		for (Field field : innerFields) {
			field.setAccessible(true);			
			String fieldType = field.getType().toString();
			if (BeanUtils.isJavaType(fieldType)) {
				if(field.get(arg0) != null && field.get(arg1) != null){
					if(!field.get(arg0).equals(field.get(arg1))){
						String fieldName = field.getName();
						sb0.append(fieldName + " = " + field.get(arg0) + ";");
						sb1.append(fieldName + " = " + field.get(arg1) + ";");
					}
				}
			}else{
				if(field.get(arg0) !=null && field.get(arg1) != null ){
					recursiveSearch(field.getName(), sb0, sb1, field.get(arg0), field.get(arg1));
				}
			}
		}
		results = new String[] {sb0.toString(), sb1.toString()};
		return results;
	}
	
	/**
	 * @param prefix
	 * @param stringBuffer
	 * @param stringBuffer2
	 * @param object
	 * @param object2
	 * @throws Exception
	 */
	private static void recursiveSearch(String prefix, StringBuffer stringBuffer, StringBuffer stringBuffer2, 
			Object object, Object object2) throws Exception{
		if(object != null){
			Field[] innerFields = object.getClass().getDeclaredFields();
			for (Field field : innerFields) {
				field.setAccessible(true);			
				String fieldType = field.getType().toString();
				if (BeanUtils.isJavaType(fieldType)) {
					if(field.get(object) != null && field.get(object2) != null){
						if(!field.get(object).equals(field.get(object2))){
							String fieldName = field.getName();
							stringBuffer.append( prefix + "." + fieldName + " = " + field.get(object) + ";");
							stringBuffer2.append( prefix + "." + fieldName + " = " + field.get(object2) + ";");
						}
					}
				}else{
					recursiveSearch( prefix + "." + field.getName() + ".",  stringBuffer, stringBuffer2, field.get(object), field.get(object2));
				}
			}
		}
	}
	
	/**
	 * @param source -> object source to be transfered
	 * @param target -> object target to get transfered
	 * @param byPassFieldNames -> name of field to be passed
	 * @return String
	 * @throws Exception
	 */
	public static String transferDifferentObjectField(Object source, Object target,
			String[] byPassFieldNames) throws Exception{
		
//		List<Field> sFields = new ArrayList<Field>();
		
	    
	    if(byPassFieldNames != null){
	    	for(int i = 0; i < byPassFieldNames.length; i++){
	    		String name = byPassFieldNames[i];
	    		if(name.contains(".")) {
	    			String[] splitName = name.split("\\.");
	    			
	    			Field sField = null;
	    			Field tField = null;
	    			Object sValue = null;
	    			Object parentValue = null;
	    			
	    			for(int j = 0; j < splitName.length; j++){
	    				System.out.println("splitname: " + splitName[j]);
	    				if(j == 0){
	    					sField = source.getClass().getField(splitName[j]);
	    	    			tField = target.getClass().getField(splitName[j]);
	    	    			sValue = sField.get(source);
	    	    			
	    				} else {
	    					parentValue = sValue;
	    					System.out.println("sField type: " + sField.getType());
		    				sField = sField.getType().getField(splitName[j]);
		    				tField = tField.getType().getField(splitName[j]);
		    				sValue = sField.get(sValue);
	    				}
	    				
		    			System.out.println("sValue value: " + sValue);
		    			System.out.println("Success until here!");
		    			
		    			if(j == 0){
		    				tField.set(target, sValue);
		    				System.out.println("tField value: " + tField.get(target));
		    			} else {
		    				tField.set(parentValue, sValue);
		    				System.out.println("tField value: " + tField.get(parentValue));
		    			}
	    				
//	    				sFields.add(sField);
	    			}
	    			
	    		} else {
	    			Field sField = source.getClass().getField(name);
	    			Field tField = target.getClass().getField(name);
	    			
	    			tField.set(target, sField.get(source));
	    			
//	    			sFields.add(sField);
	    		}
	    		
	    		
		    }
	    } else {
	    	Field[] sourceFields = source.getClass().getFields();
			Field[] targetFields = target.getClass().getFields();
			
//			System.out.println("=================\nSOURCE:" + source.getClass().getCanonicalName());
		    for (Field f : sourceFields) {
//		    	System.out.println("field: " + f.getName() + " is accesible: " + f.getModifiers() + "public" + f.PUBLIC + "\n");
		        f.setAccessible(true);
		    }
		    
//		    System.out.println("=================\nDESTINATION:" + target.getClass().getCanonicalName());
		    for (Field f : targetFields) {
//		    	System.out.println("field: " + f.getName() + " is accesible: " + f.getModifiers() + "public" + f.PUBLIC + "\n");
		        f.setAccessible(true);
		    }
	    	
	    	for (int s = 0; s < sourceFields.length; s++) {
				Field sField = sourceFields[s];
				String sFieldName = sField.getName();
				System.out.println("Source Name: " + sFieldName + " class: " + sField.getGenericType());
				
				//field must be public to be transfer to another class field
				if(sField.getModifiers() != 1){
					continue;
				}
				
				//if byPassFieldNames is filled then check for the same field name
//				if(byPassFieldNames != null && byPassFieldNames.length > 0){
//					if(!ArrayUtils.contains(byPassFieldNames, sFieldName)){
//						
//						//if doesn't contain the given pass field names then skip the field
//						continue;
//					}
//				}
				
				for (int t = 0; t < targetFields.length; t++){
					Field tField = targetFields[t];
					String tFieldName = tField.getName();
					System.out.println("Target Name: " + tFieldName + " class: " + tField.getGenericType());
					if (tFieldName.equals(sFieldName)) {
						System.out.println("Field name equals: " + sFieldName + " - " + tFieldName );
						Object sValue = sField.get(source); 
//						System.out.println("Source field: " + sField.getName() + " value: " + sValue);
						
						targetFields[t].set(target, sValue);
						System.out.println("Target field: " + tField.getName() + " value: " + tField.get(target));
						break;
						
					}
				}
			}
	    }
	    

		
		return "Success";

	}
	
	public static String transferDifferentObjectField(
			Object source, 
			Object target,
			Map<String, String> mapFieldNames) throws Exception{
		
		if(!mapFieldNames.isEmpty()){
			String[] key = new String[mapFieldNames.size()]; 
			key = mapFieldNames.values().toArray(key);
			
			
			
			for(int i = 0; i < key.length ; i++){
				System.out.println("Key: " + key[i]);
				
				String sName = key[i];
				String tName = mapFieldNames.get(key[i]);
				
				Field sField = null;
    			Field tField = null;
    			Object sValue = null;
    			Object tValue = null;
				
				//get the source value
				if(!sName.contains(".")){
					sField = source.getClass().getField(sName);
	    			sValue = source;
				} else {
	    			String[] splitName = sName.split("\\.");
	    			
	    			for(int j = 0; j < splitName.length; j++){
	    				System.out.println("splitname: " + splitName[j]);
	    				if(j == 0){
	    					sField = source.getClass().getField(splitName[j]);
	    	    			sValue = sField.get(source);
	    				} else {
		    				sField = sField.getType().getField(splitName[j]);
		    				sValue = sField.get(sValue);
	    				}
	    				
	    				System.out.println("sField type: " + sField.getType());
	    			}
	    			
	    		}
				
				//set the target value
				if(!tName.contains(".")){
					tField = target.getClass().getField(tName);
	    			tField.set(target, sValue);
					
				} else {
					String[] splitName = tName.split("\\.");
	    			
					for(int j = 0; j < splitName.length; j++){
	    				System.out.println("splitname: " + splitName[j]);
	    				if(j == 0){
	    	    			tField = target.getClass().getField(splitName[j]);
	    	    			tValue = tField.get(source);
	    				} else {	//between 1 or length - 1
		    				tField = tField.getType().getField(splitName[j]);
		    				tValue = sValue;
	    				}
	    				System.out.println("tField type: " + tField.getType());
	    				
	    			}
					
					tField.set(tValue, sValue);
				} 
				
			}
			
		}
		
		
		return "Success";
	}
	
}
