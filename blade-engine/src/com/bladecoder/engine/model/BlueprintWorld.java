package com.bladecoder.engine.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collection;

public class BlueprintWorld implements VerbContainer {
	@JsonProperty
	protected int width;
	@JsonProperty
	protected int height;
	@JsonProperty
	protected String initChapter;

	private VerbManager verbManager = new VerbManager();

	@JsonProperty
	private Collection<Verb> getVerbs() {
		return verbManager.getVerbs().values();
	}

	private void setVerbs(Collection<Verb> verbs) {
		for (Verb verb : verbs) {
			verbManager.addVerb(verb);
		}
	}

	public int getWidth() {
		return width;
	}

	@TrackPropertyChanges
	public void setWidth(int width) {
		this.width = width;
	}

	public int getHeight() {
		return height;
	}

	@TrackPropertyChanges
	public void setHeight(int height) {
		this.height = height;
	}

	public String getInitChapter() {
		return initChapter;
	}

	@TrackPropertyChanges
	public void setInitChapter(String initChapter) {
		this.initChapter = initChapter;
	}

	@Override
	public VerbManager getVerbManager() {
		return verbManager;
	}
}
