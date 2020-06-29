package fr.mcm.geovis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import fr.ign.cogit.appariement.AppariementDST;
import fr.ign.cogit.cartagen.spatialanalysis.network.Stroke;
import fr.ign.cogit.cartagen.spatialanalysis.network.StrokesNetwork;
import fr.ign.cogit.criteria.Critere;
import fr.ign.cogit.criteria.CritereGeom;
import fr.ign.cogit.dao.LigneResultat;
import fr.ign.cogit.distance.geom.DistanceEuclidienne;
import fr.ign.cogit.distance.text.DistanceSamal;
import fr.ign.cogit.geoxygene.api.feature.IFeature;
import fr.ign.cogit.geoxygene.api.feature.IPopulation;
import fr.ign.cogit.geoxygene.api.spatial.geomprim.IOrientableCurve;
import fr.ign.cogit.geoxygene.api.spatial.geomroot.IGeometry;
import fr.ign.cogit.geoxygene.contrib.cartetopo.Arc;
import fr.ign.cogit.geoxygene.contrib.cartetopo.CarteTopo;
import fr.ign.cogit.geoxygene.contrib.cartetopo.CarteTopoFactory;
import fr.ign.cogit.geoxygene.schemageo.api.support.reseau.ArcReseau;
import fr.ign.cogit.geoxygene.schemageo.api.support.reseau.NoeudReseau;
import fr.ign.cogit.geoxygene.schemageo.impl.support.reseau.ArcReseauImpl;
import fr.ign.cogit.geoxygene.schemageo.impl.support.reseau.NoeudReseauImpl;
import fr.ign.cogit.geoxygene.spatial.coordgeom.GM_LineString;
import fr.ign.cogit.geoxygene.spatial.geomaggr.GM_MultiCurve;
import fr.ign.cogit.geoxygene.util.conversion.ShapefileReader;
import fr.mcm.geovis.criteres.CritereImpNat;
import fr.mcm.geovis.criteres.CritereTopo;
import fr.mcm.geovis.distances.DistanceNatImp;

public class SimpleMapMatcher {
	
	public static String formatResultHeader(LigneResultat res) {
		String dists = "";
		for (int i = 0; i < res.getDistances().length; ++i)
			dists += res.getNomDistance(i) + " - ";
		
		String s = "id ref - id candidat - N° - " + dists + "prob pignistique 1 - prob pignistique 2 - décision";
		return s;
	}
	
	public static String formatResultLine(LigneResultat res) {
		String line = "";
		String distances = "";
		double[] dists = res.getDistances();
		for (double d : dists) {
			distances += d + " - "; 
		}
		line  += res.getIdTopoRef()   + " - " // id ref  
				+ res.getIdTopoComp() + " - " // id candidat 
				+ res.getCompteurC()  + " - " // num candidat
				+ distances
				//+ res.getGeomComp() + "- "
				+ res.getProbaPignistiquePremier() + " - "
				+ res.getProbaPignistiqueSecond()  + " - "
				+ res.isDecision();
		return line;
	}

