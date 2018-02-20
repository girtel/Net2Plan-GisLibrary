
package com.net2plan.gis.algorithms;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;

import com.jom.DoubleMatrixND;
import com.jom.OptimizationProblem;
import com.net2plan.gis.importer.GisLibrary.Building;
import com.net2plan.gis.importer.GisLibrary.GisLayer;
import com.net2plan.gis.importer.GisLibrary.GisMultilayer;
import com.net2plan.gis.importer.GisLibrary.GisObject;
import com.net2plan.gis.importer.GisLibrary.Luminaire;

//import com.net2plan.gui.plugins.networkDesign.topologyPane.TopologyPanel;
import com.net2plan.interfaces.networkDesign.IAlgorithm;
import com.net2plan.interfaces.networkDesign.Net2PlanException;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.networkDesign.Node;
import com.net2plan.utils.Pair;
import com.net2plan.utils.Triple;

import cern.colt.matrix.tdouble.DoubleFactory1D;
import cern.colt.matrix.tdouble.DoubleFactory2D;
import cern.colt.matrix.tdouble.DoubleMatrix1D;
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
	
	private List<Node> B = new ArrayList<>();	//Buildings
	private List<Node> L = new ArrayList<>();	//Luminaires
	
	private void createTopology(NetPlan netPlan){
		
		/* remove topology */
		netPlan.removeAllNetworkLayers();
		netPlan.removeAllNodes();
		netPlan.removeAllLinks();
		
		GisMultilayer gml_C = new GisMultilayer("Cartagena");
		List<File> files = new ArrayList<File>();
		File Edificios = new File("C:/Users/jlrg_/Desktop/UPCT/QGIS/OSM2QGIS/EdificiosCiudad.geojson");
		File Luminarias = new File("C:/Users/jlrg_/Desktop/UPCT/QGIS/OSM2QGIS/LuminariasCiudad.geojson");
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
		
		/*for(Node l_node : L)
		{
			l_node.setUrlNodeIcon(netPlan.getNetworkLayerDefault(), TopologyPanel.class.getResource("/resources/gui/figs/BaseStation.png"));
		}
		for(Node b_node : B)
		{
			b_node.setUrlNodeIcon(netPlan.getNetworkLayerDefault(), TopologyPanel.class.getResource("/resources/gui/figs/Building.png"));
		}*/
		
	}
	
	
	@Override
	public String executeAlgorithm(NetPlan netPlan, Map<String, String> algorithmParameters, Map<String, String> net2planParameters)
	{
		
		createTopology(netPlan);
		final BidiMap<Node,Integer> mapBuilding2Index = getAsBidiIndexMap (B);
		final BidiMap<Node,Integer> mapLuminaire2Index = getAsBidiIndexMap (L);
		final BidiMap<Pair<Node,Node>,Integer> mapLink2Index = new DualHashBidiMap<> ();
		
		
		/* Typically, you start reading the input parameters */
		final Double Dmax = Double.parseDouble (algorithmParameters.get ("Dmax"))/1000; //Max distance in m
		final Double costPerBlockedMbps = Double.parseDouble (algorithmParameters.get ("costPerBlockedMbps")); //Factor 
		final Double costPerPicoCell = Double.parseDouble (algorithmParameters.get ("costPerPicoCell")); //Mbps
		final Double maxTrafficPerPicoCellMbps = Double.parseDouble (algorithmParameters.get ("maxTrafficPerPicoCellMbps")); //Mbps


		
		final int E = mapLink2Index.size ();
		final int nB = B.size();	//number of buildings
		final int nL = L.size();	//number of luminaires
		System.out.println("Number of buildings: "+nB);
		System.out.println("Number of luminaires: "+nL);
		
		/* Compute the set of links */
		final DoubleMatrix2D z_eb = DoubleFactory2D.sparse.make(E,nB); //nB*nL is the total number of couples
		final DoubleMatrix2D z_el = DoubleFactory2D.sparse.make(E,nL);
		for (Node b : B){
			final int buildingIndex = mapBuilding2Index.get(b);
			for (Node l : L){
				final int luminaireIndex = mapLuminaire2Index.get(l);
				if (netPlan.getNodePairEuclideanDistance (b,l) <= Dmax)
				{
					final int e = mapLink2Index.size();
					mapLink2Index.put (Pair.of (b,l) , e);
					z_eb.set (e , buildingIndex, 1.0);
					z_el.set (e , luminaireIndex, 1.0);
				}
			}
		}
		
		DoubleMatrix1D X_b = DoubleFactory1D.dense.make (nB , 50.0);
		
		/* Initialize an array with the demanded traffic for each building */
		/* Create the optimization object */
		OptimizationProblem op = new OptimizationProblem();
		
		/* Add the decision variables */
		op.addDecisionVariable("x_e" , false , new int [] {1,E} , 0 , maxTrafficPerPicoCellMbps);
		op.addDecisionVariable("z_l" , true , new int [] {1,nL} , 0 , 1); 
		
		/* Add the input parameters */
		op.setInputParameter("Dmax" , Dmax);
		op.setInputParameter("X_b", X_b, "row");
		op.setInputParameter("z_eb", z_eb);
		op.setInputParameter("z_el", z_el);
		op.setInputParameter("costPerPicoCell", costPerPicoCell);
		op.setInputParameter("costPerBlockedMbps", costPerBlockedMbps);
		op.setInputParameter("maxTrafficPerPicoCellMbps", maxTrafficPerPicoCellMbps);
		
		/* Add the Objective Function */
		op.setObjectiveFunction("minimize", "  costPerBlockedMbps * (sum(X_b - (x_e * z_eb ) )) + costPerPicoCell * sum(z_l)"); 
		
		/* Add the contraints */
		op.addConstraint("x_e * z_eb <= X_b");
		op.addConstraint("x_e * z_el <= maxTrafficPerPicoCellMbps * z_l");

		/* Call the solver to solve the problem */
		op.solve("cplex", "maxSolverTimeInSeconds" , 60);
		if (!op.solutionIsOptimal()) throw new Net2PlanException ("The solution is not optimal");
		
		
		/* TO-DO */
		/* Retrieve the optimal solution found */
		//x_e
		//z_l
		final double [] z_l = op.getPrimalSolution("z_l").to1DArray();
		final double [] x_e = op.getPrimalSolution("x_e").to1DArray();
		
		for (Node b : B){
			final int buildingIndex = mapBuilding2Index.get(b);
			for (Node l : L){
				final int luminaireIndex = mapLuminaire2Index.get(l);
				if (netPlan.getNodePairEuclideanDistance (b,l) <= Dmax)
				{
					final int e = mapLink2Index.size();
					mapLink2Index.put (Pair.of (b,l) , e);
					z_eb.set (e , buildingIndex, 1.0);
					z_el.set (e , luminaireIndex, 1.0);
				}
			}
		}
		
		
						
		/* checks */
		for (Node b : B) 
		{
			final int buildingIndex = mapBuilding2Index.get(b);
			if(b.getIncomingLinksAllLayers().isEmpty()){ /* Has the building incoming links? */
				if(b.getOutgoingLinksTraffic() > X_b.get(buildingIndex)){ /* Exceeds the building the maximum traffic limit? */
					throw new Net2PlanException ("The outgoing traffic in a building exceeds the maximum limit."); }
			}else{ throw new Net2PlanException ("The buildings has one or more incoming links."); }
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
		param.add (Triple.of ("Dmax" , "50" , "Max coverage distance in m"));
		param.add (Triple.of ("costPerBlockedMbps" , "80" , "Virtual cost per Mbps of not covering a users traffic with picocells"));
		param.add (Triple.of ("maxTrafficPerPicoCellMbps" , "100" , "Max Mbps offered by each antenna"));
		param.add (Triple.of ("costPerPicoCell" , "100" , "Cost per installing each Pico Cell"));
		
		
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
}
