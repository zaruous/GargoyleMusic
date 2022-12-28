/********************************
 *	프로젝트 : gargoyle-music
 *	패키지   : com.kyj.fx.music
 *	작성일   : 2017. 6. 7.
 *	작성자   : KYJ
 *******************************/
package com.kyj.fx.music;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kyj.fx.commons.memory.StageStore;
import com.kyj.fx.commons.threads.ExecutorDemons;
import com.kyj.fx.commons.utils.DialogUtil;
import com.kyj.fx.commons.utils.FileUtil;
import com.kyj.fx.commons.utils.FxUtil;
import com.kyj.fx.commons.utils.GargoyleExtensionFilters;
import com.kyj.fx.commons.utils.GargoyleOpenExtension;
import com.kyj.fx.commons.utils.RequestUtil;
import com.kyj.fx.commons.utils.ValueUtil;
import com.kyj.fx.fxloader.FXMLController;
import com.kyj.fx.fxloader.FxPostInitialize;
import com.mpatric.mp3agic.ID3v2;
import com.mpatric.mp3agic.ID3v2TagFactory;
import com.mpatric.mp3agic.InvalidDataException;
import com.mpatric.mp3agic.UnsupportedTagException;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.event.EventTarget;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Slider;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.TextFieldListCell;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.InnerShadow;
import javafx.scene.effect.Reflection;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Screen;
import javafx.util.Callback;
import javafx.util.Duration;
import javafx.util.Pair;
import javafx.util.StringConverter;

/**
 * @author KYJ
 *
 */
@FXMLController(value = "MusicPlayerView2.fxml", isSelfController = true, css = "MusicPlayerView2.css")
public class MusicPlayerComposite2 extends BorderPane implements EventTarget, Closeable, GargoyleOpenExtension {
	private static final Logger LOGGER = LoggerFactory.getLogger(MusicPlayerComposite2.class);

	private File musicDir;

	private ExecutorService newFixedThreadExecutor = ExecutorDemons.newFixedThreadExecutor("Gargoyle-MP3-THREAD-POLL", 1);

	@FXML
	private MediaView mvPlayer;
	@FXML
	private ImageView iv;

	@FXML
	private VBox vboxMenus;

	@FXML
	private ListView<File> lvMusic;
	@FXML
	private Label lblLyric, lblTime, lblTotalTime;
	@FXML
	private TextField txtGargoyleMusicURL;
	@FXML
	private Label lblTitle, lblDesc, lblMusicList, lblMyPlayList;
	@FXML
	private Slider slVolumn, slProgress;
	@FXML
	private Button btnSrch;
	@FXML
	private ComboBox<PlayListDVO> cbPlayList;
	@FXML
	private StackPane st;

	private ObjectProperty<MediaPlayer> currentPlayingMedia = new SimpleObjectProperty<>();

	private ObjectProperty<Mp3Model> selectMp3Model = new SimpleObjectProperty<>();

	private ObjectProperty<STATUS> status = new SimpleObjectProperty<>();

	private ImageView ivPause = new ImageView(new Image(MusicPlayerComposite2.class.getResourceAsStream("pause.png")));

	private ObjectProperty<TreeMap<String, String>> currentLyric = new SimpleObjectProperty<>(new TreeMap<>());

	enum STATUS {
		play, pause, stop
	}

	private CrudService saveService;

	public MusicPlayerComposite2() {
		saveService = CrudService.getInstance();

		FxUtil.loadRoot(MusicPlayerComposite2.class, this, err -> {
			LOGGER.error(ValueUtil.toString(err));
		});

		// getStylesheets().add(MusicPlayerComposite2.class.getResource("MusicPlayerView2.css").toExternalForm());

	}

	public MusicPlayerComposite2(File musicDir) {
		this();
		this.musicDir = musicDir;

	}

