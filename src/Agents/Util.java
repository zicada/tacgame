package Agents;

public class Util {
	public static double roundUp(double x, int n) {
		double y = x;
		int d = 1;

		for (int i = 0; i < n; i++)
			d *= 10;

		y = roundUp(y * d);

		return y / d;
	}

	public static double roundUp(double x) {
		return (double) (Math.round(x));
	}

}