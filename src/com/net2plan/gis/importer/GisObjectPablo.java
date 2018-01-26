package com.net2plan.gis.importer;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GisObjectPablo
{
	private final GisMultilayer gml;
	private final String idAccordingToGeoJsonFile;
	private final GisLayer belongingLayer;
	private final long internalUniqueId; // unique at GisMultilayer level
	private List<Point2D> track;

	public GisObjectPablo(GisLayer belongingLayer , String idAccordingToGeoJsonFile , List<Point2D> track)
	{
		super();
		this.idAccordingToGeoJsonFile = idAccordingToGeoJsonFile;
		this.belongingLayer = belongingLayer;
		this.gml = belongingLayer.getGml ();
		this.internalUniqueId = gml.getNewUniqueId ();
		this.track = new ArrayList<> (track);
	}
	public GisMultilayer getGml()
	{
		return gml;
	}
	public String getIdAccordingToGeoJsonFile()
	{
		return idAccordingToGeoJsonFile;
	}
	public GisLayer getBelongingLayer()
	{
		return belongingLayer;
	}
	public long getInternalUniqueId()
	{
		return internalUniqueId;
	}
	public List<Point2D> getTrack () { return Collections.unmodifiableList(track); }
	
	
	
}
