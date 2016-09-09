package softtech.hong.hce.exception;

import org.hibernate.HibernateException;

/**
 * @author Kisman Hong
 * @email kismanhong@gmail.com
 * HCEErrorException is used for handling the exception by code
 * this is used for java 1.5 above
 * 
 */
public class HCEErrorException extends HibernateException {
    /**
	 * 
	 */
	private static final long serialVersionUID = -3224929220568773428L;

    /**
     * error/exception code
     */
    private String code;

    /**
     * error description value/s
     */
    private Object[] parameterValues = null;

    /**
     * @param message -> message to be throw
     * constructor for accepting message
     */
    public HCEErrorException(String message) {
        super(message);
        setCode(message);
    }

    /**
     * @param message -> message to be throw
     * @param code -> error code to be assigned
     * constructor for accepting message and code
     */
    public HCEErrorException(String message, String code) {
        super(message);
        setCode(code);
    }


    /**
     * @param message -> message to be throw
     * @param code -> error code to be assigned
     * @param params -> message params object
     * constructor for accepting message, code, and params
     */
    public HCEErrorException(String message, String code, Object[] params) {
        super(message);
        setCode(code);
        setParameterValues(params);
    }

    /**
     * @param cause
     */
    public HCEErrorException(Throwable cause) {
        super(cause);
        if (cause instanceof HCEErrorException) {
            setCode(((HCEErrorException) cause).getCode());
            setParameterValues(((HCEErrorException) cause).getParameterValues());
        }
    }

    /**
     * @param message -> message to be throw
     * @param code -> error code to be assigned
     */
    public HCEErrorException(String message, Throwable cause) {
        super(message, cause);
        setCode(message);
    }

    /**
     * @param code
     * @param params
     * @param cause
     */
    public HCEErrorException(String code, Object[] params, Throwable cause) {
        super(cause);
        setCode(code);
        setParameterValues(params);
    }

    /**
     * @param code
     * @param param
     * @param cause
     */
    public HCEErrorException(String code, Object param, Throwable cause) {
        this(code, new Object[]{param}, cause);
    }
 
    /**
     * @return
     */
    public String getHCERootMessage() {
        Throwable cause = this;
        while (cause.getCause() != null && cause.getCause() instanceof HCEErrorException) {
            cause = cause.getCause();
        }
        return ((HCEErrorException) cause).getLocalizedMessage();
    }

	/**
	 * @return
	 */
	public String getRootMessage() {
		Throwable cause = this;
		while (cause.getCause() != null) {
			cause = cause.getCause();
		}
		return cause.getLocalizedMessage();
	}
	
	/**
     * @return
     */
    public String getCode() {
        return code;
    }


    /**
     * @param code
     */
    public void setCode(String code) {
        this.code = code;
    }

    /**
     * @return
     */
    public Object[] getParameterValues() {
        return parameterValues;
    }

    /**
     * @param objects
     */
    public void setParameterValues(Object[] objects) {
        parameterValues = objects;
    }
}
