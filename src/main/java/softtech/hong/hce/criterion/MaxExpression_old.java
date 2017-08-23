package softtech.hong.hce.criterion;

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

import softtech.hong.hce.model.Expression;
import softtech.hong.hce.type.RestrictionType;

public class MaxExpression_old implements Criterion {

    /**
	 * 
	 */
	private static final long serialVersionUID = 161521521898589613L;
	private final String propertyName;
    private final Expression[] expressions;
    private final String referencePropertyName;
    private final String tableName;
    private final String sqlString;

    public MaxExpression_old(String propertyName, String referencePropertyName, String tableName, Expression... expressions) {
        this.propertyName = propertyName;
        this.referencePropertyName = referencePropertyName;
        this.expressions = expressions;
        this.tableName = tableName;
        this.sqlString = null;
    }

    public MaxExpression_old(String propertyName, String referencePropertyName, String tableName, String sqlString) {
        this.propertyName = propertyName;
        this.referencePropertyName = referencePropertyName;
        this.expressions = null;
        this.sqlString = sqlString;
        this.tableName = tableName;
    }
    
    public MaxExpression_old(String propertyName, String referencePropertyName, String tableName, String sqlString, Expression... expressions) {
        this.propertyName = propertyName;
        this.referencePropertyName = referencePropertyName;
        this.expressions = expressions;
        this.sqlString = sqlString;
        this.tableName = tableName;
    }
    
    public String toSqlString(Criteria criteria, CriteriaQuery criteriaQuery)
        throws HibernateException {
            String column = criteriaQuery.getColumn(criteria, propertyName);
            String referenceColumn = criteriaQuery.getColumn(criteria, referencePropertyName);
            if (column == null) {
                throw new HibernateException("propertyName may only be used with single-column properties");
            }
            if (referenceColumn == null) {
                throw new HibernateException("referencePropertyName may only be used with single-column properties");
            }
            String[] columns = StringUtils.split(referenceColumn, ".");
            StringBuffer sqlString = new StringBuffer("(" + column + " = (select max(" + columns[columns.length-1]  + ") from ").append(tableName);
            if(sqlString != null && expressions != null){
            	for(int i=0; i < expressions.length; i++){
            		Expression expression = expressions[i];
            		if(i == 0){
            			sqlString.append(" where ");        			
            		}else{
            			sqlString.append(" and ");
            		}
            		if(expression.getRestrictionType() == RestrictionType.OR){          			
            			for(Expression exp : expression.getExpressions()){
            				
            				if(exp.getRestrictionType() == RestrictionType.AND){
            					
            					for (Expression ex : exp.getExpressions()) {
            						int index = 0;
                        			sqlString.append(" ( ");
                        			
            						if(index > 0){
                    					sqlString.append(" and ");
                    				}
            						
            						String[] innerColumns = StringUtils.split(criteriaQuery.getColumn(criteria, ex.getPropertyName()), ".");
                            		sqlString.append(innerColumns[innerColumns.length-1]);
                            		if(ex.getRestrictionType() == RestrictionType.NULL){
                            			sqlString.append(" is null ");
                            		}else{
                            			sqlString.append(" = ? "); //.append(expression.getValue());
                            		}
            						index++;
								}
            					sqlString.append(" ) ");
            				}	
            			}
            			
            		}
//            		String[] innerColumns = StringUtils.split(criteriaQuery.getColumn(criteria, expression.getPropertyName()), ".");
//            		sqlString.append(innerColumns[innerColumns.length-1]);
//            		sqlString.append(" = ? "); //.append(expression.getValue());
            		sqlString.append(" and "); 
                	sqlString.append(this.sqlString);
            	}
            }else if(expressions != null && expressions.length > 0){
            	for(int i=0; i < expressions.length; i++){
            		Expression expression = expressions[i];
            		if(i == 0){
            			sqlString.append(" where ");        			
            		}else{
            			sqlString.append(" and ");
            		}
            		String[] innerColumns = StringUtils.split(criteriaQuery.getColumn(criteria, expression.getPropertyName()), ".");
            		sqlString.append(innerColumns[innerColumns.length-1]);
            		sqlString.append(" = ? "); //.append(expression.getValue());
            	}
            } else if(this.sqlString != null){
            	sqlString.append(" where "); 
            	sqlString.append(this.sqlString);
            }
            sqlString.append(" ) )");
            return sqlString.toString();
        }

    
    public TypedValue[] getTypedValues(Criteria criteria, CriteriaQuery criteriaQuery) throws HibernateException {
    	if(expressions != null){
	    	TypedValue[] typedValues = new TypedValue[expressions.length];
	    	for(int i=0; i < expressions.length; i++){
	    		if(expressions[i].getRestrictionType() == RestrictionType.OR){
//	    			for (Expression exp : expressions[i].getExpressions()) {
//	    				if(exp.getValue() instanceof Long){
//	    	    			typedValues[i] = new TypedValue(new LongType(), exp.getValue(), EntityMode.POJO);
//	    	    		}else if(exp.getValue() instanceof Double){
//	    	    			typedValues[i] = new TypedValue(new DoubleType(), exp.getValue(), EntityMode.POJO);
//	    	    		}else{
//	    	    			typedValues[i] = new TypedValue(new StringType(), exp.getValue().toString(), EntityMode.POJO);
//	    	    		}
//					}
	    		}else if(expressions[i].getValue() instanceof Long){
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
        return "(select max(" + propertyName  + ") from "+tableName;
    }
}


