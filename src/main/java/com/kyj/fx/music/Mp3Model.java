/********************************
 *	프로젝트 : gargoyle-music
 *	패키지   : com.kyj.fx.music
 *	작성일   : 2017. 4. 28.
 *	작성자   : KYJ
 *******************************/
package com.kyj.fx.music;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import com.kyj.fx.commons.utils.ValueUtil;
import com.mpatric.mp3agic.ID3v1;
import com.mpatric.mp3agic.ID3v2;

/**
 * @author KYJ
 *
 */
public class Mp3Model {

	private GMp3File mp3File;

	/**
	 * change local variable name. <br/>
	 * 
	 * @최초생성일 2018. 7. 3.
	 */
	private static InputStream emptyStream = new InputStream() {

		@Override
		public int read() throws IOException {
			/*
			 * change. <br/> 18.07.03 kyj. <br/>
			 * 
			 * <value> from '0' to '-1' </value>
			 */
			return -1;
		}
	};

	public Mp3Model(GMp3File mp3File) {
		super();
		this.mp3File = mp3File;
	}

	public File getFile() {
		return this.mp3File.getFile();
	}

	public GMp3File getMp3File() {
		return mp3File;
	}

	public void setMp3File(GMp3File mp3File) {
		this.mp3File = mp3File;
	}

	public InputStream getAlbumImage() {
		if (mp3File.hasId3v2Tag()) {
			ID3v2 id3v2Tag = mp3File.getId3v2Tag();
			byte[] albumImage = id3v2Tag.getAlbumImage();

			if (albumImage == null)
				return emptyStream;

			return new ByteArrayInputStream(albumImage);
		}

		return emptyStream;
	}

	public int getFrameCount() {
		return mp3File.getFrameCount();
	}

	public int getBitrate() {
		return mp3File.getBitrate();
	}

	public String getFileName() {
		return mp3File.getFilename();
	}

	public String getTitle() {
		String title = null;

		if (mp3File.hasId3v1Tag()) {
			ID3v1 id3v1Tag = mp3File.getId3v1Tag();
			title = id3v1Tag.getTitle();
		}

		if (ValueUtil.isEmpty(title) && mp3File.hasId3v2Tag()) {
			title = mp3File.getId3v2Tag().getTitle();
		}

		if (ValueUtil.isEmpty(title))
			title = "unknown";
		return title;
	}

	/**
	 * @return
	 */
	public boolean hasID3v1Tag() {
		return mp3File.hasId3v1Tag();
	}

	/**
	 * @return
	 */
	public boolean hasID3v2Tag() {
		return mp3File.hasId3v2Tag();
	}

	/**
	 * @return
	 */
	public boolean hasXingFrame() {
		return mp3File.hasXingFrame();
	}

	public String getAritist() {
		String artist = null;

		if (mp3File.hasId3v2Tag()) {
			ID3v2 id3v2Tag = mp3File.getId3v2Tag();
			artist = id3v2Tag.getAlbumArtist();
			if (ValueUtil.isEmpty(artist))
				artist = id3v2Tag.getOriginalArtist();
		}

		if (ValueUtil.isEmpty(artist) && mp3File.hasId3v1Tag()) {
			ID3v1 id3v1Tag = mp3File.getId3v1Tag();
			artist = id3v1Tag.getArtist();
		}

		if (ValueUtil.isEmpty(artist))
			artist = "unknown";

		return artist;
	}

	/**
	 * 음악파일에 기록된 가사
	 * 
	 * @return
	 */
	public String getRyric() {

		if (mp3File.hasId3v2Tag()) {
			ID3v2 id3v2Tag = mp3File.getId3v2Tag();
			return id3v2Tag.getLyrics();
		}

		if (mp3File.hasId3v1Tag()) {

			return "";
		}

		return "";
	}

}
