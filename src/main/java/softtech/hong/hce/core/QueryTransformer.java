package softtech.hong.hce.core;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import javax.persistence.Transient;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.FetchMode;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Property;
import org.hibernate.criterion.Restrictions;
import org.hibernate.sql.JoinType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import softtech.hong.hce.constant.QueryConstant;
import softtech.hong.hce.engine.HCETransformers;
import softtech.hong.hce.engine.HCEUsualTransformers;
import softtech.hong.hce.exception.HCEErrorException;
import softtech.hong.hce.model.Expression;
import softtech.hong.hce.model.Order;
import softtech.hong.hce.type.OrderType;
import softtech.hong.hce.utils.BeanUtils;
import softtech.hong.hce.utils.QueryUtils;
import softtech.hong.hce.utils.ReflectionUtils;

/**
 * @author Kisman Hong, email : kismanhong@gmail.com
 * 	this class is used for handling generic Search, every
 *         request list will pass this code
 *         This is a critical class, used by almost all action
 */
public abstract class QueryTransformer<T> extends QueryTransformerExt{

	private static Logger log = LoggerFactory.getLogger(QueryTransformer.class);
	
	protected Class<T> domainClass = getDomainClass();
	
	private String parentAlias;
	
	private String grandParentAlias;
	
	private Class<?> parentClass;

	@SuppressWarnings("unchecked")
	protected Class<T> getDomainClass() {
//	    if (domainClass == null) {
//	    	ParameterizedType thisType = (ParameterizedType) getClass().getGenericSuperclass();
//	        domainClass = (Class<T>) thisType.getActualTypeArguments()[0];
//	    }
//	    return domainClass;
	    
		if(domainClass == null){
		    Type genericSuperClass = getClass().getGenericSuperclass();
	
		    ParameterizedType parametrizedType = null;
		    while (parametrizedType == null) {
		        if ((genericSuperClass instanceof ParameterizedType)) {
		            parametrizedType = (ParameterizedType) genericSuperClass;
		        } else {
		            genericSuperClass = ((Class<?>) genericSuperClass).getGenericSuperclass();
		        }
		    }
	
		    domainClass = (Class<T>) parametrizedType.getActualTypeArguments()[0];
		}
	    
	    return domainClass;
	}
	
	/**
	 * this method is used for paging purpose
	 * @param projections -> field(s) to be selected
	 * @param expressions -> constraint or restrictions
	 * @param orders -> order(s) parameters, sequential
	 * @return -> Array of {@link DetachedCriteria} => one for count, the other for select
	 * @throws {@link HCEErrorException}
	 */
	protected DetachedCriteria[] queryTransformer(String[] projections , Expression[] expressions, Order... orders) 
			throws HCEErrorException{
		DetachedCriteria detachedCriteria = DetachedCriteria.forClass(domainClass, QueryConstant.ALIAS);
		DetachedCriteria countDetachedCriteria = DetachedCriteria.forClass(domainClass, QueryConstant.ALIAS);
		List<String> hasAddedCriteria = new ArrayList<String>();
		
		projections = QueryUtils.restructureProjections(domainClass, projections);  // if projections contain *, will be extracted from fields
		if(projections != null && projections.length > 0){	
			fetchProjections(domainClass, projections, hasAddedCriteria, detachedCriteria, countDetachedCriteria, null);	
			detachedCriteria.setProjection(QueryUtils.getInstance().buildProjections(projections, projections));
			detachedCriteria.setResultTransformer(HCETransformers.aliasToBean(domainClass));
		}
//		restrictionRestrict(domainClass, expressions, detachedCriteria, countDetachedCriteria, hasAddedCriteria);	
		applyRestriction(domainClass, expressions, detachedCriteria, countDetachedCriteria, hasAddedCriteria);
		countDetachedCriteria.setProjection(Projections.rowCount());
		initializeOrder(domainClass, hasAddedCriteria, orders, detachedCriteria, null);
		return new DetachedCriteria[]{countDetachedCriteria, detachedCriteria};		
	}
	
	/**
	 * this method is used for paging purpose
	 * @param projections -> field(s) to be selected
	 * @param expression -> constraint or restrictions, can be more than one restriction by using add method in {@link Expression} class
	 * @param orders -> order(s) parameters, sequential
	 * @return -> Array of {@link DetachedCriteria} => one for count, the other for select
	 * @throws {@link HCEErrorException} 
	 */
	protected DetachedCriteria[] queryTransformer(String[] projections , Expression expression, Order... orders) throws HCEErrorException{
		return queryTransformer(projections, new Expression[]{expression}, orders);
	}
	
	/**
	 * this method is used for paging purpose, when the object is not null the search will be applied to every property that is not empty.
	 * @param object -> object of class to be selected and will be generated as where statement
	 * @param orders -> order(s) parameters, sequential
	 * @param projections -> field(s) to be selected
	 * @param enableLike -> if you wanna enable like in where statement, set to true
	 * @return -> Array of {@link DetachedCriteria} => one for count, the other for select
	 * @throws {@link HCEErrorException}
	 */
	protected DetachedCriteria[] queryTransformer(Object object, String[] projections, boolean enableLike, Order... orders) throws HCEErrorException {	
		return queryTransformer(object, projections, new String[]{}, null, false, enableLike, orders);
	}
	
	/**
	 * this method is used for paging purpose, when the object is not null the search will be applied to every property that is not empty.
	 * @param object -> object of class to be selected and will be generated as where statement
	 * @param orders -> order(s) parameters, sequential
	 * @param projections -> field(s) to be selected
	 * @param enableLike -> if you wanna enable like in where statement, set to true
	 * @return -> Array of {@link DetachedCriteria} => one for count, the other for select
	 * @throws {@link HCEErrorException}
	 */
	protected DetachedCriteria[] queryTransformer(Object object, String[] projections, boolean enableLike, String[] addJoins, Order... orders) throws HCEErrorException {	
		return queryTransformer(object, projections, new String[]{}, addJoins, false, enableLike, orders);
	}
	
	/**
	 * this method is used for paging purpose, when the object is not null the search will be applied to every property that is not empty.
	 * @param object -> object of class to be selected and will be generated as where statement
	 * @param orders -> order(s) parameters, sequential
	 * @param projections -> field(s) to be selected
	 * @return -> Array of {@link DetachedCriteria} => one for count, the other for select
	 * @throws {@link HCEErrorException}
	 */
	protected DetachedCriteria[] queryTransformer(Object object, String[] projections, Order... orders) throws HCEErrorException {	
		return queryTransformer(object, projections, new String[]{}, null, false, false, orders);
	}
	
	/**
	 * this method is used for paging purpose, when the object is not null the search will be applied to every property that is not empty.
	 * @param object -> filter object, where clause based on property that is not null or not ignored
	 * @param projections -> field(s) to be selected
	 * @param ignoreProperties -> field or property to be ignored when filtering
	 * @param ignoreSensitive -> if true : field / property to be ignored must be completely spell, if false : only by checking index of
	 * @param enableLike -> if you wanna enable like in where statement, set to true
	 * @param orders -> order(s) parameters, sequential
	 * @return -> Array of {@link DetachedCriteria} => one for count, the other for select
	 * @throws {@link HCEErrorException}
	 */
	protected DetachedCriteria[] queryTransformer(Object object, String[] projections, String[] ignoreProperties, String[] addJoins, boolean ignoreSensitive, 
			boolean enableLike, Order... orders) throws HCEErrorException{
		return queryTransformer(object, projections, ignoreProperties, addJoins, ignoreSensitive, 
				enableLike, null, orders);
	}
	
	/**
	 * this method is used for paging purpose, when the object is not null the search will be applied to every property that is not empty.
	 * @param object -> filter object, where clause based on property that is not null or not ignored
	 * @param projections -> field(s) to be selected
	 * @param ignoreProperties -> field or property to be ignored when filtering
	 * @param ignoreSensitive -> if true : field / property to be ignored must be completely spell, if false : only by checking index of
	 * @param enableLike -> if you wanna enable like in where statement, set to true
	 * @param orders -> order(s) parameters, sequential
	 * @return -> Array of {@link DetachedCriteria} => one for count, the other for select
	 * @throws {@link HCEErrorException}
	 */
	protected DetachedCriteria[] queryTransformer(Object object, String[] projections, String[] ignoreProperties, String[] addJoins, boolean ignoreSensitive, 
			boolean enableLike,  HashMap<String, JoinType> joinOverrrides, Order... orders) throws HCEErrorException {
		try {
			DetachedCriteria searchCriteria = DetachedCriteria.forClass(domainClass, QueryConstant.ALIAS);
			DetachedCriteria countCriteria = DetachedCriteria.forClass(domainClass, QueryConstant.ALIAS);
			
			projections = QueryUtils.restructureProjections(domainClass, projections);  // if projections contain *, will be extracted from fields
			queryTransformation(domainClass, searchCriteria, countCriteria, object, orders, projections, ignoreProperties, addJoins, ignoreSensitive, enableLike, joinOverrrides);
			if(projections != null && projections.length > 0){						
				searchCriteria.setProjection(QueryUtils.getInstance().buildProjections(projections));
				searchCriteria.setResultTransformer(HCETransformers.aliasToBean(domainClass));
			}		
			countCriteria.setProjection(Projections.rowCount());
			return new DetachedCriteria[] { countCriteria, searchCriteria};
		} catch (Exception e) {
			throw new HCEErrorException(e);
		}
	}
	