	@FXML
	public void initialize() throws Exception {

		this.lblMusicList.setGraphic(
				new ImageView(new Image(MusicPlayerComposite2.class.getResourceAsStream("musicNode.png"), 25, 25, false, false)));
		this.lblMyPlayList.setGraphic(
				new ImageView(new Image(MusicPlayerComposite2.class.getResourceAsStream("musicNode.png"), 25, 25, false, false)));

		this.lvMusic.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

		Callback<ListView<File>, ListCell<File>> callback = new Callback<ListView<File>, ListCell<File>>() {

			@Override
			public ListCell<File> call(ListView<File> param) {
				// Callback<File, ObservableValue<Boolean>> callback = param1 ->
				// new
				// SimpleBooleanProperty();

				StringConverter<File> converter = new StringConverter<File>() {

					@Override
					public String toString(File f) {
						return f.getName();
					}

					@Override
					public File fromString(String string) {
						return null;
					}
				};
				// CheckBoxListCell<File> checkBoxListCell = new
				// CheckBoxListCell<File>(callback, converter);

				TextFieldListCell<File> cell = new TextFieldListCell<>(converter);
				return cell;
			}
		};
		this.lvMusic.setCellFactory(callback);

		this.cbPlayList.setConverter(new StringConverter<PlayListDVO>() {

			@Override
			public String toString(PlayListDVO object) {
				if (object == null)
					return "";
				return object.getTitle();
			}

			@Override
			public PlayListDVO fromString(String title) {
				Optional<PlayListDVO> findFirst = cbPlayList.getItems().stream().filter(v -> title.equals(v.getTitle())).findFirst();
				if (findFirst.isPresent())
					return findFirst.get();
				return null;
			}
		});
		this.lvMusic.setContextMenu(createContextMenu());

		EventHandler<? super MouseEvent> value = ev -> {

			if (ev.getButton() == MouseButton.PRIMARY) {

				if (ev.getClickCount() == 2) {

					MediaPlayer mediaPlayer = currentPlayingMedia.get();
					if (mediaPlayer != null) {
						mediaPlayer.stop();
						status.set(STATUS.stop);
					}

					firePlay();
				}

			}

		};

		this.lvMusic.setOnMouseClicked(value);
		this.lvMusic.getSelectionModel().selectedItemProperty().addListener(selectedItemChangeListener);

		this.addEventHandler(AlbumActionEvent.ALBUM_ACTION, eventHandler);

		this.slVolumn.valueProperty().addListener(new ChangeListener<Number>() {

			@Override
			public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {

				MediaPlayer mediaPlayer = currentPlayingMedia.get();
				if (mediaPlayer != null)
					mediaPlayer.setVolume(newValue.doubleValue());
			}
		});

		this.currentPlayingMedia.addListener(new ChangeListener<MediaPlayer>() {

			@Override
			public void changed(ObservableValue<? extends MediaPlayer> observable, MediaPlayer oldValue, MediaPlayer newValue) {

				LOGGER.debug(" onchanged current media. ");
				final Mp3Model mp3Model = selectMp3Model.get();

				newValue.setOnReady(new Runnable() {
					@Override
					public void run() {

						ExecutorDemons.getGargoyleSystemExecutorSerivce().execute(() -> {

							if (mp3Model != null && mp3Model.getFile() != null) {
								File file = mp3Model.getFile();

								List<LyricDVO> ryric = LyricMnager.getInstance().getRyric(file);
								if (ryric == null || ryric.size() == 0) {
									LOGGER.debug("가사가 없어요.");

									Platform.runLater(() -> {

										lblLyric.setText(mp3Model.getRyric());

									});

								} else {
									Platform.runLater(() -> {
										Map<String, String> collect = ryric.stream().filter(v -> !"00:00.00".equals(v.getTime()))
												.collect(() -> {
													return new TreeMap<String, String>();
												}, (a, b) -> {
													String key = String.format("%02d:%02d", b.getMin(), b.getSec());
													String value1 = b.getLyric();
													a.put(key, value1);
												}, (a, b) -> a.putAll(b));

										currentLyric.get().putAll(collect);
									});
								}
							}

						});

						slProgress.setValue(0.0);
						Duration duration = newValue.getMedia().getDuration();

						// 종료시점을 알 수 없는 경우
						if (duration.isUnknown()) {
							slProgress.setMax(0.0);
							slProgress.setDisable(true);
						}
						// 종료시점을 알 수 있는 경우
						else {
							slProgress.setDisable(false);
							double mills = duration.toMillis();
							slProgress.setMax(mills);

							int cSec = (int) (mills / 1000);
							int min = (int) (cSec / 60);
							int sec = (int) (cSec % 60);
							lblTime.setText("00:00");
							lblTotalTime.setText(String.format("%02d:%02d", min, sec));
						}

						currentLyric.get().clear();
						lblLyric.setText("");

					}

				});
				newValue.setOnPlaying(() -> {
					LOGGER.debug("playing...");
					showNotification(mp3Model);
				});

				newValue.setOnEndOfMedia(() -> {
					LOGGER.debug("end of media.");
					btnNextOnAction();
					// 메모리 해제.
					if (oldValue != null)
						oldValue.dispose();
				});

				if (oldValue != null) {
					oldValue.currentTimeProperty().removeListener(mpTimeChangeListener);
					// 메모리 해제.
					oldValue.dispose();
				}

				newValue.currentTimeProperty().addListener(mpTimeChangeListener);
			}

		});

		slProgress.setOnMousePressed(ev -> {

			MediaPlayer mp = currentPlayingMedia.get();
			if (mp != null && status.get() == STATUS.play) {
				lblLyric.setText("");
				mp.pause();
				status.set(STATUS.pause);
				mp.seek(Duration.millis(slProgress.getValue()));
			}

		});

		slProgress.setOnMouseDragged(ev -> {
			double v = slProgress.getValue();
			int val = (int) v / 1000;
			int min = val / 60;
			int sec = val % 60;
			String key = String.format("%02d:%02d", min, sec);
			lblTime.setText(key);

			MediaPlayer mp = currentPlayingMedia.get();
			if (mp != null && status.get() == STATUS.pause) {
				mp.seek(Duration.millis(v));
			}

		});

		slProgress.setOnMouseReleased(ev -> {
			MediaPlayer mp = currentPlayingMedia.get();
			if (mp != null && status.get() == STATUS.pause) {

				//
				// 재생 속도
				// mp.setRate(1);
				// 사운드 좌우조절
				// mp.setBalance(0);
				// 음소거
				// mp.setMute(false);
				// mp.seek(dur);
				mp.play();
				status.set(STATUS.play);
			}

		});

		this.status.addListener((oba, o, n) ->

		{

			switch (n) {

			case pause:
				ColorAdjust colorAdjust = new ColorAdjust();
				colorAdjust.setBrightness(-0.5d);
				this.iv.setEffect(colorAdjust);
				this.st.getChildren().add(ivPause);
				break;

			default:
				DropShadow ds = new DropShadow();
				ds.setOffsetY(10.0);
				ds.setOffsetX(10.0);
				ds.setColor(Color.GRAY);

				// Reflection ref = new Reflection();
				// ref.setFraction(0.1);

				// ds.setInput(ref);

				// ds.setRadius(100d);

				this.iv.setEffect(ds);
				this.st.getChildren().remove(ivPause);
			}
		});

		DropShadow ds = new DropShadow();
		ds.setOffsetY(5.0);
		ds.setOffsetX(5.0);
		ds.setColor(Color.BLACK);

		Reflection ref = new Reflection();
		ref.setFraction(0.1);
		ref.setTopOffset(1.0d);
		ds.setInput(ref);

		ds.setRadius(30d);

		this.iv.setEffect(ds);

		{
			InnerShadow is = new InnerShadow();
			is.setOffsetX(2.0f);
			is.setOffsetY(2.0f);
			is.setColor(Color.BLACK);
			this.lblTitle.setTextFill(Color.DARKGREEN);
			this.lblTitle.setEffect(is);
		}

	}

