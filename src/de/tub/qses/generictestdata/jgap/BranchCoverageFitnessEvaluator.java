package de.tub.qses.generictestdata.jgap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jgap.FitnessEvaluator;
import org.jgap.IChromosome;

public class BranchCoverageFitnessEvaluator implements FitnessEvaluator {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2717464912641087489L;
	
	private List<String> branches;
	
	private Map<String, Integer> visitedBranchesInWholePopulation;
	
	public BranchCoverageFitnessEvaluator(String[] branchesOfMethod) {
		branches = new ArrayList<String>();
		branches.addAll(branches);
		visitedBranchesInWholePopulation = new HashMap<String, Integer>();
	}

	/**
	 * Not supported for {@link BranchCoverageFitnessEvaluator}
	 */
	@Override
	public boolean isFitter(double arg0, double arg1) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isFitter(IChromosome chrome1, IChromosome chrome2) {
		double chrome1Weight = calculateChromeWeight(chrome1);
		double chrome2Weight = calculateChromeWeight(chrome2);
		return (chrome1Weight > chrome2Weight);
	}
	
	public void markBranchVisited(String branchName) {
		Integer visits = visitedBranchesInWholePopulation.get(branchName);
		if (visits == null) {
			visits = Integer.valueOf(0);
		}
		visits++;
		visitedBranchesInWholePopulation.put(branchName, visits);
	}
	
	public void resetVisitedBranches() {
		visitedBranchesInWholePopulation.clear();
	}
	
	@SuppressWarnings("unchecked")
	private double calculateChromeWeight(IChromosome chromosome) {
		List<String> visitedBranches = (List<String>) chromosome.getApplicationData();
		double weight = 0.0d;
		for (String branch : visitedBranches) {
			weight += calculateBranchWeight(branch);
		}
		return weight;
	}
	
	private double calculateBranchWeight(String branchName) {
		Integer visits = visitedBranchesInWholePopulation.get(branchName);
		if (visits == null) {
			return 0.0d;
		}
		double d = visits.doubleValue();
		return (double) 1.0d / d;
	}

}
