package com.kyj.fx.music;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class PlayListDVO {

	private StringProperty title;

	private ObservableList<PlayListDetailDVO> list;

	public PlayListDVO() {

		this.title = new SimpleStringProperty();
		this.list = FXCollections.observableArrayList();

	}

	public final StringProperty titleProperty() {
		return this.title;
	}

	public final String getTitle() {
		return this.titleProperty().get();
	}

	public final void setTitle(final String title) {
		this.titleProperty().set(title);
	}

	public ObservableList<PlayListDetailDVO> getList() {
		return list;
	}

	public void setList(ObservableList<PlayListDetailDVO> list) {
		this.list = list;
	}

	public void addItem(PlayListDetailDVO dvo) {
		this.list.add(dvo);
	}

}
