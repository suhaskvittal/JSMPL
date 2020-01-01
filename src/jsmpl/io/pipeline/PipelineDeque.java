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

import java.util.Map;
import java.util.NoSuchElementException;

/**
 * The PipelineDeque class implements a deque that passes data from
 * the head of the deque to the tail of the deque. The class also
 * manages all type-casting, ensuring that the destination of the
 * n-th node is compatible with the source of the (n+1)-th node.
 * 
 * The reason behind not using a LinkedList as the structure is
 * because insertions would be near impossible to account for. For
 * example, if the user removes a pipeline from the middle of the
 * structure, the result may not be compatible. With a deque, 
 * appending and removal is easy to handle.
 */
public class PipelineDeque {
	private PDNode head;
	private PDNode tail;
	private int size;
	
	public PipelineDeque() {
		head = null;
		tail = null;
		size = 0;
	}
	
	/**
	 * @param dataStores the sources and destinations required to pass
	 * 	data through. The dataStores array should contain
	 * 		source of pipeline 1, 
	 * 		destination of pipeline 1 (source of pipeline 2), 
	 * 		destination of pipeline 2 (source of pipeline 3), 
	 * 		... etc.
	 * 	As a result, there are N + 1 data stores in the array.
	 * @param kwargs any relevant kwargs to the pipelines in the deque.
	 */
	public void passData(Object[] dataStores, Map<String, Object> kwargs) {
		if (size == 0) {
			throw new NoSuchElementException(
					"There must be at least one pipeline in the deque to pass data through.");	
		}
		
		if (dataStores.length != size + 1) {
			throw new IllegalArgumentException(
					"There should be only " + (size + 1) + " data stores to use with the pipelines.");
		}
		
		PDNode curr = head;
		for (int i = 1; i < dataStores.length; i++) {
			curr.data.transferDataBetween(dataStores[i - 1], dataStores[i], kwargs);
			curr = curr.next;
		}
	}
	
	/**
	 * @param p the pipeline to add
	 * @return true if the pipeline was added successfully, false otherwise.
	 */
	public boolean addToFront(Pipeline<?, ?> p) {
		if (head == null) {
			head = new PDNode(p, null, null);
			tail = new PDNode(p, null, null);
		} else {
			if (!isValid(p, head.data)) {
				return false;
			}
			
			PDNode node = new PDNode(p, null, head);
			head.prev = node;
			head = node;
			
			if (size == 1) {
				tail.prev = node;
			}
		}
		size++;
		return true;
	}
	
	/**
	 * @param p the pipeline to add
	 * @return true if the pipeline was added successfully, false otherwise.
	 */
	public boolean addToBack(Pipeline<?, ?> p) {
		if (tail == null) {
			head = new PDNode(p, null, null);
			tail = new PDNode(p, null, null);
		} else {
			if (!isValid(tail.data, p)) {
				return false;
			}
			
			PDNode node = new PDNode(p, null, null);
			tail.next = node;
			tail = node;
			
			if (size == 1) {
				head.next = node;
			}
		}
		size++;
		return true;
	}
	
	/**
	 * @return the removed pipeline
	 */
	public Pipeline<?, ?> removeFromFront() {
		if (head == null) {
			throw new NoSuchElementException("The deque is empty.");
		}
		PDNode node = head;
		
		if (size == 1) {
			head = null;
			tail = null;
		} else {
			head = node.next;
			head.prev = null;
		}
		size--;
		return node.data;
	}
	
	/**
	 * @return the removed pipeline
	 */
	public Pipeline<?, ?> removeFromBack() {
		if (tail == null) {
			throw new NoSuchElementException("The deque is empty.");
		}
		PDNode node = head;
		
		if (size == 1) {
			head = null;
			tail = null;
		} else {
			tail = node.prev;
			tail.next = null;
		}
		size--;
		return node.data;
	}
	
	public int size() {
		return size;
	}
	
	public Pipeline<?, ?> peekHead() {
		return head == null ? null : head.data;
	}
	
	public Pipeline<?, ?> peekTail() {
		return tail == null ? null : tail.data;
	}
	
	public void clear() {
		size = 0;
		head = null;
		tail = null;
	}
	
	private static boolean isValid(Pipeline<?, ?> left, Pipeline<?, ?> right) {
		return right.getSourceType().isAssignableFrom(left.getDestinationType());
	}
	
	private static class PDNode {
		private Pipeline<?, ?> data;
		private PDNode prev;
		private PDNode next;
		
		public PDNode(Pipeline<?, ?> data, PDNode prev, PDNode next) {
			this.data = data;
			this.prev = prev;
			this.next = next;
		}
	}
}
