package fr.mcm.geovis;

import fr.ign.cogit.metadata.Objet;

public class MyObj implements Objet{
	public String cle = "ID";
	public String nom = "nom";
	public String nature = "nature";
	public String importance = "NIVEAU";

	public MyObj(String cle, String nom, String nature, String importance){
		this.cle = cle;
		this.nom = nom;
		this.nature = nature;
		this.importance = importance;
	}
	
	@Override
	public String getCle() {
		return this.cle;
	}

	@Override
	public String getNom() {
		return this.nom;
	}

	@Override
	public String getAttrNameSemantique() {
		return this.nature;
	}

	public String getAttrImportance() {
		return this.importance;
		
	}
}
