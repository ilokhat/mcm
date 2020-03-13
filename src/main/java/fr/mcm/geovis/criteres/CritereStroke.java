package fr.mcm.geovis.criteres;

import fr.ign.cogit.criteria.Critere;
import fr.ign.cogit.criteria.CritereAbstract;
import fr.ign.cogit.distance.Distance;
import fr.ign.cogit.metadata.Objet;
import fr.mcm.geovis.distances.DistanceMapMatching;
import fr.mcm.geovis.distances.DistanceStrokeAreal;

public class CritereStroke extends CritereAbstract implements Critere{    
    private String cleRef = "";
    private String cleComp = "";

    private double seuil = 0.025;
    private double eps = 0.001;


    public CritereStroke(Distance distance) {
        super(distance);
    }
    
    public void setMetadata(Objet objRef, Objet objetComp) {
        this.cleRef = objRef.getCle();
        this.cleComp = objetComp.getCle();
    }

    @Override
    public String getNom() {
        return "Stroke criteria";
    }

    @Override
    public double[] getMasse() throws Exception {
        int idComp = Integer.parseInt(featureComp.getAttribute(cleComp).toString());
        //System.out.println("distance " + idRef + " - " + idComp);
        ((DistanceStrokeAreal) distance).setIdComp(idComp);
        double distNorm = distance.getDistance();

        double[] tableau = new double[3];

        if (distNorm < seuil) {
//            tableau[0] = (-(0.5 - eps) / seuil) * distNorm + 0.5;
//            tableau[1] = ((0.1 - eps) / seuil) * distNorm;
//            tableau[2] = (0.4 / seuil) * distNorm + 0.5;
            double dd =  (seuil-distNorm);
            tableau[0] = 0.5 + dd;
            tableau[1] = 0.3 - dd/2;
            tableau[2] = 0.2 - dd/2;

        } else {
//            tableau[0] = 0.2 - eps;
//            tableau[1] = 0.3 + eps;
//            tableau[2] = 0.5;
            double dd = (distNorm-seuil) > 0.4 ? 0.4 : distNorm-seuil;
            if (dd > 0.01 && dd < 0.1)
                dd = dd * 3;
            tableau[0] = 0.25 - dd /2 ;
            tableau[1] = 0.40 + dd ;
            tableau[2] = 0.35 - dd /2;

//            tableau[0] = eps;
//            tableau[1] = 0.1 - eps;
//            tableau[2] = 0.9;
        }

        try {
            checkSommeMasseEgale1(tableau);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
        // Return 3 masses sous forme de tableau
        return tableau;

    }

}
