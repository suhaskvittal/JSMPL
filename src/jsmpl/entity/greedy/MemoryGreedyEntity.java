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

package jsmpl.entity.greedy;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import jsmpl.entity.Entity;

/**
 * The MemoryGreedyEntity class incorporates dynamic programming to
 * accelerate sample instantiation with the cost of memory. In order
 * to ensure backing structures are instantiated correctly, objects
 * cannot be instantiated directly, but rather a user can create
 * MemoryGreedyEntity objects by using static builder methods that
 * take an Entity object and return a MemoryGreedyEntity with either
 * HashMap backing or TreeMap backing.
 * 
 * During benchmarks, hashmap-backed MemoryGreedyEntity objects performed
 * best when many notes were repeated. However, in general, treemap-backed
 * MemoryGreedyEntity objects are best, simply because they scale better.
 * 
 * Note that MemoryGreedyEntities provide the largest performance boost
 * for large amounts of data; pairing other greedy entities with 
 * MemoryGreedyEntities provides the best results.
 */
public class MemoryGreedyEntity extends Entity {
	/**
	 * 
	 */
	private static final long serialVersionUID = -5014880897328598835L;
	private Entity backingEntity;
	private Map<MemoryState, MemoryEvent> memoryTable; 
	// the reason MemoryEvent is the value class is because
	// the Map get method does not have a variant that return
	// map entries. For the algorithm below to work, we need access
	// to the original entry to compare keys.
	
	protected MemoryGreedyEntity(Entity e, Map<MemoryState, MemoryEvent> m) {
		super(e.getWaveform(), e.getAttack(), e.getDecay(), 
				e.getSustain(), e.getRelease(), e.getADSR());
		
		this.backingEntity = e;
		this.memoryTable = m;
	}
	
	@Override
	public double[] playPitch(double frequency, double time, double amplitude, double samplingRate) {
		MemoryState curr = new MemoryState(frequency, time, amplitude, samplingRate);
		
		// note we want to get a copy of whatever is in the table.
		if (!memoryTable.containsKey(curr)) {
			double[] temp = backingEntity.playPitch(frequency, time, amplitude, samplingRate);
			memoryTable.put(curr, new MemoryEvent(curr, temp));
			
			return temp.clone();
		} else {
			MemoryEvent event = memoryTable.get(curr);
			MemoryState prev = event.key;
			
			if (prev.equals(curr)) {
				return event.data.clone();
			} else {
				// amplitude is different
				double[] other = event.data;
				double[] temp = new double[other.length];
				double ratio = curr.amplitude / prev.amplitude;
				
				for (int i = 0; i < other.length; i++) {
					temp[i] = ratio * other[i];
				}
				
				return temp.clone();
			}
		}
	}
	
	public static MemoryGreedyEntity hashMemory(Entity backingEntity) {
		return new MemoryGreedyEntity(backingEntity, new HashMap<MemoryState, MemoryEvent>()); 
	}
	
	public static MemoryGreedyEntity treeMemory(Entity backingEntity) {
		return new MemoryGreedyEntity(backingEntity, 
			new TreeMap<MemoryState, MemoryEvent>(
				new Comparator<MemoryState>() {
					@Override
					public int compare(MemoryState a, MemoryState b) {
						if (a.frequency == b.frequency) {
							if (a.time == b.time) {
								if (a.samplingRate == b.samplingRate) {
									return 0;
								} else {
									return a.samplingRate > b.samplingRate ? 1 : -1;
								}
							} else {
								return a.time > b.time ? 1 : -1;
							}
						} else {
							return a.frequency > b.frequency ? 1 : -1;
						}
					}
				} 
			)
		);
	}
	
	/**
	 * The MemoryState static class is used as keys in the backing map. Two
	 * MemoryState objects are considered equivalent if:
	 * 	- frequencies are equal.
	 *  - times are equal.
	 *  - sampling rates are equal.
	 * As amplitude is scalable, it is not a criteria for equivalence. With this,
	 * we can reduce the memory used.
	 */
	protected static class MemoryState {
		double frequency;
		double time;
		double amplitude;
		double samplingRate;
		
		/* Amongst the fields, frequency, length, and samp. rate are strict
		 * parameters, so when looking up a memory state in the hashmap,
		 * we will only use these two parameters to get data. 
		 * On the other hand, amplitude is flexible; 
		 * if the amplitude is what we want, we can just return 
		 * the data. Otherwise, we just scale appropriately; 
		 * either way, we cut down on time spent.
		 * */
		
		public MemoryState(double f, double t, double a, double r) {
			frequency = f;
			time = t;
			amplitude = a;
			samplingRate = r;
		}
		
		public boolean equals(Object o) {
			if (!(o instanceof MemoryState)) {
				return false;
			} else {
				MemoryState other = (MemoryState) o;
				return this.frequency == other.frequency && this.time == other.time
						&& this.samplingRate == other.samplingRate;
			}
		}
		
		public int hashCode() {
			return (int) (frequency + 10 * time + samplingRate / 10.0);
		}
	}
	
	/**
	 * The MemoryEvent class are the values in the backing map.
	 */
	private static class MemoryEvent {
		MemoryState key;  // the key in the map
		double[] data;
		
		public MemoryEvent(MemoryState k, double[] d) {
			this.key = k;
			this.data = d;
		}
	}
}
