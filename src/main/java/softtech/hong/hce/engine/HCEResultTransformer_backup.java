package softtech.hong.hce.engine;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.property.ChainedPropertyAccessor;
import org.hibernate.property.PropertyAccessor;
import org.hibernate.property.PropertyAccessorFactory;
import org.hibernate.property.Setter;
import org.hibernate.transform.ResultTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import softtech.hong.hce.constant.HCEConstant;
import softtech.hong.hce.exception.HCEErrorException;
import softtech.hong.hce.utils.MethodUtils;
import softtech.hong.hce.utils.PersisterUtils;
import softtech.hong.hce.utils.ReflectionUtils;


/**
 * @author Kisman Hong
 * Hibernate cannot automatically set the field
 *         value of field, this is used to handler it, eg.: if you have a
 *         pojo class : public class Person(){ ... private School school; ... }
 * 
 *         when you add projection "school.name", hibernate cannot automatically
 *         set name value to school object class.
 *         this transformer is used for solving the limitation
 */
public class HCEResultTransformer_backup implements ResultTransformer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -264194164964958304L;
	private static Logger log = LoggerFactory.getLogger(HCEResultTransformer_backup.class);
	private final Class<?> resultClass;
	private Setter[] setters;
	private PropertyAccessor propertyAccessor;
	private boolean isCollectionFetched = false;
	private final static String DELIMETER = "|";
