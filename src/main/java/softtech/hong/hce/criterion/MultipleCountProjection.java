package softtech.hong.hce.criterion;

import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.criterion.AggregateProjection;
import org.hibernate.criterion.CriteriaQuery;
import org.hibernate.type.LongType;
import org.hibernate.type.Type;

/**
* A count for style :  count (distinct (a || b || c))
* @author Deepak Surti
*/
public class MultipleCountProjection extends AggregateProjection {

   /**
	 * 
	 */
	private static final long serialVersionUID = 2335631103666398993L;

	private boolean distinct;

   protected MultipleCountProjection(String prop) {
      super("count", prop);
   }

   public String toString() {
      if(distinct) {
         return "distinct " + super.toString();
      } else {
         return super.toString();
      }
   }

   public Type[] getTypes(Criteria criteria, CriteriaQuery criteriaQuery) throws HibernateException {
      return new Type[] { LongType.INSTANCE };
   }

   @Override
   public String toSqlString(Criteria criteria, int position, CriteriaQuery criteriaQuery) 
		   throws HibernateException {
      StringBuffer buf = new StringBuffer();
      buf.append("count(");
      if (distinct) buf.append("distinct ");
        String[] properties = propertyName.split(";");
//        for (int i = 0; i < properties.length; i++) {
//           buf.append( criteriaQuery.getColumn(criteria, properties[i]) );
//             if(i != properties.length - 1) 
//                buf.append(" || ");
//        }
        
        for (int i = 0; i < properties.length; i++) {
            buf.append( properties[i] );
              if(i != properties.length - 1) 
                 buf.append(" , ");
         }
        buf.append(") as y");
        buf.append(position);
        buf.append('_');
        return buf.toString();
   }
   
   public MultipleCountProjection setDistinct() {
      distinct = true;
      return this;
   }
   
}