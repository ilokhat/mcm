package fr.mcm.geovis.distances;

import fr.ign.cogit.distance.Distance;
import fr.ign.cogit.geoxygene.api.spatial.coordgeom.ILineString;
import fr.mcm.geovis.utils.SubLineLength;

public class DistanceSubLineProj implements Distance{
    private ILineString lref;
    private ILineString lcomp;

    @Override
    public double getDistance() {
        double sublength = SubLineLength.getSubLineLength(lref, lcomp);
        if (sublength < 0)
            return 0.99;
        double ratio = sublength / lcomp.length() > 1. ? lcomp.length() / sublength : sublength / lcomp.length();
        return 1 - ratio ;
    }

    @Override
    public String getNom() {
        return "projected Subline ratio" ;
    }
    
    public void setLines(ILineString ref, ILineString comp) {
        this.lref = ref;
        this.lcomp = comp;
        
    }

}
