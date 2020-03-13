package fr.mcm.geovis.utils;

import java.util.Set;
import java.util.TreeSet;

import fr.ign.cogit.geoxygene.api.feature.IFeature;
import fr.ign.cogit.geoxygene.api.feature.IPopulation;
import fr.ign.cogit.geoxygene.api.spatial.geomprim.IOrientableCurve;
import fr.ign.cogit.geoxygene.spatial.coordgeom.GM_LineString;
import fr.ign.cogit.geoxygene.spatial.geomaggr.GM_MultiCurve;
import fr.ign.cogit.geoxygene.util.conversion.ShapefileReader;

public class FeaturesHelper {
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

    public static Set<Integer> getAllIds(IPopulation<IFeature> pop) {
        Set<Integer> listIds = new TreeSet<>();
        for (IFeature feat : pop) {
            int fid = Integer.parseInt(feat.getAttribute("ID").toString());
            listIds.add(fid);
        }
        System.out.println("++++++++++++++++++++++++++++++ " + pop.size() + "/" + listIds.size());
        // for (int e: listIds)
        // System.out.println(e);
        return listIds;
    }

}
