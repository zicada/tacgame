package Agents;

import java.util.ArrayList;

public class Epkgs {

	public int client;
	public ArrayList<EpNode> tickets = new ArrayList<EpNode>();
	public int utility = 0;

	public Epkgs(int c) {
		client = c;
	}

	public String toString() {
		String str = client + ": ";
		for (EpNode t : tickets)
			str += t.toString() + " ";
		return str;
	}

	public int utility(TVP p) {
		int u = 0;
		for (EpNode t : tickets) {
			u += p.events[t.type - 4];
		}
		return u;
	}

	public void addTicket(EpNode node, int[][] g) {

		if (node.value <= 0)
			return;
		int s = tickets.size();
		boolean add = false;

		if (s == 0) {
			tickets.add(node);
			add = true;
		}

		else {
			for (int i = 0; i < s; i++) {
				EpNode c = tickets.get(i);
				if (c.value > node.value)
					continue;
				else if (c.value == node.value
						&& g[c.type][c.day] > g[node.type][node.day])
					continue;
				tickets.add(i, node);
				add = true;
				break;
			}
			if (!add)
				tickets.add(s, node);
		}
	}
}
