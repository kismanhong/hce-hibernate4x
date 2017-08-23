package softtech.hong.hce.type;

/**
 * @author Kisman Hong
 * restriction query type
 */
public enum RestrictionType {
	AND,
	EQ,
	EQ_PROPERTY,
	GT,
	GT_PROPERTY,
	GE,
	GE_PROPERTY,
	LT,
	LT_PROPERTY,
	LE,
	LE_PROPERTY,
	NE,
	NE_PROPERTY,
	ILIKE,
	LIKE,
	IN,
	EMPTY,
	NOT_EMPTY,
	NULL,
	NOT_NULL,
	OR,
	BETWEEN,
	PROPERTY_BETWEEN,
	BETWEEN_PROPERTY,
	MONTH_EQ,
	SIZE_EQ,
	SIZE_GT,
	SIZE_NE,
	SIZE_LT,
	SIZE_GE,
	SIZE_LE,
	DATE_EQ,
	NOT,
	EQ_OR_IS_NULL,
	SQL,
	MAX,
	MIN,
	YEAR_EQ;
}
