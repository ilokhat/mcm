package fr.mcm.geovis.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import fr.ign.cogit.cartagen.spatialanalysis.network.Stroke;
import fr.ign.cogit.cartagen.spatialanalysis.network.StrokesNetwork;
import fr.ign.cogit.geoxygene.api.feature.IFeature;
import fr.ign.cogit.geoxygene.api.feature.IPopulation;
import fr.ign.cogit.geoxygene.api.spatial.coordgeom.ILineString;
import fr.ign.cogit.geoxygene.api.spatial.geomroot.IGeometry;
import fr.ign.cogit.geoxygene.contrib.cartetopo.Arc;
import fr.ign.cogit.geoxygene.contrib.cartetopo.CarteTopo;
import fr.ign.cogit.geoxygene.contrib.cartetopo.CarteTopoFactory;
import fr.ign.cogit.geoxygene.contrib.cartetopo.Noeud;
import fr.ign.cogit.geoxygene.schemageo.api.support.reseau.ArcReseau;
import fr.ign.cogit.geoxygene.schemageo.api.support.reseau.NoeudReseau;
import fr.ign.cogit.geoxygene.schemageo.impl.support.reseau.NoeudReseauImpl;
import fr.ign.cogit.geoxygene.util.algo.NaturalRoads;
import fr.mcm.geovis.ArealDifference;
import fr.mcm.geovis.reseau.ArcBdUni;

public class StrokesHelper {

    private static HashSet<ArcReseau> buildArcs(CarteTopo carte) {
        HashSet<ArcReseau> arcs = new HashSet<ArcReseau>();
        HashMap<IGeometry, NoeudReseau> geomsNoeuds = new HashMap<IGeometry, NoeudReseau>();

        for (Arc feat : carte.getListeArcs()) {
            int imp = -1;
            List<IFeature> corrs = feat.getCorrespondants(); //.get(0).getAttribute("NATURE").toString();
            if (corrs.size() < 1)
            	continue;
            String nature = corrs.get(0).getAttribute("NATURE").toString();
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
        return network.getStrokes();
    }

    public static Map<Integer, Double> buildRoadSectionStrokeDistance(IFeature ref, Set<Stroke> strokes) {
        Map<Integer, Double> idDist = new HashMap<>();
        Map<Integer, Double> idStrokeLength = new HashMap<>();
        double lengthRef = ref.getGeom().length();
        ILineString refGeom = (ILineString) ref.getGeom();
        for (Stroke s : strokes) {
            double d = ArealDifference.estimate(s.getGeomStroke(), refGeom);
            double score = d / lengthRef;
            // System.out.println("** Stroke "+ s.getId() + " ** " +
            // s.getGeomStroke() + "\nDistance Areal : " + score);
            // System.out.println("Troncons bduni correspondants (" +
            // s.getFeatures().size() + ") :");
            double strokeLength = s.getLength();
            for (ArcReseau a : s.getFeatures()) {
                int idBduni = a.getId();
                if ((idStrokeLength.containsKey(idBduni) && idStrokeLength.get(idBduni) > strokeLength) || !idStrokeLength.containsKey(idBduni)) {
                    idStrokeLength.put(idBduni, strokeLength);
                    idDist.put(idBduni, score);
                }
            }
        }
        return idDist;
    }

    public static Map<Integer, Double> getArealStrokeDists(IFeature ref, IPopulation<IFeature> bdUniSelection) {
        Set<Stroke> strokes = buildStrokesNetwork(bdUniSelection);
        Map<Integer, Double> idsDist = buildRoadSectionStrokeDistance(ref, strokes);
        return idsDist;
    }
    
    
    public static List<ILineString> getRoads(IPopulation<IFeature> bdUniSelection) {
        List<ILineString> roads = new ArrayList<>();
        for (IFeature f : bdUniSelection) {
            roads.add((ILineString) f.getGeom());
        }
        List<ILineString> naturalRoads = (List<ILineString>) NaturalRoads.MakeNaturalRoads(roads, 1, 0.5, 6.14);
        return naturalRoads;
    }
    
    public static List<Set<Integer>> buildNaturalRoads(IPopulation<IFeature> bdUniSelection, List<ILineString> naturalRoads){
        // Map<Integer, Integer> bdUniNaturalRoads = new HashMap<>();
        List<Set<Integer>> bdUniNaturalRoads = new ArrayList<>();
        if (naturalRoads != null) {
            for (int i = 0; i < naturalRoads.size(); ++i) {
                Set<Integer> s = new HashSet<>();
                ILineString r = naturalRoads.get(i);
                for (IFeature f : bdUniSelection) {
                    if (f.getGeom().within(r))
                        s.add(Integer.parseInt(f.getAttribute("ID").toString()));
                }
                bdUniNaturalRoads.add(s);
            }
        }
        return bdUniNaturalRoads;
    }
    
    public static Map<Integer, Double> buildRoadSectionNRDistance(IFeature ref, List<Set<Integer>> nr2bdUni,
            List<ILineString> naturalRoads) {
        Map<Integer, Double> idDist = new HashMap<>();
        ILineString refGeom = (ILineString) ref.getGeom();
        double lengthRef = refGeom.length();
        if (naturalRoads != null) {
            for (int i = 0; i < naturalRoads.size(); ++i) {
                ILineString l = naturalRoads.get(i);
                double d = ArealDifference.estimate(l, refGeom);
                double score = d / lengthRef;
                Set<Integer> idsBduni = nr2bdUni.get(i);
                for (int idbduni : idsBduni)
                    idDist.put(idbduni, score);
            }
        }
        return idDist;
    }
    
    public static Map<Integer, Double> getArealNaturalRoadDists(IFeature ref, IPopulation<IFeature> bdUniSelection) {
        List<ILineString> naturalRoads = getRoads(bdUniSelection);
        List<Set<Integer>> bdUniNaturalRoads = buildNaturalRoads(bdUniSelection, naturalRoads);
        Map<Integer, Double> idsDist = buildRoadSectionNRDistance(ref, bdUniNaturalRoads, naturalRoads);
        return idsDist;
    }

}
