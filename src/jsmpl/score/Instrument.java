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

package jsmpl.score;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import jsmpl.direction.MusicalDirection;
import jsmpl.direction.PiecewiseMusicalDirection;
import jsmpl.entity.Entity;

/**
 * This class models an instrument, with 4 fields:
 * 	1. The instrument name
 * 	2. The voices/lines in the instrument. This can be thought of the instrument's part(s).
 * 	3. The entity of the instrument, which is the timbre.
 * 	4. The left and right pans of the instrument, which is its orientation with respect to
 * 		an observer.
 */
public class Instrument {
	public static final double PAN_RIGHT_SS = Math.PI / 2.0;
	public static final double PAN_LEFT_SS = 0.0;
	
	private String name;
	private Layer[] voices;
	private Entity entity;
	private double panLeft, panRight;
	
	/**
	 * @param name The name of the instrument
	 * @param ent The entity used for the instrument.
	 * 
	 * Here, the instrument is initialized with one voice and default pan angles of
	 * L = 0 and R = PI / 2.
	 */
	public Instrument(String name, Entity ent) {
		this(name, ent, 1, PAN_LEFT_SS, PAN_RIGHT_SS);
	}
	
	/**
	 * @param name The name of the instrument
	 * @param ent The entity used for the instrument.
	 * @param numberOfVoices the number of voices for the instrument.
	 * 
	 * Here, the instrument is initialized with default pan angles of
	 * L = 0 and R = PI / 2.
	 */
	public Instrument(String name, Entity ent, int numberOfVoices) {
		this(name, ent, numberOfVoices, PAN_LEFT_SS, PAN_RIGHT_SS);
	}
	
	/**
	 * @param name The name of the instrument
	 * @param ent The entity used for the instrument.
	 * @param numberOfVoices the number of voices for the instrument.
	 * @param panLeft the left pan angle
	 * @param panRight the right pan angle
	 */
	public Instrument(String name, Entity ent, int numberOfVoices, double panLeft, double panRight) {
		this.name = name;
		this.entity = ent;
		this.voices = new Layer[numberOfVoices];
		this.panLeft = panLeft;
		this.panRight = panRight;
	} 
	
	public String getName() {
		return name;
	}
	
	public void setEntity(Entity e) {
		entity = e;
	}
	
	public Layer getVoice(int voiceNumber) {
		return voices[voiceNumber];
	}
	
	public void setVoice(Layer layer, int voiceNumber) {
		voices[voiceNumber] = layer;
	}
	
	public void setPanLeft(double pan) {
		panLeft = pan;
	}
	
	public void setPanRight(double pan) {
		panRight = pan;
	}
	
	public double getPanLeft() {
		return panLeft;
	}
	
	public double getPanRight() {
		return panRight;
	}
	
	/**
	 * @param samplingRate the sampling rate, which is traditionally 44100 Hz
	 * @param verbose 1 if console statements should be shown, 0 otherwise.
	 * @return a List of 2d double arrays; each array represents the sound samples
	 * 	for a part. There are N rows (one for each sample), and each row has
	 * 	two columns. The first column is the left channel, and the second column
	 * 	is the right channel.
	 * 
	 * Here, the directionOverlay is passed as null, which means there is no
	 * overlay used.
	 */
	public List<Double[][]> developParts(double samplingRate, int verbose) {
		return developParts(null, samplingRate, verbose);
	}
	
	
	/**
	 * The developParts process comes in two varieties: a threaded method and
	 * an unthreaded method. The threaded method is preferred for larger scores as 
	 * converting part data to sound samples is computationally expensive. Precisely,
	 * we can write the time complexity as:
	 * 		O(MNS * log(D))
	 * where M is the number of parts, N is the length of the longest part, S
	 * is the sampling rate, and D is the number of directions overlayed over the
	 * piece. For a piece that is 10 minutes, using the non-threaded 
	 * variant will take a long time for multiple parts.
	 * 
	 * @param directionOverlay the musical direction to play the instrument's lines
	 * 	with. If null, then the directionOverlay is not used.
	 * @param samplingRate the sampling rate, which is traditionally 44100 Hz
	 * @return a List of 2d double arrays; each array represents the sound samples
	 * 	for a part. There are N rows (one for each sample), and each row has
	 * 	two columns. The first column is the left channel, and the second column
	 * 	is the right channel.
	 */
	public List<Double[][]> developPartsWithThreads(PiecewiseMusicalDirection directionOverlay,
			double samplingRate) throws InterruptedException 
	{
		// we don't include a verbose option as this would make the console extremely messy.
		// also hard to decipher.
		List<Double[][]> parts = new ArrayList<Double[][]>(voices.length);
		
		final double HPI = Math.PI / 2;
		double leftPanGain = Math.sqrt((HPI - panLeft) * (1.0 / HPI) * Math.cos(panLeft));
		double rightPanGain = Math.sqrt(panRight * (1.0 / HPI) * Math.sin(panRight));
		
		ExecutorService es = Executors.newCachedThreadPool();
		long longestPartLength = 0;
		for (int i = 0; i < voices.length; i++) {
			Layer phrase = voices[i];
			
			if (phrase.isEffectivelyEmpty()) {
				continue;
			}
			if (phrase.getSize() > longestPartLength) {
				longestPartLength = (long) (0.5 + phrase.getSize());
			}
			
			double[] part = new double[(int) (phrase.getSize() * samplingRate)];
			Runnable r = new Runnable() {
				@Override
				public void run() {
					fillSampleArray(part, directionOverlay, phrase.getChordStream().iterator(), samplingRate, 0);
					Double[][] channels = new Double[part.length][2];
					for (int j = 0; j < part.length; j++) {
						channels[j][0] = part[j] * leftPanGain;
						channels[j][1] = part[j] * rightPanGain;
					}
					
					parts.add(channels);
				}
			};
			es.execute(r);
		}
		
		es.shutdown();
		boolean terminated = es.awaitTermination(longestPartLength, TimeUnit.MINUTES);  
		// wait one minute before timing out.
		if (terminated) {
			return parts;
		} else {
			throw new RuntimeException("The process timed out. "
					+ "Consider using the non-threaded method variant of this method.");
		}
	}
	
