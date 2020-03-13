package fr.mcm.geovis.criteres;

import fr.ign.cogit.criteria.Critere;
import fr.ign.cogit.criteria.CritereAbstract;
import fr.ign.cogit.distance.Distance;
import fr.ign.cogit.metadata.Objet;
import fr.mcm.geovis.distances.DistanceMapMatching;

public class CritereMapMatching extends CritereAbstract implements Critere {
    private String cleRef = "";
    private String cleComp = "";

    private double seuil = 0.6;
    private double eps = 0.01;

    public CritereMapMatching(Distance distance) {
        super(distance);
    }

    public void setMetadata(Objet objRef, Objet objetComp) {
        this.cleRef = objRef.getCle();
        this.cleComp = objetComp.getCle();
    }

    @Override
    public String getNom() {
        return "MapMatching Criteria";
    }

    @Override
    public double[] getMasse() throws Exception {
        int idRef = Integer.parseInt(featureRef.getAttribute(cleRef).toString());
        int idComp = Integer.parseInt(featureComp.getAttribute(cleComp).toString());
        //System.out.println("distance " + idRef + " - " + idComp);
        ((DistanceMapMatching) distance).setIdRefComp(idRef, idComp);
        double distNorm = distance.getDistance();

        double[] tableau = new double[3];

        if (distNorm < seuil) {
//            tableau[0] = 0.6;
//            tableau[1] = 0.1;
//            tableau[2] = 0.3;
            tableau[0] = (-(0.5 - eps) / seuil) * distNorm + 0.65;
            tableau[1] = ((0.1 - eps) / seuil) * distNorm;
            tableau[2] = (0.4 / seuil) * distNorm + 0.35;
        } else {
//            tableau[0] = eps;
//            tableau[1] = 0.1 - eps;
//            tableau[2] = 0.9;
            double dd = distNorm - seuil;
            tableau[0] = 0.2 - dd/8;
            tableau[1] = 0.3 + dd/4;
            tableau[2] = 0.5 - dd/8 ;

        }

//        // if importance not set
//        if (impComp == -1 || natComp == "") {
//            tableau[0] = 0;
//            tableau[1] = 0;
//            tableau[2] = 1;
//        }

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
