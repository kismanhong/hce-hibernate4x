package softtech.hong.hce.model;

import org.hibernate.criterion.Junction;

public class Disjunction extends Junction {
	/**
	 * 
	 */
	private static final long serialVersionUID = 4768765553482415715L;

	protected Disjunction() {
		super( Nature.OR );
	}
}