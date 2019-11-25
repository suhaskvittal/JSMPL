package jsmpl.musicxml;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import jsmpl.direction.MusicalDirection;
import jsmpl.direction.PiecewiseMusicalDirection;
import jsmpl.function.DirectionShapeFunction;
import jsmpl.score.Layer;
import jsmpl.score.Note;
import jsmpl.util.Dynamics;

public class MusicXMLParsingThread extends Thread {
	private Map<String, Object> partAttrRef;
	private Map<Integer, Map<String, Object>> globalAttrRef;
	private Map<String, Object> kwargs;
	private boolean updateGlobalSettings;
	
	private Element root;
	
	private Map<String, Object> description; 
	// this field is always initialized the same. The idea
	// is that this field "describes" attributes of the 
	// music such as tempo, or dynamics, or whether or not 
	// there's a tie.
	
	
	/**
	 * @param partAttrRef a reference to value in scoreAttr in the appropriate partID key
	 * @param globalAttrRef a reference to the global attributes
	 * @param kwargs a dictionary of keywords to apply to functions in the thread
	 * @param updateGlobalSettings whether or not to update the global attributes.
	 * 	This should only be true when updating the first part.
	 * @param root where the thread should start parsing XML.
	 */
	public MusicXMLParsingThread(Map<String, Object> partAttrRef, Map<Integer, Map<String, Object>> globalAttrRef,
						Map<String, Object> kwargs, boolean updateGlobalSettings, Element root)
	{
		this.partAttrRef = partAttrRef;
		this.globalAttrRef = globalAttrRef;
		this.kwargs = kwargs;
		this.updateGlobalSettings = updateGlobalSettings;
		this.root = root;
		
		// now initialize description
		this.description = new HashMap<String, Object>();
		description.put("position", 0.0);
		description.put("staves", 1);
		description.put("layerCount", 4);
		description.put("divisions", 4);
		description.put("tempo", 60.0);
		description.put("dynamics", 60.0);
		description.put("timeSignature", new int[] {4, 4} );
		description.put("tieMainStack", new HashMap<Integer, List<Note>>());
		description.put("tieSuppStack", new HashMap<Integer, List<Note>>());
		description.put("tiePosition", new HashMap<Integer, Double>());
		description.put("tieSize", new HashMap<Integer, Double>());
		description.put("direction", 0);
		description.put("directionQueue", new LinkedList<DirectionQueueElement>());
	}
	
	@SuppressWarnings("unchecked")
	public void run() {
		NodeList measureNodeList = root.getElementsByTagName("measure");
		for (int i = 0; i < measureNodeList.getLength(); i++) {
			Node measureNode = measureNodeList.item(i);
			if (measureNode.getNodeType() == Node.ELEMENT_NODE) {
				Element measure = (Element) measureNodeList.item(i);
				
				Layer[] measureLayers = MusicXML.parseMeasureXML(measure, description, 
						globalAttrRef, updateGlobalSettings, (Integer) kwargs.get("verbose"));
				
				if (partAttrRef.get("partData") == null) {
					partAttrRef.put("partData", measureLayers); // initialize if empty
				} else {
					for (int j = 0; j < measureLayers.length; j++) {
						((Layer[]) partAttrRef.get("partData"))[j].concatenate(measureLayers[j]);
					}
				}
			}
		}
		
		partAttrRef.put("directionData", convertQueueToObject(
				(LinkedList<DirectionQueueElement>) description.get("directionQueue"),
				(DirectionShapeFunction) kwargs.get("musicalDirectionShape")));
	}
	
	private PiecewiseMusicalDirection convertQueueToObject(LinkedList<DirectionQueueElement> queue, 
			DirectionShapeFunction pfunc) 
	{
		List<MusicalDirection> arr = new ArrayList<MusicalDirection>();
		
		double initialDynamics = 0.0;
		double initialPosition = 0.0;
		int slope = 0;
		
		for (DirectionQueueElement e : queue) {
			if (e.dynamics instanceof String) {
				String dyn = (String) e.dynamics;
				slope = dyn.contentEquals("c") ? 1 : -1;
			} else {
				double finalDynamics = Dynamics.computeAmplitude((Double) e.dynamics);
				double finalPosition = e.position;
				
				if (slope == 1 && initialDynamics >= finalDynamics) {
					finalDynamics = 2.0 * initialDynamics; // adjust if there's a contradiction
				} 
				
				if (slope == -1 && initialDynamics <= finalDynamics) {
					finalDynamics = 0.5 * initialDynamics;
				}
				
				arr.add(new MusicalDirection(initialDynamics, finalDynamics,
						finalPosition - initialPosition, pfunc));
				
				initialDynamics = Dynamics.computeAmplitude((Double) e.dynamics);
				initialPosition = e.position;
			}
		}
		
		return new PiecewiseMusicalDirection(arr);
	}
}