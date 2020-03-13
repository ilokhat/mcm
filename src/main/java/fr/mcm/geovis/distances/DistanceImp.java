package fr.mcm.geovis.distances;

import fr.ign.cogit.distance.Distance;

public class DistanceImp implements Distance{
	public int impRef;
	public int impComp;
	
	@Override
	public double getDistance() {
		int diff =  Math.abs(impRef - impComp);
		if (diff == 0)
			return 0;
		else if (diff <= 1)
			return 0.2;
		else if (diff <= 2)
			return 0.5;
		else if (diff <= 3)
			return 0.8;
		return 0.9;
	}

	public void setImps(int ref, int comp) {
		this.impRef = ref;
		this.impComp = comp;
	}
	
	@Override
	public String getNom() {
		return "Distance Nawak";
	}

}
