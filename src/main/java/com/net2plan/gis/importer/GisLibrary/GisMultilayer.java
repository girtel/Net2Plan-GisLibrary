package com.net2plan.gis.importer.GisLibrary;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.net2plan.interfaces.networkDesign.Net2PlanException;

public class GisMultilayer{
	
	String name;
	SortedMap<Long, String> mapLayerId2LayerName = new TreeMap<>();
	SortedMap<Long, GisLayer> mapLayerId2Layer = new TreeMap<>();

	public GisMultilayer(String name) {
		this.name = name;
	}
	
	public void setName(String name){ this.name = name; }

	public String getName() { return this.name; }
	
	public long getNewLayerUniqueId () {
		if(this.mapLayerId2Layer.isEmpty()){return 1;}
		return this.mapLayerId2Layer.lastKey() + 1; }
	
	public void buildFromGeoJson(List<File> files) 
	{
		try
		{
			for (File file : files)
			{
				final ObjectMapper objectMapper = new ObjectMapper();
				final GeoJSONParser parser = objectMapper.readValue(file, GeoJSONParser.class);
				final Long layerId = this.getNewLayerUniqueId();
				//print
				System.out.println(name+": "+parser.name+" layer loaded.");
				final GisLayer gl = new GisLayer(this, parser, layerId);
				this.parseObjects(parser,gl);
				this.mapLayerId2Layer.put(layerId, gl);
				this.mapLayerId2LayerName.put(layerId, gl.getName());
			}
		} catch (IOException e) { throw new Net2PlanException ("The file could not be read"); }
	}
	
	public void buildFromGeoJson(File file) throws IOException{
		ObjectMapper objectMapper = new ObjectMapper();
		GeoJSONParser parser = objectMapper.readValue(file, GeoJSONParser.class);
		Long layerId = this.getNewLayerUniqueId();
		//print
		System.out.println(name+": "+parser.name+" layer loaded.");
		GisLayer gl = new GisLayer(this, parser, layerId);
		this.parseObjects(parser,gl);
		this.mapLayerId2Layer.put(layerId, gl);
		this.mapLayerId2LayerName.put(layerId, gl.getName());

	} 

	private void parseObjects(GeoJSONParser parser, GisLayer gl){
		// Creación de GisObjects
		List<GisObject> objects = new ArrayList<GisObject>();
		ListIterator<GeoJSONParser.GeoJSONFeature> objectsIterator = parser.features.listIterator(); // iterador
		if (gl.isBuildingsLayer()) { // buildings?
			while (objectsIterator.hasNext()) {
				long id = gl.getNewObjectUniqueId();
				Building building = new Building(objectsIterator.next(), gl, id);
				gl.addObject(building);
			}
		} else if (gl.isRoadsLayer()) { // roads?
			while (objectsIterator.hasNext()) {
				long id = gl.getNewObjectUniqueId();
				Road road = new Road(objectsIterator.next(), gl, id);
				gl.addObject(road);
			}

		}else if (gl.isLuminairesLayer()) { // luminaires?
			while (objectsIterator.hasNext()) {
				long id = gl.getNewObjectUniqueId();
				Luminaire luminaire = new Luminaire(objectsIterator.next(), gl, id);
				gl.addObject(luminaire);
			}
		}else if(gl.isCellsLayer()) { 
			while (objectsIterator.hasNext()) {
				long id = gl.getNewObjectUniqueId();
				Cell cell = new Cell(objectsIterator.next(), gl, id);
				gl.addObject(cell);
			}
		}
	}
	
	public void addLayer(GisLayer gl) throws IOException {
		this.mapLayerId2Layer.put(this.getNewLayerUniqueId(), gl);
		this.mapLayerId2LayerName.put(this.getNewLayerUniqueId(), gl.getName());
	} 
			
	public void removeLayer(Long id) {
		GisLayer gl = this.mapLayerId2Layer.get(id);
		gl.setGml(null);
		this.mapLayerId2Layer.remove(id);
		this.mapLayerId2LayerName.remove(id);
	}

	public SortedMap<Long, GisLayer> getLayers() { return Collections.unmodifiableSortedMap(this.mapLayerId2Layer); }

	public Set<GisLayer> getLayersByName(String layerName) {
		Set<GisLayer> gLSet = new TreeSet();
		Iterator it = this.mapLayerId2Layer.keySet().iterator();

		while (it.hasNext()) {
			GisLayer gl = this.mapLayerId2Layer.get(it.next());
			if (gl.getName().equals(layerName)) { gLSet.add(gl); }
		}
		return gLSet;
	}
	
	public GisLayer getLayer(Long id) { return this.mapLayerId2Layer.get(id); }
	
	public String getLayerName(Long id){ return this.mapLayerId2LayerName.get(id); }

	public Collection<String> getLayerNames(){ return Collections.unmodifiableCollection(this.mapLayerId2LayerName.values()); }

}
