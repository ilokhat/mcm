package fr.mcm.geovis;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import fr.ign.cogit.appariement.AppariementDST;
import fr.ign.cogit.cartagen.spatialanalysis.network.Stroke;
import fr.ign.cogit.criteria.Critere;
import fr.ign.cogit.dao.LigneResultat;
import fr.ign.cogit.geoxygene.api.feature.IFeature;
import fr.ign.cogit.geoxygene.api.feature.IPopulation;
import fr.ign.cogit.geoxygene.api.spatial.geomroot.IGeometry;
import fr.ign.cogit.geoxygene.feature.Population;
import fr.ign.cogit.geoxygene.generalisation.simplification.SimplificationAlgorithm.resultatSuppressionCoteLigne;
import fr.mcm.geovis.criteres.CritereMapMatching;
import fr.mcm.geovis.criteres.CritereStroke;
import fr.mcm.geovis.criteres.CritereSubline;
import fr.mcm.geovis.distances.DistanceMapMatching;
import fr.mcm.geovis.distances.DistanceStrokeAreal;
import fr.mcm.geovis.distances.DistanceSubLineProj;
import fr.mcm.geovis.utils.FeaturesHelper;
import fr.mcm.geovis.utils.MapmatcherResultHelper;
import fr.mcm.geovis.utils.StrokesHelper;

public class MultiCriteriaGeneralizedLineStrings {

