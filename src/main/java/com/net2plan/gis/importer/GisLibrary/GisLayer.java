package com.net2plan.gis.importer.GisLibrary;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
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

public class GisLayer 
{
	public GisConstants.GISLAYERTYPE typeOfObjectsInside;
	public final String name;
	SortedMap<Long,GisObject> mapUid2GisObject = new TreeMap<> ();
	GisMultilayer gml;

	public GisLayer(GisMultilayer gml, GeoJSONParser cfj){
		this.gml = gml;
		this.name = cfj.name;
		setLayerType(cfj.features.get(0).geometry.type);	
	}
	
	
	public long getNewUniqueId () {
		if(this.mapUid2GisObject.isEmpty()){return 1;}
		return this.mapUid2GisObject.lastKey() + 1; }
	
	public GisMultilayer getGml () { return gml; }
	
	private void setLayerType(String type){
		if (type.equals("Polygon")){this.typeOfObjectsInside = GisConstants.GISLAYERTYPE.BUILDINGS;}
		else if(type.equals("LineString")){this.typeOfObjectsInside = GisConstants.GISLAYERTYPE.ROADS;}
		else this.typeOfObjectsInside = GisConstants.GISLAYERTYPE.UNKNOWN;
	}
	
	public void addObject(GisObject object){
		mapUid2GisObject.put(object.getId(), object);
	}
	
	public boolean isBuildingsLayer () { return typeOfObjectsInside == GisConstants.GISLAYERTYPE.BUILDINGS; }
	
	public boolean isRoadsLayer () { return typeOfObjectsInside == GisConstants.GISLAYERTYPE.ROADS; }
	
	public String getLayerName(){
		return this.name;
	}	

	public Map<Long,GisObject> getObjects(){
		return this.mapUid2GisObject;
	}
}
