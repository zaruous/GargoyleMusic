/********************************
 *	프로젝트 : gargoyle-music
 *	패키지   : com.kyj.fx.music
 *	작성일   : 2017. 5. 16.
 *	작성자   : KYJ
 *******************************/
package com.kyj.fx.music;

import java.io.File;
import java.io.IOException;

import com.mpatric.mp3agic.InvalidDataException;
import com.mpatric.mp3agic.Mp3File;
import com.mpatric.mp3agic.UnsupportedTagException;

/**
 * @author KYJ
 *
 */
public class GMp3File extends Mp3File {

	private File file;

	public GMp3File(File file) throws IOException, UnsupportedTagException, InvalidDataException {
		super(file);
		this.file = file;
	}

	public File getFile() {
		return this.file;
	}

}
