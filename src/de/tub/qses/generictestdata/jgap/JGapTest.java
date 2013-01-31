package de.tub.qses.generictestdata.jgap;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.jgap.BulkFitnessFunction;
import org.jgap.Chromosome;
import org.jgap.Configuration;
import org.jgap.Gene;
import org.jgap.Genotype;
import org.jgap.IChromosome;
import org.jgap.InvalidConfigurationException;
import org.jgap.Population;
import org.jgap.impl.BestChromosomesSelector;
import org.jgap.impl.DefaultConfiguration;
import org.jgap.impl.DoubleGene;
import org.jgap.impl.IntegerGene;

import de.tub.qses.generictestdata.annotations.CoverMethod;

public class JGapTest {
	
	public final static int INITIAL_POPULATION_SIZE = 500;
	public final static int DEFAULT_EVOLUTION_COUNT = 20;

	private final Class<?> classUnderTest;
	private final Method methodUnderTest;
	private Class<?>[] methodParameters;
	
	private Configuration jgapConfiguration;
	
	private Genotype population;
	
	private int evoluationCount = DEFAULT_EVOLUTION_COUNT;
	private BranchCoverageFitnessEvaluator evaluator;
	
	private Set<String> visitedBranches = new HashSet<String>();
	private String[] branchesToVisit = null;
	
	public JGapTest(final Class<?> classUnderTest, final Method methodUnderTest) {
		this.classUnderTest = classUnderTest;
		this.methodUnderTest = methodUnderTest;
		
		methodParameters = methodUnderTest.getParameterTypes();
		
		jgapConfiguration = new DefaultConfiguration();
		jgapConfiguration.removeNaturalSelectors(true);
		try {
			BestChromosomesSelector chromosoneSelector = new BestChromosomesSelector(jgapConfiguration, 0.95d);
			chromosoneSelector.setDoubletteChromosomesAllowed(true);
			jgapConfiguration.addNaturalSelector(chromosoneSelector, true);
		} catch (InvalidConfigurationException e) {
			// shouldn't happen.
			e.printStackTrace();
		}
		Configuration.reset();
		
		branchesToVisit = getBranchesOfMethod(methodUnderTest);
		
		evaluator = new BranchCoverageFitnessEvaluator(branchesToVisit);
		jgapConfiguration.setFitnessEvaluator(evaluator);
		jgapConfiguration.setPreservFittestIndividual(false);
		jgapConfiguration.setKeepPopulationSizeConstant(false);
		
		BulkFitnessFunction bff = new BranchCoverageFitnessFunction();
		try {
			jgapConfiguration.setBulkFitnessFunction(bff);
			jgapConfiguration.setSampleChromosome(createSampleChromosone(jgapConfiguration, methodParameters));
			jgapConfiguration.setPopulationSize(INITIAL_POPULATION_SIZE);
			population = Genotype.randomInitialGenotype(jgapConfiguration);
		} catch (InvalidConfigurationException e) {
			// should never happen... damn exceptions -.-
			e.printStackTrace();
		}
		
		
	}
	
	public boolean allBranchesVisited() {
		Set<String> visitedCopy = new HashSet<String>();
		visitedCopy.addAll(visitedBranches);
		for (String branchToVisit : branchesToVisit) {
			for (Iterator<String> visitedIt = visitedCopy.iterator(); visitedIt.hasNext();) {
				if (visitedIt.next().equals(branchToVisit)) {
					visitedIt.remove();
					break;
				}
			}
		}
		return visitedCopy.size() == 0;
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
				} catch (NoSuchMethodException e) {
					e.printStackTrace();
				} catch (SecurityException e) {
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				} catch (InvocationTargetException e) {
					e.printStackTrace();
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
				}
			}
		}
		return branches;
	}

	private IChromosome createSampleChromosone(Configuration config, Class<?>[] methodParameters) throws InvalidConfigurationException {
		Gene[] sampleGenes = new Gene[methodParameters.length];
		
		for (int i = 0; i < methodParameters.length; i++) {
			Class<?> param = methodParameters[i];
			if (param.equals(Double.class) || param.equals(double.class)) {
				sampleGenes[i] = new DoubleGene(config);
			} else if (param.equals(Integer.class) || param.equals(int.class)) {
				sampleGenes[i] = new IntegerGene(config);
			} else if (param.equals(Float.class) || param.equals(float.class)) {
				sampleGenes[i] = new DoubleGene(config, Float.MIN_VALUE, Float.MAX_VALUE);
			} else if (param.equals(Short.class) || param.equals(short.class)) {
				sampleGenes[i] = new IntegerGene(config, Short.MIN_VALUE, Short.MAX_VALUE);
			} else if (param.equals(Character.class) || param.equals(char.class)) {
				sampleGenes[i] = new IntegerGene(config, Character.MIN_VALUE, Character.MAX_VALUE);
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
				try {
					Object objectUnderTest = classUnderTest.newInstance();
					methodUnderTest.invoke(objectUnderTest, paramValues);
					Field f = classUnderTest.getField(Constants.BRANCHES_FIELD_NAME);
					if (f != null) {
						EnumSet<?> s = (EnumSet<?>) f.get(objectUnderTest);
//						System.out.println("Called "+classUnderTest.getName()+"#"+methodUnderTest.getName());
//						System.out.println("Parameters: ");
						for (int i = 0; i < paramValues.length; i++) {
//							System.out.println("\t"+(i+1)+" = "+paramValues[i]);
						}
//						System.out.println("-----------\nVisited Branches:");
						List<String> visitedBranches = new ArrayList<String>(s.size());
						for (Object obj : s) {
//							System.out.println("\t"+obj);
							visitedBranches.add(obj.toString());
							JGapTest.this.visitedBranches.add(obj.toString());
						}
						chromosome.setApplicationData(visitedBranches);
//						System.out.println("##### \n");
					}
					chromosome.setFitnessValue(1);
				} catch (InstantiationException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IllegalArgumentException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (InvocationTargetException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (NoSuchFieldException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (SecurityException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		
		public Object clone() {
			return new BranchCoverageFitnessFunction();
		}
	}
	
}
