package softtech.hong.hce.utils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.OneToOne;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.criterion.ProjectionList;
import org.hibernate.criterion.Projections;
import org.hibernate.sql.JoinType;

import softtech.hong.hce.annotation.Ignore;
import softtech.hong.hce.constant.HCEConstant;
import softtech.hong.hce.constant.QueryConstant;
import softtech.hong.hce.exception.HCEErrorException;
import softtech.hong.hce.model.Expression;
import softtech.hong.hce.model.PropertyValue;
import softtech.hong.hce.type.RestrictionType;

/**
 * @author kismanhong
 *
 */
public class QueryUtils {
	
//private final static Logger logger = LoggerFactory.getLogger(QueryUtils.class);
		
//	public static void main(String[] args) {
//		System.out.println(Restrictions.ilike("name", "value"));
//	}
	
	/**
	 * aggregation syntax using
	 */
	private static final String SUM = "sum(";
	
	private static final String SUM_ = "^";
	
	private static final String COUNT = "count(";
	
	private static final String COUNT_ = "$";
	
	private static final String AVG = "avg(";
	
	private static final String AVG_ = "@";
	
	private static final String MIN = "min(";
	
	private static final String MIN_ = "-";
	
	private static final String MAX = "max(";
	
	private static final String MAX_ = "+";
	
	private static final String GROUP_BY = "groupBy(";
	
	private static final String GROUP_BY_ = "#";
	
	private static final String ALIAS_TO = "->";
	
	private static final String WHERE_PREFIX = "_";
	
	private static final String[] javaType = {"class java.lang.Double", "class java.lang.Integer", "class java.lang.Long", "class java.lang.Float", "class java.lang.Short", 
		"class java.lang.String", "class java.util.Date", "class java.lang.Integer", "class java.lang.Boolean", "boolean", "double", "int", "float", "number", "short", 
		"class java.sql.Time", "Time"};
	
	private static QueryUtils instance = null;

	public static synchronized QueryUtils getInstance() {
		if (instance == null) {
			instance = new QueryUtils();
		}
		return instance;
	}
	
	/**
	 * get alias and property, e.g : country.name
	 * @param field
	 * @return String
	 */
	public static String getProperty(String field){
		if(field != null){
			String[] separateProperties = StringUtils.split(field, ".");
			if(separateProperties.length > 2){
				return getProperty(separateProperties);			
			}else if(separateProperties.length == 2){
				return getPropertyRemark(separateProperties);
			}else{
				return QueryConstant.ALIAS + "." + field;
			}
		}
		return "";
	}
	
	/**
	 * property deep level
	 * @param properties
	 * @return
	 */
	public static String[] convertToLevel(String[] properties){
		String[] results = new String[properties.length];
		for (int i=0; i < results.length; i++) {
			results[i] = getProperty(properties[i]);
		}
		return results;
	}
	
	/**
	 * @param properties
	 * @return
	 */
	public static String getProperty(String[] properties){
		int len = properties.length;		
		return properties[len - 2] + (len - 2) + "." + properties[len - 1];
	}
	
	/**
	 * getting alias property name
	 * @param properties
	 * @return String
	 */
	public static String getPropertyRemark(String[] properties){
		return properties[0] + "0." + properties[1];
	}
	
	/**
	 * getting join alias
	 * @param properties
	 * @return String
	 */
	public static String getJoinAlias(String[] properties){
		int len = properties.length;	
		return properties[len - 2] + (len - 2);
	}
	
	/**
	 * @param property
	 * @return
	 */
	public static String getAlias(String property){
		String[] properties = StringUtils.split(property, ".");
		String results = properties[0];
		for(int i=1; i < properties.length; i++){
			results += StringUtils.capitalize(properties[i]);
		}
		return results;
	}
	
	/**
	 * @param properties
	 * @param fieldName
	 * @return
	 */
	public static boolean isPropertyAlias(String[] properties, String fieldName){
		if(properties != null){
			for (String property : properties) {
				if(StringUtils.contains(property, fieldName))
					return true;
			}
		}
		return false;
	}
	
	/**
	 * @param properties
	 * @param fieldName
	 * @param level
	 * @return
	 */
	public static boolean isPropertyAlias(String[] properties, String fieldName, int level){
		if(properties != null){
			for (String property : properties) {
				if(StringUtils.contains(property, fieldName)){
					if(ArrayUtils.indexOf(StringUtils.split(property, "."), fieldName) == level)
						return true;
				}
			}
		}
		return false;
	}
	
