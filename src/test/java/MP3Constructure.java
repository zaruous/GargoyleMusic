import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;

import com.mpatric.mp3agic.BufferTools;

/********************************
 *	프로젝트 : gargoyle-music
 *	패키지   : 
 *	작성일   : 2017. 6. 5.
 *	프로젝트 : OPERA 
 *	작성자   : KYJ
 *******************************/

/**
 * @author KYJ
 *
 */
public class MP3Constructure {

	/**
	 * @작성자 : KYJ
	 * @작성일 : 2017. 6. 5. 
	 * @param args
	 * @throws URISyntaxException 
	 * @throws IOException 
	 */
	public static void main(String[] args) throws URISyntaxException, IOException {
		URL systemResource = ClassLoader.getSystemResource("028 싸이(PSY) - New Face.mp3");
		File file = new File(systemResource.toURI());
		RandomAccessFile accessFile = new RandomAccessFile(file, "r");

		long bytelength = accessFile.length();
		System.out.println(bytelength);
		long metadata = bytelength - 128L;
		//		byte[] b = new byte[3];
		////		accessFile.read(b);
		accessFile.seek(metadata);

		
		// METADA는 데이터 끝의 128번쨰부터
		byte[] b = new byte[128];
		accessFile.read(b);
		//ID3v1 ID3v1.1
		System.out.println("Tag : " + new String(b, 0, 3, "EUC-KR"));
		System.out.println("Title : " + new String(b, 3, 30, "EUC-KR")); // 0, 30 음악제목
		System.out.println("Artist : " + new String(b, 33, 30, "EUC-KR")); // 33 30 가수 문자열
		System.out.println("Album : " + new String(b, 63, 30, "EUC-KR")); // 63 30음반 문자열
		System.out.println("음반출시년도 : " + new String(b, 93, 4, "EUC-KR"));// 93 4 음반 출시년도 문자열
		System.out.println("비고 : " + new String(b, 97, 30, "EUC-KR"));// 97 30 비고 문자열
		System.out.println("장르 : " + new String(b, 127, 1, "EUC-KR"));// 127 1 장르 바이트
		accessFile.close();

		
		
		
		byte[] bytes = Files.readAllBytes(file.toPath());
		int DATA_LENGTH_OFFSET = 6;
		int headerLength = 10;
		//datalength가 frameLength
		int datalength = BufferTools.unpackSynchsafeInteger(bytes[DATA_LENGTH_OFFSET], bytes[DATA_LENGTH_OFFSET + 1], bytes[DATA_LENGTH_OFFSET + 2], bytes[DATA_LENGTH_OFFSET + 3]);
		System.out.println(datalength);
	}

}