	/**
	 * this method is used for paging purpose, when the object is not null the search will be applied to every property that is not empty.
	 * @param object -> filter object, where clause based on property that is not null or not ignored
	 * @param projections -> field(s) to be selected
	 * @param ignoreProperties -> field or property to be ignored when filtering
	 * @param ignoreSensitive -> if true : field / property to be ignored must be completely spell, if false : only by checking index of
	 * @param enableLike -> if you wanna enable like in where statement, set to true
	 * @param orders -> order(s) parameters, sequential
	 * @return -> Array of {@link DetachedCriteria} => one for count, the other for select
	 * @throws {@link HCEErrorException}
	 */
	protected DetachedCriteria[] queryTransformer(Object object, String[] projections, String[] ignoreProperties, boolean ignoreSensitive, 
			boolean enableLike, Order... orders) throws HCEErrorException {
		return queryTransformer(object, projections, ignoreProperties, null, ignoreSensitive, enableLike, orders);
	}
	
	/**
	 * this method is used for paging purpose, when the object is not null the search will be applied to every property that is not empty.
	 * @param object -> filter object, where clause based on property that is not null or not ignored
	 * @param projections -> field(s) to be selected
	 * @param ignoreProperties -> field or property to be ignored when filtering
	 * @param ignoreSensitive -> if true : field / property to be ignored must be completely spell, if false : only by checking index of
	 * @param enableLike -> if you wanna enable like in where statement, set to true
	 * @param orders -> order(s) parameters, sequential
	 * @return -> Array of {@link DetachedCriteria} => one for count, the other for select
	 * @throws {@link HCEErrorException}
	 */
	protected DetachedCriteria[] queryTransformer(Object object, String[] projections, String[] ignoreProperties, boolean ignoreSensitive, 
			boolean enableLike, String[] addJoins, Order... orders) throws HCEErrorException {
		return queryTransformer(object, projections, ignoreProperties, addJoins, ignoreSensitive, enableLike, orders);
	}
	
	/**
	 * this method is used for paging purpose, when the object is not null the search will be applied to every property that is not empty.
	 * @param object -> filter object, where clause based on property that is not null or not ignored
	 * @param clazz -> class to be selected and returned
	 * @param orders -> order(s) parameters, sequential
	 * @param projections -> field(s) to be selected
	 * @param ignoreProperties -> field or property to be ignored when filtering
	 * @param enableLike -> if you wanna enable like in where statement, set to true
	 * @return -> Array of {@link DetachedCriteria} => one for count, the other for select
	 * @throws {@link HCEErrorException}
	 */
	protected DetachedCriteria[] queryTransformer(Class<?> clazz, Object object, 
			String[] projections, String[] ignoreProperties, boolean enableLike, Order... orders) throws HCEErrorException {
		return queryTransformer( clazz, object, projections, ignoreProperties, enableLike, null, orders);
	}
	
	/**
	 * this method is used for paging purpose, when the object is not null the search will be applied to every property that is not empty.
	 * @param object -> filter object, where clause based on property that is not null or not ignored
	 * @param clazz -> class to be selected and returned
	 * @param orders -> order(s) parameters, sequential
	 * @param projections -> field(s) to be selected
	 * @param ignoreProperties -> field or property to be ignored when filtering
	 * @param enableLike -> if you wanna enable like in where statement, set to true
	 * @return -> Array of {@link DetachedCriteria} => one for count, the other for select
	 * @throws {@link HCEErrorException}
	 */
	protected DetachedCriteria[] queryTransformer(Class<?> clazz, Object object, 
			String[] projections, String[] ignoreProperties, boolean enableLike, HashMap<String, JoinType> joinOverrides, Order... orders) throws HCEErrorException {
		try {
			DetachedCriteria criteria = DetachedCriteria.forClass(clazz, QueryConstant.ALIAS);
			DetachedCriteria countCriteria = DetachedCriteria.forClass(clazz, QueryConstant.ALIAS);
			
			projections = QueryUtils.restructureProjections(clazz, projections);  // if projections contain *, will be extracted from fields
			queryTransformation(clazz, criteria, countCriteria, object, orders, projections, ignoreProperties, null, enableLike, false, joinOverrides);	
			if(projections != null && projections.length > 0){
				criteria.setProjection(QueryUtils.getInstance().buildProjections(projections));
				criteria.setResultTransformer(HCETransformers.aliasToBean(clazz));
			}
			return new DetachedCriteria[] { countCriteria, criteria};
		} catch (Exception e) {
			throw new HCEErrorException(e);
		}
	}
	
	/**
	 * this method is used for paging purpose, when the object is not null the search will be applied to every property that is not empty.
	 * we can ignore property by passing through ignoredProperties parameter
	 * @param clazz -> class to be selected and returned
	 * @param object -> filter object, where clause based on property that is not null or not ignored
	 * @param orders -> order(s) parameters, sequential
	 * @param projections -> field(s) to be selected
	 * @param ignoredProperties -> field or property to be ignored when filtering
	 * @return -> Array of {@link DetachedCriteria} => one for count, the other for select
	 * @throws {@link HCEErrorException}
	 */
	protected DetachedCriteria[] queryTransformer(Class<?> clazz, Object object, String[] projections, String[] ignoreProperties, Order... orders) throws HCEErrorException{
		return queryTransformer(clazz, object, projections, ignoreProperties, false, orders);
	}
	
	/**
	 * this method is used for paging purpose, when the object is not null the search will be applied to every property that is not empty.
	 * we can ignore property by passing through ignoredProperties parameter
	 * @param object -> filter object, where clause based on property that is not null or not ignored
	 * @param orders -> order(s) parameters, sequential
	 * @param projections -> field(s) to be selected
	 * @param ignoredProperties -> field or property to be ignored when filtering
	 * @return -> Array of {@link DetachedCriteria} => one for count, the other for select
	 * @throws {@link HCEErrorException}
	 */
	protected DetachedCriteria[] queryTransformer(Object object, String[] projections, String[] ignoreProperties, Order... orders) throws HCEErrorException {
		return queryTransformer(object, projections, ignoreProperties, null, false, false, orders);	
	}
	
	/**
	 * this method is used for paging purpose, when the object is not null the search will be applied to every property that is not empty.
	 * we can ignore property by passing through ignoredProperties parameter
	 * @param object -> filter object, where clause based on property that is not null or not ignored
	 * @param orders -> order(s) parameters, sequential
	 * @param projections -> field(s) to be selected
	 * @param ignoredProperties -> field or property to be ignored when filtering
	 * @param enableLike -> if you wanna enable like in where statement, set to true
	 * @return -> Array of {@link DetachedCriteria} => one for count, the other for select
	 * @throws {@link HCEErrorException}
	 */
	protected DetachedCriteria[] queryTransformer(Object object, String[] projections, String[] ignoreProperties, boolean enableLike, Order... orders) throws HCEErrorException {
		return queryTransformer(object, projections, ignoreProperties, null, false, enableLike, orders);	
	}
	
	/**
	 * this method is used for query where clause generation based on object
	 * we can ignore property by passing through ignoredProperties parameter
	 * @param object -> filter object, where clause based on property that is not null or not ignored
	 * @param orders -> order(s) parameters, sequential
	 * @param projections -> field(s) to be selected
	 * @param ignoredProperties -> field or property to be ignored when filtering
	 * @param ignoreSensitive -> if true : field / property to be ignored must be completely spell, if false : only by checking index of
	 * @return -> {@link DetachedCriteria} => select query
	 * @throws {@link HCEErrorException}
	 */
	protected DetachedCriteria queryTranslation(Object object, String[] projections, String[] ignoreProperties, boolean ignoreSensitive, Order... orders) 
			throws HCEErrorException {
		return queryTransformer(object, projections, ignoreProperties, ignoreSensitive, orders)[1];	
	}
	
