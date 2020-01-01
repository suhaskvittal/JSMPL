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

package jsmpl.entity;

import java.io.Serializable;

import jsmpl.function.ADSRFunction;
import jsmpl.function.Waveform;


/**
 * The Entity class models a sound generator, with an ADSR envelope.
 * */
public class Entity implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 4803443500097439886L;
	private Waveform waveForm;
	private ADSRFunction adsr;
	private double attack, decay, sustain, release;

	/**
	 * @param wf The waveform function that the Entity emits.
	 * @param attack a double from 0.0 to 1.0 that is the proportion of time spent in attack
	 * @param decay a double from 0.0 to 1.0 that is the proportion of time spent in decay 
	 * @param sustain a double from 0.0 to 1.0 that is the amplitude for sustain
	 * @param release a double from 0.0 to 1.0 that is the proportion of time spent in release
	 * 
	 * In this constructor, the ADSR envelope is treated as a default linear envelope.
	 */
	public Entity(Waveform wf, double attack, double decay, double sustain, double release) {
		this(wf, attack, decay, sustain, release, new ADSRFunction() {

			/**
			 * 
			 */
			private static final long serialVersionUID = -3288448922766037675L;

			@Override
			public double f(double tProp, double attack, double decay, 
					double sustain, double release) {
				double timeSustain = 1.0 - (attack + decay + release);
				
				double slope, xIntercept, yIntercept;
				if (tProp <= attack) {
					slope = 1.0 / attack;
					xIntercept = 0;
					yIntercept = 0;
				} else if (tProp - attack <= decay) {
					slope = (sustain - 1.0) / decay;
					xIntercept = attack;
					yIntercept = 1.0;
				} else if (tProp - attack - decay <= timeSustain) {
					return sustain;
				} else {
					slope = -sustain / release;
					xIntercept = timeSustain + decay + attack;
					yIntercept = sustain;
				}
				
				return slope * (tProp - xIntercept) + yIntercept;
			}
			
		});
	}
	
	/**
	 * @param wf The waveform function that the Entity emits.
	 * @param attack a double from 0.0 to 1.0 that is the proportion of time spent in attack
	 * @param decay a double from 0.0 to 1.0 that is the proportion of time spent in decay 
	 * @param sustain a double from 0.0 to 1.0 that is the amplitude for sustain
	 * @param release a double from 0.0 to 1.0 that is the proportion of time spent in release
	 * @param adsr the envelope function
	 */
	public Entity(Waveform wf, double attack, double decay, double sustain, double release,
			ADSRFunction adsr) {
		if (attack + decay + release > 1.0) {
			throw new IllegalArgumentException(
					"The sum of the attack, decay, and release is greater than 1.0");
		}
		this.attack = attack;
		this.decay = decay;
		this.sustain = sustain;
		this.release = release;
		
		this.waveForm = wf;
		this.adsr = adsr;
	}
	
	public Waveform getWaveform() {
		return waveForm;
	}
	
	public double getAttack() {
		return attack;
	}
	
	public double getDecay() {
		return decay;
	}
	
	public double getSustain() {
		return sustain;
	}
	
	public double getRelease() {
		return release;
	}
	
	public ADSRFunction getADSR() {
		return adsr;
	}
	
	/**
	 * @param frequency the pitch frequency
	 * @param time the time for which the frequency is played for
	 * @param amplitude the volume at which the frequency is played at
	 * @param samplingRate the sampling rate to generate the frequency at; 
	 * 	standard sampling rate is 44100 Hz
	 * @return an array of doubles which are the samples for the sound
	 */
	public double[] playPitch(double frequency, double time, double amplitude, double samplingRate) {
		int numberOfSamples = (int) (time * samplingRate);
		double[] waveData = new double[numberOfSamples];
		
		for (int i = 0; i < numberOfSamples; i++) {
			waveData[i] = amplitude * waveFunction(frequency, 
					(i * time) / numberOfSamples, time);
		}
		
		return waveData;
	}
	
	protected double waveFunction(double frequency, double currentTime, double totalTime) {
		return waveForm.f(2.0 * Math.PI * frequency * currentTime) *
				adsr.f(currentTime / totalTime, attack, decay, sustain, release);
	}
}