	/**
	 * @param fieldName
	 * @param fieldValue
	 * @return
	 * checking valid field
	 */
	public static boolean isValidField(String fieldName, Object fieldValue)
	{
		if(ArrayUtils.contains(QueryConstant.byPass, fieldName))
			return false;
		else if(fieldName.contains("fake"))
			return false;
		else if(fieldValue == null)
			return false;
		else if (StringUtils.isEmpty(fieldValue.toString()))
			return false;
		else if ("0".equals(fieldValue.toString()))
			return false;
		return true;
	}
	
	
	/**
	 * @param object
	 * @param clause
	 * @return
	 * construct bulk delete, actually this has already support by hibernate
	 */
	public String constructDeleteQuery(String object, String clause){
		StringBuffer queryBuffer = new StringBuffer("delete from ");
		queryBuffer.append(object);
		String[] clauses = getClauses(clause);
		if(clauses.length > 0)
			queryBuffer.append(" where ");
		
		for (String query : clauses) {
			queryBuffer.append(query);
			queryBuffer.append(" and");
		}
		return StringUtils.substring(queryBuffer.toString(), 0, queryBuffer.toString().length() - 4);
	}
	
	/**
	 * @param clause
	 * @return
	 */
	public String[] getClauses(String clause){
		return StringUtils.split(clause, ";");
	}
	
	/**
	 * @param id
	 * @return
	 */
	public static String constructInClause(Long[] id){
		return " where id in(" + StringUtils.join(id, ",") + ")";	
	}
	
//	/**
//	 * @param detachedCriteria
//	 * @param orders
//	 * @param fieldName
//	 * @param main
//	 */
//	public void orderHandler(DetachedCriteria detachedCriteria, Order[] orders, String fieldName, boolean main){ // the main object class
//		String[] orderSplit;
//		int position;
//		if(orders != null){
//			for (Order order : orders) {
//				orderSplit = StringUtils.split(order.getProperty(), ".");
//				position = ArrayUtils.indexOf(orderSplit, fieldName);
//				if(orderSplit.length == 1 && main){
//					if(order.getOrderType() == OrderType.Asc){
//						detachedCriteria.addOrder(org.hibernate.criterion.Order.asc(orderSplit[0]));
//					}else{
//						detachedCriteria.addOrder(org.hibernate.criterion.Order.desc(orderSplit[0]));
//					}
//				}else{
//					if(position > -1 && (position + 2) == orderSplit.length){
//						if(order.getOrderType() == OrderType.Asc){
//							detachedCriteria.addOrder(org.hibernate.criterion.Order.asc(getProperty(orderSplit)));
//						}else{
//							detachedCriteria.addOrder(org.hibernate.criterion.Order.desc(getProperty(orderSplit)));
//						}
//					}
//				} 
//			}
//		}
//	}
	
//	/**
//	 * @param criteria
//	 * @param params
//	 */
//	public void orderHandler(DetachedCriteria criteria,
//			Order[] params) {
//		if (params != null) {
//			for (Order order : params) {
//				String[] orderProperties = StringUtils.split(order.getProperty(),".");
//				if(orderProperties.length > 1){
//					DetachedCriteria more = criteria.createCriteria(orderProperties[0]);
//					for (int i = 1; i < orderProperties.length-1; i++) {
//						more = more.createCriteria(orderProperties[i]);
//					}
//					if (order.getOrderType() == OrderType.Asc){
//						more.addOrder(org.hibernate.criterion.Order.asc(orderProperties[orderProperties.length-1]));
//					}
//					else{
//						more.addOrder(org.hibernate.criterion.Order.desc(orderProperties[orderProperties.length-1]));
//					}
//				}else{
//					if (order.getOrderType() == OrderType.Asc){
//						criteria.addOrder(org.hibernate.criterion.Order.asc(order.getProperty()));
//					}
//					else{
//						criteria.addOrder(org.hibernate.criterion.Order.desc(order.getProperty()));
//					}
//				}
//			}
//		}
//	}
	
//	/**
//	 * @param searchCriteria
//	 * @param orders
//	 */
//	public void buildOrders(DetachedCriteria searchCriteria, Order... orders){
//		if(orders != null){
//			for (Order order : orders) {
//				String[] separateProperties = StringUtils.split(order.getProperty(), ".");
//				if (order.getOrderType() == OrderType.Asc){
//					if(separateProperties.length > 1){
//						searchCriteria.addOrder(org.hibernate.criterion.Order.asc(getProperty(separateProperties)));
//					}else{
//						searchCriteria.addOrder(org.hibernate.criterion.Order.asc(order.getProperty()));
//					}
//				}
//				else{
//					if(separateProperties.length > 1){
//						searchCriteria.addOrder(org.hibernate.criterion.Order.desc(getProperty(separateProperties)));
//					}else{
//						searchCriteria.addOrder(org.hibernate.criterion.Order.desc(order.getProperty()));
//					}			
//				}
//			}
//		}
//	}
	
