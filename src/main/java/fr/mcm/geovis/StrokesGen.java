package fr.mcm.geovis;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.geotools.referencing.CRS;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import fr.ign.cogit.cartagen.spatialanalysis.network.Stroke;
import fr.ign.cogit.cartagen.spatialanalysis.network.StrokesNetwork;
import fr.ign.cogit.geoxygene.api.feature.IFeature;
import fr.ign.cogit.geoxygene.api.feature.IPopulation;
import fr.ign.cogit.geoxygene.api.feature.type.GF_AttributeType;
import fr.ign.cogit.geoxygene.api.spatial.coordgeom.ILineString;
import fr.ign.cogit.geoxygene.api.spatial.geomprim.IOrientableCurve;
import fr.ign.cogit.geoxygene.api.spatial.geomroot.IGeometry;
import fr.ign.cogit.geoxygene.contrib.cartetopo.Arc;
import fr.ign.cogit.geoxygene.contrib.cartetopo.CarteTopo;
import fr.ign.cogit.geoxygene.contrib.cartetopo.CarteTopoFactory;
import fr.ign.cogit.geoxygene.contrib.clustering.minimalspanningtree.MinimalSpanningTreeTriangulation;
import fr.ign.cogit.geoxygene.contrib.clustering.minimalspanningtree.triangulationmodel.Cluster;
import fr.ign.cogit.geoxygene.feature.DefaultFeature;
import fr.ign.cogit.geoxygene.feature.Population;
import fr.ign.cogit.geoxygene.feature.SchemaDefaultFeature;
import fr.ign.cogit.geoxygene.schema.schemaConceptuelISOJeu.AttributeType;
import fr.ign.cogit.geoxygene.schema.schemaConceptuelISOJeu.FeatureType;
import fr.ign.cogit.geoxygene.schemageo.api.support.reseau.ArcReseau;
import fr.ign.cogit.geoxygene.schemageo.api.support.reseau.NoeudReseau;
import fr.ign.cogit.geoxygene.schemageo.impl.support.reseau.ArcReseauImpl;
import fr.ign.cogit.geoxygene.schemageo.impl.support.reseau.NoeudReseauImpl;
import fr.ign.cogit.geoxygene.spatial.coordgeom.GM_LineString;
import fr.ign.cogit.geoxygene.spatial.geomaggr.GM_MultiCurve;
import fr.ign.cogit.geoxygene.util.conversion.ShapefileReader;
import fr.ign.cogit.geoxygene.util.conversion.ShapefileWriter;
import fr.mcm.geovis.reseau.ArcBdUni;
import fr.mcm.geovis.utils.MapmatcherResultHelper;

public class StrokesGen {

    public static IPopulation<IFeature> loadShapeToLineStrings(String multiLineShapePath) {
        IPopulation<IFeature> reseau = ShapefileReader.read(multiLineShapePath);
        for (IFeature f : reseau) {
            @SuppressWarnings("unchecked")
            GM_MultiCurve<IOrientableCurve> l = (GM_MultiCurve<IOrientableCurve>) f.getGeom();
            GM_LineString ll = (GM_LineString) l.get(0);
            f.setGeom(ll);
        }
        return reseau;
    }

    public static void createShape(HashMap<Long, List<IGeometry>> strokesMap, String fileName) throws Exception {
        // Créer un featuretype du jeu correspondant
        FeatureType newFeatureType = new FeatureType();
        newFeatureType.setTypeName("troncon");
        newFeatureType.setGeometryType(ILineString.class);

        AttributeType id250k = new AttributeType("id250k", "String");
        newFeatureType.addFeatureAttribute(id250k);

        // Création d'un schéma associé au featureType
        SchemaDefaultFeature schema = new SchemaDefaultFeature();
        schema.setFeatureType(newFeatureType);

        newFeatureType.setSchema(schema);

        Map<Integer, String[]> attLookup = new HashMap<Integer, String[]>(0);
        attLookup.put(new Integer(0), new String[] { id250k.getNomField(), id250k.getMemberName() });
        schema.setAttLookup(attLookup);

        // Création de la population
        Population<DefaultFeature> entrees = new Population<DefaultFeature>(false, "entrees", DefaultFeature.class,
                true);
        entrees.setFeatureType(newFeatureType);

        // On ajoute les defaults features à la collection
        for (Map.Entry<Long, List<IGeometry>> entry : strokesMap.entrySet()) {
            Long id = entry.getKey();
            for (IGeometry geom : entry.getValue()) {
                DefaultFeature n = entrees.nouvelElement(geom);
                n.setSchema(schema);
                Object[] attributes = new Object[] { Long.toString(id) };
                n.setAttributes(attributes);
            }
        }

        CoordinateReferenceSystem crs = CRS.decode("EPSG:2154");
        ShapefileWriter.write(entrees, fileName, crs);
    }

