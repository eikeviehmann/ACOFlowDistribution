package de.acomanetopt.manetmodel;

import de.jgraphlib.graph.EdgeDistance;

public class LinkQuality extends EdgeDistance {

	public LinkQuality() {
		transmissionRate = new DataRate(0L);
		utilization = new DataRate(0L);
	}

	// Amount of interfered links
	private int interference;

	// Reception power in dB
	private double receptionPower;

	// Rate as unit (bits,kbits,mbits,...)
	private DataRate transmissionRate;

	// Rate as unit (bits,kbits,mbits,...)
	private DataRate utilization;

	// Number of actively and passively utilized links inclusive own (in
	// interference range)
	private int utilizedLinks;

	public double getReceptionPower() {
		return receptionPower;
	}

	public void setReceptionPower(double receptionPower) {
		this.receptionPower = receptionPower;
	}

	public int getUtilizedLinks() {
		return utilizedLinks;
	}

	public void setUtilizedLinks(int utilizedLinks) {
		this.utilizedLinks = utilizedLinks;
	}

	public DataRate getTransmissionRate() {
		return this.transmissionRate;
	}

	public void setTransmissionRate(DataRate transmissionBitrate) {
		this.transmissionRate = transmissionBitrate;
	}

	public DataRate getUtilization() {
		return this.utilization;
	}

	public void setUtilization(DataRate u) {
		this.utilization = u;
	}

}
