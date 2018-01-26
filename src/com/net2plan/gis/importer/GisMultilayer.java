package net2plan.gis.importer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;


public class GisMultilayer 
{
	final String name;
	boolean check;
	//ArrayList<GisLayer> glList = new ArrayList<GisLayer>();
	Map<String,GisLayer> mapLayerName2Layer = new HashMap<>();
	SortedMap<Long,GisObjectPablo> mapUid2GisObject = new TreeMap<> (); 

	public long getNewUniqueId () { return this.mapUid2GisObject.lastKey() + 1; }		

	
	
	public GisMultilayer(String name, List<File> path)
	{
		this.name = name;
		for (File geoJson : path) this.addLayer(geoJson);
	}

	
	public GisMultilayer(String name)
	{
		setName(name);
	}
	
	public List<String> getLayerNames(){
		List<String> names = new ArrayList<String>();
		for ( String key : glList.keySet() ) {
		    names.add(key);
		}
		return names;
	}
	
	public void setName(String name){
		this.name = name;
	}
	
	public String getName() {
		
		return this.name;
	}
	
	
	public GisLayer addLayer(File geoJson) throws IOException 
	{
		// Si existía una capa con ese nombre sobrescribe el contenido
		final BufferedReader b = new BufferedReader(new FileReader (geoJson.getAbsolutePath()));
		JsonReader jsonReader = Json.createReader(b);
		
		
		
		
		
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

		
		if (glList.containsKey(layerName)) {
			glList.get(layerName).parseContent(content);
		// Si no existía se crea y se añade al mapa
		} else {
			GisLayer gL = new GisLayer(layerName, content);
			glList.put(layerName, gL);
		}
		

		return multilayer;

	}
	
	public void removeLayer(String layerName){

		glList.remove(layerName);
	}
	
	public Collection <GisLayer> getLayers(){
		return glList.values();
	}
	
	public GisLayer getLayer(String layerName){
		return glList.get(layerName);
	}
	
}
