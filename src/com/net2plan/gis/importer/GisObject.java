package net2plan.gis.importer;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class GisObject 
{
	String rawData;
	Map<String, String> properties = new HashMap<String,String>();
	Map<String, String> geometry = new HashMap<String,String>();

	public GisObject(String line)
	{
		int index1;
		int index2;
		this.rawData = line;
		
		
		//Aqui tenemos una linea de contenido.
		//{ "type": "Feature", 
		//"properties": { "id": 226756226, "building": "yes" }, 
		//"geometry": { "type": "Polygon", "coordinates": [] } },
		
		line = line.replace("\"", "");
		
		//System.out.println(line);
		//******************************************************************
		//System.out.println("#### Primer substring");
		
		index1 = line.indexOf("properties: { "); // 17 caracteres
		index2 = line.indexOf(" }, g"); // 3 caracteres
		
		String sline = line.substring(index1+14, index2);
		//System.out.println(sline); //solamente las propiedades
		
		//System.out.println("#### Primer split");
		String[] splittedProperties = sline.split(", "); //tenemos las propiedades separadas
		
		/*for(int i=0; i<parts.length;i++){
			System.out.println(parts[i]);
		}*/
		
		String[] partsProperties;
		//System.out.println("#### Segundo split");
		for(int i=0; i<splittedProperties.length;i++){
			partsProperties = splittedProperties[i].split(": ");
			properties.put(partsProperties[0], partsProperties[1]);
			//System.out.println("Primer término "+p+" segundo término "+parts1[1]);
		}
		/*// Imprimimos el Map con un Iterador
		Iterator it = properties.keySet().iterator();
		while(it.hasNext()){
		  String key = (String) it.next();
		  System.out.println("Clave: " + key + " -> Valor: " + properties.get(key));
		}*/
		
		//******************************************************************
		//System.out.println("#####################################");
		//System.out.println("#### Segundo substring");
		
		index1= line.indexOf("geometry: { "); // 17 caracteres
		index2 = line.indexOf(" } }"); // 3 caracteres
		
		String l1 = line.substring(index1+12, index2);
		//System.out.println(l1); //solamente geometry
		
		//System.out.println("#### Primer split");
		index1 = l1.indexOf("type: ");
		index2 = l1.indexOf(", coordinates: ");
		geometry.put("type", l1.substring(index1+6, index2));
		//System.out.println(geometry.get("type"));
		
		geometry.put("coordinates", l1.substring(index2+15));
		//System.out.println(geometry.get("coordinates"));
	}
	
	public String getRawData(){
		return rawData;
	}

	//get properties
	public Map<String,String> getProperties(){
		return properties;
	}
	
	//get geometry
	public Map<String,String> getGeometry(){
		return geometry;
	}
	
	//get geometryType
	public String getGeometryType(){
		return geometry.get("type");
	}
	
	//get geometryCoordinates
	public String getCoordinates(){
		return geometry.get("coordinates");
	}
	
}
