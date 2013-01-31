package de.tub.qses.generictestdata.genetic;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jgap.BulkFitnessFunction;
import org.jgap.Chromosome;
import org.jgap.Configuration;
import org.jgap.Gene;
import org.jgap.GeneticOperator;
import org.jgap.Genotype;
import org.jgap.IChromosome;
import org.jgap.InvalidConfigurationException;
import org.jgap.Population;
import org.jgap.impl.BestChromosomesSelector;
import org.jgap.impl.DefaultConfiguration;
import org.jgap.impl.DoubleGene;
import org.jgap.impl.IntegerGene;
import org.jgap.impl.MutationOperator;

import de.tub.qses.generictestdata.annotations.BoundHint;
import de.tub.qses.generictestdata.annotations.CoverMethod;
import de.tub.qses.generictestdata.annotations.Hints;
import de.tub.qses.generictestdata.util.TestUtil;
import de.tub.qses.generictestdata.util.TestUtil.LookRecord;
import de.tub.qses.generictestdata.util.TestUtil.VisitRecord;

public class JGapTest {
	
	public final static int INITIAL_POPULATION_SIZE = 500;
	public final static int DEFAULT_EVOLUTION_COUNT = 100;

	private final Class<?> classUnderTest;
	private final Method methodUnderTest;
	private Class<?>[] methodParameters;
	
	private Configuration jgapConfiguration;
	
	private Genotype population;
	
	private int evoluationCount = DEFAULT_EVOLUTION_COUNT;
	private BranchCoverageFitnessEvaluator evaluator;
	
	private Set<String> visitedBranches = new HashSet<String>();
	private String[] branchesToVisit = null;
	
	private boolean allBranchesVisited = false;
	
	private Map<String, SuccessEntry> successEntries = new HashMap<String, SuccessEntry>();
	
	private int generation = 0;
	
	private MutationOperator mutationOperator;
	
	public Collection<SuccessEntry> getSuccessEntries() {
		return successEntries.values();
	}

	public boolean isAllBranchesVisited() {
		return allBranchesVisited;
	}

	public JGapTest(final Class<?> classUnderTest, final Method methodUnderTest) {
		this.classUnderTest = classUnderTest;
		this.methodUnderTest = methodUnderTest;
		
		methodParameters = methodUnderTest.getParameterTypes();
		
		Configuration.reset();
		jgapConfiguration = new DefaultConfiguration();
		jgapConfiguration.removeNaturalSelectors(true);
//		try {
//			BestChromosomesSelector chromosoneSelector = new BestChromosomesSelector(jgapConfiguration, 0.95d);
//			chromosoneSelector.setDoubletteChromosomesAllowed(true);
//			jgapConfiguration.addNaturalSelector(chromosoneSelector, true);
//		} catch (InvalidConfigurationException e) {
//			// shouldn't happen.
//			e.printStackTrace();
//		}
		Configuration.reset();
		
		branchesToVisit = getBranchesOfMethod(methodUnderTest);
		
		BoundHint[] hints = getMethodHints(methodUnderTest);
				
		evaluator = new BranchCoverageFitnessEvaluator(branchesToVisit);
		jgapConfiguration.setFitnessEvaluator(evaluator);
		jgapConfiguration.setPreservFittestIndividual(false);
		jgapConfiguration.setKeepPopulationSizeConstant(false);
		List operators = jgapConfiguration.getGeneticOperators();
		for (int i = 0; i < operators.size(); i++) {
			GeneticOperator operator = (GeneticOperator) operators.get(i);
			if (operator instanceof MutationOperator) {
				mutationOperator = (MutationOperator) operator;
				System.out.println("Mutation rate: "+mutationOperator.getMutationRate());
			}
		}
		
		BulkFitnessFunction bff = new BranchCoverageFitnessFunction();
		try {
			jgapConfiguration.setBulkFitnessFunction(bff);
			jgapConfiguration.setSampleChromosome(createSampleChromosone(jgapConfiguration, methodParameters, hints));
			jgapConfiguration.setPopulationSize(INITIAL_POPULATION_SIZE);
			population = Genotype.randomInitialGenotype(jgapConfiguration);
		} catch (InvalidConfigurationException e) {
			// should never happen... damn exceptions -.-
			e.printStackTrace();
		}
		
		
	}