	@FxPostInitialize
	public void postInit() {
		this.addNewMusic(this.musicDir);
		List<PlayListDVO> loadPlayList = saveService.loadPlayList();
		this.cbPlayList.getItems().addAll(loadPlayList);

		double volumn = saveService.getVolumn();
		this.slVolumn.setValue(volumn);
	}

	/**
	 * image caching. <br/>
	 * 
	 * @최초생성일 2017. 10. 30.
	 */
	ObjectProperty<byte[]> imageBuffer = new SimpleObjectProperty<>();
	private ChangeListener<File> selectedItemChangeListener = new ChangeListener<File>() {

		@Override
		public void changed(ObservableValue<? extends File> observable, File oldValue, File newValue) {

			// ObservableList<File> selectedItems =
			// lvMusic.getSelectionModel().getSelectedItems();

			if (newValue != null && newValue.exists()) {

				// 음악 배경 로딩 속도를 향상시키기 위한 작업

				preImageLoading(newValue);

				metadataLoading(newValue);

			}
		}
	};

	/***************************************************************************************/
	// 메타데이터 로딩

	private void preImageLoading(File mp3File) {
		try {
			byte[] buffer = Files.readAllBytes(mp3File.toPath());
			preImageLoading(buffer);
		} catch (Exception e) {
		}
	}

