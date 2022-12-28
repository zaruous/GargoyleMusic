/********************************
 *	프로젝트 : gargoyle-music
 *	패키지   : com.kyj.fx.music
 *	작성일   : 2017. 6. 7.
 *	작성자   : KYJ
 *******************************/
package com.kyj.fx.music;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kyj.fx.commons.memory.ResourceLoader;
import com.kyj.fx.commons.memory.ResourceLoaderDbProperties;
import com.kyj.fx.commons.utils.DbUtil;
import com.kyj.fx.commons.utils.ValueUtil;

import javafx.collections.ObservableList;

/**
 * @author KYJ
 *
 */
public class CrudService {
	private static final Logger LOGGER = LoggerFactory.getLogger(CrudService.class);
	private static final String DRIVER = ResourceLoader.ORG_SQLITE_JDBC;
	private static final String URL = ResourceLoaderDbProperties.GARGOYLE_DB_URL;
	private Supplier<Connection> supplier;

	private static CrudService INSTANCE;

	public static synchronized CrudService getInstance() {
		if (INSTANCE == null)
			INSTANCE = new CrudService();
		return INSTANCE;
	}

	private CrudService() {

		supplier = new Supplier<Connection>() {

			@Override
			public Connection get() {
				try {
					Connection connection = DbUtil.getConnection(DRIVER, URL, new Properties());
					connection.setAutoCommit(true);
					return connection;
				} catch (Exception e) {
					e.printStackTrace();
				}

				return null;
			}
		};

		createNewTable();

	}

