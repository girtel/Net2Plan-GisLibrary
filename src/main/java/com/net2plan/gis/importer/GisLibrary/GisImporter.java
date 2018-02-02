package com.net2plan.gis.importer.GisLibrary;

import java.awt.geom.Point2D;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import javax.json.JsonArray;
import javax.json.JsonReader;
import java.util.*;
//
import javax.json.*;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.*;

public class GisImporter
{	
	Map<String, GisMultilayer> gmlList = new HashMap<String, GisMultilayer>();
	
	// Metodo para cargar un geojson y crear GisMultilayer
	public GisMultilayer load(String name, List<File> paths) throws FileNotFoundException, IOException {
		GisMultilayer gml = null;
		ObjectMapper objectMapper = new ObjectMapper();

		ListIterator<File> fileIterator = paths.listIterator();
		while (fileIterator.hasNext()) {
			File path = fileIterator.next();

			GeoJSONParser parser = objectMapper.readValue(path, GeoJSONParser.class);
			System.out.println("Loaded " + parser.name + " from " + name);
			List coordinatesList = (List) parser.features.get(0).geometry.coordinates;
			//List point = (List) al.get(0);
			//Point2D point2d = new Point2D.Double((double)point.get(0), (double)point.get(1));
			//System.out.println(point2d.getX()+" "+point2d.getY());
			
			/*System.out.println(coordinatesList.get(0));
			List coordinatesList1 = (List) coordinatesList.get(0);
			ListIterator<List> litr1 = coordinatesList1.listIterator();
			while (litr1.hasNext()) {
				List point = (List) litr1.next();
				Point2D point2d = new Point2D.Double((double) point.get(0), (double) point.get(1));
				System.out.println(point2d.getX()+" "+point2d.getY());
			}
			*/
			
			
			System.out.println("#*#*#*#*#*#*#*#*#*#*#*#*#*#*#*#*#*#*");
			/*ListIterator<String> iterator = ls.listIterator();
			while(iterator.hasNext()){
				System.out.println(iterator.next());

					System.out.println("#*#*#*#*#*#*#*#*#*#*#*#*#*#*#*#*#*#*");
				}*/
			// Creación de GisMultilayer
			if (!this.gmlList.containsKey(name)) {
				gml = new GisMultilayer(name);
				this.gmlList.put(name, gml);
			} else {
				gml = this.gmlList.get(name);
			}

			// Creación de GisLayer
			GisLayer gl = new GisLayer(gml, parser); // gl conoce su gml
			gml.addLayer(gl);
			// Creación de GisObjects
			List<GisObject> objects = new ArrayList<GisObject>();
			ListIterator<GeoJSONFeature> objectsIterator = parser.features.listIterator(); // iterador
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

		return gml;
	}
	/*	
	//prueba
	System.out.println("Type: " + cfj.type);
	System.out.println("Name: " + cfj.name);
	System.out.println("Crs:{ type: " + cfj.crs.type+", properties: { "+ cfj.crs.properties.get("name")+"}}");
	System.out.println("Features: {type: " + cfj.features.get(0).type+", properties: "+cfj.features.get(0).properties.get("id")+
			", "+cfj.features.get(0).properties.get("building")+", "+cfj.features.get(0).geometry.get("coordinates"));
	
	System.out.println("Features: {type: " + cfj.features.get(1).type+", properties: "+cfj.features.get(1).properties.get("id")+
			", "+cfj.features.get(1).properties.get("building")+", "+cfj.features.get(1).geometry.get("coordinates"));
	*/
	
	public static void main(String[] args) throws FileNotFoundException, IOException
	{
		GisImporter gi = new GisImporter();
		List<File> files = new ArrayList<File>();
		File path = new File("C:/Users/jlrg_/Desktop/UPCT/QGIS/OSM2QGIS/Proyecto/GisLibrary/src/main/resources/Edificios.geojson");
		File path1 = new File("C:/Users/jlrg_/Desktop/UPCT/QGIS/OSM2QGIS/Proyecto/GisLibrary/src/main/resources/Carreteras.geojson");
		files.add(path);
		files.add(path1);
		GisMultilayer gml = gi.load("Cartagena", files);
		files.clear();
		files.add(path1);
		GisMultilayer gml1 = gi.load("Lorca", files);
		
		System.out.println("#*#*#*#*#*#*#*#*#*#*#*#*#*#*#*#*#*#*");
		System.out.println("GML name: "+gml.getName());
		System.out.println("Layer names del gml "+gml.getLayerNames());
		System.out.println("#*#*#*#*#*#*#*#*#*#*#*#*#*#*#*#*#*#*");
		//System.out.println("GML name: "+gml1.getName());
		//System.out.println("Layer names del gml "+gml1.getLayerNames());
		/*System.out.println("Es building layer?: "+ gml.getLayer("Carreteras").isBuildingsLayer()+", es road layer?: "+ gml.getLayer("Carreteras").isRoadsLayer());
		List<GisObjectPablo> objects = gml.getLayer("Carreteras").objects;
		System.out.println("#*#*#*#*#*#*#*#*#*#*#*#*#*#*#*#*#*#*");
		System.out.println(objects.get(0).getGml());
		System.out.println(objects.get(0).getGml());
		ListIterator<GisObjectPablo> litr = objects.listIterator();
		while(litr.hasNext()){
			GisObjectPablo object = litr.next();
				System.out.println(object.getGml().name);
				System.out.println(object.getGl().name);
				System.out.println("#*#*#*#*#*#*#*#*#*#*#*#*#*#*#*#*#*#*");
			}
		System.out.println();*/
		
	}
	
}