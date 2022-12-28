import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Enumeration;
import java.util.List;
import java.util.function.BiFunction;

import org.junit.Before;
import org.junit.Test;
import org.springframework.util.DigestUtils;

import com.kyj.fx.commons.functions.GargoyleHostNameVertifier;
import com.kyj.fx.commons.functions.GargoyleSSLVertifier;
import com.kyj.fx.commons.utils.ValueUtil;
import com.kyj.fx.music.GMp3File;
import com.kyj.fx.music.LyricDVO;
import com.kyj.fx.music.LyricMnager;
import com.kyj.fx.music.Mp3Model;
//import com.kyj.fx.voeditor.visual.main.initalize.HostNameVertifierInitializer;
//import com.kyj.fx.voeditor.visual.main.initalize.SSLInitializable;
import com.mpatric.mp3agic.InvalidDataException;
import com.mpatric.mp3agic.UnsupportedTagException;

public class AlsongLyricTest {

	@Before
	public void init() throws Exception {
		// System.setProperty(ResourceLoader.HTTP_PROXY_HOST, "127.0.0.1");
		// System.setProperty(ResourceLoader.HTTP_PROXY_PORT, "8888");
		// System.setProperty(ResourceLoader.HTTPS_PROXY_HOST, "127.0.0.1");
		// System.setProperty(ResourceLoader.HTTPS_PROXY_PORT, "8888");

		new GargoyleSSLVertifier().setup();
		new GargoyleHostNameVertifier().setup();
		// new SSLInitializable().initialize();
		// new HostNameVertifierInitializer().initialize();

	}

	@Test
	public void fileInRyric() throws URISyntaxException, UnsupportedTagException, InvalidDataException, IOException {
		URL systemResource = ClassLoader.getSystemResource("028 싸이(PSY) - New Face.mp3");
		File file = new File(systemResource.toURI());

		Mp3Model mp3Model = new Mp3Model(new GMp3File(file));
		System.out.println(mp3Model.getRyric());
	}

	/**
	 * hash값으로부터 값을 가져온다.
	 */
	@Test
	public void test() {

		String hash = "9eafbe91873585d66d51b37f0f9dcc90";
		BiFunction<String, InputStream, String> func = new BiFunction<String, InputStream, String>() {

			@Override
			public String apply(String t, InputStream u) {
				return ValueUtil.toString(u);
			}
		};

		String request2 = LyricMnager.getInstance().request(hash, func);
		System.out.println(request2);

		List<LyricDVO> request = LyricMnager.getInstance().request(hash);
		System.out.println(request.size());
		request.forEach(System.out::println);

	}

	@Test
	public void mac() {
		String target = "ce94568d106216ee198a82a8c578942039c6ea194581fe42d3f34b78b383376c";

		getMac();
	}

	public static void getMac() {
		try {
			Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
			while (networkInterfaces.hasMoreElements()) {
				NetworkInterface nextElement = networkInterfaces.nextElement();
				byte[] hardwareAddress = nextElement.getHardwareAddress();
				if (hardwareAddress == null)
					continue;
				System.out.println(new String(hardwareAddress));
				String md5DigestAsHex = DigestUtils.md5DigestAsHex(hardwareAddress);
				System.out.println(md5DigestAsHex);
			}
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
}