	private void preImageLoading(byte[] mp3data) {
		try {

			Image image = iv.getImage();
			if (image != null) {
				image.cancel();
			}

			LOGGER.debug("image loading...");
			ID3v2 id3v24tag = ID3v2TagFactory.createTag(mp3data);
			byte[] imagesBuffer = id3v24tag.getAlbumImage();
			imageBuffer.set(imagesBuffer);
			iv.setImage(new Image(new ByteArrayInputStream(imagesBuffer), 400d, 400d, false, false));
		} catch (Exception e) {
			iv.setImage(new Image(new ByteArrayInputStream(new byte[0]), 400d, 400d, false, false));
		}
	}

	private void metadataLoading(File f) {
		try {
			metadataLoading(new GMp3File(f));
		} catch (UnsupportedTagException | InvalidDataException | IOException e) {
			LOGGER.error(ValueUtil.toString(e));
		}
	}

	private void metadataLoading(GMp3File f) {
		metadataLoading(new Mp3Model(f));
	}

	private void metadataLoading(Mp3Model mp3Model) {
		int bitrate = mp3Model.getBitrate();
		int frameCount = mp3Model.getFrameCount();
		LOGGER.debug("bit rate : {} Frame Count : {}  ", bitrate, frameCount);
		// iv.setImage(new Image(mp3Model.getAlbumImage(), 400d, 400d, false,
		// false));
		lblTitle.setText(mp3Model.getTitle());
		lblDesc.setText(mp3Model.getAritist());
		selectMp3Model.set(mp3Model);
	}

	/***************************************************************************************/

	private ContextMenu createContextMenu() {

		MenuItem removeSelection = new MenuItem("선택 삭제");
		removeSelection.setOnAction(ev -> {
			ObservableList<File> selectedItems = this.lvMusic.getSelectionModel().getSelectedItems();

			PlayListDVO selectedItem = cbPlayList.getSelectionModel().getSelectedItem();
			String title = selectedItem.getTitle();
			saveService.removePlayList(title, selectedItems);

			this.lvMusic.getItems().removeAll(selectedItems);
		});

		MenuItem addNewPlayList = new MenuItem("재생 목록에 추가");
		addNewPlayList.setDisable(true);
		addNewPlayList.setOnAction(ev -> {

			ObservableList<File> selectedItems = lvMusic.getSelectionModel().getSelectedItems();
			PlayListDVO selectedItem = cbPlayList.getSelectionModel().getSelectedItem();
			if (selectedItem != null) {
				String title = selectedItem.getTitle();
				saveService.addNewPlayList(title, selectedItems);
			}

		});

		cbPlayList.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<PlayListDVO>() {

			@Override
			public void changed(ObservableValue<? extends PlayListDVO> observable, PlayListDVO oldValue, PlayListDVO newValue) {
				if (newValue != null) {
					addNewPlayList.setDisable(false);
				} else
					addNewPlayList.setDisable(false);

				if (newValue != null) {
					List<PlayListDetailDVO> loadPlayList = saveService.loadPlayList(newValue.getTitle());

					List<File> collect = loadPlayList.stream().map(d -> d.getLocation()).filter(ValueUtil::isNotEmpty).map(File::new)
							.collect(Collectors.toList());

					lvMusic.getItems().setAll(collect);
				}
			}
		});

		MenuItem miShowHash = new MenuItem("해쉬값 보기");
		miShowHash.setOnAction(ev -> {
			File selectedItem = lvMusic.getSelectionModel().getSelectedItem();
			if (selectedItem != null) {
				String hash = LyricMnager.getInstance().getHash(selectedItem);
				FxUtil.createSimpleTextAreaAndShow(hash, s -> {
					s.initOwner(StageStore.getPrimaryStage());
				});
			}
		});

		MenuItem showLyric = new MenuItem("전체 가사 보기");
		showLyric.setOnAction(ev -> {
			File selectedItem = lvMusic.getSelectionModel().getSelectedItem();
			if (selectedItem != null) {

				// String hash =
				// LyricMnager.getInstance().getHash(selectedItem);

				List<LyricDVO> ryric = LyricMnager.getInstance().getRyric(selectedItem);
				Stream<LyricDVO> stream = ryric.stream();

				Optional<String> reduce = stream.map(v -> {
					return String.format("[%s] %s", v.getTime(), v.getLyric());
				}).reduce((str1, str2) -> str1.concat("\n").concat(str2));

				Consumer<String> consumer = str -> {
					FxUtil.createCodeAreaAndShow(str, stage -> {

						Screen screen = Screen.getScreens().get(0);
						Rectangle2D visualBounds = screen.getVisualBounds();

						stage.setWidth(350d);
						stage.setHeight(visualBounds.getHeight());

						stage.setTitle(selectedItem.getName());
						stage.initOwner(StageStore.getPrimaryStage());
					});
				};

				if (reduce.isPresent()) {
					consumer.accept(reduce.get());
				} else {
					try {
						consumer.accept(new Mp3Model(new GMp3File(selectedItem)).getRyric());
					} catch (UnsupportedTagException | InvalidDataException | IOException e) {
						e.printStackTrace();
					}
				}

			}
		});