	private void createNewTable() {
		try (Connection con = supplier.get()) {

			DbUtil.update(con, "create table if not exists tbm_ms_myplaylist ( title text   )");

			DbUtil.update(con, "create table if not exists tbp_ms_myplaylist (  title text ,  location text , hash text ) ");

			DbUtil.update(con, "create table if not exists tbm_ms_myplaylist_env ( key text, value text,  primary key(key)  )");

			DbUtil.update(con, "create table if not exists tbm_ms_myplaylist_lyric ( hash text  , lyric text,  primary key(hash)  )");

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public void saveLastMusicList(String playListName, File music) {

	}

	public void loadLastMusicList(BiConsumer<String, File> handler) {
		if (handler == null)
			return;

		try (Connection con = supplier.get()) {

			String musicListName = null;
			File musicFileName = null;
			Map<String, Object> findOne = DbUtil.findOne(con,
					String.format("select value from tbm_ms_myplaylist_env where key =  'last.music.list.name' "));

			if (findOne.isEmpty())
				musicListName = findOne.get("value").toString();

			findOne = DbUtil.findOne(con, String.format("select value from tbm_ms_myplaylist_env where key =  'last.music.filename' "));

			if (findOne.isEmpty())
				musicFileName = new File(findOne.get("value").toString());

			handler.accept(musicListName, musicFileName);
		} catch (Exception e) {
			LOGGER.error(ValueUtil.toString(e));
		}

	}

	public double getVolumn() {
		double val = 0.3D;
		try (Connection con = supplier.get()) {
			Map<String, Object> findOne = DbUtil.findOne(con,
					String.format("select value from tbm_ms_myplaylist_env where key =  'volumn' "));
			if (findOne.isEmpty())
				return val;

			try {
				val = Double.parseDouble(findOne.get("value").toString());
			} catch (NumberFormatException e) {
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return val;
	}

	public void saveVolumn(double volumn) {
		try (Connection con = supplier.get()) {
			DbUtil.update(con, String.format("insert or replace into tbm_ms_myplaylist_env values  ( 'volumn', '" + volumn + "'   )"));

			LOGGER.debug("save volumn value : {} ", volumn);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public boolean existsPlayList(String title) {
		try (Connection con = supplier.get()) {
			Map<String, Object> findOne = DbUtil.findOne(con, String.format("select 1 from tbm_ms_myplaylist where title =  '%s' ", title));
			if (findOne.isEmpty())
				return false;
			return true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		throw new RuntimeException("can't check.");
	}

	public int addNewPlayList(String title) {
		int result = -1;
		try (Connection con = supplier.get()) {
			result = DbUtil.update(con, "insert into tbm_ms_myplaylist values ('" + title + "') ");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}

	public int addNewPlayList(String title, File location) {
		String hash = LyricMnager.getInstance().getHash(location);
		return addNewPlayList(title, hash, location);
	}

	public int addNewPlayList(String title, String hash, File location) {
		int result = -1;
		try (Connection con = supplier.get()) {

			result = DbUtil.update(con,
					"insert into tbp_ms_myplaylist(title, location, hash ) values ('" + title + "' , '" + location.getAbsolutePath() + "' , '" + hash + "') ");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}

	public void addNewPlayList(String title, List<File> items) {
		Connection con = supplier.get();
		try {

			con.setAutoCommit(false);
			PreparedStatement prepareStatement = con.prepareStatement("insert into tbp_ms_myplaylist(title, location, hash) values (?, ?, ?) ");

			for (File f : items) {
				prepareStatement.setString(1, title);
				prepareStatement.setString(2, f.getAbsolutePath());
				prepareStatement.setString(3, LyricMnager.getInstance().getHash(f));
				prepareStatement.executeUpdate();
			}

			con.commit();
		} catch (Exception e) {
			e.printStackTrace();
			try {
				con.rollback();
			} catch (SQLException e1) {
				e1.printStackTrace();
			}
		} finally {
			if (con != null)
				DbUtil.close(con);
		}

	}

	public List<PlayListDVO> loadPlayList() {
		List<PlayListDVO> select = Collections.emptyList();
		try (Connection con = supplier.get()) {

			select = DbUtil.selectBeans(con, "select * from tbm_ms_myplaylist", PlayListDVO.class);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return select;
	}

	/**
	 * 컬럼이 변경되면서 테이블에 hash값을 반영하는 작업을함.
	 * 
	 * @작성자 : KYJ
	 * @작성일 : 2017. 6. 30.
	 * @param title
	 * @return
	 */
	public List<PlayListDetailDVO> renewHash(String title) {
		List<PlayListDetailDVO> select = Collections.emptyList();
		try (Connection con = supplier.get()) {

			HashMap<String, Object> paramMap = new HashMap<>();
			paramMap.put("title", title);

			String sql = ValueUtil.getVelocityToText("select * from tbp_ms_myplaylist where title = :title and hash is null ", paramMap,
					true);
			// String sql = String.format("select * from tbp_ms_myplaylist where title = :title hash is null ", title);
			select = DbUtil.selectBeans(con, sql, PlayListDetailDVO.class);

			try (Connection conc = supplier.get()) {

				for (PlayListDetailDVO v : select) {
					String location = v.getLocation();
					if (ValueUtil.isEmpty(location))
						continue;
					File f = new File(location);
					if (!f.exists())
						continue;

					try {
						HashMap<String, Object> p = new HashMap<String, Object>();
						p.put("hash", LyricMnager.getInstance().getHash(f));
						p.put("title", v.getTitle());
						p.put("location", location);
						String upd = ValueUtil.getVelocityToText(
								"update tbp_ms_myplaylist set hash = :hash where title = :title and location = :location ", p, true);
						DbUtil.update(conc, upd);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return select;
	}

	public List<PlayListDetailDVO> loadPlayList(String title) {
		renewHash(title);
		List<PlayListDetailDVO> select = Collections.emptyList();
		try (Connection con = supplier.get()) {

			String sql = String.format("select * from tbp_ms_myplaylist where title = '%s'", title);
			select = DbUtil.selectBeans(con, sql, PlayListDetailDVO.class);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return select;

	}

	public PlayListDetailDVO loadPlay(String title, String hash) {
		PlayListDetailDVO d = null;
		try (Connection con = supplier.get()) {

			HashMap<String, Object> p = new HashMap<String, Object>();
			p.put("title", title);
			p.put("hash", hash);

			String sql = ValueUtil.getVelocityToText("select * from tbp_ms_myplaylist where title = :title and hash = :hash ", p, true);

			d = DbUtil.findOne(con, sql, PlayListDetailDVO.class);

		} catch (Exception e) {
			e.printStackTrace();
		}
		return d;
	}

	public void removePlayList(String title, ObservableList<File> items) {
		Connection con = supplier.get();
		try {

			con.setAutoCommit(false);
			PreparedStatement prepareStatement = con.prepareStatement("delete from tbp_ms_myplaylist where title = ? and location = ? ");

//			int count = 0;
			for (File f : items) {
				prepareStatement.setString(1, title);
				prepareStatement.setString(2, f.getAbsolutePath());
				prepareStatement.addBatch();
//				int executeUpdate = prepareStatement.executeUpdate();
//				if (executeUpdate == 1)
//					count++;
			}
			prepareStatement.executeBatch();
			con.commit();
		} catch (Exception e) {
			e.printStackTrace();
			try {
				con.rollback();
			} catch (SQLException e1) {
				e1.printStackTrace();
			}
		} finally {
			if (con != null)
				DbUtil.close(con);
		}
	}

	public int registLyric(String hash, String song) {

		int result = -1;
		if (ValueUtil.isEmpty(song))
			return result;

		try (Connection con = supplier.get()) {

			String _song = Base64.getEncoder().encodeToString(song.getBytes());
			result = DbUtil.update(con, "insert or replace into tbm_ms_myplaylist_lyric values ('" + hash + "', '" + _song + "') ");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}

	public String getLyric(String hash) {
		try (Connection con = supplier.get()) {

			Map<String, Object> findOne = DbUtil.findOne(con, "select lyric from tbm_ms_myplaylist_lyric where hash = '" + hash + "' ");
			if (findOne.isEmpty())
				return null;

			String string = findOne.get("lyric").toString();
			String _song = new String(Base64.getDecoder().decode(string.getBytes()));
			return _song;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

}
