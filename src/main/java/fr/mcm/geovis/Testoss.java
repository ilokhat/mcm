package fr.mcm.geovis;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.math3.geometry.euclidean.threed.SubLine;

import com.vividsolutions.jts.densify.Densifier;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.operation.distance.DistanceOp;
import com.vividsolutions.jts.operation.linemerge.LineMerger;
import com.vividsolutions.jts.operation.linemerge.LineSequencer;

import fr.ign.cogit.cartagen.spatialanalysis.network.Stroke;
import fr.ign.cogit.geoxygene.api.feature.IFeature;
import fr.ign.cogit.geoxygene.api.feature.IPopulation;
import fr.ign.cogit.geoxygene.api.spatial.coordgeom.IDirectPosition;
import fr.ign.cogit.geoxygene.api.spatial.coordgeom.ILineString;
import fr.ign.cogit.geoxygene.api.spatial.geomroot.IGeometry;
import fr.ign.cogit.geoxygene.contrib.cartetopo.Arc;
import fr.ign.cogit.geoxygene.contrib.cartetopo.CarteTopo;
import fr.ign.cogit.geoxygene.contrib.cartetopo.CarteTopoFactory;
import fr.ign.cogit.geoxygene.contrib.clustering.minimalspanningtree.MinimalSpanningTreeTriangulation;
import fr.ign.cogit.geoxygene.feature.Population;
import fr.ign.cogit.geoxygene.spatial.coordgeom.DirectPosition;
import fr.ign.cogit.geoxygene.util.algo.NaturalRoads;
import fr.ign.cogit.geoxygene.util.algo.geometricAlgorithms.CommonAlgorithmsFromCartAGen;
import fr.ign.cogit.geoxygene.util.conversion.JtsGeOxygene;
import fr.ign.cogit.geoxygene.util.conversion.ParseException;
import fr.ign.cogit.geoxygene.util.conversion.WktGeOxygene;
import fr.mcm.geovis.utils.FeaturesHelper;
import fr.mcm.geovis.utils.StrokesHelper;
import fr.mcm.geovis.utils.SubLineLength;

public class Testoss {

    public static Collection<IGeometry> geomsFromCsv(String csvResultFile) throws IOException, ParseException {
        // List<IGeometry> res = new ArrayList<>();
        Set<IGeometry> res = new HashSet<>();
        Reader in = new FileReader(csvResultFile);
        Iterable<CSVRecord> records = CSVFormat.RFC4180.withDelimiter(';').parse(in);
        Set<String> ss = new HashSet<>();
        for (CSVRecord record : records) {
            String wkt = record.get(2);
            IGeometry line = WktGeOxygene.makeGeOxygene(wkt);
            res.add(line);
            ss.add(wkt);
        }
        double length = 0;
        for (String s : ss)
            length += WktGeOxygene.makeGeOxygene(s).length();
        // System.out.println("ss "+ ss.size() + ": " + length);
        return res;
    }