		MenuItem bandwidth = new MenuItem("밴드위드");
		bandwidth.setOnAction(ev -> {
			FxUtil.createStageAndShow(new EqualizerBandComposite(currentPlayingMedia), stage -> {
			});
		});

		MenuItem fileLocation = new MenuItem("파일위치");
		fileLocation.setOnAction(ev -> {

			File selectedItem = lvMusic.getSelectionModel().getSelectedItem();

			if (selectedItem != null) {

				try {
					FileUtil.explorer(selectedItem);
				} catch (Exception e) {

					String absolutePath = selectedItem.getAbsolutePath();
					FxUtil.createSimpleTextAreaAndShow(absolutePath, s -> {
						s.initOwner(StageStore.getPrimaryStage());
					});

				}
			}

		});

		return new ContextMenu(removeSelection, addNewPlayList, miShowHash, showLyric, bandwidth, fileLocation);
	}

	/**
	 * for showing progress bar.
	 * 
	 * @최초생성일 2017. 10. 30.
	 */
	ChangeListener<Duration> mpTimeChangeListener = new ChangeListener<Duration>() {

		@Override
		public void changed(ObservableValue<? extends Duration> observable, Duration oldValue, Duration newValue) {
			MediaPlayer mp = currentPlayingMedia.get();
			if (status.getValue() == STATUS.play) {
				Duration currentTime = mp.getCurrentTime();
				double mills = currentTime.toMillis();
				int s = (int) mills / 1000;
				slProgress.setValue(mills);

				int minutes = s / 60;
				int seconds = s % 60;

				String key = String.format("%02d:%02d", minutes, seconds);
				lblTime.setText(key);

				String lyric1 = "";
				String lyric2 = "";
				String lyric3 = "";

				// 두번째
				Entry<String, String> ent = currentLyric.get().lowerEntry(key);
				if (ent != null) {
					lyric2 = ent.getValue();
				}

				// 첫번째
				if (ent != null) {
					ent = currentLyric.get().lowerEntry(ent.getKey());
					if (ent != null)
						lyric1 = ent.getValue();
				}

				// 세번째
				lyric3 = currentLyric.get().get(key);

				if (ValueUtil.isEmpty(lyric1))
					lyric1 = "";

				if (ValueUtil.isEmpty(lyric2))
					lyric2 = "";

				if (ValueUtil.isEmpty(lyric3))
					return;

				String format = String.format("%s\n%s\n%s", lyric1, lyric2, lyric3);

				lblLyric.setText(format);
			}

			// }

		}
	};

	/**
	 * 앨범이벤트핸들러
	 * 
	 * @최초생성일 2017. 6. 8.
	 */
	private EventHandler<? super AlbumActionEvent> eventHandler = ev -> {

		LOGGER.debug("Accepted. AlbumActionEvent handler");
		STATUS _currentStatus = status.get();
		final MediaPlayer _prev = currentPlayingMedia.get(); // mvPlayer.getMediaPlayer();
		String prevSource = _prev == null ? "" : _prev.getMedia().getSource();

		final Mp3Model mp3Model = ev.getMp3Model();
		File file = new File(mp3Model.getFileName());
		String newSource = file.toURI().toString();

		LOGGER.debug("current status : {} ", _currentStatus);
		if (_currentStatus == null) {

			final MediaPlayer mediaPlayer1 = createNewMedia(newSource);

			currentPlayingMedia.set(mediaPlayer1);
			mediaPlayer1.play();
			status.set(STATUS.play);
			// asign the current statue.
			_currentStatus = STATUS.play;
			return;
		}

		switch (_currentStatus) {
		case play:

			if (prevSource.equals(newSource)) {
				if (_prev != null) {
					_prev.pause();
					status.set(STATUS.pause);
				}
			} else {
				_prev.pause();
				status.set(STATUS.pause);
			}

			break;
		case pause:

			if (prevSource.equals(newSource)) {
				if (_prev != null) {
					_prev.play();
					status.set(STATUS.play);
				}
			} else {
				if (_prev != null) {
					_prev.stop();
					_prev.dispose();
					status.set(STATUS.stop);
				}
				final Media newMedia1 = new Media(newSource);
				final MediaPlayer mediaPlayer2 = createNewMedia(newMedia1);

				currentPlayingMedia.set(mediaPlayer2);
				mediaPlayer2.play();
				status.set(STATUS.play);
			}
			break;
		case stop:

			if (prevSource.equals(newSource)) {
				if (_prev != null) {
					_prev.play();
					status.set(STATUS.play);
				}
			} else {
				if (_prev != null) {
					_prev.stop();
					_prev.dispose();
					status.set(STATUS.stop);
				}

				LOGGER.debug("new player source : {} ", newSource);
				final Media newMedia2 = new Media(newSource);
				final MediaPlayer mediaPlayer3 = createNewMedia(newMedia2);

				currentPlayingMedia.set(mediaPlayer3);
				mediaPlayer3.play();
				status.set(STATUS.play);
			}

			break;
		default:

			if (_prev != null) {
				_prev.stop();
				_prev.dispose();
				status.set(STATUS.stop);
			}
			final Media newMedia3 = new Media(newSource);
			final MediaPlayer mediaPlayer4 = createNewMedia(newMedia3);

			currentPlayingMedia.set(mediaPlayer4);
			mediaPlayer4.play();
			status.set(STATUS.play);
			break;

		}

	};

	private MediaPlayer createNewMedia(File source) {
		return createNewMedia(source.toURI().toString());
	}

	private MediaPlayer createNewMedia(String source) {
		final Media newMedia = new Media(source);
		return createNewMedia(newMedia);
	}

	private MediaPlayer createNewMedia(Media source) {
		MediaPlayer mediaPlayer = new MediaPlayer(source);
		mediaPlayer.setVolume(slVolumn.getValue());
		double rate = mediaPlayer.getRate();
		double balance = mediaPlayer.getBalance();
		LOGGER.debug("balance : {} ", balance);
		LOGGER.debug("rate : {}", rate);
		return mediaPlayer;
	}

	/**
	 * 알림팝업 표시 <br/>
	 * 
	 * @작성자 : KYJ
	 * @작성일 : 2017. 6. 8.
	 * @param mp3Model
	 */
	private void showNotification(Mp3Model mp3Model) {

		byte[] buf = imageBuffer.get();
		Image image = null;
		if (buf != null) {
			image = new Image(new ByteArrayInputStream(buf), 50, 50, false, false);
		} else {
			image = new Image(new ByteArrayInputStream(new byte[] { 0 }), 50, 50, false, false);
		}

		Node graphics = new ImageView(image);
		String title = mp3Model.getTitle();
		String cont = mp3Model.getAritist();
		FxUtil.showNotification(graphics, title, cont, Pos.BOTTOM_RIGHT);
	}

	/**
	 * use only mp3 file extensions. <br/>
	 * 
	 * @최초생성일 2017. 10. 30.
	 */
	private Predicate<File> mp3Filter = f -> {
		return f != null && f.getName().endsWith(".mp3");
	};

	/**
	 * this service is called when music files are added. <br/>
	 * 
	 * @최초생성일 2017. 10. 30.
	 */
	private Service<Void> addNewMussicSerivce = new Service<Void>() {

		@Override
		protected Task<Void> createTask() {

			return new Task<Void>() {

				@Override
				protected Void call() throws Exception {
					FileWalk<File> fileWalk = new FileWalk<>(MusicPlayerComposite2.this.musicDir, 2000);
					fileWalk.setFilter(mp3Filter);
					fileWalk.setConvert(f -> {
						lvMusic.getItems().add(f);
						return f;
					});
					fileWalk.walk();
					return null;
				}
			};
		}
	};

	/**
	 * this function called when music file added. <br/>
	 * 
	 * @작성자 : KYJ
	 * @작성일 : 2017. 10. 30.
	 * @param file
	 */
	private void addNewMusic(File file) {
		if (file == null)
			return;

		if (file.isFile()) {
			if (mp3Filter.test(file)) {
				lvMusic.getItems().add(file);
			}
			return;
		}

		addNewMussicSerivce.setExecutor(newFixedThreadExecutor);
		addNewMussicSerivce.start();
	}

	@FXML
	public void btnPlayOnClick(MouseEvent e) {
		if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 1) {
			firePlay();
		}
	}

	@FXML
	public void pauseOnAction() {

		MediaPlayer mediaPlayer = currentPlayingMedia.get();
		if (status.get() == null)
			return;

		switch (status.get()) {
		case play:

			if (mediaPlayer != null) {
				mediaPlayer.pause();
				status.set(STATUS.pause);
			}

			break;

		case pause:
			if (mediaPlayer != null) {
				mediaPlayer.play();
				status.set(STATUS.play);
			}

		case stop:
			if (mediaPlayer != null) {
				mediaPlayer.play();
				status.set(STATUS.play);
			}

		default:
			break;
		}

	}

	@FXML
	public void btnSrchOnAction() {
		List<File> showMultiFileDialog = DialogUtil.showMultiFileDialog(getScene().getWindow(), chooser -> {
			chooser.getExtensionFilters().add(new ExtensionFilter(GargoyleExtensionFilters.MP3_NAME, GargoyleExtensionFilters.MP3));
		});

		this.lvMusic.getItems().addAll(showMultiFileDialog);

		if (!showMultiFileDialog.isEmpty()) {
			newFixedThreadExecutor.execute(() -> {

				PlayListDVO selectedItem = cbPlayList.getSelectionModel().getSelectedItem();
				if (selectedItem != null) {
					String title = selectedItem.getTitle();
					saveService.addNewPlayList(title, showMultiFileDialog);
				}

			});
		}

	}

	@FXML
	public void btnPlayListAddOnAction() {

		Optional<Pair<String, String>> showInputDialog = DialogUtil.showInputDialog(this, "신규 재생목록", "제목 입력.");
		showInputDialog.ifPresent(pair -> {
			String title = pair.getValue();

			if (ValueUtil.isNotEmpty(title)) {
				PlayListDVO playListDVO = new PlayListDVO();
				playListDVO.setTitle(title);

				if (!saveService.existsPlayList(title)) {
					saveService.addNewPlayList(title);
					this.cbPlayList.getItems().add(playListDVO);
					this.cbPlayList.getSelectionModel().select(playListDVO);
				} else {
					DialogUtil.showMessageDialog("이미 존재하는 재생목록입니다.");
				}
			}

		});
	}

	public void firePlay() {
		Mp3Model mp3Model = this.selectMp3Model.get();
		if (mp3Model != null) {
			new AlbumEvent(mp3Model).handle();
		}
	}

	private class AlbumEvent implements EventHandler<MouseEvent> {

		private Mp3Model f;

		public AlbumEvent(Mp3Model f) {
			this.f = f;
		}

		@Override
		public void handle(MouseEvent ev) {
			if (ev.getButton() == MouseButton.PRIMARY && ev.getClickCount() == 1) {
				handle();
			}
		}

		public void handle() {
			AlbumActionEvent albumClickEvent = new AlbumActionEvent(f, AlbumActionEvent.ALBUM_ACTION);
			AlbumActionEvent.fireEvent(MusicPlayerComposite2.this, albumClickEvent);
		}
	}

	@Override
	public void close() throws IOException {

		double value = this.slVolumn.getValue();
		saveService.saveVolumn(value);

		MediaPlayer mediaPlayer = currentPlayingMedia.get();
		if (mediaPlayer != null) {
			mediaPlayer.dispose();
		}

	}

	@FXML
	public void btnPrevOnAction() {
		status.set(STATUS.stop);

		Mp3Model mp3Model = selectMp3Model.get();

		ObservableList<File> items = lvMusic.getItems();
		int currentIdx = items.indexOf(mp3Model.getFile());

		if (currentIdx == -1) {
			System.err.println("can't not found current Idx");
			return;
		}

		int nextIdx = currentIdx - 1;
		File next = null;
		if (nextIdx == -1) {
			next = lvMusic.getItems().get(items.size() - 1);
			nextIdx = items.size() - 1;
		} else {
			next = lvMusic.getItems().get(nextIdx);
		}
		LOGGER.debug("next play : {}", next.getName());
		lvMusic.getSelectionModel().clearAndSelect(nextIdx);

		firePlay();
	}

	@FXML
	public void btnNextOnAction() {
		status.set(STATUS.stop);

		Mp3Model mp3Model = selectMp3Model.get();

		ObservableList<File> items = lvMusic.getItems();
		int currentIdx = items.indexOf(mp3Model.getFile());

		if (currentIdx == -1) {
			LOGGER.error("can't not found current Idx");
			return;
		}

		int nextIdx = currentIdx + 1;
		File next = null;
		if (items.size() > nextIdx) {
			next = lvMusic.getItems().get(nextIdx);
		} else {
			next = lvMusic.getItems().get(0);
			nextIdx = 0;
		}
		LOGGER.debug("next play : {} ", next.getName());
		lvMusic.getSelectionModel().clearAndSelect(nextIdx);

		firePlay();
	}

	@FXML
	public void onPropertiesContextRequest() {
		ContextMenu menu = new ContextMenu();

		// Music Server Properties
		{
			Menu miMusicServer = new Menu(" Music Server ");
			// Server On
			{
				MenuItem miMusicServerOn = new MenuItem(" Server On ");
				miMusicServerOn.setOnAction(this::musicServerOn);
				miMusicServer.getItems().add(miMusicServerOn);
			}
			// Server Off
			{
				MenuItem miMusicServerOff = new MenuItem(" Server Off ");
				miMusicServerOff.setOnAction(this::musicServerOff);
				miMusicServer.getItems().add(miMusicServerOff);
			}

			menu.getItems().add(miMusicServer);
		}
		menu.show(FxUtil.getWindow(this));
	}

	void musicServerOn(ActionEvent e) {

		// new Thread(() -> {
		// MusicServer create = MusicServer.create(1555);
		// create.run();
		// }).start();

	}

	void musicServerOff(ActionEvent e) {
		// MusicServer.getInstance().stop();
	}

	public Optional<ServerResponseModel> listFromUrl(final String url) {

		Optional<ServerResponseModel> o = Optional.empty();
		try {
			ServerResponseModel req200 = RequestUtil.req200(new URL(url + "/listFiles"), (is, code) -> {
				ObjectMapper mapper = new ObjectMapper();

				ServerResponseModel readValue = new ServerResponseModel();

				try {
					readValue = mapper.readValue(is, ServerResponseModel.class);
				} catch (Exception e) {
					e.printStackTrace();
				}

				readValue.setUrl(url);
				return readValue;
			}, true);

			o = Optional.of(req200);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return o;
	}

	@FXML
	public void txtGargoyleMusicURLOnKeyPressd(KeyEvent e) {
		if (e.getCode() == KeyCode.ENTER) {
			String url = txtGargoyleMusicURL.getText();
			Optional<ServerResponseModel> v = listFromUrl(url);

			v.ifPresent(m -> {

				String string = m.getHashes().get(0);

				//
				try {
					String source = String.format("%s/%s", m.getUrl(),
							"getFile?title=" + URLEncoder.encode(m.getTitle(), "UTF-8") + "&hash=" + string);

					URL u = new URL(source);
					File file = null;

					ByteArrayOutputStream out = new ByteArrayOutputStream();
					try (InputStream is = u.openStream()) {
						int r = -1;
						while ((r = is.read()) != -1) {
							out.write(r);
						}
					}

					byte[] byteArray = out.toByteArray();
					file = new File("fromServer.mp3");
					try (FileOutputStream fos = new FileOutputStream(file)) {
						fos.write(byteArray);
						fos.flush();
					}

					if (file != null && file.exists()) {
						Mp3Model value = new Mp3Model(new GMp3File(file));
						this.selectMp3Model.set(value);
						preImageLoading(byteArray);
						metadataLoading(value);
					}

					firePlay();

				} catch (Exception e1) {
					e1.printStackTrace();
				}

			});
		}

	}

	@Override
	public boolean canOpen(File file) {
		if (file.isFile()) {

			if ("mp3".equals(FileUtil.getFileExtension(file)))
				return true;

		}

		return false;
	}

	@Override
	public void setOpenFile(File file) {
		addNewMusic(file);
	}

	/**
	 * 구글 드라이브 뷰 
	 * @작성자 : KYJ
	 * @작성일 : 2018. 7. 16. 
	 */
	@FXML
	public void googleDriverOnAction() {
		try {

			TableView<com.google.api.services.drive.model.File> tb = GoogleDriveHelper.createGoogleDriveTableView();
			tb.getItems().setAll(GoogleDriveHelper.listFiles());

			FxUtil.createStageAndShow(tb);	
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
