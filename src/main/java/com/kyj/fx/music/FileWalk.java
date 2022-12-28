/********************************
 *	프로젝트 : gargoyle-music
 *	패키지   : com.kyj.fx.music
 *	작성일   : 2017. 5. 8.
 *	작성자   : KYJ
 *******************************/
package com.kyj.fx.music;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * @author KYJ
 *
 */
public class FileWalk<T> {

	private File root;
	private int maxSize;
	private List<T> item;
	private Function<File, T> convert;
	private Predicate<File> filter;

	public FileWalk(File root) {
		this.root = root;
		this.item = new ArrayList<>();
	}

	public FileWalk(File root, int maxSize) {
		this.root = root;
		this.maxSize = maxSize;
		this.item = new ArrayList<>(this.maxSize);
	}

	public void walk() {
		walk(this.root);
	}

	public void setConvert(Function<File, T> convert) {
		this.convert = convert;
	}

	public Function<File, T> getConvert() {
		return convert;
	}

	public Predicate<File> getFilter() {
		return filter;
	}

	public void setFilter(Predicate<File> filter) {
		this.filter = filter;
	}

	private int count = 0;

	private void walk(File f) {
		count = 0;
		Queue<File> queue = new LinkedList<>();
		queue.add(f);

		while (!queue.isEmpty()) {
			File poll = queue.poll();

			if (poll.isFile()) {

				if (count >= this.maxSize)
					return;

				if (this.filter != null) {
					if (this.filter.test(poll)) {
						boolean add = this.item.add(convert.apply(poll));
						if (add)
							count++;
					}

				}

			} else {
				queue.addAll(Arrays.asList(poll.listFiles()));
			}
		}
		if (item.size() >= this.maxSize)
			return;

	}

	public List<T> getItem() {
		return item;
	}

}