    public static double getProjectedLength(ILineString lineRef, ILineString linetoBeProjected) throws Exception {
        Geometry jtsRef = JtsGeOxygene.makeJtsGeom(lineRef);
        Geometry jtsComp = JtsGeOxygene.makeJtsGeom(linetoBeProjected);
        Coordinate c1 = DistanceOp.nearestPoints(jtsRef, ((LineString) jtsComp).getStartPoint())[0];
        Coordinate c2 = DistanceOp.nearestPoints(jtsRef, ((LineString) jtsComp).getEndPoint())[0];
        IDirectPosition p1 = new DirectPosition(c1.x, c1.y);
        System.out.println("POINT(" + c1.x + " " + c1.y + ")");
        System.out.println("POINT(" + c2.x + " " + c2.y + ")");
        IDirectPosition p2 = new DirectPosition(c2.x, c2.y);
        ILineString subline = CommonAlgorithmsFromCartAGen.getSubLine(lineRef, p1, p2);
        System.out.println(subline);
        return subline.length();
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

    public static void main(String[] args) throws Exception {
        MinimalSpanningTreeTriangulation mst = new MinimalSpanningTreeTriangulation();
        
         String lineRefWkt = "LINESTRING(982117 6500897, 982076 6500884, 981454 6500518, 981425 6500495)";
        // String lineCompWkt = "LINESTRING(981701.40000000002328306 6500697,
        // 981745.19999999995343387 6500675.09999999962747097,
        // 981774.69999999995343387 6500660.20000000018626451)";
        //
         IGeometry lineRef = WktGeOxygene.makeGeOxygene(lineRefWkt);
        // IGeometry lineComp = WktGeOxygene.makeGeOxygene(lineCompWkt);
         ILineString lineR = (ILineString) lineRef;
         lineRef.coord();
        // ILineString lineC = (ILineString) lineComp;
        //
        // System.out.println(getProjectedLength(lineR, lineC));
        // System.out.println(getProjectedLengthJtsInterp(lineR, lineC, 1.0));

        String pathTo250kShape = "/home/imran/projets/multicriteriamatching/routes_appariement/routes_250k_alpes.shp";
        String pathToBdUniShape = "/home/imran/projets/multicriteriamatching/routes_appariement/routes_bduni_alpes.shp";

        System.out.println("*************** loading networks");
        IPopulation<IFeature> reseau250k = FeaturesHelper.loadShapeToLineStrings(pathTo250kShape);
        IPopulation<IFeature> reseauBdUni = FeaturesHelper.loadShapeToLineStrings(pathToBdUniShape);
        System.out.println("*************** getting mapmatcher results");
        IFeature ref = FeaturesHelper.getFeatureById(reseau250k, 254734); // reseau250k.get(250);
        IGeometry buff = ref.getGeom().buffer(120);
        IPopulation<IFeature> selection = new Population<>();
        selection.addAll(reseauBdUni.select(buff));

        for (IFeature f : selection) {
            if (true)
                continue;
            ILineString subline = SubLineLength.getProjectedLine((ILineString) ref.getGeom(),
                    (ILineString) f.getGeom());
            double d = subline.length();
            double dd = ArealDifference.estimate(subline, (ILineString) f.getGeom());
            // double d =
            // getProjectedLengthJtsInterp((ILineString)ref.getGeom(),
            // (ILineString)f.getGeom(), 0.5);
            // System.out.println(f.getAttribute("ID")+ ";" + f.getGeom() + ";"
            // + d/f.getGeom().length());
            System.out.println(f.getAttribute("ID") + ";" + ";" + Math.abs(1 - d / f.getGeom().length()) + ";"
                    + dd / ((ILineString) f.getGeom()).length());
            System.out.println(subline);
        }
        // System.out.println(ref.getGeom());
        List<ILineString> roads = new ArrayList<>();
        for (IFeature f : selection) {
            roads.add((ILineString) f.getGeom());
        }
        List<ILineString> ll = (List<ILineString>) NaturalRoads.MakeNaturalRoads(roads, 1, 0.5, 6.14);

        for (ILineString l : ll)
            System.out.println(l);
        //
        System.out.println("-------------------------------------------------------------");

        Set<Stroke> strokes = StrokesHelper.buildStrokesNetwork(selection);
        for (Stroke s : strokes)
            System.out.println(s.getGeomStroke());
        //
        // System.out.println(strokes.size() + " -- " + ll.size());

        double total_length = 0;
        for (IFeature f1 : reseau250k)
            total_length += f1.getGeom().length();
        double res250kLength = total_length;
        System.out.println("Reseau 250k : " + total_length);
        String csvFile = "geoms3crits_3.csv";
        String csvRes = "/home/imran/projets/multicriteriamatching/routes_appariement/" + csvFile;
        Collection<IGeometry> res = geomsFromCsv(csvRes);
        total_length = 0;
        for (IGeometry g : res)
            total_length += g.length();
        System.out.println(csvFile + "(" + res.size() + ") : " + total_length + " -- ratio (res/ref) : "
                + total_length / res250kLength);

        csvFile = "geomsAll.csv";
        csvRes = "/home/imran/projets/multicriteriamatching/routes_appariement/" + csvFile;
        res = geomsFromCsv(csvRes);
        total_length = 0;
        for (IGeometry g : res)
            total_length += g.length();
        System.out.println(csvFile + "(" + res.size() + ") : " + total_length + " -- ratio (res/ref) : "
                + total_length / res250kLength);
        
        csvFile = "geoms3crits_natroads.csv";
        csvRes = "/home/imran/projets/multicriteriamatching/routes_appariement/" + csvFile;
        res = geomsFromCsv(csvRes);
        total_length = 0;
        for (IGeometry g : res)
            total_length += g.length();
        System.out.println(csvFile + "(" + res.size() + ") : " + total_length + " -- ratio (res/ref) : "
                + total_length / res250kLength);
        
//        for (IFeature f: selection) {
//            for (int i = 0; i < ll.size() ; ++i) {
//                ILineString nr = ll.get(i);
//                if (f.getGeom().within(nr))
//                    System.out.println(f.getAttribute("ID") + " - " + i + " -- " + nr);
//            }
//        }
        

    }

}
