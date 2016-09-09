package softtech.hong.hce.utils;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.criterion.Conjunction;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Restrictions;
import org.hibernate.sql.JoinType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import softtech.hong.hce.criterion.BetweenPropertyExpression;
import softtech.hong.hce.criterion.DateEqProperty;
import softtech.hong.hce.criterion.MonthEqProperty;
import softtech.hong.hce.criterion.PropertyBetweenExpression;
import softtech.hong.hce.exception.HCEErrorException;
import softtech.hong.hce.model.Disjunction;
import softtech.hong.hce.model.Expression;

public class CriterionUtils {
	
	private static Logger log = LoggerFactory.getLogger(CriterionUtils.class);
	
	/**
	 * @param expression
	 * @return org.hibernate.criterion.Criterion
	 * switch the condition for restrictions
	 */
	public static Criterion getCriterion(Expression expression, Class<?> clazz, DetachedCriteria detachedCriteria, 
			DetachedCriteria detachedCriteria2, List<String> hasAddedCriteria){
		
		buildJoinExpression(expression, clazz, detachedCriteria, detachedCriteria2, hasAddedCriteria);
		expression.setPropertyName(QueryUtils.getProperty(expression.getPropertyName()));
		
		Criterion criterion;
		switch (expression.getRestrictionType()) {
		case EQ:
			criterion = Restrictions.eq(expression.getPropertyName(), expression.getValue());
			break;
		case EQ_PROPERTY:
			buildJoinExpressionSpecialExpression(expression, clazz, detachedCriteria, detachedCriteria2, hasAddedCriteria);
			criterion = Restrictions.eqProperty(expression.getPropertyName(), QueryUtils.getProperty(expression.getValue().toString()));
			break;
		case GT:
			criterion = Restrictions.gt(expression.getPropertyName(), expression.getValue());
			break;
		case GT_PROPERTY:
			buildJoinExpressionSpecialExpression(expression, clazz, detachedCriteria, detachedCriteria2, hasAddedCriteria);
			criterion = Restrictions.gtProperty(expression.getPropertyName(), QueryUtils.getProperty(expression.getValue().toString()));
			break;
		case GE:
			criterion = Restrictions.ge(expression.getPropertyName(), expression.getValue());
			break;
		case GE_PROPERTY:
			buildJoinExpressionSpecialExpression(expression, clazz, detachedCriteria, detachedCriteria2, hasAddedCriteria);
			criterion = Restrictions.geProperty(expression.getPropertyName(), QueryUtils.getProperty(expression.getValue().toString()));
			break;
		case LT:
			criterion = Restrictions.lt(expression.getPropertyName(), expression.getValue());
			break;
		case LT_PROPERTY:
			buildJoinExpressionSpecialExpression(expression, clazz, detachedCriteria, detachedCriteria2, hasAddedCriteria);
			criterion = Restrictions.ltProperty(expression.getPropertyName(), QueryUtils.getProperty(expression.getValue().toString()));
			break;
		case LE:
			criterion = Restrictions.le(expression.getPropertyName(), expression.getValue());
			break;
		case LE_PROPERTY:
			buildJoinExpressionSpecialExpression(expression, clazz, detachedCriteria, detachedCriteria2, hasAddedCriteria);
			criterion = Restrictions.leProperty(expression.getPropertyName(), QueryUtils.getProperty(expression.getValue().toString()));
			break;
		case NE:
			criterion = Restrictions.ne(expression.getPropertyName(), expression.getValue());
			break;
		case NE_PROPERTY:
			buildJoinExpressionSpecialExpression(expression, clazz, detachedCriteria, detachedCriteria2, hasAddedCriteria);
			criterion = Restrictions.neProperty(expression.getPropertyName(), QueryUtils.getProperty(expression.getValue().toString()));
			break;
		case ILIKE:
			if(expression.getMatchMode() == null){
				criterion = Restrictions.ilike(expression.getPropertyName(), expression.getValue());
			}else{
				criterion = Restrictions.ilike(expression.getPropertyName(), expression.getValue().toString(), expression.getMatchMode());
			}
			break;
		case LIKE:
			if(expression.getMatchMode() == null){
				criterion = Restrictions.like(expression.getPropertyName(), expression.getValue());
			}else{
				criterion = Restrictions.like(expression.getPropertyName(), expression.getValue().toString(), expression.getMatchMode());
			}
			break;
		case IN:
			if(expression.getValue() instanceof Collection<?>){
				criterion = Restrictions.in(expression.getPropertyName(),(Collection<?>) expression.getValue());
			}else{
				criterion = Restrictions.in(expression.getPropertyName(), (Object[]) expression.getValue());
			}
			break;
		case EMPTY:
			criterion = Restrictions.isEmpty(expression.getPropertyName());
			break;
		case NOT_EMPTY:
			criterion = Restrictions.isNotEmpty(expression.getPropertyName());
			break;
		case NULL:
			criterion = Restrictions.isNull(expression.getPropertyName());
			break;
		case NOT_NULL:
			criterion = Restrictions.isNotNull(expression.getPropertyName());
			break;
		case OR: 
			Disjunction disjunction = Expression.disjunction();
			for (Expression expression2 : expression.getExpressions()) {
				disjunction.add(getCriterion(expression2, clazz, detachedCriteria, detachedCriteria2, hasAddedCriteria));
			}
			criterion = disjunction;
			break;
		case AND:
			Conjunction conjunction = Expression.conjunction();
			for (Expression expression2 : expression.getExpressions()) {
				conjunction.add(getCriterion(expression2, clazz, detachedCriteria, detachedCriteria2, hasAddedCriteria));
			}
			criterion = conjunction;
			break;
		case BETWEEN:
			criterion = Restrictions.between(expression.getPropertyName(), expression.getValue(), expression.getValue1());
			break;
		case SIZE_EQ:
			criterion = Restrictions.sizeEq(expression.getPropertyName(), (Integer) expression.getValue());
			break;
		case SIZE_GT:
			criterion = Restrictions.sizeGt(expression.getPropertyName(), (Integer) expression.getValue());
			break;
		case SIZE_GE:
			criterion = Restrictions.sizeGe(expression.getPropertyName(), (Integer) expression.getValue());
			break;
		case SIZE_LE:
			criterion = Restrictions.sizeLe(expression.getPropertyName(), (Integer) expression.getValue());
			break;
		case SIZE_LT:
			criterion = Restrictions.sizeLt(expression.getPropertyName(), (Integer) expression.getValue());
			break;
		case SIZE_NE:
			criterion = Restrictions.sizeNe(expression.getPropertyName(), (Integer) expression.getValue());
			break;
		case EQ_OR_IS_NULL:
			criterion = Restrictions.eqOrIsNull(expression.getPropertyName(), expression.getValue());
			break;
		case DATE_EQ:
			criterion = new DateEqProperty(expression.getPropertyName(), expression.getValue().toString(), expression.getValue1().toString());
			break;
		case NOT:
			criterion = Restrictions.not(getCriterion(expression, clazz, detachedCriteria, detachedCriteria2, hasAddedCriteria));
			break;
		case PROPERTY_BETWEEN:
			criterion = new PropertyBetweenExpression(expression.getPropertyName(), expression.getValue().toString(), expression.getValue1().toString());
			break;
		case BETWEEN_PROPERTY:
			buildJoinExpressionSpecialExpression(expression, clazz, detachedCriteria, detachedCriteria2, hasAddedCriteria);
			criterion = new BetweenPropertyExpression(expression.getPropertyName(), QueryUtils.getProperty(expression.getValue().toString()), 
					QueryUtils.getProperty(expression.getValue1().toString()));
			break;
		case MONTH_EQ:
			criterion = new MonthEqProperty(expression.getPropertyName(), (Date) expression.getValue());
			break;
		default:
			criterion = Restrictions.eq(expression.getPropertyName(), expression.getValue());
			break;
		}
		return criterion;
	}
	
