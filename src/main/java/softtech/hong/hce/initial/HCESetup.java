package softtech.hong.hce.initial;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.Map;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Id;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.PrimaryKeyJoinColumns;
import javax.persistence.Table;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.persister.entity.JoinedSubclassEntityPersister;
import org.hibernate.persister.entity.SingleTableEntityPersister;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.filter.ElementFilter;
import org.jdom2.input.SAXBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import softtech.hong.hce.constant.DialectType;
import softtech.hong.hce.constant.HCEConstant;
import softtech.hong.hce.exception.HCEErrorException;
import softtech.hong.hce.utils.ReflectionUtils;

/**
 * @author Kisman Hong
 * initial class for storing entity information
 */
public class HCESetup {
	private static Logger log = LoggerFactory.getLogger(HCESetup.class);
	
	private SessionFactory sessionFactory;
	
	private EntityManagerFactory entityManagerFactory;
	
	/**
	 * setting / assign Hibernate Session Factory to HCE
	 * @param sessionFactory
	 */
	public void setSessionFactory(SessionFactory sessionFactory){
		this.sessionFactory = sessionFactory;
	}
	
	public void setEntityManagerFactory(EntityManagerFactory entityManagerFactory){
		this.entityManagerFactory = entityManagerFactory;
	}
	
//	public HCESetup() throws Exception {
//		cacheEntityToTable();
//	}

	/**
	 * collection table name from entity and store to Map
	 * @throws Exception
	 */
	public void cacheTableNameForEntity() throws Exception{
		if(sessionFactory != null){
			cacheInfo(sessionFactory);
		}else if(entityManagerFactory != null){
			EntityManager entityManager = entityManagerFactory.createEntityManager();
//			Metamodel metamodel = entityManager.getMetamodel();
//			Set<EntityType<?>> entityTypes = metamodel.getEntities();
//			for (EntityType<?> entityType : entityTypes) {
//				System.out.println(ToStringBuilder.reflectionToString(entityType));
//			}
			Session session = (Session) entityManager.getDelegate();
			cacheInfo(session.getSessionFactory());
		}else {
			throw new HCEErrorException("SessionFactory or EntityManagerFactory is null, you must set one of them");
		}
		
	}
	
	public void cacheTableNameForEntity(Class<?>[] classes, DialectType dialectType) throws Exception{
		for (Class<?> clazz : classes) {
			PrimaryKeyJoinColumn primaryKeyJoinColumn = clazz.getAnnotation(PrimaryKeyJoinColumn.class);
			PrimaryKeyJoinColumns primaryKeyJoinColumns = clazz.getAnnotation(PrimaryKeyJoinColumns.class);
			if(primaryKeyJoinColumn == null && primaryKeyJoinColumns == null){
			
				Field[] fields = ReflectionUtils.getInheritedPrivateFields(clazz);
				for (Field field : fields) {
					Id id = field.getAnnotation(Id.class);
					if(id != null){
						putIdentifyProperty(clazz.getCanonicalName(), field.getName());
						
						Column column = field.getAnnotation(Column.class);
						if(column != null){
							putIdentifyColumnName(clazz.getCanonicalName(), new String[]{ StringUtils.isEmpty(column.name())?field.getName() : column.name() });
						}else{
							putIdentifyColumnName(clazz.getCanonicalName(), new String[]{ field.getName() });
						}
					}				
				}
			}else{
				if(primaryKeyJoinColumn != null){
					putIdentifyColumnName(clazz.getCanonicalName(), new String[]{ primaryKeyJoinColumn.name()});
				}else{
					String[] columns = {};
										
					PrimaryKeyJoinColumn[] primaryKeyJoinColumns2 = primaryKeyJoinColumns.value();
					for (PrimaryKeyJoinColumn primaryKeyJoinColumn2 : primaryKeyJoinColumns2) {
						columns = ArrayUtils.add(columns, primaryKeyJoinColumn2.name());
					}
					
					putIdentifyColumnName(clazz.getCanonicalName(), columns);
				}
			}
			
			Entity entity = clazz.getAnnotation(Entity.class);
			Table table = clazz.getAnnotation(Table.class);
			String tableName;
			if(entity != null){
				tableName = StringUtils.isEmpty(entity.name())?clazz.getSimpleName() : entity.name();
			}else if(table != null){
				tableName = StringUtils.isEmpty(table.name())?clazz.getSimpleName() : table.name();
			}else {
				tableName = clazz.getSimpleName();
			}
			putEntity(clazz.getCanonicalName(), tableName);
			log.info("Map from Hibernate Entity {} to {}", clazz.getCanonicalName(), tableName);
		}
		
//		Dialect dialect = ((SessionFactoryImplementor) sessionFactory).getDialect();
		HCEConstant.DIALECT_TYPE = dialectType;
	}
	
