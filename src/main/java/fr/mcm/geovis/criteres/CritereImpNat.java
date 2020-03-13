package fr.mcm.geovis.criteres;

import fr.ign.cogit.criteria.Critere;
import fr.ign.cogit.criteria.CritereAbstract;
import fr.ign.cogit.distance.Distance;
import fr.mcm.geovis.MyObj;
import fr.mcm.geovis.distances.DistanceNatImp;

public class CritereImpNat extends CritereAbstract implements Critere{
	private String attrCompImportance = "";
	private String attrCompNat = "";
	private double seuil = 0.6;
	  
	private double eps = 0.01;


	public CritereImpNat(Distance distance) {
		super(distance);
	}
	
	public void setMetadata(MyObj cand) {
		this.attrCompNat = cand.getAttrNameSemantique();
		this.attrCompImportance = cand.getAttrImportance();
	}
	
	@Override
	public String getNom() {
		return "Critere ImpNat";
	}

//	@Override
//	public void setFeature(IFeature featureRef, IFeature featureComp) {
//		super.setFeature(featureRef, featureComp);
//	}

	@Override
	public double[] getMasse() throws Exception {
		int impComp = -1;
		String natComp = "";
		
		if (featureComp.getAttribute(attrCompImportance) != null && featureComp.getAttribute(attrCompImportance) != "") {
			try {
			impComp = Integer.parseInt((String)featureComp.getAttribute(attrCompImportance));
			}
			catch(NumberFormatException e) {
				//impComp = -1;
			}
		    //impComp=((Long)(featureComp.getAttribute(attrCompImportance))).intValue();
		 }
		    
		if (featureComp.getAttribute(attrCompNat) != null && featureComp.getAttribute(attrCompNat) != "") {
	        natComp=new String(featureComp.getAttribute(attrCompNat).toString().getBytes("ISO-8859-1"), "UTF-8");
	        //natComp=nomTopoComp.toLowerCase();
	    }
		
		
		((DistanceNatImp)distance).setImps(impComp, natComp);
		Double distNorm = distance.getDistance();
    
		double[] tableau = new double[3];

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
		if (impComp == -1 || natComp == "" ) {
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
