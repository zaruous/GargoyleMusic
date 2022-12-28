/********************************
 *	프로젝트 : gargoyle-music
 *	패키지   : com.kyj.fx.music
 *	작성일   : 2017. 6. 7.
 *	작성자   : KYJ
 *******************************/
package com.kyj.fx.music;

import java.io.IOException;
import java.util.stream.Stream;

import com.kyj.fx.commons.fx.controls.CloseableParent;

import javafx.beans.property.ObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.geometry.Orientation;
import javafx.scene.control.Slider;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.media.AudioEqualizer;
import javafx.scene.media.EqualizerBand;
import javafx.scene.media.MediaPlayer;

/**
 * @author KYJ
 *
 */
public class EqualizerBandComposite extends CloseableParent<BorderPane> {

	private static final int EQUALIZER_BAND_COUNT = 9;
	private ObjectProperty<MediaPlayer> currentPlayingMedia;

	public EqualizerBandComposite(ObjectProperty<MediaPlayer> currentPlayingMedia) {
		super(new BorderPane(), false);
		this.currentPlayingMedia = currentPlayingMedia;
		init();
		createMediaChangeListener();
		updateValues();
	}

	private void updateValues() {
		MediaPlayer mediaPlayer = this.currentPlayingMedia.get();
		if (mediaPlayer == null)
			return;

		AudioEqualizer audioEqualizer = mediaPlayer.getAudioEqualizer();
		ObservableList<EqualizerBand> bands = audioEqualizer.getBands();
		for (int i = 0; i < EQUALIZER_BAND_COUNT; i++) {
			EqualizerBand equalizerBand = bands.get(i);
			Slider slider = array[i];
			equalizerBand.setGain(slider.getValue());
		}
	}

	ChangeListener<MediaPlayer> listener = new ChangeListener<MediaPlayer>() {
		@Override
		public void changed(ObservableValue<? extends MediaPlayer> oba, MediaPlayer o, MediaPlayer n) {

			if (n == null)
				return;

			updateValues();
		}
	};

	private void createMediaChangeListener() {
		this.currentPlayingMedia.addListener(listener);
	}

	private Slider[] array;

	private void init() {

		/*
		 * Band Index Center Frequency (Hz) Bandwidth (Hz) 0 32 19 1 64 39 2 125 78 3 250 156 4 500 312 5 1000 625 6 2000 1250 7 4000 2500 8
		 * 8000 5000 9 16000 10000
		 */

		array = Stream.iterate(0, a -> a + 1).limit(EQUALIZER_BAND_COUNT).map(idx -> {
			Slider slider = new Slider(EqualizerBand.MIN_GAIN, EqualizerBand.MAX_GAIN, 1);
			slider.setId(idx+"");
			slider.setOrientation(Orientation.VERTICAL);
			slider.setShowTickLabels(true);
			slider.valueProperty().addListener(new ChangeListener<Number>() {

				@Override
				public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
					
					MediaPlayer mediaPlayer = currentPlayingMedia.get();
					if (mediaPlayer == null)
						return;
					
					String id = slider.getId();
					EqualizerBand equalizerBand = mediaPlayer.getAudioEqualizer().getBands().get(Integer.parseInt(id));
					equalizerBand.setGain(newValue.doubleValue());
				}
			});
			return slider;
		}).toArray(Slider[]::new);
		HBox hBox = new HBox(5, array);
		getParent().setCenter(hBox);
	}

	@Override
	public void close() throws IOException {

		// remove listener
		this.currentPlayingMedia.addListener(listener);
	}

}
