package softtech.hong.hce.model;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.lang3.ArrayUtils;
import org.hibernate.criterion.Conjunction;
import org.hibernate.criterion.Junction;
import org.hibernate.criterion.MatchMode;

import softtech.hong.hce.type.JunctionType;
import softtech.hong.hce.type.RestrictionType;


/**
 * @author kismanhong
 * this class is used for defining restriction of object's properties
 */
public class Expression {
	
	private static SimpleDateFormat monthSdf = new SimpleDateFormat("MM");
	
	private String propertyName;
	
	private Object value;
	
	private Object value1;
	
	private RestrictionType restrictionType;
	
	private MatchMode matchMode;
	
	private JunctionType junctionType;
	
	private Expression expression;
	
	private Expression expression2;
	
	private Expression[] expressions;
	
	private Junction junction;

	public Expression(){}
	
	public Expression(Junction junction){
		this.restrictionType = RestrictionType.OR;
		this.junction = junction;
		add(this);
	}
	
	public Expression(String propertyName, Object value){
		this.propertyName = propertyName;
		this.value = value;
		this.restrictionType = RestrictionType.EQ;
		this.junctionType = JunctionType.AND;
		add(this);
	}
	
	public Expression(String propertyName, RestrictionType restrictionType){
		this.propertyName = propertyName;
		this.restrictionType = restrictionType;
		this.junctionType = JunctionType.AND;
		add(this);
	}
	
	public Expression(String propertyName, Object lo, Object hi){
		this.propertyName = propertyName;
		this.value = lo;
		this.value1 = hi;
		this.restrictionType = RestrictionType.BETWEEN;
		this.junctionType = JunctionType.AND;
		add(this);
	}
	
//	public Expression(Expression expression, Expression expression2){
//		this.expression = expression;
//		this.expression2 = expression2;
//		this.restrictionType = RestrictionType.OR;
//		this.junctionType = JunctionType.AND;
//		add(this);
//	}
	
	public Expression(String propertyName, Object value, RestrictionType restrictionType){
		this.propertyName = propertyName;
		this.value = value;
		this.restrictionType = restrictionType;
		this.junctionType = JunctionType.AND;
		add(this);
	}
	
	public Expression(String propertyName, Object value, Object value1, RestrictionType restrictionType){
		this.propertyName = propertyName;
		this.value = value;
		this.value1 = value1;
		this.restrictionType = restrictionType;
		this.junctionType = JunctionType.AND;
		add(this);
	}
	
	public Expression(String propertyName, Object value, RestrictionType restrictionType, MatchMode matchMode){
		this.propertyName = propertyName;
		this.value = value;
		this.restrictionType = restrictionType;
		this.junctionType = JunctionType.AND;
		this.matchMode = matchMode;
		add(this);
	}
	
	public Expression(String propertyName, Object value, JunctionType junctionType){
		this.propertyName = propertyName;
		this.value = value;
		this.restrictionType = RestrictionType.EQ;
		this.junctionType = junctionType;
		add(this);
	}
	
	public Expression(String propertyName, Object value, RestrictionType restrictionType, JunctionType junctionType){
		this.propertyName = propertyName;
		this.value = value;
		this.restrictionType = restrictionType;
		this.junctionType = junctionType;
		add(this);
	}
	
	public Expression(RestrictionType restrictionType, Expression... express){
//		this.restrictionType = restrictionType;
//		expressions = express;
		Expression expression = new Expression();
		expression.setRestrictionType(restrictionType);
		expression.setExpressions(express);
		add(expression);
	}

	public String getPropertyName() {
		return propertyName;
	}

	public void setPropertyName(String propertyName) {
		this.propertyName = propertyName;
	}

	public Object getValue() {
		return value;
	}

	public void setValue(Object value) {
		this.value = value;
	}

	public Object getValue1() {
		return value1;
	}

	public void setValue1(Object value1) {
		this.value1 = value1;
	}

	public RestrictionType getRestrictionType() {
		return restrictionType;
	}

	public void setRestrictionType(RestrictionType restrictionType) {
		this.restrictionType = restrictionType;
	}

