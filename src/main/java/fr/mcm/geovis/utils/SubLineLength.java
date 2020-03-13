package fr.mcm.geovis.utils;

import com.vividsolutions.jts.densify.Densifier;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.operation.distance.DistanceOp;

import fr.ign.cogit.geoxygene.api.spatial.coordgeom.IDirectPosition;
import fr.ign.cogit.geoxygene.api.spatial.coordgeom.ILineString;
import fr.ign.cogit.geoxygene.spatial.coordgeom.DirectPosition;
import fr.ign.cogit.geoxygene.util.algo.geometricAlgorithms.CommonAlgorithmsFromCartAGen;
import fr.ign.cogit.geoxygene.util.conversion.JtsGeOxygene;

public class SubLineLength {
    public static ILineString getProjectedLine(ILineString lineRef, ILineString linetoBeProjected) throws Exception {
        Geometry jtsRef = JtsGeOxygene.makeJtsGeom(lineRef);
        Geometry jtsComp = JtsGeOxygene.makeJtsGeom(linetoBeProjected);
        Coordinate c1 = DistanceOp.nearestPoints(jtsRef, ((LineString) jtsComp).getStartPoint())[0];
        Coordinate c2 = DistanceOp.nearestPoints(jtsRef, ((LineString) jtsComp).getEndPoint())[0];
        IDirectPosition p1 = new DirectPosition(c1.x, c1.y);
        //System.out.println("POINT(" + c1.x + " " + c1.y + ")");
        //System.out.println("POINT(" + c2.x + " " + c2.y + ")");
        IDirectPosition p2 = new DirectPosition(c2.x, c2.y);
        ILineString subline = CommonAlgorithmsFromCartAGen.getSubLine(lineRef, p1, p2);
        //System.out.println(subline);
        return subline;
    }
    
    public static double getProjectedLengthJtsInterp(ILineString lineRef, ILineString linetoBeProjected,
            double DENSIFIER) throws Exception {
        Geometry jtsRef = JtsGeOxygene.makeJtsGeom(lineRef);
        Geometry jtsComp = JtsGeOxygene.makeJtsGeom(linetoBeProjected);
        Coordinate c1 = DistanceOp.nearestPoints(jtsRef, ((LineString) jtsComp).getStartPoint())[0];
        Coordinate c2 = DistanceOp.nearestPoints(jtsRef, ((LineString) jtsComp).getEndPoint())[0];
        Geometry jtsRefds = Densifier.densify(jtsRef, DENSIFIER);
        int rangC1 = 0;
        int rangC2 = 0;
        Coordinate[] coords = ((LineString) jtsRefds).getCoordinates();
        for (int i = 0; i < coords.length; ++i) {
            if (coords[i].equals2D(c1, DENSIFIER / 2))
                rangC1 = i;
            if (coords[i].equals2D(c2, DENSIFIER / 2))
                rangC2 = i;
        }
        double distOnLine = Math.abs(rangC1 - rangC2) * DENSIFIER;
        return distOnLine;
    }

    
    public static double getSubLineLength(ILineString lineRef, ILineString linetoBeProjected) {
        double d = -1;
        try {
            //d = getProjectedLine(lineRef, linetoBeProjected).length();
            d = getProjectedLengthJtsInterp(lineRef, linetoBeProjected, 1.0);
        } catch (Exception e) { } //ugly as hell
        return d;
    }


}