	/**
	 * @param properties
	 * @return
	 * used for creating projections based on array of field given
	 * this will detect the level such as user.employee.fullName, the aliases will be user = user0, employee = employee1
	 * this is used when the criteria generated by QueryBuilder or QueryHandler
	 */
	public ProjectionList buildProjections(String[] properties){
		ProjectionList projectionList = Projections.projectionList();
		if(properties != null){
			for (String property : properties) {
				if(StringUtils.startsWith(property, COUNT) || StringUtils.startsWith(property, COUNT_)){
					String[] separateProperties = StringUtils.split(property, ALIAS_TO);
					if(separateProperties.length < 2){
						throw new HCEErrorException("Projection Count for syntax "+property+" is invalid, e.g : count(name)->cnt or $name->cnt => " +
								"name = property to be count, cnt = field to be alias");
					}
					property = StringUtils.substringBetween(property, "count(", ")").trim();
					property = StringUtils.remove(property, COUNT_).trim();			
					projectionList.add(Projections.count(getProperty(property)), separateProperties[1]);
				}else if(StringUtils.startsWith(property, SUM) || StringUtils.startsWith(property, SUM_)){
					String[] separateProperties = StringUtils.split(property, ALIAS_TO);
					if(separateProperties.length < 2){
						throw new HCEErrorException("Projection Sum for syntax "+property+" is invalid, e.g : sum(amount)->sm or ^amount->sm => " +
								"amount = property to be sum, sm = field to be alias");
					}
					property = StringUtils.substringBetween(property, "sum(", ")").trim();
					property = StringUtils.remove(property, SUM_).trim();			
					projectionList.add(Projections.sum(getProperty(property)), separateProperties[1]);
				}else if(StringUtils.startsWith(property, AVG) || StringUtils.startsWith(property, AVG_)){
					String[] separateProperties = StringUtils.split(property, ALIAS_TO);
					if(separateProperties.length < 2){
						throw new HCEErrorException("Projection Avg for syntax "+property+" is invalid, e.g : avg(amount)->average or @amount->average => " +
								"amount = property to be average, average = field to be alias");
					}
					property = StringUtils.substringBetween(property, "avg(", ")").trim();
					property = StringUtils.remove(property, AVG_).trim();			
					projectionList.add(Projections.avg(getProperty(property)), separateProperties[1]);
				}else if(StringUtils.startsWith(property, MAX) || StringUtils.startsWith(property, MAX_)){
					String[] separateProperties = StringUtils.split(property, ALIAS_TO);
					if(separateProperties.length < 2){
						throw new HCEErrorException("Projection Max for syntax "+property+" is invalid, e.g : max(amount)->mx or +amount->mx => " +
								"amount = property to be max, mx = field to be alias");
					}
					property = StringUtils.substringBetween(property, "max(", ")").trim();
					property = StringUtils.remove(property, MAX_).trim();			
					projectionList.add(Projections.max(getProperty(property)), separateProperties[1]);
				}else if(StringUtils.startsWith(property, MIN) || StringUtils.startsWith(property, MIN_)){
					String[] separateProperties = StringUtils.split(property, ALIAS_TO);
					if(separateProperties.length < 2){
						throw new HCEErrorException("Projection Min for syntax "+property+" is invalid, e.g : min(amount)->mn or -amount->mn => " +
								"amount = property to be min, mn = field to be alias");
					}
					property = StringUtils.substringBetween(property, "min(", ")").trim();
					property = StringUtils.remove(property, MIN_).trim();			
					projectionList.add(Projections.min(getProperty(property)), separateProperties[1]);
				}else if(StringUtils.startsWith(property, GROUP_BY) || StringUtils.startsWith(property, GROUP_BY_)){
					String[] separateProperties = StringUtils.split(property, ALIAS_TO);
					if(separateProperties.length < 2){
						throw new HCEErrorException("Projection Group By for syntax "+property+" is invalid, e.g : groupBy(country)->gb or #country->gb => " +
								"country = property to be group by, gb = field to be alias");
					}
					property = StringUtils.substringBetween(property, "groupBy(", ")").trim();
					property = StringUtils.remove(property, GROUP_BY_).trim();			
					projectionList.add(Projections.groupProperty(getProperty(property)), separateProperties[1]);
				}else{
					projectionList.add(Projections.property(getProperty(property)), StringUtils.replace(property,".","|"));
				}
			}
		}
		return projectionList;
	}
	
