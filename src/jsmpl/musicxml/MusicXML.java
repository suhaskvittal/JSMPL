package jsmpl.musicxml;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import jsmpl.function.DirectionShapeFunction;
import jsmpl.score.Chord;
import jsmpl.score.Layer;
import jsmpl.score.Note;
import jsmpl.score.ext.PaddedNote;
import jsmpl.score.ext.SequentialNote;
import jsmpl.util.Dynamics;

public class MusicXML {
	private static Map<String, Object> PARSE_XML_DEFAULT_KWARGS = new HashMap<String, Object>();
	private static final int LAYERS_PER_STAFF = 6;
	static {
		// initialize any static variables if necessary
		PARSE_XML_DEFAULT_KWARGS.put("musicalDirectionShape", DirectionShapeFunction.linear);
		PARSE_XML_DEFAULT_KWARGS.put("verbose", 0);
	}
	
	public static Map<String, Map<String, Object>> parseXML(File file, 
			Map<String, Object> kwargs) throws Exception 
	{
		DocumentBuilder docBuilder = DocumentBuilderFactory
										.newInstance()
										.newDocumentBuilder(); 
		Document document = docBuilder.parse(file);
		Element root = document.getDocumentElement();
		
		// initialize some variables to use
		Map<String, Map<String, Object>> scoreAttr = new HashMap<String, Map<String, Object>>();
		Map<Integer, Map<String, Object>> globalAttr = new HashMap<Integer, Map<String, Object>>();
		boolean firstPart = true;
		
		// check kwargs
		if (kwargs == null) {
			kwargs = new HashMap<String, Object>();
		}
		
		for (String key : PARSE_XML_DEFAULT_KWARGS.keySet()) {
			if (!kwargs.containsKey(key)) {
				kwargs.put(key, PARSE_XML_DEFAULT_KWARGS.get(key));
			}
		}
		
		NodeList childNodes = root.getChildNodes();
		ExecutorService es = Executors.newCachedThreadPool();
		
		for (int i = 0; i < childNodes.getLength(); i++) {
			Node childNode = childNodes.item(i);
			if (childNode.getNodeType() == Node.ELEMENT_NODE) {
				Element child = (Element) childNode;
				
				if (child.getTagName().contentEquals("part-list")) {
					NodeList scorePartList = child.getElementsByTagName("score-part");
					for (int j = 0; j < scorePartList.getLength(); j++) {
						Node scorePartNode = scorePartList.item(j);
						if (scorePartNode.getNodeType() == Node.ELEMENT_NODE) {
							Element scorePart = (Element) scorePartList.item(j);
							
							String partId = scorePart.getAttribute("id");
							String partName = scorePart.getElementsByTagName("part-name")
												.item(0).getTextContent();
							Map<String, Object> attr = new HashMap<String, Object>();
							attr.put("partName", partName);  // value is String
							attr.put("partData", null);  // value is Layer[]
							attr.put("directionData", null);  // value is PiecewiseMusicalDirection
							
							scoreAttr.put(partId, attr);
						}
					}
				} else if (child.getTagName().contentEquals("part")) {
					String id = child.getAttribute("id");
					
					Thread thread = new MusicXMLParsingThread(scoreAttr.get(id), globalAttr,
							kwargs, firstPart, child);
					if (firstPart) {
						thread.start();
						thread.join();
					} else {
						es.execute(thread);
					}
					
					firstPart = false;
				}
			}
		}
		
		System.out.println("Outside thread creator.");
		es.shutdown();
		boolean terminated = es.awaitTermination(1, TimeUnit.MINUTES);
		System.out.println("terminated: " + terminated);
		if (!terminated) {
			throw new RuntimeException("The parser timed out after 1 minute.");
		} else {
			System.out.println(scoreAttr);
			return scoreAttr;
		}
	} 
	