	/**
	 * this method is used for query where clause generation based on object
	 * we can ignore property by passing through ignoredProperties parameter
	 * @param object -> filter object, where clause based on property that is not null or not ignored
	 * @param orders -> order(s) parameters, sequential
	 * @param projections -> field(s) to be selected
	 * @param ignoredProperties -> field or property to be ignored when filtering
	 * @param ignoreSensitive -> if true : field / property to be ignored must be completely spell, if false : only by checking index of
	 * @param enableLike -> if you wanna enable like in where statement, set to true
	 * @return -> {@link DetachedCriteria} => select query
	 * @throws {@link HCEErrorException}
	 */
	protected DetachedCriteria queryTranslation(Object object, String[] projections, String[] ignoreProperties, boolean ignoreSensitive, boolean enableLike, Order... orders) 
			throws HCEErrorException {
		return queryTransformer(object, projections, ignoreProperties, null, ignoreSensitive, enableLike, orders)[1];	
	}
	
	/**
	 * this method is used for query where clause generation based on object
	 * we can ignore property by passing through ignoredProperties parameter
	 * @param object -> filter object, where clause based on property that is not null or not ignored
	 * @param orders -> order(s) parameters, sequential
	 * @param projections -> field(s) to be selected
	 * @param ignoredProperties -> field or property to be ignored when filtering
	 * @return -> {@link DetachedCriteria} => select query
	 * @throws {@link HCEErrorException}
	 */
	protected DetachedCriteria queryTranslation(Object object, String[] projections, String[] ignoreProperties, Order... orders) throws HCEErrorException {
		return queryTranslation(object, projections, ignoreProperties, false, orders);	
	}
		
	/**
	 * this method is used for query where clause generation based on object
	 * we can ignore property by passing through ignoredProperties parameter
	 * @param object -> filter object, where clause based on property that is not null or not ignored
	 * @param orders -> order(s) parameters, sequential
	 * @param projections -> field(s) to be selected
	 * @param ignoredProperties -> field or property to be ignored when filtering
	 * @param enableLike -> if you wanna enable like in where statement, set to true
	 * @return -> {@link DetachedCriteria} => select query
	 * @throws {@link HCEErrorException}
	 */
	protected DetachedCriteria queryTranslation(Object object, String[] projections, boolean enableLike, String[] ignoreProperties, Order... orders) throws HCEErrorException {
		return queryTranslation(object, projections, ignoreProperties, false, enableLike, orders);	
	}	
	
	/**
	 * used for querying object using id and return object will be clazz parameter
	 * @param clazz -> class to be selected / queried and returned
	 * @param projections -> field(s) to be selected
	 * @param id -> object identity value
	 * @return -> {@link DetachedCriteria}
	 * @throws Exception 
	 */
	protected DetachedCriteria criteriaById (Class<?> clazz, String[] projections, Long id) throws HCEErrorException{
		return criteriaById(clazz, projections, id, null);
	}
	
	/**
	 * used for querying object using id and return object will be clazz parameter
	 * @param clazz -> class to be selected / queried and returned
	 * @param projections -> field(s) to be selected
	 * @param id -> object identity value
	 * @return -> {@link DetachedCriteria}
	 * @throws Exception 
	 */
	protected DetachedCriteria criteriaById (Class<?> clazz, String[] projections, Long id, HashMap<String, JoinType> joinOverrides) throws HCEErrorException{
		DetachedCriteria criteria = DetachedCriteria.forClass(clazz, QueryConstant.ALIAS);
		List<String> hasAddedCriteria = new ArrayList<String>();
		
		projections = QueryUtils.restructureProjections(clazz, projections);  // if projections contain *, will be extracted from fields
		fetchProjections(clazz, projections, hasAddedCriteria, criteria, joinOverrides);
		criteria.add(Restrictions.eq(QueryConstant.ALIAS + "." + QueryUtils.getIdentifier(clazz.getCanonicalName()), id));
		if(projections != null && projections.length > 0){
			criteria.setProjection(QueryUtils.getInstance().buildProjections(projections));
			criteria.setResultTransformer(HCETransformers.aliasToBean(clazz));
		}
		return criteria;
	}

	/**
	 * used for querying object using id and return domainClass
	 * @param projections -> field(s) to be selected
	 * @param id -> object identity value
	 * @return -> {@link DetachedCriteria}
	 * @throws Exception 
	 */
	protected DetachedCriteria criteriaById (String[] projections, Long id) throws HCEErrorException{
		return criteriaById(domainClass, projections, id, null);
	}
	
	/**
	 * used for querying object using id and return domainClass
	 * @param projections -> field(s) to be selected
	 * @param id -> object identity value
	 * @return -> {@link DetachedCriteria}
	 * @throws Exception 
	 */
	protected DetachedCriteria criteriaById (String[] projections, Long id, HashMap<String, JoinType> joinOverrides) throws HCEErrorException{
		return criteriaById(domainClass, projections, id, joinOverrides);
	}
	
	/**
	 * select all the field(s) with give any restriction (detect join type) and return object will be domainClass
	 * @param expressions -> constraint or restrictions
	 * @param orders -> order(s) parameters, sequential
	 * @return -> {@link DetachedCriteria}
	 */
	protected DetachedCriteria criteriaWithRestriction (Expression[] expressions, Order... orders) throws HCEErrorException{
		return criteriaWithRestriction(domainClass, expressions, orders);
	}
	
	/**
	 * select all the field(s) with give any restriction (detect join type) and return object will be domainClass
	 * @param expressions -> constraint or restrictions
	 * @param orders -> order(s) parameters, sequential
	 * @return -> {@link DetachedCriteria}
	 */
	protected DetachedCriteria criteriaWithRestriction (Expression[] expressions, HashMap<String, JoinType> joinOverrides, Order... orders) throws HCEErrorException{
		return criteriaWithRestriction(domainClass, expressions, joinOverrides, orders);
	}
	
	/**
	 * select all the field(s) with give any restriction (detect join type) and return object will be clazz param
	 * @param clazz -> class to be selected / queried
	 * @param expressions -> constraint or restrictions
	 * @param orders -> order(s) parameters, sequential	
	 * @return -> {@link DetachedCriteria}
	 */
	protected DetachedCriteria criteriaWithRestriction (Class<?> clazz, Expression[] expressions, Order... orders) throws HCEErrorException{
		return criteriaByProperty(clazz, null, expressions, orders);
	}
	
	/**
	 * select all the field(s) with give any restriction (detect join type) and return object will be clazz param
	 * @param clazz -> class to be selected / queried
	 * @param expressions -> constraint or restrictions
	 * @param orders -> order(s) parameters, sequential	
	 * @return -> {@link DetachedCriteria}
	 */
	protected DetachedCriteria criteriaWithRestriction (Class<?> clazz, Expression[] expressions, HashMap<String, JoinType> joinOverrides, Order... orders) 
			throws HCEErrorException{
		return criteriaByProperty(clazz, null, expressions, joinOverrides, orders);
	}
	
	/**
	 * select all the field(s) with give any restriction (detect join type) and return object will be domainClass
	 * @param expression -> constraint or restrictions, can be more than one, use add method in PropertyValue class
	 * @param orders -> order(s) parameters, sequential 
	 * @return -> {@link DetachedCriteria}
	 */
	protected DetachedCriteria criteriaWithRestriction (Expression expression, Order... orders) throws HCEErrorException{
		return criteriaWithRestriction(domainClass, new Expression[]{expression}, orders);
	}
	
	/**
	 * select all the field(s) with give any restriction (detect join type) and return object will be domainClass
	 * @param expression -> constraint or restrictions, can be more than one, use add method in PropertyValue class
	 * @param orders -> order(s) parameters, sequential 
	 * @return -> {@link DetachedCriteria}
	 */
	protected DetachedCriteria criteriaWithRestriction (Expression expression, HashMap<String, JoinType> joinOverrides, Order... orders) throws HCEErrorException{
		return criteriaWithRestriction(domainClass, new Expression[]{expression}, joinOverrides, orders);
	}
	
	/**
	 * select all the field(s) with give any restriction (detect join type) and return object will be clazz param
	 * @param clazz -> class to be selected / queried
	 * @param expression -> constraint or restrictions, can be more than one, use add method in PropertyValue class
	 * @param orders -> order(s) parameters, sequential		
	 * @return -> {@link DetachedCriteria}
	 */
	protected DetachedCriteria criteriaWithRestriction (Class<?> clazz, Expression expression, Order... orders) throws HCEErrorException{
		return criteriaWithRestriction(clazz, new Expression[]{expression}, orders);
	}
	
	/**
	 * select all the field(s) with give any restriction (detect join type) and return object will be clazz param
	 * @param clazz -> class to be selected / queried
	 * @param expression -> constraint or restrictions, can be more than one, use add method in PropertyValue class
	 * @param orders -> order(s) parameters, sequential		
	 * @return -> {@link DetachedCriteria}
	 */
	protected DetachedCriteria criteriaWithRestriction (Class<?> clazz, Expression expression, HashMap<String, JoinType> joinOverrides, Order... orders) 
			throws HCEErrorException{
		return criteriaWithRestriction(clazz, new Expression[]{expression}, joinOverrides, orders);
	}
	
