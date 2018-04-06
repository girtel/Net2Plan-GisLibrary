
package com.net2plan.gis.algorithms;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;

import com.jom.DoubleMatrixND;
import com.jom.OptimizationProblem;
import com.net2plan.gis.importer.GisLibrary.Building;
import com.net2plan.gis.importer.GisLibrary.Luminaire;
import com.net2plan.gis.importer.GisLibrary.Cell;
import com.net2plan.gis.importer.GisLibrary.GisLayer;
import com.net2plan.gis.importer.GisLibrary.GisMultilayer;
import com.net2plan.gis.importer.GisLibrary.GisObject;


//import com.net2plan.gui.plugins.networkDesign.topologyPane.TopologyPanel;
import com.net2plan.interfaces.networkDesign.IAlgorithm;
import com.net2plan.interfaces.networkDesign.Net2PlanException;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.networkDesign.Node;
import com.net2plan.internal.SystemUtils;
import com.net2plan.utils.Pair;
import com.net2plan.utils.Triple;

import cern.colt.matrix.tdouble.DoubleFactory1D;
import cern.colt.matrix.tdouble.DoubleFactory2D;
import cern.colt.matrix.tdouble.DoubleMatrix1D;
import cern.colt.matrix.tdouble.DoubleMatrix2D;

import java.io.Serializable;



/** This is a template to be used in the lab work, a starting point for the students to develop their programs
 * 
 */
public class NodeLocation implements IAlgorithm, java.io.Serializable
{
	
	/** The method called by Net2Plan to run the algorithm (when the user presses the "Execute" button)
	 * @param netPlan The input network design. The developed algorithm should modify it: it is the way the new design is returned
	 * @param algorithmParameters Pair name-value for the current value of the input parameters
	 * @param net2planParameters Pair name-value for some general parameters of Net2Plan
	 * @return
	 */
	
	//private List<Node> B = new ArrayList<>();	//Buildings
	private List<Node> L = new ArrayList<>();	//Luminaires
	private List<Node> C = new ArrayList<>();	//Cells

	private void createTopology(NetPlan netPlan, String path_cells/*String path_buildings*/, String path_luminaires){
		System.out.println("creating topology");
		
		/* remove topology */
		netPlan.removeAllAttributes();
		netPlan.removeAllNodes();
		netPlan.removeAllNetworkLayers();
		netPlan.removeAllLinks();
		
		GisMultilayer gml_C = new GisMultilayer("Cartagena");
		List<File> files = new ArrayList<File>();
		//File Edificios = new File(path_buildings);
		File Luminarias = new File(path_luminaires);
		File Cells = new File(path_cells);
		//files.add(Edificios);
		files.add(Luminarias);
		files.add(Cells);
		try {
			gml_C.buildFromGeoJson(gml_C, files);
		} catch (Exception e) {};
		
		Map<Long, GisLayer> layers = gml_C.getLayers();
		//System.out.println(layers.size());
		Iterator<Long> gl_iterator = layers.keySet().iterator();
		while (gl_iterator.hasNext()) {
			Long key = gl_iterator.next();
			GisLayer gl = gml_C.getLayer(key);
			//System.out.println(gl.getName());
			Collection<GisObject> goc = gl.getObjects().values();
			for(GisObject go:goc){
				/*if (gl.isBuildingsLayer()) {
					Building object = (Building) go;
					B.add(netPlan.addNode(object.getCenter().getX(), object.getCenter().getY(),
							"Building_"+String.valueOf(object.getId()), object.getProperties()));
					//System.out.println("Added building "+"Building_"+String.valueOf(object.getId()));
				}else*/ if (gl.isLuminairesLayer()) {
					Luminaire object = (Luminaire) go;
					L.add(netPlan.addNode(object.getPoint().getX(), object.getPoint().getY(),"Luminaire_"+String.valueOf(object.getId()), null));
					//System.out.println("Added luminaire "+"Luminaire_"+String.valueOf(object.getId()));
				}else if (gl.isCellsLayer()) {
					Cell object = (Cell) go;
					//C.add(netPlan.addNode(object.getPoint().getX(), object.getPoint().getY(),"Cell_"+String.valueOf(object.getId()), null));
					//System.out.println("Added luminaire "+"Luminaire_"+String.valueOf(object.getId()));
				}
			}
		}
		
		/*for (Node l_node : L) {
			try {
				l_node.setUrlNodeIcon(netPlan.getNetworkLayerDefault(), new File("data/BaseStation.png").toURL());
			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			for (Node b_node : B) {
				try {
					b_node.setUrlNodeIcon(netPlan.getNetworkLayerDefault(), new File("data/Building.png").toURL());
				} catch (MalformedURLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

		}*/
		
	}
	