    private static HashSet<ArcReseau> buildArcs(CarteTopo carte) {
        HashSet<ArcReseau> arcs = new HashSet<ArcReseau>();
        HashMap<IGeometry, NoeudReseau> geomsNoeuds = new HashMap<IGeometry, NoeudReseau>();

        for (Arc feat : carte.getListeArcs()) {
            int imp = -1;
            String nature = feat.getCorrespondants().get(0).getAttribute("NATURE").toString();
            try {
                imp = Integer.parseInt(feat.getCorrespondants().get(0).getAttribute("IMPORTANCE").toString());
            } catch (NumberFormatException e) {
                System.out.println("Sans Objet");
            }
            // ArcReseau arc = new ArcReseauImpl();
            ArcReseau arc = new ArcBdUni(imp, nature);
            arc.setGeom(feat.getGeom());
            arc.setId(((Long) (feat.getCorrespondants().get(0).getAttribute("ID"))).intValue());
            IGeometry noeudIniGeom = feat.getNoeudIni().getGeom();
            IGeometry noeudFinGeom = feat.getNoeudFin().getGeom();

            if (!geomsNoeuds.containsKey(noeudIniGeom)) {
                NoeudReseau noeudIni = new NoeudReseauImpl();
                noeudIni.setGeom(noeudIniGeom);
                geomsNoeuds.put(noeudIniGeom, noeudIni);
            }

            if (!geomsNoeuds.containsKey(noeudFinGeom)) {
                NoeudReseau noeudFin = new NoeudReseauImpl();
                noeudFin.setGeom(noeudFinGeom);
                geomsNoeuds.put(noeudFinGeom, noeudFin);
            }
            NoeudReseau noeudIni = geomsNoeuds.get(noeudIniGeom);
            NoeudReseau noeudFin = geomsNoeuds.get(noeudFinGeom);
            noeudIni.getArcsSortants().add(arc);
            noeudFin.getArcsEntrants().add(arc);
            arc.setNoeudInitial(noeudIni);
            arc.setNoeudFinal(noeudFin);
            arcs.add(arc);
        }
        return arcs;
    }

    public static Set<Stroke> buildStrokesNetwork(IPopulation<IFeature> selection) {
        CarteTopo carte = CarteTopoFactory.newCarteTopo(selection);
        carte.creeNoeudsManquants(0.5);
        carte.fusionNoeuds(0.5);
        HashSet<ArcReseau> arcs = buildArcs(carte);
        StrokesNetwork network = new StrokesNetwork(arcs);
        HashSet<String> attributeNames = new HashSet<String>();
        attributeNames.add("importance");
        attributeNames.add("nature");
        network.buildStrokes(attributeNames, 112.5, 45.0, true);
        // network.buildStrokes(attributeNames, 70., 90., true);
        return network.getStrokes();
    }

    public static HashMap<Integer, Double> buildRoadSectionStrokeDistance(IFeature ref, Set<Stroke> strokes) {
        HashMap<Integer, Double> idDist = new HashMap<>();
        double lengthRef = ref.getGeom().length();
        for (Stroke s : strokes) {
            double d = ArealDifference.estimate(s.getGeomStroke(), (ILineString) ref.getGeom());
            double score = d / lengthRef;
            // System.out.println("** Stroke "+ s.getId() + " ** " +
            // s.getGeomStroke() + "\nDistance Areal : " + score);
            // System.out.println("Troncons bduni correspondants (" +
            // s.getFeatures().size() + ") :");
            for (ArcReseau a : s.getFeatures()) {
                int idBduni = a.getId();
                idDist.put(idBduni, score);
            }
        }
        return idDist;
    }

