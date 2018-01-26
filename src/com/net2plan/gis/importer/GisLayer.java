package net2plan.gis.importer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;

public class GisLayer {
	
	String name;
	ArrayList<GisObject> goList = new ArrayList<GisObject>();
	
	public GisLayer(String layerName, String content){
		setLayerName(layerName);
		try {
			parseContent(content);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void parseContent(String content) throws IOException {

		// Va leyendo lineas del contenido. Cada linea es un nuevo objeto que se añade a la capa
		BufferedReader bufReader = new BufferedReader(new StringReader(content));
		String line = null;
		while ((line = bufReader.readLine()) != null) {
			GisObject object = new GisObject(line);
			goList.add(object);
		}

	}
	
	public Collection <GisObject> getObjects(){
		return goList;
	}
	
	public void setLayerName(String name){
		this.name = name;
	}
	
	public String getLayerName(){
		return name;
	}

	
	

}
