package fr.mcm.geovis.distances;

import fr.ign.cogit.distance.Distance;

public class DistanceNatImp implements Distance {
	//private int impRef;
	private int impComp;
	private String natComp;
	
	@Override
	public double getDistance() {
		if (natComp.toLowerCase() == "chemin" || impComp == -1)
			return 0.9;
		double sup; 
		if (impComp == 0)
			sup = 0.2;
		else if (impComp == 1)
			sup = 0.3;
		else 
			sup = 1. / impComp;
		return 1. - sup;
	}

	public void setImps(int comp, String nat) {
		//this.impRef = ref;
		this.impComp = comp;
		this.natComp = nat;
	}
	
	@Override
	public String getNom() {
		return "Nawak 2";
	}
}
