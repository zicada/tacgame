package Gr6_Manager;

import java.util.ArrayList;
import java.util.Collections;

public class HotelAuction {
	
	public Boolean opened;
	public ArrayList<HotelBid> bids;
	public ArrayList<HotelBid> winners;
	public int day;
	public int type;
	public static final float START_PRICE = 10;

	
	public HotelAuction(int type, int day) {
		bids = new ArrayList<HotelBid>();
		winners = new ArrayList<HotelBid>();
		this.type = type;
		this.day = day;
		opened = true;
	}
	
	public float ask_price() {
		int totalRooms = 0;
		float askPrice = START_PRICE;
		Collections.sort(bids);
		if (!hasBids()) {
			return askPrice;
		} else {
			for (int i = 0; i < bids.size(); i++) {
				totalRooms += bids.get(i).quantity;
				askPrice = bids.get(i).price;
				if (totalRooms > 16) {
					askPrice = bids.get(i).price;
					return askPrice;
				}
			}
		return askPrice;
		}
		
	}
	public ArrayList<HotelBid> getWinners() {
		int totalRooms = 0;
		Collections.sort(bids);
		if (!hasBids()) {
			return null;
		} else {
			for (int i = 0; i < bids.size(); i++) {
				totalRooms += bids.get(i).quantity;
				if (!bidderExists(bids.get(i).agent))
					winners.add(bids.get(i));
				if (totalRooms > 16) {
					break;
				}
			}
			return winners;
		}
	}
	
	public Boolean bidderExists(String agent) {
		for (int i = 0; i < winners.size(); i++) {
			if (winners.get(i).agent.equals(agent))
				return true;
		}
		return false;
	}
	
	public Boolean hasBids() {
		if(bids.isEmpty())
			return false;
		else
			return true;
	}
	


}
