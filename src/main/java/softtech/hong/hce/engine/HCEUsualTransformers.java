package softtech.hong.hce.engine;

import org.hibernate.transform.ResultTransformer;

/**
 * @author Kisman Hong
 * HCETransformers for setting query result to the result class
 * This transformer will not distinct root entity, data is flat
 */
final public class HCEUsualTransformers {

	private HCEUsualTransformers() {
	}

	/**
	 * Creates a resulttransformer that will inject aliased values into
	 * instances of Class via property methods or fields.
	 */
	public static ResultTransformer aliasToBean(Class<?> target) {
		return new HCEUsualResultTransformer(target);
	}

}
