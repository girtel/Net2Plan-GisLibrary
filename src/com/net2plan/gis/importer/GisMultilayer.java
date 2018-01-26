package net2plan.gis.importer;

import java.util.ArrayList;
import java.io.IOException;
import java.util.*;

public class GisMultilayer {
	
	String name;
	boolean check;
	//ArrayList<GisLayer> glList = new ArrayList<GisLayer>();
	Map<String,GisLayer> glList = new HashMap<String,GisLayer>();
	

	public GisMultilayer(String name){
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
	
	
	public void addLayer(String layerName, String content) throws IOException {

		// Si existía una capa con ese nombre sobrescribe el contenido
		if (glList.containsKey(layerName)) {
			glList.get(layerName).parseContent(content);
		// Si no existía se crea y se añade al mapa
		} else {
			GisLayer gL = new GisLayer(layerName, content);
			glList.put(layerName, gL);
		}
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