	/**
	 * @param properties
	 * @param aliases
	 * @return
	 */
	public ProjectionList buildProjections(String[] properties, String[] aliases){
		ProjectionList projectionList = Projections.projectionList();
		String[] separateProperties;
		String newProperty = null;
		int i =0;
		for (String property : properties) {
			if(StringUtils.startsWith(property, COUNT) || StringUtils.startsWith(property, COUNT_)){
				separateProperties = StringUtils.split(property, ALIAS_TO);
				if(separateProperties.length < 2){
					throw new HCEErrorException("Projection Count for syntax "+property+" is invalid, e.g : count(name)->cnt or $name->cnt => " +
							"name = property to be count, cnt = field to be alias");
				}
				property = StringUtils.substringBetween(property,"count(", ")").trim();
				property = StringUtils.remove(property, COUNT_).trim();			
				projectionList.add(Projections.count(getProperty(property)), separateProperties[1]);
			}else if(StringUtils.startsWith(property, SUM) || StringUtils.startsWith(property, SUM_)){
				separateProperties = StringUtils.split(property, ALIAS_TO);
				if(separateProperties.length < 2){
					throw new HCEErrorException("Projection Sum for syntax "+property+" is invalid, e.g : sum(amount)->sm or ^amount->sm => " +
							"amount = property to be sum, sm = field to be alias");
				}
				property = StringUtils.substringBetween(property,"sum(", ")").trim();
				property = StringUtils.remove(property, SUM_).trim();			
				projectionList.add(Projections.sum(getProperty(property)), separateProperties[1]);
			}else if(StringUtils.startsWith(property, AVG) || StringUtils.startsWith(property, AVG_)){
				separateProperties = StringUtils.split(property, ALIAS_TO);
				if(separateProperties.length < 2){
					throw new HCEErrorException("Projection Avg for syntax "+property+" is invalid, e.g : avg(amount)->average or @amount->average => " +
							"amount = property to be average, average = field to be alias");
				}
				property = StringUtils.substringBetween(property,"avg(", ")").trim();
				property = StringUtils.remove(property, AVG_).trim();			
				projectionList.add(Projections.avg(getProperty(property)), separateProperties[1]);
			}else if(StringUtils.startsWith(property, MAX) || StringUtils.startsWith(property, MAX_)){
				separateProperties = StringUtils.split(property, ALIAS_TO);
				if(separateProperties.length < 2){
					throw new HCEErrorException("Projection Max for syntax "+property+" is invalid, e.g : max(amount)->mx or +amount->mx => " +
							"amount = property to be max, mx = field to be alias");
				}
				property = StringUtils.substringBetween(property,"max(", ")").trim();
				property = StringUtils.remove(property, MAX_).trim();			
				projectionList.add(Projections.max(getProperty(property)), separateProperties[1]);
			}else if(StringUtils.startsWith(property, MIN) || StringUtils.startsWith(property, MIN_)){
				separateProperties = StringUtils.split(property, ALIAS_TO);
				if(separateProperties.length < 2){
					throw new HCEErrorException("Projection Min for syntax "+property+" is invalid, e.g : min(amount)->mn or -amount->mn => " +
							"amount = property to be min, mn = field to be alias");
				}
				property = StringUtils.substringBetween(property,"min(", ")").trim();
				property = StringUtils.remove(property, MIN_).trim();			
				projectionList.add(Projections.min(getProperty(property)), separateProperties[1]);
			}else if(StringUtils.startsWith(property, GROUP_BY) || StringUtils.startsWith(property, GROUP_BY_)){
				separateProperties = StringUtils.split(property, ALIAS_TO);
				if(separateProperties.length < 2){
					throw new HCEErrorException("Projection Group By for syntax "+property+" is invalid, e.g : groupBy(country)->gb or #country->gb => " +
							"country = property to be group by, gb = field to be alias");
				}
				property = StringUtils.substringBetween(property,"groupBy(", ")").trim();
				property = StringUtils.remove(property, GROUP_BY_).trim();			
				projectionList.add(Projections.groupProperty(getProperty(property)), separateProperties[1]);
			}else{
				if(StringUtils.contains(property, ALIAS_TO)){
					separateProperties = StringUtils.split(property,ALIAS_TO);
					String [] newSeparateProperties = StringUtils.split(separateProperties[0],".");
					if(newSeparateProperties.length > 2){
						newProperty = getProperty(newSeparateProperties);			
					}else if(newSeparateProperties.length == 2){
						newProperty = getPropertyRemark(newSeparateProperties);
					}else{
						newProperty = property;
					}
					projectionList.add(Projections.property(newProperty), StringUtils.replace(separateProperties[1], ".", "|"));
				}else{
					separateProperties = StringUtils.split(property,".");
					if(separateProperties.length > 2){
						newProperty = getProperty(separateProperties);			
					}else if(separateProperties.length == 2){
						newProperty = getPropertyRemark(separateProperties);
					}else{
						newProperty = property;
					}
					projectionList.add(Projections.property(newProperty), StringUtils.replace(aliases[i], ".", "|"));
				}
				i++;
			}			
		}
		return projectionList;
	}

