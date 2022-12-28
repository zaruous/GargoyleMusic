package com.kyj.fx.music;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kyj.fx.commons.functions.GargoyleHostNameVertifier;
import com.kyj.fx.commons.functions.GargoyleSystemPropertyVertifer;
import com.kyj.fx.commons.memory.StageStore;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {
	private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);
	private static Stage stage;
	private MusicPlayerComposite2 root;

	@Override
	public void start(Stage primaryStage) {
		stage = primaryStage;
		StageStore.setPrimaryStage(primaryStage);

		try {

			stage.setTitle(" Gargoyle Music Room ");

			root = new MusicPlayerComposite2();
			Scene scene = new Scene(root);
			// scene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());
			primaryStage.setScene(scene);
			primaryStage.show();

			primaryStage.setOnCloseRequest(ev -> {
				try {
					System.out.println("close request");
					root.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			});
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) throws Exception {

		new GargoyleHostNameVertifier().setup();
		new GargoyleSystemPropertyVertifer().setup();
		
		// new AppDuplDepenceInitializer(54546) {
		//
		// @Override
		// public void handle(Exception t) {
		// LOGGER.error(ValueUtil.toString(t));
		// Platform.exit();
		// System.exit(-1);
		// }
		// }.initialize();

		launch(args);

		// LOGGER.debug.println(new Date().getTime());
		// test();
	}

}
