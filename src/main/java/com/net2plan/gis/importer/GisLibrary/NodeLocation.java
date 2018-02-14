
package com.net2plan.gis.importer.GisLibrary;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.jom.DoubleMatrixND;
import com.jom.OptimizationProblem;
import com.net2plan.gui.plugins.networkDesign.topologyPane.TopologyPanel;
import com.net2plan.interfaces.networkDesign.IAlgorithm;
import com.net2plan.interfaces.networkDesign.Net2PlanException;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.networkDesign.Node;
import com.net2plan.utils.Triple;

import cern.colt.matrix.tdouble.DoubleFactory2D;
import cern.colt.matrix.tdouble.DoubleMatrix2D;



/** This is a template to be used in the lab work, a starting point for the students to develop their programs
 * 
 */
public class NodeLocation implements IAlgorithm
{
	
	/** The method called by Net2Plan to run the algorithm (when the user presses the "Execute" button)
	 * @param netPlan The input network design. The developed algorithm should modify it: it is the way the new design is returned
	 * @param algorithmParameters Pair name-value for the current value of the input parameters
	 * @param net2planParameters Pair name-value for some general parameters of Net2Plan
	 * @return
	 */
	
	List<Node> B = new ArrayList<Node>();	//Buildings
	List<Node> L = new ArrayList<Node>();	//Luminaires
	
