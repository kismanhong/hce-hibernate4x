package softtech.hong.hce.criterion;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.EntityMode;
import org.hibernate.HibernateException;
import org.hibernate.criterion.CriteriaQuery;
import org.hibernate.criterion.Criterion;
import org.hibernate.engine.spi.TypedValue;
import org.hibernate.type.StringType;

public class BetweenPropertyExpression implements Criterion {

    /**
	 * 
	 */
	private static final long serialVersionUID = 161521521898589613L;
	private final String propertyName1;
	private final String propertyName2;
    private final String value;

    public BetweenPropertyExpression(String value, String propertyName1, String propertyName2) {
        this.propertyName1 = propertyName1;
        this.propertyName2 = propertyName2;
        this.value = value;
    }

    
    public String toSqlString(Criteria criteria, CriteriaQuery criteriaQuery)
        throws HibernateException {
            String column1 = criteriaQuery.getColumn(criteria, propertyName1);
            String column2 = criteriaQuery.getColumn(criteria, propertyName2);
            if (column1 == null || column2 == null) {
                throw new HibernateException("BetweenPropertyExpression may only be used with single-column properties");
            }
            return " ? between "+column1+" and "+column2;
        }

    
    public TypedValue[] getTypedValues(Criteria criteria, CriteriaQuery criteriaQuery) throws HibernateException {
        return new TypedValue[] {new TypedValue( new StringType(), StringUtils.remove(value, "this."), EntityMode.POJO)};
    }

    @Override
    public String toString() {
    	return value + " between "+propertyName1+" and "+propertyName2;
    }
}

