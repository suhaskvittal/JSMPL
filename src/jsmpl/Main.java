package jsmpl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jsmpl.entity.Entity;
import jsmpl.entity.greedy.LinearGreedyEntity;
import jsmpl.entity.greedy.MemoryGreedyEntity;
import jsmpl.entity.greedy.PeriodicGreedyEntity;
import jsmpl.function.Waveform;
import jsmpl.io.pipeline.MusicXMLParserPipeline;
import jsmpl.io.pipeline.PipelineDeque;
import jsmpl.io.pipeline.ScorePlayerPipeline;
import jsmpl.io.pipeline.ScorePlayerPipeline.OutputMode;
import jsmpl.score.Instrument;
import jsmpl.score.Layer;
import jsmpl.score.Note;
import jsmpl.score.Score;

public final class Main {
	public static void entityBenchmark(String[] args) {
		Entity norm = new Entity(Waveform.SINE, 0.5, 0.5, 0, 0);
		Entity lingreed = new LinearGreedyEntity(norm, 5);
		Entity pergreed = new PeriodicGreedyEntity(norm);
		Entity mhnorm = MemoryGreedyEntity.hashMemory(norm);
		Entity mhlingreed = MemoryGreedyEntity.hashMemory(lingreed);
		Entity mhpergreed = MemoryGreedyEntity.hashMemory(pergreed);
		Entity mtnorm = MemoryGreedyEntity.treeMemory(norm);
		Entity mtlingreed = MemoryGreedyEntity.treeMemory(lingreed);
		Entity mtpergreed = MemoryGreedyEntity.treeMemory(pergreed);
		
		Entity[] test = { norm, lingreed, pergreed, 
							mhnorm, mhlingreed, mhpergreed, 
							mtnorm, mtlingreed, mtpergreed };
		
		for (int i = 0; i < test.length; i++) {
			long prevTime = System.currentTimeMillis();
			xmlTestWithEntity(test[i]);
			long afterTime = System.currentTimeMillis();
			
			System.out.printf("Entity %d took %d ms to complete.\n", i, afterTime - prevTime);
		}
	}
	
	public static void xmlTest(String[] args) {
		Entity timbre = 
				new PeriodicGreedyEntity(
						new Entity(Waveform.SINE, 0.5, 0.5, 0, 0));  // create an entity to produce sound
		xmlTestWithEntity(timbre); 
	}
	
	public static void soundTest(String[] args) {
		String freq = "A4";
		Entity timbre = 
				new Entity(Waveform.SINE, 0.5, 0.5, 0, 0);  // create an entity to produce sound
		
		Layer layer = new Layer();
		layer.add(new Note(freq, 4.0, 0.5));
		
		Instrument instrument = new Instrument("test", timbre);
		instrument.setVoice(layer, 0);
		
		Score score = new Score();
		score.add(instrument);
		
		ScorePlayerPipeline spp = new ScorePlayerPipeline();
		spp.transferDataBetween(score, freq, null);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static void xmlTestWithEntity(Entity timbre) {
		String xmlFile;
		
		// xmlFile = "direction-test.xml";
		xmlFile = "suhas_vittal-mixed_messages.xml";
		// xmlFile = "beethoven-grobe_fuge.xml";
		
		Map<String, Object> kwargs = new HashMap<String, Object>();
		
		List entities = new ArrayList();
		entities.add(timbre);
		kwargs.put("entities", entities);
		
		MusicXMLParserPipeline mxpp = new MusicXMLParserPipeline();
		ScorePlayerPipeline spp = new ScorePlayerPipeline(OutputMode.LOW_MEMORY);
		
		PipelineDeque deque = new PipelineDeque();
		deque.addToBack(mxpp);
		deque.addToBack(spp);
		
		Score score = new Score();
		deque.passData(new Object[] { xmlFile, score, "test" }, kwargs); 
	}
	
	public static void main(String[] args) {
		xmlTest(args);
		// entityBenchmark(args);
	}
}
