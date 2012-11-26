package Flight;

import yinyang.*;

import java.io.*;
import java.util.*;
import java.rmi.*;

import Gr6_Manager.Gr6_Manager;

public class Flight {
	public double[] inFlightPrice = new double[4];
	public double[] outFlightPrice = new double[4];
	public long interval = 0;
	private long gameLength = 9 * 60 * 1000; // the length of a game
	private long pertubeInterval = 10 * 1000; // the length of updating price
	private Gr6_Manager man;
	private static Gr6_Manager cs;

	public Flight(Gr6_Manager cs) {
		man = cs;
	}

	private double random(double a, double b) {
		Random rand = new Random();
		double val = a + rand.nextDouble() * (b - a);
		return Util.roundUp(val, 2);
	}

	// ----- pertube the price with a stochastic function
	private void pertubePrice(int i, int type, long t) {
		double bound = random(-10, 30);
		double x = 10 + (t * 1.0 / gameLength) * (bound - 10);
		double y = 0;

		if (x > 0)
			y = random(-10, x);
		else if (x < 0)
			y = random(x, 10);
		else if (x == 0)
			y = random(-10, 10);
		if (type == 0)
			inFlightPrice[i] += y;
		else if (type == 1)
			outFlightPrice[i] += y;
	}

	// execute the thread
	public void run() throws IOException, RemoteException, NotBoundException {
		for (;;) {
			try {
				boolean isOn = getStatus();
				if (isOn && interval == 0)
					start();
				else if (isOn && interval < gameLength && interval > 0) {
					interval += pertubeInterval;
					for (int i = 0; i < 4; i++) {
						pertubePrice(i, 0, interval);
						pertubePrice(i, 1, interval);
					}
					sendFlightPrices();
				} else if (!isOn || interval > gameLength)
					close();
				Thread.sleep(pertubeInterval);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	public void sendFlightPrices() {
		Message msg = new Message();
		msg.setPerformative(TACOntology.flight_prices);
		msg.setSender("Flight");
		msg.setReceiver("Manager");
		msg.setContent(flightPrices());
		try {
			@SuppressWarnings("unused")
			Message res = man.send(msg);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	public String flightPrices() {
		String str = "";
		for (int i = 0; i < 4; i++)
			str += Util.roundUp(inFlightPrice[i], 2) + " ";
		for (int i = 0; i < 4; i++)
			str += Util.roundUp(outFlightPrice[i], 2) + " ";
		System.out.println(str);
		return str;
	}

	// ask for status
	public boolean getStatus() {
		boolean isOn = false;
		Message msg = new Message();
		msg.setPerformative(TACOntology.ask_status);
		msg.setSender("Flight");
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

	// ask for time interval
	public int getTimeInterval() {
		int time = 0;
		Message msg = new Message();
		msg.setPerformative(TACOntology.ask_time);
		msg.setSender("Flight");
		msg.setReceiver("Manager");
		try {
			Message res = man.send(msg);
			time = Integer.parseInt(res.getContent());
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		return time;
	}

	// close the game round
	public void close() {
		interval = 0;
	}

	// start the game round
	public void start() {
		interval = getTimeInterval();
		for (int i = 0; i < 4; i++) {
			inFlightPrice[i] = random(250, 400);
			outFlightPrice[i] = random(250, 400);
		}
		System.out.println("Time : " + (interval * 1. / 1000) + " seconds");
		sendFlightPrices();
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
				Flight flight = new Flight(cs);
				flight.run();

			} catch (Exception ex) {
				getHost();
			}
		}
	}

}
