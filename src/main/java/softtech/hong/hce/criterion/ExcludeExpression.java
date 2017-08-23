package softtech.hong.hce.criterion;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import javax.persistence.Column;
import javax.persistence.JoinColumn;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.EntityMode;
import org.hibernate.HibernateException;
import org.hibernate.criterion.CriteriaQuery;
import org.hibernate.criterion.Criterion;
import org.hibernate.engine.spi.TypedValue;
import org.hibernate.type.DoubleType;
import org.hibernate.type.LongType;
import org.hibernate.type.StringType;

import softtech.hong.hce.exception.HCEErrorException;
import softtech.hong.hce.model.Expression;
import softtech.hong.hce.utils.QueryUtils;
import softtech.hong.hce.utils.ReflectionUtils;

public class ExcludeExpression implements Criterion {

    /**
	 * 
	 */
	private static final long serialVersionUID = 161521521898589613L;
	private final String propertyName;
    private final String referencePropertyName;
    private final Class<?> clazz;
    private final Expression[] expressions;

    public ExcludeExpression(String propertyName, String referencePropertyName, Class<?> clazz, Expression... expressions) {
        this.propertyName = propertyName;
        this.referencePropertyName = referencePropertyName;
        this.clazz = clazz;
        this.expressions = expressions;
    }

    
    public String toSqlString(Criteria criteria, CriteriaQuery criteriaQuery)
        throws HibernateException {
            String column = criteriaQuery.getColumn(criteria, propertyName);
            String referenceColumn  = null; // = criteriaQuery.getColumn(criteria, QueryUtils.getProperty(referencePropertyName));
            String tableName = null;
            String[] columns = null;
            if (column == null) {
                throw new HibernateException("propertyName may only be used with single-column properties");
            }
          
            String[] projections;
			try {
				projections = StringUtils.split(referencePropertyName, ".");
			} catch (Exception e1) {
				throw new HibernateException("referencePropertyName must not be null");
			}
            Field field, childField = null;
            Class<?> type = clazz;
            if(projections != null){
            	for(int i=0; i < projections.length; i++){
            		if(i > 0){
						try {					
							field = ReflectionUtils.findField(type, projections[i-1]);
							type = field.getType();					
							if("interface java.util.Set".equalsIgnoreCase(field.getType().toString()) || 
									"interface java.util.List".equalsIgnoreCase(field.getType().toString()) || 
									"interface java.util.Map".equalsIgnoreCase(field.getType().toString())){
								 Type chieldType = field.getGenericType();   
							        if (chieldType instanceof ParameterizedType) {  
							            ParameterizedType pt = (ParameterizedType) chieldType;  
							            type = (Class<?>) (pt.getActualTypeArguments()[0]); 
							        }  
							}	
							if(i == (projections.length - 1)){
								tableName = QueryUtils.getTableName(type.getCanonicalName());
								if(expressions != null){
					            	columns = new String[expressions.length];
					            	for (int j=0; j < expressions.length; j++) {
										Expression expression = expressions[j];
										columns[j] = QueryUtils.getColumnName(type, expression.getPropertyName());
									}
					            }
							}
							childField = ReflectionUtils.findField(type, projections[i]);
						} catch (Exception e) {
	//						log.error("cannot find field name : {}, class : {}", projections[i], type.getCanonicalName());
							throw new HCEErrorException("cannot find field name : "+projections[i] +", class : "+ type.getCanonicalName(), e);
						} 
					
            		}else{						
						try {
							field = ReflectionUtils.findField(clazz, projections[i]);
							childField = field;
							tableName = QueryUtils.getTableName(clazz.getCanonicalName());
							if(expressions != null){
				            	columns = new String[expressions.length];
				            	for (int j=0; j < expressions.length; j++) {
									Expression expression = expressions[j];
									columns[j] = QueryUtils.getColumnName(type, expression.getPropertyName());
								}
				            }
						} catch (Exception e) {
//							log.error("cannot find field name : {} from class : {} ", projections[i], clazz.getCanonicalName());
							throw new HCEErrorException("cannot find field name : "+projections[i] +", class : "+ clazz.getCanonicalName(), e);
						} 
					}
            	}
            }
            for(Annotation annotation : childField.getAnnotations()){
            	if(annotation instanceof Column){
            		referenceColumn = ((Column) annotation).name();
					break;
				}else if(annotation instanceof JoinColumn){
					referenceColumn = ((JoinColumn) annotation).name();
					break;
				}
            }
            
            if(StringUtils.isEmpty(referenceColumn)){
            	referenceColumn = childField.getName();
            }
            
            if (referenceColumn == null) {
                throw new HibernateException("referencePropertyName may only be used with single-column properties");
            }
//            String[] columns = StringUtils.split(referenceColumn, ".");
            StringBuffer sqlString = new StringBuffer("(" + column + " not in (select " + referenceColumn  + " from ").append(
            		tableName);
            if(expressions != null && expressions.length > 0){
            	for(int i=0; i < expressions.length; i++){
//            		Expression expression = expressions[i];
                     	if(i == 0){
                     		sqlString.append(" where ");        			
                     	}else{
                     		sqlString.append(" and ");
                     	}
//                     	String[] innerColumns = StringUtils.split(criteriaQuery.getColumn(criteria, QueryUtils.getProperty(expression.getPropertyName())), ".");
                     	sqlString.append(columns[i]);
                     	sqlString.append(" = ? "); //.append(expression.getValue());
                     }
                }		
            sqlString.append(") )");
            return sqlString.toString();
        }

    
    public TypedValue[] getTypedValues(Criteria criteria, CriteriaQuery criteriaQuery) throws HibernateException {
    	if(expressions != null){
	    	TypedValue[] typedValues = new TypedValue[expressions.length];
	    	for(int i=0; i < expressions.length; i++){
	    		if(expressions[i].getValue() instanceof Long){
	    			typedValues[i] = new TypedValue(new LongType(), expressions[i].getValue(), EntityMode.POJO);
	    		}else if(expressions[i].getValue() instanceof Double){
	    			typedValues[i] = new TypedValue(new DoubleType(), expressions[i].getValue(), EntityMode.POJO);
	    		}else{
	    			typedValues[i] = new TypedValue(new StringType(), expressions[i].getValue().toString(), EntityMode.POJO);
	    		}
	    	}
	    	return typedValues;
    	}
        return new TypedValue[] {};
    }

    @Override
    public String toString() {
        return "";
    }
}

