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

package jsmpl.direction;

import java.util.Arrays;
import java.util.List;
import java.util.TreeMap;

import jsmpl.util.AVL;
import jsmpl.util.AVLNode;


/**
 * 
 * The PiecewiseMusicalDirection class is a subclass of
 * MusicalDirection that implements a situation where
 * multiple MusicalDirections are concatenated together.
 * 
 * The MusicalDirections are stored in an AVL tree for fast
 * look up (the so-called "hash tree"), and users have access
 * to the "head" MusicalDirection, which is the first valid
 * MusicalDirection in the concatenation.
 * 
 * Each MusicalDirection is represented as a DirectionTuple,
 * which stores three pieces of information:
 * 	1. The MusicalDirection object.
 *  2. Its index in the array passed to the constructor.
 *  3. Its starting location in the larger PiecewiseMusicalDirection.
 * This static inner class is exposed to the user due to the
 * importance of the information stored, especially (1) and (3)
 * for the implementation of user programs.
 * 
 * */
public class PiecewiseMusicalDirection extends MusicalDirection {
	private AVL<DirectionTuple> hashTree;
	private DirectionTuple head;
	
	/**
	 * @param directions A list of MusicalDirections to concatenate together.
	 */
	public PiecewiseMusicalDirection(PiecewiseMusicalDirection other) {
		super(other.getInitialDynamics(), other.getFinalDynamics(), other.getLength());
		hashTree = other.hashTree;
		head = other.head;
		
	}
	
	public PiecewiseMusicalDirection(MusicalDirection... directions) {
		this(Arrays.asList(directions));
	}
	
	/**
	 * @param directions A list of MusicalDirections to concatenate together.
	 */
	public PiecewiseMusicalDirection(List<MusicalDirection> directions) {
		super(0, 0, 0);
		
		double length = 0.0;
		TreeMap<Integer, Double> lengthTree = new TreeMap<Integer, Double>();
		double initialDynamics = -1.0;
		double finalDynamics = -1.0;
		
		for (int i = 0; i < directions.size(); i++) {
			MusicalDirection dir = directions.get(i);
			if (dir.isEmpty()) {
				continue;
			}
			
			lengthTree.put(i, length);
			length += dir.getLength();
			
			if (initialDynamics == -1.0) {
				initialDynamics = dir.getInitialDynamics();
			}
			finalDynamics = dir.getFinalDynamics();
		}
		
		// here, we need to use a protected level setter from
		// the superclass. Unlike pPthon, Java requires that
		// constructor calls be called at the beginning. In
		// Python, you can do computation that does not create
		// class variables and then call a super constructor.
		
		setInitialDynamics(initialDynamics);
		setFinalDynamics(finalDynamics);
		setLength(length); 
		
		hashTree = new AVL<DirectionTuple>();
		Integer key = lengthTree.firstKey();
		while (key != null) {
			DirectionTuple tuple = 
					new DirectionTuple(directions.get(key), key, lengthTree.get(key));
			hashTree.add(tuple);
			
			if (head == null) {
				head = tuple;
			}
			
			key = lengthTree.higherKey(key);
		}
	}
	
	/**
	 * Like in the parent class, this method computes the value
	 * of the MusicalDirection at some point t. However, if the
	 * hash size of the PiecewiseMusicalDirection is large and
	 * this method is called repeatedly, it is best to use the
	 * getBucket method when there are limited resources, as 
	 * that will minimize the amount of lookups for MusicalDirections.
	 * 
	 * @param t the value at which to compute the amplitude of the direction
	 * 
	 * @return the amplitude of the musical direction at point t.
	 */
	public double value(double t) {
		DirectionTuple key = getBucketKey(t);
		if (key == null) {
			return getInitialDynamics();
		} else {
			return key.direction.value(t - key.length);
		}
	}
	
	/**
	 * When faced with limited resources and repeated calls to the 
	 * value method, this method is the best alternative. Given a value
	 * t, the function gets the DirectionTuple bucket that the value
	 * method would have called anyways. As a result, instead of calling
	 * the value method to retrieve the same bucket and get a value,
	 * users can just get the bucket directly and compute as many values
	 * as they would need.
	 * 
	 * @param t the value at which to find the corresponding bucket
	 * @return the bucket for which the value t fits in.
	 */
	public DirectionTuple getBucket(double t) {
		DirectionTuple key = getBucketKey(t);
		if (key == null) {
			DirectionTuple dummyTuple = new DirectionTuple(
					new MusicalDirection(getInitialDynamics(), 
							getInitialDynamics(), 
							head.length), -1, 0);
			return dummyTuple;
		} else {
			return key;
		}
	}
	
	/**
	 * @return the number of musical directions contained in the class
	 */
	public int getHashSize() {
		return hashTree.size();
	}
	
	/**
	 * As an AVL is a self-balancing BST, we simply search through the tree
	 * to find the corresponding bucket. In order for a value to be in a bucket,
	 * there are 2 criteria:
	 * 	1.) The bucket's location in the larger direction should be less than the value.
	 * 	2.) The value should be closest to a bucket's location when compared to all 
	 * 		other buckets that satisfy (1).
	 * 
	 * @param value the value to find the bucket for
	 * @return the DirectionTuple object that represents the bucket.
	 */
	private DirectionTuple getBucketKey(double value) {
		AVLNode<DirectionTuple> curr = hashTree.getRoot();
		
		DirectionTuple closestValue = null;
		while (curr != null) {
			DirectionTuple currData = curr.getData();
			
			if (currData.length < value 
					&& (closestValue == null 
						|| value - currData.length < value - closestValue.length)) {
				closestValue = currData;
			}
			
			if (value - currData.length < 0) {
				curr = curr.getLeft();
			} else if (value - currData.length > 0) {
				curr = curr.getRight();
			} else {
				return currData;
			}
		}
		
		return closestValue;
	}
	
	/**
	 * The primary means of representing MusicalDirections in the class.
	 * Has three fields:
	 * 	(1) direction (MusicalDirection) -> the MusicalDirection object
	 * 	(2) index (int)	-> the index in the array passed in the constructor
	 * 	(3) length (double)	-> the location
	 * 
	 * DirectionTuples are ordered by their length field.
	 */
	public static class DirectionTuple implements Comparable<DirectionTuple> {
		public final MusicalDirection direction;
		public final int index;
		public final double length;
		
		public DirectionTuple(MusicalDirection d, int i, double s) {
			direction = d;
			index = i;
			length = s;
		}
		
		@Override
		public int compareTo(DirectionTuple other) {
			// TODO Auto-generated method stub
			return (int) (this.length - other.length);
		}
	}
}
