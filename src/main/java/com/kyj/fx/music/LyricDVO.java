package com.kyj.fx.music;

public class LyricDVO {

	private LyricHeaderDVO header;
	private String time;
	private String lyric;
	private int min;
	private int sec;
	private int mills;

	public LyricDVO() {
	}

	public LyricDVO(LyricHeaderDVO header) {
		this.header = header;
	}

	public LyricHeaderDVO getHeader() {
		return header;
	}

	public void setHeader(LyricHeaderDVO header) {
		this.header = header;
	}

	public String getTime() {
		return time;
	}

	public void setTime(String time) {
		this.time = time;
	}

	public String getLyric() {
		return lyric;
	}

	public void setLyric(String lyric) {
		this.lyric = lyric;
	}

	public int getMin() {
		return min;
	}

	public void setMin(int min) {
		this.min = min;
	}

	public int getSec() {
		return sec;
	}

	public void setSec(int sec) {
		this.sec = sec;
	}

	public int getMills() {
		return mills;
	}

	public void setMills(int mills) {
		this.mills = mills;
	}

	@Override
	public String toString() {
		return "[" + time + "]" + lyric;
	}

}
