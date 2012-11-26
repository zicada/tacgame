package Agents;

import yinyang.*;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.rmi.*;

import Gr6_Manager.Gr6_Manager;

public class A1 {
	private Gr6_Manager man;
	private SimpleDateFormat tf = new SimpleDateFormat("HH:mm:ss");
	private long gameLength = 9 * 60 * 1000; // the length of a game
	private long askPriceInterval; // the length of updating price
	private int[][] preferences = new int[8][6];
	private int[][] G = new int[7][5];
	private int[][] demands = new int[7][5];
	private int[][] endowments = new int[3][4];
	private ArrayList<TVP> p = new ArrayList<TVP>();
	private ArrayList<String> transactions = new ArrayList<String>();
	private long interval;
	private int MAX_HOTEL_PRICE;
	private int MAX_EVENTS_PRICE;
	private int HOTEL_BID_INCREMENT;
	private static Gr6_Manager cs;
	float totalCost = 0;
	String agentName = "";
	private Boolean hasClosed = false;
	

	public A1(Gr6_Manager cs, String name) {
		agentName = name;
		man = cs;
		interval = 0;
		setMAX_HOTEL_PRICE(80);
		setMAX_EVENTS_PRICE(120);
		setHOTEL_BID_INCREMENT(2);
		setAskPriceInterval(10 * 1000);
	}

