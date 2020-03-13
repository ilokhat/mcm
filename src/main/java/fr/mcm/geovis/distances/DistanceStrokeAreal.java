package fr.mcm.geovis.distances;

import java.util.Map;
import fr.ign.cogit.distance.Distance;

public class DistanceStrokeAreal implements Distance{
    
    private Map<Integer, Double> strokesDistResults;
    private int idComp;

    @Override
    public double getDistance() {
        if (!strokesDistResults.containsKey(idComp))
            return 0.85;
        double dist = strokesDistResults.get(idComp) >= 0.99 ? 1.0: strokesDistResults.get(idComp);
        return dist;
    }

    @Override
    public String getNom() {
        return "Areal Stroke Distance";
    }
    
    public void setStrokesDistResults(Map<Integer, Double> strokesDistResults) {
        this.strokesDistResults = strokesDistResults;
        //System.out.println(strokesDistResults);
    }
    
    public void setIdComp(int idComp) {
        this.idComp = idComp;
    }


}
