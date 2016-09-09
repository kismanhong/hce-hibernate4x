package softtech.hong.hce.engine;

import org.hibernate.transform.ResultTransformer;

/**
 * @author Kisman Hong
 * HCETransformers for setting query result to the result class
 */
final public class HCETransformers {

	private HCETransformers() {
	}

	/**
	 * Creates a resulttransformer that will inject aliased values into
	 * instances of Class via property methods or fields.
	 */
	public static ResultTransformer aliasToBean(Class<?> target) {
		return new HCEResultTransformer(target);
	}

}