	/**
	 * select based on clazz and return it to resultClass, we must declare alias to exact name of the field of resultClass, inner class is accepted
	 * e.g: employee.name
	 * @param clazz -> class to be selected / queried
	 * @param resultClass -> class to be returned
	 * @param projections -> field(s) to be selected
	 * @param alias -> alias of property that be selected, must be exact name of the field of resultClass
	 * @param expressions -> constraint or restrictions
	 * @param orders -> order(s) parameters, sequential		
	 * @return -> {@link DetachedCriteria}
	 * @throws {@link HCEErrorException}
	 */
	protected DetachedCriteria criteriaByProperty ( Class<?> clazz, Class<?> resultClass, String[] projections, 
			String[] alias, Expression[] expressions, Order... orders)throws HCEErrorException{		
		return criteriaByProperty(clazz, resultClass, projections, alias, expressions, null, orders);
	}
	
	/**
	 * select based on clazz and return it to resultClass, we must declare alias to exact name of the field of resultClass, inner class is accepted
	 * e.g: employee.name
	 * @param clazz -> class to be selected / queried
	 * @param resultClass -> class to be returned
	 * @param projections -> field(s) to be selected
	 * @param alias -> alias of property that be selected, must be exact name of the field of resultClass
	 * @param expressions -> constraint or restrictions
	 * @param joinOverrides -> endorse join type, e.g : by default inner join and want to override to left join 
	 * @param orders -> order(s) parameters, sequential		
	 * @return -> {@link DetachedCriteria}
	 * @throws {@link HCEErrorException}
	 */
	protected DetachedCriteria criteriaByProperty ( Class<?> clazz, Class<?> resultClass, String[] projections, 
			String[] alias, Expression[] expressions, HashMap<String, JoinType> joinOverrides, Order... orders)throws HCEErrorException{
		DetachedCriteria detachedCriteria = DetachedCriteria.forClass(clazz, QueryConstant.ALIAS);
		List<String> hasAddedCriteria = new ArrayList<String>();
		
		projections = QueryUtils.restructureProjections(clazz, projections);  // if projections contain *, will be extracted from fields
		alias = QueryUtils.restructureProjections(clazz, alias);
		if(projections != null && projections.length > 0){
			fetchProjections(clazz, projections, hasAddedCriteria, detachedCriteria, null, joinOverrides);	
			detachedCriteria.setProjection(QueryUtils.getInstance().buildProjections(projections, alias));
			detachedCriteria.setResultTransformer(HCETransformers.aliasToBean(resultClass));
		}
		restrictionRestrict(clazz, expressions, detachedCriteria, hasAddedCriteria);
		initializeOrder(clazz, hasAddedCriteria, orders, detachedCriteria, joinOverrides);
		return detachedCriteria;
	}
	
	/**
	 * select based on domainClass and return it to resultClass, we must declare alias exact name of the field of resultClass, inner class is accepted
	 * e.g: employee.name
	 * @param clazz -> class to be selected / queried
	 * @param resultClass -> class to be returned
	 * @param projections -> field(s) to be selected
	 * @param alias -> alias of property that be selected, must exact name of the field of resultClass
	 * @param expression -> constraint or restrictions, can be more than one, use add method in PropertyValue class
	 * @param orders -> order(s) parameters, sequential
	 * @return -> {@link DetachedCriteria}	
	 * @throws {@link HCEErrorException}
	 */
	protected DetachedCriteria criteriaByProperty ( Class<?> clazz, Class<?> resultClass, String[] projections, 
			String[] alias, Expression expression, Order... orders)throws HCEErrorException{
		return criteriaByProperty(clazz, resultClass, projections, alias, new Expression[]{expression}, orders);
	}
	
	/**
	 * select based on domainClass and return it to resultClass, we must declare alias exact name of the field of resultClass, inner class is accepted
	 * e.g: employee.name
	 * @param clazz -> class to be selected / queried
	 * @param resultClass -> class to be returned
	 * @param projections -> field(s) to be selected
	 * @param alias -> alias of property that be selected, must exact name of the field of resultClass
	 * @param expression -> constraint or restrictions, can be more than one, use add method in PropertyValue class
	 * @param orders -> order(s) parameters, sequential
	 * @return -> {@link DetachedCriteria}	
	 * @throws {@link HCEErrorException}
	 */
	protected DetachedCriteria criteriaByProperty ( Class<?> clazz, Class<?> resultClass, String[] projections, 
			String[] alias, Expression expression, HashMap<String, JoinType> joinOverrides, Order... orders)throws HCEErrorException{
		return criteriaByProperty(clazz, resultClass, projections, alias, new Expression[]{expression}, joinOverrides, orders);
	}

	/**
	 * select based on domainClass and return it to resultClass, we must declare alias exact name of the field of resultClass, inner class is accepted
	 * e.g: employee.name
	 * @param resultClass -> class to be returned
	 * @param projections -> field(s) to be selected
	 * @param alias -> alias of property that be selected, must enableLike the field of resultClass
	 * @param expression -> constraint or restrictions
	 * @param orders -> order(s) parameters, sequential
	 * @return -> {@link DetachedCriteria}	
	 * @throws {@link HCEErrorException}
	 */
	protected DetachedCriteria criteriaByProperty ( Class<?> resultClass, String[] projections, 
			String[] alias, Expression[] expressions, Order... orders)throws HCEErrorException{
		return criteriaByProperty(domainClass, resultClass, projections, alias, expressions, orders);
	}
	
	/**
	 * select based on domainClass and return it to resultClass, we must declare alias exact name of the field of resultClass, inner class is accepted
	 * e.g: employee.name
	 * @param resultClass -> class to be returned
	 * @param projections -> field(s) to be selected
	 * @param alias -> alias of property that be selected, must enableLike the field of resultClass
	 * @param expression -> constraint or restrictions
	 * @param orders -> order(s) parameters, sequential
	 * @return -> {@link DetachedCriteria}	
	 * @throws {@link HCEErrorException}
	 */
	protected DetachedCriteria criteriaByProperty ( Class<?> resultClass, String[] projections, 
			String[] alias, Expression[] expressions, HashMap<String, JoinType> joinOverrides, Order... orders)throws HCEErrorException{
		return criteriaByProperty(domainClass, resultClass, projections, alias, expressions, joinOverrides, orders);
	}
	
	/**
	 * select based on domainClass and return it to resultClass, filter based on restrictions are given
	 * @param projections -> field(s) to be selected
	 * @param expressions -> constraint or restrictions
	 * @param orders -> order(s) parameters, sequential
	 * @return -> {@link DetachedCriteria}	
	 * @throws {@link HCEErrorException}
	 */
	protected DetachedCriteria criteriaByProperty( String[] projections, Expression[] expressions, Order... orders) throws HCEErrorException{
		return criteriaByProperty(domainClass, domainClass, projections, projections, expressions, orders);
	}
	
	/**
	 * select based on domainClass and return it to resultClass, filter based on restrictions are given
	 * @param projections -> field(s) to be selected
	 * @param expressions -> constraint or restrictions
	 * @param orders -> order(s) parameters, sequential
	 * @return -> {@link DetachedCriteria}	
	 * @throws {@link HCEErrorException}
	 */
	protected DetachedCriteria criteriaByProperty( String[] projections, Expression[] expressions, HashMap<String, JoinType> joinOverrides, Order... orders)
			throws HCEErrorException{
		return criteriaByProperty(domainClass, domainClass, projections, projections, expressions, joinOverrides, orders);
	}
	
	/**
	 * select based on domainClass and return it to resultClass, we must declare alias exact name of the field of resultClass, inner class is accepted
	 * e.g: employee.name
	 * @param resultClass -> class to be returned
	 * @param projections -> field(s) to be selected
	 * @param alias -> alias of property that be selected, must enableLike the field of resultClass
	 * @param expression -> constraint or restrictions, can be more than one, use add method in {@link Expression} class
	 * @param orders -> order(s) parameters, sequential	
	 * @return -> {@link DetachedCriteria}
	 * @throws {@link HCEErrorException}
	 */
	protected DetachedCriteria criteriaByProperty ( Class<?> resultClass, String[] projections, 
			String[] alias, Expression expression, Order... orders)throws HCEErrorException{
		return criteriaByProperty(resultClass, projections, alias, new Expression[]{expression}, orders);
	}
	