	private static void buildJoinExpression(Expression expression, Class<?> clazz, DetachedCriteria detachedCriteria, DetachedCriteria detachedCriteria2, 
			List<String> hasAddedCriteria){
		//for AND and OR, it has member expressions in it, e.g: Expression one and Expression two and Expression ...
		switch (expression.getRestrictionType()) {
		case AND : 
		case OR:
			for (Expression memberExpression : expression.getExpressions()) { 
				buildJoinExpression(memberExpression, clazz, detachedCriteria, detachedCriteria2, hasAddedCriteria);
			}
			break;

		default:
			buildJoinIfNotAvailable(expression.getPropertyName(), clazz, detachedCriteria, detachedCriteria2, hasAddedCriteria);
			break;
		}
	}
	
	private static void buildJoinExpressionSpecialExpression(Expression expression, Class<?> clazz, DetachedCriteria detachedCriteria, DetachedCriteria detachedCriteria2, 
			List<String> hasAddedCriteria){
		//for AND and OR, it has member expressions in it, e.g: Expression one and Expression two and Expression ...
		switch (expression.getRestrictionType()) {
		case AND : 
		case OR:
			for (Expression memberExpression : expression.getExpressions()) { 
				buildJoinExpression(memberExpression, clazz, detachedCriteria, detachedCriteria2, hasAddedCriteria);
			}
			break;

		default:
			buildJoinIfNotAvailable(expression.getValue().toString(), clazz, detachedCriteria, detachedCriteria2, hasAddedCriteria);
			break;
		}
	}
	
