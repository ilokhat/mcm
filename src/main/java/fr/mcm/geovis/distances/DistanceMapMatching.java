package fr.mcm.geovis.distances;

import java.util.Map;
import java.util.Set;

import fr.ign.cogit.distance.Distance;

public class DistanceMapMatching implements Distance {
    private int idRef;
    private int idComp;
    private Map<Integer, Set<Integer>> mapmatchResult;

    @Override
    public double getDistance() {
        double score = 0.85;
        Set<Integer> troncons250k = mapmatchResult.get(idComp);
        if (troncons250k == null)
            return 0.85;
        if (troncons250k.size() == 1 && troncons250k.contains(idRef))
            return 0.2;
        if (troncons250k.size() > 1 && troncons250k.contains(idRef))
            return 1. - (1. / troncons250k.size());
        return score;
    }

    @Override
    public String getNom() {
        return "Distance MapMatching";
    }
    
    public void setResultMap(Map<Integer, Set<Integer>> mapmatchResult) {
        this.mapmatchResult = mapmatchResult;
    }
    
    public void setIdRefComp(int idRef, int idComp) {
        this.idRef = idRef;
        this.idComp = idComp;
    }

}