	public static String getTableName(String className){
		return HCEConstant.ENTITY_TO_TABLE.get(className);
	}
	
	public static String getTableName(Class<?> clazz){
//		return HCEConstant.ENTITY_TO_TABLE.get(clazz.getCanonicalName());
		String result = null;
		Annotation[] annotations = clazz.getAnnotations();
		for (Annotation annotation : annotations) {
			if(annotation instanceof Entity){
				Entity entity = (Entity) annotation;
				result = entity.name();
			}else if(annotation instanceof Table){
				Table table = (Table) annotation;
				result = table.name();
			}else if(annotation instanceof org.hibernate.annotations.Table){
				//org.hibernate.annotations.Table table = (org.hibernate.annotations.Table) annotation;
				result = clazz.getSimpleName();
			}
		}
		return result;
	}
	
	public static String getIdentifier(String className){
		return HCEConstant.ENTITY_IDENTIFIER_PROPERTY.get(className);
	}
	
	public static String[] getIdentifierColumnNames(String className){
		return HCEConstant.ENTITY_IDENTIFIER_PRIMARY_COLUMN.get(className);
	}
	
	public static String getIdentifierColumnName(String className){
		try {
			return HCEConstant.ENTITY_IDENTIFIER_PRIMARY_COLUMN.get(className)[0];
		} catch (Exception e) {
			return "cannot find identifier, key :"+className;
		}
	}
	
	public static String getColumnName(Class<?> clazz, String fieldName) throws HCEErrorException{
		String[] columns = StringUtils.split(fieldName, ".");
		Field field= null;
		Class<?> type = clazz;
		for (int i=0; i < columns.length; i++) {
			try {
				field = ReflectionUtils.findField(type, columns[i]);
				if(i < (columns.length - 1)){
					type = field.getType();
				}
			} catch (Exception e) {
				throw new HCEErrorException(e);
			}
		}
		String result = null;
		try {
			//Field field = clazz.getDeclaredField(fieldName);
			Annotation[] annotations = field.getAnnotations();			
			for (Annotation annotation : annotations) {
				if(annotation instanceof Column){
					result = ((Column) annotation).name();
					break;
				}else if(annotation instanceof JoinColumn){
					result = ((JoinColumn) annotation).name();
					break;
				}
			}
		} catch (Exception e) {
			throw new HCEErrorException(e);
		}
		if(StringUtils.isEmpty(result)){
			String getMethod;
			if(!boolean.class.equals(field.getType())){
				getMethod  = MethodUtils.getGetMethodName(fieldName);
			}else{
				getMethod = "is" + StringUtils.capitalize(fieldName);
			}
			Class<?>[] parameterTypes = new Class<?>[0];
			Annotation[] annotations;
			try {
				annotations = type.getMethod(getMethod, parameterTypes).getAnnotations();
			} catch (Exception e) {
				throw new HCEErrorException(e);
			}	
			for (Annotation annotation : annotations) {
				if(annotation instanceof Column){
					result = ((Column) annotation).name();
					break;
				}else if(annotation instanceof JoinColumn){
					result = ((JoinColumn) annotation).name();
					break;
				}
			}	
		}
		if(StringUtils.isEmpty(result)){
			return fieldName;
		}
		return result;
	}
	
	/**
	 * @param ignoreProperties
	 * @param property
	 * @param fieldName
	 * @param ignoreSensitive
	 * @return
	 */
	public static boolean isIgnored(String[] ignoreProperties, String property, String fieldName, boolean ignoreSensitive){
		if(ignoreSensitive && ArrayUtils.contains(ignoreProperties, property)){
			return true;
		}else{
			if(softtech.hong.hce.utils.ArrayUtils.indexOfString(ignoreProperties, fieldName)){
				return true;
			}
		}
		return false;
	}
	
