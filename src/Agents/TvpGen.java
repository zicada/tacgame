package Agents;

import java.util.ArrayList;

public class TvpGen {
	public int[][] G;
	public int[][] preferences;

	public ArrayList<TVP> p = new ArrayList<TVP>();
	public ArrayList<TVP> tvp;
	public int maxUtil;
	public String allocatedTickets;
	
	public TvpGen(int[][]G, int[][]preferences) {
		this.G = G;
		this.preferences = preferences;
	}

	public ArrayList<TVP> makePackageList(int[][] g2, TVP pref) {
		prune(g2);
		ArrayList<TVP> list = new ArrayList<TVP>();
		for (TVP n : tvp) {
			boolean add = false;
			int s = list.size();
			if (s == 0) {
				list.add(n);
				add = true;
			} else {
				for (int i = 0; i < s; i++) {
					TVP tmp = list.get(i);
					if (tmp.utility(pref) > n.utility(pref))
						continue;
					list.add(i, n);
					add = true;
					break;
				}
			}
			if (!add) {
				list.add(s, n);
			}
		}
		return list;
	}

	public void prune(int[][] g2) {
		tvp = new ArrayList<TVP>();
		ArrayList<TVP> tv1 = new ArrayList<TVP>();
		for (int i = 0; i < 4; i++) {
			for (int j = i + 1; j < 5; j++) {
				TVP n = new TVP(i, j, 2);
				tv1.add(n);
				n = new TVP(i, j, 3);
				tv1.add(n);
			}
		}
		for (TVP n : tv1) {
			if (g2[0][n.in] == 0)
				continue;
			if (g2[1][n.out] == 0)
				continue;
			if (!hotAvailable(n.in, n.out, g2, n.hot))
				continue;

			tvp.add(n);
		}
	}

	boolean hotAvailable(int in, int out, int[][] g, int hot) {
		for (int i = in; i < out; i++) {
			if (g[hot][i] <= 0)
				return false;
		}
		return true;
	}

	boolean hotAvailable(int in, int out, int[][] g) {
		if (hotAvailable(in, out, g, 2))
			return true;
		if (hotAvailable(in, out, g, 3))
			return true;

		return false;
	}

	public TVP allocation(ArrayList<TVP> list, TVP tvp2, int g[][]) {
		if (list.size() == 0)
			return null;
		TVP n = list.get(0);
		g[0][n.in]--;
		g[1][n.out]--;
		for (int i = n.in; i < n.out; i++)
			g[n.hot][i]--;
		return n;
	}

	public ArrayList<TVP> makePackage() {
		ArrayList<TVP> pk = new ArrayList<TVP>();
		for (int i = 0; i < 8; i++)
			pk.add(null);
		int[][] g = G;
		int[] cl = { 0, 1, 2, 3, 4, 5, 6, 7 };
		for (int i = 0; i < cl.length; i++) {
			ArrayList<TVP> list = makePackageList(g, p.get(cl[i]));
			TVP n = allocation(list, p.get(cl[i]), g);
			if (n != null)
				pk.set(cl[i], n);
		}

		return pk;
	}

	public ArrayList<TVP> makePackages() {
		ArrayList<TVP> p = getPreferences();
		ArrayList<TVP> pk = new ArrayList<TVP>();
		int maxUtility = 0;
		ArrayList<TVP> pk1 = makePackage();
		int u = 0;
		for (int i = 0; i < pk1.size(); i++) {
			TVP tmp = pk1.get(i);
			if (tmp != null)
				u += tmp.utility(p.get(i));
			if (maxUtility < u) {
				maxUtility = u;
				pk = pk1;
			}
		}
		maxUtil = u; 
		epks(pk, G);
		return pk;
	}
	
	public int getMaxUtil() {
		return maxUtil;
	}

	public ArrayList<TVP> getPreferences() {
		for (int i = 0; i < 8; i++) {
			TVP setPref = new TVP();
			setPref.in = preferences[i][0];
			setPref.out = preferences[i][1];
			setPref.hot = preferences[i][2];
			setPref.events[0] = preferences[i][3];
			setPref.events[1] = preferences[i][4];
			setPref.events[2] = preferences[i][5];
			p.add(setPref);
		}
		return p;
	}
	
	public ArrayList<Epkgs> epks(ArrayList<TVP> travelPackages, int [][]g) {
		ArrayList<Epkgs> entertainmentPk = new ArrayList<Epkgs>();	
		for (int i = 0; i < p.size(); i++) entertainmentPk.add(new Epkgs(i));
		for (int day = 0; day < 4; day++) {
			for (int e = 0; e < 3; e++) {
				if (g[4+e][day] > 0) {
					for (int i = 0; i < p.size(); i++ ) {
						TVP clpk = null;
						if (!travelPackages.isEmpty())
							clpk = travelPackages.get(i);
						if (clpk == null) continue;
						TVP pref = p.get(i);
						Epkgs epack = entertainmentPk.get(i);
						if (clpk.in <= day && day < clpk.out) {
							EpNode node = new EpNode(day, 4+e, pref.events[e]);
							epack.addTicket(node, g);
						}
					}
				}
			}
		}

		allocationAlg(travelPackages, g, entertainmentPk);
		return entertainmentPk;
	}
	
	boolean destroy(EpNode a, EpNode b) {
		if(a.day != b.day && a.type != b.type) {
			return true;
		}
		return false;
	}
	                                                      
	public ArrayList<Epkgs> allocationAlg(ArrayList<TVP> travelPackages, int[][] g,
		ArrayList<Epkgs> entertainmentPk) {
		ArrayList<Epkgs> finishedTickets = new ArrayList<Epkgs>();
		EpNode initial = null;
		EpNode a = null;
		ArrayList<EpNode> ticketsPerClient = new ArrayList<EpNode>();

		for (int i = 0; i < p.size(); i++) {

			Epkgs current = entertainmentPk.get(i);
			ArrayList<EpNode> node = current.tickets;
			if (node.size() > 0) {
				initial = node.get(0);
				if (g[initial.type][initial.day] > 0) {
					ticketsPerClient.add(initial);
					g[initial.type][initial.day]--;
					node.remove(initial);
				} else {
					node.remove(initial);
					initial = node.get(0);
					if (g[initial.type][initial.day] > 0) {
						ticketsPerClient.add(initial);
						g[initial.type][initial.day]--;
						node.remove(initial);
					}
				}
				while (node.size() > 0) {
					a = node.get(0);
					if (destroy(initial, a) == true) {
						if (g[a.type][a.day] > 0) {
							ticketsPerClient.add(a);
							g[a.type][a.day]--;
							System.out.println(a + " added.");
						}
					}
					node.remove(a);
				}

			}

		}
		allocatedTickets = ticketsPerClient.toString();
		return finishedTickets;
	}
	
	public String getAllocatedTickets() {
		return allocatedTickets;
	}

}