	private void putEntity(String key, String value){
		HCEConstant.ENTITY_TO_TABLE.put(key, value);
	}
	
	private void putIdentifyProperty(String key, String value){
		HCEConstant.ENTITY_IDENTIFIER_PROPERTY.put(key, value);
	}
	
	private void putIdentifyColumnName(String key, String[] value){
		HCEConstant.ENTITY_IDENTIFIER_PRIMARY_COLUMN.put(key, value);
	}
	
	public void addExcludeType(Class<?> ... types){
		HCEConstant.BY_PASS_TYPE = (Class<?>[]) ArrayUtils.addAll(HCEConstant.BY_PASS_TYPE, types);
	}
	
	@SuppressWarnings("unchecked")
	public void cacheTableNameForEntity(String configPath, DialectType dialectType){
		SAXBuilder builder = new SAXBuilder();
		
		InputStream in = this.getClass().getClassLoader().getResourceAsStream(configPath);
	 
		  try {
	 
			Document document = (Document) builder.build(in);
			Element rootNode = document.getRootElement();
			
			ElementFilter filter=new ElementFilter("class");
			
			Class<?>[] classes = {};			
			
			Iterator<Element> iterable = rootNode.getDescendants(filter);
			while(iterable.hasNext()) {
		         Element element = iterable.next();
		         classes = ArrayUtils.addAll(classes, Class.forName(element.getText()));
		    }
			
			cacheTableNameForEntity(classes, dialectType);
	 
		  } catch (Exception io) {
			throw new HCEErrorException("Cannot read class from hibernate configuration xml caused by " + io.getMessage());
		 }
	}
	
	private void cacheInfo(SessionFactory sessionFactory){
		Map<String, ClassMetadata> map = sessionFactory.getAllClassMetadata();
		for (Iterator<?> iterator = map.keySet().iterator(); iterator.hasNext();) {
			String key = (String) iterator.next();
			if(((Object)map.get(key)) instanceof SingleTableEntityPersister){
				SingleTableEntityPersister singleTableEntityPersister = (SingleTableEntityPersister)map.get(key);
				putIdentifyProperty(key, singleTableEntityPersister.getIdentifierPropertyName());
				putEntity(key, singleTableEntityPersister.getRootTableName());
				putIdentifyColumnName(key, singleTableEntityPersister.getIdentifierColumnNames());
				log.info("Map from Hibernate Entity {} to {}", key, singleTableEntityPersister.getRootTableName());
			}else if(((Object)map.get(key)) instanceof JoinedSubclassEntityPersister){
				JoinedSubclassEntityPersister joinedSubclassEntityPersister = (JoinedSubclassEntityPersister) map.get(key);
				putIdentifyProperty(key, joinedSubclassEntityPersister.getIdentifierPropertyName());
				putEntity(key, joinedSubclassEntityPersister.getRootTableName());
				putIdentifyColumnName(key, joinedSubclassEntityPersister.getIdentifierColumnNames());
				log.info("Map from Hibernate Entity {} to {}", key, joinedSubclassEntityPersister.getRootTableName());
			}
		}
		Dialect dialect = ((SessionFactoryImplementor) sessionFactory).getDialect();
		String dialectName = dialect.toString().toLowerCase();
		if(StringUtils.contains(dialectName, "mysql")){
			HCEConstant.DIALECT_TYPE = DialectType.MySQL;
		}else if(StringUtils.contains(dialectName, "postgresql")){
			HCEConstant.DIALECT_TYPE = DialectType.PostgreSQL;
		}else if(StringUtils.contains(dialectName, "oracle")){
			HCEConstant.DIALECT_TYPE = DialectType.Oracle;
		}else if(StringUtils.contains(dialectName, "db2")){
			HCEConstant.DIALECT_TYPE = DialectType.DB2;
		}else{
			HCEConstant.DIALECT_TYPE = DialectType.SAPDB;
		}
	}
}