	/**
	 * select based on domainClass and return it to resultClass, we must declare alias exact name of the field of resultClass, inner class is accepted
	 * e.g: employee.name
	 * @param resultClass -> class to be returned
	 * @param projections -> field(s) to be selected
	 * @param alias -> alias of property that be selected, must enableLike the field of resultClass
	 * @param expression -> constraint or restrictions, can be more than one, use add method in {@link Expression} class
	 * @param orders -> order(s) parameters, sequential	
	 * @return -> {@link DetachedCriteria}
	 * @throws {@link HCEErrorException}
	 */
	protected DetachedCriteria criteriaByProperty ( Class<?> resultClass, String[] projections, 
			String[] alias, Expression expression, HashMap<String, JoinType> joinOverrides, Order... orders)throws HCEErrorException{
		return criteriaByProperty(resultClass, projections, alias, new Expression[]{expression}, joinOverrides, orders);
	}
	
	/**
	 * select based on domainClass and return it to resultClass, filter based on restriction(s) are given
	 * @param projections -> field(s) to be selected
	 * @param expression -> constraint or restrictions, can be more than one, use add method in {@link Expression} class
	 * @param orders -> order(s) parameters, sequential
	 * @return -> {@link DetachedCriteria}	
	 * @throws {@link HCEErrorException}
	 */
	protected DetachedCriteria criteriaByProperty( String[] projections, Expression expression, Order... orders) throws HCEErrorException{
		return criteriaByProperty(domainClass, projections, projections, new Expression[]{expression}, orders);
	}
	
	/**
	 * select based on domainClass and return it to resultClass, filter based on restriction(s) are given
	 * @param projections -> field(s) to be selected
	 * @param expression -> constraint or restrictions, can be more than one, use add method in {@link Expression} class
	 * @param orders -> order(s) parameters, sequential
	 * @return -> {@link DetachedCriteria}	
	 * @throws {@link HCEErrorException}
	 */
	protected DetachedCriteria criteriaByProperty( String[] projections, Expression expression, HashMap<String, JoinType> joinOverrides, Order... orders) 
			throws HCEErrorException{
		return criteriaByProperty(domainClass, projections, projections, new Expression[]{expression}, joinOverrides, orders);
	}
	
	/**
	 * select based on clazz and return it to clazz, filter based on restriction(s) are given and return as collection of clazz
	 * @param clazz -> class to be selected / queried
	 * @param projections -> field(s) to be selected
	 * @param expressions -> constraint or restrictions, can be more than one, use add method in {@link Expression} class
	 * @param orders -> order(s) parameters, sequential	
	 * @return -> {@link DetachedCriteria}
	 * @throws {@link HCEErrorException}
	 */
	protected DetachedCriteria criteriaByProperty(Class<?> clazz, String[] projections, Expression expression, Order... orders) throws HCEErrorException{
		return criteriaByProperty(clazz, clazz, projections, projections, new Expression[]{ expression }, orders);
	}
	
	/**
	 * select based on clazz and return it to clazz, filter based on restriction(s) are given and return as collection of clazz
	 * @param clazz -> class to be selected / queried
	 * @param projections -> field(s) to be selected
	 * @param expressions -> constraint or restrictions, can be more than one, use add method in {@link Expression} class
	 * @param orders -> order(s) parameters, sequential	
	 * @return -> {@link DetachedCriteria}
	 * @throws {@link HCEErrorException}
	 */
	protected DetachedCriteria criteriaByProperty(Class<?> clazz, String[] projections, Expression[] expressions, Order... orders) throws HCEErrorException{
		return criteriaByProperty(clazz, clazz, projections, projections, expressions, orders);
	}
	
	/**
	 * select based on clazz and return it to clazz, filter based on restriction(s) are given and return as collection of clazz
	 * @param clazz -> class to be selected / queried
	 * @param projections -> field(s) to be selected
	 * @param expressions -> constraint or restrictions, can be more than one, use add method in {@link Expression} class
	 * @param orders -> order(s) parameters, sequential	
	 * @return -> {@link DetachedCriteria}
	 * @throws {@link HCEErrorException}
	 */
	protected DetachedCriteria criteriaByProperty(Class<?> clazz, String[] projections, Expression[] expressions, HashMap<String, JoinType> joinOverrides,
			Order... orders) throws HCEErrorException{
		return criteriaByProperty(clazz, clazz, projections, projections, expressions, joinOverrides, orders);
	}
	
	/**
	 * select based on domainClass and return it to resultClass, filter based on restriction(s) are given and return as collection of resultClass 
	 * @param resultClass -> class to be returned
	 * @param projections -> field/property to be selected
	 * @param alias -> alias of field/property, this must be the field of resultClass
	 * @param expressions -> constraint or restrictions, can be more than one, use add method in {@link Expression} class
	 * @param orders -> order(s) parameters, sequential	
	 * @return -> {@link DetachedCriteria}
	 * @throws {@link HCEErrorException}
	 */
	protected DetachedCriteria criteriaFetchByProperty ( Class<?> resultClass, String[] projections, 
			String[] alias, Expression[] expressions, Order... orders)throws HCEErrorException{
		DetachedCriteria detachedCriteria = DetachedCriteria.forClass(domainClass, QueryConstant.ALIAS);
		List<String> hasAddedCriteria = new ArrayList<String>();
		
		projections = QueryUtils.restructureProjections(domainClass, projections);  // if projections contain *, will be extracted from fields
		if(projections != null && projections.length > 0){
			fetchProjections(domainClass, projections, hasAddedCriteria, detachedCriteria);		
			detachedCriteria.setProjection(QueryUtils.getInstance().buildProjections(projections, alias));
			detachedCriteria.setResultTransformer(HCEUsualTransformers.aliasToBean(resultClass));
		}	
		restrictionRestrict(domainClass, expressions, detachedCriteria, hasAddedCriteria);
		initializeOrder(domainClass, hasAddedCriteria, orders, detachedCriteria, null);
		return detachedCriteria;
	}
	
	/**
	 * select based on domainClass and return it as collection of domainClass, the result is not root distinct
	 * @param projections -> field/property to be selected
	 * @param expressions -> constraint or restrictions, can be more than one, use add method in {@link Expression} class
	 * @param orders -> order(s) parameters, sequential	
	 * @return -> {@link DetachedCriteria}
	 * @throws {@link HCEErrorException}
	 */
	protected DetachedCriteria criteriaFetchByProperty( String[] projections, Expression[] expressions, Order... orders) throws HCEErrorException{
		return criteriaFetchByProperty(domainClass, projections, projections, expressions, orders);
	}
	
	/**
	 * select to clazz, only get and order, and return as collection of clazz
	 * @param clazz -> class to be selected / queried
	 * @param projections -> field(s) to be selected
	 * @param orders -> order(s) parameters, sequential	
	 * @return -> {@link DetachedCriteria}
	 * @throws {@link HCEErrorException} 
	 */
	protected DetachedCriteria criteriaWithProjections (Class<?> clazz, String[] projections, Order... orders) throws HCEErrorException{
		return criteriaByProperty(clazz, projections, new Expression[] {}, orders);
	}

	/**
	 * select to domainClass, only get and order, and return as collection of domainClass
	 * @param projections -> field(s) to be selected
	 * @param orders -> order(s) parameters, sequential	
	 * @return -> {@link DetachedCriteria}
	 * @throws {@link HCEErrorException} 
	 */
	protected DetachedCriteria criteriaWithProjections (String[] projections, Order... orders) throws HCEErrorException{
		return criteriaWithProjections(domainClass, projections, orders);
	}
	
	/**
	 * select to domainClass, only get and order, and return as collection of domainClass, the result is not root distinct
	 * @param projections -> field(s) to be selected
	 * @param orders -> order(s) parameters, sequential	
	 * @return -> {@link DetachedCriteria}
	 * @throws {@link HCEErrorException} 
	 */
	protected DetachedCriteria criteriaFetchProjections (String[] projections, Order... orders) throws HCEErrorException{
		DetachedCriteria criteria = DetachedCriteria.forClass(domainClass, QueryConstant.ALIAS);
		List<String> hasAddedCriteria = new ArrayList<String>();
		Class<?> type = domainClass;
		
		projections = QueryUtils.restructureProjections(domainClass, projections);  // if projections contain *, will be extracted from fields
		fetchProjections(type, projections, hasAddedCriteria, criteria);	
		if(projections != null && projections.length > 0){
			criteria.setProjection(QueryUtils.getInstance().buildProjections(projections));
			criteria.setResultTransformer(HCEUsualTransformers.aliasToBean(domainClass));
		}
		initializeOrder(type, hasAddedCriteria, orders, criteria, null);
		return criteria;
	}
	
	protected DetachedCriteria last(String propertyName, Expression...expressions){
		return last(domainClass, propertyName, expressions);
	}
	
