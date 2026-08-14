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
	WEDGE_SW("wedge_sw", "Wedge Southwest", "templates/wedge_sw.json"),
	HIP_NE("hip_ne", "Hip Northeast", "templates/hip.json"),
	HIP_NW("hip_nw", "Hip Northwest", "templates/hip.json"),
	HIP_SE("hip_se", "Hip Southeast", "templates/hip.json"),
	HIP_SW("hip_sw", "Hip Southwest", "templates/hip.json"),
	PYRAMID("pyramid", "Pyramid", "templates/pyramid.json");

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
		return this == WEDGE_NE || this == WEDGE_NW
			|| this == WEDGE_SE || this == WEDGE_SW;
	}

	public boolean isHip() {
		return this == HIP_NE || this == HIP_NW
			|| this == HIP_SE || this == HIP_SW;
	}

	public boolean isPyramid() {
		return this == PYRAMID;
	}

	public boolean usesBarrierCarrier() {
		return isWedge() || isHip() || isPyramid();
	}
}
