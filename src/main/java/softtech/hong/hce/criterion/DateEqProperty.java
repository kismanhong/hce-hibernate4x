package softtech.hong.hce.criterion;

import java.util.Date;

import org.hibernate.Criteria;
import org.hibernate.EntityMode;
import org.hibernate.HibernateException;
import org.hibernate.criterion.CriteriaQuery;
import org.hibernate.criterion.Criterion;
import org.hibernate.engine.spi.TypedValue;
import org.hibernate.type.DateType;
import org.hibernate.type.StringType;

import softtech.hong.hce.constant.DialectType;
import softtech.hong.hce.core.DetectDialect;

/**
 * @author kismanhong
 *
 */
public class DateEqProperty implements Criterion {

    /**
	 * 
	 */
	private static final long serialVersionUID = 161521521898589613L;
	private final String propertyName;
    private final String dateString;
    private final String format;
    private final Date date;

    public DateEqProperty(String propertyName, String dateString, String format) {
        this.propertyName = propertyName;
        this.dateString = dateString;
        this.date = null;
        this.format = format;
    }
    
    public DateEqProperty(String propertyName, Date date, String format) {
        this.propertyName = propertyName;
        this.dateString = null;
        this.date = date;
        this.format = format;
    }

    
    public String toSqlString(Criteria criteria, CriteriaQuery criteriaQuery)
        throws HibernateException {
            String column = criteriaQuery.getColumn(criteria, propertyName);
            if (column == null) {
                throw new HibernateException("monthEq may only be used with single-column properties");
            }
            DialectType dialect = DetectDialect.getDialect();
            if(dialect == DialectType.MySQL){    
	            return "DATE_FORMAT("+column+",'"+format+"') = ?";
//	            return "DATE_FORMAT("+column+",'%m-%d') = ?";
            }else if(dialect == DialectType.PostgreSQL || dialect == DialectType.Oracle){
            	return "TO_CHAR("+column+",'"+format+"') = ?";
//            	return "TO_CHAR("+column+",'MM-DD') = ?";
            }
            return "";  
    }

    
    public TypedValue[] getTypedValues(Criteria criteria, CriteriaQuery criteriaQuery) throws HibernateException {
    	if(date != null)
    		return new TypedValue[] {new TypedValue( new DateType(), date, EntityMode.POJO)};
    	else
    		return new TypedValue[] {new TypedValue( new StringType(), dateString, EntityMode.POJO)};
    }

    @Override
    public String toString() {
        return "value = " + date;
    }
}

