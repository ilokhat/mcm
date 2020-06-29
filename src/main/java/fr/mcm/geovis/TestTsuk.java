package fr.mcm.geovis;

import java.io.BufferedWriter;
import java.io.File;

import Jama.Matrix;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Scanner;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TestTsuk {

        public static void main(String[] args) {
                
                String file_path = "/home/mac/hdd/code/equarissage_network/tsukuba.wkt";
                String file_path_output = "/home/mac/hdd/code/equarissage_network/tsukuba_output.wkt";
                
                System.out.println("------------------------------------------------------------------------");
                System.out.println("Reading network file "+file_path+"...");
                System.out.println("------------------------------------------------------------------------");
                
                Scanner scan = null;
                
                try {
                        scan = new Scanner(new File(file_path));
                } catch (FileNotFoundException e) {
                        e.printStackTrace();
                }
                
                String regex = "\"([^\"]*)\"";
                
                Hashtable<String, Double[]> NODES = new Hashtable<String, Double[]> ();
                ArrayList<String[]> EDGES = new ArrayList<String[]>();
                ArrayList<String> K = new ArrayList<String>();
                
                
                // Header
                scan.nextLine();
                
                double xini;
                double yini;
                double xfin;
                double yfin;
                
                int node_counter = 0;
                int l = 0;
                while(scan.hasNextLine()) {
                        
                        String line = scan.nextLine();
                        
                        Matcher m = Pattern.compile(regex).matcher(line);
                        m.find();
                        String wkt = m.group();
                        
                        StringTokenizer wkt_token = new StringTokenizer(wkt.substring(19, wkt.length()-3), ",");
                        
                        String coords = wkt_token.nextToken(",");
                        StringTokenizer coords_token = new StringTokenizer(coords," ");
                        
                        xini = Double.parseDouble(coords_token.nextToken(" "));
                        yini = Double.parseDouble(coords_token.nextToken(" "));
                        

                        
                        while(wkt_token.hasMoreTokens()) {
                                coords = wkt_token.nextToken(",");
                        }

                        coords_token = new StringTokenizer(coords," ");
                        xfin = Double.parseDouble(coords_token.nextToken(" "));
                        yfin = Double.parseDouble(coords_token.nextToken(" "));
                        
                        m.find();
                        String n_ini = m.group();
                        n_ini = n_ini.substring(1, n_ini.length()-1);
                        
                        m.find();
                        String n_fin = m.group();
                        n_fin = n_fin.substring(1, n_fin.length()-1);
                        System.out.println(l + "/ c " + coords + "-- n_ini "+ n_ini + " n_fin " + n_fin);
                        if (!NODES.containsKey(n_ini)) {
                                Double[] COORDS = {xini, yini, (double) node_counter};
                                NODES.put(n_ini, COORDS);
                                K.add(n_ini);
                                System.out.println("Adding node #"+node_counter+": "+n_ini+" ["+xini+" "+yini+"]");
                                node_counter ++;
                        }
                        if (!NODES.containsKey(n_fin)) {
                                Double[] COORDS = {xfin, yfin, (double) node_counter};
                                NODES.put(n_fin, COORDS);
                                K.add(n_fin);
                                System.out.println("Adding node #"+node_counter+": "+n_fin+" ["+xfin+" "+yfin+"]");
                                node_counter ++;
                        }
                        
                        String[] edge = {n_ini, n_fin};
                        EDGES.add(edge);
                        
                        System.out.println("Adding edge #"+(EDGES.size()-1)+":  "+n_ini+" -> "+n_fin);
                        ++l;
                        
                }
                
                int n = node_counter;
                int m = EDGES.size();
                int f = 2+m-n;
                
                System.out.println("------------------------------------------------------------------------");
                System.out.println("Number of nodes: " + n);
                System.out.println("Number of edges: " + m);
                System.out.println("Estimated number of faces: " + f);
                System.out.println("------------------------------------------------------------------------");
                System.out.println("Solving least squares...");
                System.out.println("------------------------------------------------------------------------");

                
                double[][] Aa = new double[m+2*n][2*n]; 
                double[][] Ba = new double[m+2*n][1]; 
                double[][] Pa = new double[m+2*n][m+2*n]; 
                
                System.out.println("Unknown matrix size:    [" + Aa[0].length + "x1]");
                System.out.println("Design matrix size:     [" + Aa.length + "x" + Aa[0].length + "]");
                System.out.println("Weight matrix size:     [" + Pa.length + "x" + Pa[0].length + "]");
                System.out.println("Obs. matrix size:       [" + Ba.length + "x" + Ba[0].length + "]");
                System.out.println("------------------------------------------------------------------------");
                
                double rsup = Math.sqrt(2)+1;
                double rinf = Math.sqrt(2)-1;
                
                for (int i=0; i<EDGES.size(); i++) {
                        
                        String id_ini = EDGES.get(i)[0];
                        String id_fin = EDGES.get(i)[1];
                        Double[] node_ini = NODES.get(id_ini);
                        Double[] node_fin = NODES.get(id_fin);
                        Integer n_ini = node_ini[2].intValue();
                        Integer n_fin = node_fin[2].intValue();
                        
                        double deltax = Math.abs(node_ini[0]-node_fin[0]);
                        double deltay = Math.abs(node_ini[1]-node_fin[1]);
                        
                        
                        if (deltay > deltax) {
                                Aa[i][n_ini] = +1;
                                Aa[i][n_fin] = -1;
                                Pa[i][i] = 20;
                                
                        }else {
                                Aa[i][n+n_ini] = +1;
                                Aa[i][n+n_fin] = -1;
                                Pa[i][i] = 20;
                        }
                        
                        Ba[i][0] = 0;
                        
                        
                        
                        /*
                        Pa[i][i] = 10;
                        
                        if (deltay > rsup*deltax) {
                                Aa[i][n_ini] = +1;
                                Aa[i][n_fin] = -1;
                                Ba[i][0] = 0;
                                continue;
                        }
                        
                        
                        if (deltay < rinf*deltax) {
                                Aa[i][n+n_ini] = +1;
                                Aa[i][n+n_fin] = -1;
                                Ba[i][0] = 0;
                                continue;
                        }
                        
                        Aa[i][n_ini] = +Math.signum(node_ini[0]-node_fin[0]);
                        Aa[i][n_fin] = -Math.signum(node_ini[0]-node_fin[0]);
                        Aa[i][n+n_ini] = +Math.signum(node_ini[1]-node_fin[1]);
                        Aa[i][n+n_fin] = -Math.signum(node_ini[1]-node_fin[1]);
                        Ba[i][0] = 0;
                        */
                        
                }
                
                for (int i=0; i<n; i++) {
                        
                        Aa[m+i][i] = 1;
                        Aa[m+n+i][n+i] = 1;
                        
                        Ba[m+i][0] = NODES.get(K.get(i))[0];
                        Ba[m+n+i][0] = NODES.get(K.get(i))[1];
                        
                        Pa[m+i][m+i] = 1;
                        Pa[m+n+i][m+n+i] = 1;
                        
                }
                
                
                Matrix A = new Matrix(Aa);
                Matrix B = new Matrix(Ba);
                Matrix P = new Matrix(Pa);

                
                // Solving
                Matrix X = (A.transpose().times(P).times(A)).solve(A.transpose().times(P).times(B));
                
                
                StringBuilder output = new StringBuilder();
                output.append("ID,WKT,SOURCE,TARGET\r\n");

                for (int i=0; i<m; i++) {
                        String id_ini = EDGES.get(i)[0];
                        String id_fin = EDGES.get(i)[1];
                        Double[] node_ini = NODES.get(id_ini);
                        Double[] node_fin = NODES.get(id_fin);
                        Integer n_ini = node_ini[2].intValue();
                        Integer n_fin = node_fin[2].intValue();
                        double xi = X.get(n_ini, 0);
                        double yi = X.get(n+n_ini, 0);
                        double xf = X.get(n_fin, 0);
                        double yf = X.get(n+n_fin, 0);
                        output.append(i+",\"LINESTRING("+xi+" "+yi+","+xf+" "+yf+")\","+id_ini+","+id_fin+"\r\n");
                }
                
                BufferedWriter writer;

                try {
                        writer = new BufferedWriter(new FileWriter(file_path_output));
                        writer.write(output.toString());
                        writer.close();
                } catch (IOException e) {
                        e.printStackTrace();
                }
                
                System.out.println("Solution written in ["+file_path_output+"]");

        }

}