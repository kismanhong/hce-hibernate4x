package softtech.hong.hce.utils;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import softtech.hong.hce.exception.HCEErrorException;

/**
 * @author kismanhong
 * 
 */
public class PersisterUtils {
	
	private static PersisterUtils instance = null;

	public static synchronized PersisterUtils getInstance() {
		if (instance == null) {
			instance = new PersisterUtils();
		}
		return instance;
	}
	
	/**
	 * @param resultClass
	 * @param aliases
	 * @return
	 * @throws Exception
	 */
	public PersisterObject createObjectStack(Class<?> resultClass, String[] aliases ) throws Exception{
		HashMap<String, Object> mapStack = new HashMap<String, Object>();	
		boolean isCollection = false;
		mapStack.put("this", resultClass.newInstance());	
		for (String alias : aliases) {
			String[] properties = StringUtils.split(alias, "|");
			String[] anotherProperties = StringUtils.split(alias, "|");
			if(properties.length > 1){
				String key = StringUtils.join(ArrayUtils.remove(anotherProperties, anotherProperties.length - 1), "|");
				if(!mapStack.containsKey(key)){
					Field field = null;
					Class<?> innerClass = resultClass;
					for(int i=0; i < (properties.length - 1); i++){
						field = ReflectionUtils.findField(innerClass, properties[i]);
//						if(StringUtils.contains(field.getType().toString(), "List") || StringUtils.contains(field.getType().toString(), "Set") || 
//								StringUtils.contains(field.getType().toString(), "Map") ){			
						
						if(Collection.class.isAssignableFrom(field.getType()) || Map.class.isAssignableFrom(field.getType())) {						
							Type type = field.getGenericType();  
					        if (type instanceof ParameterizedType) {  
					            ParameterizedType pt = (ParameterizedType) type;  
					            innerClass = (Class<?>) pt.getActualTypeArguments()[0];  	
					        }else{
					        	throw new HCEErrorException("The parameter type of your collection mapping must be declared, class : "+innerClass.getCanonicalName() + 
					        			" field : "+field.getName());
					        }
					        isCollection = true;
						}else{
							innerClass = field.getType();
						}
					}
					mapStack.put(key, innerClass.newInstance());
				}
			}
		}	
		return new PersisterObject(isCollection, mapStack);
	}
	
	/**
	 * @param aliases
	 * @return
	 * @throws Exception
	 */
	public static String[] distinctAlias(String[] aliases) throws Exception {
		String[] results = {};
		for (String alias : aliases) {
			String[] properties = StringUtils.split(alias, "|");
			String[] anotherProperties = StringUtils.split(alias, "|");
			if(properties.length > 1){
				String key = StringUtils.join(ArrayUtils.remove(anotherProperties, anotherProperties.length - 1), "|");
				if(!ArrayUtils.contains(results, key)){
					results = (String[]) ArrayUtils.add(results, key);				
				}
			}
		}	
		return results;
	}

	/**
	 * @param collections
	 * @param identityObject
	 * @param identityProperty
	 * @return
	 * @throws Exception
	 */
	public static Object findObjectFromList(List<?> collections, Object identityObject, String identityProperty) throws Exception{
		if(identityObject != null){
			Field identityField;
			Object object;
			for(int i=0; i<collections.size(); i++ ){
				object = collections.get(i);
				identityField = ReflectionUtils.findField(object.getClass(), identityProperty);
				identityField.setAccessible(true);
				if(identityField.get(object).equals(identityObject)){
					return object;
				}
			}
		}
		return null;
	}
	
	/**
	 * @param collections
	 * @param identityObject
	 * @param identityProperty
	 * @return
	 * @throws Exception
	 */
	@SuppressWarnings("rawtypes")
	public static Object findObjectFromSet(Set<?> collections, Object identityObject, String identityProperty) throws Exception{
		if(identityObject != null){
			Field identityField;
			Object object;
			Iterator it = collections.iterator();
	        while (it.hasNext()) {
	            object = it.next();
	            identityField = ReflectionUtils.findField(object.getClass(), identityProperty);
				identityField.setAccessible(true);
				if(identityField.get(object).equals(identityObject)){
					return object;
				}
	        } 
		}
		return null;
	}
	
	/**
	 * @param collections
	 * @param identityObject
	 * @param identityProperty
	 * @return
	 * @throws Exception
	 */
	@SuppressWarnings("rawtypes")
	public static Object findObjectFromMap(Map<?,?> collections, Object identityObject) throws Exception{
		if(identityObject != null){
	        Iterator it = collections.entrySet().iterator();
		    while (it.hasNext()) {
		        Map.Entry pairs = (Map.Entry)it.next();
		        String key = pairs.getKey().toString();
		        if(identityObject.toString().equals(key)){
		        	return pairs.getValue();
		        }
		    }
		}
		return null;
	}
	
//	public static void main(String[] args) throws Exception {
////		String[] aliases = new String[]{"id", "name", "url", "submenus|id", "submenus|name", 
////				"submenus|url","submenus|image", "submenus|subMenus|id", "submenus|subMenus|name"};
////		PersisterObject persisterObject = PersisterUtils.getInstance().createObjectStack(Menu.class, aliases);
////		Map<String, Object> map = persisterObject.getMapStack();
////		 Iterator<?> it = map.entrySet().iterator();
////		    while (it.hasNext()) {
////		        Map.Entry pairs = (Map.Entry)it.next();
////		        String key = pairs.getKey().toString();
////			    System.out.println("Class Name : "+pairs.getValue().getClass().getSimpleName() + " Key : "+key);
////		    }
//	}
	
	
	public class PersisterObject implements Cloneable{
		private boolean isCollection;
		private HashMap<String, Object> mapStack;
		
		public Object clone() {
	        try {
	            return super.clone();
	        }
	        catch (CloneNotSupportedException e) {
	            // This should never happen
	            throw new InternalError(e.toString());
	        }
	    }
		
		public PersisterObject(boolean isCollection, HashMap<String, Object> mapStack){
			this.isCollection = isCollection;
			this.mapStack = mapStack;
		}

		public boolean isCollection() {
			return isCollection;
		}

		public void setCollection(boolean isCollection) {
			this.isCollection = isCollection;
		}

		public HashMap<String, Object> getMapStack() {
			return mapStack;
		}

		public void setMapStack(HashMap<String, Object> mapStack) {
			this.mapStack = mapStack;
		}
	}
}
