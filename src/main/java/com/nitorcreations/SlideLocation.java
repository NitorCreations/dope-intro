package com.nitorcreations;

public class SlideLocation {
	final int x, y;
	public final double coordX, coordY;
	public static final double SLIDE_SLOT_SIZE=200;
	public static final double SLIDE_RANDOM_POSITION=80;
	public SlideLocation(int x, int y) {
		this.x = x;
		this.y = y;
		this.coordX = (x * SLIDE_SLOT_SIZE) + (SLIDE_SLOT_SIZE/2) + (SLIDE_RANDOM_POSITION * Math.random());
		this.coordY = (y * SLIDE_SLOT_SIZE) + (SLIDE_SLOT_SIZE/2) + (SLIDE_RANDOM_POSITION * Math.random());
	}
}
