package fr.mcm.geovis.criteres;

import fr.ign.cogit.criteria.Critere;
import fr.ign.cogit.criteria.CritereAbstract;
import fr.ign.cogit.distance.Distance;
import fr.ign.cogit.distance.text.DistanceAbstractText;
import fr.ign.cogit.geoxygene.api.feature.IFeature;
import fr.ign.cogit.metadata.Objet;
import fr.mcm.geovis.MyObj;
import fr.mcm.geovis.distances.DistanceImp;

public class CritereImportance extends CritereAbstract implements Critere{
	private String attrRefImportance = ""; //NIVEAU";
	private String attrCompImportance = ""; //IMPORTANCE";
	private double seuil = 0.6;
	  
	private double eps = 0.01;


	public CritereImportance(Distance distance) {
		super(distance);
	}

	public void setMetadata(MyObj ref, MyObj cand) {
		this.attrRefImportance = ref.getAttrImportance();
		this.attrCompImportance = cand.getAttrImportance();
	}
	
	@Override
	public String getNom() {
		return "Critere Importance";
	}

//	@Override
//	public void setFeature(IFeature featureRef, IFeature featureComp) {
//		super.setFeature(featureRef, featureComp);
//	}

	@Override
	public double[] getMasse() throws Exception {
		int impRef = -1;
		int impComp = -1;
		
		if (featureComp.getAttribute(attrCompImportance) != null && featureComp.getAttribute(attrCompImportance) != "") {
			impComp = Integer.parseInt((String)featureComp.getAttribute(attrCompImportance));
		    //impComp=((Long)(featureComp.getAttribute(attrCompImportance))).intValue();
		 }
		    
		if (featureRef.getAttribute(attrRefImportance) != null) {
		       impRef=((Long)(featureRef.getAttribute(attrRefImportance))).intValue();
		}
		((DistanceImp)distance).setImps(impRef, impComp);
		Double distNorm = distance.getDistance();
    
		double[] tableau = new double[3];
//		if (distNorm.isNaN()) {
//            tableau[0] = 0;
//            tableau[1] = 0;
//            tableau[2] = 1;
//        } else 
        if (distNorm < seuil) {
			tableau[0] = (-(0.5 - eps)/seuil)*distNorm + 0.5;
			tableau[1] = ((0.1-eps)/seuil) * distNorm;
			tableau[2] = (0.4/seuil) * distNorm + 0.5;
		} else {
			tableau[0] = eps;
			tableau[1] = 0.1 - eps;
			tableau[2] = 0.9;
		}
    
		// if importance not set 
		if (impRef == -1 || impComp == -1 ) {
			tableau[0] = 0;
			tableau[1] = 0;
			tableau[2] = 1;
		}
    
		try {
			checkSommeMasseEgale1(tableau);
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
  
		// 	Return 3 masses sous forme de tableau
		return tableau;
	}

}
