package softtech.hong.hce.criterion;

import org.hibernate.Criteria;
import org.hibernate.EntityMode;
import org.hibernate.HibernateException;
import org.hibernate.criterion.CriteriaQuery;
import org.hibernate.criterion.Criterion;
import org.hibernate.engine.spi.TypedValue;
import org.hibernate.type.IntegerType;

public class MonthEqExpression implements Criterion {

    /**
	 * 
	 */
	private static final long serialVersionUID = 161521521898589613L;
	private final String propertyName;
    private final int month;

    public MonthEqExpression(String propertyName, int month) {
        this.propertyName = propertyName;
        this.month = month;
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
        return new TypedValue[] {new TypedValue( new IntegerType(), month, EntityMode.POJO)};
    }

    @Override
    public String toString() {
        return "month(" + propertyName + ") = " + month;
    }
}

