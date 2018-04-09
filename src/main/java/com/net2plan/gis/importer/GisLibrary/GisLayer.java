package com.net2plan.gis.importer.GisLibrary;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.SortedMap;
import java.util.TreeMap;

public class GisLayer implements Comparable<GisLayer>
{
	final long uniqueLayerId;
	final GisConstants.GISLAYERTYPE typeOfObjectsInside;
	final String name;
	SortedMap<Long,GisObject> mapUid2GisObject = new TreeMap<> ();
	GisMultilayer gml;

	GisLayer(GisMultilayer gml, GeoJSONParser gJSONParser, long id){
		this.gml = gml;
		this.name = gJSONParser.name;
		
		boolean isLuminaire = gJSONParser.features.get(0).properties.containsKey("POTENCIA");
		String geometryType = gJSONParser.features.get(0).geometry.type;
		this.typeOfObjectsInside = setLayerType(geometryType, isLuminaire);	
		
		this.uniqueLayerId = id;
	}
	
	public void setGml(GisMultilayer gml){
		this.gml = gml;
	}
	
	public long getUniqueLayerId() { return this.uniqueLayerId;}

	public GisConstants.GISLAYERTYPE setLayerType(String geometryType, boolean isLuminaire){
		if (geometryType.equals("Polygon")){return GisConstants.GISLAYERTYPE.BUILDINGS;}
		else if(geometryType.equals("LineString")){return GisConstants.GISLAYERTYPE.ROADS;}
		else if(geometryType.equals("Point") && !isLuminaire){return GisConstants.GISLAYERTYPE.CELLS;}
		else if(geometryType.equals("Point") && isLuminaire){return GisConstants.GISLAYERTYPE.LUMINAIRES;}
		else return GisConstants.GISLAYERTYPE.UNKNOWN;
	}
	
	public long getNewObjectUniqueId () {
		if(this.mapUid2GisObject.isEmpty()){return 1;}
		return this.mapUid2GisObject.lastKey() + 1; }
	
	public GisMultilayer getGml () { return gml; }
	
	public void addObject(GisObject object){ mapUid2GisObject.put(object.getId(), object); }
	
	public boolean isBuildingsLayer () { return typeOfObjectsInside == GisConstants.GISLAYERTYPE.BUILDINGS; }
	
	public boolean isRoadsLayer () { return typeOfObjectsInside == GisConstants.GISLAYERTYPE.ROADS; }
	
	public boolean isLuminairesLayer () { return typeOfObjectsInside == GisConstants.GISLAYERTYPE.LUMINAIRES; }
	
	public boolean isCellsLayer () { return typeOfObjectsInside == GisConstants.GISLAYERTYPE.CELLS; }

	public String getName(){ return this.name;}
	
	public SortedMap<Long,GisObject> getObjects(){return Collections.unmodifiableSortedMap(this.mapUid2GisObject);}
	
	public GisObject getObject(Long id){return this.mapUid2GisObject.get(id);}

	@Override
	public int compareTo(GisLayer gl) {
		return Long.compare(this.getUniqueLayerId () , gl.getUniqueLayerId ()); 
	}
	
}
