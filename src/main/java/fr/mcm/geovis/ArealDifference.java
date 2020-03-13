package fr.mcm.geovis;

import java.util.ArrayList;
import java.util.Random;

import fr.ign.cogit.geoxygene.api.spatial.coordgeom.ILineString;
import fr.ign.cogit.geoxygene.spatial.coordgeom.DirectPositionList;


//------------------------------------------------------------------------------------------------
// Monte Carlo implementation of areal distance
// Input : two line strings L1 and L2
// Output : areal distance between line strings 
// ------------------------------------------------------------------------------------------------
// Areal distance is defined as the area "between" lines divided by the length of the shortest line.
// The two linestrings should globally be oriented in the same direction to be able to define a 
// consistent polygon between L1 and L2. 
// ------------------------------------------------------------------------------------------------
// Also, note that in the classical definition of areal distance the surface is divided by the 
// longest length or by the average of lengths of L1 and L2. In order to keep mathematical distance 
// properties (at least for the restricted set of instances where L1 and L2 start and end in same 
// points), this definition has been modified by dividing the surface by the shortest length. The 
// original definition A may be easily obtained by :
//
//      A = d*min(|L1|,|L2|)/max(|L1|,|L2|)
//
//where d is the result returned by the following function.
// ------------------------------------------------------------------------------------------------

public class ArealDifference {


	// Sampling points number
	private static int N = 100000;

	// Grid resolution
	private static double grid_cells_number = 100; //700 

	//Get frame
	private static double xmin = Double.MAX_VALUE;
	private static double ymin = Double.MAX_VALUE;
	private static double xmax = Double.MIN_VALUE;
	private static double ymax = Double.MIN_VALUE;

	private static double dx = 0;
	private static double dy = 0;
	
	// Setters
	public static void setSamplingNumber(int number){N = number;}
	public static void setGridCellNumber(int number){grid_cells_number = number;}

	// --------------------------------------------------------------------------------------------
	// Method to compute areal difference between two polylines with Monte-Carlo
	// --------------------------------------------------------------------------------------------
	// Input : two polylines L1 and L2
	// Output : estimates areal difference between these two polylines
	// --------------------------------------------------------------------------------------------
	// Random sampling is uniform (default 100000 samples). Convergence may be reached earlier 
	// with quasi Monte Carlo methods (for example with a low discrepancy sequencey such as a two
	// dimensional Hamilton sequence generator
	// --------------------------------------------------------------------------------------------
	public static double estimate(ILineString L1, ILineString L2){

		DirectPositionList dpl1 =  (DirectPositionList) L1.coord();
		DirectPositionList dpl2 =  (DirectPositionList) L2.coord();

		double[] X1 = new double[dpl1.size()];
		double[] Y1 = new double[dpl1.size()];

		double[] X2 = new double[dpl2.size()];
		double[] Y2 = new double[dpl2.size()];


		// Get line 1 in (X1,Y1)
		for (int i=0; i<X1.length; i++){

			X1[i] = dpl1.get(i).getX();
			Y1[i] = dpl1.get(i).getY();

		}

		// Get line 2 in (X2,Y2)
		for (int i=0; i<X2.length; i++){

			X2[i] = dpl2.get(i).getX();
			Y2[i] = dpl2.get(i).getY();

		}

		return estimate(X1, Y1, X2, Y2);

	}

	// -----------------------------------------------------
	// Monte Carlo implementation of areal distance
	// -----------------------------------------------------
	public static double estimate(double[] X1, double[] Y1, double[] X2, double[] Y2){


		Random rand = new Random();

		// Spatial index
		ArrayList<Double> GRID_X = new ArrayList<Double>();
		ArrayList<Double> GRID_Y = new ArrayList<Double>();


		// Adapt frame to L1
		for (int i=0; i<X1.length; i++){

			if (X1[i] > xmax){xmax = X1[i];}
			if (X1[i] < xmin){xmin = X1[i];}
			if (Y1[i] > ymax){ymax = Y1[i];}
			if (Y1[i] < ymin){ymin = Y1[i];}

		}

		// Adapt frame to L2
		for (int i=0; i<X2.length; i++){

			if (X2[i] > xmax){xmax = X2[i];}
			if (X2[i] < xmin){xmin = X2[i];}
			if (Y2[i] > ymax){ymax = Y2[i];}
			if (Y2[i] < ymin){ymin = Y2[i];}

		}

		// Resolution
		dx = (xmax-xmin)/grid_cells_number;
		dy = (ymax-ymin)/grid_cells_number;


		// -------------------------------------------------
		// Lines fusion
		// -------------------------------------------------
		double[] Xt = new double[X1.length + X2.length + 1];
		double[] Yt = new double[Y1.length + Y2.length + 1];

		// Merging L1
		for (int i=0; i<X1.length; i++){

			Xt[i] = X1[i];
			Yt[i] = Y1[i];

		}

		// Merging L2
		for (int i=0; i<X2.length; i++){

			Xt[i+X1.length] = X2[X2.length-1-i];
			Yt[i+Y1.length] = Y2[Y2.length-1-i];

		}

		Xt[Xt.length-1] = X1[0];
		Yt[Yt.length-1] = Y1[0];

		// -------------------------------------------------
		// Compute grid
		// -------------------------------------------------
		for (double x=xmin-dx/2.0; x<=xmax+dx; x+=dx){

			for (double y=ymin-dy/2.0; y<=ymax; y+=dy){

				double[] Xc = {x, x+dx, x+dx, x, x};
				double[] Yc = {y, y, y+dy, y+dy, y};

				if (intersects(Xc, Yc, X1, Y1) || intersects(Xc, Yc, X2, Y2) || (inside(x, y, Xt, Yt))){

					GRID_X.add(x);
					GRID_Y.add(y);

				}

			}

		}



		// -------------------------------------------------
		// Sampling points
		// -------------------------------------------------

		// Points inside
		int V = 0;

		// Sampling a cell
		for (int i=0; i<N; i++){
                    /** hopefully  this will not affect anything **/
                    if (GRID_X.isEmpty() || GRID_Y.isEmpty())
                        break;



			int cell = (int)(Math.floor(rand.nextDouble()*GRID_X.size()));
			
			// Sampling point
			double x = GRID_X.get(cell) + rand.nextDouble()*dx;
			double y = GRID_Y.get(cell) + rand.nextDouble()*dy;

			// ---------------------------------------------
			// Test inclusion
			// ---------------------------------------------
			if (inside(x, y, Xt, Yt)){

				V++;

			}

		}		

		// Sampling area surface
		double sampling_area = GRID_X.size()*dx*dy;

		// Inside points rate
		double f = (double)(V)/(double)(N);

		// Area approximation
		double area = f*sampling_area;

		// -------------------------------------------------
		// Lines length
		// -------------------------------------------------
		double length1 = 0;
		double length2 = 0;

		for (int i=0; i<X1.length-1; i++){

			length1 += Math.sqrt((X1[i+1]-X1[i])*(X1[i+1]-X1[i])+(Y1[i+1]-Y1[i])*(Y1[i+1]-Y1[i]));

		}

		for (int i=0; i<X2.length-1; i++){

			length2 += Math.sqrt((X2[i+1]-X2[i])*(X2[i+1]-X2[i])+(Y2[i+1]-Y2[i])*(Y2[i+1]-Y2[i]));

		}

		// Areal distance
		double distance = area/Math.min(length1, length2);


		return distance;

	}


