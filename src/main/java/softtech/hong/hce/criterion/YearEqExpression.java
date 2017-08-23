package softtech.hong.hce.criterion;

import org.hibernate.Criteria;
import org.hibernate.EntityMode;
import org.hibernate.HibernateException;
import org.hibernate.criterion.CriteriaQuery;
import org.hibernate.criterion.Criterion;
import org.hibernate.engine.spi.TypedValue;
import org.hibernate.type.StringType;

public class YearEqExpression implements Criterion {
	 
		/**
	 * 
	 */
		private static final long serialVersionUID = 387108007180491427L;
		private final String propertyName;
	    private final String year;

	    public YearEqExpression(String propertyName, String year) {
	        this.propertyName = propertyName;
	        this.year = year;
	    }
    
	    public String toSqlString(Criteria criteria, CriteriaQuery criteriaQuery)
	        throws HibernateException {
	            String column = criteriaQuery.getColumn(criteria, propertyName);
	            if (column == null) {
	                throw new HibernateException("yearEq may only be used with single-column properties");
	            }
//	            DialectType dialect = DetectDialect.getDialect();
//	            if(dialect == DialectType.MySQL){    
		            return "YEAR("+column+") = ? ";
//		            return "DATE_FORMAT("+column+",'%m-%d') = ?";
//	            }else if(dialect == DialectType.PostgreSQL || dialect == DialectType.Oracle){
//	            	return "TO_CHAR("+column+",'"+format+"') = ?";
//	            	return "TO_CHAR("+column+",'MM-DD') = ?";
//	            }
//	            return "";  
	    }

	    
	    public TypedValue[] getTypedValues(Criteria criteria, CriteriaQuery criteriaQuery) throws HibernateException {
	    	return new TypedValue[] {new TypedValue( new StringType(), year, EntityMode.POJO)};
	    }

	    @Override
	    public String toString() {
	        return "value = " + year;
	    }
}