	public JunctionType getJunctionType() {
		return junctionType;
	}

	public void setJunctionType(JunctionType junctionType) {
		this.junctionType = junctionType;
	}

	public Expression getPropertyValue() {
		return expression;
	}

	public Expression getPropertyValue2() {
		return expression2;
	}
	
	public void add(Expression expression){
		expressions = (Expression[]) ArrayUtils.add(expressions, expression);
	}
	
	public void addOr(Expression expression){
//		this.restrictionType = RestrictionType.OR;
		expression.setRestrictionType(RestrictionType.OR);
		expressions = (Expression[]) ArrayUtils.add(expressions, expression);
//		return this;	
//		Expression expression = new Expression();
//		expression.setRestrictionType(restrictionType);
//		expression.setExpressions(express);
//		add(expression);
	}
	
	public Expression[] getExpressions(){
		return expressions;
	}

	public MatchMode getMatchMode() {
		return matchMode;
	}
	
	public Junction getJunction() {
		return junction;
	}

	public static Expression eq(String propertyName, Object value){
		return new Expression(propertyName, value);
	}
	
	public static Expression yearEq(String propertyName, Object value){
		return new Expression(propertyName, value, RestrictionType.YEAR_EQ);
	}
	
	public static Expression eqProperty(String leftProperty, String rightProperty){
		return new Expression(leftProperty, rightProperty, RestrictionType.EQ_PROPERTY);
	}
	
	public static Expression eqOrIsNull(String propertyName, Object value){
		return new Expression(propertyName, value, RestrictionType.EQ_OR_IS_NULL);
	}
	
	public static Expression like(String propertyName, Object value, MatchMode matchMode){
		return new Expression(propertyName, value, RestrictionType.LIKE, matchMode);
	}
	
	public static Expression ilike(String propertyName, Object value, MatchMode matchMode){
		return new Expression(propertyName, value, RestrictionType.ILIKE, matchMode);
	}
	
	public static Expression gt(String propertyName, Object value){
		return new Expression(propertyName, value, RestrictionType.GT);
	}
	
	public static Expression ge(String propertyName, Object value){
		return new Expression(propertyName, value, RestrictionType.GE);
	}
	
	public static Expression lt(String propertyName, Object value){
		return new Expression(propertyName, value, RestrictionType.LT);
	}
	
	public static Expression le(String propertyName, Object value){
		return new Expression(propertyName, value, RestrictionType.LE);
	}
	
	public static Expression ne(String propertyName, Object value){
		return new Expression(propertyName, value, RestrictionType.NE);
	}
	
	public static Expression neProperty(String propertyName, Object value){
		return new Expression(propertyName, value, RestrictionType.NE_PROPERTY);
	}
	
	public static Expression gtProperty(String propertyName, Object value){
		return new Expression(propertyName, value, RestrictionType.GT_PROPERTY);
	}
	
	public static Expression geProperty(String propertyName, Object value){
		return new Expression(propertyName, value, RestrictionType.GE_PROPERTY);
	}
	
	public static Expression ltProperty(String propertyName, Object value){
		return new Expression(propertyName, value, RestrictionType.LT_PROPERTY);
	}
	
	public static Expression leProperty(String propertyName, Object value){
		return new Expression(propertyName, value, RestrictionType.LE_PROPERTY);
	}
	
	public static Expression in(String propertyName, Object value){
		return new Expression(propertyName, value, RestrictionType.IN);
	}
	
	public static Expression isNull(String propertyName){
		return new Expression(propertyName, null, RestrictionType.NULL);
	}
	
	public static Expression isNotNull(String propertyName){
		return new Expression(propertyName, null, RestrictionType.NOT_NULL);
	}
	
	public static Expression max(String propertyName, String referencePropertyName, Object value){
		return new Expression(propertyName, referencePropertyName, value, RestrictionType.MAX);
	}
	
	public static Expression isEmpty(String propertyName){
		return new Expression(propertyName, null, RestrictionType.EMPTY);
	}
	
	public static Expression isNotEmpty(String propertyName){
		return new Expression(propertyName, null, RestrictionType.NOT_EMPTY);
	}
	