	protected DetachedCriteria last(Class<?> clazz, String propertyName, Expression...expressions){
		DetachedCriteria maxDetachedCriteria = DetachedCriteria.forClass(clazz);
		List<String> hasAddedCriteria = new ArrayList<String>();
		fetchProjections(clazz, new String[]{propertyName}, hasAddedCriteria, maxDetachedCriteria);
		restrictionRestrict(clazz, expressions, maxDetachedCriteria, hasAddedCriteria);
		maxDetachedCriteria.setProjection( Projections.max(QueryUtils.getProperty(propertyName)) );
		
		DetachedCriteria detachedCriteria = DetachedCriteria.forClass(clazz);
		detachedCriteria.add(Property.forName(propertyName).eq(maxDetachedCriteria));
		return detachedCriteria;
	}
	
	protected DetachedCriteria first(String propertyName, Expression...expressions){
		return first(domainClass, propertyName, expressions);
	}
	
	protected DetachedCriteria first(Class<?> clazz, String propertyName, Expression...expressions){
		DetachedCriteria minDetachedCriteria = DetachedCriteria.forClass(clazz);
		List<String> hasAddedCriteria = new ArrayList<String>();
		fetchProjections(clazz, new String[]{propertyName}, hasAddedCriteria, minDetachedCriteria);
		restrictionRestrict(clazz, expressions, minDetachedCriteria, hasAddedCriteria);
		minDetachedCriteria.setProjection( Projections.min(QueryUtils.getProperty(propertyName)) );
		
		DetachedCriteria detachedCriteria = DetachedCriteria.forClass(clazz);
		detachedCriteria.add(Property.forName(propertyName).eq(minDetachedCriteria));
		return detachedCriteria;
	}
	
	/**
	 * this method is used for restrictions auto detection, when a object that is not null will be considered as query condition, except it is ignored in 
	 * command ignoreProperties
	 * @param searchCriteria -> {@link DetachedCriteria} select field(s)
	 * @param countCriteria -> {@link DetachedCriteria} select count
	 * @param field -> {@link Field} of object or property to be checked for restriction assignment
	 * @param object -> Object of the processing check
	 * @param level -> deep of object detection
	 * @param alias -> alias of association
	 * @throws Exception
	 */
	private void assignRestriction(DetachedCriteria searchCriteria, DetachedCriteria countCriteria, Field field, Field parentField, Object object, int level, 
			String alias, String[] ignoreProperties, boolean ignoreSensitive, boolean enableLike, List<String> hasAddedCriteria) throws Exception{	
		Transient transient1 = field.getAnnotation(Transient.class);
		if(transient1 == null){
			if(BeanUtils.isJavaType(field.getType())) {	
				String fieldName = field.getName();	
				Object fieldValue = field.get(object);
				String property = level > 0 ? alias + (level-1) + "." + fieldName : QueryConstant.ALIAS + "." + fieldName;
				if( /*!("id".equals(fieldName) && level == 0) && */ QueryUtils.isValidField(fieldName, fieldValue) && 
						!QueryUtils.isIgnored(ignoreProperties, property, fieldName, ignoreSensitive)) {
					if(level == 1) { //the statement in the domain class, so we use this as alias
						if(!hasAddedCriteria.contains(alias + (level-1)) && !alias.equals(QueryConstant.ALIAS) && StringUtils.isNotEmpty(parentAlias)){
							JoinType joinType = QueryUtils.getJoinType(parentClass, fieldName);
							hasAddedCriteria.add(alias + (level-1));
							searchCriteria.createAlias(QueryConstant.ALIAS + "." + alias, alias + (level-1), joinType);
							searchCriteria.setFetchMode(QueryConstant.ALIAS + "." + alias, FetchMode.JOIN);
							countCriteria.createAlias(QueryConstant.ALIAS + "." + alias, alias + (level-1), joinType);
							log.info("{} Join to : {}", QueryUtils.getJoinName(joinType), QueryConstant.ALIAS + "." + alias, alias + (level-1) );
						}
					}else if(level == 2){ //the statement should be use class + "0"
						if(!hasAddedCriteria.contains(alias + (level-1)) && !alias.equals(QueryConstant.ALIAS) && StringUtils.isNotEmpty(parentAlias)){
							JoinType joinType = QueryUtils.getJoinType(parentClass, fieldName);
							hasAddedCriteria.add(alias + (level-1));
							searchCriteria.createAlias(parentAlias + (level-2) + "." + alias, alias + (level-1), joinType);
							searchCriteria.setFetchMode(parentAlias + (level-2) + "." + alias, FetchMode.JOIN);
							countCriteria.createAlias(parentAlias + (level-2) + "." + alias, alias + (level-1), joinType);
							log.info("{} Join to : {}", QueryUtils.getJoinName(joinType), parentAlias + (level-2) + "." + alias, alias + (level-1) );
						}
					}else if(level > 2){
						if(!hasAddedCriteria.contains(alias + (level-1)) && !alias.equals(QueryConstant.ALIAS) && StringUtils.isNotEmpty(grandParentAlias)){
							JoinType joinType = QueryUtils.getJoinType(parentClass, fieldName);
							hasAddedCriteria.add(alias + (level-1));
							searchCriteria.createAlias(parentAlias + (level-2) + "." + alias, alias + (level-1), joinType);
							searchCriteria.setFetchMode(parentAlias + (level-2) + "." + alias, FetchMode.JOIN);
							countCriteria.createAlias(parentAlias + (level-2) + "." + alias, alias + (level-1), joinType);
							log.info("{} Join to : {}", QueryUtils.getJoinName(joinType), (parentAlias + (level-2)) + "." + alias, alias + (level-1) );
							
//							searchCriteria.createAlias(grandParentAlias + (level-2) + "." + alias, alias + (level-1), joinType);
//							countCriteria.createAlias(grandParentAlias + (level-2) + "." + alias, alias + (level-1), joinType);
//							log.info("{} Join to : {}", QueryUtils.getJoinName(joinType), (grandParentAlias + (level-2)) + "." + alias, alias + (level-1) );
						}
					}
				
					if(ArrayUtils.contains(QueryConstant.eqFields, field.getType().toString()) || field.getType().isEnum()){
						log.info("Search for field : {}, type : EQUAL, value : {} ", property, fieldValue);
						searchCriteria.add(Restrictions.eq(property, fieldValue));
						countCriteria.add(Restrictions.eq(property, fieldValue));
					}else{
						if(enableLike){
							log.info("Search for field : {}, type : LIKE, value : {} ", property, fieldValue);
							searchCriteria.add(Restrictions.like(property, fieldValue.toString(), MatchMode.ANYWHERE));
							countCriteria.add(Restrictions.like(property, fieldValue.toString(), MatchMode.ANYWHERE));
						}else{
							log.info("Search for field : {}, type : EQUAL, value : {} ", property, fieldValue);
							searchCriteria.add(Restrictions.eq(property, fieldValue.toString()));
							countCriteria.add(Restrictions.eq(property, fieldValue.toString()));
						}
					}
				}
			}else{	
				try {
					Object fieldValue = field.get(object);			
					if(fieldValue != null && !(fieldValue instanceof Collection<?>) && !BeanUtils.isTypeByPass(fieldValue.getClass())){
						Field[] fields = ReflectionUtils.getInheritedPrivateFields(fieldValue.getClass()); // fieldValue.getClass().getDeclaredFields();
						parentClass = field.getType();		
						if(level == 1){
							grandParentAlias = field.getName();
						}
						
						if(level == 0 || parentField == null){
							parentAlias = QueryConstant.ALIAS;
						}else{
							parentAlias = parentField.getName();
						}
						
						for (Field field2 : fields) {
							if(!ReflectionUtils.isStatic(field2)){
	//						System.out.println("Field Name = "+fieldValue.getClass().getCanonicalName() + " .... " +field2.getName());
								field2.setAccessible(true);
								assignRestriction(searchCriteria, countCriteria, field2, field, fieldValue, level + 1, field.getName(), ignoreProperties, ignoreSensitive, 
										enableLike, hasAddedCriteria);
							}
						}		
						//parentClass = field.getType();
						
					}
				} catch (Exception e) {
					log.error("assignment value fail, caused by : {}", e.getMessage());
					throw new Exception(e);
				}
			}
		}
	}
	
