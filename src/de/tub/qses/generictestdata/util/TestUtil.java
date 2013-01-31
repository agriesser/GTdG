package de.tub.qses.generictestdata.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TestUtil {
	
	private static Set<String> lookedNodes = new HashSet<String>();
	private static Map<String, Node> nodes = new HashMap<String, Node>();
	private static List<VisitRecord> visitRecords = new LinkedList<VisitRecord>();
	private static List<LookRecord> lookRecords = new LinkedList<LookRecord>();
	private static boolean roundFoundBranch = false;
	
	public static void addWatchedNode(String identifier) {
		lookedNodes.add(identifier);
	}
	
	public static Set<String> getWatchedNodes() {
		return lookedNodes;
	}
	
	public static Node createNode(String identifier) {
		Node n = new Node(identifier);
		nodes.put(identifier, n);
		return n;
	}
	
	public static void visitNode(String identifier) {
		Node n = findNode(identifier);
		if (n == null) {
			n = createNode(identifier);
		}
		visitRecords.add(new VisitRecord(n));
	}
	
	public static void lookNode(String identifier, Number a, Number b) {
		Node n = findNode(identifier);
		if (n == null) {
			n = createNode(identifier);
		}
		lookRecords.add(new LookRecord(n, a, b));
	}
	
	public static Collection<Node> getAllBranches() {
		return nodes.values();
	}
	
	public static List<VisitRecord> getVisitedBranches() {
		return visitRecords;
	}
	
	public static List<LookRecord> getLookingBranches() {
		return lookRecords;
	}
	
	public static boolean generationProducedNewBranch() {
		return roundFoundBranch; 
	}
	
	public static void clear() {
		nodes.clear();
		visitRecords.clear();
		lookRecords.clear();
		roundFoundBranch = false;
		lookedNodes.clear();
	}
	
	private static Node findNode(String identifier) {
		Node ret = null;
		boolean found = false;
		Iterator<Node> nodeIt = nodes.values().iterator();
		while (!found && nodeIt.hasNext()) {
			Node n = nodeIt.next();
			if (n.isNode(identifier)) {
				ret = n;
			} else if (n.isOnPath(identifier)) {
				nodeIt = n.getChildren().iterator();
			}
		}
		return ret;
	}
	
	public static class Node {
		private String identifier;
		private String[] path;
		private List<Node> children = new LinkedList<TestUtil.Node>();
		private List<Node> siblings = new LinkedList<TestUtil.Node>();
		
		public Node(String identifier) {
			this.identifier = identifier;
			this.path = identifier.split("\\.");
		}
		
		public boolean isNode(String identifier) {
			return this.identifier.equals(identifier);
		}
		
		public boolean isOnPath(String oIdentifier) {
			String[] oPath = identifier.split("\\.");
			if (oPath.length <= path.length) {
				for (int i = 0; i < oPath.length; i++) {
					if (!oPath[i].equals(path[i])) {
						return false;
					}
				}
				return true;
			}
			return false;
		}
		
		public List<Node> getSiblings() {
			return siblings;
		}
		
		public List<Node> getChildren() {
			return children;
		}
		
		public String getIdentifier() {
			return identifier;
		}
	}

	public static class VisitRecord {
		private Node node;
		public Node getNode() {
			return node;
		}

		public List<Number> getParameters() {
			return parameters;
		}

		private List<Number> parameters = new LinkedList<Number>();
		
		public VisitRecord(Node n) {
			this.node = n;
		}
	}
	
	public static class LookRecord {
		private Node node;
		private Number a;
		private Number b;
		
		public LookRecord(Node n, Number a, Number b) {
			this.node = n;
			this.a = a;
			this.b = b;
		}

		public Node getNode() {
			return node;
		}

		public Number getA() {
			return a;
		}

		public Number getB() {
			return b;
		}
	}

}
