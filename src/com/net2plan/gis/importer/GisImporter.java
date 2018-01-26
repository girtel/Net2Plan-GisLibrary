package com.net2plan.gis.importer;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class GisImporter
{

	// ArrayList<GisMultilayer> gmlList = new ArrayList<GisMultilayer>();
	Map<String, GisMultilayer> gmlList = new HashMap<String, GisMultilayer>();

	GisMultilayer multilayer;

	// Metodo para cargar un geojson y crear GisMultilayer
	public GisMultilayer load(String name, String path) throws FileNotFoundException, IOException
	{
		{

			String line = "";
			String content = "";

			FileReader f = new FileReader(path);
			BufferedReader b = new BufferedReader(f);
			while ((line = b.readLine()) != null)
			{
				if (content.equals(""))
				{
					content = line;
				} else
				{
					content = content + "\n" + line;
				}
			}
			b.close();
			int index1 = content.indexOf("\"name\": \""); // 9 caracteres
			int index2 = content.indexOf("\"crs"); // 3 caracteres
			// Nombre de la capa
			String layerName = content.substring(index1 + 9, index2 - 3);
			// Objetos
			content = content.substring(content.indexOf("\"features\": [") + 14, content.length() - 4);

			// Si hay algún multilayer con ese nombre no crea uno nuevo,
			// actualiza el existente añadiendo un nuevo path (layer).

			if (gmlList.containsKey(name))
			{
				gmlList.get(name).addLayer(layerName, content);
			} else
			{
				multilayer = new GisMultilayer(name);
				// Añade a la lista
				gmlList.put(name, multilayer);
				// Añade capa
				multilayer.addLayer(layerName, content);
			}
			return multilayer;

		}
	}

	public GisMultilayer getMultilayer(String name)
	{
		return gmlList.get(name);
	}

	public Collection<GisMultilayer> getMultilayers()
	{
		return gmlList.values();
	}

	public static void main(String[] args) throws FileNotFoundException, IOException
	{

		// ########## TEST ##########
		// Cargamos fichero edificios
		String pathBuildings = "C:/Users/jlrg_/Desktop/UPCT/QGIS/OSM2QGIS/e1.geojson"; // Edificios
		String pathRoads = "C:/Users/jlrg_/Desktop/UPCT/QGIS/OSM2QGIS/c1.geojson"; // Carreteras
		GisImporter gi = new GisImporter();
		gi.load("Cartagena", pathBuildings);
		gi.load("Cartagena", pathRoads);

		System.out.println("***********************************");
		// Creamos objeto GisMultilayer
		GisMultilayer gml = gi.getMultilayer("Cartagena");
		// Comprobacion
		// Get MultiLayer name
		System.out.println("Multilayer name: ");
		System.out.println(gml.getName());
		System.out.println("***********************************");
		// Get Layer names
		List<GisLayer> glNames = new ArrayList<GisLayer>();
		System.out.println("Layer name: ");

		List<String> layerNames = gml.getLayerNames();
		Iterator<String> iterator = layerNames.iterator();
		while (iterator.hasNext())
		{
			String name = iterator.next();
			System.out.println(name);
			// Creamos objeto GisLayer
			glNames.add(gml.getLayer(name));
		}
		System.out.println("***********************************");
		System.out.println("***********************************");
		// Comprobación GisLayer
		Iterator<GisLayer> iterator1 = glNames.iterator();
		while (iterator1.hasNext())
		{
			GisLayer gl = iterator1.next();
			System.out.println("Getting objects from " + gl.getLayerName());
			System.out.println("---------------------------------------------");
			Collection<GisObject> goList = gl.getObjects();
			Iterator<GisObject> iterator2 = goList.iterator();
			while (iterator2.hasNext())
			{
				GisObject go = iterator2.next();
				System.out.println("Raw Data: " + go.getRawData());
				System.out.println("Properties: ");
				Iterator it = go.getProperties().keySet().iterator();
				while (it.hasNext())
				{
					String key = (String) it.next();
					System.out.println(key + " : " + go.getProperties().get(key));
				}
				System.out.println("Geometry Type: " + go.getGeometryType());
				System.out.println("Geometry Coordinates: " + go.getCoordinates());
				System.out.println("---------------------------------------------");
			}

			System.out.println("***********************************");

		}

	}

    private static class ClassForJackson
    {
        public String type;
        public String name;
        public JoseLuisCrs crs;
        public List<JoseLuisfeatrure> features;
        
        public ClassToSaveJackson(
                @JsonProperty("type") String type,
                @JsonProperty("name") String name, 
        		@JsonProperty("crs") JoseLuisCrs crs , 
                @JsonProperty("features") List<JoseLuisFeature> features)
        {
			super();
			this.type = type;
			this.ports = ports;
			this.chassis = chassis;
			this.otsFiberSpanModels = otsFiberSpanModels;
			this.ochTpBidiModels = ochTpBidiModels;
			this.ochRegUnidiModels = ochRegUnidiModels;
		}
    }

    private static class JoseLuisCrs
    {
    	public String type;
    	public Map<String,String> properties;
    	public JoseLuisCrs ()
    	{
    		
    	}
    }
	
}