
package com.net2plan.gis.algorithms;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.DoubleStream;

import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;

import com.jom.OptimizationProblem;
import com.net2plan.gis.importer.GisLibrary.Cell;
import com.net2plan.gis.importer.GisLibrary.GisLayer;
import com.net2plan.gis.importer.GisLibrary.GisMultilayer;
import com.net2plan.gis.importer.GisLibrary.GisObject;
import com.net2plan.gis.importer.GisLibrary.LTEAntenna;
import com.net2plan.gis.importer.GisLibrary.Luminaire;
//import com.net2plan.gui.plugins.networkDesign.topologyPane.TopologyPanel;
import com.net2plan.interfaces.networkDesign.IAlgorithm;
import com.net2plan.interfaces.networkDesign.Net2PlanException;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.networkDesign.Node;
import com.net2plan.utils.DoubleUtils;
import com.net2plan.utils.Pair;
import com.net2plan.utils.Triple;

import cern.colt.matrix.tdouble.DoubleFactory1D;
import cern.colt.matrix.tdouble.DoubleFactory2D;
import cern.colt.matrix.tdouble.DoubleMatrix1D;
import cern.colt.matrix.tdouble.DoubleMatrix2D;
import java.util.stream.*;


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
	
	//private List<Node> B = new ArrayList<>();	//Buildings
	private List<Node> L = new ArrayList<>();	//Luminaires
	private List<Node> C_grid = new ArrayList<>();	//Grid Cells
	private List<Cell> C_density = new ArrayList<>(); //Density Cells
	private List<Node> LTEAntennas = new ArrayList<>();	//4G

	private void createTopology(NetPlan netPlan, String pathLuminaires/*String path_buildings*/, String pathCells_grid, String pathCells_density, String pathLTEAntennas){
		System.out.println("creating topology");
		
		/* remove topology */
		netPlan.removeAllAttributes();
		netPlan.removeAllNodes();
		netPlan.removeAllNetworkLayers();
		netPlan.removeAllLinks();
		
		final GisMultilayer gml_C = new GisMultilayer("Cartagena");
		gml_C.buildFromGeoJson( Arrays.asList(new File (pathLuminaires) , new File (pathCells_grid), new File (pathCells_density), new File (pathLTEAntennas) ) ); 
		
		System.out.println("Computing the number of luminaires, cells and 4G antennas from files...");
		//System.out.println(layers.size());
		for (GisLayer gl : gml_C.getLayers().values())
		{
			//if (!gl.isLuminairesLayer() && !gl.isCellsLayer()) continue; 
			//System.out.println(gl.getName());
			for(GisObject go : gl.getObjects().values())
			{ 
				if (gl.isLuminairesLayer()) 
				{
					final Luminaire object = (Luminaire) go;
					L.add(netPlan.addNode(object.getPoint().getX(), object.getPoint().getY(),"Luminaire_"+String.valueOf(object.getId()), null));
				}else if (gl.isCellsLayer()) 
				{
					final Cell object = (Cell) go;
					if(object.getProperty("quadrant").isPresent()) C_density.add(object);
					else C_grid.add(netPlan.addNode(object.getPoint().getX(), object.getPoint().getY(),"Cell_grid_"+String.valueOf(object.getId()), null));
				}else if (gl.isLTEAntennasLayer()) 
				{
					final LTEAntenna object = (LTEAntenna) go;
					LTEAntennas.add(netPlan.addNode(object.getPoint().getX(), object.getPoint().getY(),"4GAntenna_"+String.valueOf(object.getId()), null));
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

			for (Node c_node : C) {
				try {
					c_node.setUrlNodeIcon(netPlan.getNetworkLayerDefault(), new File("data/phone1.png").toURL());
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
		final String pathLuminaires = algorithmParameters.get("pathLuminaires");
		final String pathCells_grid = algorithmParameters.get("pathCells_grid");	
		final String pathCells_density = algorithmParameters.get("pathCells_density");
		final String pathLTEAntennas = algorithmParameters.get("pathLTEAntennas");	
		final Double trafPerUser = Double.parseDouble (algorithmParameters.get ("trafPerUser")); //Mbps
		final Double percUsersInStreet = Double.parseDouble (algorithmParameters.get ("percUsersInStreet"))/100; //percentage
		final Double percCoverageRatio = Double.parseDouble (algorithmParameters.get ("percCoverageRatio"))/100; //percentage
		final String solverLibraryName = algorithmParameters.get("solverLibraryName");
		final Double maxSolverTimeInMinutes = Double.parseDouble (algorithmParameters.get ("maxSolverTimeInMinutes"));
		System.out.println("perCoverageRatio: "+percCoverageRatio);

		createTopology(netPlan, pathLuminaires, pathCells_grid, pathCells_density, pathLTEAntennas);
		
		
		
		final BidiMap<Node,Integer> mapLuminaire2Index = getAsBidiIndexMap (L);
		final BidiMap<Node,Integer> mapCell2Index = getAsBidiIndexMap (C_grid);
		final BidiMap<Node,Integer> mapLTE2Index = getAsBidiIndexMap (LTEAntennas);
		final BidiMap<Pair<Node,Node>,Integer> mapLink2Index = new DualHashBidiMap<>();
		
		Map<Node, Integer> mapCellsInCoverage = new HashMap<>();
		Map<Node, Integer> mapLuminairesInCoverage = new HashMap<>();
		
		final int nL = L.size();	//number of luminaires
		final int nC = C_grid.size();	//number of cells
		System.out.println("Number of luminaires: "+nL);
		System.out.println("Number of cells: "+nC);
		System.out.println("Number of density cells: "+C_density.size());
		System.out.println("Number of LTE Antennas: "+LTEAntennas.size());
		System.out.println("Maximum coverage distance: "+Dmax);
		System.out.println("Maximum traffic per user: "+trafPerUser);
		
		for (Node c : C_grid) {
			for (Node l : L) {
				if (netPlan.getNodePairHaversineDistanceInKm(c, l) <= Dmax) {
					final int e = mapLink2Index.size();
					mapLink2Index.put(Pair.of(c, l), e);
					mapCellsInCoverage.put(c, mapCell2Index.get(c));
					mapLuminairesInCoverage.put(l, mapLuminaire2Index.get(l));
				}
			}
		}
		
		final int E = mapLink2Index.size ();
		System.out.println("Number of potential links in coverage: "+E);
		System.out.println("Number of cells in coverage: "+mapCellsInCoverage.size());
		System.out.println("Number of luminaires in coverage: "+mapLuminairesInCoverage.size());
		
		final DoubleMatrix2D z_ec = DoubleFactory2D.sparse.make(E,nC);
		final DoubleMatrix2D z_el = DoubleFactory2D.sparse.make(E,nL);
		System.out.println("Sparse matrix created!");
		
		for (int e = 0; e < E; e++) 
		{
			/* Retrieve info */
			final Pair<Node, Node> pair = mapLink2Index.getKey(e);
			final Node c = pair.getFirst();
			final int cellIndex = mapCell2Index.get(c);
			final Node l = pair.getSecond();
			final int luminaireIndex = mapLuminaire2Index.get(l);
			z_ec.set (e , cellIndex, 1.0);
			z_el.set (e , luminaireIndex, 1.0);
		}
		
		//## HERE WE NEED TO IDENTIFY WHAT CELLS CORRESPOND TO EACH QUADRANT ##		
		double [] t_c = setDensitiesInCells(mapCellsInCoverage, nC, mapLuminairesInCoverage, nL, C_density, trafPerUser, percUsersInStreet);
		System.out.println("t_c vector created");
		
		System.out.println("percCoverageRatio * sum(t_c) = "+ percCoverageRatio * DoubleStream.of(t_c).sum());
		
		/* Initialize an array with the demanded traffic for each building */
		/* Create the optimization object */
		OptimizationProblem op = new OptimizationProblem();
		System.out.println("Optimization problem created");
		
		/* Add the decision variables */
		op.addDecisionVariable("x_e" , false , new int [] {1,E} , 0 , maxTrafficPerPicoCellMbps);
		op.addDecisionVariable("z_l" , true , new int [] {1,nL} , 0 , 1); 
		System.out.println("Decision Variables added");
		
		/* Add the input parameters */
		op.setInputParameter("t_c", t_c, "row");
		op.setInputParameter("z_ec", z_ec); // PABLO: negative index here
		op.setInputParameter("z_el", z_el); // PABLO: negative index here
		op.setInputParameter("maxTrafficPerPicoCellMbps", maxTrafficPerPicoCellMbps);
		op.setInputParameter("percCoverageRatio", percCoverageRatio);
		System.out.println("Input Parameters has been set");
		
		/* Add the Objective Function */
		op.setObjectiveFunction("minimize", "sum(z_l)"); 
		System.out.println("Objective function has been set");
		
		/* Add the constraints */
		// PABLO: Probar traspuesta. Ver codigo JOM
		op.addConstraint("(x_e * z_el) <= ( maxTrafficPerPicoCellMbps * z_l ) "); // Out of memory here
		op.addConstraint("(x_e * z_ec) <= t_c"); // Out of memory here
		op.addConstraint("sum(x_e) >= ( percCoverageRatio * sum(t_c) )"); // Not feasible solution
		System.out.println("Constraints added");

		System.out.println("Calling solver....");
		/* Call the solver to solve the problem */
		op.solve("cplex" ,"solverLibraryName", solverLibraryName , "maxSolverTimeInSeconds", maxSolverTimeInMinutes*60);
		
		if (!op.solutionIsFeasible()) throw new Net2PlanException ("The solver " + solverLibraryName+ " could not find a feasible solution");
		System.out.println("A feasible solution was found. It is guaranteed to be optimal?: " + op.solutionIsOptimal());
		
		
		/* Retrieve info */
		final double [] z_l = op.getPrimalSolution("z_l").to1DArray();
		final double [] x_e = op.getPrimalSolution("x_e").to1DArray();
		
		DoubleMatrix1D lum2LTEAssociations = DoubleFactory1D.dense.make(LTEAntennas.size());
		boolean[] luminaireIsChecked = new boolean[z_l.length];
		Arrays.fill(luminaireIsChecked, false);
		
		for (int e = 0; e < E; e++) 
		{
			/* Retrieve info */
			final Pair<Node, Node> pair = mapLink2Index.getKey(e);
			final Node c = pair.getFirst();
			final int CellIndex = mapCell2Index.get(c);
			final Node l = pair.getSecond();
			final int luminaireIndex = mapLuminaire2Index.get(l);

			double distance = Double.MAX_VALUE;
			int indexLTE = -1;

			if ((z_l[luminaireIndex] == 1) && (x_e[e] > 0))
			{
				if ( (z_ec.get(e, CellIndex) == 1.0) && (z_el.get(e, luminaireIndex) == 1.0) ) 
				{
					netPlan.addLink(c, l, x_e[e], netPlan.getNodePairHaversineDistanceInKm(c, l), 200000, null);
					l.addTag("HASPICOCELL");
					
					if (!luminaireIsChecked[luminaireIndex]) 
					{
						for (Node LTE : LTEAntennas) 
						{
							if (netPlan.getNodePairHaversineDistanceInKm(LTE, l) < distance) 
							{
								distance = netPlan.getNodePairHaversineDistanceInKm(LTE, l);
								indexLTE = mapLTE2Index.get(LTE);
							}
						}
						
						lum2LTEAssociations.set(indexLTE, lum2LTEAssociations.get(indexLTE) + 1);
						luminaireIsChecked[luminaireIndex] = true;
					}
			}else throw new Net2PlanException ("If there is traffic in a pair, z_ec[index] and z_el[index] must be one.");
			}
		}
		
		final double numLuminariesWithAntenna = DoubleUtils.sum(z_l);
		
		/* checks */
		if(lum2LTEAssociations.zSum() != numLuminariesWithAntenna){ throw new Net2PlanException ("The number of luminaires with micro-cell does not match "
				+ "the number of luminaires associated with LTE Antennas: "+numLuminariesWithAntenna+" vs "+lum2LTEAssociations.zSum()); }
		
		for (Node c : C_grid)
		{
			final int CellIndex = mapCell2Index.get(c);
			if(c.getIncomingLinksAllLayers().isEmpty()){ /* Has the cell incoming links? */
				if(c.getOutgoingLinksTraffic() > t_c[CellIndex]){ /* Exceeds the cell the maximum traffic limit? */
					throw new Net2PlanException ("The outgoing traffic in a cell exceeds the maximum limit."); }
			}else{ throw new Net2PlanException ("The cells has one or more incoming links."); }
		}
		
		for (Node l : L) {
			if (l.getOutgoingLinksAllLayers().isEmpty()) { /* Has the luminaire outgoing links? */
				if (l.getIncomingLinksAllLayers().isEmpty()) { /* if the luminaire does not have incoming layers... */
					if (l.hasTag("HASPICOCELL")) { /* ... it must not have the "HASPICOCELL" tag */
						throw new Net2PlanException ("The luminaire is not tagged correctly. It must not have the \"HASPICOCELL\" tag."); }
				} else { /* else the luminaire has incoming links... */
					if (!l.hasTag("HASPICOCELL")) { /* ... it must have the tag.*/
						throw new Net2PlanException ("The luminaire is not tagged correctly. It must have the \"HASPICOCELL\" tag.");}
					if (l.getIncomingLinksTraffic() > maxTrafficPerPicoCellMbps) { /* Exceeds the luminaire the maximum traffic limit? */
						throw new Net2PlanException ("The luminaire exceeds the maximum traffic limit.");}
				}
			} else { throw new Net2PlanException ("A luminaire has one or more outgoing links."); }
		}
		
		// DOCUMENT 1 FORMAT: 1.# MicroCells; 2.# Luminaires in Coverage; 3.# Luminaires; 
		//					  4.# Cells in coverage; 5.# Cells; 6. total traffic carried
		// DOCUMENT 2 FORMAT (MACROCELLS): # microcells in each macrocell. Save only macrocells 
		//									with at least one microcell connected.
		
		/* Save in a file to be loaded in Matlab */
		PrintWriter writer;
		try {
			writer = new PrintWriter("graphs/NodeLocation_"+trafPerUser+"_"+percCoverageRatio+".txt", "UTF-8");
			writer.println(numLuminariesWithAntenna);
			writer.println(mapLuminairesInCoverage.size());
			writer.println(nL);
			writer.println(mapCellsInCoverage.size());
			writer.println(nC);
			writer.println(DoubleUtils.sum(x_e));
			writer.println(DoubleStream.of(t_c).sum());
			writer.close();
			
			writer = new PrintWriter("graphs/NodeLocation_"+trafPerUser+"_"+percCoverageRatio+"_4G"+".txt", "UTF-8");
			for(int i=0; i<lum2LTEAssociations.size();i++)
			{
				if(lum2LTEAssociations.get(i)>0.0)writer.println(lum2LTEAssociations.get(i));
			}
			writer.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	
		return "Ok! total cost: " + numLuminariesWithAntenna; 
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

		param.add (Triple.of ("pathCells_grid" , "data/Centroids20Completed.geojson" , "Grid cells file"));
		param.add (Triple.of ("pathCells_density" , "data/DensityCentroids.geojson" , "Density cells file"));
		param.add (Triple.of ("pathLuminaires" , "data/luminarias-ct-estudio.geojson" , "Luminaires file"));
		param.add (Triple.of ("pathLTEAntennas" , "data/4G.geojson" , "4G Antennas file"));
		param.add (Triple.of ("maxTrafficPerPicoCellMbps" , "1024" , "Max Mbps offered by each antenna"));
		param.add (Triple.of ("Dmax" , "16" , "Max coverage distance in m"));
		param.add (Triple.of ("trafPerUser" , "50" , "Traffic per user in Mbps"));
		param.add (Triple.of ("percUsersInStreet" , "80" , "Peak % users in the street"));
		param.add (Triple.of ("percCoverageRatio" , "10" , "Traffic Coverage in %"));
		param.add (Triple.of ("solverLibraryName" , "cplex" , "Solver Library Name"));
		param.add (Triple.of ("maxSolverTimeInMinutes" , "1" , "Max Solver time in minutes"));

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
	
	private static <S> BidiMap<S,Integer> getAsBidiIndexMap (List<S> list)
	{
		final BidiMap<S,Integer> res = new DualHashBidiMap<> ();
		for (S element : list)
			res.put (element , res.size ());
		return res;
	}
	
	private static double[] setDensitiesInCells(Map<Node, Integer> mapCellsInCoverage, int nC, Map<Node, Integer> mapLuminairesInCoverage, int nL, List<Cell> C_density, double trafPerUser, double percUsersInStreet) 
	{
		
		int[] quadrant_c = new int[nC];
		int[] quadrant_l = new int[nL];
		
		
		double[] t_c = new double[nC];
		Map<Integer, Integer> densityMap = new HashMap<>();

		for(Cell c_density : C_density) {
			
			densityMap.put((int)c_density.getProperty("quadrant").get(), (int)c_density.getProperty("density").get());
			
			System.out.println("############################################################################# ");
			System.out.println("Quadrant"+ (int)c_density.getProperty("quadrant").get()+" density: "+ (int)c_density.getProperty("density").get());
			double c_density_xmin =  (double) c_density.getProperty("xmin").get();
			double c_density_xmax = (double) c_density.getProperty("xmax").get();
			double c_density_ymin = (double) c_density.getProperty("ymin").get();
			double c_density_ymax = (double) c_density.getProperty("ymax").get();
			
			/*System.out.println("c_density_xmin: "+c_density_xmin);
			System.out.println("c_density_xmax: "+c_density_xmax);
			System.out.println("c_density_ymin: "+c_density_ymin);
			System.out.println("c_density_ymax: "+c_density_ymax);*/
			
			
			
			for(Node c_grid : mapCellsInCoverage.keySet()) {
				double c_grid_x = c_grid.getXYPositionMap().getX();
				double c_grid_y = c_grid.getXYPositionMap().getY();
				/*System.out.println("c_grid_x: "+c_grid_x);
				System.out.println("c_grid_y: "+c_grid_y);
				System.out.println("############################################################################# ");*/
				if( (c_grid_x > c_density_xmin && c_grid_x < c_density_xmax) && (c_grid_y > c_density_ymin && c_grid_y < c_density_ymax) ) {
					quadrant_c[mapCellsInCoverage.get(c_grid)] = (int) c_density.getProperty("quadrant").get();
				}
			}
			
			for(Node c_luminaire : mapLuminairesInCoverage.keySet()) {
				double c_luminaire_x = c_luminaire.getXYPositionMap().getX();
				double c_luminaire_y = c_luminaire.getXYPositionMap().getY();
				//System.out.println("c_luminaire_x: "+c_luminaire_x);
				//System.out.println("c_luminaire_y: "+c_luminaire_y);

				if( (c_luminaire_x > c_density_xmin && c_luminaire_x < c_density_xmax) && (c_luminaire_y > c_density_ymin && c_luminaire_y < c_density_ymax) ) {
					quadrant_l[mapLuminairesInCoverage.get(c_luminaire)] = (int) c_density.getProperty("quadrant").get();
					//System.out.println("MATCH: "+(int) c_density.getProperty("quadrant").get());
				}
				//System.out.println("############################################################################# ");
			}
			
		}
		
		
        Map<Integer, Integer> occurrenceMapCells = new HashMap<>();
        for (int key : quadrant_c) {
            if (occurrenceMapCells.containsKey(key)) {
                int occurrence = occurrenceMapCells.get(key);
                occurrence++;
                occurrenceMapCells.put(key, occurrence);
            } else {
            	occurrenceMapCells.put(key, 1);
            }
        }
        
        Map<Integer, Integer> occurrenceMapLuminaires = new HashMap<>();
        for (int key : quadrant_l) {
            if (occurrenceMapLuminaires.containsKey(key)) {
                int occurrence = occurrenceMapLuminaires.get(key);
                occurrence++;
                occurrenceMapLuminaires.put(key, occurrence);
            } else {
            	occurrenceMapLuminaires.put(key, 1);
            }
        }
        
        // Just to check if the quadrant has been calculated correctly
        for (Integer key : occurrenceMapLuminaires.keySet()) {
            int occurrence = occurrenceMapLuminaires.get(key);
            System.out.println("Luminaires in coverage in quadrant "+key + ": " + occurrence);
        }
        

        // Just to check if the quadrant has been calculated correctly
        for (Integer key : occurrenceMapCells.keySet()) {
            int occurrence = occurrenceMapCells.get(key);
            if(key != 0) {
            	/*System.out.println(key);
                System.out.println((double) densityMap.get(key));
                System.out.println((double) occurrence);*/
                double traf = (double) densityMap.get(key) / (double) occurrence * trafPerUser * percUsersInStreet;
                System.out.println("Cells in coverage in quadrant "+key + ": " + occurrence+". Being the traffic in each cell = density("+densityMap.get(key)+") / ocurrences("+occurrence+") * trafPerUser("+trafPerUser +") * percUsersInStreet("+percUsersInStreet+") = "+ traf+". The total traffic in quadrant "+key+" is: "+ Math.floor(traf*occurrence/1024)+" Gbps");
            }else System.out.println("Cells out of range (no quadrant assigned and/or not in coverage): "+occurrence);
        }
        
        for(Node c_grid : mapCellsInCoverage.keySet()) {
        	int index = mapCellsInCoverage.get(c_grid);
        	int cellQuadrant = quadrant_c[index];
        	if(cellQuadrant != 0) {
            	/*System.out.println("##################################################");
            	System.out.println("t_c[index] "+ t_c[index]);
            	System.out.println("cellQuadrant "+ cellQuadrant);
            	System.out.println("densityMap.get(cellQuadrant): "+ densityMap.get(cellQuadrant)+" users");
            	System.out.println("occurrenceMapCells.get(cellQuadrant): "+ occurrenceMapCells.get(cellQuadrant) +" in coverage");*/
            	t_c[index] = (double) densityMap.get(cellQuadrant) / (double) occurrenceMapCells.get(cellQuadrant) * trafPerUser * percUsersInStreet;
            	//System.out.println("t_c[index] "+ t_c[index]+" Mbps");
        	}
        }
		return t_c;
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
//  public static DoubleMatrix1D multiply(double[] a, double[][] b) {
//  int columns_a = a.length;
//  int rows_b = b.length; //rows
//  int columns_b = b[0].length; //columns
//  if (columns_a != rows_b) throw new RuntimeException("Illegal matrix dimensions.");
//  double[] c = new double[columns_b];
//  for (int i = 0; i < columns_b; i++)
//      for (int j = 0; j < columns_a; j++)
//              c[i] += a[j] * b[j][i];
//  DoubleMatrix1D dm1d = DoubleFactory1D.dense.make (c.length , 0);
//  dm1d.assign(c);
//  return dm1d;
//}
//
}
