package softtech.hong.hce.constant;


/**
 * @author Kisman Hong
 *
 */
public class QueryConstant {

	/**
	 * variable for main class alias
	 */
	public static final String ALIAS = "this";
	
//	public static final String[] byPass = {"active", "number", "viewable", "editable", "removed", "serialVersionUID"};
	
	/**
	 * field to be by pass for detection
	 */
	public static final String[] byPass = {"serialVersionUID"};
	
	/**
	 * object type that will be assigned as equals
	 */
	public static final String[] eqFields = {"double","int","long", "float", "class java.lang.Integer", 
												"class java.lang.Float", "class java.lang.Double", "class java.lang.Long", 
												"class java.util.Date", "class java.sql.Time", "boolean", "class java.lang.Boolean"};
}
