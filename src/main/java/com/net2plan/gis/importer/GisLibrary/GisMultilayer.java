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
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import com.fasterxml.jackson.databind.ObjectMapper;

public class GisMultilayer implements Cloneable{
	
	
	/**		Añadir un campo "uniqueLayerId" de tipo long al GisLayer. 
	 ** 	Que no se repita entre dos capas del mismo gml. Ahora el identificador único de un objeto es (uiqueLayerId,uniqueObjetOdWithinLayer). 
	 *		No se borra el name, pero puede haber mas de una capa con el mismo name. Ahora  mapLayerName2Layer tiene que devolver Set<GisLayer>, 
	 *		poruqe puede haber más de una. Tienes que hacer un mapLayerId2Layer<Long,GisLayer>
	*/
	
	String name;
	//SortedMap<String, GisLayer> mapLayerName2Layer = new TreeMap<>();
	SortedSet<GisLayer> mapLayerName2Layer = new TreeSet<>();
	SortedMap<Long, GisLayer> mapLayerId2Layer = new TreeMap<>();
	//SortedMap<Long,GisObject> mapUid2GisObject = new TreeMap<> ();

	public GisMultilayer(String name) {
		this.name = name;
	}
	
	public long getNewLayerUniqueId () {
		if(this.mapLayerId2Layer.isEmpty()){return 1;}
		return this.mapLayerId2Layer.lastKey() + 1; }
	
	
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
					long id = gl.getNewObjectUniqueId();
					Building building = new Building(objectsIterator.next(), gl, id);
					gl.addObject(building);
				}
			} else if (gl.isRoadsLayer()) { // son carreteras?
				while (objectsIterator.hasNext()) {
					long id = gl.getNewObjectUniqueId();
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
		this.mapLayerId2Layer.put(this.getNewLayerUniqueId(), gl);
		try{updateChilds();}catch(Exception e){}
	} 
	
	public GisLayer addLayer(File file) throws IOException, CloneNotSupportedException {
		ObjectMapper objectMapper = new ObjectMapper();
		GeoJSONParser parser = objectMapper.readValue(file, GeoJSONParser.class);
		Long layerId = this.getNewLayerUniqueId();
		
		GisLayer gl = new GisLayer((GisMultilayer) this.clone(), parser, layerId);
		this.mapLayerId2Layer.put(layerId, gl);
		try{updateChilds();}catch(Exception e){}
		
		return gl;
	} 
	
	public void removeLayer(Long id) {
		GisLayer gl = this.mapLayerId2Layer.get(id);
		gl.setGml(null);
		this.mapLayerId2Layer.remove(id);
	}

	public SortedMap<Long, GisLayer> getLayers() {return Collections.unmodifiableSortedMap(this.mapLayerId2Layer);
	}

	//getLayer con name
	public GisLayer getLayer(Long id) {
		return this.mapLayerId2Layer.get(id);

	}
	
	//
	public String getLayerNames(){
		return this.mapLayerName2Layer.keySet().toString();
	}
	
	private void updateChilds() throws CloneNotSupportedException{
		System.out.println("Updating childs");
		Iterator<Long> it = this.mapLayerId2Layer.keySet().iterator();
		while(it.hasNext()){
			GisLayer gl = this.mapLayerId2Layer.get(it.next());
			gl.setGml((GisMultilayer)this.clone());
		}
	}


}
