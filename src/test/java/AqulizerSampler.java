import java.io.File;

import com.mpatric.mp3agic.Mp3File;

import javafx.application.Application;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.control.Slider;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.media.AudioEqualizer;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.stage.Stage;

public class AqulizerSampler extends Application {

	public static void main(String[] args) {
		launch(args);
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
//		String file = "F:\\\음악\\[멜론] 5월28일 TOP100 어반자카파,솔지,XIA,에디킴,CLC,이주한,얀키\\[멜론] 5월28일 실시간 TOP 100\\020 백아연 - 이럴거면 그러지말지 (Feat. 영현).mp3";
		String file = "F:\\음악\\멜론 2017 5월1일\\다모뮤직(damo.kr) 멜론(Melon) 5월 1일 실시간 Top100\\010 지코 (ZICO) - She`s a Baby.mp3";
		
		Media newMedia = new Media(new File(file).toURI().toURL().toString());
		MediaPlayer mediaPlayer = new MediaPlayer(newMedia);

		BorderPane root = new BorderPane();

		HBox hBox = new HBox(10);
		hBox.setPrefSize(Double.MAX_VALUE, Double.MAX_VALUE);
		AudioEqualizer audioEqualizer = mediaPlayer.getAudioEqualizer();
		audioEqualizer.getBands().forEach(band -> {

			Slider slider = new Slider();
			slider.setOrientation(Orientation.VERTICAL);
			// slider.valueProperty().bind(band.bandwidthProperty());
			slider.setValue(band.getBandwidth());

			System.out.println(band.toString());
			slider.valueProperty().addListener((oba, o, n) -> {
				band.setBandwidth(n.doubleValue());
			});

			hBox.getChildren().add(slider);
		});

		root.setCenter(hBox);
		primaryStage.setScene(new Scene(root));
		primaryStage.show();

		mediaPlayer.play();

	}

}
