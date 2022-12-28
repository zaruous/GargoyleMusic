/********************************
 *	프로젝트 : gargoyle-music
 *	패키지   : com.kyj.fx.music
 *	작성일   : 2017. 6. 7.
 *	작성자   : KYJ
 *******************************/
package com.kyj.fx.music;

import java.io.File;
import java.io.IOException;

import com.kyj.fx.commons.fx.controls.CloseableParent;
import com.kyj.fx.commons.utils.GargoyleOpenExtension;

/**
 * @author KYJ
 *
 */

public class MusicPlayerCompositeWrapper extends CloseableParent<MusicPlayerComposite2> implements GargoyleOpenExtension {

	public MusicPlayerCompositeWrapper() {
		super(new MusicPlayerComposite2());
	}

	@Override
	public void close() throws IOException {
		((MusicPlayerComposite2) getParent()).close();
	}

	@Override
	public boolean canOpen(File file) {
		return getParent().canOpen(file);
	}

	@Override
	public void setOpenFile(File file) {
		getParent().setOpenFile(file);
	}
}