package softtech.hong.hce.core;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.HashMap;

import javax.persistence.Column;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.PrimaryKeyJoinColumn;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.sql.JoinType;

import softtech.hong.hce.utils.ReflectionUtils;

/**
 * @author Kisman Hong
 * getting join type, criterion, and join name
 */
public abstract class CommonMethod {	
	
	/**
	 * @param theClass
	 * @param fieldName
	 * @return org.hibernate.sql.JoinType
	 * @throws SecurityException
	 * @throws NoSuchFieldException
	 * getting join type based on nullable in oneToMany, ManyToOne or ManyToMany (Association)
	 */
	protected JoinType getJoinType(Class<?> theClass, String fieldName) throws SecurityException, NoSuchFieldException{
		Field field = ReflectionUtils.findField(theClass, fieldName);
		Annotation[] annotations = field.getAnnotations();
		for (Annotation annotation : annotations) {
			if(annotation instanceof JoinColumn){
				JoinColumn joinColumn = (JoinColumn) annotation;
				if(joinColumn.nullable())
					return JoinType.LEFT_OUTER_JOIN;
			}else if(annotation instanceof PrimaryKeyJoinColumn){
				return JoinType.LEFT_OUTER_JOIN;
			}else if(annotation instanceof JoinTable){
				JoinTable joinTable = (JoinTable) annotation;
				JoinColumn[] joinColumns = joinTable.joinColumns();
				for (JoinColumn joinColumn : joinColumns) {
					if(joinColumn.nullable()){
						return JoinType.LEFT_OUTER_JOIN;
					}
				}
			}else if(annotation instanceof Column){
				if(((Column) annotation).nullable()){
					return JoinType.LEFT_OUTER_JOIN;
				}
			}
		}
		return JoinType.INNER_JOIN;
	}
	
	/**
	 * @param field
	 * @return org.hibernate.sql.JoinType
	 * @throws SecurityException
	 * @throws NoSuchFieldException
	 * getting join type based on nullable in oneToMany, ManyToOne or ManyToMany (Association)
	 */
	protected JoinType getJoinType(Field field) throws SecurityException, NoSuchFieldException{
		Annotation[] annotations = field.getAnnotations();
		for (Annotation annotation : annotations) {
			if(annotation instanceof JoinColumn){
				JoinColumn joinColumn = (JoinColumn) annotation;
				if(joinColumn.nullable())
					return JoinType.LEFT_OUTER_JOIN;
			}else if(annotation instanceof PrimaryKeyJoinColumn){
				return JoinType.LEFT_OUTER_JOIN;
			}else if(annotation instanceof JoinTable){
				JoinTable joinTable = (JoinTable) annotation;
				JoinColumn[] joinColumns = joinTable.joinColumns();
				for (JoinColumn joinColumn : joinColumns) {
					if(joinColumn.nullable()){
						return JoinType.LEFT_OUTER_JOIN;
					}
				}
			}else if(annotation instanceof Column){
				if(((Column) annotation).nullable()){
					return JoinType.LEFT_OUTER_JOIN;
				}
			}
		}
		return JoinType.INNER_JOIN;
	}
	
	protected JoinType getJoinType(Field field, String[] projections, HashMap<String, JoinType> joinOverrides) throws SecurityException, NoSuchFieldException{
		String newProjections = StringUtils.join(ArrayUtils.remove(projections, (projections.length - 1)), ".");
		if(joinOverrides != null && joinOverrides.get(newProjections) != null){
			return joinOverrides.get(newProjections);
		}
		return getJoinType(field);
	}
	
	/**
	 * @param joinType
	 * @return String of JoinType
	 * getting Join Name for logging
	 */
	protected String getJoinName(JoinType joinType){
		if(JoinType.INNER_JOIN == joinType){
			return "Inner";
		}
		return "Left";
	}
}