	/**
	 * this method called by queryTransformer based on object behavior
	 * this criteria alias should be synchronized with QueryUtils initialUsualProperty (alias created based on level)
	 * @param searchCriteria -> {@link DetachedCriteria} select field(s)
	 * @param countCriteria -> {@link DetachedCriteria} select count
	 * @param object -> Object of the processing check
	 * @param orders -> order(s) parameter
	 * @param projections -> field(s) to be selected
	 * @throws {@link HCEErrorException}
	 */
	private void queryTransformation(Class<?> clazz, DetachedCriteria searchCriteria, DetachedCriteria countCriteria, Object object, 
			Order[] orders, String[] projections, String[] ignoreProperties, String[] addJoins, boolean ignoreSensitive, boolean enableLike, 
			HashMap<String, JoinType> joinOverrides) throws HCEErrorException{
		try{
			List<String> hasAddedCriteria = new ArrayList<String>();
			if(projections != null && projections.length > 0){
				hasAddedCriteria = joinBuilder(searchCriteria, countCriteria, orders, projections, clazz, joinOverrides, addJoins);
			}
			parentClass = clazz;
			if(object != null){
				if(ignoreProperties == null){
					ignoreProperties = new String[0];
				}
				ignoreProperties = QueryUtils.convertToLevel(ignoreProperties);
				Field[] fields = ReflectionUtils.getInheritedPrivateFields(clazz); //clazz.getDeclaredFields();
				for (Field field : fields) {		
					if(!ReflectionUtils.isStatic(field)){
						field.setAccessible(true);
						parentAlias = field.getName();
						assignRestriction(searchCriteria, countCriteria, field, null, object, 0, field.getName(),  ignoreProperties, 
								ignoreSensitive, enableLike, hasAddedCriteria);
					}
				}				
			}
		} catch (Exception e) {
			throw new HCEErrorException(e);
		}
	}	
	
	/**
	 * @param searchCriteria -> {@link DetachedCriteria} select field(s)
	 * @param orders -> order(s) parameter
	 * @param projections -> field(s) to be selected
	 * @param theClass -> class to be detect
	 * @throws {@link HCEErrorException}
	 * detect association and create the join
	 */
	private List<String> joinBuilder(DetachedCriteria searchCriteria, DetachedCriteria countCriteria,
			Order[] orders, String[] projections, Class<?> theClass, HashMap<String, JoinType> joinOverrides, String[] addJoins) throws HCEErrorException{
		try{			
			/* when projections define inner class property/s, then join the table to avoid cannot resolved property */
			String[] separateProperties;
			DetachedCriteria detachedCriteria = searchCriteria;
			DetachedCriteria detachedCountCriteria = countCriteria;
			List<String> hasAddedCriteria = new ArrayList<String>();
			Class<?> cacheClass;
			for (String property : projections) {
				cacheClass = theClass;
				separateProperties = StringUtils.split(property, ".");
				if(separateProperties.length > 1){
					for(int i = 0; i < separateProperties.length - 1; i++){
						cacheClass = joinDefinition(cacheClass, separateProperties, i, detachedCountCriteria, detachedCriteria, hasAddedCriteria, joinOverrides);
						
//						if(!hasAddedCriteria.contains(separateProperties[i] + i)){	
//							//see parent join type e.g: Leave.nextApproval.employee 
//							//when join to employee we must get the join type nextApproval to Leave
//							JoinType joinType;
//							if(joinOverrides != null && joinOverrides.containsKey(separateProperties[i])){
//								joinType = joinOverrides.get(separateProperties[i]);
//							}else{
//								joinType = QueryUtils.getJoinType(cacheClass, separateProperties[i]);
//							}
//							
//							if(i == 0){
//								detachedCriteria = searchCriteria.createAlias(separateProperties[0], separateProperties[0] + 0, joinType);
//								detachedCountCriteria = countCriteria.createAlias(separateProperties[0], separateProperties[0] + 0, joinType);								
//							}else{
//								detachedCriteria.createAlias(separateProperties[i-1] + (i - 1) + "." + separateProperties[i], separateProperties[i] + i, joinType);	
//								detachedCountCriteria.createAlias(separateProperties[i-1] + (i - 1) +"." + separateProperties[i], separateProperties[i] + i, joinType);	
//							}
//							hasAddedCriteria.add(separateProperties[i] + i);
//							log.info("{} Join to : {}", QueryUtils.getJoinName(joinType), separateProperties[i] + i);
//						}
//							
//						Field field = ReflectionUtils.findField(cacheClass, separateProperties[i]);
//						cacheClass = field.getType();
//						if (Collection.class.isAssignableFrom(cacheClass)) {		
//							Type chieldType = field.getGenericType();   
//						    if (chieldType instanceof ParameterizedType) {  
//						        ParameterizedType pt = (ParameterizedType) chieldType;  
//						        cacheClass = (Class<?>) (pt.getActualTypeArguments()[0]); 
//						    }  
//						}	
					}					
				}
			}
			
			if(addJoins != null){
				for(String joinAssociation : addJoins){
					cacheClass = theClass;
					separateProperties = StringUtils.split(joinAssociation, ".");
					if(separateProperties.length > 1){
						for(int i = 0; i < separateProperties.length; i++){
							cacheClass = joinDefinition(cacheClass, separateProperties, i, detachedCountCriteria, detachedCriteria, hasAddedCriteria, joinOverrides);
						}
					}
				}
			}
			
			initializeOrder(theClass, hasAddedCriteria, orders, detachedCriteria, joinOverrides);
			return hasAddedCriteria;
		} catch (Exception e) {
			throw new HCEErrorException(e);
		}
	}
	
	private Class<?> joinDefinition(Class<?> cacheClass, String[] separateProperties, int i,  DetachedCriteria detachedCountCriteria, DetachedCriteria detachedCriteria, 
			List<String> hasAddedCriteria, HashMap<String, JoinType> joinOverrides) throws SecurityException, NoSuchFieldException{
		if(!hasAddedCriteria.contains(separateProperties[i] + i)){	
			//see parent join type e.g: Leave.nextApproval.employee 
			//when join to employee we must get the join type nextApproval to Leave
			JoinType joinType;
			if(joinOverrides != null && joinOverrides.containsKey(separateProperties[i])){
				joinType = joinOverrides.get(separateProperties[i]);
			}else{
				joinType = QueryUtils.getJoinType(cacheClass, separateProperties[i]);
			}
			
			if(i == 0){
				detachedCriteria.createAlias(separateProperties[0], separateProperties[0] + 0, joinType);
				detachedCriteria.setFetchMode(separateProperties[0], FetchMode.JOIN);
				
				detachedCountCriteria.createAlias(separateProperties[0], separateProperties[0] + 0, joinType);								
			}else{
				detachedCriteria.createAlias(separateProperties[i-1] + (i - 1) + "." + separateProperties[i], separateProperties[i] + i, joinType);
				detachedCriteria.setFetchMode(separateProperties[i-1] + (i - 1) + "." + separateProperties[i], FetchMode.JOIN);
				detachedCountCriteria.createAlias(separateProperties[i-1] + (i - 1) +"." + separateProperties[i], separateProperties[i] + i, joinType);	
			}
			hasAddedCriteria.add(separateProperties[i] + i);
			log.info("{} Join to : {}", QueryUtils.getJoinName(joinType), separateProperties[i] + i);
		}
			
		Field field = ReflectionUtils.findField(cacheClass, separateProperties[i]);
		cacheClass = field.getType();
		if (Collection.class.isAssignableFrom(cacheClass)) {		
			Type chieldType = field.getGenericType();   
		    if (chieldType instanceof ParameterizedType) {  
		        ParameterizedType pt = (ParameterizedType) chieldType;  
		        cacheClass = (Class<?>) (pt.getActualTypeArguments()[0]); 
		    }  
		}
		
		return cacheClass;
	}
	
	/**
	 * @param clazz
	 * @param properties
	 * @param hasAddedCriteria
	 * @param criteria
	 * @throws {@link HCEErrorException}
	 */
	private void fetchProjections(Class<?> clazz, String[] properties, List<String> hasAddedCriteria, DetachedCriteria criteria, HashMap<String, JoinType> joinOverrides) throws HCEErrorException{
		fetchProjections(clazz, properties, hasAddedCriteria, criteria, null, joinOverrides);
	}
	
	/**
	 * @param clazz
	 * @param properties
	 * @param hasAddedCriteria
	 * @param criteria
	 * @throws {@link HCEErrorException}
	 */
	private void fetchProjections(Class<?> clazz, String[] properties, List<String> hasAddedCriteria, DetachedCriteria criteria) throws HCEErrorException{
		fetchProjections(clazz, properties, hasAddedCriteria, criteria, null, null);
	}
	
