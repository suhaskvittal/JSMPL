package jsmpl.score.ext;

import jsmpl.entity.Entity;
import jsmpl.score.Note;

/**
 * A note that is a wrapper for two notes played without 
 * release. For example, passages with legato can be
 * represented as one SequentialNote object, as one would 
 * simply nest SequentialNote objects to create a long
 * legato passage. Similarly, chained ties can be 
 * represented with nested SequentialNote objects.
 */
public class SequentialNote extends Note {
	/**
	 * 
	 */
	private static final long serialVersionUID = -3545896681470948428L;
	private Note first, second;
	
	public SequentialNote() {
		super(0.0, 0.0);
		
		first = new Note(0.0, 0.0);
		second = new Note(0.0, 0.0);
	}
	
	public SequentialNote(Note note1) {
		super(note1.getFrequency(), note1.getLength(), note1.getVolume());
		
		first = note1;
		second = new Note(0.0, 0.0);
	}
	
	public SequentialNote(Note note1, Note note2) {
		super(note1.getFrequency(), note1.getLength() + note2.getLength(), note1.getVolume());
		
		first = note1;
		second = note2;
	}
	
	public Note getFirstNote() {
		return first;
	}
	
	public Note getSecondNote() {
		return second;
	}
	
	public void setFirstNote(Note n) {
		first = n;
	}
	
	public void setSecondNote(Note n) {
		second = n;
	}
	
	public double[] produceData(Entity entity, double samplingRate) {
		double[] samples = new double[(int) (getLength() * samplingRate)];
		
		double[] fsamp = entity.playPitch(first.getFrequency(), getLength(), first.getVolume(), samplingRate);
		double[] ssamp = entity.playPitch(second.getFrequency(), getLength(), second.getVolume(), samplingRate);
		// we get samples the length of the encapsulating sequation note
		// because we want to mimic the slur effect, which is simply a 
		// movement between pitches within the same envelope.
		
		int firstLength = (int) (first.getLength() * samplingRate);
		int secondLength = (int) (second.getLength() * samplingRate);
		
		int k = 0;
		for (int i = 0; i < firstLength; i++) {
			samples[k++] = fsamp[i];
		}
		
		for (int i = firstLength; i < secondLength; i++) {
			samples[k++] = ssamp[i];
		}
		
		return samples;
	}
}
