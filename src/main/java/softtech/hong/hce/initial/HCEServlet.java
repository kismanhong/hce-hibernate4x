/**
 *
 */
package softtech.hong.hce.initial;

import java.util.Set;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import softtech.hong.hce.constant.DialectType;
import softtech.hong.hce.utils.ClassUtils;

// TODO: Auto-generated Javadoc
/**
 * The Class ContextListener.
 * 
 * @author Kisman Hong
 */
public class HCEServlet implements ServletContextListener {

	/** The Constant logger. */
	private static Log logger = LogFactory.getLog(HCEServlet.class);

	/** The context. */
	private ServletContext context = null;
	
	/**
	 * Record the fact that this web application has been destroyed.
	 * 
	 * @param event
	 *            The servlet context event
	 */
	public void contextDestroyed(ServletContextEvent event) {
		try {

		} catch (Exception e) {
			log("Nothing happened", e);
		}

		log("Web application shutdown");
		this.context = null;
	}

	/**
	 * Record the fact that this web application has been initialized.
	 * 
	 * @param event
	 *            The servlet context event
	 */
	public void contextInitialized(ServletContextEvent event) {
	
		try{
			log("Fetching informations for hibernate hce integration...");
			
			Set<Class<?>> classes = ClassUtils.getClasses("id.co.infoflow.valueobject");
			Class<?>[] classes2 = new Class<?>[classes.size()];
			int i =0;
			for (Class<?> class1 : classes) {
				classes2[i] = class1;
				i++;
			}
			
			HCESetup hceSetup = new HCESetup();
			hceSetup.cacheTableNameForEntity(classes2, DialectType.Oracle);
		} catch (Exception e) {			
			e.printStackTrace();
			log("HCE Integration failed, caused by : "+ e.getMessage());
		} 
	}

	/**
	 * Log a message to the servlet context application log.
	 * 
	 * @param message
	 *            Message to be logged
	 */
	private void log(String message) {
		if (context != null) {
			context.log(message);
		} else {
			System.out.println("ContextListener: " + message);
		}
	}

	/**
	 * Log a message and associated exception to the servlet context application
	 * log.
	 * 
	 * @param message
	 *            Message to be logged
	 * @param throwable
	 *            Exception to be logged
	 */
	private void log(String message, Throwable throwable) {
		logger.error(throwable.getMessage(), throwable);

		if (context != null) {
			context.log("ContextListener: " + message, throwable);
		} else {
			System.out.println("ContextListener: " + message);
			throwable.printStackTrace(System.out);
		}

	}

}