	/**
	 * @param clazz
	 * @param propertyValues
	 * @return String
	 */
	public static String setClause(Class<?> clazz, PropertyValue...propertyValues){
		StringBuffer stringBuffer = new StringBuffer();
		int length = propertyValues.length;
		for(int i=0; i<length; i++){
			stringBuffer.append(getColumnName(clazz, propertyValues[i].getPropertyName())).append(" = ").append(":").append(propertyValues[i].getPropertyName());
			if(i < (length - 1)){
				stringBuffer.append(",");
			}
		}
		return stringBuffer.toString();
	}
	
	/**
	 * @param clazz
	 * @param expressions
	 * @return
	 */
	public static String whereClause(Class<?> clazz, Expression...expressions){
		StringBuffer stringBuffer = new StringBuffer();
		if(expressions != null){
			int length = expressions.length;
			for (int i=0; i<length; i++) {
				Expression expression = expressions[i];
				stringBuffer.append(getQueryString(clazz, expression));
				if(expression.getRestrictionType() == RestrictionType.OR){
					if(i < (length - 1)){
						stringBuffer.append( " OR " );
					}
				}else{
					if(i < (length - 1)){
						stringBuffer.append( " AND " );
					}
				}
			}
		}
		return stringBuffer.toString();
	}
	
	/**
	 * @param clazz
	 * @param expression
	 * @return
	 */
	private static String getQueryString(Class<?> clazz, Expression expression){
		StringBuffer stringBuffer = new StringBuffer("(");
		Expression[] expressions = expression.getExpressions();
		int length = expressions.length;
		for(int i=0; i < length; i++){
			Expression express = expressions[i];
			switch (express.getRestrictionType()) {
				
				case EQ:
					stringBuffer.append(getColumnName(clazz, express.getPropertyName())).append( " = " ).append(":").append(WHERE_PREFIX).append(express.getPropertyName());
					break;
				case GT:
					stringBuffer.append(getColumnName(clazz, express.getPropertyName())).append( " > " ).append(":").append(WHERE_PREFIX).append(express.getPropertyName());
					break;
				case GE:
					stringBuffer.append(getColumnName(clazz, express.getPropertyName())).append( " >= " ).append(":").append(WHERE_PREFIX).append(express.getPropertyName());
					break;
				case LT:
					stringBuffer.append(getColumnName(clazz, express.getPropertyName())).append( " < " ).append(":").append(WHERE_PREFIX).append(express.getPropertyName());
					break;
				case LE:
					stringBuffer.append(getColumnName(clazz, express.getPropertyName())).append( " <= " ).append(":").append(WHERE_PREFIX).append(express.getPropertyName());
					break;
				case NE:
					stringBuffer.append(getColumnName(clazz, express.getPropertyName())).append( " != " ).append(":").append(WHERE_PREFIX).append(express.getPropertyName());
					break;
				case ILIKE:
//					if(expression.getMatchMode() == null){
//						stringBuffer.append(getColumnName(clazz, express.getPropertyName())).append( " ilike(%" ).append(":").append(WHERE_PREFIX).append(express.getPropertyName()).append("%)");
//					}else{
//						stringBuffer.append(getColumnName(clazz, express.getPropertyName())).append( " ilike(%" ).append(":").append(WHERE_PREFIX).append(express.getPropertyName()).append("%)");
//					}
					break;
				case LIKE:
//					if(expression.getMatchMode() == null){
//						criterion = Restrictions.like(expression.getPropertyName(), expression.getValue());
//					}else{
//						criterion = Restrictions.like(expression.getPropertyName(), expression.getValue().toString(), expression.getMatchMode()).toString();
//					}
					break;
				case IN:
					stringBuffer.append(getColumnName(clazz, express.getPropertyName())).append( " IN (" ).append(":").append(WHERE_PREFIX).append(express.getPropertyName()).append(")");
					break;
//				case EMPTY:
//					criterion = Restrictions.isEmpty(expression.getPropertyName());
//					break;
//				case NOT_EMPTY:
//					criterion = Restrictions.isNotEmpty(expression.getPropertyName());
//					break;
				case NULL:
					stringBuffer.append(getColumnName(clazz, express.getPropertyName())).append( " IS NULL " );
					break;
				case NOT_NULL:
					stringBuffer.append(getColumnName(clazz, express.getPropertyName())).append( " IS NOT NULL " );
					break;
//				case OR:
//					criterion = expression.getJunction();
//					break;
				case BETWEEN:
					stringBuffer.append(getColumnName(clazz, express.getPropertyName())).append( " BETWEEN (" )
						.append(":").append(WHERE_PREFIX).append(express.getPropertyName()).append(" AND ")
						.append(":and").append(WHERE_PREFIX).append(express.getPropertyName()) ;
					break;
//				case SIZE_EQ:
//					criterion = Restrictions.sizeEq(expression.getPropertyName(), (Integer) expression.getValue());
//					break;
//				case SIZE_GT:
//					criterion = Restrictions.sizeGt(expression.getPropertyName(), (Integer) expression.getValue());
//					break;
//				case SIZE_GE:
//					criterion = Restrictions.sizeGe(expression.getPropertyName(), (Integer) expression.getValue());
//					break;
//				case SIZE_LE:
//					criterion = Restrictions.sizeLe(expression.getPropertyName(), (Integer) expression.getValue());
//					break;
//				case SIZE_LT:
//					criterion = Restrictions.sizeLt(expression.getPropertyName(), (Integer) expression.getValue());
//					break;
//				case SIZE_NE:
//					criterion = Restrictions.sizeNe(expression.getPropertyName(), (Integer) expression.getValue());
//					break;
//				case DATE_EQ:
//					criterion = new DateEqProperty(expression.getPropertyName(), expression.getValue().toString(), expression.getValue1().toString());
//					break;
//				case PROPERTY_BETWEEN:
//					criterion = new PropertyBetweenExpression(expression.getPropertyName(), expression.getValue().toString(), expression.getValue1().toString());
//					break;
//				case BETWEEN_PROPERTY:
//					criterion = new BetweenPropertyExpression(expression.getPropertyName(), expression.getValue().toString(), expression.getValue1().toString());
//					break;
//				case MONTH_EQ:
//					criterion = new MonthEqProperty(expression.getPropertyName(), (Date) expression.getValue());
//					break;
				default:
					stringBuffer.append(getColumnName(clazz, express.getPropertyName())).append( " = " ).append(":").append(WHERE_PREFIX).append(express.getPropertyName());
					break;
				}
			
				if(i < (length -1)){
					stringBuffer.append(" AND ");
				}
			}
		
			stringBuffer.append(")");
			
			return stringBuffer.toString();
		}
	
//	if(expression.getRestrictionType() == RestrictionType.OR){
//		int length = expressions.length;
//		Disjunction disjunction = Expression.disjunction();
//		for(int i=0; i < length; i++){
//			Expression express = expressions[i];
//			disjunction.add(CriterionUtils.getCriterion(express));			
//		}
//		stringBuffer.append(")");
//	}else{
//		int length = expressions.length;
//		Conjunction conjunction = Expression.conjunction();
//		for(int i=0; i < length; i++){
//			Expression express = expressions[i];
//			conjunction.add(CriterionUtils.getCriterion(express));			
//		}
//		stringBuf
//		stringBuffer.append(")");
//	}
		
//	}
	
