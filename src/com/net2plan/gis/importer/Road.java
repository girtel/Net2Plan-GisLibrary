package com.net2plan.gis.importer;

import java.awt.geom.Point2D;
import java.util.List;

public class Road extends GisObjectPablo
{

	public Road(GisLayer belongingLayer, String idAccordingToGeoJsonFile, List<Point2D> track)
	{
		super(belongingLayer, idAccordingToGeoJsonFile, track);
		// TODO Auto-generated constructor stub
	}
	public Point2D getOriginPoint () { return this.getTrack().get(0); }
	public Point2D getEndPoint () { return this.getTrack().get(this.getTrack().size()-1); }
	public List<Point2D> getTrack () { return this.getTrack(); }
	public boolean hasTrack () { return !this.getTrack().isEmpty(); } 
}