	/**
	 * @param directionOverlay the musical direction to play the instrument's lines
	 * 	with. If null, then the directionOverlay is not used.
	 * @param samplingRate the sampling rate, which is traditionally 44100 Hz
	 * @param verbose 1 if console statements should be shown, 0 otherwise.
	 * @return a List of 2d double arrays; each array represents the sound samples
	 * 	for a part. There are N rows (one for each sample), and each row has
	 * 	two columns. The first column is the left channel, and the second column
	 * 	is the right channel.
	 */
	public List<Double[][]> developParts(PiecewiseMusicalDirection directionOverlay, 
			double samplingRate, int verbose) 
	{
		List<Double[][]> parts = new ArrayList<Double[][]>(voices.length);
		
		final double HPI = Math.PI / 2;
		double leftPanGain = Math.sqrt((HPI - panLeft) * (1.0 / HPI) * Math.cos(panLeft));
		double rightPanGain = Math.sqrt(panRight * (1.0 / HPI) * Math.sin(panRight));
	
		for (int i = 0; i < voices.length; i++) {
			Layer phrase = voices[i];
			
			if (phrase.isEffectivelyEmpty()) {
				continue;
			}
			
			if (verbose == 1) {
				System.out.printf("\tVoice number: %d", i);
			}
			
			double[] part = new double[(int) (phrase.getSize() * samplingRate)];
			fillSampleArray(part, directionOverlay, phrase.getChordStream().iterator(), samplingRate, verbose);
			
			Double[][] channels = new Double[part.length][2];
			for (int j = 0; j < part.length; j++) {
				channels[j][0] = part[j] * leftPanGain;
				channels[j][1] = part[j] * rightPanGain;
			}
			
			parts.add(channels);
		}
		
		return parts;
	}
	
	private void fillSampleArray(double[] array, PiecewiseMusicalDirection directionOverlay, 
			Iterator<Chord> iter, double samplingRate, int verbose) 
	{
		int pointer = 0;
		int chordNumber = 0;
		
		MusicalDirection currDirection = null;
		int currKey = -1;
		double currLoc = 0.0;
		
		while (iter.hasNext()) {
			Chord chord = iter.next();
			
			if (verbose == 1) {
				if (chordNumber % 50 == 0) {
					System.out.printf("\t\tInstance number: %d, chord: %s", chordNumber++, chord);
				}
			}
			
			double[] samples;
			if (chord.isRest()) {
				samples = new double[(int) (chord.getLength() * samplingRate)];
			} else {
				samples = chord.produceData(entity, samplingRate);
			}

			for (int j = 0; j < samples.length; j++) {
				double adjust;
				if (directionOverlay == null || chord.isRest()) {
					adjust = 1;
				} else {
					double t = pointer / samplingRate;
					
					if ((currDirection == null || t > currLoc + currDirection.getLength())
							&& currKey < directionOverlay.getHashSize()) 
					{
						PiecewiseMusicalDirection.DirectionTuple tmpTuple = 
								directionOverlay.getBucket(t);
						
						currDirection = tmpTuple.direction;
						currKey = tmpTuple.index;
						currLoc = tmpTuple.length;
					}
					
					adjust = currDirection.value(t - currLoc) / chord.getVolume();
				}
				
				array[pointer++] = samples[j] * adjust;
			}
		}
	}
}
