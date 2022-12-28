/********************************
 *	프로젝트 : gargoyle-music
 *	패키지   : com.kyj.fx.music
 *	작성일   : 2017. 4. 28.
 *	작성자   : KYJ
 *******************************/
package com.kyj.fx.music;

import javafx.event.Event;
import javafx.event.EventType;

/**
 * @author KYJ
 *
 */
public class AlbumActionEvent extends Event {

	private Mp3Model f;

	// public AlbumClickEvent(EventType<? extends Event> eventType) {
	// super(eventType);
	// }

	// public AlbumClickEvent(Object source, EventTarget target, EventType<?
	// extends Event> eventType) {
	// super(source, target, eventType);
	// }

	public AlbumActionEvent(Mp3Model f, EventType<AlbumActionEvent> action) {
		super(null, null, action);
		this.f = f;
	}

	public static final EventType<AlbumActionEvent> ALBUM_ACTION = new EventType<AlbumActionEvent>(AlbumActionEvent.ANY, "ALBUM_ACTION");

	/**
	 * @최초생성일 2017. 4. 28.
	 */
	private static final long serialVersionUID = 6946610215281961766L;

	public Mp3Model getMp3Model() {
		return f;
	}
}
