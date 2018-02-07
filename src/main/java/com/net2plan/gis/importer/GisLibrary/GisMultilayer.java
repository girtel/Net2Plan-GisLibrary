package com.net2plan.gis.importer.GisLibrary;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import com.fasterxml.jackson.databind.ObjectMapper;

public class GisMultilayer implements Cloneable{
	String name;
	SortedMap<String, GisLayer> mapLayerName2Layer = new TreeMap<>();

	public GisMultilayer(String name) {
		this.name = name;
	}
	
	
	public void buildFromGeoJson(List<File> files) throws IOException, CloneNotSupportedException{
		ObjectMapper objectMapper = new ObjectMapper();

		ListIterator<File> fileIterator = files.listIterator();
		while (fileIterator.hasNext()) {
			File path = fileIterator.next();

			GeoJSONParser parser = objectMapper.readValue(path, GeoJSONParser.class);
			
			//print
			System.out.println(name+": "+parser.name+" layer loaded.");
			
			/*// Creación de GisMultilayer
			if (!this.gmlList.containsKey(name)) {
				gml = new GisMultilayer(name);
				this.gmlList.put(name, gml);
			} else {
				gml = this.gmlList.get(name);
			}*/

			// Creación de GisLayer
			//GisLayer gl = new GisLayer(gml, parser); // gl conoce su gml
			GisLayer gl = this.addLayer(path);
			// Creación de GisObjects
			List<GisObject> objects = new ArrayList<GisObject>();
			ListIterator<GeoJSONParser.GeoJSONFeature> objectsIterator = parser.features.listIterator(); // iterador
			if (gl.isBuildingsLayer()) { // son edificios?
				while (objectsIterator.hasNext()) {
					long id = gl.getNewUniqueId();
					Building building = new Building(objectsIterator.next(), gl, id);
					gl.addObject(building);
				}
			} else if (gl.isRoadsLayer()) { // son carreteras?
				while (objectsIterator.hasNext()) {
					long id = gl.getNewUniqueId();
					Road road = new Road(objectsIterator.next(), gl, id);
					gl.addObject(road);
				}

			}
		}

	}

	
	public void setName(String name){
		this.name = name;
		try{updateChilds();}catch(Exception e){}
	}

	public String getName() {
		return this.name;
	}
	
	public void addLayer(GisLayer gl) throws IOException {
		this.mapLayerName2Layer.put(gl.getName(), gl);
		try{updateChilds();}catch(Exception e){}
	} 
	
	public GisLayer addLayer(File file) throws IOException, CloneNotSupportedException {
		ObjectMapper objectMapper = new ObjectMapper();
		GeoJSONParser parser = objectMapper.readValue(file, GeoJSONParser.class);
		
		GisLayer gl = new GisLayer((GisMultilayer) this.clone(), parser);
		this.mapLayerName2Layer.put(gl.getName(), gl);
		try{updateChilds();}catch(Exception e){}
		
		return gl;
	} 
	
	public void removeLayer(String layerName) {
		GisLayer gl = this.mapLayerName2Layer.get(layerName);
		gl.setGml(null);
		this.mapLayerName2Layer.remove(layerName);
	}

	public SortedMap<String, GisLayer> getLayers() {return Collections.unmodifiableSortedMap(this.mapLayerName2Layer);
	}

	public GisLayer getLayer(String layerName) {
		return this.mapLayerName2Layer.get(layerName);

	}
	
	public String getLayerNames(){
		return this.mapLayerName2Layer.keySet().toString();
	}
	
	private void updateChilds() throws CloneNotSupportedException{
		System.out.println("Updating childs");
		Iterator<String> it = this.mapLayerName2Layer.keySet().iterator();
		while(it.hasNext()){
			GisLayer gl = this.mapLayerName2Layer.get(it.next());
			gl.setGml((GisMultilayer)this.clone());
		}
	}


}
