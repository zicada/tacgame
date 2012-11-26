package Gr6_Manager;

import yinyang.*;
import java.util.*;
import java.text.*;
import java.rmi.*;
import java.rmi.server.*;

import Agents.TACOntology;
import net.jini.core.lookup.*;
import net.jini.discovery.*;
import net.jini.lookup.*;

@SuppressWarnings("unused")
public class Gr6_ManagerImpl extends UnicastRemoteObject implements Gr6_ManagerProxy,
		ServiceIDListener {
	private static final long serialVersionUID = 1L;

	private String gameId;
	private SimpleDateFormat tf = new SimpleDateFormat("HH:mm:ss");
	private SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	private long gameLength = 9 * 60 * 1000; // the length of a game
	private long gameInterval = 12 * 60 * 1000; // the length between two games
	private long hotCloseInterval = 60 * 1000; // the length of updating price
	private long interval = 0;
	private Date startTime, endTime;
	private boolean isOn = false;
	public ArrayList<String> transactionList = new ArrayList<String>();
	public ArrayList<HotelAuction> hotAuct;
	public String[] hotWinners = new String[32];

	private double[] inFlight = new double[4];
	private double[] outFlight = new double[4];

	public Gr6_ManagerImpl() throws RemoteException {

	}

	public void serviceIDNotify(ServiceID id) {
	}

	public Message send(Message kqml) throws RemoteException {
		if (kqml.getPerformative().equals(TACOntology.ask_status))
			return status(kqml);
		else if (kqml.getPerformative().equals(TACOntology.ask_time))
			return time(kqml);
		else if (kqml.getPerformative().equals(TACOntology.ask_preferences))
			return preferences(kqml);
		else if (kqml.getPerformative().equals(TACOntology.ask_endowments))
			return endowments(kqml);
		else if (kqml.getPerformative().equals(TACOntology.flight_prices))
			return flightPrices(kqml);
		else if (kqml.getPerformative().equals(TACOntology.ask_flight_prices))
			return quoteFlightPrices(kqml);
		else if (kqml.getPerformative().equals(TACOntology.bidFlight))
			return handleFlightBid(kqml);
		else if (kqml.getPerformative().equals(TACOntology.ask_hotel_prices))
			return hotelPrice(kqml);
		else if (kqml.getPerformative().equals(TACOntology.bidHotel))
			return handleHotelBid(kqml);
		else if (kqml.getPerformative().equals(TACOntology.ask_hotel_winner))
			return hotelWinner(kqml);
		else if (kqml.getPerformative().equals(TACOntology.ask_hotel_status))
			return hotelStatus(kqml);

		return null;
	}

	private Message status(Message kqml) {
		Message response = new Message();
		response.setPerformative("status");
		response.setSender("manager");
		response.setReceiver(kqml.getSender());
		if (isOn)
			response.setContent(TACOntology.on);
		else
			response.setContent(TACOntology.off);
		return response;
	}

	private Message time(Message kqml) {
		Message response = new Message();
		response.setPerformative("time");
		response.setSender("manager");
		response.setReceiver(kqml.getSender());
		response.setContent("" + interval);
		return response;
	}

	private Message preferences(Message kqml) {
		Message response = new Message();
		response.setPerformative("preferences");
		response.setSender("manager");
		response.setReceiver(kqml.getSender());
		response.setContent(this.getPreferences());
		return response;
	}

	private Message endowments(Message kqml) {
		Message response = new Message();
		response.setPerformative("endowments");
		response.setSender("manager");
		response.setReceiver(kqml.getSender());
		response.setContent(this.getEndowments());
		return response;
	}

	private Message flightPrices(Message kqml) {
		Message response = new Message();
		response.setPerformative("flight_prices");
		response.setSender("manager");
		response.setReceiver(kqml.getSender());
		response.setContent("ok");
		String str = kqml.getContent();
		String[] pr = str.split(" ");
		for (int i = 0; i < 4; i++)
			inFlight[i] = Double.parseDouble(pr[i]);
		for (int i = 4; i < 8; i++)
			outFlight[i - 4] = Double.parseDouble(pr[i]);
		return response;
	}

	private Message quoteFlightPrices(Message kqml) {
		Message response = new Message();
		response.setPerformative("flight_prices");
		response.setSender(kqml.getReceiver());
		response.setReceiver(kqml.getSender());
		String auction = kqml.getReceiver();
		if (auction.equals(TACOntology.inflight_0))
			response.setContent("" + inFlight[0]);
		else if (auction.equals(TACOntology.inflight_1))
			response.setContent("" + inFlight[1]);
		else if (auction.equals(TACOntology.inflight_2))
			response.setContent("" + inFlight[2]);
		else if (auction.equals(TACOntology.inflight_3))
			response.setContent("" + inFlight[3]);
		else if (auction.equals(TACOntology.outflight_1))
			response.setContent("" + outFlight[0]);
		else if (auction.equals(TACOntology.outflight_2))
			response.setContent("" + outFlight[1]);
		else if (auction.equals(TACOntology.outflight_3))
			response.setContent("" + outFlight[2]);
		else if (auction.equals(TACOntology.outflight_4))
			response.setContent("" + outFlight[3]);
		return response;
	}
	
	private Message handleFlightBid(Message kqml) {
		Message response = new Message();
		response.setPerformative(TACOntology.bidFlight);
		response.setSender("manager");
		response.setReceiver(kqml.getSender());
		String bidString = kqml.getContent();
		String bid[] = bidString.split(" ");
		int type = Integer.valueOf(bid[0]);
		int day = Integer.valueOf(bid[1]);
		float price = Float.valueOf(bid[2]);
		int q = Integer.valueOf(bid[3]);
		String typeName = "";
		
		if (type == 0) {
			if (price >= inFlight[day]) {
				typeName = "inflight";
				response.setContent("accepted");
				transactions(typeName, day, kqml.getSender(), q, price, tf.format(new Date()));
			} else
			response.setContent("rejected");
		} else {
			if (price >= outFlight[day]) {
				typeName = "outflight";
				response.setContent("accepted");
				transactions(typeName, day, kqml.getSender(), q, price, tf.format(new Date()));
			} else
			response.setContent("rejected");
		}

		return response;
	}
	
	private Message handleHotelBid(Message kqml) {
		Message response = new Message();
		response.setPerformative(TACOntology.bidHotel);
		response.setSender("manager");
		response.setReceiver(kqml.getSender());
		String bidString = kqml.getContent();
		String bid[] = bidString.split(" ");
		int type = Integer.valueOf(bid[0]);
		int day = Integer.valueOf(bid[1]);
		float price = Float.valueOf(bid[2]);
		int q = Integer.valueOf(bid[3]);
		String typeName = "";
		
		HotelAuction hotel = null;
		// get the right auction based on type and day
		for (int i = 0; i < hotAuct.size(); i++) {
			if (hotAuct.get(i).day == day && hotAuct.get(i).type == type) {
				hotel = hotAuct.get(i);
			}
		}
		
		if (price > hotel.ask_price() && hotel.opened) {
			response.setContent("accepted");
			updateHotel(day, type, kqml.getSender(), q, price, tf.format(new Date()));
		} else {
			response.setContent("rejected");
		}
	
		
		return response;
	}
	
	private void updateHotel(int day, int type, String agent, int q, float price, String date) {
		
		HotelAuction hotel = null;
		for (int i = 0; i < hotAuct.size(); i++) {
			if (hotAuct.get(i).day == day && hotAuct.get(i).type == type) {
				hotel = hotAuct.get(i);
			}
		}
		HotelBid bid = new HotelBid(type, day, price, agent, q, date);
		hotel.bids.add(bid);
		
	}
	
	public Boolean allHotelsClosed() {
		Boolean closed = true;
		for (int i = 0; i < hotAuct.size(); i++) {
			if (hotAuct.get(i).opened == true)
				closed = false;
		}
		return closed;
	}

	public void closeHotelAuction() {
		// Make sure we close every auction
		Random random = new Random();
		HotelAuction hotel = hotAuct.get(random.nextInt(8));
		if(hotel.opened == true) {
			hotel.opened = false;
		} else if (!allHotelsClosed()) {
			closeHotelAuction();
			return;
		} else if (allHotelsClosed()) {
			return;
		}
		String typeName = "";
		if (hotel.type == 2) {
			typeName = "TT";
		} else if (hotel.type == 3) {
			typeName = "SS";
		}
		System.out.println(typeName + "_" + hotel.day + " closed" );

		// get the winning bids.
		if (hotel.hasBids()) {
			ArrayList<HotelBid> winners = hotel.getWinners();
			if (!winners.equals(null)) {
				for (int i = 0; i < winners.size(); i++) {
					transactions(typeName, hotel.day, winners.get(i).agent, winners.get(i).quantity, winners.get(i).price, winners.get(i).date);
				}
			}
		}
		
	}
	
	public Message hotelStatus(Message kqml) {
		Message response = new Message();
		response.setPerformative("hotelstatus");
		response.setSender("manager");
		response.setReceiver(kqml.getSender());
		String queryString = kqml.getContent();
		String query[] = queryString.split(" ");
		int type = Integer.valueOf(query[0]);
		int day = Integer.valueOf(query[1]);
		String agentName = query[2];
		HotelAuction hotel = null;
		for (int i = 0; i < hotAuct.size(); i++) {
			if (hotAuct.get(i).day == day && hotAuct.get(i).type == type) {
				hotel = hotAuct.get(i);
			}
		}
		String reply = "";
		if (hotel.opened) 
			reply = "open";
			else
				reply = "closed";
		
		response.setContent(reply);
		return response;
	}
	
	public Message hotelPrice(Message kqml) {
		Message response = new Message();
		response.setPerformative("hotelprices");
		response.setSender(kqml.getReceiver());
		response.setReceiver(kqml.getSender());
		
		String bidString = kqml.getContent();
		String bid[] = bidString.split(" ");
		int type = Integer.valueOf(bid[0]);
		int day = Integer.valueOf(bid[1]);
		String agentName = bid[2];
		
		HotelAuction hotel = null;
		for (int i = 0; i < hotAuct.size(); i++) {
			if (hotAuct.get(i).day == day && hotAuct.get(i).type == type) {
				hotel = hotAuct.get(i);
			}
		}
		response.setContent("" + hotel.ask_price());
				
		return response;
	}
	
	public Message hotelWinner(Message kqml) {
		Message response = new Message();
		response.setPerformative("hotelwinner");
		response.setSender(kqml.getReceiver());
		response.setReceiver(kqml.getSender());
		
		String queryString = kqml.getContent();
		String query[] = queryString.split(" ");
		int type = Integer.valueOf(query[0]);
		int day = Integer.valueOf(query[1]);
		String agentName = query[2];
		
		String result = "";
		
		HotelAuction hotel = null;
		for (int i = 0; i < hotAuct.size(); i++) {
			if (hotAuct.get(i).day == day && hotAuct.get(i).type == type) {
				hotel = hotAuct.get(i);
			}
		}
		
		if (!hotel.opened && hotel.hasBids()) {
			ArrayList<HotelBid> winners = hotel.getWinners();
			for (int i = 0; i < winners.size(); i++) {
				if (winners.get(i).agent.equals(agentName))
					result = winners.get(i).price + " " + winners.get(i).quantity;
					hotel.winners.remove(i);
			}

		}
		
		response.setContent(result);
		
		return response;
	}
	
	
	public void transactions(String typeName, int day, String sender, int qty, float price, String date) {
		float totalPrice = price * qty;
		transactionList.add(date + " " + typeName  + "_" + day + " " + sender + " Auction " + qty + " " + price);
		System.out.println(transactionList.get(transactionList.size()-1));

	}

	public boolean isOn() throws RemoteException {
		return isOn;
	}

	public long timeInterval() throws RemoteException {
		return interval;
	}

	private int[][] generateEndowment() {
		int[][] endowments = new int[3][4];
		Random rand = new Random();
		int d = rand.nextInt(2);
		if (d == 0)
			setEndowment(endowments, 0, 0, 3);
		else if (d == 1)
			setEndowment(endowments, 3, 0, 3);
		d = rand.nextInt(2);
		if (d == 0)
			setEndowment(endowments, 1, 1, 2);
		else if (d == 1)
			setEndowment(endowments, 2, 1, 2);
		return endowments;
	}

	private void setEndowment(int[][] endowments, int d, int day1, int day2) {
		int d2 = 0;
		int type = 0, type2 = 0;
		Random rand = new Random();

		type = rand.nextInt(3);
		endowments[type][d] = 4;
		type2 = rand.nextInt(3);
		while (type == type2)
			type2 = rand.nextInt(3);

		d2 = rand.nextInt(2);
		if (d2 == 0)
			d2 = day1;
		else
			d2 = day2;
		endowments[type2][d2] = 2;
	}

	private String getEndowments() {
		if (!isOn)
			return "The auction is closed!";
		String str = "";
		int[][] endowments = generateEndowment();
		for (int i = 0; i < endowments.length; i++) {
			for (int j = 0; j < endowments[0].length; j++)
				str += endowments[i][j] + " ";
			str += "\n";
		}
		return str;
	}

	public String genPref() {
		String str = "";
		Random rand = new Random();
		int in = rand.nextInt(4);
		int out = rand.nextInt(4) + 1;
		while (in >= out)
			out = rand.nextInt(4) + 1;
		int hot = rand.nextInt(100) + 50;
		int AW = rand.nextInt(200);
		int AP = rand.nextInt(200);
		int M = rand.nextInt(200);
		str += in + " " + out + " " + hot + " " + AW + " " + AP + " " + M + "\n";
		return str;
	}

	private String getPreferences() {
		if (!isOn)
			return "The auction is closed!";
		String str = "";
		for (int i = 0; i < 8; i++)
			str += genPref();
		return str;
	}

	public Date tacTime(long i) {
		Date d = new Date();
		long t = startTime.getTime();
		t += i;
		d.setTime(t);
		return d;
	}

	public void startAuction() {
		hotAuct = new ArrayList<HotelAuction>();
		// Create 8 hotelauctions with correct type and day
		for (int i = 2; i < 4; i++)
			for (int j = 0; j < 4; j++) 
				hotAuct.add(new HotelAuction(i, j));
		Date st = new Date();
		gameId = df.format(st);
		startTime = st;
		endTime = tacTime(gameLength);
		isOn = true;
		tacStatus();
		System.out.println("---> The TAC Game is on");
	}

	public void endAuction() {
		isOn = false;
		System.out.println("---> The TAC Game is closed");

		for (int i = 0; i < 4; i++) {
			inFlight[i] = -1;
			outFlight[i] = -1;
		}
	}

	private void tacStatus() {
		System.out.println("------------- The TAC Game ---------------");
		System.out.println("      Start: " + df.format(startTime));
		System.out.println("      End: " + df.format(endTime));
		System.out.println("      Next Game: " + df.format(tacTime(gameInterval)));
		System.out.println("----------------------------------------------------");
	}

	public void run() {
		startAuction();
		for (;;) {
			try {
				if (interval == gameInterval) {
					startAuction();
					interval = 0;
				} else if (interval == gameLength) {
					endAuction();
				}
				if (interval > 0 && interval % hotCloseInterval == 0 && !allHotelsClosed()) {
					closeHotelAuction();
				}
				interval += 1000;
				Thread.sleep(1000);
			} catch (InterruptedException e) {
			}
		}
	}
	


	// the main function
	public static void main(String[] args) throws Exception {
		System.setSecurityManager(new RMISecurityManager());
		String[] groups = new String[] { "" };

		// Create the instance of the service; the JoinManager will
		

		// register it and renew its leases with the lookup service
		Gr6_ManagerImpl csi = new Gr6_ManagerImpl();
		LookupDiscoveryManager mgr = new LookupDiscoveryManager(groups, null, null);
		JoinManager manager = new JoinManager(csi, null, csi, mgr, null);
		csi.run();
	}

}
