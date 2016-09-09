package softtech.hong.hce.engine;

import java.lang.reflect.Field;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.property.PropertyAccessor;
import org.hibernate.property.Setter;

import softtech.hong.hce.exception.HCEErrorException;
import softtech.hong.hce.utils.ReflectionUtils;

/**
 * @author Kisman Hong
 *
 */
public abstract class HCEProcessor {
	
	private final static String DELIMETER = "|";
	
	protected void setterProcess(PropertyAccessor propertyAccessor, String alias, Setter[] setters, int i, Object result, Class<?> resultClass, Object[] tuple)
		throws HCEErrorException{
		try {
			if (StringUtils.contains(alias, DELIMETER)) {
				String[] als = StringUtils.split(alias, DELIMETER);
				Field field = null;
				Object value,cacheValue;
				try {
					field = ReflectionUtils.findField(result.getClass(), als[0]);
					field.setAccessible(true);
					value = field.get(result);
					if (value == null) {
						field.set(result, field.getType().newInstance());
						value = field.get(result);
					}

					if (als.length > 2) {
						for (int j = 1; j < als.length - 1; j++) {
							field = ReflectionUtils.findField(field.getType(), als[j]); /* get the field from alias, detect through parent class too */
							field.setAccessible(true);
							cacheValue = value;
							value = field.get(value);
							if (value == null) {
								field.set(cacheValue, field.getType().newInstance());
								value = field.get(cacheValue);
							}
						}
					}
					setters[i] = propertyAccessor.getSetter(field.getType(), als[als.length - 1]);
					setters[i].set(value, tuple[i], null);

				} catch (SecurityException e) {
					throw new HCEErrorException("Could not access field : " + field.getName());
				} 
			} else {
				setters[i] = propertyAccessor.getSetter(resultClass, alias);
				setters[i].set(result, tuple[i], null);
			}
		} catch (Exception e) {
			throw new HCEErrorException(e);
		}
	}
}
