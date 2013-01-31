package de.tub.qses.generictestdata.genetic;

public class Fitness {

	public static double evaluateFitness(Number a, Number b) {
		return (double) (1 / Math.pow(Math.abs(a.doubleValue() - b.doubleValue())+0.01d, 2.0d));
	}
}
