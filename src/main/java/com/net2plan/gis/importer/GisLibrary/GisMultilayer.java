package com.net2plan.gis.importer.GisLibrary;

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

public class GisMultilayer{
	String name;
	Map<String, GisLayer> mapLayerName2Layer = new HashMap<>();

	public GisMultilayer(String name) {
		this.name = name;
	}

	
	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return this.name;
	}

	public void addLayer(GisLayer gl) throws IOException {
		this.mapLayerName2Layer.put(gl.getLayerName(), gl);
	} 
	
	public void removeLayer(String layerName) {
		this.mapLayerName2Layer.remove(layerName);
	}

	public Map<String, GisLayer> getLayers() {
		return this.mapLayerName2Layer;
	}

	public GisLayer getLayer(String layerName) {
		return this.mapLayerName2Layer.get(layerName);

	}
	
	public String getLayerNames(){
		return this.mapLayerName2Layer.keySet().toString();
	}

}
