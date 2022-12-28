import java.net.URLEncoder;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.stage.Stage;

/********************************
 *	프로젝트 : gargoyle-music
 *	패키지   : 
 *	작성일   : 2017. 6. 29.
 *	프로젝트 : OPERA 
 *	작성자   : KYJ
 *******************************/

/**
 * @author KYJ
 *
 */
public class WebMediaTest extends Application {

	/**
	 * @작성자 : KYJ
	 * @작성일 : 2017. 6. 29. 
	 * @param args
	 */
	public static void main(String[] args) {
		launch(args);

	}

	@Override
	public void start(Stage primaryStage) throws Exception {

		String title = URLEncoder.encode("아이유", "UTF-8");
		MediaPlayer mediaPlayer = new MediaPlayer(
				new Media("http://localhost:1555/getFile?title=" + title + "&hash=23b43adc206b4cfb07e6b9b84e49ada6"));
		mediaPlayer.play();
		primaryStage.setScene(new Scene(new BorderPane(), 1200, 600));
		primaryStage.show();
	}

}
