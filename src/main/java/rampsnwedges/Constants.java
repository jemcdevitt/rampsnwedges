package rampsnwedges;
/*
 * Ramps n Wedges
 * Copyright (c) 2026, Jere McDevitt
 *
 * Licensed under the MIT License.
 * See LICENSE file in the project root for full license information.
 */

import org.bukkit.NamespacedKey;

public class Constants {
	static public final String NAME_SPACE = "rampsnwedges";
	

	static public final NamespacedKey RNW_TYPE_KEY = new NamespacedKey(NAME_SPACE, "rnw_type_key");
	static public final String RNW_TYPE_BLOCK = "rnw_type_block";
	
	static public final NamespacedKey RNW_ID_KEY = new NamespacedKey(NAME_SPACE, "rnw_id_key");


	static public final NamespacedKey BLOCK_ID_KEY = new NamespacedKey(NAME_SPACE, "rnw_block_id_key");
	static public final String BLOCK_KEY_PREFIX = "placed_";


	//value of this key will be 'ramp_x_y_z' which is the block number a ramp ItemDisplay is connected to
	static public final NamespacedKey RNW_RAMP_ID_KEY = new NamespacedKey(NAME_SPACE, "rnw_ramp_id_key");

	//value of this key will be 'wedge_x_y_z' for the barrier block a wedge display represents
	static public final NamespacedKey RNW_WEDGE_ID_KEY = new NamespacedKey(NAME_SPACE, "rnw_wedge_id_key");
}
