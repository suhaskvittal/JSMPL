package jsmpl.io;

import java.io.File;
import java.io.IOException;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

public class WAVFileInputStream extends AudioInputStream {
	/**
	 * @param s the path of the file to read from
	 * @throws UnsupportedAudioFileException
	 * @throws IOException
	 */
	public WAVFileInputStream(String s) throws UnsupportedAudioFileException, IOException {
		this(AudioSystem.getAudioInputStream(new File(s)));
	}
	
	
	/**
	 * @param f the file to read from
	 * @throws UnsupportedAudioFileException
	 * @throws IOException
	 */
	public WAVFileInputStream(File f) throws UnsupportedAudioFileException, IOException {
		this(AudioSystem.getAudioInputStream(f));
	}
	
	/**
	 * AudioSystem sets up an AudioInputStream based on the file, so
	 * we should just use that input stream.
	 * 
	 * @param inputStream an AudioInputStream that streams data from the
	 * 	wave file.
	 * @throws IOException 
	 * @throws UnsupportedAudioFileException 
	 * */
	public WAVFileInputStream(AudioInputStream inputStream) throws UnsupportedAudioFileException, IOException {
		super(inputStream, inputStream.getFormat(), inputStream.getFrameLength());
		
		if (AudioSystem.getAudioFileFormat(inputStream).getType() != AudioFileFormat.Type.WAVE) {
			throw new IllegalArgumentException(
					"The file passed is not a .wav file OR the input stream passed does not reference a .wav file.");
		}
	}
}
