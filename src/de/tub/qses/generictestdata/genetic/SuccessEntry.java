package de.tub.qses.generictestdata.genetic;

import java.util.List;

public class SuccessEntry {

	private String branchName;
	private List<Object> parameters;
	
	public SuccessEntry(String branchName, List<Object> parameters) {
		this.branchName = branchName;
		this.parameters = parameters;
	}

	public String getBranchName() {
		return branchName;
	}

	public List<Object> getParameters() {
		return parameters;
	}
}
