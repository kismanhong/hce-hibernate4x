package softtech.hong.hce.core;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.hibernate.HibernateException;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.internal.CriteriaImpl;
import org.hibernate.internal.CriteriaImpl.Subcriteria;

import softtech.hong.hce.exception.HCEErrorException;
import softtech.hong.hce.model.Expression;
import softtech.hong.hce.utils.CriterionUtils;

/**
 * @author Kisman Hong
 * add Restrictions to detached criteria(s)
 */
public abstract class QueryTransformerExt { //extends CommonMethod{
	
//	private static Logger log = LoggerFactory.getLogger(QueryTransformerExt.class);
	
//	private static final RestrictionType[] PROPERTY_RESTRICTION_TYPES = { RestrictionType.EQ_PROPERTY, RestrictionType.GT_PROPERTY, RestrictionType.GE_PROPERTY, 
//		RestrictionType.LT_PROPERTY, RestrictionType.LE_PROPERTY, RestrictionType.NE_PROPERTY};
//	
//	private static final RestrictionType[] SPECIAL_RESTRICTION_TYPES = {RestrictionType.AND, RestrictionType.OR, RestrictionType.NOT};
	
	/**
	 * add restrictions, if the association cannot be found, it will created automatically
	 * @param detachedCriteria -> Array of {@link DetachedCriteria} to be add a restriction
	 * @param expressions -> Restriction
	 * @throws HibernateException
	 */
	protected void addExpression(DetachedCriteria detachedCriteria, Expression... expressions) throws HibernateException {
//		boolean isLogged = true;
		
		List<String> hasAddedCriteria = new ArrayList<String>();
//		Class<?> type, clazz;
		Class<?> clazz;
		try {
			Field field = detachedCriteria.getClass().getDeclaredField("impl");
			field.setAccessible(true);
			CriteriaImpl criteriaImpl = (CriteriaImpl) field.get(detachedCriteria);
			clazz = Class.forName(criteriaImpl.getEntityOrClassName());
//			type = clazz;
			for (Iterator<?> it = criteriaImpl.iterateSubcriteria(); it.hasNext();){
				Subcriteria subcriteria = (Subcriteria) it.next();
				hasAddedCriteria.add(subcriteria.getAlias());
			}		
		} catch (Exception e) {
			throw new HCEErrorException("Cannot find field CriteriaImpl from DetachedCriteria , "+e.getMessage());
		}
		applyRestriction(clazz, expressions, detachedCriteria, null, hasAddedCriteria);

	}
	
	/**
	 * add restrictions, if the association cannot be found, it will created automatically
	 * @param detachedCriterias -> Array of {@link DetachedCriteria} to be add a restriction
	 * @param expressions -> Restriction
	 * @throws HibernateException
	 */
	protected void addExpression(DetachedCriteria[] detachedCriterias, Expression... expressions) throws HibernateException {
//		boolean isLogged = true;
		if(detachedCriterias == null || detachedCriterias.length == 0 || detachedCriterias.length < 2){
			throw new HCEErrorException("Invalid detachedCriterias parameter, please assign a value");
		}
		
		List<String> hasAddedCriteria = new ArrayList<String>();
//		Class<?> type, clazz;
		Class<?> clazz;
		try {
			Field field = detachedCriterias[0].getClass().getDeclaredField("impl");
			field.setAccessible(true);
			CriteriaImpl criteriaImpl = (CriteriaImpl) field.get(detachedCriterias[0]);
			clazz = Class.forName(criteriaImpl.getEntityOrClassName());
//			type = clazz;
			for (Iterator<?> it = criteriaImpl.iterateSubcriteria(); it.hasNext();){
				Subcriteria subcriteria = (Subcriteria) it.next();
				hasAddedCriteria.add(subcriteria.getAlias());
			}		
		} catch (Exception e) {
			throw new HCEErrorException("Cannot find field CriteriaImpl from DetachedCriteria , "+e.getMessage());
		}
		
		applyRestriction(clazz, expressions, detachedCriterias[0], detachedCriterias[1], hasAddedCriteria);

	}
	
	
	/*
	 * bisa saja satu level expression dan di dalamnya ada beberapa level
	 * algoritmanya harusnya memroses satu per satu
	 */
	
	protected void applyRestriction(Class<?> clazz, Expression[] expressions, DetachedCriteria detachedCriteria, DetachedCriteria detachedCriteria2,
			List<String> hasAddedCriteria){
		if(expressions != null){
			for (Expression expression : expressions) {
				if(expression.getExpressions() != null){
					for (Expression exp : expression.getExpressions()) {
	//					buildJoinExpression(exp, clazz, detachedCriteria, detachedCriteria2, hasAddedCriteria);
						Criterion criterion = CriterionUtils.getCriterion(exp, clazz, detachedCriteria, detachedCriteria2, hasAddedCriteria);
						if(criterion != null){
							detachedCriteria.add(criterion);	
							if(detachedCriteria2 != null){
								detachedCriteria2.add(criterion);	
							}
						}
					}
				}
			}
		}
	}
}
