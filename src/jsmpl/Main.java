package jsmpl;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import jsmpl.direction.PiecewiseMusicalDirection;
import jsmpl.entity.Entity;
import jsmpl.function.Generator;
import jsmpl.function.Waveform;
import jsmpl.io.WAVFileOutputStream;
import jsmpl.musicxml.MusicXML;
import jsmpl.score.Instrument;
import jsmpl.score.Layer;
import jsmpl.score.Score;

public class Main {
	public static void main(String[] args) {
		String xmlFile = "direction-test.xml";
		
		Entity timbre = new Entity(Waveform.SINE, 0.5, 0.5, 0, 0);  // create an entity to produce sound
		try {
			Map<String, Map<String, Object>> scoreAttr = MusicXML.parseXML(new File(xmlFile), null);
			Score score = new Score();   // initialize score
			
			for (String partID : scoreAttr.keySet()) {
				Map<String, Object> attr = scoreAttr.get(partID);
				String partName = (String) attr.get("partName");  // retrieve score attributes
				Layer[] partData = (Layer[]) attr.get("partData");
				PiecewiseMusicalDirection directionData = 
						(PiecewiseMusicalDirection) attr.get("directionData");
				
				if (partData == null) {
					continue;
				}
				
				Instrument instrument = new Instrument(partName, timbre, partData.length);
				for (int i = 0; i < partData.length; i++) {
					instrument.setVoice(partData[i], i);  // set voices to part data from xml
					System.out.printf("%d | %s\n", i, partData[i].toString());
				}
				
				score.add(instrument, directionData);
			}
			
			Map<Integer, Generator<List<Double[][]>>> sampleGeneratingMap = 
					score.developInstrumentsAsReference(44100.0, 0);
			
			for (int partKey : sampleGeneratingMap.keySet()) {
				Generator<List<Double[][]>> generator = sampleGeneratingMap.get(partKey);
				
				List<Double[][]> samples = generator.generate();
				Double[][] sampleData = samples.get(0);
				
				double maxSampleValue = 0.0;
				for (int i = 1; i < samples.size(); i++) {
					Double[][] other = samples.get(i);
					
					for (int ii = 0; ii < sampleData.length && ii < other.length; ii++) {
						for (int jj = 0; jj < sampleData[ii].length && jj < other[ii].length; jj++) {
							sampleData[ii][jj] += other[ii][jj];
							
							if (sampleData[ii][jj] > maxSampleValue) {
								maxSampleValue = sampleData[ii][jj];  // we want to keep track of this for normalization.
							}
						}
					} 
				}
				
				if (maxSampleValue > 0.0) {
					for (int i = 0; i < sampleData.length; i++) {
						for (int j = 0; j < sampleData[i].length; j++) {
							sampleData[i][j] /= maxSampleValue;  // normalize here
						}
					}
				}
				
				System.out.println("Writing to file for part " + partKey + ".");
				WAVFileOutputStream ostream = new WAVFileOutputStream("test_" + partKey + ".wav", 2);
				ostream.write(sampleData);
				ostream.close();
			}

			System.out.println("Done.");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
