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
	RAMP("ramp",                             ShapeDirection.NONE, "Ramp",                      "templates/ramp.json"),
	WEDGE_NE("wedge_ne",                     ShapeDirection.NE,   "Wedge Northeast",           "templates/wedge_ne.json"),
	WEDGE_NW("wedge_nw",                     ShapeDirection.NW,   "Wedge Northwest",           "templates/wedge_nw.json"),
	WEDGE_SE("wedge_se",                     ShapeDirection.SE,   "Wedge Southeast",           "templates/wedge_se.json"),
	WEDGE_SW("wedge_sw",                     ShapeDirection.SW,   "Wedge Southwest",           "templates/wedge_sw.json"),
	HIP_NE("hip_ne",                         ShapeDirection.NE,   "Hip Northeast",             "templates/hip.json"),
	HIP_NW("hip_nw",                         ShapeDirection.NW,   "Hip Northwest",             "templates/hip.json"),
	HIP_SE("hip_se",                         ShapeDirection.SE,   "Hip Southeast",             "templates/hip.json"),
	HIP_SW("hip_sw",                         ShapeDirection.SW,   "Hip Southwest",             "templates/hip.json"),
	HIP_NE_INVERTED("hip_ne_inverted",       ShapeDirection.NE,   "Hip Northeast Inverted",    "templates/hip.json"),
	HIP_NW_INVERTED("hip_nw_inverted",       ShapeDirection.NW,   "Hip Northwest Inverted",    "templates/hip.json"),
	HIP_SE_INVERTED("hip_se_inverted",       ShapeDirection.SE,   "Hip Southeast Inverted",    "templates/hip.json"),
	HIP_SW_INVERTED("hip_sw_inverted",       ShapeDirection.SW,   "Hip Southwest Inverted",    "templates/hip.json"),
	VALLEY_NE("valley_ne",                   ShapeDirection.NE,   "Valley Northeast",          "templates/valley.json"),
	VALLEY_NW("valley_nw",                   ShapeDirection.NW,   "Valley Northwest",          "templates/valley.json"),
	VALLEY_SE("valley_se",                   ShapeDirection.SE,   "Valley Southeast",          "templates/valley.json"),
	VALLEY_SW("valley_sw",                   ShapeDirection.SW,   "Valley Southwest",          "templates/valley.json"),
	VALLEY_NE_INVERTED("valley_ne_inverted", ShapeDirection.NE,   "Valley Northeast Inverted", "templates/valley.json"),
	VALLEY_NW_INVERTED("valley_nw_inverted", ShapeDirection.NW,   "Valley Northwest Inverted", "templates/valley.json"),
	VALLEY_SE_INVERTED("valley_se_inverted", ShapeDirection.SE,   "Valley Southeast Inverted", "templates/valley.json"),
	VALLEY_SW_INVERTED("valley_sw_inverted", ShapeDirection.SW,   "Valley Southwest Inverted", "templates/valley.json"),
	PYRAMID("pyramid",                       ShapeDirection.NONE, "Pyramid",                   "templates/pyramid.json");

	private final ShapeDirection direction;
	private final String id;
	private final String displayName;
	private final String templateResource;

	BlockShape(String id, ShapeDirection direction, String displayName, String templateResource) {
		this.id = id;
		this.direction = direction;
		this.displayName = displayName;
		this.templateResource = templateResource;
	}

	public ShapeDirection direction() {
		return direction;
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
			|| this == HIP_SE || this == HIP_SW
			|| this == HIP_NE_INVERTED || this == HIP_NW_INVERTED
			|| this == HIP_SE_INVERTED || this == HIP_SW_INVERTED;
	}

	public boolean isValley() {
		return this == VALLEY_NE || this == VALLEY_NW
			|| this == VALLEY_SE || this == VALLEY_SW
			|| this == VALLEY_NE_INVERTED || this == VALLEY_NW_INVERTED
			|| this == VALLEY_SE_INVERTED || this == VALLEY_SW_INVERTED;
	}

	public boolean isInverted() {
		return this == HIP_NE_INVERTED || this == HIP_NW_INVERTED
			|| this == HIP_SE_INVERTED || this == HIP_SW_INVERTED
			|| this == VALLEY_NE_INVERTED || this == VALLEY_NW_INVERTED
			|| this == VALLEY_SE_INVERTED || this == VALLEY_SW_INVERTED;
	}

	public boolean isPyramid() {
		return this == PYRAMID;
	}

	public boolean usesBarrierCarrier() {
		return isWedge() || isHip() || isValley() || isPyramid();
	}
}