    public static void writeCsvfromAllgeoms(Map<Integer, List<String>> geomsPer250kId, String path) {
        String csvContent = "";
        int bid = 0;
        for (Entry<Integer, List<String>> kv : geomsPer250kId.entrySet()) {
            String s = "";
            int id = kv.getKey();
            for (String ss : kv.getValue()) {
                s += bid++ + ";" + id + ";" + ss + "\n";
            }
            csvContent += s;
        }
        try {
            Files.write(Paths.get(path), csvContent.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void writeCsvFromResults(Map<Integer, List<LigneResultat>> resultsPerId, String path) {
        String csvContent = "";
        for (Entry<Integer, List<LigneResultat>> kv : resultsPerId.entrySet()) {
            String s = "";
            int id250k = kv.getKey();
            for (LigneResultat res : kv.getValue()) {
                int idComp = Integer.parseInt(res.getIdTopoComp());
                String geom = res.getGeomComp().toString();
                double[] scores = Arrays.copyOf(res.getDistances(), res.getDistances().length + 1);
                scores[scores.length - 1] = res.getProbaPignistiquePremier();
                s += idComp + ";" + id250k + ";" + res.getProbaPignistiquePremier() + ";" + geom + "\n";
            }
            csvContent += s;
        }
        try {
            Files.write(Paths.get(path), csvContent.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static void printGeoms(Map<Integer, List<LigneResultat>> resultsPerId, int id) {
        List<LigneResultat> ligne = resultsPerId.get(id);
        System.out.println("****************** " + id + " ******************");
        for (LigneResultat res : ligne)
            System.out.println(res.getGeomComp());
        System.out.println("*************************************************");
    }

    public static void main(String[] args) throws Exception {
        final double BUFFER_SIZE = 150;
        //aussi dispo dans src/main/resources/results.csv
        String csvFile = "/home/imran/projets/multicriteriamatching/routes_appariement/results.csv"; 
        // String pathTo250kShape =
        // "/home/imran/projets/multicriteriamatching/routes_appariement/558317_splitted_250k.shp";
        String pathTo250kShape = "/home/imran/projets/multicriteriamatching/routes_appariement/routes_250k_alpes.shp";
        String pathToBdUniShape = "/home/imran/projets/multicriteriamatching/routes_appariement/routes_bduni_alpes.shp";

        System.out.println("*************** loading networks");
        IPopulation<IFeature> reseau250k = FeaturesHelper.loadShapeToLineStrings(pathTo250kShape);
        IPopulation<IFeature> reseauBdUni = FeaturesHelper.loadShapeToLineStrings(pathToBdUniShape);
        System.out.println("*************** getting mapmatcher results");

        // Getting precomputed results from MapMatching between bduni and 250k
        // lines
        Map<Integer, Set<Integer>> mapmatchRes = null;
        try {
            mapmatchRes = MapmatcherResultHelper.buildmapMatcherResult(csvFile);
        } catch (IOException e) {
            e.printStackTrace();
        }

        AppariementDST evidenceAlgoFusionCritere = new AppariementDST();
        evidenceAlgoFusionCritere.setSeuilIndecision(0.001);
        MyObj ref250k = new MyObj("ID", "ID", "SYMBOLISAT", "NIVEAU");
        MyObj candBdUni = new MyObj("ID", "ID", "NATURE", "IMPORTANCE");
        evidenceAlgoFusionCritere.setMetadata(ref250k, candBdUni);

        List<Critere> listCriteres = new ArrayList<>();
        // Critere MapMatching
        DistanceMapMatching dm = new DistanceMapMatching();
        dm.setResultMap(mapmatchRes);
        CritereMapMatching cm = new CritereMapMatching(dm);
        cm.setMetadata(ref250k, candBdUni);
        listCriteres.add(cm);

        // Critere ArealDistance
        DistanceStrokeAreal ds = new DistanceStrokeAreal();
        CritereStroke cs = new CritereStroke(ds);
        cs.setMetadata(ref250k, candBdUni);
        listCriteres.add(cs);

        // Critere SubLine
        DistanceSubLineProj dp = new DistanceSubLineProj();
        CritereSubline csb = new CritereSubline(dp);
        listCriteres.add(csb);

        evidenceAlgoFusionCritere.setListCritere(listCriteres);
        Set<Integer> listIds = FeaturesHelper.getAllIds(reseau250k);
        Map<Integer, List<String>> geomsPer250kId = new HashMap<>();
        Map<Integer, List<LigneResultat>> resultsPerId = new HashMap<>();
        int idx_250k = 0;
        int selectedId = 440383; // 254734 516474 545694 553184 278901 252027
                                 // 205446 459543
        for (int id250k : listIds) {
//            if (id250k != selectedId) // if (id250k == 18621 || id250k == 54914) //176930
//                continue;
            selectedId = id250k;
            List<String> geoms = new ArrayList<>();
            // IFeature ref = reseau250k.get(idx_250k);
            IFeature ref = FeaturesHelper.getFeatureById(reseau250k, id250k);
            System.out.println(
                    "-----------------------------------------------------------------------------------------------------");
            System.out.println("computing for bd 250k ref " + id250k);

            IGeometry buff = ref.getGeom().buffer(BUFFER_SIZE);
            IPopulation<IFeature> selection = new Population<>();
            selection.addAll(reseauBdUni.select(buff));

            // Building Strokes areal distance for this buffer
            // Map<Integer, Double> strokesDistResults =
            // StrokesHelper.getArealStrokeDists(ref, selection);
            Map<Integer, Double> strokesDistResults = StrokesHelper.getArealStrokeDists(ref, selection); //getArealNaturalRoadDists(ref, selection);
            ds.setStrokesDistResults(strokesDistResults);

            List<LigneResultat> resultForThisRef = new ArrayList<>();
            for (IFeature f : selection) {
                IPopulation<IFeature> currentTroncon = new Population<IFeature>();
                currentTroncon.add(f);
                List<LigneResultat> lres = evidenceAlgoFusionCritere.appariementObjet(ref, currentTroncon);
                for (LigneResultat res : lres) {
                    if (res.isDecision() == "true" && res.getIdTopoComp() != "NA") {
                        resultForThisRef.add(res);
                    }

                }
            }
            resultsPerId.put(id250k, resultForThisRef);
            // System.out.println("********************************************************************");
            // for (Stroke s : StrokesHelper.buildStrokesNetwork(selection))
            // System.out.println(s.getGeomStroke().toString());
            // System.out.println("********************************************************************");
            geomsPer250kId.put(id250k, geoms);
            System.out.println(++idx_250k + "/" + reseau250k.size() + " All things done for " + id250k + " -- "
                    + resultForThisRef.size() + " matches/" + selection.size());
            System.out.println("********************************************************************");
            printGeoms(resultsPerId, selectedId);
            System.out.println("********************************************************************");
        }
        String csvOut = "/home/imran/projets/multicriteriamatching/routes_appariement/res3c_wkts.csv";
        // writeCsvfromAllgeoms(geomsPer250kId, csvOut);
        //writeCsvFromResults(resultsPerId, csvOut);
        //printGeoms(resultsPerId, selectedId);
    }
}
