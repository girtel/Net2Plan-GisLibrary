
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
import java.net.MalformedURLException;
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

	private void createTopology(NetPlan netPlan, String path_luminaires/*String path_buildings*/, String path_cells){
		System.out.println("creating topology");
		
		/* remove topology */
		netPlan.removeAllAttributes();
		netPlan.removeAllNodes();
		netPlan.removeAllNetworkLayers();
		netPlan.removeAllLinks();
		
		GisMultilayer gml_C = new GisMultilayer("Cartagena");
		List<File> files = new ArrayList<File>();
		File Luminarias = new File(path_luminaires);
		File Cells = new File(path_cells);
		files.add(Luminarias);
		files.add(Cells);
		try {
			gml_C.buildFromGeoJson(gml_C, files);
		} catch (Exception e) {};
		
		System.out.println("Computing the number of luminaires and cells from files...");
		Map<Long, GisLayer> layers = gml_C.getLayers();
		//System.out.println(layers.size());
		Iterator<Long> gl_iterator = layers.keySet().iterator();
		while (gl_iterator.hasNext()) {
			Long key = gl_iterator.next();
			GisLayer gl = gml_C.getLayer(key);
			//System.out.println(gl.getName());
			Collection<GisObject> goc = gl.getObjects().values();
			for(GisObject go:goc){
				if (gl.isLuminairesLayer()) {
					Luminaire object = (Luminaire) go;
					L.add(netPlan.addNode(object.getPoint().getX(), object.getPoint().getY(),"Luminaire_"+String.valueOf(object.getId()), null));
				}else if (gl.isCellsLayer()) {
					Cell object = (Cell) go;
					C.add(netPlan.addNode(object.getPoint().getX(), object.getPoint().getY(),"Cell_"+String.valueOf(object.getId()), null));
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
		final String path_luminaires = algorithmParameters.get("path_luminaires");
		final String path_cells = algorithmParameters.get("path_cells");	
		final Double TrafPerUser = Double.parseDouble (algorithmParameters.get ("TrafPerUser")); //Mbps
		final Double percUsersInStreet = Double.parseDouble (algorithmParameters.get ("percUsersInStreet"))/100; //percentage
		final Double percCoverageRatio = Double.parseDouble (algorithmParameters.get ("percCoverageRatio"))/100; //percentage
		final Integer numInhabitants = Integer.parseInt(algorithmParameters.get ("numInhabitants"));
		final String solverLibraryName = algorithmParameters.get("solverLibraryName");
		final Double maxSolverTimeInMinutes = Double.parseDouble (algorithmParameters.get ("maxSolverTimeInMinutes"));


		createTopology(netPlan, path_luminaires, path_cells);
		
		final BidiMap<Node,Integer> mapLuminaire2Index = getAsBidiIndexMap (L);
		final BidiMap<Node,Integer> mapCell2Index = getAsBidiIndexMap (C);
		final BidiMap<Pair<Node,Node>,Integer> mapLink2Index = new DualHashBidiMap<> ();
		
		final int nL = L.size();	//number of luminaires
		final int nC = C.size();	//number of cells
		System.out.println("Number of luminaires: "+nL);
		System.out.println("Number of cells: "+nC);

			for (Node c : C) {
				for (Node l : L) {
					if (netPlan.getNodePairHaversineDistanceInKm(c, l) <= Dmax) {
						final int e = mapLink2Index.size();
						mapLink2Index.put(Pair.of(c, l), e);
					}
				}
			}
		
		final int E = mapLink2Index.size ();
		System.out.println("Number of potential links in coverage: "+E);
		
		final DoubleMatrix2D z_ec = DoubleFactory2D.sparse.make(E,nC);
		final DoubleMatrix2D z_el = DoubleFactory2D.sparse.make(E,nL);
		System.out.println("Sparse matrix created!");
		
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
		
		double trafficPerCell = TrafPerUser*percUsersInStreet*numInhabitants/nC;
		DoubleMatrix1D t_c = DoubleFactory1D.dense.make(nC, trafficPerCell);
		System.out.println("t_c vector created");
		
		/* Initialize an array with the demanded traffic for each building */
		/* Create the optimization object */
		OptimizationProblem op = new OptimizationProblem();
		
		/* Add the decision variables */
		op.addDecisionVariable("x_e" , false , new int [] {1,E} , 0 , maxTrafficPerPicoCellMbps);
		op.addDecisionVariable("z_l" , true , new int [] {1,nL} , 0 , 1); 
		
		/* Add the input parameters */
		op.setInputParameter("t_c", t_c, "row");
		op.setInputParameter("z_ec", z_ec);
		op.setInputParameter("z_el", z_el);
		op.setInputParameter("maxTrafficPerPicoCellMbps", maxTrafficPerPicoCellMbps);
		op.setInputParameter("percCoverageRatio", percCoverageRatio);
		
		/* Add the Objective Function */
		op.setObjectiveFunction("minimize", "sum(z_l)"); 
		
		/* Add the constraints */
		op.addConstraint("(x_e * z_el) <= maxTrafficPerPicoCellMbps * z_l ");
		op.addConstraint("(x_e * z_ec)' <= t_c' ");
		op.addConstraint("sum(x_e) >= percCoverageRatio*sum(t_c)");


		System.out.println(solverLibraryName);
		/* Call the solver to solve the problem */
		op.solve("cplex" ,"solverLibraryName", solverLibraryName , "maxSolverTimeInSeconds", maxSolverTimeInMinutes*60);
		
		/* Retrieve info */
		final double [] z_l = op.getPrimalSolution("z_l").to1DArray();
		final double [] x_e = op.getPrimalSolution("x_e").to1DArray();
		
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
		param.add (Triple.of ("path_luminaires" , "data/luminarias-ct-estudio.geojson" , "Luminaires file"));
		param.add (Triple.of ("maxTrafficPerPicoCellMbps" , "1024" , "Max Mbps offered by each antenna"));
		param.add (Triple.of ("Dmax" , "50" , "Max coverage distance in m"));
		param.add (Triple.of ("TrafPerUser" , "50" , "Traffic per user in Mbps"));
		param.add (Triple.of ("percUsersInStreet" , "80" , "Peak % users in the street"));
		param.add (Triple.of ("percCoverageRatio" , "10" , "Traffic Coverage in %"));
		param.add (Triple.of ("numInhabitants" , "49966" , "Numbers of inhabitants"));
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
