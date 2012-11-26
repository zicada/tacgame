package Agents;

public class EpNode {

	   public int day; // 0, 1, 2, 3
	   public int type; // 4, 5, 6
	   public int value;
	
	public EpNode(int g, int i, int events) {
		day = g;
		type =  i;
		value = events;
	}
	
	public String toString() {
		String str = "(" + day + " " + type + " " + value + ")";
		return str;
	}
	
	public EpNode getHighest(EpNode node) {
		
		EpNode highest = node;
		return highest;		
	}

}
