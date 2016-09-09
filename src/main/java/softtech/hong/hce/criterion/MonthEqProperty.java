package softtech.hong.hce.criterion;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.hibernate.Criteria;
import org.hibernate.EntityMode;
import org.hibernate.HibernateException;
import org.hibernate.criterion.CriteriaQuery;
import org.hibernate.criterion.Criterion;
import org.hibernate.engine.spi.TypedValue;
import org.hibernate.type.StringType;

public class MonthEqProperty implements Criterion {

    /**
	 * 
	 */
	private static final long serialVersionUID = 161521521898589613L;
	private final String propertyName;
    private final Date date;
    private final SimpleDateFormat monthOnly = new SimpleDateFormat("MM");

    public MonthEqProperty(String propertyName, Date date) {
        this.propertyName = propertyName;
        this.date = date;
    }

    
    public String toSqlString(Criteria criteria, CriteriaQuery criteriaQuery)
        throws HibernateException {
            String column = criteriaQuery.getColumn(criteria, propertyName);
            if (column == null) {
                throw new HibernateException("monthEq may only be used with single-column properties");
            }
            return "month(" + column + ") = ?";
        }

    
    public TypedValue[] getTypedValues(Criteria criteria, CriteriaQuery criteriaQuery) throws HibernateException {
        return new TypedValue[] {new TypedValue( new StringType(), monthOnly.format(date), EntityMode.POJO)};
    }

    @Override
    public String toString() {
        return "month(" + propertyName + ") = " + monthOnly.format(date);
    }
}

