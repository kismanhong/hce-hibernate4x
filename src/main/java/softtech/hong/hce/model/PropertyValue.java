package softtech.hong.hce.model;

import org.apache.commons.lang3.ArrayUtils;


/**
 * @author kismanhong
 * this class is used for defining update of object's properties
 */
public class PropertyValue {
	private String propertyName;
	
	private Object value;

	public PropertyValue(){}
	
	private PropertyValue[] propertyValues;
	
	public PropertyValue(String propertyName, Object value){
		this.propertyName = propertyName;
		this.value = value;
		add(this);
	}
	
	public static PropertyValue assign(String propertyName, Object value){
		return new PropertyValue(propertyName, value);
	}

	public String getPropertyName() {
		return propertyName;
	}

	public void setPropertyName(String propertyName) {
		this.propertyName = propertyName;
	}

	public Object getValue() {
		return value;
	}

	public void setValue(Object value) {
		this.value = value;
	}

	public void add(PropertyValue propertyValue){
		this.propertyValues = (PropertyValue[]) ArrayUtils.add(this.propertyValues, propertyValue);
	}
	
	public PropertyValue[] getPropertyValues(){
		return propertyValues;
	}
}