	public static void buildJoinIfNotAvailable(String propertyName, Class<?> clazz, DetachedCriteria detachedCriteria, DetachedCriteria detachedCriteria2, 
			List<String> hasAddedCriteria){
		
		if(StringUtils.isEmpty(propertyName)){
			throw new HCEErrorException("Empty field name, please check the expression property ");
		}
		
		String[] props = StringUtils.split(propertyName, ".");
		
		int len = 0;
		if(props != null) {
			len = props.length;
		}
		
		Class<?> type = clazz;
		//has split name
		if(len > 1){			
			for (int i= 0; i < len -1; i++) {
				Field field = null, childField = null;
				if(i > 0){								
					field = ReflectionUtils.findField(type, props[i-1]);
					type = field.getType();
				}
				if(!hasAddedCriteria.contains(props[i] + i)){
					if(i > 0){
						try {
							if(ReflectionUtils.isCollection(field.getType())){
								 Type chieldType = field.getGenericType();   
							        if (chieldType instanceof ParameterizedType) {  
							            ParameterizedType pt = (ParameterizedType) chieldType;  
							            type = (Class<?>) (pt.getActualTypeArguments()[0]); 
							        }  
							}									
							childField = ReflectionUtils.findField(type, props[i]);
						} catch (Exception e) {
							log.error("cannot find field name : {}, class : {}", props[i], type.getCanonicalName());
							throw new HCEErrorException("cannot find field name : "+props[i] +", class : "+ type.getCanonicalName(), e);
						} 
						JoinType joinType;
						try {
							joinType = QueryUtils.getJoinType(childField);
						} catch (Exception e) {
							log.error("cannot get join type field name : {}, class : {}", childField.getName(), type.getCanonicalName());
							throw new HCEErrorException("cannot get join type field name : "+childField.getName() +", class : "+ type.getCanonicalName(), e);
						} 			
						// when deep two or more, alias index will be included
						detachedCriteria.createAlias(props[i-1] + (i-1) + "." + props[i], props[i] + i, joinType);	
						if(detachedCriteria2 != null){
							detachedCriteria2.createAlias(props[i-1] + (i-1) + "." + props[i], props[i] + i, joinType);	
						}
					}else{	
						try {
							field = ReflectionUtils.findField(clazz, props[i]);
						} catch (Exception e) {
							log.error("cannot find field name : {} from class : {} ", props[i], clazz.getCanonicalName());
							throw new HCEErrorException("cannot find field name : "+props[i] +", class : "+ clazz.getCanonicalName(), e);
						} 
						JoinType joinType;
						try {
							joinType = QueryUtils.getJoinType(field);
						} catch (Exception e) {
							log.error("cannot get join type field name : {}, class : {}", field.getName(), type.getCanonicalName());
							throw new HCEErrorException("cannot get join type field name : "+field.getName() +", class : "+ type.getCanonicalName(), e);
						} 
						detachedCriteria.createAlias(props[i], props[i] + i, joinType);
						if(detachedCriteria2 != null){
							detachedCriteria2.createAlias(props[i], props[i] + i, joinType);
						}
					}
					hasAddedCriteria.add(props[i] + i);
				}
			}	
		} 
		
	}
}
