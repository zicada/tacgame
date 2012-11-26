package Agents;

public class TVP {
	public int in;
	public int out;
	public int hot;
	public int[] events = new int[3];
	public int aw;
	public int ap;
	public int mu;

	public TVP() {

	}
	
	public TVP(int i, int j, int k) {
		in = i;
		out = j;
		hot = k;
		
	}


	public String toString() {
		String str = "(" + in + " " + out + " " + hot + ")";
		return str;
	}

	public int utility(TVP p) {
		
		int utility = 1000 - 100*(Math.abs(p.in - in) + Math.abs(p.out - out));
		if (hot == 2) utility += p.hot;
		return utility;
	}
}