	public static Expression or(Expression leftPropertyValue, Expression rightPropertyValue){
		Expression expression = new Expression(RestrictionType.OR, new Expression[]{leftPropertyValue, rightPropertyValue});
		expression.setRestrictionType(RestrictionType.OR);
		return expression;
//		return new Expression(RestrictionType.OR, new Expression[]{leftPropertyValue, rightPropertyValue});
	}
	
	public static Expression and(Expression leftPropertyValue, Expression rightPropertyValue){
		Expression expression = new Expression(RestrictionType.AND, new Expression[]{leftPropertyValue, rightPropertyValue});
		expression.setRestrictionType(RestrictionType.AND);
		return expression;
//		return new Expression(RestrictionType.AND, new Expression[]{leftPropertyValue, rightPropertyValue});
	}
	
	public static Expression between(String propertyName, Object lo, Object hi){
		return new Expression(propertyName, lo, hi);
	}
	
	public static Expression propertyBetween(String propertyName, Object lo, Object hi){
		return new Expression(propertyName, lo, hi, RestrictionType.PROPERTY_BETWEEN);
	}
	
	public static Expression betweenProperty(String value, String propertyName1, String propertyName2){
		return new Expression(value, propertyName1, propertyName2, RestrictionType.BETWEEN_PROPERTY);
	}
	
	public static Expression monthEq(String propertyName, Date date){
		return new Expression(propertyName, date, RestrictionType.MONTH_EQ);
	}
	
	public static Expression monthEq(String propertyName, int month){
		Date date;
		try {
			date = monthSdf.parse(String.valueOf(month));
		} catch (ParseException e) {
			e.printStackTrace();
			date = new Date();
		}
		return new Expression(propertyName, date, RestrictionType.MONTH_EQ);
	}
	
	/**
	 * Constrain a collection valued property by size
	 */
	public static Expression sizeEq(String propertyName, int size) {
		return new Expression(propertyName, size, RestrictionType.SIZE_EQ);
	}
	
	/**
	 * Constrain a collection valued property by size
	 */
	public static Expression sizeNe(String propertyName, int size) {
		return new Expression(propertyName, size, RestrictionType.SIZE_NE);
	}
	
	/**
	 * Constrain a collection valued property by size
	 */
	public static Expression sizeGt(String propertyName, int size) {
		return new Expression(propertyName, size, RestrictionType.SIZE_GT);
	}
	
	/**
	 * Constrain a collection valued property by size
	 */
	public static Expression sizeLt(String propertyName, int size) {
		return new Expression(propertyName, size, RestrictionType.SIZE_LT);
	}
	
	/**
	 * Constrain a collection valued property by size
	 */
	public static Expression sizeGe(String propertyName, int size) {
		return new Expression(propertyName, size, RestrictionType.SIZE_GE);
	}
	
	/**
	 * Constrain a collection valued property by size
	 */
	public static Expression sizeLe(String propertyName, int size) {
		return new Expression(propertyName, size, RestrictionType.SIZE_LE);
	}
	
	public static Expression dateEq(String propertyName, String date, String format) {
		return new Expression(propertyName, date, format, RestrictionType.DATE_EQ);
	}
	
	public static Disjunction disjunction() {
		return new Disjunction();
	}
	
	public static Conjunction conjunction() {
		return new Conjunction();
	}
	
	public static Expression or(Expression ... expressions){
		Expression expression = new Expression(RestrictionType.OR, expressions);
		expression.setRestrictionType(RestrictionType.OR);
		return expression;
	}
	
	public static Expression and(Expression ... expressions){
		Expression expression = new Expression(RestrictionType.AND, expressions);
		expression.setRestrictionType(RestrictionType.AND);
		return expression;
//		return new Expression(RestrictionType.AND, expressions);
	}
	
	public static Expression not(Expression expression){
		return new Expression(RestrictionType.NOT, expression);
	}
	
	public static Expression sql(String sql){
		return new Expression("-sql-restriction-", sql, RestrictionType.SQL);
	}
	
	public void setExpressions(Expression...expressions){
		this.expressions = expressions;
	}
}
