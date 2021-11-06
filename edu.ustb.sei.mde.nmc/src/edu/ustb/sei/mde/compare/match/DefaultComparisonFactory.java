package edu.ustb.sei.mde.compare.match;

import static com.google.common.base.Preconditions.checkNotNull;

import edu.ustb.sei.mde.compare.CompareFactory;
import edu.ustb.sei.mde.compare.Comparison;
import edu.ustb.sei.mde.compare.IComparisonFactory;
import edu.ustb.sei.mde.compare.IEqualityHelper;
import edu.ustb.sei.mde.compare.IEqualityHelperFactory;


/**
 * A default implementation of {@link IComparisonFactory} that creates a new {@link Comparison} through the
 * {@link CompareFactory#eINSTANCE default CompareFactory}.
 * 
 * @author <a href="mailto:mikael.barbero@obeo.fr">Mikael Barbero</a>
 */
public class DefaultComparisonFactory implements IComparisonFactory {

	/** The factory used to instantiate IEqualityHelper to associate with Comparison. */
	private final IEqualityHelperFactory equalityHelperFactory;

	/**
	 * Creates a new DefaultComparisonFactory.
	 * 
	 * @param equalityHelperFactory
	 *            The factory used to instantiate IEqualityHelper to associate with Comparison.
	 */
	public DefaultComparisonFactory(IEqualityHelperFactory equalityHelperFactory) {
		this.equalityHelperFactory = checkNotNull(equalityHelperFactory);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.emf.compare.match.IComparisonFactory#createComparison()
	 */
	public Comparison createComparison() {
		Comparison comparison = CompareFactory.eINSTANCE.createComparison();

		IEqualityHelper equalityHelper = equalityHelperFactory.createEqualityHelper();

		comparison.eAdapters().add(equalityHelper);
		equalityHelper.setTarget(comparison);

		return comparison;
	}

}