	public static String[] restructureProjections(Class<?> clazz, String[] projections){
		if(projections == null || projections.length == 0){
			return projections;
		}
		String[] projs = {};
		Class<?> type = clazz;
		for(int i=0; i<projections.length; i++){
			if(StringUtils.contains(projections[i], "*")){
				Field field = null;
				String[] projectionFields = StringUtils.split(projections[i], ".");
				if(projectionFields.length > 1){
					for(int j=0; j<projectionFields.length; j++){
						if(!"*".equals(projectionFields[j])){
							try {
								field = ReflectionUtils.findField(type, projectionFields[j]);
								
								if(ReflectionUtils.isCollection(field.getType())){
									 Type chieldType = field.getGenericType();   
								        if (chieldType instanceof ParameterizedType) {  
								            ParameterizedType pt = (ParameterizedType) chieldType;  
								            type = (Class<?>) (pt.getActualTypeArguments()[0]); 
								        }  
								}else{
									type = field.getType();
								}
//								childField = ReflectionUtils.findField(type, projectionFields[j]);
								
							} catch (Exception e) {
			//					log.error("cannot find field name : {}, class : {}", projections[i], type.getCanonicalName());
								throw new HCEErrorException("cannot find field name : "+projections[i] +", class : "+ type.getCanonicalName(), e);
							} 
						}
					}
					Field[] fields = ReflectionUtils.getInheritedPrivateFields(type);
					for(int k=0; k<fields.length; k++){
						Ignore ignore = fields[k].getAnnotation(Ignore.class);
						Transient trans  = fields[k].getAnnotation(Transient.class);
						if(ignore == null && trans == null && ArrayUtils.contains(javaType, fields[k].getType().toString()) || fields[k].getType().isEnum()){
							projs = ArrayUtils.add(projs, StringUtils.replace(projections[i], "*", fields[k].getName()));
						}
					}
					type = clazz;
				}else{
					Field[] fields = ReflectionUtils.getInheritedPrivateFields(type);
					for(int k=0; k<fields.length; k++){
						Ignore ignore = fields[k].getAnnotation(Ignore.class);
						Transient trans = fields[k].getAnnotation(Transient.class);
						if(!ReflectionUtils.isStatic(fields[k]) && !ReflectionUtils.isStaticFinal(fields[k]) && 
								ignore == null && trans == null && (ArrayUtils.contains(javaType, fields[k].getType().toString()) || fields[k].getType().isEnum())){
							projs = ArrayUtils.add(projs, StringUtils.replace(projections[i], "*", fields[k].getName()));
						}
					}
				}
			}else{
				projs = ArrayUtils.add(projs, projections[i]);
			}
		}
		return projs;
	}
	
