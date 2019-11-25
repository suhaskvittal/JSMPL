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

import jsmpl.function.DirectionShapeFunction;
import jsmpl.util.Dynamics;


/**
 * 
 * The MusicalDirection class represents a musical direction, such
 * as a crescendo or descrescendo, mathematically. The default 3-arg
 * constructor represents the direction as a linear movement from one
 * volume to another, while the 4-arg constructors can represent the 
 * movement of the volume from initial to final with any function, 
 * provided the function conforms to p(0) = 0 and p(1) = 1.
 * 
 * */
public class MusicalDirection {
	private double initialDynamics, finalDynamics, length;
	private DirectionShapeFunction shapeFunction;
	
	/**
	 * @param initialDynamics the initial dynamics for the direction
	 * @param finalDynamics the final dynamics for the direction
	 * @param distance the length, or distance, over which the direction takes place
	 * */
	public MusicalDirection(double initialDynamics, double finalDynamics, double distance) {
		this(initialDynamics, finalDynamics, distance, DirectionShapeFunction.linear);
	}
	
	/**
	 * @param initialDynamics the initial dynamics as a String from "ppp" to "fff"
	 * @param finalDynamics the final dynamics as a String from "ppp" to "fff"
	 * @param distance the length, or distance, over which the direction takes place
	 * 
	 * */
	public MusicalDirection(String initialDynamics, String finalDynamics, double distance) {
		this(Dynamics.valueOf(initialDynamics).value,
				Dynamics.valueOf(finalDynamics).value, distance);
	}
	
	/**
	 * @param initialDynamics the initial dynamics for the direction
	 * @param finalDynamics the final dynamics for the direction
	 * @param distance the length, or distance, over which the direction takes place
	 * @param pfunc the transformation function, such that 0 -> 0 and 1 -> 1
	 */
	public MusicalDirection(double initialDynamics, double finalDynamics, double distance, 
			DirectionShapeFunction pfunc) {
		this.initialDynamics = initialDynamics;
		this.finalDynamics = finalDynamics;
		this.length = distance;
		this.shapeFunction = pfunc;
	}
	
	/**
	 * @param initialDynamics the initial dynamics as a String from "ppp" to "fff"
	 * @param finalDynamics the final dynamics as a String from "ppp" to "fff"
	 * @param distance the length, or distance, over which the direction takes place
	 * @param pfunc the transformation function, such that 0 -> 0 and 1 -> 1
	 */
	public MusicalDirection(String initialDynamics, String finalDynamics, double distance, 
			DirectionShapeFunction pfunc) {
		this(Dynamics.valueOf(initialDynamics).value,
				Dynamics.valueOf(finalDynamics).value,
				distance, pfunc);
	}
	
	/**
	 * Returns the amplitude of the musical direction at some point t between 0 and 
	 * the length of the direction. Unexpected behavior happens when t < 0 or
	 * t > length, based on the transformation function passed.
	 * 
	 * @param t the value for which to compute the amplitude of the direction.
	 * 
	 * @return the amplitude of the direction at the point t.
	 */
	public double value(double t) {
		return wedgeFunction(t);
	}
	
	public double getInitialDynamics() { 
		return initialDynamics; 
	}
	
	protected void setInitialDynamics(double d) {
		initialDynamics = d;
	}
	
	public double getFinalDynamics() {
		return finalDynamics;
	}
	
	protected void setFinalDynamics(double d) {
		finalDynamics = d;
	}
	
	public double getLength() {
		return length;
	}
	
	protected void setLength(double s) {
		length = s;
	}
	
	/**
	 * @return true if the initial dynamics are equal to the final dynamics, 
	 * 	false otherwise
	 */
	public boolean isFlat() {
		return initialDynamics == finalDynamics;
	}
	
	
	/**
	 * @return true if the length of the direction is 0, false otherwise.
	 */
	public boolean isEmpty() {
		return length == 0;
	}
	
	/**
	 * Does the low level computation of finding the amplitude at some value t.
	 * 
	 * @param t the value to compute the amplitude at.
	 * @return the amplitude at t.
	 */
	private double wedgeFunction(double t) {
		if (length == 0.0) {
			return 0.0;
		} else {
			return initialDynamics + (finalDynamics - initialDynamics) 
					* shapeFunction.f(t / length);
		}
	}
}
