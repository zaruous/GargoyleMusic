import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.URL;

import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.springframework.util.DigestUtils;

import com.kyj.fx.commons.functions.GargoyleHostNameVertifier;
import com.kyj.fx.commons.functions.GargoyleSSLVertifier;
import com.kyj.fx.commons.utils.ValueUtil;


/********************************
 *	프로젝트 : gargoyle-music
 *	패키지   : 
 *	작성일   : 2017. 5. 22.
 *	작성자   : KYJ
 *******************************/

/**
 * @author KYJ
 *
 */
public class AlsongLyric {

	public static String getSoapFormat(String checksum) {
		StringBuffer sb = new StringBuffer();
		sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
		sb.append("<SOAP-ENV:Envelope xmlns:SOAP-ENV=\"http://www.w3.org/2003/05/soap-envelope\" \n");
		sb.append("  xmlns:SOAP-ENC=\"http://www.w3.org/2003/05/soap-encoding\" \n");
		sb.append("  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" \n");
		sb.append("  xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" \n");
		sb.append("  xmlns:ns2=\"ALSongWebServer/Service1Soap\" \n");
		sb.append("  xmlns:ns1=\"ALSongWebServer\" \n");
		sb.append("  xmlns:ns3=\"ALSongWebServer/Service1Soap12\">\n");
		sb.append("\n");
		sb.append("<SOAP-ENV:Body>\n");
		sb.append("	<ns1:GetLyric7>\n");
		sb.append(
				"	<ns1:encData>99a512b687fab230e18c3b2ad4ed4cf9de0a522a24d2c9b52314ab943b016cc4334d78618ce7b796f9ee7a2ceb7af55b2b7bcf69bdbd521e4dd3de01c939a934024048956ab53a3f3947f2b4683f4fd7f8397577303d0a805bfd7d78675355c84886ac25c69112928e4c6a9ccbfc8e70e05eec621d2f9d06dd57420a7d9f6d65</ns1:encData>\n");
		sb.append("		<ns1:stQuery>\n");
		sb.append("		 <ns1:strChecksum>"+checksum+"</ns1:strChecksum>\n");
		sb.append("		 <ns1:strVersion>3.36</ns1:strVersion>\n");
		// mac -> dummy
		sb.append("		 <ns1:strMACAddress>12e4568d106216ee198a82a8c578942039c6ea194581fe42d3f34b78b383376c</ns1:strMACAddress>\n");
		// ip -> dummy
		sb.append("		 <ns1:strIPAddress>192.168.0.3</ns1:strIPAddress>\n");
		sb.append("		</ns1:stQuery>\n");
		sb.append("	</ns1:GetLyric7>\n");
		sb.append("</SOAP-ENV:Body>\n");
		sb.append("</SOAP-ENV:Envelope>\n");

		return sb.toString();
	}

	public static void request(String checksum) throws ClientProtocolException, IOException {
		String soapFormat = getSoapFormat(checksum);

		HttpPost httpPost = new HttpPost("http://lyrics.alsong.co.kr/alsongwebservice/service1.asmx");
		httpPost.addHeader("contentType", "application/soap+xml; charset=utf-8");
		httpPost.addHeader("User-Agent", "gSOAP/2.7");
		httpPost.addHeader("Connection", "close");
		 httpPost.addHeader("SOAPAction", "ALSongWebServer/GetLyric7");
//		httpPost.addHeader("SOAPAction", "ALSongWebServer/GetMurekaInfo");
		httpPost.addHeader("Host", "lyrics.alsong.co.kr");
		// httpPost.addHeader("SO", "lyrics.alsong.co.kr");

		StringEntity entity = new StringEntity(soapFormat, "UTF-8");
		entity.setContentType("text/xml");
		httpPost.setEntity(entity);

		HttpClientBuilder create = HttpClientBuilder.create();
		CloseableHttpClient build = create.build();

		CloseableHttpResponse execute = build.execute(httpPost);
		HttpEntity response = execute.getEntity();

		InputStream content = response.getContent();
		System.out.println(ValueUtil.toString(content));
		EntityUtils.consume(response);

		build.close();

	}

	/**
	 * @작성자 : KYJ
	 * @작성일 : 2017. 5. 22.
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		
		new GargoyleSSLVertifier().setup();
		new GargoyleHostNameVertifier().setup();
//		String file = "C:\\Users\\KYJ\\Music\\뜸부기\\다모뮤직(damo.kr) 멜론(Melon) 5월 1일 실시간 Top100\\010 지코 (ZICO) - She`s a Baby.mp3";
		URL systemResource = ClassLoader.getSystemResource("008 아이유 - 이런 엔딩.mp3");
		File file = new File(systemResource.toURI());
		RandomAccessFile accessFile = new RandomAccessFile(file, "r");

		byte[] b = new byte[3];
		if (accessFile.read(b) != -1) {
			String x = new String(b);
			System.out.println(x);

			if ("ID3".equals(x)) {

				// ID3v2길이구하기
				accessFile.seek(accessFile.getFilePointer() + 3);
				b = new byte[4];
				accessFile.read(b);
				System.out.println(new String(b));
				int size = b[0] << 21 | b[1] << 14 | b[2] << 7 | b[3];
				System.out.println(size);
				accessFile.seek(size + 10);
			}

		}

		// 공백있는 ID3 태그 에 대한 처리
		for (int i = 0; i < 50000; i++) {
			int a = accessFile.readUnsignedByte();
			if (a == 255) {
				a = accessFile.readUnsignedByte();
				if ((a >> 5) == 7) {
					accessFile.seek(accessFile.getFilePointer() - 2);
					break;
				}
			}
		}
		b = new byte[163840];
		accessFile.read(b);
		byte[] b2 = DigestUtils.md5Digest(b);

		StringBuffer sb = new StringBuffer();

		for (int i = 0; i < b2.length; i++) {
			sb.append(Integer.toString((b2[i] & 0xff) + 0x100, 16).substring(1));
		}

		// System.out.println(sb.toString());
		String md5hash = DigestUtils.md5DigestAsHex(b);
		System.out.println(md5hash);
		// System.out.println("22dba79268ba929948c5dcda9d87d4bc");
		accessFile.close();
		//

		request(md5hash);
	}

}
