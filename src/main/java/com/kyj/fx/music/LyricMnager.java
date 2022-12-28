/********************************
 *	프로젝트 : gargoyle-music
 *	패키지   : com.kyj.fx.music
 *	작성일   : 2017. 5. 24.
 *	작성자   : KYJ
 *******************************/
package com.kyj.fx.music;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.DigestUtils;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import com.kyj.fx.commons.utils.SAXPasrerUtil;
import com.kyj.fx.commons.utils.SAXPasrerUtil.Handler;
import com.kyj.fx.commons.utils.SAXPasrerUtil.SAXHandler;
import com.kyj.fx.commons.utils.ValueUtil;

/**
 * @author KYJ
 *
 */
public class LyricMnager {

	private static LyricMnager INSTANCE;
	private static final Logger LOGGER = LoggerFactory.getLogger(LyricMnager.class);

	public synchronized static LyricMnager getInstance() {
		if (INSTANCE == null) {
			INSTANCE = new LyricMnager();
		}

		return INSTANCE;
	}

	public List<LyricDVO> getRyric(File mp3) {
		return getRyric(getHash(mp3));

	}

	public String getHash(File mp3) {
		String hash = null;
		try (RandomAccessFile accessFile = new RandomAccessFile(mp3, "r")) {

			byte[] b = new byte[3];
			if (accessFile.read(b) != -1) {
				String x = new String(b);

				if ("ID3".equals(x)) {

					// ID3v2길이구하기
					accessFile.seek(accessFile.getFilePointer() + 3);
					b = new byte[4];
					accessFile.read(b);
					LOGGER.debug(new String(b));
					int size = b[0] << 21 | b[1] << 14 | b[2] << 7 | b[3];
					LOGGER.debug("{}", size);
					accessFile.seek(size + 10);
				} else {
					LOGGER.debug("song has't id3");
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
			hash = DigestUtils.md5DigestAsHex(b);
			LOGGER.debug("song hash : {}", hash);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return hash;
	}

	public List<LyricDVO> getRyric(String md5hash) {
		return request(md5hash);
	}

	/**
	 * test code.
	 * 
	 * @작성자 : KYJ
	 * @작성일 : 2017. 5. 26.
	 * @param is
	 * @return
	 * @throws Exception
	 */
	@Deprecated
	private List<String> parse(InputStream is) throws Exception {

		List<String> lyrics = SAXPasrerUtil.getAllQNames(is, new SAXHandler() {

			private String qName;
			private String strArtist;
			private String strAlbum;
			private StringBuffer strLyric = new StringBuffer();;

			private boolean startLyric;
			private boolean endLyric;

			@Override
			public void startElement(String url, String arg1, String qName, Attributes arg3) throws SAXException {
				super.startElement(url, arg1, qName, arg3);
				this.qName = qName;
				if ("strLyric".equals(this.qName)) {
					startLyric = true;
				}
			}

			@Override
			public void characters(char[] ch, int start, int len) throws SAXException {
				// super.characters(ch, start, length);
				if (ch.length != 0) {

					switch (qName) {
					case "strAlbum":
						strAlbum = new String(ch, start, len).trim();
						LOGGER.debug(strAlbum);
						break;
					case "strArtist":
						strArtist = new String(ch, start, len).trim();
						LOGGER.debug(strArtist);
						break;
					case "strLyric":

						if (startLyric && !endLyric) {

							// int timeStart = -1;
							// int timeEnd = -1;
							// int lyricStart = -1;
							// for (int i = start; i < len; i++) {
							// char c = ch[i];
							// if (c == '[') {
							// timeStart = i;
							// } else if (c == ']') {
							// timeEnd = i;
							// }
							//
							// if (timeStart != -1 && timeEnd != -1) {
							// lyricStart = i;
							// }
							// }

							// LyricDVO lyricDVO = new LyricDVO();
							// lyricDVO.setTime(new String(ch, timeStart, timeEnd).trim());
							LOGGER.debug(new String(ch, start, len).trim());
							strLyric.append(new String(ch, start, len).trim());
						}

						break;
					}

				}

			}

			@Override
			public void endElement(String uri, String localName, String qName) throws SAXException {
				super.endElement(uri, localName, qName);

				if ("strLyric".equals(this.qName)) {
					endLyric = true;
				}
			}

			@Override
			public List<String> getList() {
				String[] split = this.strLyric.toString().split("<br>");
				return Arrays.asList(split);
			}

		});

		return lyrics;
	}

	public List<LyricDVO> request(String checksum) {
		return request(checksum, this::toLyricDVO);
	}

	public <T> T request(String checksum, BiFunction<String, InputStream, T> convert) {
		CloseableHttpClient build = null;
		T result = null;

		try {
			String soapFormat = getSoapFormat(checksum);

			HttpPost httpPost = new HttpPost("http://lyrics.alsong.co.kr/alsongwebservice/service1.asmx");
			httpPost.addHeader("contentType", "application/soap+xml; charset=utf-8");
			httpPost.addHeader("User-Agent", "gSOAP/2.7");
			httpPost.addHeader("Connection", "close");
			httpPost.addHeader("SOAPAction", "ALSongWebServer/GetLyric7");
			// httpPost.addHeader("SOAPAction", "ALSongWebServer/GetMurekaInfo");
			httpPost.addHeader("Host", "lyrics.alsong.co.kr");
			// httpPost.addHeader("SO", "lyrics.alsong.co.kr");

			StringEntity entity = new StringEntity(soapFormat, "UTF-8");
			entity.setContentType("text/xml");
			httpPost.setEntity(entity);

			HttpClientBuilder create = HttpClientBuilder.create()
					
					/*.setHostnameVerifier(new X509HostnameVerifier() {

				@Override
				public boolean verify(String arg0, SSLSession arg1) {
					// TODO Auto-generated method stub
					return false;
				}

				@Override
				public void verify(String host, String[] cns, String[] subjectAlts) throws SSLException {
					// TODO Auto-generated method stub

				}

				@Override
				public void verify(String host, X509Certificate cert) throws SSLException {
					// TODO Auto-generated method stub

				}

				@Override
				public void verify(String host, SSLSocket ssl) throws IOException {
					// TODO Auto-generated method stub

				}
			}) 
			*/
			/* .setProxy(new HttpHost("127.0.0.1", 8888))*/;
			build = create.build();

			CloseableHttpResponse execute = build.execute(httpPost);
			HttpEntity response = execute.getEntity();

			InputStream is = response.getContent();

			// wrtie in memory.
			is = toBuffer(is);
			// BufferedInputStream buf = new BufferedInputStream(is);

			// result = parse(is);
			result = convert.apply(checksum, is);
			// result = ValueUtil.toString(content);
			EntityUtils.consume(response);

		} catch (Exception e) {
			LOGGER.error(ValueUtil.toString(e));
		} finally {
			try {
				build.close();
			} catch (IOException e) {
				LOGGER.error(ValueUtil.toString(e));
			}
		}
		return result;

	}

	/**
	 * 버퍼에 저장한 stream으로 변환
	 * 
	 * @작성자 : KYJ
	 * @작성일 : 2017. 9. 3.
	 * @param is
	 * @return
	 * @throws IOException
	 */
	private InputStream toBuffer(InputStream is) throws IOException {

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		int read = -1;
		while ((read = is.read()) != -1) {
			out.write(read);
		}

		return new ByteArrayInputStream(out.toByteArray());
	}

	private static String getLocalhost() {
		try {
			return InetAddress.getLocalHost().getHostAddress();
		} catch (UnknownHostException e) {
			LOGGER.error(ValueUtil.toString(e));
		}
		return "";
	}

	public String getSoapFormat(String checksum) {
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
				"	<ns1:encData>c5fed6401b87979f0a10b3c054434ed6daedf60f5869ccc33522f714e2bf5a54400d0d2d8d470e78e17975590f598d45e9ac943391e525f9cbd73a3b8fa14710900655684b4d0b1a75e55c6a3db77af7142f6e580b46e972e918f171b71b43055117eb5bd5e3a0027f95719646b6e28439702ff58ae6c9aeec5e7f27a8e2f35b</ns1:encData>\n");
		sb.append("		<ns1:stQuery>\n");
		sb.append("		 <ns1:strChecksum>" + checksum + "</ns1:strChecksum>\n");
		sb.append("		 <ns1:strVersion>3.4</ns1:strVersion>\n");
		// mac -> dummy
		sb.append(
				"		 <ns1:strMACAddress></ns1:strMACAddress>\n");
		// ip -> dummy
		sb.append("		 <ns1:strIPAddress>" + getLocalhost() + "</ns1:strIPAddress>\n");
		sb.append("		</ns1:stQuery>\n");
		sb.append("	</ns1:GetLyric7>\n");
		sb.append("</SOAP-ENV:Body>\n");
		sb.append("</SOAP-ENV:Envelope>\n");

		return sb.toString();
	}

	public String getSoapMurekaInfoFormat(String checksum) {
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
		// sb.append(" <ns1:GetLyric7>\n");
		// sb.append(
		// "
		// <ns1:encData>99a512b687fab230e18c3b2ad4ed4cf9de0a522a24d2c9b52314ab943b016cc4334d78618ce7b796f9ee7a2ceb7af55b2b7bcf69bdbd521e4dd3de01c939a934024048956ab53a3f3947f2b4683f4fd7f8397577303d0a805bfd7d78675355c84886ac25c69112928e4c6a9ccbfc8e70e05eec621d2f9d06dd57420a7d9f6d65</ns1:encData>\n");
		// sb.append(" <ns1:stQuery>\n");
		// sb.append(" <ns1:strChecksum>" + checksum + "</ns1:strChecksum>\n");
		// sb.append(" <ns1:strVersion>3.36</ns1:strVersion>\n");
		// mac -> dummy
		// sb.append("
		// <ns1:strMACAddress>ce94568d106216ee198a82a8c578942039c6ea194581fe42d3f34b78b383376c</ns1:strMACAddress>\n");
		// ip -> dummy
		// sb.append(" <ns1:strIPAddress>" + getLocalhost() + "</ns1:strIPAddress>\n");
		sb.append(
				"	<ns1:GetMurekaInfo><ns1:encData>8ea3d2513a18d3e838ca89b310d2d4a67e6602c47357d1fd7d2fb388a3830250f02e31f824985ca6f2a075519e8ca527ebb4d3ee3d0f817d7015de4409d719cbbc8639e92c1c7777f3e07020fc51594c86e371f3d91a661be68f4c0e793efcd7edd37efe530000073bbbb5bb3d392fd877a630a710e2ddfa569947d0cdce3cf7</ns1:encData><ns1:md5>"
						+ checksum + "</ns1:md5></ns1:GetMurekaInfo>\n");

		// sb.append(" </ns1:stQuery>\n");
		// sb.append(" </ns1:GetLyric7>\n");
		sb.append("</SOAP-ENV:Body>\n");
		sb.append("</SOAP-ENV:Envelope>\n");

		return sb.toString();
	}

	private List<LyricDVO> toLyricDVO(String checksum, InputStream is) {

		String lyric = CrudService.getInstance().getLyric(checksum);
		if (ValueUtil.isNotEmpty(lyric)) {
			LOGGER.debug("loaded from  local database. ");
			return parse(lyric);
		}

		List<LyricDVO> allQNames = Collections.emptyList();
		try {
			allQNames = SAXPasrerUtil.getAll(is, new Handler<LyricDVO>() {

				private String qName;
				private String strArtist;
				private String strAlbum;
				private StringBuffer strLyric = new StringBuffer();;

				private boolean startLyric;
				private boolean endLyric;

				@Override
				public void startElement(String url, String arg1, String qName, Attributes arg3) throws SAXException {
					// super.startElement(url, arg1, qName, arg3);
					this.qName = qName;
					if ("strLyric".equals(this.qName)) {
						startLyric = true;
					}
				}

				@Override
				public void characters(char[] ch, int start, int len) throws SAXException {
					// super.characters(ch, start, length);
					if (ch.length != 0) {

						switch (qName) {
						case "strAlbum":
							strAlbum = new String(ch, start, len).trim();
							break;
						case "strArtist":
							strArtist = new String(ch, start, len).trim();
							break;
						case "strLyric":

							if (startLyric && !endLyric) {
								strLyric.append(new String(ch, start, len));
							}

							break;
						}

					}

				}

				@Override
				public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
					// TODO Auto-generated method stub
					super.ignorableWhitespace(ch, start, length);
				}

				@Override
				public void endElement(String uri, String localName, String qName) throws SAXException {
					super.endElement(uri, localName, qName);

					if ("strLyric".equals(this.qName)) {
						endLyric = true;
					}
				}

				@Override
				public List<LyricDVO> getList() {

					// LyricHeaderDVO lyricHeaderDVO = new LyricHeaderDVO(strAlbum, strArtist);

					String string = this.strLyric.toString();
					CrudService.getInstance().registLyric(checksum, string);

					/*
					 * String[] split = this.strLyric.toString().split("<br>");
					 * 
					 * List<LyricDVO> arrayList = new ArrayList<LyricDVO>(split.length);
					 * 
					 * Pattern compile = Pattern.compile("\\d\\d:\\d\\d.\\d\\d");
					 * 
					 * for (String str : split) { LOGGER.debug(str); Matcher matcher =
					 * compile.matcher(str); if (matcher.find()) { String timeInfo =
					 * matcher.group();
					 * 
					 * int idx1 = timeInfo.indexOf(':'); int min =
					 * Integer.parseInt(timeInfo.substring(0, idx1)); int idx2 =
					 * timeInfo.indexOf('.', idx1); int sec =
					 * Integer.parseInt(timeInfo.substring(idx1 + 1, idx2)); int mills =
					 * Integer.parseInt(timeInfo.substring(idx2 + 1));
					 * 
					 * int end = matcher.end(); end++; String lyric = str.substring(end);
					 * 
					 * LyricDVO lyricDVO = new LyricDVO(lyricHeaderDVO); lyricDVO.setTime(timeInfo);
					 * lyricDVO.setLyric(lyric); lyricDVO.setMin(min); lyricDVO.setSec(sec);
					 * lyricDVO.setMills(mills); arrayList.add(lyricDVO); }
					 * 
					 * }
					 */

					return parse(string);
				}

			});
		} catch (Exception e) {
			e.printStackTrace();
		}

		return allQNames;
	}

	private List<LyricDVO> parse(String input) {
		String[] split = input.split("<br>");

		List<LyricDVO> arrayList = new ArrayList<LyricDVO>(split.length);

		Pattern compile = Pattern.compile("\\d\\d:\\d\\d.\\d\\d");

		for (String str : split) {
			LOGGER.debug(str);
			Matcher matcher = compile.matcher(str);
			if (matcher.find()) {
				String timeInfo = matcher.group();

				int idx1 = timeInfo.indexOf(':');
				int min = Integer.parseInt(timeInfo.substring(0, idx1));
				int idx2 = timeInfo.indexOf('.', idx1);
				int sec = Integer.parseInt(timeInfo.substring(idx1 + 1, idx2));
				int mills = Integer.parseInt(timeInfo.substring(idx2 + 1));

				int end = matcher.end();
				end++;
				String lyric = str.substring(end);

				LyricDVO lyricDVO = new LyricDVO();
				lyricDVO.setTime(timeInfo);
				lyricDVO.setLyric(lyric);
				lyricDVO.setMin(min);
				lyricDVO.setSec(sec);
				lyricDVO.setMills(mills);
				arrayList.add(lyricDVO);
			}

		}
		return arrayList;
	}

	static class AlsongStringHandler extends Handler<String> {

		private String qName;
		private String strArtist;
		private String strAlbum;
		private StringBuffer strLyric = new StringBuffer();;

		private boolean startLyric;
		private boolean endLyric;

		@Override
		public void startElement(String url, String arg1, String qName, Attributes arg3) throws SAXException {
			// super.startElement(url, arg1, qName, arg3);
			this.qName = qName;
			if ("strLyric".equals(this.qName)) {
				startLyric = true;
			}
		}

		@Override
		public void characters(char[] ch, int start, int len) throws SAXException {
			// super.characters(ch, start, length);
			if (ch.length != 0) {

				switch (qName) {
				case "strAlbum":
					strAlbum = new String(ch, start, len).trim();

					LOGGER.debug(strAlbum);
					break;
				case "strArtist":
					strArtist = new String(ch, start, len).trim();
					LOGGER.debug(strArtist);
					break;
				case "strLyric":

					if (startLyric && !endLyric) {
						strLyric.append(new String(ch, start, len));
					}

					break;
				}

			}

		}

		@Override
		public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
			super.ignorableWhitespace(ch, start, length);
		}

		@Override
		public void endElement(String uri, String localName, String qName) throws SAXException {
			super.endElement(uri, localName, qName);

			if ("strLyric".equals(this.qName)) {
				endLyric = true;
			}
		}

		public String toString() {
			return strLyric.toString();
		}

	}

	public String toString(InputStream is) {

		AlsongStringHandler handler = new AlsongStringHandler();
		try {
			SAXPasrerUtil.getAll(is, handler);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return handler.toString();
	}

}