	public int getPopulationCount() {
		return jgapConfiguration.getPopulationSize();
	}
	
	public int getCurrentGeneration() {
		return generation;
	}
	
	public boolean allBranchesVisited() {
		return allBranchesVisited;
	}
	
	public Set<String> getVisitedBranches() {
		return visitedBranches;
	}
	
	public void evolve() {
		population.evolve();
	}
	
	public int getEvoluationCount() {
		return evoluationCount;
	}

	public void setEvoluationCount(int evoluationCount) {
		this.evoluationCount = evoluationCount;
	}
	
	private String[] getBranchesOfMethod(Method methodUnderTest2) {
		String[] branches = null;
		Annotation[] annos = methodUnderTest2.getAnnotations();
		for (Annotation anno : annos) {
			if (anno.annotationType().getName().equals(CoverMethod.class.getName())) {
				try {
					// wow... da muss es doch was besseres geben?!
					Method m = anno.getClass().getClassLoader().loadClass(CoverMethod.class.getName()).getMethod("branches", (Class<?>[])null);
					branches = (String[]) m.invoke(anno, (Object[]) null);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		return branches;
	}
	
	
	private BoundHint[] getMethodHints(Method methodUnderTest2) {
		BoundHint[] hints = null;
		Annotation[] annos = methodUnderTest2.getAnnotations();
		for (Annotation anno : annos) {
			if (anno.annotationType().getName().equals(Hints.class.getName())) {
				try {
					// wow... da muss es doch was besseres geben?!
					Method m = anno.getClass().getClassLoader().loadClass(Hints.class.getName()).getMethod("bounds", (Class<?>[])null);
					hints = (BoundHint[]) m.invoke(anno, (Object[]) null);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		return hints;
	}

	private IChromosome createSampleChromosone(Configuration config, Class<?>[] methodParameters, BoundHint[] hints) throws InvalidConfigurationException {
		Gene[] sampleGenes = new Gene[methodParameters.length];
		
		boolean useHints = false;
		if (hints != null && hints.length > 0) {
			useHints = true;
			
		}
		for (int i = 0; i < methodParameters.length; i++) {
			Class<?> param = methodParameters[i];
			double hintMin = Double.NaN;
			double hintMax = Double.NaN;
			if (useHints) {
				for (int j = 0; j < hints.length; j++) {
					if (hints[j].paramId() == i) {
						hintMin = hints[j].min();
						hintMax = hints[j].max();
					}
				}
			}
			if (param.equals(Double.class) || param.equals(double.class)) {
				if (Double.isNaN(hintMin)) {
					sampleGenes[i] = new DoubleGene(config, hintMin, hintMax);
				} else {
					sampleGenes[i] = new DoubleGene(config, Double.MIN_VALUE, Double.MAX_VALUE);
				}
			} else if (param.equals(Integer.class) || param.equals(int.class)) {
				if (!Double.isNaN(hintMin)) {
					sampleGenes[i] = new IntegerGene(config, (int) hintMin, (int) hintMax);
				} else {
					sampleGenes[i] = new IntegerGene(config, Integer.MIN_VALUE, Integer.MAX_VALUE);
				}
			} else if (param.equals(Float.class) || param.equals(float.class)) {
				if (!Double.isNaN(hintMin)) {
					sampleGenes[i] = new DoubleGene(config, hintMin, hintMax);
				} else {
					sampleGenes[i] = new DoubleGene(config, Float.MAX_VALUE, Float.MIN_VALUE);
				}
			} else if (param.equals(Short.class) || param.equals(short.class)) {
				if (!Double.isNaN(hintMin)) {
					sampleGenes[i] = new IntegerGene(config, (int) hintMin, (int) hintMax);
				} else {
					sampleGenes[i] = new IntegerGene(config, Short.MAX_VALUE, Short.MIN_VALUE);
				}
			} else if (param.equals(Character.class) || param.equals(char.class)) {
				if (!Double.isNaN(hintMin)) {
					sampleGenes[i] = new IntegerGene(config, (int) hintMin, (int) hintMax);
				} else {
					sampleGenes[i] = new IntegerGene(config, Character.MIN_VALUE, Character.MAX_VALUE);
				}
			} 
		}
		return new Chromosome(jgapConfiguration, sampleGenes);
	}
	
	private class BranchCoverageFitnessFunction extends BulkFitnessFunction {

		/**
		 * 
		 */
		private static final long serialVersionUID = -3151224578623731689L;

		@Override
		public void evaluate(Population a_chromosomes) {
			Iterator<IChromosome> it = a_chromosomes.getChromosomes().iterator();
			try {
				Object objectUnderTest = classUnderTest.newInstance();
				while (it.hasNext()) {
					IChromosome chromosome = it.next();
					
					Object[] paramValues = new Object[chromosome.getGenes().length];
					for (int i = 0; i < chromosome.getGenes().length; i++) {
						Class<?> param = methodParameters[i];
						Gene g = chromosome.getGene(i);
						if (param.equals(Double.class) || param.equals(double.class)) {
							paramValues[i] = Double.valueOf(((DoubleGene) g).doubleValue());
						} else if (param.equals(Integer.class) || param.equals(int.class)) {
							paramValues[i] = Integer.valueOf(((IntegerGene) g).intValue());
						} else if (param.equals(Float.class) || param.equals(float.class)) {
							paramValues[i] = Float.valueOf((float) ((DoubleGene) g).doubleValue());
						} else if (param.equals(Short.class) || param.equals(short.class)) {
							paramValues[i] = Short.valueOf((short) ((IntegerGene) g).intValue());
						} else if (param.equals(Character.class) || param.equals(char.class)) {
							paramValues[i] = Character.valueOf((char) ((IntegerGene) g).intValue());
						}
					}
					
					methodUnderTest.invoke(objectUnderTest, paramValues);
					Field f = classUnderTest.getField(Constants.BRANCHES_FIELD_NAME);
					Set<String> watchedNodes = TestUtil.getWatchedNodes();
					for (VisitRecord vr : TestUtil.getVisitedBranches()) {
						List<Object> params = new ArrayList<Object>();
						Collections.addAll(params, paramValues);
						successEntries.put(vr.getNode().getIdentifier(), new SuccessEntry(vr.getNode().getIdentifier(), params));
						if (watchedNodes.contains(vr.getNode().getIdentifier())) {
							System.out.println("\n\nBranch: "+vr.getNode().getIdentifier()+"\n\n");
						}
					}
					double fitness = 0.0d;
					for (LookRecord lr : TestUtil.getLookingBranches()) {
						double nodeFitness = Fitness.evaluateFitness(lr.getA(), lr.getB()); 
						if (watchedNodes.contains(lr.getNode().getIdentifier()) && nodeFitness > 0.0001d) {
							System.out.println("Branch: "+lr.getNode().getIdentifier());
							System.out.println("Current fitness: "+nodeFitness);
							System.out.println("A = "+lr.getA()+", B = "+lr.getB());
						}
						fitness += nodeFitness;
					}
					chromosome.setFitnessValue(fitness);
					if (successEntries.size() == TestUtil.getAllBranches().size() && !allBranchesVisited) {
						System.out.println("FERTIG!");
						allBranchesVisited = true;
						TestUtil.getVisitedBranches().clear();
						TestUtil.getLookingBranches().clear();
						
					}
					TestUtil.getVisitedBranches().clear();
					TestUtil.getLookingBranches().clear();
				} 
			} catch (Exception e) {
				e.printStackTrace();
			}
			generation++;
		}
		
		public Object clone() {
			return new BranchCoverageFitnessFunction();
		}
	}
	
}
