package softtech.hong.hce.engine;

import java.util.List;

import org.hibernate.property.ChainedPropertyAccessor;
import org.hibernate.property.PropertyAccessor;
import org.hibernate.property.PropertyAccessorFactory;
import org.hibernate.property.Setter;
import org.hibernate.transform.ResultTransformer;

import softtech.hong.hce.exception.HCEErrorException;


/**
 * @author Kisman Hong
 * Hibernate cannot automatically set the field
 *         value of field, this is used to handler it, eg.: if you have a
 *         pojo class : public class Person(){ ... private School school; ... }
 * 
 *         when you add projection "school.name", hibernate cannot automatically
 *         set name value to school object class.
 *         this transformer is used for solving the limitation
 */
public class HCEUsualResultTransformer extends HCEProcessor implements ResultTransformer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -264194164964958304L;
	private final Class<?> resultClass;
	private Setter[] setters;
	private PropertyAccessor propertyAccessor;

	public HCEUsualResultTransformer(Class<?> resultClass) {
		if (resultClass == null)
			throw new HCEErrorException("resultClass cannot be null");
		this.resultClass = resultClass;
		propertyAccessor = new ChainedPropertyAccessor(new PropertyAccessor[] {
				PropertyAccessorFactory.getPropertyAccessor(resultClass, null),
				PropertyAccessorFactory.getPropertyAccessor("field") });
	}

	public Object transformTuple(Object[] tuple, String[] aliases) {
		Object result;
		try {
			result = resultClass.newInstance();
			if (setters == null) {
				setters = new Setter[aliases.length];
					for (int i = 0; i < aliases.length; i++) {
						String alias = aliases[i];
						if (alias != null) {
							setterProcess(propertyAccessor, alias, setters, i, result, resultClass, tuple);
						}
					}
			}
			setters = null;
		} catch (Exception e) {
			throw new HCEErrorException("Could not instantiate resultclass: " + resultClass.getName());
		}
		return result;
	}

	@SuppressWarnings({ "rawtypes" })
	public List transformList(List collection) {
		return collection;
	}

}
