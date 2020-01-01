/**
 * Copyright (C) 2019 Suhas Vittal
 *
 *  This file is part of Stoch-MPL.
 *
 *  Stoch-MPL is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Stoch-MPL is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *  
 *  You should have received a copy of the GNU General Public License
 *  along with Stoch-MPL.  If not, see <https://www.gnu.org/licenses/>.
 * */

package jsmpl.io.pipeline;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.AudioFileFormat.Type;
import javax.sound.sampled.AudioFormat;

import jsmpl.function.Generator;
import jsmpl.io.WAVFileOutputStream;
import jsmpl.score.Score;

/**
 * The ScorePlayerPipeline class provides objects that can read a 
 * score and save the sample data generated from instruments in the
 * score as .WAV files. Objects operate in 3 different modes, depending
 * on what is specified during construction:
 * 	1.) STANDARD - this mode does nothing special; it simply loads in
 * 		each instrument in order and saves each .WAV file subsequently.
 * 		This mode is only recommended when there are serious output 
 * 		issues when using the other two modes.
 * 	2.) LOW_TIME - this mode uses multi-threading to read all instruments
 * 		simultaneously. Due to the nature of threads in a process, this
 * 		uses a lot of memory, so this mode should only be used for short
 * 		pieces.
 * 	3.) LOW_MEMORY - this mode uses multi-threading as well but saves 
 * 		resources to the temporary file system first to avoid memory 
 * 		issues seen with larger scores. This mode is recommended.
 * 
 * The transferDataBetween method supports the following optional kwargs
 * 		verbose (Type Integer) - STANDARD mode only. Set to 1 to see
 * 			console output.
 * 		iteratorBlockSize (Type Integer) - LOW_MEMORY mode only. Sets
 * 			how many samples each thread cycles looks at. Should only
 * 			be touched in low memory (or high memory) setups. Default
 * 			is 60 * samplingRate, in order to look at 60s of data per
 * 			thread cycle.
 */
public class ScorePlayerPipeline extends Pipeline<Score, String> {
	public static enum OutputMode {
		STANDARD,
		LOW_TIME,
		LOW_MEMORY
	}
	
	public static final Map<String, Class<?>> OPTIONAL_KWARGS_STANDARD;
	public static final Map<String, Class<?>> OPTIONAL_KWARGS_LOW_MEMORY;
	
	static {
		Map<String, Class<?>> oks = new HashMap<String, Class<?>>();
		oks.put("verbose", Integer.class);
		
		Map<String, Class<?>> oklm = new HashMap<String, Class<?>>();
		oklm.put("iteratorBlockSize", Integer.class);
		
		OPTIONAL_KWARGS_STANDARD = oks;
		OPTIONAL_KWARGS_LOW_MEMORY = oklm;
	}
	
	private OutputMode mode;
	private double samplingRate;
	private int bitSize;
	
	/**
	 * Default constructor initializes object with
	 * 	LOW_MEMORY mode,
	 * 	44.1 kHz sampling rate,
	 * 	32 bits per sample
	 */
	public ScorePlayerPipeline() {
		this(OutputMode.LOW_MEMORY);
	}
	
	/**
	 * @param mode the mode to use when transfering data
	 * 
	 * Defaults sampling rate to 44.1 kHz and bits/sample to 32.
	 */
	public ScorePlayerPipeline(OutputMode mode) {
		this(mode, 44100.0);
	}
	
	/**
	 * @param mode the mode to use when transfering data
	 * @param samplingRate the sampling rate of the output .WAV file
	 * 
	 * Default bits/sample is 32.
	 */
	public ScorePlayerPipeline(OutputMode mode, double samplingRate) {
		this(mode, samplingRate, 32);
	}
	
	/**
	 * @param mode the mode to use when transfering data
	 * @param samplingRate the sampling rate of the output .WAV file
	 * @param bitSize the number of bits per sample
	 */
	public ScorePlayerPipeline(OutputMode mode, double samplingRate, int bitSize) {
		this.mode = mode;
		this.samplingRate = samplingRate;
		this.bitSize = bitSize;
	}
	
	public void transferDataBetween(Object src, Object dest, Map<String, Object> kwargs) {
		super.transferDataBetween(src, dest, kwargs);
		transferDataBetween((Score) src, (String) dest, kwargs);
	}
	
	public void transferDataBetween(Score src, String dest, Map<String, Object> kwargs) {
		switch (mode) {
		case STANDARD:
			standardTransfer(src, dest, kwargs);
			break;
		case LOW_TIME:
			lowTimeTransfer(src, dest, kwargs);
			break;
		case LOW_MEMORY:
			lowMemoryTransfer(src, dest, kwargs);
			break;
		}
	}
	
	private void standardTransfer(Score src, String dest, Map<String, Object> kwargs) {
		// no threading for this one.
		/*
		 * Optional kwargs
		 * 	verbose (Integer)
		 * 
		 * */
		
		Map<String, Boolean> validationMap = validateOptionalKwargs(OPTIONAL_KWARGS_STANDARD, kwargs);
		int verbose;
		if (validationMap.get("verbose")) {
			verbose = (int) kwargs.get("verbose");
		} else {
			verbose = 0;
		}
		
		
		Map<Integer, Generator<List<Double[][]>>> generators = 
				src.developInstrumentsAsReference(samplingRate, verbose);
		
		for (int partKey : generators.keySet()) {
			List<Double[][]> data = generators.get(partKey).generate();
			writePartData(data, dest + "_" + partKey + ".wav");
		}
	}
	
