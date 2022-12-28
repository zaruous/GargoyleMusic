/********************************
 *	프로젝트 : gargoyle-music
 *	패키지   : com.kyj.fx.music
 *	작성일   : 2018. 7. 16.
 *	작성자   : KYJ
 *******************************/
package com.kyj.fx.music;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.kyj.fx.commons.utils.DialogUtil;
import com.kyj.fx.commons.utils.FxUtil;
import com.kyj.google.drive.GoogleDriveStore;
import com.kyj.google.drive.MP3MusicDrive;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TableView.TableViewSelectionModel;

/**
 * 
 * 구글 드라이브에서 음악 관련 파일을 다운로드 받을 수 있게 지원 <br/>
 * 
 * @author KYJ
 *
 */
public class GoogleDriveHelper {

	/**
	 * @작성자 : KYJ
	 * @작성일 : 2018. 7. 16.
	 * @return
	 * @throws IOException
	 */
	public static List<File> listFiles() throws IOException {
		// Build a new authorized API client service.
		Drive service = GoogleDriveStore.getInstance();

		MP3MusicDrive mp3MusicDrive = new MP3MusicDrive(service);
		List<File> listFile = mp3MusicDrive.listFile();
		return listFile;
	}

	/**
	 * @작성자 : KYJ
	 * @작성일 : 2018. 7. 16.
	 * @return
	 */
	public static TableView<File> createGoogleDriveTableView() {

		TableView<File> tb = new TableView<File>();
		TableViewSelectionModel<File> selectionModel = tb.getSelectionModel();
		selectionModel.setCellSelectionEnabled(true);
		selectionModel.setSelectionMode(SelectionMode.MULTIPLE);

		TableColumn<File, String> tcName = new TableColumn<File, String>();
		tcName.setCellValueFactory(v -> {
			return new SimpleStringProperty(v.getValue().getName());
		});

		tb.getColumns().add(tcName);

		installDownloadContextMenu(tb);

		return tb;
	}

	public static void installDownloadContextMenu(TableView<File> tb) {

		ContextMenu cm = new ContextMenu();
		tb.setContextMenu(cm);

		MenuItem miDownload = new MenuItem("Download");
		cm.getItems().add(miDownload);
		miDownload.setOnAction(ev -> {

			if (ev.isConsumed())
				return;

			java.io.File dir = DialogUtil.showDirectoryDialog(e -> {
			});
			if (dir == null || !dir.exists())
				return;

			ObservableList<File> selectedItems = tb.getSelectionModel().getSelectedItems();
			if (selectedItems == null)
				return;

			FxUtil.showLoading(new Task<Void>() {

				@Override
				protected Void call() throws Exception {
					try {

						MP3MusicDrive mp3MusicDrive = new MP3MusicDrive(GoogleDriveStore.getInstance());
						for (File f : selectedItems) {
							String id = f.getId();
							String name = f.getName();

							java.io.File outputFile = new java.io.File(dir, name);
							mp3MusicDrive.download(new FileOutputStream(outputFile), id);
						}

					} catch (Exception e) {
						javafx.application.Platform.runLater(() -> {
							DialogUtil.showExceptionDailog(e);
						});
					}

					return null;
				}
			});

			ev.consume();

		});

	}
}