	// run forever
	public void run() throws IOException, RemoteException, NotBoundException {
		for (;;) {
			try {
				boolean isOn = getStatus();
				if (!isOn || interval >= gameLength)
					close();
				else if (isOn && interval == 0)
					start();
				else if (isOn && interval < gameLength && interval > 0) {
					interval += getAskPriceInterval();
					askFlightPrice();
					askHotelPrice();
				}
				Thread.sleep(getAskPriceInterval());
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	// ask for flight prices
	public void askFlightPrice() {
		Random random = new Random();
		int type = random.nextInt(2);
		int d = random.nextInt(4);
		Message msg = new Message();
		msg.setPerformative(TACOntology.ask_flight_prices);
		msg.setSender(agentName);
		String auction = "";
		if (type == 0)
			auction = "inflight_" + d;
		else
			auction = "outflight_" + (d + 1);
		msg.setReceiver(auction);
		msg.setContent(auction);
		try {
			Message res = man.send(msg);
			// Bid on this offer if we need tickets for it.
			if (demands[type][d] > 0)
				bidFlight(type, d, demands[type][d], res.getContent());
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		
	}
	
	public void bidFlight(int type, int day, int amt, String price) {
		Message msg = new Message();
		msg.setPerformative(TACOntology.bidFlight);
		msg.setSender(agentName);
		msg.setReceiver("Manager");
		
		String str = type + " " + day + " " + price + " " + amt;
		msg.setContent(str);

		String reply = null;
		try {
			Message res = man.send(msg);
			reply = res.getContent();
		} catch (RemoteException e) {
			e.printStackTrace();
		}
				
		if (reply.equals("accepted")) {
			float priceFloat = Float.valueOf(price);
			updateGoods(type, day, amt, priceFloat);
		}
	}
	
	public void askHotelPrice() {
		Random random = new Random();
		int type = random.nextInt(2) + 2;
		int d = random.nextInt(4);
		Message msg = new Message();
		msg.setPerformative(TACOntology.ask_hotel_prices);
		msg.setSender(agentName);
		msg.setReceiver("Manager");
		msg.setContent(type + " " + d + " " + agentName);
		try {
			Message res = man.send(msg);
			// Place a bid on this hotel if we need it.
			if (demands[type][d] > 0 && askHotelStatus(type, d, agentName))
				bidHotel(type, d, demands[type][d], res.getContent());
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}
	
	public void bidHotel(int type, int day, int quantity, String price) {
		Message msg = new Message();
		msg.setPerformative(TACOntology.bidHotel);
		msg.setSender(agentName);
		msg.setReceiver("Manager");
		
		Float floatPrice = Float.valueOf(price);
		// Bid must be larger than ask_price.
		floatPrice += getHOTEL_BID_INCREMENT();
		
		String str = type + " " + day + " " + floatPrice + " " + quantity;
		msg.setContent(str);
		
		String reply = null;
		try {
			Message res = man.send(msg);
			reply = res.getContent();
		} catch (RemoteException e) {
			e.printStackTrace();
		}
				
		if (reply.equals("accepted")) {
			 
		}
	}
	
	public void getHotelResult(int type, int d, String agent) {
		Message msg = new Message();
		msg.setPerformative(TACOntology.ask_hotel_winner);
		msg.setSender(agentName);
		msg.setReceiver("Manager");
		msg.setContent(type + " " + d + " " + agentName);
		
		String response = "";
		try {
			Message res = man.send(msg);
			response = res.getContent();
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		if (!response.equals("")) {
			String query[] = response.split(" ");
			float price = Float.valueOf(query[0]);
			int amt = Integer.valueOf(query[1]);
			if (G[type][d] < amt) {
				updateGoods(type, d, amt, price);
			}
		}
	}
	
	public Boolean askHotelStatus(int type, int day, String agent) {
		Message msg = new Message();
		msg.setPerformative(TACOntology.ask_hotel_status);
		msg.setSender(agentName);
		msg.setReceiver("Manager");
		msg.setContent(type + " " + day + " " + agentName);
		String response = "";
		try {
			Message res = man.send(msg);
			response = res.getContent();
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		if (response.equals("open"))
			return true;
		else
		    return false;
	}
	
	public void updateGoods(int type, int day, int amt, float price) {
		// Add our newly purchased tickets to G, and remove them from demands. Then update totalCost so we know how much money we spent.
		G[type][day] += amt;
		demands[type][day] -= amt;
		totalCost += price*amt;
		String strType = "";
		if (type == 0) 
			strType = " inflight";
		else if (type == 1)
			strType = " outflight";
		else if (type == 2)
			strType = " TT";
		else if (type == 3)
			strType = " SS";
		transactions.add(tf.format(new Date()) + strType + " " + day + " " + agentName + " Auction " + amt + " " + price + "\n");
		System.out.println(transactions.get(transactions.size()-1));
	}

	// ask for status, on or off
	public boolean getStatus() {
		boolean isOn = false;
		Message msg = new Message();
		msg.setPerformative(TACOntology.ask_status);
		msg.setSender(agentName);
		msg.setReceiver("Manager");
		try {
			Message res = man.send(msg);
			if (res.getContent().equals(TACOntology.on))
				isOn = true;
			else
				isOn = false;
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		return isOn;
	}

	// ask for time interval when the agent starts the game
	public int getTimeInterval() {
		int time = 0;
		Message msg = new Message();
		msg.setPerformative(TACOntology.ask_time);
		msg.setSender(agentName);
		msg.setReceiver("Manager");
		try {
			Message res = man.send(msg);
			time = Integer.parseInt(res.getContent());
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		return time;
	}

	// get preferences.
	public String getPreferences() {
		String str = "";
		Message msg = new Message();
		msg.setPerformative(TACOntology.ask_preferences);
		msg.setSender(agentName);
		msg.setReceiver("Manager");
		try {
			Message res = man.send(msg);
			str = res.getContent();
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		// Convert string res to preferences multidim array
		String[] arr = str.split("\n");
		for (int i = 0; i < arr.length ; i++) {
			String temp[] = arr[i].split(" ");
			for(int j = 0; j < temp.length; j++) {
				preferences[i][j] = Integer.valueOf(temp[j]);
			}
		}
		for (int i = 0; i < 8; i++) {
			TVP pref = new TVP();
			pref.in = preferences[i][0];
			pref.out = preferences[i][1];
			pref.hot = preferences[i][2];
			pref.events[0] = preferences[i][3];
			pref.events[1] = preferences[i][4];
			pref.events[2] = preferences[i][5];
			p.add(pref);
			// Populate demands, add TT if client is willing to pay more than MAX_HOTEL_PRICE, if not, give them SS.
			// Set up demands for endowments based on the price clients are willing to pay.
			for (int j = pref.in; j < pref.out; j++) {
				if (pref.hot > getMAX_HOTEL_PRICE()) demands[2][j]++;
				else demands[3][j]++;
				
				if (pref.events[0] > getMAX_EVENTS_PRICE()) 
					demands[4][j]++;
			    else if (pref.events[1] > getMAX_EVENTS_PRICE())
			    	demands[5][j]++;
			    else if (pref.events[2] > getMAX_EVENTS_PRICE())
			    	demands[6][j]++;
			}
			demands[0][pref.in]++;
			demands[1][pref.out]++;
		}

		return str;
	}
	
	public String getEndowments() {
		String str = "";
		Message msg = new Message();
		msg.setPerformative(TACOntology.ask_endowments);
		msg.setSender(agentName);
		msg.setReceiver("Manager");
		try {
			Message res = man.send(msg);
			str = res.getContent();
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		
		String[] arr = str.split("\n");
		for (int i = 0; i < arr.length ; i++) {
			String temp[] = arr[i].split(" ");
			for(int j = 0; j < temp.length; j++) {
				endowments[i][j] = Integer.valueOf(temp[j]);
			}
		}
		// put endowments in G
		for (int i = 4; i < 7; i++) {
			for (int j = 0; j < 4; j++) {
				if (demands[i][j] > 0 && endowments[i-4][j] > 0) {
					G[i][j] = endowments[i-4][j];
				}
				 
			}
		}
		return str;
	}
	
	public String printDemands() {
		String str = "";
		for (int i = 0; i < 7; i++) {
			for (int j = 0; j < 5; j++) {
				str += (demands[i][j] + " ");
			}
			str += ("\n");
		}
		return str;
	}
	
	public String printG() {
		String str = "";
		for (int i = 0; i < 7; i++) {
			for (int j = 0; j < 5; j++) {
				str += (G[i][j] + " ");
			}
			str += ("\n");
		}
		return str;
	}

	// start the game, get preferences and endowments.
	public void start() {
		hasClosed = false;
		totalCost = 0;
		
		// Clean up demands and G for the next round.
		for (int i = 0; i < 5; i++) {
			for (int j = 0; j < 7; j++) {
				demands[j][i] = 0;
				G[j][i] = 0;
			}
		}
		try {
			boolean isOn = getStatus();
			if (isOn) {
				interval = getTimeInterval();
				transactions.add("------------" + agentName + "------------\n");
				transactions.add("\n----------- Preferences ----------\n");
				transactions.add(getPreferences());
				transactions.add("----------- Endowments ----------\n");
				transactions.add(getEndowments());
				transactions.add("----------- Demands ----------\n");
				transactions.add(printDemands());
				transactions.add("----------- Transactions ----------\n");
				for (int i = 0; i < transactions.size(); i++) {
					System.out.println(transactions.get(i));
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void runOnceAtClose() throws IOException {
		if (!hasClosed) {
			for (int i = 2; i < 4; i++) {
				for (int j = 0; j < 4; j++) {
					getHotelResult(i, j, agentName);
				}
				
			}
			try {
				Thread.sleep(20000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			transactions.add("----------- G ----------\n");
			transactions.add(printG());
			try {
				TvpGen tvp = new TvpGen(G, preferences);
				tvp.makePackages();
				transactions.add("\n Tickets allocated: " + tvp.getAllocatedTickets() + "\n");
				transactions.add(tvp.getMaxUtil() + " " +  totalCost + " " + (tvp.getMaxUtil() - totalCost) + "\n");
			} catch (Exception e) {
				transactions.add("Unable to get utility and tickets for this agent. \n");
				e.printStackTrace();
			}
			// Write finished report to file. We do this on each agent instead of sending all the data to the TACMAN to save time.
			FileWriter writer = new FileWriter(agentName + ".dat", false);
			for(String str: transactions) {
				writer.write(str);
			}
			writer.close();
		}
		hasClosed = true;
	}
	
	public void close() {
		try {
			runOnceAtClose();
		} catch (IOException e) {
			e.printStackTrace();
		}

		interval = 0;

	}

	public long getAskPriceInterval() {
		return askPriceInterval;
	}

	public void setAskPriceInterval(long askPriceInterval) {
		this.askPriceInterval = askPriceInterval;
	}

	public int getMAX_HOTEL_PRICE() {
		return MAX_HOTEL_PRICE;
	}

	public void setMAX_HOTEL_PRICE(int mAX_HOTEL_PRICE) {
		MAX_HOTEL_PRICE = mAX_HOTEL_PRICE;
	}

	public int getMAX_EVENTS_PRICE() {
		return MAX_EVENTS_PRICE;
	}

	public void setMAX_EVENTS_PRICE(int mAX_EVENTS_PRICE) {
		MAX_EVENTS_PRICE = mAX_EVENTS_PRICE;
	}

	public int getHOTEL_BID_INCREMENT() {
		return HOTEL_BID_INCREMENT;
	}

	public void setHOTEL_BID_INCREMENT(int hOTEL_BID_INCREMENT) {
		HOTEL_BID_INCREMENT = hOTEL_BID_INCREMENT;
	}


	
	public static void getHost() {
		try {
			ServiceFinder sf = new ServiceFinder(Gr6_Manager.class);
			cs = (Gr6_Manager) sf.getObject();
		} catch (Exception ex) {
		}
	}

	public static void main(String[] args) throws Exception {
		System.setSecurityManager(new RMISecurityManager());

		while (true) {
			try {
				A1 a1 = new A1(cs, "A1");
				a1.run();

			} catch (Exception ex) {
				getHost();
			}
		}


	}

}