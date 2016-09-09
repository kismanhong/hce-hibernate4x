package softtech.hong.hce.engine;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
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
import softtech.hong.hce.constant.QueryConstant;
import softtech.hong.hce.exception.HCEErrorException;
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
public class HCEResultTransformer extends HCEProcessor implements ResultTransformer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -264194164964958304L;
	private static Logger log = LoggerFactory.getLogger(HCEResultTransformer.class);
	private final Class<?> resultClass;
	private Setter[] setters;
	private PropertyAccessor propertyAccessor;
	private boolean isCollectionFetched = false;
	private final static String DELIMETER = "|";
	private PersisterUtils.PersisterObject persisterObject;
	@SuppressWarnings("rawtypes")
	private List results = new ArrayList();
	private int index = 0;
	private String[] distinctAlias;

	/**
	 * @param resultClass -> result class from fetching
	 */
	public HCEResultTransformer(Class<?> resultClass) {
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
							setterProcess(propertyAccessor, alias, setters, i, result, resultClass, tuple);
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
								key = QueryConstant.ALIAS;
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
//								log.error("Cannot find field " + fieldName + " from " + field.getClass().getSimpleName() + " caused by :" + e.getMessage());
								throw new HCEErrorException("Cannot find field " + fieldName + " from " + field.getClass().getSimpleName() + " caused by :" + e.getMessage());
							}
						}
					}
					Object targetObj = null;
					Object sourceObj = tempMap.get(QueryConstant.ALIAS);
					Object mainObj = null;
					String identityProperty = HCEConstant.ENTITY_IDENTIFIER_PROPERTY.get(sourceObj.getClass().getCanonicalName());
					Object idValue = getIdentityValue(sourceObj, identityProperty);
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
							targetObj = tempMap.get(QueryConstant.ALIAS);
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
							targetObj = tempMap.get(QueryConstant.ALIAS);
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
						if (!key.equals(QueryConstant.ALIAS)) {
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
										Object targetObject = field.get(targetObj);
										if(targetObject == null){
											setField(field, targetObj, keys, i);
										}else{
											if (targetObject instanceof Collection<?>) {
												Object parentObj = tempMap.get(join(i, keys));
												collectionsProcess(targetObj, parentObj);
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
												String targetIdentity = HCEConstant.ENTITY_IDENTIFIER_PROPERTY.get(targets.get(0).getClass().getCanonicalName());
												Object destinyObject = PersisterUtils.findObjectFromList(targets, fieldOfProp.get(targetObject), targetIdentity);
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
//													log.info("HCE detect parent based on identity of object, please select parent identity of class '{}' , " +
//															"cannot find parent from collection '{}'", tempMap.get(targetKey).getClass().getCanonicalName(), 
//															targetObj.getClass().getSimpleName());
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
												Object objTarget = null;
												for(Object target: targets){
													objTarget = target;
													break;
												}
												String targetIdentity = HCEConstant.ENTITY_IDENTIFIER_PROPERTY.get(objTarget.getClass().getCanonicalName());
												if (identityObj != null) {
													Object resultObject = PersisterUtils.findObjectFromSet(targets, identityObj, targetIdentity);
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
//											if (!held && targets.size() > 0) {
//												log.info("HCE detect parent based on identity of object, please select parent identity of class '{}' , " +
//														"cannot find parent from collection '{}'", tempMap.get(targetKey).getClass().getCanonicalName(), 
//														targetObj.getClass().getSimpleName());
//											}
										}
										if (!(targetObj instanceof Collection<?>)) {
											Field fieldInner;
											fieldInner = ReflectionUtils.findField(targetObj.getClass(), keys[i]);
											fieldInner.setAccessible(true);
											Object targetObject = fieldInner.get(targetObj);
											
											if (targetObject == null) {
												if(!setField(fieldInner, targetObj, keys, i)){
													fieldInner.setAccessible(true);
													field = fieldInner;
												}
											}
											if(i < lastIndex){
												targetObj = fieldInner.get(targetObj);
											}else{
												field = fieldInner;
											}
										}
									} else {										
										if (Collection.class.isAssignableFrom(field.getType())){
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
								} catch (Exception e) {
									log.error("Cannot find field from " + obj.getClass().getSimpleName() + " caused by :" + e.getMessage());
									throw new HCEErrorException("Cannot find field from " + obj.getClass().getSimpleName()
										+ " caused by :" + e.getMessage());
								}
							} else {
								field.setAccessible(true);
								Object targetField = field.get(targetObj);
								
								if (List.class.isAssignableFrom(field.getType())){
									if(null == targetField){
										field.set(targetObj, new ArrayList());
									}
									targetField = field.get(targetObj);
									List collTarget = (List) targetField;
									if(!collTarget.contains(obj)){
										collTarget.add(obj);
									}
								}else if(Set.class.isAssignableFrom(field.getType())){
									if(null == targetField){
										field.set(targetObj, new HashSet());
									}
									targetField = field.get(targetObj);
									((Set) targetField).add(obj);
								}else if(Map.class.isAssignableFrom(field.getType())){
									if(null == targetField){
										field.set(targetObj, new HashMap());
									}
									targetField = field.get(targetObj);
									Map collMap = (Map) targetField;
									if(!collMap.containsValue(obj)){
										collMap.put(System.currentTimeMillis(), obj);
									}
								}else{
									if (obj != null && field.get(targetObj)==null) {
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
		} catch (Exception e) {
			e.printStackTrace();
			throw new HCEErrorException(e);
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
	
	/*
	 * add value to collection, detect it's parent before set it
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void collectionsProcess(Object type, Object parentObj) throws Exception{
		String identityProp = HCEConstant.ENTITY_IDENTIFIER_PROPERTY.get(parentObj.getClass().getCanonicalName());
		Field fieldOfProp;
		fieldOfProp = ReflectionUtils.findField(parentObj.getClass(), identityProp);
		fieldOfProp.setAccessible(true);
		Object identityObj = fieldOfProp.get(parentObj);
		if (type instanceof List<?>) {	
			List targetList = ((List) type);
			Object object = PersisterUtils.findObjectFromList(targetList, identityObj, identityProp);
			if (object == null && identityObj != null) {
				if(!targetList.contains(parentObj)){
					targetList.add(parentObj);
				}
			}
		} else if (type instanceof Set<?>) {
			Object object = PersisterUtils.findObjectFromSet((Set) type, identityObj, identityProp);
			if (object == null && identityObj != null) {
				((Set) type).add(parentObj);
			}
		} else if (type instanceof Map<?,?>) {
			Object object = PersisterUtils.findObjectFromMap((Map) type, identityObj);
			if (object == null && identityObj != null) {
				Map targetMap = (Map) type;
				if(!targetMap.containsValue(parentObj)){
					targetMap.put(identityObj.toString(), parentObj);
				}
			}
		}
	}
	
	/*
	 * initial collection to target, usually the first time target property is null, so initial them
	 */
	@SuppressWarnings("rawtypes")
	private boolean setField(Field field, Object targetObj, String[] keys, int i) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException{
		field.setAccessible(true);
		if (List.class.isAssignableFrom(field.getType())) {		
			field.set(targetObj, new ArrayList());
			return true;
		} else if (Set.class.isAssignableFrom(field.getType())) {
			field.set(targetObj, new HashSet());
			return true;
		} else if (Map.class.isAssignableFrom(field.getType())) {
			field.set(targetObj, new HashMap());
			return true;
		}
		return false;
	}
	
	/*
	 * getting primary key value from object/table
	 */
	private Object getIdentityValue(Object sourceObj, String identityProperty) throws IllegalArgumentException, IllegalAccessException{
		Field idField = null;
		try {
			idField = ReflectionUtils.findField(sourceObj.getClass(), identityProperty);
			idField.setAccessible(true);
		} catch (Exception e) {
			log.error("Cannot find Identity Property from :" + sourceObj.getClass().getCanonicalName());
		}
		Object idValue = idField.get(sourceObj);
		return idValue;
	}

}
