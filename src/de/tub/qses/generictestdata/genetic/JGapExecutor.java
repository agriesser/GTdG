package de.tub.qses.generictestdata.genetic;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import de.tub.qses.generictestdata.annotations.CoverMethod;

public class JGapExecutor {
	
	public final static Class<?>[] ALLOWED_PARAM_TYPES = new Class[] {
		Double.class, Integer.class, Character.class, Short.class, Float.class, int.class, double.class, short.class, char.class, float.class
	};

	/**
	 * This is the base method to evaluate a class with JGap.
	 * The class already has to be instrumentated for this to run!
	 * 
	 * @param clazz
	 */
	public static void generateJGapEvolution(Class<?> clazz) {
		if (!checkValidClass(clazz)) {
			return;
		}
		List<Method> methodsToTest = getMethodsToTest(clazz);
		for (Method method : methodsToTest) {
			handleMethod(clazz, method);
		}
	}
	
	private static void handleMethod(Class<?> clazz, Method method) {
		Class<?>[] params = method.getParameterTypes();
		boolean containsNotAllowedParameter = false;
		for (int i = 0; i < params.length; i++) {
			if (!isAllowedParameterType(params[i])) {
				containsNotAllowedParameter = true;
				System.out.println("The "+(i+1)+". parameter of type "+params[i].getName()+" is not supported! Method will be skipped.");
			}
		}
		if (containsNotAllowedParameter) {
			return;
		}
		
		setupJGap(clazz, method, params);
	}
	
	private static void setupJGap(final Class<?> clazz, final Method method, final Class<?>[] params) {
		// since this is a long running task I'll guess we better create a job for it, hm?
		Job job = new Job("JGapExecutor.executing: "+clazz.getName()+"#"+method.getName()) {

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				monitor.beginTask("Evolutionary test data search", 100);
				
				monitor.subTask("Setting up JGap");
				JGapTest test = new JGapTest(clazz, method);
				monitor.worked(10);
				
				int increment = (int) Math.floor(90/test.getEvoluationCount());
				for (int i = 0; i < test.getEvoluationCount(); i++) {
					monitor.subTask("Evolutionary step "+(i+1));
					test.evolve();
					monitor.worked(increment);
					if (test.allBranchesVisited()) {
						System.out.println("HÄ!");
						break;
					}
				}
				System.out.println("Amount of generations: "+test.getCurrentGeneration());
				System.out.println("Population: "+test.getPopulationCount());
				System.out.println("Achieved full branch coverage? "+test.allBranchesVisited());
				System.out.println("The following input data can be used: ");
				for (SuccessEntry branch : test.getSuccessEntries()) {
					System.out.println("Branchname: "+branch.getBranchName());
					System.out.println("Parameter: ");
					for (int i = 0; i < branch.getParameters().size(); i++) {
						System.out.println("\t"+(i+1)+": "+branch.getParameters().get(i));
					}
					System.out.println("\n");
				}
				monitor.done();
				return Status.OK_STATUS;
			}
			
		};
		job.schedule();
	}
	
	private static List<Method> getMethodsToTest(Class<?> clazz) {
		List<Method> methods = new ArrayList<Method>();
		Method[] declaredMethods = clazz.getDeclaredMethods();
		for (Method method : declaredMethods) {
			for (Annotation anno : method.getAnnotations()) {
				if (anno.annotationType().getName().equals(CoverMethod.class.getName())) {
					methods.add(method);
				}
			}
		}
		return methods;
	}

	private static boolean checkValidClass(Class<?> clazz) {
		Field[] fields = clazz.getDeclaredFields();
		boolean foundField = false;
		for (Field field : fields) {
			if (field.getName().equals(Constants.BRANCHES_FIELD_NAME)) {
				foundField = true;
				break;
			}
		}
		if (!foundField) {
			return false;
		}
		return true;
	}
	
	private static boolean isAllowedParameterType(Class<?> paramType) {
		boolean found =false;
		for (int i = 0; i < ALLOWED_PARAM_TYPES.length; i++) {
			if (ALLOWED_PARAM_TYPES[i].equals(paramType)) {
				found = true;
				break;
			}
		}
		return found;
	}
}