	private static String computeCanonicalRepresentation(char pitchLetter, int pitchAdjust, 
			int pitchOctave) 
	{
		if (pitchAdjust > 1) {
			if (pitchLetter == 'E') {
				return computeCanonicalRepresentation('F', pitchAdjust - 1, pitchOctave);
			} else if (pitchLetter == 'B') {
				return computeCanonicalRepresentation('C', pitchAdjust - 1, pitchOctave + 1);
				// add one to octave because BN -> C(N+1) (e.g. B4 -> C5)
			} else {
				char neighborLetter = (char) (((pitchLetter - 'A') + 1) % 7 + 'A');
				return computeCanonicalRepresentation(neighborLetter, pitchAdjust - 2, pitchOctave);
			}
		} else if (pitchAdjust == 1) {
			if (pitchLetter == 'E') {
				return "F" + pitchOctave;
			} else if (pitchLetter == 'B') {
				return "C" + (pitchOctave + 1);
			} else {
				return pitchLetter + "+" + pitchOctave;
			}
		} else if (pitchAdjust == -1) {
			if (pitchLetter == 'F') {
				return "E" + pitchOctave;
			} else if (pitchLetter == 'C') {
				return "B" + (pitchOctave + 1);
			} else {
				return pitchLetter + "-" + pitchOctave;
			}
		} else if (pitchAdjust < -1) {
			if (pitchLetter == 'F') {
				return computeCanonicalRepresentation('E', pitchAdjust + 1, pitchOctave);
			} else if (pitchLetter == 'C') {
				return computeCanonicalRepresentation('B', pitchAdjust + 1, pitchOctave - 1);
			} else {
				char neighborLetter = (char) (((pitchLetter - 'A') + 6) % 7 + 'A');
				// note that 6 = -1	mod 7.
				return computeCanonicalRepresentation(neighborLetter, pitchAdjust + 2, pitchOctave);
			} 
		} else {
			return pitchLetter + "" + pitchOctave;
		}
	}
	
	private static void readSoundDefinition(Element soundXML, Map<String, Object> description) {
		NamedNodeMap attributes = soundXML.getAttributes();
		for (int i = 0; i < attributes.getLength(); i++) {
			Node attributeNode = attributes.item(i);
			
			if (attributeNode.getNodeType() == Node.ATTRIBUTE_NODE) {
				Attr attribute = (Attr) attributeNode;
				String attrName = attribute.getName();
				String value = attribute.getValue();
				
				description.put(attrName, Double.parseDouble(value));
			}
		}
	}
	
