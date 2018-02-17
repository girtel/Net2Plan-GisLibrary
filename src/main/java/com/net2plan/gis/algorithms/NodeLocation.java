
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
		//netPlan.removeAllNetworkLayers();
		netPlan.removeAllNodes();
		//netPlan.removeAllLinks();
		
		GisMultilayer gml_C = new GisMultilayer("Cartagena");
		List<File> files = new ArrayList<File>();
		File Edificios = new File("C:/Users/jlrg_/Desktop/UPCT/QGIS/OSM2QGIS/Edificios.geojson");
		File Luminarias = new File("C:/Users/jlrg_/Desktop/UPCT/QGIS/OSM2QGIS/Luminarias.geojson");
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
		
		//netPlan.removeAllLinks();
		
		/* Typically, you start reading the input parameters */
		final Double Dmax = Double.parseDouble (algorithmParameters.get ("Dmax"))/1000; //Max distance in m
		//System.out.println(Dmax);
		final Double costPerBlockedMbps = Double.parseDouble (algorithmParameters.get ("costPerBlockedMbps")); //Factor 
		final Double costPerPicoCell = Double.parseDouble (algorithmParameters.get ("costPerPicoCell")); //Mbps
		final Double maxTrafficPerPicoCellMbps = Double.parseDouble (algorithmParameters.get ("maxTrafficPerPicoCellMbps")); //Mbps
		///////////////////////////// 1 /////////////////////////////
		/* Create the optimization object */
		OptimizationProblem op = new OptimizationProblem();
		
		///////////////////////////// 2 /////////////////////////////
		
		final int nB = B.size();	//number of buildings
		final int nL = L.size();	//number of luminaires

		//inicializar vector X_b de tama�o nB y con valores entre 0 y X.
		DoubleMatrix1D X_b = DoubleFactory1D.dense.make (nB , 50.0);
		
		final DoubleMatrix2D isInCoverageConstraintLimit = DoubleFactory2D.dense.make(nB , nL , maxTrafficPerPicoCellMbps);
		for(Node b:B){
			for(Node l:L){
				//System.out.println(netPlan.getNodePairEuclideanDistance(b,l));
				if(netPlan.getNodePairEuclideanDistance(b,l)> Dmax ){
					final int buildingIndex = mapBuilding2Index.get(b);
					final int luminaireIndex = mapLuminaire2Index.get(l);
					isInCoverageConstraintLimit.set(buildingIndex, luminaireIndex, 0.0);
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
		op.setInputParameter("costPerPicoCell", costPerPicoCell);
		op.setInputParameter("costPerBlockedMbps", costPerBlockedMbps);
		op.setInputParameter("maxTrafficPerPicoCellMbps", maxTrafficPerPicoCellMbps);
										//               coste Mbps perdidos  + coste instalaci�n luminaria
		op.setObjectiveFunction("minimize", "  costPerBlockedMbps * (sum(X_b - sum(x_bl,2)')) + costPerPicoCell * sum(z_l)"); 
		//" ALPHA * Z * sum( X_b � sum(x_bl) ) + Z * sum(z_l)"
		
		op.addConstraint("sum(x_bl,2) <= X_b'");
		op.addConstraint("sum(x_bl,1) <= maxTrafficPerPicoCellMbps * z_l");

		/* Call the solver to solve the problem */
		op.solve("cplex");
		if (!op.solutionIsOptimal()) throw new Net2PlanException ("The solution is not optimal");
		/* Retrieve the optimal solution found */
		final double [] z_l = op.getPrimalSolution("z_l").to1DArray();
		final double [][] x_bl = (double [][]) op.getPrimalSolution("x_bl").toArray();
		System.out.println("Building demanded traffic: "+X_b.toString());
		System.out.println("Luminaries offered traffic: "+Arrays.toString(z_l));
		System.out.println("X is buildings, Y is luminaries");
		printMatrix(x_bl);
				
		/* Save the access-to-node links in the design (links are not bidirectional) */
		for (Node b : B) 
		{
			final int buildingIndex = mapBuilding2Index.get(b);
			for (Node l : L)
			{
				final int luminaireIndex = mapLuminaire2Index.get(l);
				if ((z_l[luminaireIndex]==1) && (x_bl[buildingIndex][luminaireIndex] > 0))
				{
					netPlan.addLink(b, l, x_bl[buildingIndex][luminaireIndex], netPlan.getNodePairEuclideanDistance(b,l) , 200000 , null);
					l.addTag ("HASPICOCELL");
				}
			}
		}
		
		/* checks */
		for (Node b : B) 
		{
			final int buildingIndex = mapBuilding2Index.get(b);
			if(b.getIncomingLinksAllLayers().isEmpty()){
				// keep checking
				if(b.getOutgoingLinksTraffic() > X_b.get(buildingIndex)){ break; }
			}else{ break; }

			//b.getOutgoingLinks ()--> sumo para todos ellos la capacidad, y debe ser menor o igual que el trafico ofrecido por el building
			//b.incomingLinks debe estar vacio
		}
		
		for (Node l : L) {
			if (l.getOutgoingLinksAllLayers().isEmpty()) {
				// keep checking
				if (l.getIncomingLinksAllLayers().isEmpty()) {
					if (l.hasTag("HASPICOCELL")) { break; }
				} else {
					if (!l.hasTag("HASPICOCELL")) { break; }
					if (l.getIncomingLinksTraffic() > maxTrafficPerPicoCellMbps) { break; }
				}
			} else { break; }

			// l.getIncomginLink ()--> si esta vacio el l no puede tener la tag
			// HASPICOCELL
			// l.getIncomginLink ()--> si no esta vacio el l debe tener la tag
			// HASPICOCELL
			// l.getIncomginLink ()--> si no esta vacio, la suma de las
			// capacidaddes de los enlaces entrantes debe ser menor o igual a la
			// capacidad de la picocell
			// l.outgoingLink debe estar vacio
		}
		
		op.addConstraint("sum(x_bl,2) <= X_b'");
		op.addConstraint("sum(x_bl,1) <= maxTrafficPerPicoCellMbps * z_l");

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