	public static void main(String[] args) throws Exception {
		
		AppariementDST evidenceAlgoFusionCritere = new AppariementDST();
		evidenceAlgoFusionCritere.setSeuilIndecision(0.001);
		MyObj ref = new MyObj("ID", "", "SYMBOLISAT", "NIVEAU");
		MyObj cand = new MyObj("ID", "", "NATURE", "IMPORTANCE");
		evidenceAlgoFusionCritere.setMetadata(ref, cand);
		
        List<Critere> listCritere = new ArrayList<Critere>();
        // Critere toponymique
        DistanceSamal ds = new DistanceSamal();
        CritereTopo ct = new CritereTopo(ds);
        ct.setMetadata(ref, cand);
        ct.setSeuil(0.6);
        //listCritere.add(ct);
            
        // Critere géométrique
        DistanceEuclidienne dg = new DistanceEuclidienne();
        //DistanceEcartOrientation deo = new DistanceEcartOrientation();
        CritereGeom cg = new CritereGeom(dg);
        cg.setSeuil(2, 10);
        listCritere.add(cg);
        
        // Critere sémantique
//        DistanceWuPalmer dwp = new DistanceWuPalmer();
//        CritereSemantique cs = new CritereSemantique(dwp);
//        cs.setMetadata(ref, cand);
//        cs.setSeuil(0.7);
//        listCritere.add(cs);
        
        // Critere importance
//        DistanceImp di = new DistanceImp();
//        CritereImportance ci = new CritereImportance(di);
//        ci.setMetadata(ref, cand);
        //listCritere.add(ci);
        
        // Critere NatImp
        DistanceNatImp dni = new DistanceNatImp();
        CritereImpNat cni = new CritereImpNat(dni);
        cni.setMetadata(cand);
        listCritere.add(cni);
        
        evidenceAlgoFusionCritere.setListCritere(listCritere);
        //String pathToShapes = "/home/imran/projets/multicriteriamatching/routes_appariement/routes_250k_alpes.shp";
        String pathToShapes = "/home/imran/projets/multicriteriamatching/routes_appariement/558418_250k.shp";
        IPopulation<IFeature> reseauRoutier = ShapefileReader.read(pathToShapes);
        pathToShapes = "/home/imran/projets/multicriteriamatching/routes_appariement/548649_bduni.shp";
        pathToShapes = "/home/imran/projets/multicriteriamatching/routes_appariement/558418_bduni.shp";
        IPopulation<IFeature> reseauSelection = ShapefileReader.read(pathToShapes);
        
        // it does not like multilinestrings..
        for (IFeature f: reseauSelection) {
        	GM_MultiCurve<IOrientableCurve> l = (GM_MultiCurve<IOrientableCurve>) f.getGeom();
        	GM_LineString ll = (GM_LineString) l.get(0);
        	f.setGeom(ll);
        }
        
        //CarteTopo carte = CarteTopoFactory.newCarteTopo("carte topo", reseauSelection, 0.5, true);
        CarteTopo carte = CarteTopoFactory.newCarteTopo(reseauSelection);
        carte.creeNoeudsManquants(0.5);
        carte.fusionNoeuds(0.5);

        HashSet<ArcReseau> arcs = new HashSet<ArcReseau>();
        HashMap<IGeometry, NoeudReseau> geomsNoeuds = new HashMap<IGeometry, NoeudReseau>();

        for (IFeature feat : carte.getListeArcs()) {
        	ArcReseau arc = new ArcReseauImpl();
        	arc.setGeom(feat.getGeom());
        	IGeometry noeudIniGeom = ((Arc)feat).getNoeudIni().getGeom();
        	IGeometry noeudFinGeom = ((Arc)feat).getNoeudFin().getGeom();

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
        
        System.out.println("\nNoeuds ajoutés aux arcs : " + geomsNoeuds.size());
        System.out.println("Noeuds carte topo : " + carte.getListeNoeuds().size());
        StrokesNetwork network = new StrokesNetwork(arcs);
        HashSet<String> attributeNames = new HashSet<String>();
        //network.buildStrokes(attributeNames, 112.5, 45.0, true);
        network.buildStrokes(attributeNames, 70., 90., true);
        System.out.println("strokes " + network.getStrokes().size() + "\n");
        for (Stroke s: network.getStrokes()) {
        	//System.out.println(s + " ");
        	if (s.getLength() > 2000.)
        		System.out.println(s.getGeom());
        }
        System.out.println();
        
        

        //IPopulation<IFeature> reseauSelectionLim = new Population<IFeature>();
        //reseauSelection.setFeatureType(reseauRoutier.getFeatureType());
//        for (int i = 10950; i < 11000; ++i) {
//        	reseauSelection.add(reseauRoutier2.get(i));
//        }


//        IFeature reff = reseauRoutier.get(0);
//        List<String> ids = new ArrayList<>();
//        List<LigneResultat> lres_out = new ArrayList<>();
//        for (int i = 0; i < reseauSelection.size(); ++i) {
//        	IPopulation<IFeature> reseauSelectionLim = new Population<IFeature>();
//        	reseauSelectionLim.add(reseauSelection.get(i));
//        	List<LigneResultat> lres = evidenceAlgoFusionCritere.appariementObjet(reff, reseauSelectionLim);
//        	for (LigneResultat res : lres) {
//        		System.out.println(formatResultLine(res));
//        		if (res.isDecision() == "true" && res.getIdTopoComp() != "NA") {
//        			ids.add(res.getIdTopoComp());
//        			lres_out.add(res);
//        		}
//        		
//        	}
//        }
//        System.out.println(ids.size() + "/ " + reseauSelection.size() + " : " + ids);
//        System.out.println(lres_out.size());
//        System.out.println(formatResultHeader(lres_out.get(0)));
//        
//        for (LigneResultat res: lres_out) {
//        	System.out.println(formatResultLine(res));
//        }
        
        //IFeature reff = reseauRoutier.get(0);
        
//        List<LigneResultat> lres = evidenceAlgoFusionCritere.appariementObjet(reff, reseauSelectionLim);
//        System.out.println(formatResultHeader(lres.get(0)));
//        for (int i = 0; i < lres.size(); ++i) {
//        	LigneResultat res = lres.get(i);
//        	System.out.println(formatResultLine(res));
//        }
//        System.out.println(lres.size() + " " + reseauSelection.size());
//        TableauResultatFrame tableauPanel = new TableauResultatFrame();
//        tableauPanel.displayEnsFrame("tests", lres);
//        int[] tab = tableauPanel.analyse();
//        System.out.println("NB non-app : " + tab[0]);
//        System.out.println("NB app : " + tab[1]);
//        System.out.println("NB d'indécis : " + tab[2]);
//        System.out.println(reseauRoutier.envelope());

	}

}
