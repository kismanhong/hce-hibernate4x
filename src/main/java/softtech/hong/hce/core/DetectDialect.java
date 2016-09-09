package softtech.hong.hce.core;

import softtech.hong.hce.constant.DialectType;
import softtech.hong.hce.constant.HCEConstant;

public class DetectDialect {
	public static DialectType getDialect(){
//		Dialect dialect = HCEConstant.dialect;
//		String dialectName = dialect.toString().toLowerCase();
//		if(StringUtils.contains(dialectName, "mysql")){
//			return DialectType.MySQL;
//		}else if(StringUtils.contains(dialectName, "postgresql")){
//			return DialectType.PostgreSQL;
//		}else if(StringUtils.contains(dialectName, "oracle")){
//			return DialectType.Oracle;
//		}else if(StringUtils.contains(dialectName, "db2")){
//			return DialectType.DB2;
//		}else{
//			return DialectType.SAPDB;
//		}
		return HCEConstant.DIALECT_TYPE;
	}
}
