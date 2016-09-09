package softtech.hong.hce.utils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import softtech.hong.hce.exception.HCEErrorException;

public class ReflectionUtils {
	
	/**
	 * check if a field is Public Static
	 * @param field
	 * @return true or false
	 */
	public static boolean isPublicStaticFinal(Field field) {
	    int modifiers = field.getModifiers();
	    return (Modifier.isPublic(modifiers) && Modifier.isStatic(modifiers) && Modifier.isFinal(modifiers));
	 }
	
	/**
	 * check if a field is Public Static Final
	 * @param field
	 * @return true or false
	 */
	public static boolean isPrivateStaticFinal(Field field) {
	    int modifiers = field.getModifiers();
	    return (Modifier.isPrivate(modifiers) && Modifier.isStatic(modifiers) && Modifier.isFinal(modifiers));
	}
	
	/**
	 * check if a field is Static Final
	 * @param field
	 * @return true or false
	 */
	public static boolean isStaticFinal(Field field){
		int modifiers = field.getModifiers();
	    return (Modifier.isStatic(modifiers) && Modifier.isFinal(modifiers));
	}
	
	/**
	 * check if a field is Static
	 * @param field
	 * @return true or false
	 */
	public static boolean isStatic(Field field){
		int modifiers = field.getModifiers();
	    return (Modifier.isStatic(modifiers));
	}

	/**
	 * get fields from class and super class
	 * @param type
	 * @return array of fields
	 */
	public static Field[] getInheritedPrivateFields(Class<?> type) {
	    List<Field> result = new ArrayList<Field>();

	    Class<?> i = type;
	    while (i != null && i != Object.class) {
	        for (Field field : i.getDeclaredFields()) {
	            if (!field.isSynthetic()) {
	                result.add(field);
	            }
	        }
	        i = i.getSuperclass();
	    }
	    return (Field[]) result.toArray(new Field[result.size()]);
//	    return result;
	}

	
	/**
	 * finding field from class or it's super class
	 * @param clazz
	 * @param name
	 * @param type
	 * @return
	 * @throws HCEErrorException
	 */
	public static Field findField(Class<?> clazz, String name, Class<?> type) throws HCEErrorException{
//		Assert.notNull(clazz, "Class must not be null");
//		Assert.isTrue(name != null || type != null, "Either name or type of the field must be specified");
		Class<?> searchType = clazz;
		while (!Object.class.equals(searchType) && searchType != null) {
			Field[] fields = searchType.getDeclaredFields();
			for (int i = 0; i < fields.length; i++) {
				Field field = fields[i];
				if ((name == null || name.equals(field.getName()))
						&& (type == null || type.equals(field.getType()))) {
					return field;
				}
			}
			searchType = searchType.getSuperclass();
		}
//		return null;
		throw new HCEErrorException("cannot find field :"+name+" from class :"+clazz.getCanonicalName());
	}
	
	/**
	 * find field from a class
	 * @param clazz
	 * @param name
	 * @return
	 * @throws HCEErrorException
	 */
	public static Field findField(Class<?> clazz, String name) throws HCEErrorException{
		return findField(clazz, name, null);
	}
	
	/**
	 * Attempt to find a {@link Method} on the supplied class with the supplied name
	 * and no parameters. Searches all superclasses up to <code>Object</code>.
	 * <p>Returns <code>null</code> if no {@link Method} can be found.
	 * @param clazz the class to introspect
	 * @param name the name of the method
	 * @return the Method object, or <code>null</code> if none found
	 */
	public static Method findMethod(Class<?> clazz, String name) throws HCEErrorException {
		return findMethod(clazz, name, new Class[0]);
	}

	/**
	 * Attempt to find a {@link Method} on the supplied class with the supplied name
	 * and parameter types. Searches all superclasses up to <code>Object</code>.
	 * <p>Returns <code>null</code> if no {@link Method} can be found.
	 * @param clazz the class to introspect
	 * @param name the name of the method
	 * @param paramTypes the parameter types of the method
	 * (may be <code>null</code> to indicate any signature)
	 * @return the Method object, or <code>null</code> if none found
	 */
	public static Method findMethod(Class<?> clazz, String name, Class<?>[] paramTypes) throws HCEErrorException {
//		Assert.notNull(clazz, "Class must not be null");
//		Assert.notNull(name, "Method name must not be null");
		Class<?> searchType = clazz;
		while (!Object.class.equals(searchType) && searchType != null) {
			Method[] methods = (searchType.isInterface() ? searchType.getMethods() : searchType.getDeclaredMethods());
			for (int i = 0; i < methods.length; i++) {
				Method method = methods[i];
				if (name.equals(method.getName()) &&
						(paramTypes == null || Arrays.equals(paramTypes, method.getParameterTypes()))) {
					return method;
				}
			}
			searchType = searchType.getSuperclass();
		}
		throw new HCEErrorException("cannot find method :"+name+" from class :"+clazz.getCanonicalName());
	}
	
	/**
	 * Invoke the given callback on all fields in the target class,
	 * going up the class hierarchy to get all declared fields.
	 * @param targetClass the target class to analyze
	 * @param fc the callback to invoke for each field
	 * @param ff the filter that determines the fields to apply the callback to
	 */
	public static void doWithFields(Class<?> targetClass, FieldCallback fc, FieldFilter ff)
			throws IllegalArgumentException {

		// Keep backing up the inheritance hierarchy.
		do {
			// Copy each field declared on this class unless it's static or file.
			Field[] fields = targetClass.getDeclaredFields();
			for (int i = 0; i < fields.length; i++) {
				// Skip static and final fields.
				if (ff != null && !ff.matches(fields[i])) {
					continue;
				}
				try {
					fc.doWith(fields[i]);
				}
				catch (IllegalAccessException ex) {
					throw new IllegalStateException(
							"Shouldn't be illegal to access field '" + fields[i].getName() + "': " + ex);
				}
			}
			targetClass = targetClass.getSuperclass();
		}
		while (targetClass != null && targetClass != Object.class);
	}
	
	/**
	 * Callback interface invoked on each field in the hierarchy.
	 */
	public static interface FieldCallback {

		/**
		 * Perform an operation using the given field.
		 * @param field the field to operate on
		 */
		void doWith(Field field) throws IllegalArgumentException, IllegalAccessException;
	}


	/**
	 * Callback optionally used to filter fields to be operated on by a field callback.
	 */
	public static interface FieldFilter {

		/**
		 * Determine whether the given field matches.
		 * @param field the field to check
		 */
		boolean matches(Field field);
	}
	
	@SuppressWarnings("rawtypes")
	public static boolean isCollection(Class type){
		return Collection.class.isAssignableFrom(type);
	}

}
