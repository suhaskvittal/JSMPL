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

import java.lang.reflect.ParameterizedType;
import java.util.HashMap;
import java.util.Map;

/**
 * The Pipeline class is an abstract structure that aims
 * to aid users to develop larger musical programs. The 
 * Pipeline class is inherently communal by providing
 * tools to regulate data transfer and ensure valid
 * arguments are being provided.
 * 
 * Every pipeline has two ends: a source, and a destination.
 * Data is taken from the source (or perhaps the source IS
 * the data!), and transformed in some way before being sent
 * to the destination.
 * 
 * For example, in the ScorePlayerPipeline class, the source
 * is a Score, while the destination is a String. The pipeline
 * transfers sample data from instruments in the score to .WAV
 * files of the form "<dest_string>_<inst_num>.wav".
 * 
 * The best way to implement a subclass of Pipeline<S, D> is
 * to (replace S and D with the actual classes of course)
 * 	1) Create a method of signature
 * 			void transferDataBetween(S src, D dest,
 * 								Map<String, Object> kwargs)
 *  2) Override the method
 *  		void transferDataBetween(Object src, Object dest, 
 *  							Map<String, Object> kwargs)
 *  	to call the super class method and then call the 
 *  	new method with specific types.
 *  
 *  Note that the transferDataBetween method specified in this 
 *  class only checks if the types of the src and dest parameters
 *  match those indicated in the generic parameters for the class.
 *  
 *  This allows for two methods -- one where the types of the inputs 
 *  are unknown, and one where they are known. Unknown inputs appear
 *  can appear, especially when working with general Pipeline objects,
 *  as in the PipelineDeque class.
 * 
 * @param <S> the source type
 * @param <D> the destination type
 */
public abstract class Pipeline<S, D> {
	public void transferDataBetween(Object src, Object dest, Map<String, Object> kwargs) {
		if (!src.getClass().equals(getSourceType()) || 
				!dest.getClass().equals(getDestinationType())) {
			throw new ClassCastException("The source or destination parameter are not of the correct type.");
		}
	}
	
	@SuppressWarnings("unchecked")
	public Class<S> getSourceType() {
		return ((Class<S>) ((ParameterizedType) getClass()
			      .getGenericSuperclass()).getActualTypeArguments()[0]);
	}
	
	@SuppressWarnings("unchecked")
	public Class<D> getDestinationType() {
		return ((Class<D>) ((ParameterizedType) getClass()
			      .getGenericSuperclass()).getActualTypeArguments()[1]);
	}
	
	protected void validateRequiredKwargs(Map<String, Class<?>> requiredKwargs, Map<String, Object> args) {
		for (String kw : requiredKwargs.keySet()) {
			Class<?> type = requiredKwargs.get(kw);
			
			if (!args.containsKey(kw)) {
				throw new IndexOutOfBoundsException("Missing required keyword argument \"" + kw + "\".");
			}
			
			if (!type.isAssignableFrom(args.get(kw).getClass())) {
				throw new ClassCastException("Required keyword \"" + kw + "\" is not of type " + type + ".");
			}
		}
	}
	
	protected Map<String, Boolean> validateOptionalKwargs(Map<String, Class<?>> optionalKwargs, Map<String, Object> args) {
		Map<String, Boolean> validationMap = new HashMap<String, Boolean>();
		
		for (String kw : optionalKwargs.keySet()) {
			// if the keyword is not in args or is not valid in args
			// then we put (kw, false) in the validationMap. This indicates
			// that the keyword is not valid in args, so one must account for 
			// this. Otherwise, we put true, and the client's arg for that
			// keyword is valid.
			
			Class<?> type = optionalKwargs.get(kw);
			
			if (args.containsKey(kw) && type.isAssignableFrom(args.get(kw).getClass())) {
				validationMap.put(kw, true);
			} else {
				validationMap.put(kw, false);
			}
		}
		
		return validationMap;
	}
}