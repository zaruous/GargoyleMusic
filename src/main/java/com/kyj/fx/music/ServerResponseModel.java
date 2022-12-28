/********************************
 *	프로젝트 : gargoyle-music
 *	패키지   : com.kyj.fx.music
 *	작성일   : 2017. 7. 18.
 *	프로젝트 : OPERA 
 *	작성자   : KYJ
 *******************************/
package com.kyj.fx.music;

import java.util.List;

/**
 * @author KYJ
 *
 */
public class ServerResponseModel {

	private String url;
	private String title;
	private List<String> hashes;

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public List<String> getHashes() {
		return hashes;
	}

	public void setHashes(List<String> hashes) {
		this.hashes = hashes;
	}

}
