package com.net2plan.gis.importer.GisLibrary;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Optional;
import java.util.SortedMap;

public class GisObject implements Comparable <GisObject>
{
	private final GisMultilayer gml;
	private final GisLayer gl;
	private final long id;
	private final GeoJSONParser.GeoJSONFeature gjsonfeatures;
	private final String type;
	private final String geometryType;
	private SortedMap<String,String> properties;
	private final List<Point2D> track;

	
	//private final String idAccordingToGeoJsonFile;

	public GisObject(GeoJSONParser.GeoJSONFeature object, GisLayer belongingLayer, long id)
	{
		super();
		this.gml = belongingLayer.getGml ();
		this.gl = belongingLayer;
		this.id = id;
		this.gjsonfeatures = object;
		this.type = object.type;
		this.properties = object.properties;
		this.geometryType = object.geometry.type;
		this.track = setTrack();
		//this.idAccordingToGeoJsonFile = idAccordingToGeoJsonFile;
	}
	
	
	public GisMultilayer getGml(){return gml;}
	
	public GisLayer getGl(){return gl;}
	
	public long getId(){return id;}
	
	public List<Point2D> getTrack () {return track;}

	public List<Point2D> setTrack () { 
		
		List<Point2D> track = new ArrayList<Point2D>();
		List coordinatesList = (List) gjsonfeatures.geometry.coordinates;
		
		if(this.gl.isRoadsLayer()){
			ListIterator <List> litr = coordinatesList.listIterator();
			while(litr.hasNext()){
				List point = (List) litr.next();
				Point2D point2d = new Point2D.Double((double)point.get(0), (double)point.get(1));
				track.add(point2d);
			}
		}else if(this.gl.isBuildingsLayer()){
			List coordinatesList1 = (List) coordinatesList.get(0);
			ListIterator<List> litr = coordinatesList1.listIterator();
			while (litr.hasNext()) {
				List point = (List) litr.next();
				Point2D point2d = new Point2D.Double((double) point.get(0), (double) point.get(1));
				track.add(point2d);
			}
		}else if(this.gl.isLuminairesLayer()){
			List point = coordinatesList;
			Point2D point2d = new Point2D.Double((double) point.get(0), (double) point.get(1));
			track.add(point2d);
			}
		return track;
	}

	public GeoJSONParser.GeoJSONFeature getGeoJSONFeatures() {return gjsonfeatures;}

	public String getType() {return type;}

	public SortedMap<String, String> getProperties() {return Collections.unmodifiableSortedMap(this.properties);}

	public String getGeometryType() {return geometryType;}

	public void addProperty(String propertyName, String propertyValue){
		this.properties.put(propertyName, propertyName);
	}
	
	public void removeProperty(String propertyName, String propertyValue){
		this.properties.remove(propertyName, propertyName);
	}
	
	public Optional<String> getProperty (String propertyName){
		return Optional.of(this.properties.get(propertyName));
	}

	public double getPropertyAsDouble(String propertyName, double defaultValue) {
		try {
			return Double.parseDouble(properties.get(propertyName));
		} catch (Exception e) {
			return defaultValue;
		}

	}

	@Override
	public int compareTo(GisObject object) {
		final int cLayers = this.getGl().compareTo (object.getGl());
		if (cLayers != 0) return cLayers;
		 return Long.compare(this.getId () , object.getId ()); 

	}
}
