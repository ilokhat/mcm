package fr.mcm.geovis.criteres;

import fr.ign.cogit.criteria.Critere;
import fr.ign.cogit.criteria.CritereAbstract;
import fr.ign.cogit.distance.Distance;
import fr.ign.cogit.geoxygene.api.spatial.coordgeom.ILineString;
import fr.mcm.geovis.distances.DistanceSubLineProj;

public class CritereSubline extends CritereAbstract implements Critere{
    private double seuil = 0.22;
    private double eps = 0.1;
    
    public CritereSubline(Distance distance) {
        super(distance);
    }

    @Override
    public String getNom() {
        return "Subline Criteria";
    }

    @Override
    public double[] getMasse() throws Exception {
        ILineString lineRef = (ILineString) featureRef.getGeom();
        ILineString lineComp = (ILineString) featureComp.getGeom();
        ((DistanceSubLineProj) distance).setLines(lineRef, lineComp);
        double distNorm = distance.getDistance();
        //System.out.println("------- fid " + featureComp.getAttribute("ID") + " --- " + distNorm);
        double[] tableau = new double[3];

        if (distNorm < seuil) {
//            double dd = (seuil-distNorm);
//            tableau[0] = 0.5 + dd;
//            tableau[1] = 0.2 - dd/2;
//            tableau[2] = 0.3 - dd/2;
            tableau[0] = (-(0.5 - eps) / seuil) * distNorm + 0.70; //0.65;
            tableau[1] = ((0.2 - eps) / seuil) * distNorm;
            tableau[2] = (0.3 / seuil) * distNorm + 0.30; //0.35;

        } else {
            double dd = (distNorm-seuil) > 0.4 ? 0.4 : distNorm-seuil;
            if (dd > 0.01 && dd < 0.1)
                dd = dd * 3;
            tableau[0] = 0.3 - dd/2 ;
            tableau[1] = 0.5 - dd/2 ;
            tableau[2] = 0.2 + dd;

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