	/**
	 * @param theClass
	 * @param fieldName
	 * @return org.hibernate.sql.JoinType
	 * @throws SecurityException
	 * @throws NoSuchFieldException
	 * getting join type based on nullable in oneToMany, ManyToOne or ManyToMany (Association)
	 */
	public static JoinType getJoinType(Class<?> theClass, String fieldName) throws SecurityException, NoSuchFieldException{
		Field field = ReflectionUtils.findField(theClass, fieldName);
		Annotation[] annotations = field.getAnnotations();
		for (Annotation annotation : annotations) {
			if(annotation instanceof JoinColumn){
				JoinColumn joinColumn = (JoinColumn) annotation;
				if(joinColumn.nullable())
					return JoinType.LEFT_OUTER_JOIN;
			}else if(annotation instanceof PrimaryKeyJoinColumn){
				return JoinType.LEFT_OUTER_JOIN;
			}else if(annotation instanceof JoinTable){
				JoinTable joinTable = (JoinTable) annotation;
				JoinColumn[] joinColumns = joinTable.joinColumns();
				for (JoinColumn joinColumn : joinColumns) {
					if(joinColumn.nullable()){
						return JoinType.LEFT_OUTER_JOIN;
					}
				}
			}else if(annotation instanceof Column){
				if(((Column) annotation).nullable()){
					return JoinType.LEFT_OUTER_JOIN;
				}
			}else if(annotation instanceof OneToOne){
				if(((OneToOne) annotation).optional()){
					return JoinType.LEFT_OUTER_JOIN;
				}
			}
		}
		return JoinType.INNER_JOIN;
	}
	
	/**
	 * @param field
	 * @return org.hibernate.sql.JoinType
	 * @throws SecurityException
	 * @throws NoSuchFieldException
	 * getting join type based on nullable in oneToMany, ManyToOne or ManyToMany (Association)
	 */
	public static JoinType getJoinType(Field field) throws SecurityException, NoSuchFieldException{
		Annotation[] annotations = field.getAnnotations();
		for (Annotation annotation : annotations) {
			if(annotation instanceof JoinColumn){
				JoinColumn joinColumn = (JoinColumn) annotation;
				if(joinColumn.nullable())
					return JoinType.LEFT_OUTER_JOIN;
			}else if(annotation instanceof PrimaryKeyJoinColumn){
				return JoinType.LEFT_OUTER_JOIN;
			}else if(annotation instanceof JoinTable){
				JoinTable joinTable = (JoinTable) annotation;
				JoinColumn[] joinColumns = joinTable.joinColumns();
				for (JoinColumn joinColumn : joinColumns) {
					if(joinColumn.nullable()){
						return JoinType.LEFT_OUTER_JOIN;
					}
				}
			}else if(annotation instanceof Column){
				if(((Column) annotation).nullable()){
					return JoinType.LEFT_OUTER_JOIN;
				}
			}else if(annotation instanceof OneToOne){
				if(((OneToOne) annotation).optional()){
					return JoinType.LEFT_OUTER_JOIN;
				}
			}
		}
		return JoinType.INNER_JOIN;
	}
	
	public static JoinType getJoinType(Field field, String[] projections, HashMap<String, JoinType> joinOverrides) throws SecurityException, NoSuchFieldException{
		String newProjections = StringUtils.join(ArrayUtils.remove(projections, (projections.length - 1)), ".");
		if(joinOverrides != null && joinOverrides.get(newProjections) != null){
			return joinOverrides.get(newProjections);
		}
		return getJoinType(field);
	}
	
	/**
	 * @param joinType
	 * @return String of JoinType
	 * getting Join Name for logging
	 */
	public static String getJoinName(JoinType joinType){
		if(JoinType.INNER_JOIN == joinType){
			return "Inner";
		}
		return "Left";
	}
	
}
