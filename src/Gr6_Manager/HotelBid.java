package Gr6_Manager;

public class HotelBid implements Comparable<HotelBid> {
	
	
	public int type;
	public int day;
	public float price;
	public String agent;
	public int quantity;
	public String date;
	
	public HotelBid(int type, int day, float price, String agent, int quantity, String date) {
		this.type = type;
		this.day = day;
		this.price = price;
		this.agent = agent;
		this.quantity = quantity;
		this.date = date;
	}

	@Override
	public int compareTo(HotelBid o) {
		
		return (int) (o.price - this.price);
	}



	
	
}