	public void createTopology(NetPlan netPlan){
		
		/* remove topology */
		netPlan.removeAllNetworkLayers();
		netPlan.removeAllNodes();
		netPlan.removeAllLinks();
		
		GisMultilayer gml_C = new GisMultilayer("Cartagena");
		List<File> files = new ArrayList<File>();
		File Edificios = new File("C:/Users/jlrg_/Desktop/UPCT/QGIS/OSM2QGIS/E2.geojson");
		File Luminarias = new File("C:/Users/jlrg_/Desktop/UPCT/QGIS/OSM2QGIS/L1.geojson");
		files.add(Edificios);
		files.add(Luminarias);
		try {
			gml_C.buildFromGeoJson(gml_C, files);
		} catch (Exception e) {};
		
		Map<Long, GisLayer> layers = gml_C.getLayers();
		//System.out.println(layers.size());
		Iterator<Long> gl_iterator = layers.keySet().iterator();
		while (gl_iterator.hasNext()) {
			Long key = gl_iterator.next();
			GisLayer gl = gml_C.getLayer(key);
			System.out.println(gl.getName());
			Collection<GisObject> goc = gl.getObjects().values();
			for(GisObject go:goc){
				if (gl.isBuildingsLayer()) {
					Building object = (Building) go;
					B.add(netPlan.addNode(object.getCenter().getX(), object.getCenter().getY(),
							"Building_"+String.valueOf(object.getId()), object.getProperties()));
					//System.out.println("Added building "+"Building_"+String.valueOf(object.getId()));
				}else if (gl.isLuminairesLayer()) {
					Luminaire object = (Luminaire) go;
					L.add(netPlan.addNode(object.getPoint().getX(), object.getPoint().getY(),"Luminaire_"+String.valueOf(object.getId()), null));
					//System.out.println("Added luminaire "+"Luminaire_"+String.valueOf(object.getId()));
				}
			}
			
		}
		
		for(Node l_node : L)
		{
			l_node.setUrlNodeIcon(netPlan.getNetworkLayerDefault(), TopologyPanel.class.getResource("/resources/gui/figs/BaseStation.png"));
		}
		for(Node b_node : B)
		{
			b_node.setUrlNodeIcon(netPlan.getNetworkLayerDefault(), TopologyPanel.class.getResource("/resources/gui/figs/Building.png"));
		}
		
	}
	
	
	@Override
	public String executeAlgorithm(NetPlan netPlan, Map<String, String> algorithmParameters, Map<String, String> net2planParameters)
	{
		
		createTopology(netPlan);
		//netPlan.removeAllLinks();
		
		/* Typically, you start reading the input parameters */
		final int Dmax = Integer.parseInt (algorithmParameters.get ("Dmax")); //Distancia m�xima de cobertura
		final Double ALPHA = Double.parseDouble (algorithmParameters.get ("ALPHA")); //Factor 
		final Double Z = Double.parseDouble (algorithmParameters.get ("Z")); //Coste de instalaci�n
		final Double X = Double.parseDouble (algorithmParameters.get ("X")); //Mbps
		

		
		///////////////////////////// 1 /////////////////////////////
		/* Create the optimization object */
		OptimizationProblem op = new OptimizationProblem();
		
		///////////////////////////// 2 /////////////////////////////
		
		int nB = B.size();	//number of buildings
		int nL = L.size();	//number of luminaires

		//inicializar vector X_b de tama�o nB y con valores entre 0 y X.
		double[] X_b = new double[nB];
		for(Node b:B){
			X_b[B.indexOf(b)] = 50; 
		}

		
		final DoubleMatrix2D isInCoverageConstraintLimit = DoubleFactory2D.dense.make(nB , nL , X);
		for(Node b:B) // recorro edificios
		{
			for(Node l:L) // recorro luminarias
			{
				//System.out.println(netPlan.getNodePairEuclideanDistance(b,l));
				if(netPlan.getNodePairEuclideanDistance(b,l)> Dmax )
				{
					System.out.println(b.getName() +" is too far from "+ l.getName());
					isInCoverageConstraintLimit.set(B.indexOf(b), L.indexOf(l), 0.0);
				}
			}
		}

		
		
		
		/* Add the decision variables */
		op.addDecisionVariable("x_bl" , false , new int [] {nB,nL} , new DoubleMatrixND (DoubleFactory2D.dense.make(nB , nL)) , new DoubleMatrixND (isInCoverageConstraintLimit)); 
		op.addDecisionVariable("z_l" , true , new int [] {1,nL} , 0 , 1); 
		
		///////////////////////////// 3 /////////////////////////////
		/* Add the input parameters */
		op.setInputParameter("Dmax" , Dmax);
		op.setInputParameter("X_b", X_b, "row");
		op.setInputParameter("ALPHA", ALPHA);
		op.setInputParameter("Z", Z);
		op.setInputParameter("X", X);
										//               coste Mbps perdidos  + coste instalaci�n luminaria
		op.setObjectiveFunction("minimize", "  ALPHA * (sum(X_b)-sum(x_bl,2)) + sum(z_l)"); 
		//" ALPHA * Z * sum( X_b � sum(x_bl) ) + Z * sum(z_l)"
		
		///////////////////////////// 4 /////////////////////////////
		for(Node b:B) // recorro edificios
		{
		op.setInputParameter("b", B.indexOf(b));
		op.addConstraint("sum(x_bl (b,all)) <= X_b(b)");
		}
		
		for(Node l:L) // recorro luminarias
		{
		op.setInputParameter("l", L.indexOf(l));
		op.addConstraint("sum(x_bl (all,l)) <= X*z_l(l)");
		}
		
		/* Call the solver to solve the problem */
		op.solve("cplex");
		if (!op.solutionIsOptimal()) throw new Net2PlanException ("The solution is not optimal");
		/* Retrieve the optimal solution found */
		final double [] z_l = op.getPrimalSolution("z_l").to1DArray();
		final double [][] x_bl = (double [][]) op.getPrimalSolution("x_bl").toArray();
		System.out.println("Building demanded traffic: "+Arrays.toString(X_b));
		System.out.println("Luminaries offered traffic: "+Arrays.toString(z_l));
		System.out.println("X is buildings, Y is luminaries");
		printMatrix(x_bl);
				
		/* Save the access-to-node links in the design (links are not bidirectional) */
		for (Node b : B) 
			for (Node l : L)
				if ((z_l[L.indexOf(l)]==1) && (x_bl[B.indexOf(b)][L.indexOf(l)] > 0))
					netPlan.addLink(b, l, 1, netPlan.getNodePairEuclideanDistance(b,l) , 200000 , null);

		/* Compute the total number of core nodes */
		//int numCoreNodes = 0; for (int n = 0; n < N ; n ++) numCoreNodes += z_j [n];


		return "Ok! total cost: " + op.getOptimalCost(); 
	}

	/** Returns a description message that will be shown in the graphical user interface
	 */
	@Override
	public String getDescription()
	{
		return "GIS Algorithm";
	}

	
	/** Returns the list of input parameters of the algorithm. For each parameter, you should return a Triple with its name, default value and a description
	 * @return
	 */
	@Override
	public List<Triple<String, String, String>> getParameters()
	{
		final List<Triple<String, String, String>> param = new LinkedList<Triple<String, String, String>> ();
		param.add (Triple.of ("Dmax" , "50" , "Max coverage distance"));
		param.add (Triple.of ("ALPHA" , "0.01" , ""));
		param.add (Triple.of ("Z" , "100" , "Installation Cost"));
		param.add (Triple.of ("X" , "200" , "Max Mbps offered by each antenna"));

		return param;
	}
	
	public void printMatrix(double[][] m){
	    try{
	        int rows = m.length;
	        int columns = m[0].length;
	        String str = "|\t";

	        for(int i=0;i<rows;i++){
	            for(int j=0;j<columns;j++){
	                str += m[i][j] + "\t";
	            }

	            System.out.println(str + "|");
	            str = "|\t";
	        }

	    }catch(Exception e){System.out.println("Matrix is empty!!");}
	}
}