	private static void readDirTypeDefinition(Element dirXML, Map<String, Object> description) {
		NodeList dirChildList = dirXML.getChildNodes();
		
		for (int i = 0; i < dirChildList.getLength(); i++) {
			Node dirChildNode = dirChildList.item(i);
			
			if (dirChildNode.getNodeType() == Node.ELEMENT_NODE) {
				Element dirChild = (Element) dirChildNode;
				if (dirChild.getTagName().contentEquals("wedge")) {
					String wedgeType = dirChild.getAttribute("type");
					
					if (wedgeType.contentEquals("crescendo")) {
						description.put("direction", 1);
					} else if (wedgeType.contentEquals("diminuendo")) {
						description.put("direction", -1);
					} else {
						description.put("direction", 0);
					}
				}
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	private static void pushChordBuffer(Layer[] measureLayers, ArrayList<Note>[] chordBuffers, int index, Map<String, Object> description) {
		if (chordBuffers[index] == null || chordBuffers[index].isEmpty()) {
			return;
		}
		
		Map<Integer, List<Note>> tieSuppStack = (Map<Integer, List<Note>>) description.get("tieSuppStack");
		if (tieSuppStack.containsKey(index) && !tieSuppStack.get(index).isEmpty()) {
			double tailPadLength = chordBuffers[index].get(0).getLength();
			
			while (!tieSuppStack.get(index).isEmpty()) {
				Note e = tieSuppStack.get(index).remove(tieSuppStack.get(index).size() - 1);
				Note ne = new PaddedNote(e.getFrequency(), e.getLength(), e.getVolume(), 0.0, tailPadLength);
				
				chordBuffers[index].add(ne);
			}
		}
		
		Note[] tmpBuffer = new Note[chordBuffers[index].size()];
		chordBuffers[index].toArray(tmpBuffer);
		Chord ch = new Chord(tmpBuffer);
		if (measureLayers[index] == null) {
			measureLayers[index] = new Layer(); // initialize if nonexistent.
		}
		measureLayers[index].add(ch);
		
		chordBuffers[index].clear();  // empty the buffer.
	}
	
	@SuppressWarnings("unchecked")
	private static Note applyTieMorph(Note note, int index, Map<String, Object> description) {
		Map<Integer, List<Note>> tmp = (Map<Integer, List<Note>>) description.get("tieSuppStack");
		if (!tmp.containsKey(index)) {
			tmp.put(index, new ArrayList<Note>());
		}
		
		List<Note> arr = tmp.get(index);
		Note newNote;
		if (arr.size() == 0) {
			Map<Integer, Double> tieSize = (Map<Integer, Double>) description.get("tieSize");
			double headPadSize = tieSize.containsKey(index) ? tieSize.get(index) : 0;
			newNote = new PaddedNote(note.getFrequency(), note.getLength(), note.getVolume(), headPadSize, 0.0);
		} else {
			SequentialNote last = (SequentialNote) arr.remove(arr.size() - 1);
			last.setSecondNote(note);
			newNote = last;
		}
		
		return newNote;
	}
	
	private static MusicXMLNoteWrapper parseNoteXML(Element noteXML, Map<String, Object> description) {
		double noteLength = 0;
		String notePitch = null;
		
		MusicXMLNoteWrapper noteWrapper = new MusicXMLNoteWrapper();
		
		NodeList noteChildList = noteXML.getChildNodes();
		for (int i = 0; i < noteChildList.getLength(); i++) {
			Node noteChildNode = noteChildList.item(i);
			
			if (noteChildNode.getNodeType() == Node.ELEMENT_NODE) {
				Element noteChild = (Element) noteChildNode;
				String noteChildTag = noteChild.getTagName();
				
				if (noteChildTag.contentEquals("grace")) {
					noteLength = (60.0 / ((Double) description.get("tempo"))) * 1.0/8.0;
					noteWrapper.grace = 1;
				} else if (noteChildTag.contentEquals("pitch")) {
					String pitchLetter = noteChild.getElementsByTagName("step").item(0).getTextContent();
					String pitchOctave = noteChild.getElementsByTagName("octave").item(0).getTextContent();
					
					Node pitchAdjustNode = noteChild.getElementsByTagName("alter").item(0);  // it may or may not exist
					String pitchAdjust;
 					if (pitchAdjustNode == null) {
 						pitchAdjust = "0";
 					} else {
 						pitchAdjust = pitchAdjustNode.getTextContent();
 					}
					
					notePitch = computeCanonicalRepresentation(pitchLetter.charAt(0), 
																Integer.parseInt(pitchAdjust),
																Integer.parseInt(pitchOctave));
				} else if (noteChildTag.contentEquals("duration")) {
					noteLength = 60.0 / ((Double) description.get("tempo")) 
									* Double.parseDouble(noteChild.getTextContent()) / (Integer) description.get("divisions");
				} else if (noteChildTag.contentEquals("voice")) {
					noteWrapper.voiceNumber = Integer.parseInt(noteChild.getTextContent()) - 1;
				} else if (noteChildTag.contentEquals("staff")) {
					noteWrapper.staffNumber = Integer.parseInt(noteChild.getTextContent()) - 1;
				} else if (noteChildTag.contentEquals("chord")) {
					noteWrapper.chord = 1;
				} else if (noteChildTag.contentEquals("tie")) {
					String tieType = noteChild.getAttribute("type");
					
					if (tieType.contentEquals("start")) {
						noteWrapper.tieStart = 1;
					} else {
						noteWrapper.tieStop = 0;
					}
				}
			}
		}
		
		double amplitude = Dynamics.computeAmplitude((Double) description.get("dynamics"));
		Note note;
		if (notePitch == null) {
			note = new Note(0.0, noteLength);
		} else {
			note = new Note(notePitch, noteLength, amplitude);
		}
		
		noteWrapper.note = note;
		return noteWrapper;
	}
	
	@SuppressWarnings("unchecked")
	static Layer[] parseMeasureXML(Element measure, Map<String, Object> description,
								Map<Integer, Map<String, Object>> globalAttr, boolean updateGlobalSettings, int verbose) 
	{
		int measureNumber = Integer.parseInt(measure.getAttribute("number"));
		double measurePosition = (Double) description.get("position");
		
		if (!updateGlobalSettings) {
			Map<String, Object> msrGlobalAttr = globalAttr.get(measureNumber);
			for (String key : msrGlobalAttr.keySet()) {
				description.put(key, msrGlobalAttr.get(key));
			}
		}
		
		int layerCount = (Integer) description.get("layerCount");
		Layer[] measureLayers = new Layer[layerCount];
		ArrayList<Note>[] chordBuffers = (ArrayList<Note>[]) new ArrayList[layerCount];
		int[] layerLocalPosition = new int[layerCount];
		int[] insideTieStart = new int[layerCount];
		int[] insideTieStop = new int[layerCount];
		
		NodeList measureChildList = measure.getChildNodes();
		
		for (int i = 0; i < measureChildList.getLength(); i++) {
			Node measureChildNode = measureChildList.item(i);
			
			if (measureChildNode.getNodeType() == Node.ELEMENT_NODE) {
				Element measureChild = (Element) measureChildNode;
				String measureChildTag = measureChild.getTagName();
				
				if (measureChildTag.contentEquals("attributes")) {
					NodeList attributeChildList = measureChild.getChildNodes();
					for (int j = 0; j < attributeChildList.getLength(); j++) {
						Node attrChildNode = attributeChildList.item(j);
						
						if (attrChildNode.getNodeType() == Node.ELEMENT_NODE) {
							Element attrChild = (Element) attrChildNode;
							String attrChildTag = attrChild.getTagName();
							
							if (attrChildTag.contentEquals("divisions")) {
								description.put("divisions", 
										Integer.parseInt(attrChild.getTextContent()));
							} else if (attrChildTag.contentEquals("staves")) {
								description.put("staves", 
										Integer.parseInt(attrChild.getTextContent()));
							} else if (attrChildTag.contentEquals("time")) {
								String beatText = attrChild.getElementsByTagName("beats")
													.item(0).getTextContent();
								String beatTypeText = attrChild.getElementsByTagName("beat-type")
													.item(0).getTextContent();
								description.put("timeSignature", new int[] { 
									Integer.parseInt(beatText),
									Integer.parseInt(beatTypeText)
								});
							}
						}
					}
					
					// initialize measure layers and chord buffers now
					// because of new definition
					
					layerCount = LAYERS_PER_STAFF * ((Integer) description.get("staves"));
					measureLayers = new Layer[layerCount];
					chordBuffers = (ArrayList<Note>[]) new ArrayList[layerCount];
					layerLocalPosition = new int[layerCount];
					insideTieStart = new int[layerCount];
					insideTieStop = new int[layerCount];
					
					description.put("layerCount", layerCount);
				} else if (measureChildTag.contentEquals("direction")) {
					NodeList directionChildList = measureChild.getChildNodes();
					
					for (int j = 0; j < directionChildList.getLength(); j++) {
						Node directionChildNode = directionChildList.item(j);
						
						if (directionChildNode.getNodeType() == Node.ELEMENT_NODE) {
							Element directionChild = (Element) directionChildNode;
							String directionChildTag = directionChild.getTagName();
							
							if (directionChildTag.contentEquals("sound")) {
								readSoundDefinition(directionChild, description);
							} else if (directionChildTag.contentEquals("direction-type")) {
								readDirTypeDefinition(directionChild, description);
							}
						}
					}
				} else if (measureChildTag.contentEquals("sound")) {
					readSoundDefinition(measureChild, description);
				} else if (measureChildTag.contentEquals("note")) {
					MusicXMLNoteWrapper noteWrapper = parseNoteXML(measureChild, description);
				
					int layerBucketIndex = noteWrapper.staffNumber * LAYERS_PER_STAFF + noteWrapper.voiceNumber;
					boolean hasBeenMorphed = false;  
					// when morphing notes from Note to SequentialNote or PaddedNote
					// we don't want to double morph.
					
					double layerGlobalPosition = measurePosition + layerLocalPosition[layerBucketIndex];
					if (noteWrapper.chord == 0) {
						if (noteWrapper.tieStop == 0 && insideTieStart[layerBucketIndex] == 0) {
							((Map<Integer, Double>) description.get("tiePosition"))
								.put(layerBucketIndex, layerGlobalPosition);
							((Map<Integer, Double>) description.get("tieSize"))
								.put(layerBucketIndex, 0.0);
						}
						
						if ((Double) description.get("position") == 0 || 
								(Double) description.get("position") < layerGlobalPosition)
						{
							System.out.println("yeet");
							description.put("position", layerGlobalPosition);
							
							// add to direction queue
							int dirSlope = (Integer) description.get("direction");
							Object dqv = dirSlope == 1 ? "c" : (dirSlope == -1 ? "d" : description.get("dynamics"));
							((LinkedList<DirectionQueueElement>) description.get("directionQueue"))
								.addLast(new DirectionQueueElement(dqv, (Double) description.get("position")));	
						}
						
						layerLocalPosition[layerBucketIndex] += noteWrapper.note.getLength();
					}
					
					if (noteWrapper.tieStop == 0 || noteWrapper.chord == 0) {
						insideTieStop[layerBucketIndex] = 0;
					}
					
					if (noteWrapper.grace == 1) {  // add space in each voice to accomodate for the grace note
						for (int j = 0; j < measureLayers.length; j++) {
							if (j != layerBucketIndex) {
								measureLayers[j].add(new Note(0.0, noteWrapper.note.getLength()));
								layerLocalPosition[j] += noteWrapper.note.getLength();
							}
						}
					}
					
					if (noteWrapper.tieStop == 1) {
						// finding a matching element to the tie in the main stack
						List<Note> arr = ((Map<Integer, List<Note>>) description.get("tieMainStack")).get(layerBucketIndex);
						for (int j = 0; j < arr.size(); j++) {
							Note other = arr.get(j);
							
							if (noteWrapper.note.getFrequency() == other.getFrequency()) {
								double tmpTimeLength = noteWrapper.note.getLength();
								noteWrapper.note = new Note(other.getFrequency(), tmpTimeLength + other.getLength(), other.getVolume());
								hasBeenMorphed = true;
								arr.remove(j);
								break;  // we have found a valid note, so stop.
							}
						}
						
						insideTieStop[layerBucketIndex] = 1;  // flip to tie stop
						insideTieStart[layerBucketIndex] = 0;  
					}
					
					if (insideTieStop[layerBucketIndex] == 1 && noteWrapper.tieStop != 1) {
						noteWrapper.note = applyTieMorph(noteWrapper.note, layerBucketIndex, description);
						hasBeenMorphed = true;
					}
					
					if (noteWrapper.tieStart == 1) {
						// first check if note should be morphed
						
						double tiePosition = ((Map<Integer, Double>) description.get("tiePosition")).get(layerBucketIndex);
						if (layerGlobalPosition > tiePosition + noteWrapper.note.getLength() && !hasBeenMorphed) {
							// there was a tie before
							noteWrapper.note = applyTieMorph(noteWrapper.note, layerBucketIndex, description);
						}
						
						Map<Integer, List<Note>> tieMainStack = (Map<Integer, List<Note>>) description.get("tieMainStack");
						if (!tieMainStack.containsKey(layerBucketIndex)) {
							tieMainStack.put(layerBucketIndex, new ArrayList<Note>());
						}
						tieMainStack.get(layerBucketIndex).add(noteWrapper.note);
						insideTieStart[layerBucketIndex] = 1;
						
						// update tie size and tie position
						if (noteWrapper.chord == 0) {
							((Map<Integer, Double>) description.get("tieSize")).put(layerBucketIndex, noteWrapper.note.getLength());
							((Map<Integer, Double>) description.get("tiePosition")).put(layerBucketIndex, layerGlobalPosition);
						}
						
						// clear the chord buffer in case of notes we added by accident
						if (noteWrapper.chord == 1) {
							Map<Integer, List<Note>> tieSuppStack = (Map<Integer, List<Note>>) description.get("tieSuppStack");
							if (chordBuffers[layerBucketIndex] == null) {
								chordBuffers[layerBucketIndex] = new ArrayList<Note>();
							}
							
							while (!chordBuffers[layerBucketIndex].isEmpty()) {
								Note e = chordBuffers[layerBucketIndex].remove(
											chordBuffers[layerBucketIndex].size() - 1);
								if (!tieSuppStack.containsKey(layerBucketIndex)) {
									tieSuppStack.put(layerBucketIndex, new ArrayList<Note>());
								}
								tieSuppStack.get(layerBucketIndex).add(new SequentialNote(e));
							}
						}
					}
					
					if (insideTieStart[layerBucketIndex] == 1 && noteWrapper.tieStart != 1) {
						if (noteWrapper.chord == 0) {
							noteWrapper.note = applyTieMorph(noteWrapper.note, layerBucketIndex, description);
							insideTieStop[layerBucketIndex] = 1;
						} else {
							noteWrapper.note = new SequentialNote(noteWrapper.note);
							Map<Integer, List<Note>> tieSuppStack = (Map<Integer, List<Note>>) description.get("tieSuppStack");
							if (!tieSuppStack.containsKey(layerBucketIndex)) {
								tieSuppStack.put(layerBucketIndex, new ArrayList<Note>());
							} 
							tieSuppStack.get(layerBucketIndex).add(noteWrapper.note);
						}
					}
					
					if (noteWrapper.chord == 0) {
						// update overall instrument position with max rule
						pushChordBuffer(measureLayers, chordBuffers, layerBucketIndex, description);
					}
					
					if (insideTieStart[layerBucketIndex] == 0) {
						if (chordBuffers[layerBucketIndex] == null) {
							chordBuffers[layerBucketIndex] = new ArrayList<Note>();
						}
						chordBuffers[layerBucketIndex].add(noteWrapper.note);
					}
				}
				
			}
		}
		
		// dump all buffers as measure is over
		for (int i = 0; i < chordBuffers.length; i++) {
			pushChordBuffer(measureLayers, chordBuffers, i, description);
		}
		
		// MUSICXML specs say that if there is no definition for a voice or a staff
		// in a measure, it is filled with rests. We have to check for this now.
		int[] timeSignature = (int[]) description.get("timeSignature");
		double msrBufferSize = 60.0 / ((Double) description.get("tempo")) 
								* ((timeSignature[0] * 4.0) / timeSignature[1]);
		for (int i = 0; i < measureLayers.length; i++) {
			double layerGlobalPosition = measurePosition + layerLocalPosition[i];
			if ((Double) description.get("position") < layerGlobalPosition) {
				description.put("position", layerGlobalPosition);
				
				int dirSlope = (Integer) description.get("direction");
				Object dqv = dirSlope == 1 ? "c" : (dirSlope == -1 ? "d" : description.get("dynamics"));
				((LinkedList<DirectionQueueElement>) description.get("directionQueue"))
					.add(new DirectionQueueElement(dqv, (Double) description.get("position")));
			}
			
			Map<Integer, Double> tieSize = (Map<Integer, Double>) description.get("tieSize");
			if (measureLayers[i] == null) {
				measureLayers[i] = new Layer(); // initialize if empty.
			}
			
			double layerSize = measureLayers[i].getSize() + (tieSize.containsKey(i) ? tieSize.get(i) : 0);
			double emptyBufferSpace = msrBufferSize - layerSize;
			
			if (emptyBufferSpace <= 0) {
				continue;
			}
			Note nullNote = new Note(0.0, emptyBufferSpace);
			measureLayers[i].add(nullNote);
			layerLocalPosition[i] += nullNote.getLength();
		}
		
		if (updateGlobalSettings) {
			globalAttr.put(measureNumber, new HashMap<String, Object>());
			globalAttr.get(measureNumber).put("tempo", description.get("tempo"));
		}
		
		return measureLayers;
	}
	
	private static class MusicXMLNoteWrapper {
		private Note note;
		private int voiceNumber;
		private int staffNumber;
		private int chord;
		private int grace;
		private int tieStart;
		private int tieStop;
		
		MusicXMLNoteWrapper() {}  // initialize everything to default values.
		
		@SuppressWarnings("unused")
		MusicXMLNoteWrapper(Note note, int vn, int sn, int c, int g, int tst, int tsp) {
			this.note = note;
			this.voiceNumber = vn;
			this.staffNumber = sn;
			this.chord = c;
			this.grace = g;
			this.tieStart = tst;
			this.tieStop = tsp;
		}
	}
	
}
