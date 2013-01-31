package de.tub.qses.generictestdata.eclipse;

import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import de.tub.qses.generictestdata.genetic.Constants;

public class Configuration extends PreferencePage implements IWorkbenchPreferencePage {

	private Spinner spnPopulation;
	private Spinner spnGenerations;

	/**
	 * Create the preference page.
	 */
	public Configuration() {
	}

	/**
	 * Create contents of the preference page.
	 * @param parent
	 */
	@Override
	public Control createContents(Composite parent) {
		Composite container = new Composite(parent, SWT.NULL);
		container.setLayout(new GridLayout(2, false));
		
		Label lblPopulationSize = new Label(container, SWT.NONE);
		lblPopulationSize.setText("Population size:");
		
		spnPopulation = new Spinner(container, SWT.BORDER);
		spnPopulation.setMaximum(100000);
		spnPopulation.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				Constants.DEFAULT_POPULATION = spnPopulation.getSelection();
			}
		});
		
		Label lblEvolutionarySteps = new Label(container, SWT.NONE);
		lblEvolutionarySteps.setText("Evolutionary steps:");
		
		spnGenerations = new Spinner(container, SWT.BORDER);
		spnGenerations.setMaximum(100000);
		spnGenerations.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				Constants.MAX_GENERATIONS = spnGenerations.getSelection();
			}
		});

		spnPopulation.setSelection(Constants.DEFAULT_POPULATION);
		spnGenerations.setSelection(Constants.MAX_GENERATIONS);
		return container;
	}

	/**
	 * Initialize the preference page.
	 */
	public void init(IWorkbench workbench) {
		// Initialize the preference page
		
	}
}