	@Override
	public String executeAlgorithm(NetPlan netPlan, Map<String, String> algorithmParameters, Map<String, String> net2planParameters)
	{
	
		/* Typically, you start reading the input parameters */
		final Double Dmax = Double.parseDouble (algorithmParameters.get ("Dmax"))/1000; //Max distance in km
		final Double maxTrafficPerPicoCellMbps = Double.parseDouble (algorithmParameters.get ("maxTrafficPerPicoCellMbps")); //Mbps
		//final String path_buildings = algorithmParameters.get("path_buildings");
		final String path_luminaires = algorithmParameters.get("path_luminaires");
		final String path_cells = algorithmParameters.get("path_cells");
		final String solverLibraryName = algorithmParameters.get("solverLibraryName");
		final Double maxSolverTimeInMinutes = Double.parseDouble (algorithmParameters.get ("maxSolverTimeInMinutes"));
		
		final Double TrafPerUser = Double.parseDouble (algorithmParameters.get ("TrafPerUser")); //Mbps
		final Double percUsersInStreet = Double.parseDouble (algorithmParameters.get ("percUsersInStreet"))/100; //percentage
		final Double percCoverageRatio = Double.parseDouble (algorithmParameters.get ("percCoverageRatio"))/100; //percentage
		final int numInhabitants = Integer.parseInt(algorithmParameters.get ("numInhabitants"));

		createTopology(netPlan, path_luminaires, path_cells);
		
		//final BidiMap<Node,Integer> mapBuilding2Index = getAsBidiIndexMap (B);
		final BidiMap<Node,Integer> mapLuminaire2Index = getAsBidiIndexMap (L);
		final BidiMap<Node,Integer> mapCell2Index = getAsBidiIndexMap (C);
		final BidiMap<Pair<Node,Node>,Integer> mapLink2Index = new DualHashBidiMap<> ();
		
		//final int nB = B.size();	//number of buildings
		final int nL = L.size();	//number of luminaires
		final int nC = C.size();	//number of Cells
		//System.out.println("Number of buildings: "+nB);
		System.out.println("Number of luminaires: "+nL);
		System.out.println("Number of cells: "+nC);
		
		/* Compute the set of links */
		//Map<Node,String> buildingsInCoverage = new HashMap();
		Map<Node,String> luminairesInCoverage = new HashMap();
		Map<Node,String> cellsInCoverage = new HashMap();

	//### ESTO HACERLO SOLAMENTE UNA VEZ ###//	
		//for (Node b : B){
		String path_cellsInCoverage = "data/cellsInCoverage.txt";
		String path_luminairesInCoverage = "data/luminairesInCoverage.txt";
		String path_mapLink2Index = "data/mapLink2Index.txt";
		
		File file_cellsInCoverage = new File(path_cellsInCoverage);
		File file_luminairesInCoverage = new File(path_luminairesInCoverage);
		File file_mapLink2Index = new File(path_mapLink2Index);
		
		if (file_cellsInCoverage.exists() && file_luminairesInCoverage.exists() && file_mapLink2Index.exists()) { // load
			try {
		         FileInputStream fileIn = new FileInputStream(path_cellsInCoverage);
		         ObjectInputStream in = new ObjectInputStream(fileIn);
		         cellsInCoverage = (Map<Node,String>) in.readObject();
		         in.close();
		         fileIn.close();
		         
		         fileIn = new FileInputStream(path_luminairesInCoverage);
		         in = new ObjectInputStream(fileIn);
		         luminairesInCoverage = (Map<Node,String>) in.readObject();
		         in.close();
		         fileIn.close();
		         
		         fileIn = new FileInputStream(path_mapLink2Index);
		         in = new ObjectInputStream(fileIn);
		         mapLink2Index = (BidiMap<Pair<Node,Node>,Integer>) in.readObject();
		         in.close();
		         fileIn.close();
		         
		      } catch (IOException i) {
		         i.printStackTrace();
		      } catch (ClassNotFoundException c) {
		         System.out.println("Employee class not found");
		         c.printStackTrace();
		      }
		} else { // run and save

			for (Node c : C) {
				for (Node l : L) {
					if (netPlan.getNodePairHaversineDistanceInKm(c, l) <= Dmax) {
						cellsInCoverage.put(c, "1");
						luminairesInCoverage.put(l, "1");
						final int e = mapLink2Index.size();
						mapLink2Index.put(Pair.of(c, l), e);
						
						// System.out.println("################## Pair in
						// coverage number: "+e);
					}

				}
			}	
		      try {
		          FileOutputStream fileOut = new FileOutputStream(path_cellsInCoverage);
		          ObjectOutputStream out = new ObjectOutputStream(fileOut);
		          out.writeObject(cellsInCoverage);
		          out.close();
		          fileOut.close();
		          
		          fileOut = new FileOutputStream(path_luminairesInCoverage);
		          out = new ObjectOutputStream(fileOut);
		          out.writeObject(luminairesInCoverage);
		          out.close();
		          fileOut.close();
		          
		          fileOut = new FileOutputStream(path_mapLink2Index);
		          out = new ObjectOutputStream(fileOut);
		          out.writeObject(mapLink2Index);
		          out.close();
		          fileOut.close();
		         
		          System.out.printf("Serialized data are saved");
		       } catch (IOException i) {
		          i.printStackTrace();
		       }
		}
		
		final int E = mapLink2Index.size ();
		System.out.println("Number of potential links in coverage: "+E);
		
		final DoubleMatrix2D z_ec = DoubleFactory2D.sparse.make(E,nC);
		final DoubleMatrix2D z_el = DoubleFactory2D.sparse.make(E,nL);
		for (int e = 0; e < E; e++) {
			/* Retrieve info */
			final Pair<Node, Node> pair = mapLink2Index.getKey(e);
			final Node c = pair.getFirst();
			final int cellIndex = mapCell2Index.get(c);
			final Node l = pair.getSecond();
			final int luminaireIndex = mapLuminaire2Index.get(l);
			z_ec.set (e , cellIndex, 1.0);
			z_el.set (e , luminaireIndex, 1.0);
		}
		
		DoubleMatrix1D t_c = DoubleFactory1D.dense.make (nC , TrafPerUser*percUsersInStreet*numInhabitants/nC);
		
		/* Initialize an array with the demanded traffic for each building */
		/* Create the optimization object */
		OptimizationProblem op = new OptimizationProblem();
		
		/* Add the decision variables */
		op.addDecisionVariable("x_cl" , false , new int [] {nC,nL} , 0 , maxTrafficPerPicoCellMbps*C_lc);
		op.addDecisionVariable("z_l" , true , new int [] {1,nL} , 0 , 1); 
		
		/* Add the input parameters */
		op.setInputParameter("Dmax" , Dmax);
		op.setInputParameter("t_c", t_c, "row");
		op.setInputParameter("z_ec", z_ec);
		op.setInputParameter("z_el", z_el);
		op.setInputParameter("maxTrafficPerPicoCellMbps", maxTrafficPerPicoCellMbps);
		op.setInputParameter("percCoverageRatio", percCoverageRatio);
		
		/* Add the Objective Function */
		op.setObjectiveFunction("minimize", "sum(z_l)"); 
		
		/* Add the contraints */
		op.addConstraint("sum(x_cl,1) <= maxTrafficPerPicoCellMbps * z_l "); //aquí hay que sumar las c para que se quede un vector fila de l
		op.addConstraint("sum(x_cl,2) <= t_c' "); // aquí hay que sumar las l para que se quede un vector columna de c
		op.addConstraint("sum(x_cl) >= percCoverageRatio*sum(t_c)");


		System.out.println(solverLibraryName);
		/* Call the solver to solve the problem */
		op.solve("cplex" ,"solverLibraryName", solverLibraryName , "maxSolverTimeInSeconds", maxSolverTimeInMinutes*60);

/*		//if (!op.solutionIsOptimal()) throw new Net2PlanException ("The solution is not optimal");
		
		
		 TO-DO 
		 Retrieve the optimal solution found 
		//x_e
		//z_l
		final double [] z_l = op.getPrimalSolution("z_l").to1DArray();
		final double [] x_e = op.getPrimalSolution("x_e").to1DArray();
		
		for (int e = 0; e < E; e++) {
			 Retrieve info 
			final Pair<Node, Node> pair = mapLink2Index.getKey(e);
			final Node b = pair.getFirst();
			final int buildingIndex = mapBuilding2Index.get(b);
			final Node l = pair.getSecond();
			final int luminaireIndex = mapLuminaire2Index.get(l);

			if ((z_l[luminaireIndex] == 1) && (x_e[e] > 0)) {
				if (z_eb.get(e, buildingIndex) == 1.0 && z_el.get(e, luminaireIndex) == 1.0) {
					netPlan.addLink(b, l, x_e[e], netPlan.getNodePairHaversineDistanceInKm(b, l), 200000, null);
					l.addTag("HASPICOCELL");
				}
			}
		}
		
		
		 checks 
		DoubleMatrix1D x_edm = DoubleFactory1D.dense.make (x_e.length , 0);
		x_edm.assign(x_e);
		
		if(trafficPerBuilding.zSum() != x_edm.zSum())
			throw new Net2PlanException ("The sum of the traffic per each building must be equal to the sum of the traffic in each link.");
			
		for (Node b : B)
		{
			final int buildingIndex = mapBuilding2Index.get(b);
			if(b.getIncomingLinksAllLayers().isEmpty()){  Has the building incoming links? 
				if(b.getOutgoingLinksTraffic() > X_b.get(buildingIndex)){  Exceeds the building the maximum traffic limit? 
					throw new Net2PlanException ("The outgoing traffic in a building exceeds the maximum limit."); }
			}else{ throw new Net2PlanException ("The buildings has one or more incoming links."); }
		}
		
		for (Node l : L) {
			if (l.getOutgoingLinksAllLayers().isEmpty()) {  Has the luminaire outgoing links? 
				if (l.getIncomingLinksAllLayers().isEmpty()) {  if the luminaire does not have incoming layers... 
					if (l.hasTag("HASPICOCELL")) {  ... it must not have the "HASPICOCELL" tag 
						throw new Net2PlanException ("The luminaire is not tagged correctly. It must not have the \"HASPICOCELL\" tag."); }
				} else {  else the luminaire has incoming links... 
					if (!l.hasTag("HASPICOCELL")) {  ... it must have the tag.
						throw new Net2PlanException ("The luminaire is not tagged correctly. It must have the \"HASPICOCELL\" tag.");}
					if (l.getIncomingLinksTraffic() > maxTrafficPerPicoCellMbps) {  Exceeds the luminaire the maximum traffic limit? 
						throw new Net2PlanException ("The luminaire exceeds the maximum traffic limit.");}
				}
			} else { throw new Net2PlanException ("A luminaire has one or more outgoing links."); }
		}
		
		Double alpha = costPerPicoCell/costPerBlockedMbps;
		// 1- X ALPHA, Y number of luminaires with picocell
		List<Double> z_lL = Arrays.stream(z_l).boxed().collect(Collectors.toList());
		long numberOfConnectedLuminaires = z_lL.stream().filter(n -> n == 1.0).count(); //number of luminaires with picocell
		final Double luminairesRelation = (double) numberOfConnectedLuminaires/ (double) luminairesInCoverage.size();
		
		// 2- X ALPHA, Y number of buildings out of coverage
		DoubleMatrix1D trafficPerBuilding =  multiply(x_e, z_eb.toArray()); // Given traffic by luminaires
		int numberOfConnectedBuildings = 0;
		for(int i=0; i<trafficPerBuilding.size();i++){
			if(trafficPerBuilding.get(i) > 0) numberOfConnectedBuildings++;
		}		
		final Double buildingsRelation = (double) numberOfConnectedBuildings/ (double) buildingsInCoverage.size();
		
		// 3- X ALPHA, Y Mbps not covered
		Double mbpsNotCovered = X_b.zSum() - trafficPerBuilding.zSum();
		
		// 4- X ALPHA, Y relation MbpsNotCovered/MbpsDemanded
		Double relationNotCoveredAndOffered = mbpsNotCovered/X_b.zSum();
		
		 Save in a file to be loaded in Matlab 
		PrintWriter writer;
		System.out.println("Creating data file...");
		try {
			writer = new PrintWriter("graphs/NodeLocation_"+alpha+"_"+DemandedTrafficPerBuilding+".txt", "UTF-8");
			writer.println(alpha);
			writer.println(luminairesRelation);
			writer.println(buildingsRelation);
			writer.println(mbpsNotCovered);
			writer.println(relationNotCoveredAndOffered);
			writer.close();
			System.out.println("graphs/NodeLocation_"+alpha+"_"+DemandedTrafficPerBuilding+".txt CREATED");
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/

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

		param.add (Triple.of ("path_cells" , "data/Centroids5.geojson" , "Cells file"));
		param.add (Triple.of ("path_luminaires" , "data/L1.geojson" , "Luminaires file"));
		param.add (Triple.of ("maxTrafficPerPicoCellMbps" , "1024" , "Max Mbps offered by each antenna"));
		param.add (Triple.of ("Dmax" , "50" , "Max coverage distance in m"));
		param.add (Triple.of ("TrafPerUser" , "50" , "Traffic per user in Mbps"));
		param.add (Triple.of ("percUsersInStreet" , "80" , "Peak % users in street"));
		param.add (Triple.of ("percCoverageRatio" , "10" , "Ratio Coverage in %"));
		param.add (Triple.of ("numInhabitants" , "49966" , "Numbers of inhabitants"));
		param.add (Triple.of ("solverLibraryName" , "cplex" , "Solver Library Name"));
		param.add (Triple.of ("maxSolverTimeInMinutes" , "2" , "Max Solver time in minutes"));

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
	
    public static DoubleMatrix1D multiply(double[] a, double[][] b) {
        int columns_a = a.length;
        int rows_b = b.length; //rows
        int columns_b = b[0].length; //columns
        if (columns_a != rows_b) throw new RuntimeException("Illegal matrix dimensions.");
        double[] c = new double[columns_b];
        for (int i = 0; i < columns_b; i++)
            for (int j = 0; j < columns_a; j++)
                    c[i] += a[j] * b[j][i];
        DoubleMatrix1D dm1d = DoubleFactory1D.dense.make (c.length , 0);
        dm1d.assign(c);
        return dm1d;
    }

	private static <S> BidiMap<S,Integer> getAsBidiIndexMap (List<S> list)
	{
		final BidiMap<S,Integer> res = new DualHashBidiMap<> ();
		for (S element : list)
			res.put (element , res.size ());
		return res;
	}
}
