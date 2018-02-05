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
			
			//print
			System.out.println(name+": "+parser.name+" layer loaded.");
			
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
}