	private void lowTimeTransfer(Score src, String dest, Map<String, Object> kwargs) {
		Map<Integer, List<Double[][]>> dataMap;
		try {
			dataMap = src.developInstruments(samplingRate, 0, true);
			
			ExecutorService es = Executors.newCachedThreadPool();
			for (int partKey : dataMap.keySet()) {
				Runnable r = new Runnable() {
					public void run() {
						List<Double[][]> data = dataMap.get(partKey);
						writePartData(data, dest + "_" + partKey + ".wav");
					}
				};
				
				es.execute(r);
			}
			
			es.shutdown();
			es.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void lowMemoryTransfer(Score src, String dest, Map<String, Object> kwargs) {
		/*
		 * Optional kwargs:
		 * 	iteratorBlockSize (Integer)
		 * */
		
		Map<String, Boolean> validationMap = validateOptionalKwargs(OPTIONAL_KWARGS_LOW_MEMORY, kwargs);
		
		int iteratorBlockSize;
		if (validationMap.get("iteratorBlockSize")) {
			iteratorBlockSize = (int) kwargs.get("iteratorBlockSize");
		} else {
			iteratorBlockSize = (int) (samplingRate * 60);
		}
		
		
		Map<Integer, Iterator<List<Double[]>>> iterators = src.developInstrumentsAsIterator(samplingRate);
		
		ExecutorService es = Executors.newCachedThreadPool();
		for (int partKey : iterators.keySet()) {
			Runnable r = new Runnable() {
				public void run() {
					Iterator<List<Double[]>> iter = iterators.get(partKey);
					
					try {
						List<Double[]> curr = iter.next();
						int channelCount = curr.get(0).length;
						String fileHeader = dest + "_" + partKey;

						Double[][] block = new Double[iteratorBlockSize][channelCount];
						int k = 0;
						int totalBlockCount = 0;
						
						File byteDumpFile = File.createTempFile(fileHeader, null);
						BufferedOutputStream ostream = new BufferedOutputStream(
								new FileOutputStream(byteDumpFile, true));
						
						while (curr != null) {
							Double[] aggregate = new Double[curr.get(0).length];
							
							for (int i = 0; i < curr.size(); i++) {
								Double[] x = curr.get(i);
								
								for (int j = 0; j < aggregate.length; j++) {
									if (aggregate[j] == null) {
										aggregate[j] = 0.0;
									}
									
									aggregate[j] += x[j] / curr.size();
								}
							}
							
							if (k < iteratorBlockSize) {
								block[k++] = aggregate;
							} else {
								ostream.write(convertToLittleEndian(block, k));
								totalBlockCount += k;
								k = 0;
							} 
							
							curr = iter.next();
						}
						
						// write any remaining elements in block
						ostream.write(convertToLittleEndian(block, k));
						totalBlockCount += k;
						ostream.close();
						
						// now that the data has been written
						// open an audioinputstream to write to the target file
						AudioInputStream istream = new AudioInputStream(
								new BufferedInputStream(new FileInputStream(byteDumpFile)),
								new AudioFormat((float) samplingRate, bitSize, channelCount, true, false),
								totalBlockCount);
						AudioSystem.write(istream, Type.WAVE, new File(fileHeader + ".wav"));
						byteDumpFile.delete();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			};
			
			es.execute(r);
		}
		
		es.shutdown();
		try {
			es.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void writePartData(List<Double[][]> data, String toFile) {
		int channelCount = data.get(0)[0].length;
		
		Double[][] aggregate = new Double[data.get(0).length][channelCount];
		for (int k = 0; k < data.size(); k++) {
			Double[][] temp = data.get(k);
			
			if (temp.length > aggregate.length) {  // resize.
				int oldLength = aggregate.length;
				aggregate = Arrays.copyOf(aggregate, temp.length);
				
				for (int i = oldLength; i < temp.length; i++) {
					aggregate[i] = new Double[channelCount];
				}
			}
			for (int i = 0; i < temp.length; i++) {
				for (int j = 0; j < temp[0].length; j++) {
					// the reason not to put j < temp[i].length as the predicate
					// is because j < temp[0].length implicitly checks that all
					// rows of data have the same number of columns, which is required.
					// NOTE: #columns = #channels.
					
					if (aggregate[i][j] == null) {
						aggregate[i][j] = temp[i][j] / data.size();
					} else {
						aggregate[i][j] += temp[i][j] / data.size();
					}
				}
			}
		}
		
		try {
			WAVFileOutputStream ostream = 
					new WAVFileOutputStream(toFile, channelCount, bitSize, samplingRate);
			ostream.write(aggregate);
			ostream.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private byte[] convertToLittleEndian(Double[][] block, int len) {
		int endianCount = bitSize / 8;
		long smin = -(1 << (bitSize - 1));
		long smax = (1 << (bitSize - 1)) - 1;
		
		byte[] b = new byte[block.length * block[0].length * endianCount];
		
		for (int i = 0; i < len; i++) {
			for (int j = 0; j < block[i].length; j++) {
				// convert to little endian
				double value = block[i][j];
				long sample = (long) (smax * value);
				
				if (sample > smax) {
					sample = smax;
				} 
				
				if (sample < smin) {
					sample = smin;
				}
				
				for (int r = 0; r < endianCount; r++) {
					b[(i * block[i].length + j) * endianCount + r] = 
							(byte) ((sample >> (8 * r)) & 255);
				}
			}
		}
		
		return b;
	}
}
