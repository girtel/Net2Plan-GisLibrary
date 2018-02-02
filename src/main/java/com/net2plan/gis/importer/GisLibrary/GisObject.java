package com.net2plan.gis.importer.GisLibrary;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

public class GisObject
{
	private final GisMultilayer gml;
	private final GisLayer gl;
	private final long id;
	private final GeoJSONFeature jlf;
	private  List<Point2D> track;
	private String type;
	private Map<String,String> properties;
	private String geometryType;
	
	//private final String idAccordingToGeoJsonFile;
	//private final long internalUniqueId; // unique at GisMultilayer level
	//private List<Point2D> track;

	public GisObject(GeoJSONFeature object, GisLayer belongingLayer, long id /*, String idAccordingToGeoJsonFile , List<Point2D> track*/)
	{
		super();
		this.gml = belongingLayer.getGml ();
		this.gl = belongingLayer;
		this.id = id;
		this.jlf = object;
		this.type = object.type;
		this.properties = object.properties;
		this.geometryType = object.geometry.type;
		this.track = setTrack();
		//this.idAccordingToGeoJsonFile = idAccordingToGeoJsonFile;
		//this.internalUniqueId = gml.getNewUniqueId ();
		//this.track = new ArrayList<> (track);
	}
	
	
	public GisMultilayer getGml(){return gml;}
	
	public GisLayer getGl(){return gl;}
	
	public long getId(){return id;}
	
	public List<Point2D> getTrack () {return track;}

	public List<Point2D> setTrack () { 
		List<Point2D> track = new ArrayList<Point2D>();
		List coordinatesList = (List) jlf.geometry.coordinates;
		if(this.gl.isBuildingsLayer()){
			List coordinatesList1 = (List) coordinatesList.get(0);
			ListIterator<List> litr = coordinatesList1.listIterator();
			while (litr.hasNext()) {
				List point = (List) litr.next();
				Point2D point2d = new Point2D.Double((double) point.get(0), (double) point.get(1));
				track.add(point2d);
			}	
		}else if(this.gl.isRoadsLayer()){
			ListIterator <List> litr = coordinatesList.listIterator();
			while(litr.hasNext()){
				List point = (List) litr.next();
				Point2D point2d = new Point2D.Double((double)point.get(0), (double)point.get(1));
				track.add(point2d);
			}
		}
		return track;
	}
}