	/**
	 * @param clazz
	 * @param properties
	 * @param hasAddedCriteria
	 * @param criteria
	 * @param countCriteria
	 * @throws {@link HCEErrorException} 
	 */
	private void fetchProjections(Class<?> clazz, String[] properties, List<String> hasAddedCriteria, DetachedCriteria criteria, DetachedCriteria countCriteria, 
			HashMap<String, JoinType> joinOverrides) 
			throws HCEErrorException{
		Class<?> type = clazz;
		if(properties != null){
			for (String property : properties) {			
				String[] projections = StringUtils.split(property, ".");
				int len = projections.length;
				if(len > 1){
					for (int i= 0; i < projections.length -1; i++) {
						if(!hasAddedCriteria.contains(projections[i] + i)){  // workShiftOvertime.overtimeRoundConversions.xyzComponents.componentConversion.id
							if(i > 0){						
								Field field, childField = null;
								try {
									field = ReflectionUtils.findField(type, projections[i-1]);
									type = field.getType();
									if(ReflectionUtils.isCollection(field.getType())) {
										 Type chieldType = field.getGenericType();   
									        if (chieldType instanceof ParameterizedType) {  
									            ParameterizedType pt = (ParameterizedType) chieldType;  
									            type = (Class<?>) (pt.getActualTypeArguments()[0]); 
									        }  
									}									
									childField = ReflectionUtils.findField(type, projections[i]);
								} catch (Exception e) {
									log.error("cannot find field name : {}, class : {}", projections[i], type.getCanonicalName());
									throw new HCEErrorException("cannot find field name : "+projections[i] +", class : "+ type.getCanonicalName(), e);
								} 
								JoinType joinType;
								try {
									joinType = QueryUtils.getJoinType(childField, projections, joinOverrides);
								} catch (Exception e) {
									log.error("cannot get join type field name : {}, class : {}", childField.getName(), type.getCanonicalName());
									throw new HCEErrorException("cannot get join type field name : "+childField.getName() +", class : "+ type.getCanonicalName(), e);
								} 
								criteria = criteria.createAlias(projections[i-1] + (i-1) + "." + projections[i], projections[i] + i, joinType);
								criteria.setFetchMode(projections[i-1] + (i-1) + "." + projections[i], FetchMode.JOIN);
								if(countCriteria != null){
									countCriteria = countCriteria.createAlias(projections[i-1] + (i-1) + "." + projections[i], projections[i] + i, joinType);
								}
								log.info("{} Join to : {}", QueryUtils.getJoinName(joinType), projections[i] + i);
							}else{						
								Field field = null;
								try {
									field = ReflectionUtils.findField(clazz, projections[i]);
								} catch (Exception e) {
									log.error("cannot find field name : {} from class : {} ", projections[i], clazz.getCanonicalName());
									throw new HCEErrorException("cannot find field name : "+projections[i] +", class : "+ clazz.getCanonicalName(), e);
								} 
								JoinType joinType;
								try {
									joinType = QueryUtils.getJoinType(field, projections, joinOverrides);
								} catch (Exception e) {
									log.error("cannot get join type field name : {}, class : {}", field.getName(), type.getCanonicalName());
									throw new HCEErrorException("cannot get join type field name : "+field.getName() +", class : "+ type.getCanonicalName(), e);
								} 
								criteria = criteria.createAlias(projections[i], projections[i] + i, joinType);
								criteria.setFetchMode(projections[i], FetchMode.JOIN);
								if(countCriteria != null){
									countCriteria = countCriteria.createAlias(projections[i], projections[i] + i, joinType);
								}
								log.info("{} Join to : {}", QueryUtils.getJoinName(joinType), projections[i] + i);
							}
							hasAddedCriteria.add(projections[i] + i);
						}else{
							if(i == 0){
								type = clazz;
							}else{
								try {
									Field field = null;
									try {
										field = ReflectionUtils.findField(type, projections[i-1]);
										type = field.getType();
										if(ReflectionUtils.isCollection(field.getType())){
											 Type chieldType = field.getGenericType();   
										        if (chieldType instanceof ParameterizedType) {  
										            ParameterizedType pt = (ParameterizedType) chieldType;  
										            type = (Class<?>) (pt.getActualTypeArguments()[0]); 
										        }  
										}									
									} catch (Exception e) {
										log.error("cannot find field name : {}, class : {}", projections[i], type.getCanonicalName());
										throw new HCEErrorException("cannot find field name : "+projections[i] +", class : "+ type.getCanonicalName(), e);
									} 
//									type = ReflectionUtils.findField(type, projections[i-1]).getType();
								} catch (SecurityException e) {
									log.error("cannot find field name : {} from class : {} ", projections[i-1], type.getCanonicalName());
									throw new HCEErrorException("cannot find field name : "+projections[i-1] +", class : "+ type.getCanonicalName(), e);
								}
							}
						}
					}
					type = clazz;
				}
			}	
		}
	}
	
	/**
	 * @param clazz
	 * @param expressions
	 * @param detachedCriteria
	 * @param hasAddedCriteria
	 */
	private void restrictionRestrict(Class<?> clazz, Expression[] expressions, DetachedCriteria detachedCriteria, 
			List<String> hasAddedCriteria){
//		restrictionRestrict(clazz, expressions, detachedCriteria, null, hasAddedCriteria);
		applyRestriction(clazz, expressions, detachedCriteria, null, hasAddedCriteria);
	}
	
	/**
	 * @param hasAddedCriteria
	 * @param orders
	 * @param detachedCriteria
	 */
	private void initializeOrder(Class<?> clazz, List<String> hasAddedCriteria, Order[] orders, DetachedCriteria detachedCriteria, HashMap<String, JoinType> joinOverrides){
		Class<?> type = clazz;
		if(orders != null && orders.length > 0){
			for (Order declaredOrder : orders) {
				for(Order order: declaredOrder.getOrders()){
					String[] projections = StringUtils.split(order.getProperty(), ".");
					if(projections.length > 1){
						for (int i=0; i<(projections.length - 1); i++) {
							if(!hasAddedCriteria.contains(projections[i] + i)){
								if(i > 0){	
									Field field, childField = null;
									try {
										field = ReflectionUtils.findField(type, projections[i-1]);
										type = field.getType();
										if(ReflectionUtils.isCollection(field.getType())){
											 Type chieldType = field.getGenericType();   
										        if (chieldType instanceof ParameterizedType) {  
										            ParameterizedType pt = (ParameterizedType) chieldType;  
										            type = (Class<?>) (pt.getActualTypeArguments()[0]); 
										        }  
										}									
										childField = ReflectionUtils.findField(type, projections[i]);
									} catch (Exception e) {
										log.error("cannot find field name : {}, class : {}", projections[i], type.getCanonicalName());
										throw new HCEErrorException("cannot find field name : "+projections[i] +", class : "+ type.getCanonicalName(), e);
									} 
									JoinType joinType;
									try {
										joinType = QueryUtils.getJoinType(childField, projections, joinOverrides);
									} catch (Exception e) {
										log.error("cannot get join type field name : {}, class : {}", childField.getName(), type.getCanonicalName());
										throw new HCEErrorException("cannot get join type field name : "+childField.getName() +", class : "+ type.getCanonicalName(), e);
									} 
									// when deep two or more, alias index will be included
									detachedCriteria.createAlias(projections[i-1] + (i-1) + "." + projections[i], projections[i] + i, joinType);
//									detachedCriteria.setFetchMode(projections[i-1] + (i-1) + "." + projections[i], FetchMode.JOIN);
									log.info("{} Join to : {}", QueryUtils.getJoinName(joinType), projections[i] + i);
								}else{			
									Field field = null;
									try {
										field = ReflectionUtils.findField(clazz, projections[i]);
									} catch (Exception e) {
										log.error("cannot find field name : {} from class : {} ", projections[i], clazz.getCanonicalName());
										throw new HCEErrorException("cannot find field name : "+projections[i] +", class : "+ clazz.getCanonicalName(), e);
									} 
									JoinType joinType;
									try {
										joinType = QueryUtils.getJoinType(field, projections, joinOverrides);
									} catch (Exception e) {
										log.error("cannot get join type field name : {}, class : {}", field.getName(), type.getCanonicalName());
										throw new HCEErrorException("cannot get join type field name : "+field.getName() +", class : "+ type.getCanonicalName(), e);
									} 
									detachedCriteria.createAlias(projections[i], projections[i] + i, joinType);
									log.info("{} Join to : {}", QueryUtils.getJoinName(joinType), projections[i] + i);
								}
								hasAddedCriteria.add(projections[i] + i);					
							}
						}
						if (order.getOrderType() == OrderType.Asc){
							detachedCriteria.addOrder(org.hibernate.criterion.Order.asc(QueryUtils.getProperty(order.getProperty())));
						}
						else{
							detachedCriteria.addOrder(org.hibernate.criterion.Order.desc(QueryUtils.getProperty(order.getProperty())));
						}
					}else{
						if (order.getOrderType() == OrderType.Asc){
							detachedCriteria.addOrder(org.hibernate.criterion.Order.asc(QueryUtils.getProperty(order.getProperty())));
						}
						else{
							detachedCriteria.addOrder(org.hibernate.criterion.Order.desc(QueryUtils.getProperty(order.getProperty())));
						}
					}
				}
			}
		}
	}
}
