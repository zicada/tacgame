package Agents;

import java.rmi.RMISecurityManager;


import yinyang.ServiceFinder;
import Gr6_Manager.Gr6_Manager;

public class A3 extends A1 {
	private static Gr6_Manager cs;
	private long askPriceInterval;
	private int MAX_HOTEL_PRICE;
	private int MAX_EVENTS_PRICE;
	private int HOTEL_BID_INCREMENT;

	public A3(Gr6_Manager cs, String name) {
		super(cs, name);
		setMAX_HOTEL_PRICE(60);
		setMAX_EVENTS_PRICE(120);
		setHOTEL_BID_INCREMENT(4);
		setAskPriceInterval(10 * 1000);
		
	}
	public static void getHost() {
		try {
			ServiceFinder sf = new ServiceFinder(Gr6_Manager.class);
			cs = (Gr6_Manager) sf.getObject();
		} catch (Exception ex) {
		}
	}

	// the main function
	public static void main(String[] args) throws Exception {
		System.setSecurityManager(new RMISecurityManager());

		while (true) {
			try {
				A3 a3 = new A3(cs, "A3");
				a3.run();

			} catch (Exception ex) {
				getHost();
			}
		}


	}

	public long getAskPriceInterval() {
		return askPriceInterval;
	}

	public void setAskPriceInterval(long askPriceInterval) {
		this.askPriceInterval = askPriceInterval;
	}

	public int getHOTEL_BID_INCREMENT() {
		return HOTEL_BID_INCREMENT;
	}

	public void setHOTEL_BID_INCREMENT(int hOTEL_BID_INCREMENT) {
		HOTEL_BID_INCREMENT = hOTEL_BID_INCREMENT;
	}

	public int getMAX_EVENTS_PRICE() {
		return MAX_EVENTS_PRICE;
	}

	public void setMAX_EVENTS_PRICE(int mAX_EVENTS_PRICE) {
		MAX_EVENTS_PRICE = mAX_EVENTS_PRICE;
	}

	public int getMAX_HOTEL_PRICE() {
		return MAX_HOTEL_PRICE;
	}

	public void setMAX_HOTEL_PRICE(int mAX_HOTEL_PRICE) {
		MAX_HOTEL_PRICE = mAX_HOTEL_PRICE;
	}
	
}


