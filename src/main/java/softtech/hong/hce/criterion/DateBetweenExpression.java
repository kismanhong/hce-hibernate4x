package softtech.hong.hce.criterion;

import org.hibernate.Criteria;
import org.hibernate.EntityMode;
import org.hibernate.HibernateException;
import org.hibernate.criterion.CriteriaQuery;
import org.hibernate.criterion.Criterion;
import org.hibernate.engine.spi.TypedValue;
import org.hibernate.type.StringType;

public class DateBetweenExpression implements Criterion {

    /**
	 * 
	 */
	private static final long serialVersionUID = 161521521898589613L;
	private final String propertyName;
    private final String lo;
    private final String hi;

    public DateBetweenExpression(String propertyName, String lo, String hi) {
        this.propertyName = propertyName;
        this.lo = lo;
        this.hi = hi;
    }

    
    public String toSqlString(Criteria criteria, CriteriaQuery criteriaQuery)
        throws HibernateException {
            String column = criteriaQuery.getColumn(criteria, propertyName);
            if (column == null) {
                throw new HibernateException("DateBetweenExpression may only be used with single-column properties");
            }
            return column + " between ? and ? ";
        }

    
    public TypedValue[] getTypedValues(Criteria criteria, CriteriaQuery criteriaQuery) throws HibernateException {
        return new TypedValue[] {new TypedValue( new StringType(), lo, EntityMode.POJO), new TypedValue(new StringType(), hi, EntityMode.POJO)};
    }

    @Override
    public String toString() {
        return propertyName + " between "+lo+" and "+hi;
    }
}

