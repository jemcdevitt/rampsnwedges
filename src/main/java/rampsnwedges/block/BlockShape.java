package rampsnwedges.block;
/*
 * Ramps n Wedges
 *
 * Copyright (c) 2026, Jere McDevitt
 *
 * Licensed under the MIT License.
 * See LICENSE file in the project root for full license information.
 */

public enum BlockShape {
	RAMP("ramp", "Ramp", "templates/ramp.json"),
	WEDGE_NE("wedge_ne", "Wedge Northeast", "templates/wedge_ne.json"),
	WEDGE_NW("wedge_nw", "Wedge Northwest", "templates/wedge_nw.json"),
	WEDGE_SE("wedge_se", "Wedge Southeast", "templates/wedge_se.json"),
	WEDGE_SW("wedge_sw", "Wedge Southwest", "templates/wedge_sw.json");

	private final String id;
	private final String displayName;
	private final String templateResource;

	BlockShape(String id, String displayName, String templateResource) {
		this.id = id;
		this.displayName = displayName;
		this.templateResource = templateResource;
	}

	public String id() {
		return id;
	}

	public String displayName() {
		return displayName;
	}

	public String templateResource() {
		return templateResource;
	}

	public boolean isRamp() {
		return this == RAMP;
	}

	public boolean isWedge() {
		return this != RAMP;
	}
}
