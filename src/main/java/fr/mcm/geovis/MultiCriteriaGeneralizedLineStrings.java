package fr.mcm.geovis;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import fr.ign.cogit.appariement.AppariementDST;
import fr.ign.cogit.criteria.Critere;
import fr.ign.cogit.dao.LigneResultat;
import fr.ign.cogit.geoxygene.api.feature.IFeature;
import fr.ign.cogit.geoxygene.api.feature.IPopulation;
import fr.ign.cogit.geoxygene.api.spatial.geomroot.IGeometry;
import fr.ign.cogit.geoxygene.contrib.cartetopo.Arc;
import fr.ign.cogit.geoxygene.contrib.cartetopo.CarteTopo;
import fr.ign.cogit.geoxygene.contrib.cartetopo.CarteTopoFactory;
import fr.ign.cogit.geoxygene.feature.Population;
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
        String csvContent = "id;id_250k;prob;geom\n";
        int nb_lignes = 0;
        for (Entry<Integer, List<LigneResultat>> kv : resultsPerId.entrySet()) {
            String s = "";
            int id250k = kv.getKey();
            for (LigneResultat res : kv.getValue()) {
                int idComp = Integer.parseInt(res.getIdTopoComp());
                String geom = res.getGeomComp().toString();
                double[] scores = Arrays.copyOf(res.getDistances(), res.getDistances().length + 1);
                scores[scores.length - 1] = res.getProbaPignistiquePremier();
                s += idComp + ";" + id250k + ";" + res.getProbaPignistiquePremier() + ";" + geom + "\n";
                ++nb_lignes;
            }
            csvContent += s;
        }
        try {
            Files.write(Paths.get(path), csvContent.getBytes());
            System.out.println(nb_lignes + " lines written in " + path);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

	public static void printGeoms(Map<Integer, List<LigneResultat>> resultsPerId, int id) {
		List<LigneResultat> ligne = resultsPerId.get(id);
		System.out.println("****************** " + id + " ******************");
		if (ligne != null)
			for (LigneResultat res : ligne)
				System.out.println(res.getGeomComp());
		System.out.println("*************************************************");
	}

    public static Map<Integer, List<Integer>[]> idsVoisins(IPopulation<IFeature> selection) {
		Map<Integer, List<Integer>> ids2Voisins = new HashMap<>();
		Map<Integer, List<Integer>[]> voisinsIniFin = new HashMap<>();
        CarteTopo carte = CarteTopoFactory.newCarteTopo(selection);
        carte.creeNoeudsManquants(0.5);
        carte.fusionNoeuds(0.5);
        for (Arc c: carte.getListeArcs()) {
        	int id = Integer.parseInt(c.getCorrespondant(0).getAttribute("ID").toString());
        	List<Arc> arcsNoeudsFin = new ArrayList<>();
        	List<Arc> arcsNoeudsIni = new ArrayList<>();
        	arcsNoeudsFin.addAll(c.getNoeudFin().getEntrants());
        	arcsNoeudsFin.addAll(c.getNoeudFin().getSortants());
        	arcsNoeudsIni.addAll(c.getNoeudIni().getEntrants());
        	arcsNoeudsIni.addAll(c.getNoeudIni().getSortants());
        	List<Integer> idsIni = new ArrayList<>();
        	for (Arc an: arcsNoeudsIni) {
        		int idv = Integer.parseInt(an.getCorrespondant(0).getAttribute("ID").toString());
        		if (idv != id)
        			idsIni.add(idv);
        	}
        	List<Integer> idsFin = new ArrayList<>();
        	for (Arc an: arcsNoeudsFin) {
        		int idv = Integer.parseInt(an.getCorrespondant(0).getAttribute("ID").toString());
        		if (idv != id)
        			idsFin.add(idv);
        	}
        	List<Integer>[] iniFin = new List[2];
        	iniFin[0] = idsIni;
        	iniFin[1] = idsFin;

        	List<Arc> arcsNoeuds = new ArrayList<>();
        	arcsNoeuds.addAll(c.getNoeudFin().getEntrants());
        	arcsNoeuds.addAll(c.getNoeudFin().getSortants());
        	arcsNoeuds.addAll(c.getNoeudIni().getEntrants());
        	arcsNoeuds.addAll(c.getNoeudIni().getSortants());
        	List<Integer> idsv = new ArrayList<>();
        	for (Arc an: arcsNoeuds) {
        		int idv = Integer.parseInt(an.getCorrespondant(0).getAttribute("ID").toString());
        		if (idv != id)
        			idsv.add(idv);
        	}
       		ids2Voisins.put(id,  idsv);
       		voisinsIniFin.put(id, iniFin);
        }
        return voisinsIniFin;
	}
    
    public static AppariementDST createAppariement(IFeature ref, IPopulation<IFeature> selection, Map<Integer, Set<Integer>> mapmatchRes) {
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
        Map<Integer, Double> strokesDistResults = StrokesHelper.getArealStrokeDists(ref, selection);
        ds.setStrokesDistResults(strokesDistResults);
        cs.setMetadata(ref250k, candBdUni);
        listCriteres.add(cs);

        // Critere SubLine
        DistanceSubLineProj dp = new DistanceSubLineProj();
        CritereSubline csb = new CritereSubline(dp);
        listCriteres.add(csb);
        
        evidenceAlgoFusionCritere.setListCritere(listCriteres);
        return evidenceAlgoFusionCritere;
    }


    public static void main(String[] args) throws Exception {
        final double BUFFER_SIZE = 150;
        //csv des resultats du mapmatching
        String csvMapMatching = "/home/mac/hdd/code/multicriteriamatching/test_create/results.csv";
        //réseau sous-jacent
        String pathTo250kShape = "/home/mac/hdd/code/multicriteriamatching/test_create/routes_250k_alpes.shp";
        // réseau plus détaillé qu'on cherche à apparier au réseau sous-jacent
        String pathToBdUniShape = "/home/mac/hdd/code/multicriteriamatching/test_create/routes_bduni_alpes.shp";
        // csv du résultat
        String csvOut = "/home/mac/hdd/code/multicriteriamatching/test_create/results3pass.csv";
        
        Instant debut = Instant.now();
        System.out.println("*************** loading networks");
        IPopulation<IFeature> reseau250k = FeaturesHelper.loadShapeToLineStrings(pathTo250kShape);
        IPopulation<IFeature> reseauBdUni = FeaturesHelper.loadShapeToLineStrings(pathToBdUniShape);
        System.out.println("*************** getting mapmatcher results");

        // Getting precomputed results from MapMatching between bduni and 250k
        // lines
        Map<Integer, Set<Integer>> mapmatchRes = null;
        try {
            mapmatchRes = MapmatcherResultHelper.buildmapMatcherResult(csvMapMatching);
        } catch (IOException e) {
            e.printStackTrace();
        }

        Set<Integer> listIds = FeaturesHelper.getAllIds(reseau250k);
        Map<Integer, List<LigneResultat>> resultsPerId = new HashMap<>();
        int idx_250k = 0;
        // int selectedId = 521297 ; 
        for (int id250k : listIds) {
            IFeature ref = FeaturesHelper.getFeatureById(reseau250k, id250k);
            IGeometry buff = ref.getGeom().buffer(BUFFER_SIZE);
            IPopulation<IFeature> selection = new Population<>();
            selection.addAll(reseauBdUni.select(buff));

            System.out.println("----------------------------------------------------------------------------------");
            System.out.println("computing for bd 250k ref " + id250k + " -- "+ ++idx_250k + "/" + reseau250k.size());
            System.out.println();
                        
            AppariementDST evidenceAlgoFusionCritere = createAppariement(ref, selection, mapmatchRes);

            List<LigneResultat> resultForThisRef = new ArrayList<>();
            for (IFeature f : selection) {
                IPopulation<IFeature> currentTroncon = new Population<IFeature>();
                currentTroncon.add(f);
                List<LigneResultat> lres = evidenceAlgoFusionCritere.appariementObjet(ref, currentTroncon);
                for (LigneResultat res : lres) {
                    //if (res.isDecision() == "true" && res.getIdTopoComp() != "NA") {
                    if (res.getIdTopoComp() != "NA") {
                        resultForThisRef.add(res);
                    }
                }
            }
            Map<Integer, List<Integer>[]> voisinage = idsVoisins(selection);
            System.out.println("********************************************************************");
            System.out.println("Pass 2 - Suppression troncons isoles ");
            List<LigneResultat> toremove = new ArrayList<>();
            for (LigneResultat res :resultForThisRef) {
            	if (res.isDecision() == "true") {
                    int id = Integer.parseInt(res.getIdTopoComp());
                    List<Integer> v1 = voisinage.get(id)[0];
                    int c1 = 0;
                    for (int idv: v1)
                    	for (LigneResultat l : resultForThisRef)
                    		if (Integer.parseInt(l.getIdTopoComp()) == idv && l.getProbaPignistiquePremier() >= 0.5)
                    			++c1;
                    
                    List<Integer> v2 = voisinage.get(id)[1];
                    int c2 = 0;
                    for (int idv: v2)
                    	for (LigneResultat l : resultForThisRef)
                    		if (Integer.parseInt(l.getIdTopoComp()) == idv && l.getProbaPignistiquePremier() >= 0.5)
                    			++c2;
                    if (c1 == 0 && c2 ==0) {
                    	toremove.add(res);
                    }
            	}
            }
            resultForThisRef.removeAll(toremove);
            System.out.println("removed " + toremove.size());
            System.out.println("********************************************************************");
            System.out.println("Pass 3 - Repechage troncons in-between");
            List<LigneResultat> toadd = new ArrayList<>();
            for (LigneResultat res :resultForThisRef) {
            	if (res.isDecision() != "true") {
                    int id = Integer.parseInt(res.getIdTopoComp());
                    List<Integer> v1 = voisinage.get(id)[0];
                    int c1 = 0;
                    for (int idv: v1)
                    	for (LigneResultat l : resultForThisRef)
                    		if (Integer.parseInt(l.getIdTopoComp()) == idv && l.getProbaPignistiquePremier() >= 0.5)
                    			++c1;                  
                    List<Integer> v2 = voisinage.get(id)[1];
                    int c2 = 0;
                    for (int idv: v2)
                    	for (LigneResultat l : resultForThisRef)
                    		if (Integer.parseInt(l.getIdTopoComp()) == idv && l.getProbaPignistiquePremier() >= 0.5)
                    			++c2;
                    if (c1 >= 1 && c2 >=1 && res.getProbaPignistiquePremier() > 0.45)
                    	toadd.add(res);
            	}
            }
            System.out.println("reincluded " + toadd.size());
            System.out.println("********************************************************************");
			for (LigneResultat res: resultForThisRef) {
                if (res.isDecision() == "true")
                	toadd.add(res);
                	//System.out.println(res.getGeomComp());
            }
			resultsPerId.put(id250k, toadd);
            System.out.println(" All things done for " + id250k + " -- " + toadd.size() + " matches/" + selection.size());
            // printGeoms(resultsPerId, id250k);
        }
        Instant fin = Instant.now();
        Duration duration = Duration.between(debut, fin);
        System.out.println();
        System.out.println("********************************************************************");
        System.out.println("********************************************************************");
        System.out.println("Computed in " + duration.toMinutes() + " minutes (" + duration + ")");
        writeCsvFromResults(resultsPerId, csvOut);
    }
}
