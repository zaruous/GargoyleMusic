package com.kyj.fx.music;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

// @JsonIgnoreProperties 특정필드 제외할 목적으로 사용
@JsonIgnoreProperties({ "location" })
public class PlayListDetailDVO {

	private StringProperty title;

	private StringProperty hash;

	private StringProperty location;

	public PlayListDetailDVO() {
		this.title = new SimpleStringProperty();
		this.hash = new SimpleStringProperty();
		this.location = new SimpleStringProperty();
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

	public final StringProperty locationProperty() {
		return this.location;
	}

	public final String getLocation() {
		return this.locationProperty().get();
	}

	public final void setLocation(final String location) {
		this.locationProperty().set(location);
	}

	public final StringProperty hashProperty() {
		return this.hash;
	}

	public final java.lang.String getHash() {
		return this.hashProperty().get();
	}

	public final void setHash(final java.lang.String hash) {
		this.hashProperty().set(hash);
	}

}
