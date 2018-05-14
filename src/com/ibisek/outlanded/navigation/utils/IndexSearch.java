package com.ibisek.outlanded.navigation.utils;

/**
 * Metoda odhadu polohy v intervalu. Funguje dobre na ekvidistantni data, jinak
 * moc ne :(
 * 
 * @author ibisek
 * @version 2013-11-09
 */
public class IndexSearch {

	private int maxLoops;
	private int indexDelta;

	/**
	 * @param maxLoops
	 * @param indexDelta
	 *          index tolerance
	 */
	public IndexSearch(int maxLoops, int indexDelta) {
		this.maxLoops = maxLoops;
		this.indexDelta = indexDelta;
	}

	/**
	 * @param value
	 *          searched value
	 * @param values
	 *          list of values
	 * @return
	 */
	public int findIndex(double value, double[] values) {
		int minIndex = 0, maxIndex = values.length - 1;
		double minVal = 0, maxVal = values[maxIndex];

		if (value < minVal)
			return minIndex;
		if (value > maxVal)
			return maxIndex;

		int newIndex = -1;
		int previousIndexApproximation = minIndex;
		int i = 0;
		while (i++ < maxLoops) {
			// percents from the range:
			double p = (value - minVal) / (maxVal - minVal);

			newIndex = (int) Math.round(minIndex + (maxIndex - minIndex) * p);
			// System.out.println("i="+i+"; newIndex="+newIndex);

			// are we finished?
			if (value == values[newIndex])
				return newIndex; // not very likely
			if (Math.abs(newIndex - previousIndexApproximation) < indexDelta)
				return newIndex;
			previousIndexApproximation = newIndex;

			// set-up new indexes a search again..
			if (value < values[newIndex]) {
				maxIndex = newIndex;
				maxVal = values[maxIndex];
			} else {
				minIndex = newIndex;
				minVal = values[minIndex];
			}
		}

		return newIndex;
	}

	public static void main(String[] args) {

		int n = 100;
		double[] values = new double[n];
		for (int i = 0; i < n; i++) {
			values[i] = i;
		}

		double value = 66.6d;

		IndexSearch indexSearch = new IndexSearch(10, 4);
		int i = indexSearch.findIndex(value, values);

		System.out.println(String.format("values[%s]=%s", i, values[i]));

	}
}