//	private final static String[] COLLECTION_TYPES = {"List", "Set", "Map"};
	private PersisterUtils.PersisterObject persisterObject;
	@SuppressWarnings("rawtypes")
	private List results = new ArrayList();
	private int index = 0;
	private String[] distinctAlias;

	/**
	 * @param resultClass -> result class from fetching
	 */
	public HCEResultTransformer_backup(Class<?> resultClass) {
		if (resultClass == null) 
			throw new HCEErrorException("resultClass cannot be null");
		this.resultClass = resultClass;
		propertyAccessor = new ChainedPropertyAccessor(new PropertyAccessor[] { PropertyAccessorFactory.getPropertyAccessor(resultClass, null),
				PropertyAccessorFactory.getPropertyAccessor("field") });
	}

	/* (non-Javadoc)
	 * @see org.hibernate.transform.ResultTransformer#transformTuple(java.lang.Object[], java.lang.String[])
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Object transformTuple(Object[] tuple, String[] aliases) {
		Object result;
		try {
			result = resultClass.newInstance();
			if(persisterObject == null){
				try {
					persisterObject = PersisterUtils.getInstance().createObjectStack(resultClass, aliases);
				} catch (Exception e) {
					log.error("Cannot find alias from class : {}, actual message : {}", resultClass.getClass().getCanonicalName(),
							e.getMessage() + "caused by : " + e.getCause());
					throw new HCEErrorException("Cannot find alias from class : "+resultClass.getClass().getCanonicalName()
							+", actual message : " + e.getMessage() + "caused by : " + e.getCause());
				}
				isCollectionFetched = persisterObject.isCollection();
			}
			if (setters == null) {
				setters = new Setter[aliases.length];
				if (!persisterObject.isCollection()) {
					for (int i = 0; i < aliases.length; i++) {
						String alias = aliases[i];
						if (alias != null) {
							if (StringUtils.contains(alias, DELIMETER)) {
								String[] als = StringUtils.split(alias, DELIMETER);
								Field field = null;
								Object value;
								Object cacheValue;
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
						}
					}
				} else {
					HashMap<String, Object> mapStack = persisterObject.getMapStack();
					HashMap<String, Object> tempMap = new HashMap<String, Object>();
					Iterator iterator = mapStack.entrySet().iterator();
					// cloning object of Map, prevent call this PersisterUtils.getInstance().createObjectStack(resultClass, aliases); many times
				    while (iterator.hasNext()) {
				        Map.Entry pairs = (Map.Entry)iterator.next();
				        tempMap.put(pairs.getKey().toString(), pairs.getValue().getClass().newInstance()); 
				    }

					for (int i = 0; i < aliases.length; i++) {
						String[] als = StringUtils.split(aliases[i], DELIMETER);
						if (als != null) {
							String fieldName, key;
							if(als.length > 1){
								fieldName = als[als.length - 1];
								key = StringUtils.join(ArrayUtils.remove(als, als.length - 1), "|");
							}else{
								fieldName = als[0];
								key = "this";
							}
							
							Object obj = tempMap.get(key);
							Field field = null;
							try {
								field = ReflectionUtils.findField(obj.getClass(), fieldName);
								field.setAccessible(true);
								if (tuple[i] != null) {
									field.set(obj, tuple[i]);
								}
							} catch (Exception e) {
								log.error("Cannot find field " + fieldName + " from " + field.getClass().getSimpleName() + " caused by :" + e.getMessage());
								throw new HCEErrorException("Cannot find field " + fieldName + " from " + field.getClass().getSimpleName() + " caused by :" + e.getMessage());
							}
						
							
						/*	if (als.length > 1) {
								String fieldName = als[als.length - 1];
								String key = StringUtils.join(ArrayUtils.remove(als, als.length - 1), "|");
								Field field = null;
								Object obj = tempMap.get(key);
								try {
									field = ReflectionUtils.findField(obj.getClass(), fieldName);
								} catch (Exception e) {
									log.error("Cannot find field " + als[als.length] + " from " + field.getClass().getSimpleName()
											+ " caused by :" + e.getMessage());
									throw new HCEErrorException("Cannot find field " + als[als.length] + " from "
													+ field.getClass().getSimpleName() + " caused by :" + e.getMessage());
								}
								field.setAccessible(true);
								if (tuple[i] != null) {
									field.set(obj, tuple[i]);
								}
							} else {
								Object obj = tempMap.get("this");
								Field field = null;
								try {
									field = ReflectionUtils.findField(obj.getClass(), als[0]);
								} catch (Exception e) {
									log.error("Cannot find field " + als[0] + " from " + field.getClass().getSimpleName() 
											+ " caused by :" + e.getMessage());
									throw new HCEErrorException("Cannot find field " + als[0] + " from "
													+ field.getClass().getSimpleName() + " caused by :" + e.getMessage());
								}
								try {
									field.setAccessible(true);
									if (tuple[i] != null) {
										field.set(obj, tuple[i]);
									}
								} catch (IllegalArgumentException e) {
									log.error("Cannot set field " + als[0] + " to " + obj.getClass().getSimpleName() + " caused by :" + e.getMessage());
									throw new HCEErrorException("Cannot set field " + als[0] + " to " + obj.getClass().getSimpleName()
										+ " caused by :" + e.getMessage());
								}
							} */
						}
					}
					Object targetObj = null;
					Object sourceObj = tempMap.get("this");
					Object mainObj = null;
					Field idField = null;
					String identityProperty = HCEConstant.ENTITY_IDENTIFIER_PROPERTY.get(sourceObj.getClass().getCanonicalName());
					try {
						idField = ReflectionUtils.findField(sourceObj.getClass(), identityProperty);
						idField.setAccessible(true);
					} catch (Exception e) {
						log.error("Cannot find Identity Property from :" + sourceObj.getClass().getCanonicalName());
					}
					Object idValue = idField.get(sourceObj);
					if (idValue != null) {
						Object object;
						try {
							object = PersisterUtils.findObjectFromList(results, idValue, identityProperty);
						} catch (Exception e) {
							log.error("Error happened when trying to get Object from collections" + " caused by :" + e.getMessage());
							throw new HCEErrorException("Error happened when trying to get Object from collections" + " caused by :" + e.getMessage());
						}
						if (object != null) {
							targetObj = object;
							mainObj = object;
						} else {
							targetObj = tempMap.get("this");
							results.add(targetObj);
							targetObj = results.get(index);
							mainObj = targetObj;
							index++;
						}
					} else {
						int exists = results.indexOf(sourceObj);
						if (exists > -1) {
							targetObj = results.get(exists);
							mainObj = targetObj;
						} else {
							targetObj = tempMap.get("this");
							results.add(targetObj);
							targetObj = results.get(index);
							mainObj = targetObj;
							index++;
						}
					}

					try {
						if (distinctAlias == null) {
							distinctAlias = PersisterUtils.distinctAlias(aliases);
						}
					} catch (Exception e1) {
						log.error("Error happened when trying to distinct alias " + " caused by :" + e1.getMessage());
						throw new HCEErrorException("Error happened when trying to distinct alias  caused by :" + e1.getMessage());
					}
					for (String mapKey : distinctAlias) {
						String key = (String) mapKey;
						if (!key.equals("this")) {
							Object obj = tempMap.get(key);
							String[] keys = StringUtils.split(key, DELIMETER);
							String targetKey = StringUtils.join(ArrayUtils.remove(keys, keys.length - 1), "|");
							Field field = null;
							int lastIndex = keys.length - 1;
							for (int i = 0; i < keys.length; i++) {
								try {
									if (!(targetObj instanceof Collection<?>)) {
										field = ReflectionUtils.findField(targetObj.getClass(), keys[i]);
										field.setAccessible(true);
										Method method;
										method = ReflectionUtils.findMethod(targetObj.getClass(), MethodUtils.getGetMethodName(keys[i]));
										Object type = method.invoke(targetObj);
										if (type == null) {
											collMethodInvoke(field, targetObj, keys, i);
//											Method methodSet;
//											if (StringUtils.contains(field.getType().toString(), "List")) {
//												methodSet = ReflectionUtils.findMethod(targetObj.getClass(), MethodUtils.getSetMethodName(keys[i]), new Class<?>[]{List.class});
//												methodSet.invoke(targetObj, new ArrayList());
//											} else if (StringUtils.contains(field.getType().toString(), "Set")) {
//												methodSet = ReflectionUtils.findMethod(targetObj.getClass(), MethodUtils.getSetMethodName(keys[i]), new Class<?>[]{Set.class});
//												methodSet.invoke(targetObj, new HashSet());
//											} else if (StringUtils.contains(field.getType().toString(), "Map")) {
//												methodSet = ReflectionUtils.findMethod(targetObj.getClass(), MethodUtils.getSetMethodName(keys[i]), new Class<?>[]{Map.class});
//												methodSet.invoke(targetObj, new HashMap());
//											}
										} else {
											if (type instanceof Collection<?>) {
												Object parentObj = tempMap.get(join(i, keys));
												collectionsProcess(type, parentObj);
																							
//												String identityProp = HCEConstant.ENTITY_IDENTIFIER_PROPERTY.get(
//														parentObj.getClass().getCanonicalName());
//												Field fieldOfProp;
//												fieldOfProp = ReflectionUtils.findField(parentObj.getClass(), identityProp);
//												fieldOfProp.setAccessible(true);
//												Object identityObj = fieldOfProp.get(parentObj);
//												if (type instanceof List<?>) {
//													Object object = PersisterUtils.findObjectFromList((List) type, identityObj, identityProp);
//													if (object == null && identityObj != null) {
//														((List) type).add(parentObj);
//													}
//												} else if (type instanceof Set<?>) {
//													Object object = PersisterUtils.findObjectFromSet((Set) type, identityObj, identityProp);
//													if (object == null && identityObj != null) {
//														((Set) type).add(parentObj);
//													}
//												} else if (type instanceof Map<?,?>) {
//													Object object = PersisterUtils.findObjectFromMap((Map) type, identityObj);
//													if (object == null && identityObj != null) {
//														((Map) type).put(identityObj.toString(), parentObj);
//													}
//												}
											}
										}
									}

									if (targetObj instanceof Collection<?>) {
										if (targetObj instanceof List<?>) {
											List targets = (List) targetObj;
											Object targetObject = tempMap.get(targetKey);
											String identityProp = HCEConstant.ENTITY_IDENTIFIER_PROPERTY.get(targetObject.getClass().getCanonicalName());
											Field fieldOfProp;
											fieldOfProp = ReflectionUtils.findField(targetObject.getClass(), identityProp);
											fieldOfProp.setAccessible(true);
											Object identityObj = fieldOfProp.get(targetObject);
											if (identityObj != null && targets.size() > 0) {
												Object destinyObject = PersisterUtils.findObjectFromList(targets, fieldOfProp.get(targetObject),
													identityProp);
												if (destinyObject != null) {
													targetObj = destinyObject;
												} else {
													log.error("Broken tree, cannot find parent of " + targetObject.getClass().getCanonicalName()
														+ " from collection of " + targetObj.getClass().getSimpleName());
													throw new HCEErrorException("Broken tree, cannot find parent of " + targetObject.getClass()
														.getCanonicalName() + " from collection of " + targetObj.getClass().getSimpleName());
												}
											} else {
												int index = targets.indexOf(targetObject);
												if (index > -1) {
													targetObj = targets.get(index);
												} else {
//													log.warn("HCE detect parent based on identity of object, please select parent identity of class '{}' , " +
//															"cannot find parent from collection '{}'", tempMap.get(targetKey).getClass().getCanonicalName(), 
//															targetObj.getClass().getSimpleName());
//													log.error("Broken tree, cannot find parent of " + targetObject.getClass().getCanonicalName()
//														+ " from collection of " + targetObj .getClass().getSimpleName());
//													throw new HCEErrorException("Broken tree, cannot find parent of "+ targetObject
//														.getClass().getCanonicalName() + " from collection of " + targetObj
//														.getClass().getSimpleName());
												}
											}
										} else if (targetObj instanceof Set<?>) {
											boolean held = false;
											Set targets = (Set) targetObj;
											Object targetObject = tempMap.get(targetKey);
											String identityProp = HCEConstant.ENTITY_IDENTIFIER_PROPERTY.get(targetObject.getClass().getCanonicalName());
											Field fieldOfProp;
											fieldOfProp = ReflectionUtils.findField(targetObject.getClass(), identityProp);
											fieldOfProp.setAccessible(true);
											Object identityObj = fieldOfProp.get(targetObject);

											if (targets != null && targets.size() > 0) {
												if (identityObj != null) {
													Object resultObject = PersisterUtils.findObjectFromSet(targets, identityObj, identityProp);
													if (resultObject != null) {
														targetObj = resultObject;
														held = true;
													}
												} else {
													Iterator it = targets.iterator();
													while (it.hasNext()) {
														Object compare = it.next();
														if (tempMap.get(targetKey).equals(compare)) {
															targetObj = compare;
															held = true;
															break;
														}
													}
												}
											}
											if (!held && targets.size() > 0) {
//												log.warn("HCE detect parent based on identity of object, please select parent identity of class '{}' , " +
//														"cannot find parent from collection '{}'", tempMap.get(targetKey).getClass().getCanonicalName(), 
//														targetObj.getClass().getSimpleName());
//												break;
//												log.error("Broken tree, cannot find parent of " + tempMap.get(targetKey).getClass().getCanonicalName()
//													+ " from collection of " + targetObj.getClass().getSimpleName());
//												throw new HCEErrorException("Broken tree, cannot find parent of " + tempMap.get(targetKey).getClass()
//													.getCanonicalName() + " from collection of " + targetObj.getClass().getSimpleName());
											}
										}
										if (!(targetObj instanceof Collection<?>)) {
											Field fieldInner;
											fieldInner = ReflectionUtils.findField(targetObj.getClass(), keys[i]);
											fieldInner.setAccessible(true);
											if (fieldInner.get(targetObj) == null) {
												Method method;
												method = ReflectionUtils.findMethod(targetObj.getClass(), MethodUtils.getGetMethodName(keys[i]));
												Object type = method.invoke(targetObj);
												if (type == null) {
													if(!collMethodInvoke(fieldInner, targetObj, keys, i)){
														fieldInner.setAccessible(true);
														field = fieldInner;
													}
//													Method methodSet;
//													if (StringUtils.contains(fieldInner.getType().toString(), "List")) {
//														methodSet = ReflectionUtils.findMethod(targetObj.getClass(), MethodUtils.getSetMethodName(keys[i]), new Class<?>[]{List.class});
//														methodSet.invoke(targetObj, new ArrayList());
//													} else if (StringUtils.contains(fieldInner.getType().toString(), "Set")) {
//														methodSet = ReflectionUtils.findMethod(targetObj.getClass(), MethodUtils.getSetMethodName(keys[i]), new Class<?>[]{Set.class});
//														methodSet.invoke(targetObj, new HashSet());
//													}else if (StringUtils.contains(field.getType().toString(), "Map")) {
//														methodSet = ReflectionUtils.findMethod(targetObj.getClass(), MethodUtils.getSetMethodName(keys[i]), new Class<?>[]{Map.class});
//														methodSet.invoke(targetObj, new HashMap());
//													}else {
//														fieldInner.setAccessible(true);
//														field = fieldInner;
//													}
												}
											}
											if(i < lastIndex){
												targetObj = fieldInner.get(targetObj);
											}else{
												field = fieldInner;
											}
										}
									} else {										
										if (StringUtils.contains(field.getType().toString(), "List") || 
												StringUtils.contains(field.getType().toString(), "Set") || 
												StringUtils.contains(field.getType().toString(), "Map")){
											Object newTarget = field.get(targetObj);
											if (newTarget != null) {
												targetObj = newTarget;
											}else{
												targetObj = field.getType().newInstance();
											}
										}else{
											if(i < lastIndex){
												Object newTarget = field.get(targetObj);
												if (newTarget != null) {
													targetObj = newTarget;
												}else{
													targetObj = field.getType().newInstance();
												}
											}
										}
									}

								} catch (Exception e) {
									log.error("Cannot find field " + keys[i] + " from " + targetObj.getClass().getSimpleName() 
											+ " caused by :" + e.getMessage());
									throw new HCEErrorException("Cannot find field " + keys[i] + " from " + targetObj.getClass().getSimpleName()
											+ " caused by :" + e.getMessage());
								}
							}
							if (targetObj instanceof Collection<?>) {
								try {
									collectionsProcess(targetObj, obj);
									
//									String identityProp = HCEConstant.ENTITY_IDENTIFIER_PROPERTY.get(obj.getClass().getCanonicalName());
//									Field fieldOfProp;
//									fieldOfProp = ReflectionUtils.findField(obj.getClass(), identityProp);
//									fieldOfProp.setAccessible(true);
//									Object identityObj = fieldOfProp.get(obj);
//									if (targetObj instanceof List<?>) {
//										Object object = PersisterUtils.findObjectFromList((List) targetObj, identityObj, identityProp);
//										if (object == null && identityObj != null) {
//											((List) targetObj).add(obj);
//										}
//									} else if (targetObj instanceof Set<?>) {
//										Object object = PersisterUtils.findObjectFromSet((Set) targetObj, identityObj, identityProp);
//										if (object == null && identityObj != null) {
//											((Set) targetObj).add(obj);
//										}
//									}  else if (targetObj instanceof Map<?,?>) {
//										Object object = PersisterUtils.findObjectFromMap((Map) targetObj, identityObj);
//										if (object == null && identityObj != null) {
//											((Map) targetObj).put(identityObj.toString(), obj);
//										}
//									}
									
								} catch (Exception e) {
									log.error("Cannot find field from " + obj.getClass().getSimpleName() + " caused by :" + e.getMessage());
									throw new HCEErrorException("Cannot find field from " + obj.getClass().getSimpleName()
										+ " caused by :" + e.getMessage());
								}
							} else {
								field.setAccessible(true);
								Object targetField = field.get(targetObj);
//								if(softtech.hong.hce.utils.ArrayUtils.indexOfString(COLLECTION_TYPES, field.getType().toString())){
//									
//								}
								
								if (StringUtils.contains(field.getType().toString(), "List")){
									if(null == targetField){
										field.set(targetObj, new ArrayList());
									}
									targetField = field.get(targetObj);
									((List) targetField).add(obj);
								}else if(StringUtils.contains(field.getType().toString(), "Set")){
									if(null == targetField){
										field.set(targetObj, new HashSet());
									}
									targetField = field.get(targetObj);
									((Set) targetField).add(obj);
								}else if(StringUtils.contains(field.getType().toString(), "Map")){
									if(null == targetField){
										field.set(targetObj, new HashMap());
									}
									targetField = field.get(targetObj);
									((HashMap) targetField).put(System.currentTimeMillis(), obj);
								}else{
									if (obj != null) {
										field.set(targetObj, obj);
									}
								}
							}
						}
						targetObj = mainObj;
					}
				}
			}
			setters = null;
		} catch (InstantiationException e) {
			throw new HCEErrorException("Could not instantiate resultclass: " + resultClass.getName());
		} catch (IllegalAccessException e) {
			throw new HCEErrorException("Could not instantiate resultclass: " + resultClass.getName());
		}
		if (persisterObject.isCollection()) {
			return null;
		}
		return result;
	}

	/* (non-Javadoc)
	 * @see org.hibernate.transform.ResultTransformer#transformList(java.util.List)
	 */
	@SuppressWarnings({ "rawtypes" })
	public List transformList(List collection) {
		if (isCollectionFetched) {
			return results;
		}
		return collection;
	}

	private static String join(int endIndex, String[] keys) {
		StringBuffer results = new StringBuffer();
		for (int i = 0; i <= endIndex; i++) {
			if (i > 0 && i <= endIndex) {
				results.append(DELIMETER);
			}
			results.append(keys[i]);
		}
		return results.toString();
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void collectionsProcess(Object type, Object parentObj) throws Exception{
		String identityProp = HCEConstant.ENTITY_IDENTIFIER_PROPERTY.get(
				parentObj.getClass().getCanonicalName());
		Field fieldOfProp;
		fieldOfProp = ReflectionUtils.findField(parentObj.getClass(), identityProp);
		fieldOfProp.setAccessible(true);
		Object identityObj = fieldOfProp.get(parentObj);
		if (type instanceof List<?>) {	
			Object object = PersisterUtils.findObjectFromList((List) type, identityObj, identityProp);
			if (object == null && identityObj != null) {
				((List) type).add(parentObj);
			}
		} else if (type instanceof Set<?>) {
			Object object = PersisterUtils.findObjectFromSet((Set) type, identityObj, identityProp);
			if (object == null && identityObj != null) {
				((Set) type).add(parentObj);
			}
		} else if (type instanceof Map<?,?>) {
			Object object = PersisterUtils.findObjectFromMap((Map) type, identityObj);
			if (object == null && identityObj != null) {
				((Map) type).put(identityObj.toString(), parentObj);
			}
		}
	}
	
	@SuppressWarnings("rawtypes")
	private boolean collMethodInvoke(Field field, Object targetObj, String[] keys, int i) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException{
		Method methodSet;
		if (StringUtils.contains(field.getType().toString(), "List")) {
			methodSet = ReflectionUtils.findMethod(targetObj.getClass(), MethodUtils.getSetMethodName(keys[i]), new Class<?>[]{List.class});
			methodSet.invoke(targetObj, new ArrayList());
			return true;
		} else if (StringUtils.contains(field.getType().toString(), "Set")) {
			methodSet = ReflectionUtils.findMethod(targetObj.getClass(), MethodUtils.getSetMethodName(keys[i]), new Class<?>[]{Set.class});
			methodSet.invoke(targetObj, new HashSet());
			return true;
		} else if (StringUtils.contains(field.getType().toString(), "Map")) {
			methodSet = ReflectionUtils.findMethod(targetObj.getClass(), MethodUtils.getSetMethodName(keys[i]), new Class<?>[]{Map.class});
			methodSet.invoke(targetObj, new HashMap());
			return true;
		}
		return false;
	}

}
