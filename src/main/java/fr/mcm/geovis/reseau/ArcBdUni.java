package fr.mcm.geovis.reseau;

import fr.ign.cogit.geoxygene.schemageo.impl.support.reseau.ArcReseauImpl;

public class ArcBdUni extends ArcReseauImpl{
	private int importance = -1;
	private String nature = "";
	
	public ArcBdUni(int importance) {
		this.importance = importance;
	}
	
	public ArcBdUni(int importance, String nature) {
		this.importance = importance;
		this.nature = nature;
	}

	public int getImportance() {
		return this.importance;
	}
	
	public String getNature() {
		return this.nature;
	}

}