	// -----------------------------------------------------
	// Fonction equation cartesienne
	// Entree : segment
	// Sortie : liste de parametres (a,b,c)
	// -----------------------------------------------------
	private static double[] cartesienne(double[] segment){

		double x1 = segment[0];
		double y1 = segment[1];
		double x2 = segment[2];
		double y2 = segment[3];

		double u1 = x2-x1;
		double u2 = y2-y1;

		double b = -u1;
		double a = u2;

		double c = -(a*x1+b*y1);

		double[] parametres = {a, b, c};

		return parametres;

	}

	// -----------------------------------------------------
	// Fonction de test d'equation de droite
	// Entrees : paramatres et coords (x,y)
	// Sortie : en particulier 0 si le point 
	// appartient a la droite
	// -----------------------------------------------------
	private static double eval(double[] param, double x, double y){

		double a = param[0];
		double b = param[1];
		double c = param[2];

		return a*x+b*y+c;

	}


	// -----------------------------------------------------
	// Fonction booleenne d'intersection
	// Entrees : segment1 et segment2
	// Sortie : true s'il y a intersection
	// -----------------------------------------------------
	private static boolean  intersects(double[] segment1, double[] segment2){

		double[] param_1 = cartesienne(segment1);
		double[] param_2 = cartesienne(segment2);

		double x11 = segment1[0];
		double y11 = segment1[1];
		double x12 = segment1[2];
		double y12 = segment1[3];

		double x21 = segment2[0];
		double y21 = segment2[1];
		double x22 = segment2[2];
		double y22 = segment2[3];

		double val11 = eval(param_1,x21,y21);
		double val12 = eval(param_1,x22,y22);

		double val21 = eval(param_2,x11,y11);
		double val22 = eval(param_2,x12,y12);

		double val1 = val11*val12;
		double val2 = val21*val22;

		return (val1 <= 0) & (val2 <= 0);

	}

	// -----------------------------------------------------
	// Fonction booleenne d'intersection
	// Entrees : deux polylignes
	// Sortie : true s'il y a intersection
	// -----------------------------------------------------
	private static boolean  intersects(double[] X1, double[] Y1, double[] X2, double[] Y2){

		boolean output = false;

		for (int i=0; i<X1.length-1; i++){

			for (int j=0; j<X2.length-1; j++){

				double[] segment1 = {X1[i], Y1[i], X1[i+1], Y1[i+1]};
				double[] segment2 = {X2[j], Y2[j], X2[j+1], Y2[j+1]};

				if (intersects(segment1, segment2)){

					return true;

				}

			}

		}

		return output;

	}

	// -----------------------------------------------------
	// Fonction de test d'inclusion : point in polygon
	// Entrees : coordonnées (x,y) du point, coordonnées 
	// (X,Y) en tableau du polygone.
	// Sortie : booléen (true si et seulement si inclusion)
	// -----------------------------------------------------
	private static boolean inside(double x, double y, double[] X, double[] Y){

		int count = 0;

		double[] omegax = {xmin, xmax+dx};
		double[] omegay = {ymin, ymax+dy};

		double[] segment_point = {x, y, sample(omegax), sample(omegay)};

		for (int i=0; i<X.length-1; i++){

			double[] segment_poly = {X[i], Y[i], X[i+1], Y[i+1]};

			if (intersects(segment_point, segment_poly)){

				count ++;

			}

		}

		return (count % 2) == 1;

	}


	// -----------------------------------------------------
	// Fonction de choix d'un élément au hasard
	// Entrees : une liste de doubles
	// Sortie : un double tiré au hasard
	// -----------------------------------------------------
	private static double sample(double[] omega){

		int index = (int)(Math.floor(Math.random()*omega.length));

		return omega[index];

	}

}