    public static double mapmatcherDist(Map<Integer, Set<Integer>> mapmatchResult, int idBdUni, int idBd250k) {
        double score = 0.9;
        Set<Integer> troncons250k = mapmatchResult.get(idBdUni);
        if (troncons250k.size() == 1 && troncons250k.contains(idBd250k))
            return 0.1;
        if (troncons250k.size() > 1 && troncons250k.contains(idBd250k))
            return 1. - (1. / troncons250k.size());
        return score;
    }

    public static IFeature getFeatureById(IPopulation<IFeature> pop, int id) {
        for (IFeature feat : pop) {
            int fid = Integer.parseInt(feat.getAttribute("ID").toString());
            if (fid == id) {
                System.out.println(id);
                return feat;
            }
        }
        System.out.println("nullos");
        return null;
    }

    public static void main(String[] args) throws IOException {
        String csvFile = "/home/imran/projets/multicriteriamatching/routes_appariement/results.csv";
        // String pathTo250kShape =
        // "/home/imran/projets/multicriteriamatching/routes_appariement/558418_250k.shp";
        String pathTo250kShape = "/home/imran/projets/multicriteriamatching/routes_appariement/routes_250k_alpes.shp";
        // String pathToBdUniShape =
        // "/home/imran/projets/multicriteriamatching/routes_appariement/558418_bduni.shp";
        String pathToBdUniShape = "/home/imran/projets/multicriteriamatching/routes_appariement/routes_bduni_alpes.shp";

        System.out.println("*************** loading networks");
        IPopulation<IFeature> reseau250k = loadShapeToLineStrings(pathTo250kShape);
        IPopulation<IFeature> reseauBdUni = loadShapeToLineStrings(pathToBdUniShape);
        System.out.println("*************** getting mapmatcher results");
        Map<Integer, Set<Integer>> mapmatchRes = MapmatcherResultHelper.buildmapMatcherResult(csvFile);

        // IFeature road250k = reseau250k.get(194);
        // IFeature road250k = getFeatureById(reseau250k, 550141);
        IFeature road250k = getFeatureById(reseau250k, 571971);
        int idRef = Integer.parseInt(road250k.getAttribute("ID").toString());
        IGeometry buff = road250k.getGeom().buffer(100);
        IPopulation<IFeature> selection = new Population<IFeature>();
        selection.addAll(reseauBdUni.select(buff));

        System.out.println("*************** computing strokes");
        Set<Stroke> strokes = buildStrokesNetwork(selection);
        System.out.println("*************** " + strokes.size() + " strokes built");

        System.out.println("*************** computing areal dists");
        System.out.println("around bd250k road id : " + idRef);
        HashMap<Integer, Double> idsDist = buildRoadSectionStrokeDistance(road250k, strokes);
        System.out.println("*************** done for " + idsDist.size() + " sections");

        Set<Integer> idCandidates = new HashSet<>();
        for (Entry<Integer, Double> e : idsDist.entrySet()) {
            int idbdUni = e.getKey();
            double arealDist = e.getValue();
            double mmDist = mapmatcherDist(mapmatchRes, idbdUni, idRef);
            double res = (mmDist + 2 * arealDist) / 3.;
            res = (mmDist + arealDist) / 2;
            System.out.println(idbdUni + " -- " + arealDist + " -- " + mmDist + " -- " + res);
            if (res < 0.06) {
                idCandidates.add(idbdUni);
            }
        }

        for (IFeature f : reseauBdUni) {
            int id = Integer.parseInt(f.getAttribute("ID").toString());
            if (idCandidates.contains(id))
                System.out.println(f.getGeom());
        }

        System.out.println("------------------------------------------------------------------------------");
        for (Stroke s : strokes)
            System.out.println(s.getGeomStroke());

        // try {
        // String shapeName =
        // "/home/imran/projets/multicriteriamatching/routes_appariement/strokes/strokes_all.shp";
        // System.out.println("Writing " + shapeName);
        // createShape(idsStrokes, shapeName);
        // System.out.println("Done");
        // } catch (Exception e) {
        // e.printStackTrace();
        // }
    }

}
